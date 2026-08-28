package com.beatlamp.client.audio;

import java.util.Arrays;

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
	private final int lowHighBin;
	private final int highHighBin;

	private final float[] ring;
	private int ringIndex;
	private int ringFilled;
	private final float[] window;
	private final float[] fftReal;
	private final float[] fftImag;
	private final float[] aWeights;

	private final float[] bands = new float[BAND_COUNT];
	private final float[] bandAverages = new float[BAND_COUNT];
	private final int[] bandLowBin = new int[BAND_COUNT];
	private final int[] bandHighBin = new int[BAND_COUNT];

	private long totalSamples;
	private float bassAverage = 0.001F;
	private float envelope;
	private volatile boolean beatReady;
	private volatile boolean kickReady;
	private volatile boolean snareReady;
	private volatile boolean hihatReady;
	private volatile boolean impactReady;

	private float previousEnvelope;
	private long lastBeatSample = -1_000_000L;
	private float lastBeatIntensity;

	// Complex Spectral Difference tracking
	private final float[] prevMag;
	private final float[] prevPhase1;
	private final float[] prevPhase2;

	// 7-Frame Median Filter for Noise Floor Rejection
	private static final int MEDIAN_WINDOW = 7;
	private final float[] medianRing = new float[MEDIAN_WINDOW];
	private final float[] medianScratch = new float[MEDIAN_WINDOW];
	private int medianIndex;
	private int medianFilled;

	// Beat Grid & Online BPM Induction
	private float estimatedBpm = 124.0F;
	private float gridPhase = 0.0F;
	private float gridPulse = 0.0F;
	private long prevBeatSample = -1_000_000L;

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
		this.aWeights = new float[this.fftSize / 2 + 1];

		float binHz = (float) this.sampleRate / this.fftSize;

		// 1. Hann Windowing
		for (int i = 0; i < this.fftSize; i++) {
			this.window[i] = (float) (0.5 - 0.5 * Math.cos(2.0 * Math.PI * i / (this.fftSize - 1)));
		}

		// 2. ISO 226 / A-Weighting Human Equal-Loudness Contour Table
		for (int i = 1; i <= this.fftSize / 2; i++) {
			double f = i * binHz;
			double f2 = f * f;
			double num = 12194.0 * 12194.0 * f2 * f2;
			double den = (f2 + 20.6 * 20.6) * Math.sqrt((f2 + 107.7 * 107.7) * (f2 + 737.9 * 737.9)) * (f2 + 12194.0 * 12194.0);
			double ra = den > 0 ? num / den : 0.0;
			double db = 2.0 + 20.0 * Math.log10(Math.max(1e-6, ra));
			// Convert dB to perceptual linear scaling factor (normalized around 1kHz)
			this.aWeights[i] = (float) Math.clamp(Math.pow(10.0, db / 20.0) * 1.15, 0.25, 2.0);
		}
		this.aWeights[0] = this.aWeights[1];

		// 3. Band Bins Mapping
		for (int b = 0; b < BAND_COUNT; b++) {
			float lowHz = BAND_MIN_HZ * (float) Math.pow(BAND_MAX_HZ / BAND_MIN_HZ, (double) b / BAND_COUNT);
			float highHz = BAND_MIN_HZ * (float) Math.pow(BAND_MAX_HZ / BAND_MIN_HZ, (double) (b + 1) / BAND_COUNT);
			this.bandLowBin[b] = Math.max(1, (int) (lowHz / binHz));
			this.bandHighBin[b] = Math.max(this.bandLowBin[b], Math.min(this.fftSize / 2 - 1, (int) (highHz / binHz)));
			this.bandAverages[b] = 0.001F;
		}

		// Bass band (Kick drums: 30 - 150 Hz)
		this.lowBassBin = Math.max(1, (int) (30.0F / binHz));
		this.highBassBin = Math.min(this.fftSize / 2 - 1, (int) (150.0F / binHz));

		// Mid-high transient band (Snare/Clap/Rimshot: 1100 - 4500 Hz)
		this.lowMidBin = Math.max(1, (int) (1100.0F / binHz));
		this.highMidBin = Math.min(this.fftSize / 2 - 1, (int) (4500.0F / binHz));

		// High band (Hi-hat/Cymbals: 6000 - 12000 Hz)
		this.lowHighBin = Math.max(1, (int) (6000.0F / binHz));
		this.highHighBin = Math.min(this.fftSize / 2 - 1, (int) (12000.0F / binHz));

		int maxTrackedBin = Math.max(this.highBassBin, Math.max(this.highMidBin, this.highHighBin)) + 1;
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
		this.update(count);
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

	public boolean consumeKick() {
		boolean kick = this.kickReady;
		this.kickReady = false;
		return kick;
	}

	public boolean consumeSnare() {
		boolean snare = this.snareReady;
		this.snareReady = false;
		return snare;
	}

	public boolean consumeHihat() {
		boolean hihat = this.hihatReady;
		this.hihatReady = false;
		return hihat;
	}

	public boolean consumeImpact() {
		boolean impact = this.impactReady;
		this.impactReady = false;
		return impact;
	}

	private void update(int stepSamples) {
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

		// 1. Complex-Domain Spectral Difference for Sub-Bass Kick
		for (int bin = this.lowBassBin; bin <= this.highBassBin; bin++) {
			float mag = magnitude(bin) * this.aWeights[bin];
			float phase = (float) Math.atan2(this.fftImag[bin], this.fftReal[bin]);

			// Phase prediction
			float expectedPhase = 2.0F * this.prevPhase1[bin] - this.prevPhase2[bin];
			float phaseDiff = phase - expectedPhase;
			float pMag = this.prevMag[bin];

			// Complex distance
			float csd = (float) Math.sqrt(Math.max(0.0F, mag * mag + pMag * pMag - 2.0F * mag * pMag * (float) Math.cos(phaseDiff)));
			if (mag >= pMag) {
				complexBassFlux += csd;
			} else {
				complexBassFlux += csd * 0.15F;
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
			float mag = magnitude(bin) * this.aWeights[bin];
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

		// 3. Hi-Hat / Cymbal Sizzle Detection
		float highFlux = 0.0F;
		for (int bin = this.lowHighBin; bin <= this.highHighBin; bin++) {
			float mag = magnitude(bin) * this.aWeights[bin];
			float pMag = this.prevMag[bin];
			if (mag > pMag) {
				highFlux += (mag - pMag);
			}
			this.prevMag[bin] = mag;
		}
		int highBins = this.highHighBin - this.lowHighBin + 1;
		highFlux /= highBins * this.fftSize * 0.5F;

		// 4. Combined Onset Flux with 7-Frame Median Filter
		float rawFlux = complexBassFlux * 1.0F + complexMidFlux * 0.38F;

		this.medianRing[this.medianIndex] = rawFlux;
		this.medianIndex = (this.medianIndex + 1) % MEDIAN_WINDOW;
		if (this.medianFilled < MEDIAN_WINDOW) this.medianFilled++;

		float filteredFlux = this.getMedianFlux(rawFlux);

		this.fluxTimes[this.fluxCursor] = this.totalSamples;
		this.fluxValues[this.fluxCursor] = filteredFlux;
		this.fluxCursor = (this.fluxCursor + 1) % FLUX_HISTORY_SIZE;
		if (this.fluxFilled < FLUX_HISTORY_SIZE) {
			this.fluxFilled++;
		}

		float localAvg = this.localFluxAverage();
		float localVariance = this.localFluxVariance(localAvg);
		boolean rising = filteredFlux > this.previousFlux;
		this.previousFlux = filteredFlux;

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

		// 16 Frequency Equalizer Bands (Equipped with A-Weighting)
		for (int b = 0; b < BAND_COUNT; b++) {
			float sum = 0.0F;
			int count = 0;

			for (int bin = this.bandLowBin[b]; bin <= this.bandHighBin[b]; bin++) {
				sum += magnitude(bin) * this.aWeights[bin];
				count++;
			}

			float value = count > 0 ? sum / (count * this.fftSize * 0.5F) : 0.0F;
			this.bandAverages[b] = this.bandAverages[b] * 0.99F + value * 0.01F;
			float bandTarget = clamp01(value / (this.bandAverages[b] * 2.2F + 0.0001F));
			float bandDiff = bandTarget - this.bands[b];
			this.bands[b] += bandDiff * (bandDiff > 0.0F ? 0.55F : 0.14F);
		}

		// 5. Exponential Refractory Decay (Anti Double-Trigger)
		float elapsedSec = (float) (this.totalSamples - this.lastBeatSample) / this.sampleRate;
		float refractoryDecay = this.lastBeatIntensity * 0.85F * (float) Math.exp(-elapsedSec / 0.085F);

		float adaptiveThreshold = localAvg * 1.22F + (float) Math.sqrt(localVariance) * 0.36F + refractoryDecay + 0.0008F;
		long minBeatGap = (long) (this.sampleRate * 0.115); // ~115ms min gap

		boolean hasKick = complexBassFlux > adaptiveThreshold * 0.95F && bassEnergy > 0.005F;
		boolean hasSnare = complexMidFlux > adaptiveThreshold * 0.65F && complexMidFlux > 0.007F;
		boolean hasHihat = highFlux > adaptiveThreshold * 0.45F && highFlux > 0.006F;

		// 6. Beat-Grid Metronome Phase Progression
		float beatPeriodSamples = ((float) this.sampleRate * 60.0F) / Math.max(60.0F, this.estimatedBpm);
		this.gridPhase = (this.gridPhase + (float) stepSamples / beatPeriodSamples) % 1.0F;
		float phaseDist = Math.min(this.gridPhase, 1.0F - this.gridPhase);
		this.gridPulse = (float) Math.exp(-(phaseDist * phaseDist) / 0.016F);

		if (rising && (hasKick || hasSnare) && (this.totalSamples - this.lastBeatSample > minBeatGap)) {
			long ioiSamples = this.totalSamples - this.prevBeatSample;
			if (ioiSamples > (long) (this.sampleRate * 0.28) && ioiSamples < (long) (this.sampleRate * 1.5)) {
				float instantBpm = 60.0F * (float) this.sampleRate / (float) ioiSamples;
				while (instantBpm < 85.0F) instantBpm *= 2.0F;
				while (instantBpm > 185.0F) instantBpm *= 0.5F;

				this.estimatedBpm = this.estimatedBpm * 0.88F + instantBpm * 0.12F;
			}
			this.prevBeatSample = this.totalSamples;
			this.gridPhase *= 0.25F; // Lock phase to real beat

			this.lastBeatSample = this.totalSamples;
			this.lastBeatIntensity = filteredFlux;
			this.beatReady = true;
			if (hasKick) this.kickReady = true;
			if (hasSnare) this.snareReady = true;
		}

		if (hasHihat) {
			this.hihatReady = true;
		}
	}

	public float getGridPulse() {
		return this.gridPulse;
	}

	public float getEstimatedBpm() {
		return this.estimatedBpm;
	}

	private float getMedianFlux(float fallback) {
		if (this.medianFilled <= 0) return fallback;
		for (int i = 0; i < this.medianFilled; i++) {
			this.medianScratch[i] = this.medianRing[i];
		}
		Arrays.sort(this.medianScratch, 0, this.medianFilled);
		return this.medianScratch[this.medianFilled / 2];
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
