/*
 * Copyright (C) 2015 Daniel Nilsson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.danielnilsson9.colorpickerview.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.github.danielnilsson9.colorpickerview.ColorUtils;
import com.github.danielnilsson9.colorpickerview.R;
import com.github.danielnilsson9.colorpickerview.view.ColorPanelView;
import com.github.danielnilsson9.colorpickerview.view.ColorPickerView;
import com.github.danielnilsson9.colorpickerview.view.ColorPickerView.OnColorChangedListener;

public class ColorPickerDialogFragment extends DialogFragment {

	private static final String ARG_HEXADECIMAL_INPUT = "hexadecimal_input";
	private int mDialogId = -1;
	private ColorPickerView mColorPicker;
	private ColorPanelView mNewColorPanel;
	private EditText mHexadecimalInput;
	private boolean mSkipHexadecimalTextChange;
	private ColorPickerDialogListener mListener;

	private static ColorPickerDialogFragment newInstance(
			int dialogId, String title, String okButtonText, int initialColor, boolean showAlphaSlider, boolean showHexadecimalInput) {

		ColorPickerDialogFragment frag = new ColorPickerDialogFragment();
		Bundle args = new Bundle();
		args.putInt("id", dialogId);
		args.putString("title", title);
		args.putString("ok_button", okButtonText);
		args.putBoolean("alpha", showAlphaSlider);
		args.putInt("init_color", initialColor);
		args.putBoolean(ARG_HEXADECIMAL_INPUT, showHexadecimalInput);

		frag.setArguments(args);

		return frag;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mDialogId = getArguments().getInt("id");
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
			try {
				mListener = (ColorPickerDialogListener) activity;
			} catch (ClassCastException e) {
				e.printStackTrace();
				throw new ClassCastException("Parent activity must implement "
						+ "ColorPickerDialogListener to receive result.");
			}
		}
	}

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);

		try {
			mListener = (ColorPickerDialogListener) context;
		} catch (ClassCastException e) {
			e.printStackTrace();
			throw new ClassCastException("Parent activity must implement "
					+ "ColorPickerDialogListener to receive result.");
		}
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Dialog d = super.onCreateDialog(savedInstanceState);


		d.requestWindowFeature(Window.FEATURE_NO_TITLE);
		d.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT,
				ViewGroup.LayoutParams.WRAP_CONTENT);

		return d;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.colorpickerview__dialog_color_picker, container);

		final boolean showHexadecimalInput = getArguments().getBoolean(ARG_HEXADECIMAL_INPUT);
		final boolean showAlphaSlider = getArguments().getBoolean("alpha");
		TextView titleView = (TextView) v.findViewById(android.R.id.title);

		mColorPicker = (ColorPickerView)
				v.findViewById(R.id.colorpickerview__color_picker_view);
		ColorPanelView mOldColorPanel = (ColorPanelView)
				v.findViewById(R.id.colorpickerview__color_panel_old);
		mNewColorPanel = (ColorPanelView)
				v.findViewById(R.id.colorpickerview__color_panel_new);
		Button mOkButton = (Button) v.findViewById(android.R.id.button1);
		mHexadecimalInput = (EditText) v.findViewById(R.id.colorpickerview__hexadecimal_input);

		mColorPicker.setOnColorChangedListener(new OnColorChangedListener() {

			@Override
			public void onColorChanged(int newColor) {
				if (showHexadecimalInput) {
					String hexadecimalColor = ColorUtils.colorToString(newColor, showAlphaSlider);
					mSkipHexadecimalTextChange = true;
					mHexadecimalInput.setText(hexadecimalColor);
				}
				mNewColorPanel.setColor(newColor);
			}
		});

		mOkButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				mListener.onColorSelected(mDialogId, mColorPicker.getColor());
				getDialog().dismiss();
			}

		});


		String title = getArguments().getString("title");

		if (title != null) {
			titleView.setText(title);
		} else {
			titleView.setVisibility(View.GONE);
		}


		if (savedInstanceState == null) {
			mColorPicker.setAlphaSliderVisible(showAlphaSlider);

			String ok = getArguments().getString("ok_button");
			if (ok != null) {
				mOkButton.setText(ok);
			}

			mHexadecimalInput.setVisibility(showHexadecimalInput ? View.VISIBLE : View.GONE);
			if (showHexadecimalInput) {
				int maxCharCount = showAlphaSlider ? 9 : 7;
				mHexadecimalInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(maxCharCount)});

				mHexadecimalInput.addTextChangedListener(new TextWatcher() {
					@Override
					public void beforeTextChanged(CharSequence s, int start, int count, int after) {
					}

					@Override
					public void onTextChanged(CharSequence s, int start, int before, int count) {
					}

					@Override
					public void afterTextChanged(Editable s) {
						if (mSkipHexadecimalTextChange) {
							mSkipHexadecimalTextChange = false;
						} else {
							if (s.length() == 0 || s.charAt(0) != '#') {
								s.insert(0, "#");
							} else if (ColorUtils.isValidHexadecimal(s.toString())) {
								int color = ColorUtils.parseColor(s.toString());
								mColorPicker.setColor(color);
								mNewColorPanel.setColor(color);
							}
						}
					}
				});
			}

			int initColor = getArguments().getInt("init_color");

			mOldColorPanel.setColor(initColor);
			mColorPicker.setColor(initColor, true);
		}


		return v;
	}

	@Override
	public void onDismiss(DialogInterface dialog) {
		super.onDismiss(dialog);
		mListener.onDialogDismissed(mDialogId);
	}


	public interface ColorPickerDialogListener {
		void onColorSelected(int dialogId, int color);

		void onDialogDismissed(int dialogId);
	}

	/**
	 * Builder class for ColorPickerDialogFragment.
	 */
	public static class Builder {
		private final int dialogId;
		private final int initialColor;
		private String okButtonText;
		private String title;
		private boolean showAlphaSlider;
		private boolean showHexadecimalInput;

		public Builder(int dialogId, int initialColor) {
			this.dialogId = dialogId;
			this.initialColor = initialColor;
		}

		public Builder title(String title) {
			this.title = title;
			return this;
		}

		public Builder okButtonText(String text) {
			this.okButtonText = text;
			return this;
		}

		public Builder showAlphaSlider(boolean show) {
			this.showAlphaSlider = show;
			return this;
		}

		public Builder showHexadecimalInput(boolean show) {
			this.showHexadecimalInput = show;
			return this;
		}

		public ColorPickerDialogFragment build() {
			return ColorPickerDialogFragment.newInstance(dialogId, title, okButtonText, initialColor, showAlphaSlider, showHexadecimalInput);
		}
	}
}
