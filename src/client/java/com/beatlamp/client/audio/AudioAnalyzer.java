package com.beatlamp.client.audio;

public final class AudioAnalyzer {
	public static final int FFT_SIZE = 1024;
	public static final int BAND_COUNT = 16;
	private static final float BAND_MIN_HZ = 40.0F;
	private static final float BAND_MAX_HZ = 12000.0F;

	private final int sampleRate;
	private final float[] ring = new float[FFT_SIZE];
	private int ringIndex;
	private int ringFilled;
	private final float[] window = new float[FFT_SIZE];
	private final float[] fftReal = new float[FFT_SIZE];
	private final float[] fftImag = new float[FFT_SIZE];
	private final float[] bands = new float[BAND_COUNT];
	private final float[] bandAverages = new float[BAND_COUNT];
	private final int[] bandLowBin = new int[BAND_COUNT];
	private final int[] bandHighBin = new int[BAND_COUNT];

	private long totalSamples;
	private float bassAverage = 0.001F;
	private float envelope;
	private boolean beatReady;
	private long lastBeatSample = -1_000_000L;

	public AudioAnalyzer(int sampleRate) {
		this.sampleRate = Math.max(8000, sampleRate);

		for (int i = 0; i < FFT_SIZE; i++) {
			this.window[i] = (float) (0.5 - 0.5 * Math.cos(2.0 * Math.PI * i / (FFT_SIZE - 1)));
		}

		float binHz = (float) this.sampleRate / FFT_SIZE;

		for (int b = 0; b < BAND_COUNT; b++) {
			float lowHz = BAND_MIN_HZ * (float) Math.pow(BAND_MAX_HZ / BAND_MIN_HZ, (double) b / BAND_COUNT);
			float highHz = BAND_MIN_HZ * (float) Math.pow(BAND_MAX_HZ / BAND_MIN_HZ, (double) (b + 1) / BAND_COUNT);
			this.bandLowBin[b] = Math.max(1, (int) (lowHz / binHz));
			this.bandHighBin[b] = Math.max(this.bandLowBin[b], Math.min(FFT_SIZE / 2 - 1, (int) (highHz / binHz)));
			this.bandAverages[b] = 0.001F;
		}
	}

	public void push(float[] samples, int count) {
		for (int i = 0; i < count; i++) {
			this.ring[this.ringIndex] = samples[i];
			this.ringIndex = (this.ringIndex + 1) & (FFT_SIZE - 1);
			if (this.ringFilled < FFT_SIZE) {
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
		if (this.ringFilled < FFT_SIZE) {
			return;
		}

		for (int i = 0; i < FFT_SIZE; i++) {
			int index = (this.ringIndex + i) & (FFT_SIZE - 1);
			this.fftReal[i] = this.ring[index] * this.window[i];
			this.fftImag[i] = 0.0F;
		}

		Fft.fft(this.fftReal, this.fftImag);

		float binHz = (float) this.sampleRate / FFT_SIZE;
		int lowBin = Math.max(1, (int) (25.0F / binHz));
		int highBin = Math.min(FFT_SIZE / 2 - 1, (int) (160.0F / binHz));
		float bass = 0.0F;

		for (int bin = lowBin; bin <= highBin; bin++) {
			bass += magnitude(bin);
		}

		bass /= (highBin - lowBin + 1) * FFT_SIZE * 0.5F;

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

			float value = count > 0 ? sum / (count * FFT_SIZE * 0.5F) : 0.0F;
			this.bandAverages[b] = this.bandAverages[b] * 0.99F + value * 0.01F;
			float bandTarget = clamp01(value / (this.bandAverages[b] * 2.2F + 0.0001F));
			float bandDiff = bandTarget - this.bands[b];
			this.bands[b] += bandDiff * (bandDiff > 0.0F ? 0.5F : 0.12F);
		}

		long minBeatGap = (long) (this.sampleRate * 0.18);
		if (bass > previousAverage * 1.4F && bass > 0.015F && this.totalSamples - this.lastBeatSample > minBeatGap) {
			this.lastBeatSample = this.totalSamples;
			this.beatReady = true;
		}
	}

	private float magnitude(int bin) {
		return (float) Math.sqrt(this.fftReal[bin] * this.fftReal[bin] + this.fftImag[bin] * this.fftImag[bin]);
	}

	private static float clamp01(float value) {
		return value < 0.0F ? 0.0F : Math.min(value, 1.0F);
	}
}
