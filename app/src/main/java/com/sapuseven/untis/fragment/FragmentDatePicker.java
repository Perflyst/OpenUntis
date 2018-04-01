package com.sapuseven.untis.fragment;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.widget.DatePicker;

import com.sapuseven.untis.R;
import com.sapuseven.untis.activity.ActivityMain;

import java.util.Calendar;
import java.util.Objects;

public class FragmentDatePicker extends DialogFragment implements DatePickerDialog.OnDateSetListener {

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		DatePickerDialog dialog;
		if (getArguments() != null) {
			int year = getArguments().getInt("year");
			int month = getArguments().getInt("month");
			int day = getArguments().getInt("day");
			dialog = new DatePickerDialog(Objects.requireNonNull(getActivity()), this, year, month, day);
		} else {
			Calendar c = Calendar.getInstance();
			int year = c.get(Calendar.YEAR);
			int month = c.get(Calendar.MONTH);
			int day = c.get(Calendar.DAY_OF_MONTH);
			dialog = new DatePickerDialog(Objects.requireNonNull(getActivity()), this, year, month, day);
		}
		dialog.setButton(DatePickerDialog.BUTTON_NEUTRAL, getString(R.string.today), (dialogInterface, i) -> {
			Calendar calendar = Calendar.getInstance();
			onDateSet(new DatePicker(getContext()), calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
		});
		return dialog;
	}

	public void onDateSet(DatePicker view, int year, int month, int day) {
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.YEAR, year);
		calendar.set(Calendar.MONTH, month);
		calendar.set(Calendar.DAY_OF_MONTH, day);
		((ActivityMain) Objects.requireNonNull(getActivity())).goTo(calendar);
	}
}