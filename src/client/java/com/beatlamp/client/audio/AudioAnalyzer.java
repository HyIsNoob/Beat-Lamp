package com.beatlamp.client.audio;

public final class AudioAnalyzer {
	public static final int BAND_COUNT = 16;
	private static final float BAND_MIN_HZ = 40.0F;
	private static final float BAND_MAX_HZ = 12000.0F;

	private final int sampleRate;
	private final int fftSize;
	private final int fftMask;
	private final int lowBassBin;
	private final int highBassBin;
	private final int lowMidBin;
	private final int highMidBin;

	private final float[] ring;
	private int ringIndex;
	private int ringFilled;
	private final float[] window;
	private final float[] fftReal;
	private final float[] fftImag;
	private final float[] bands = new float[BAND_COUNT];
	private final float[] bandAverages = new float[BAND_COUNT];
	private final int[] bandLowBin = new int[BAND_COUNT];
	private final int[] bandHighBin = new int[BAND_COUNT];

	private long totalSamples;
	private float bassAverage = 0.001F;
	private float envelope;
	private volatile boolean beatReady;
	private volatile boolean impactReady;
	private float previousEnvelope;
	private long lastBeatSample = -1_000_000L;

	// Complex Spectral Difference tracking
	private final float[] prevMag;
	private final float[] prevPhase1;
	private final float[] prevPhase2;

	private static final int FLUX_HISTORY_SIZE = 64;
	private static final float FLUX_WINDOW_SECONDS = 1.0F;
	private final long[] fluxTimes = new long[FLUX_HISTORY_SIZE];
	private final float[] fluxValues = new float[FLUX_HISTORY_SIZE];
	private int fluxCursor;
	private int fluxFilled;
	private float previousFlux;

	public AudioAnalyzer(int sampleRate, boolean highQuality) {
		this.sampleRate = Math.max(8000, sampleRate);
		this.fftSize = highQuality ? 2048 : 1024;
		this.fftMask = this.fftSize - 1;
		this.ring = new float[this.fftSize];
		this.window = new float[this.fftSize];
		this.fftReal = new float[this.fftSize];
		this.fftImag = new float[this.fftSize];

		for (int i = 0; i < this.fftSize; i++) {
			this.window[i] = (float) (0.5 - 0.5 * Math.cos(2.0 * Math.PI * i / (this.fftSize - 1)));
		}

		float binHz = (float) this.sampleRate / this.fftSize;

		for (int b = 0; b < BAND_COUNT; b++) {
			float lowHz = BAND_MIN_HZ * (float) Math.pow(BAND_MAX_HZ / BAND_MIN_HZ, (double) b / BAND_COUNT);
			float highHz = BAND_MIN_HZ * (float) Math.pow(BAND_MAX_HZ / BAND_MIN_HZ, (double) (b + 1) / BAND_COUNT);
			this.bandLowBin[b] = Math.max(1, (int) (lowHz / binHz));
			this.bandHighBin[b] = Math.max(this.bandLowBin[b], Math.min(this.fftSize / 2 - 1, (int) (highHz / binHz)));
			this.bandAverages[b] = 0.001F;
		}

		// Bass band (Kick drums: 30 - 160 Hz)
		this.lowBassBin = Math.max(1, (int) (30.0F / binHz));
		this.highBassBin = Math.min(this.fftSize / 2 - 1, (int) (160.0F / binHz));

		// Mid-high transient band (Snare/Clap/Hi-hat: 1000 - 5000 Hz)
		this.lowMidBin = Math.max(1, (int) (1000.0F / binHz));
		this.highMidBin = Math.min(this.fftSize / 2 - 1, (int) (5000.0F / binHz));

		int maxTrackedBin = Math.max(this.highBassBin, this.highMidBin) + 1;
		this.prevMag = new float[maxTrackedBin];
		this.prevPhase1 = new float[maxTrackedBin];
		this.prevPhase2 = new float[maxTrackedBin];
	}

	public void push(float[] samples, int count) {
		this.push(samples, 0, count);
	}

	public void push(float[] samples, int offset, int count) {
		for (int i = 0; i < count; i++) {
			this.ring[this.ringIndex] = samples[offset + i];
			this.ringIndex = (this.ringIndex + 1) & this.fftMask;
			if (this.ringFilled < this.fftSize) {
				this.ringFilled++;
			}
		}

		this.totalSamples += count;
		this.update();
	}

	public long getTotalSamples() {
		return this.totalSamples;
	}

	public int getSampleRate() {
		return this.sampleRate;
	}

	public float getLevel() {
		return this.envelope;
	}

	public float[] getBands() {
		return this.bands;
	}

	public boolean consumeBeat() {
		boolean beat = this.beatReady;
		this.beatReady = false;
		return beat;
	}

	public boolean consumeImpact() {
		boolean impact = this.impactReady;
		this.impactReady = false;
		return impact;
	}

	private void update() {
		if (this.ringFilled < this.fftSize) {
			return;
		}

		for (int i = 0; i < this.fftSize; i++) {
			int index = (this.ringIndex + i) & this.fftMask;
			this.fftReal[i] = this.ring[index] * this.window[i];
			this.fftImag[i] = 0.0F;
		}

		Fft.fft(this.fftReal, this.fftImag);

		float bassEnergy = 0.0F;
		float complexBassFlux = 0.0F;

		// 1. Complex-Domain Spectral Difference for Bass (Kick Transient Detection)
		for (int bin = this.lowBassBin; bin <= this.highBassBin; bin++) {
			float mag = magnitude(bin);
			float phase = (float) Math.atan2(this.fftImag[bin], this.fftReal[bin]);

			// Expected phase prediction: phi_t = 2*phi_{t-1} - phi_{t-2}
			float expectedPhase = 2.0F * this.prevPhase1[bin] - this.prevPhase2[bin];
			float phaseDiff = phase - expectedPhase;
			float pMag = this.prevMag[bin];

			// Complex Euclidean distance: |X_t - \hat{X}_t|
			float csd = (float) Math.sqrt(Math.max(0.0F, mag * mag + pMag * pMag - 2.0F * mag * pMag * (float) Math.cos(phaseDiff)));
			
			// Rectified onset weight: only count positive transient spikes
			if (mag >= pMag) {
				complexBassFlux += csd;
			} else {
				complexBassFlux += csd * 0.2F;
			}

			this.prevPhase2[bin] = this.prevPhase1[bin];
			this.prevPhase1[bin] = phase;
			this.prevMag[bin] = mag;
			bassEnergy += mag;
		}

		int bassBins = this.highBassBin - this.lowBassBin + 1;
		bassEnergy /= bassBins * this.fftSize * 0.5F;
		complexBassFlux /= bassBins * this.fftSize * 0.5F;

		// 2. Mid Transient Detection (Snare / Clap Transients)
		float complexMidFlux = 0.0F;
		for (int bin = this.lowMidBin; bin <= this.highMidBin; bin++) {
			float mag = magnitude(bin);
			float phase = (float) Math.atan2(this.fftImag[bin], this.fftReal[bin]);

			float expectedPhase = 2.0F * this.prevPhase1[bin] - this.prevPhase2[bin];
			float phaseDiff = phase - expectedPhase;
			float pMag = this.prevMag[bin];

			float csd = (float) Math.sqrt(Math.max(0.0F, mag * mag + pMag * pMag - 2.0F * mag * pMag * (float) Math.cos(phaseDiff)));
			if (mag >= pMag) {
				complexMidFlux += csd;
			}

			this.prevPhase2[bin] = this.prevPhase1[bin];
			this.prevPhase1[bin] = phase;
			this.prevMag[bin] = mag;
		}

		int midBins = this.highMidBin - this.lowMidBin + 1;
		complexMidFlux /= midBins * this.fftSize * 0.5F;

		// Combined Complex Onset Flux
		float totalFlux = complexBassFlux * 1.0F + complexMidFlux * 0.35F;

		this.fluxTimes[this.fluxCursor] = this.totalSamples;
		this.fluxValues[this.fluxCursor] = totalFlux;
		this.fluxCursor = (this.fluxCursor + 1) % FLUX_HISTORY_SIZE;
		if (this.fluxFilled < FLUX_HISTORY_SIZE) {
			this.fluxFilled++;
		}

		float localAvg = this.localFluxAverage();
		float localVariance = this.localFluxVariance(localAvg);
		boolean rising = totalFlux > this.previousFlux;
		this.previousFlux = totalFlux;

		float previousAverage = this.bassAverage;
		this.bassAverage = this.bassAverage * 0.995F + bassEnergy * 0.005F;

		float target = clamp01(bassEnergy / (previousAverage * 2.3F + 0.0001F));
		float diff = target - this.envelope;
		this.envelope += diff * (diff > 0.0F ? 0.65F : 0.12F);

		// Drop / Heavy Impact Detection
		if (this.envelope > 0.52F && this.previousEnvelope <= 0.38F) {
			this.impactReady = true;
		}
		this.previousEnvelope = this.envelope;

		// 16 Frequency Equalizer Bands
		for (int b = 0; b < BAND_COUNT; b++) {
			float sum = 0.0F;
			int count = 0;

			for (int bin = this.bandLowBin[b]; bin <= this.bandHighBin[b]; bin++) {
				sum += magnitude(bin);
				count++;
			}

			float value = count > 0 ? sum / (count * this.fftSize * 0.5F) : 0.0F;
			this.bandAverages[b] = this.bandAverages[b] * 0.99F + value * 0.01F;
			float bandTarget = clamp01(value / (this.bandAverages[b] * 2.2F + 0.0001F));
			float bandDiff = bandTarget - this.bands[b];
			this.bands[b] += bandDiff * (bandDiff > 0.0F ? 0.55F : 0.14F);
		}

		// Adaptive Dynamic Threshold
		float adaptiveThreshold = localAvg * 1.25F + (float) Math.sqrt(localVariance) * 0.38F + 0.0008F;
		long minBeatGap = (long) (this.sampleRate * 0.115); // ~115ms min gap

		if (totalFlux > adaptiveThreshold && rising && (bassEnergy > 0.005F || complexMidFlux > 0.006F) && (this.totalSamples - this.lastBeatSample > minBeatGap)) {
			this.lastBeatSample = this.totalSamples;
			this.beatReady = true;
		}
	}

	private float localFluxAverage() {
		long cutoff = this.totalSamples - (long) ((float) this.sampleRate * FLUX_WINDOW_SECONDS);
		float sum = 0.0F;
		int count = 0;

		for (int i = 0; i < this.fluxFilled; i++) {
			if (this.fluxTimes[i] >= cutoff) {
				sum += this.fluxValues[i];
				count++;
			}
		}

		return count > 0 ? sum / count : 0.0F;
	}

	private float localFluxVariance(float mean) {
		long cutoff = this.totalSamples - (long) ((float) this.sampleRate * FLUX_WINDOW_SECONDS);
		float sumSq = 0.0F;
		int count = 0;

		for (int i = 0; i < this.fluxFilled; i++) {
			if (this.fluxTimes[i] >= cutoff) {
				float diff = this.fluxValues[i] - mean;
				sumSq += diff * diff;
				count++;
			}
		}

		return count > 0 ? sumSq / count : 0.0F;
	}

	private float magnitude(int bin) {
		return (float) Math.sqrt(this.fftReal[bin] * this.fftReal[bin] + this.fftImag[bin] * this.fftImag[bin]);
	}

	private static float clamp01(float value) {
		return value < 0.0F ? 0.0F : Math.min(value, 1.0F);
	}
}
