package com.beatlamp.block;

public enum LampOrientation {
	AUTO("screen.beatlamp.orientation.auto"),
	HORIZONTAL("screen.beatlamp.orientation.horizontal"),
	VERTICAL("screen.beatlamp.orientation.vertical");

	private final String translationKey;

	LampOrientation(String translationKey) {
		this.translationKey = translationKey;
	}

	public String getTranslationKey() {
		return this.translationKey;
	}

	public LampOrientation next() {
		LampOrientation[] values = values();
		return values[(this.ordinal() + 1) % values.length];
	}
}
