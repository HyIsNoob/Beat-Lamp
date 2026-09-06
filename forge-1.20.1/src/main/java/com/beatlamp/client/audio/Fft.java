package com.beatlamp.client.audio;

public final class Fft {
	private Fft() {
	}

	public static void fft(float[] real, float[] imag) {
		int n = real.length;
		int j = 0;

		for (int i = 0; i < n - 1; i++) {
			if (i < j) {
				float tempR = real[i];
				float tempI = imag[i];
				real[i] = real[j];
				imag[i] = imag[j];
				real[j] = tempR;
				imag[j] = tempI;
			}

			int k = n >> 1;
			while (k <= j) {
				j -= k;
				k >>= 1;
			}
			j += k;
		}

		for (int len = 2; len <= n; len <<= 1) {
			double angle = -2.0 * Math.PI / len;
			float wlenR = (float) Math.cos(angle);
			float wlenI = (float) Math.sin(angle);

			for (int i = 0; i < n; i += len) {
				float wR = 1.0F;
				float wI = 0.0F;

				for (int k = 0; k < (len >> 1); k++) {
					int uIndex = i + k;
					int vIndex = i + k + (len >> 1);

					float uR = real[uIndex];
					float uI = imag[uIndex];
					float vR = real[vIndex] * wR - imag[vIndex] * wI;
					float vI = real[vIndex] * wI + imag[vIndex] * wR;

					real[uIndex] = uR + vR;
					imag[uIndex] = uI + vI;
					real[vIndex] = uR - vR;
					imag[vIndex] = uI - vI;

					float nextWR = wR * wlenR - wI * wlenI;
					float nextWI = wR * wlenI + wI * wlenR;
					wR = nextWR;
					wI = nextWI;
				}
			}
		}
	}
}
