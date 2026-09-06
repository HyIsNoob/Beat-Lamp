package com.beatlamp.client.audio;

public final class Fft {
	private Fft() {
	}

	public static void fft(float[] real, float[] imag) {
		int n = real.length;

		for (int i = 1, j = 0; i < n; i++) {
			int bit = n >> 1;

			while ((j & bit) != 0) {
				j ^= bit;
				bit >>= 1;
			}

			j ^= bit;

			if (i < j) {
				float tempR = real[i];
				real[i] = real[j];
				real[j] = tempR;
				float tempI = imag[i];
				imag[i] = imag[j];
				imag[j] = tempI;
			}
		}

		for (int len = 2; len <= n; len <<= 1) {
			double angle = -2.0 * Math.PI / len;
			float wReal = (float) Math.cos(angle);
			float wImag = (float) Math.sin(angle);

			for (int i = 0; i < n; i += len) {
				float curReal = 1.0F;
				float curImag = 0.0F;
				int half = len >> 1;

				for (int k = 0; k < half; k++) {
					int u = i + k;
					int v = i + k + half;
					float tReal = real[v] * curReal - imag[v] * curImag;
					float tImag = real[v] * curImag + imag[v] * curReal;
					real[v] = real[u] - tReal;
					imag[v] = imag[u] - tImag;
					real[u] += tReal;
					imag[u] += tImag;
					float nextReal = curReal * wReal - curImag * wImag;
					curImag = curReal * wImag + curImag * wReal;
					curReal = nextReal;
				}
			}
		}
	}
}
