package com.sapuseven.untis.fragment;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.widget.DatePicker;

import com.sapuseven.untis.R;
import com.sapuseven.untis.activity.ActivityMain;

import java.util.Calendar;

/**
 * @author paul
 * @version 1.0
 * @since 2016-11-03
 */

public class FragmentDatePicker extends DialogFragment implements DatePickerDialog.OnDateSetListener {

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		final int year = getArguments().getInt("year");
		final int month = getArguments().getInt("month");
		final int day = getArguments().getInt("day");
		DatePickerDialog dialog = new DatePickerDialog(getActivity(), this, year, month, day);
		dialog.setButton(DatePickerDialog.BUTTON_NEUTRAL, getString(R.string.today), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialogInterface, int i) {
				Calendar calendar = Calendar.getInstance();
				onDateSet(new DatePicker(getContext()), calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
			}
		});
		return dialog;
	}

	public void onDateSet(DatePicker view, int year, int month, int day) {
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.YEAR, year);
		calendar.set(Calendar.MONTH, month);
		calendar.set(Calendar.DAY_OF_MONTH, day);
		((ActivityMain) getActivity()).goTo(calendar);
	}
}