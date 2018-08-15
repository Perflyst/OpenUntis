package com.perflyst.untis.preference;

import android.content.Context;
import android.preference.EditTextPreference;
import android.util.AttributeSet;

public class UnitPreference extends EditTextPreference {
	private static final String ATTRIBUTE_KEY_UNIT = "unit";

	private String unit = "";

	public UnitPreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		setupUnit(attrs);
	}

	public UnitPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		setupUnit(attrs);
	}

	public UnitPreference(Context context) {
		super(context);
	}

	private void setupUnit(AttributeSet attrs) {
		for (int i = 0; i < attrs.getAttributeCount(); i++) {
			String attr = attrs.getAttributeName(i);
			if (attr.equalsIgnoreCase(ATTRIBUTE_KEY_UNIT))
				unit = attrs.getAttributeValue(i);
		}
	}

	@Override
	public CharSequence getSummary() {
		CharSequence summary = super.getSummary();
		if (summary == null)
			return getText() + unit;
		else
			return String.format(summary.toString(), getText());
	}
}
