package com.sapuseven.untis.fragment;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.sapuseven.untis.R;
import com.sapuseven.untis.utils.DateOperations;
import com.sapuseven.untis.utils.ListManager;
import com.sapuseven.untis.utils.TimegridUnitManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import static com.sapuseven.untis.utils.DateOperations.addDaysToInt;
import static com.sapuseven.untis.utils.DateOperations.getDayNameFromInt;
import static com.sapuseven.untis.utils.DateOperations.getStringDateFromInt;

public class FragmentTimetableHeader extends Fragment {
	private float scale;

	public FragmentTimetableHeader() {
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		scale = getActivity().getResources().getDisplayMetrics().density;
		int startDateOffset = getArguments().getInt("position") - 50;
		ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.content_header, container, false);

		ListManager listManager = new ListManager(getContext());
		TimegridUnitManager unitManager = null;
		LinearLayout contentHeader = (LinearLayout) rootView.findViewById(R.id.header_content);

		try {
			unitManager = new TimegridUnitManager();
			unitManager.setList(new JSONObject(listManager.readList("userData", false)).getJSONObject("masterData").getJSONObject("timeGrid").getJSONArray("days"));
		} catch (JSONException e) {
			e.printStackTrace();
		}

		int startDateFromWeek = Integer.parseInt(new SimpleDateFormat("yyyyMMdd", Locale.US)
				.format(DateOperations.getStartDateFromWeek(Calendar.getInstance(), startDateOffset * 7).getTime()));

		for (int i = 0; i < unitManager.getNumberOfDays(); i++) {
			@SuppressLint("InflateParams") View day = getActivity().getLayoutInflater().inflate(R.layout.item_day, null);
			day.setLayoutParams(new LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.MATCH_PARENT,
					LinearLayout.LayoutParams.MATCH_PARENT,
					1.0f));
			((TextView) day.findViewById(R.id.tvDayOfWeek)).setText(getDayNameFromInt(addDaysToInt(startDateFromWeek, i)));
			((TextView) day.findViewById(R.id.tvDateOfDay)).setText(getStringDateFromInt(addDaysToInt(startDateFromWeek, i)));

			String date = String.valueOf(addDaysToInt(startDateFromWeek, i));
			if (new SimpleDateFormat("yyyyMMdd", Locale.US).format(Calendar.getInstance().getTime()).equals(date)) {
				GradientDrawable bottomShape = new GradientDrawable();
				bottomShape.setColor(0xFFBBBBBB);

				Drawable[] layers = {bottomShape};
				LayerDrawable layerList = new LayerDrawable(layers);
				layerList.setLayerInset(0, 0, dp2px(44), 0, 0);
				day.setBackground(layerList);
			}
			contentHeader.addView(day);
		}

		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
		if (sharedPrefs.getBoolean("preference_dark_theme_amoled", false) && sharedPrefs.getBoolean("preference_dark_theme", false))
			rootView.setBackgroundColor(Color.BLACK);

		return rootView;
	}

	@SuppressWarnings("SameParameterValue")
	private int dp2px(int dp) {
		return (int) (dp * scale + 0.5f);
	}
}
