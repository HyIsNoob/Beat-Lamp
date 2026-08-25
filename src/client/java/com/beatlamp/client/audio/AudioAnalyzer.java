package com.beatlamp.client.audio;

public final class AudioAnalyzer {
	public static final int BAND_COUNT = 16;
	private static final float BAND_MIN_HZ = 40.0F;
	private static final float BAND_MAX_HZ = 12000.0F;

	private final int sampleRate;
	private final int fftSize;
	private final int fftMask;
	private final int lowFluxBin;
	private final int highFluxBin;
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
	private boolean beatReady;
	private long lastBeatSample = -1_000_000L;
	private final float[] prevBassMagnitude;
	private static final int FLUX_HISTORY_SIZE = 64;
	private static final float FLUX_WINDOW_SECONDS = 1.2F;
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

		float fluxMaxHz = highQuality ? 250.0F : 160.0F;
		this.lowFluxBin = Math.max(1, (int) (25.0F / binHz));
		this.highFluxBin = Math.min(this.fftSize / 2 - 1, (int) (fluxMaxHz / binHz));
		this.prevBassMagnitude = new float[Math.max(1, this.highFluxBin - this.lowFluxBin + 1)];
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

		float bass = 0.0F;
		float flux = 0.0F;

		for (int bin = this.lowFluxBin; bin <= this.highFluxBin; bin++) {
			float magnitude = magnitude(bin);
			float previous = this.prevBassMagnitude[bin - this.lowFluxBin];
			this.prevBassMagnitude[bin - this.lowFluxBin] = magnitude;
			flux += Math.max(0.0F, magnitude - previous);
			bass += magnitude;
		}

		int fluxBins = this.highFluxBin - this.lowFluxBin + 1;
		bass /= fluxBins * this.fftSize * 0.5F;
		flux /= fluxBins * this.fftSize * 0.5F;

		this.fluxTimes[this.fluxCursor] = this.totalSamples;
		this.fluxValues[this.fluxCursor] = flux;
		this.fluxCursor = (this.fluxCursor + 1) % FLUX_HISTORY_SIZE;
		if (this.fluxFilled < FLUX_HISTORY_SIZE) {
			this.fluxFilled++;
		}

		float localAverage = this.localFluxAverage();
		boolean rising = flux > this.previousFlux;
		this.previousFlux = flux;

		float previousAverage = this.bassAverage;
		this.bassAverage = this.bassAverage * 0.995F + bass * 0.005F;

		float target = clamp01(bass / (previousAverage * 2.5F + 0.0001F));
		float diff = target - this.envelope;
		this.envelope += diff * (diff > 0.0F ? 0.55F : 0.10F);

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
			this.bands[b] += bandDiff * (bandDiff > 0.0F ? 0.5F : 0.12F);
		}

		long minBeatGap = (long) (this.sampleRate * 0.15);
		if (flux > localAverage * 1.35F + 0.0012F && rising && bass > 0.008F && this.totalSamples - this.lastBeatSample > minBeatGap) {
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

	private float magnitude(int bin) {
		return (float) Math.sqrt(this.fftReal[bin] * this.fftReal[bin] + this.fftImag[bin] * this.fftImag[bin]);
	}

	private static float clamp01(float value) {
		return value < 0.0F ? 0.0F : Math.min(value, 1.0F);
	}
}
