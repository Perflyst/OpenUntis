package com.sapuseven.untis.utils.timetable;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.sapuseven.untis.R;
import com.sapuseven.untis.activity.ActivityRoomFinder;
import com.sapuseven.untis.fragment.FragmentTimetable;
import com.sapuseven.untis.utils.Constants;
import com.sapuseven.untis.utils.DateOperations;
import com.sapuseven.untis.utils.ElementName;
import com.sapuseven.untis.utils.ListManager;
import com.sapuseven.untis.utils.SessionInfo;
import com.sapuseven.untis.view.TimetableItemBackground;
import com.sapuseven.untis.view.VerticalTextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import static com.sapuseven.untis.fragment.FragmentTimetable.isBetween;
import static com.sapuseven.untis.utils.Conversions.dp2px;
import static com.sapuseven.untis.utils.DateOperations.addDaysToInt;
import static com.sapuseven.untis.utils.ElementName.ROOM;
import static com.sapuseven.untis.utils.ElementName.TEACHER;
import static com.sapuseven.untis.utils.PreferenceUtils.getPrefBool;
import static com.sapuseven.untis.utils.PreferenceUtils.getPrefInt;

public class TimetableSetup extends AsyncTask<Timetable, Void, GridLayout> {
	private static final float DARKNESS_FACTOR = 0.8f;
	private WeakReference<FragmentTimetable> fragmentContext;

	public TimetableSetup(FragmentTimetable context) {
		this.fragmentContext = new WeakReference<>(context);
	}

	private static int manipulateColor(int color) {
		int a = Color.alpha(color);
		int r = Math.round(Color.red(color) * DARKNESS_FACTOR);
		int g = Math.round(Color.green(color) * DARKNESS_FACTOR);
		int b = Math.round(Color.blue(color) * DARKNESS_FACTOR);
		return Color.argb(a,
				Math.min(r, 255),
				Math.min(g, 255),
				Math.min(b, 255));
	}

	@Override
	protected GridLayout doInBackground(Timetable... timetable) {
		Context context = this.fragmentContext.get().getContext();

		if (context == null)
			return null;

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(fragmentContext.get().main);

		int rows = timetable[0].getHoursPerDay();
		int cols = timetable[0].getNumberOfDays() * 2;

		GridLayout glTimetable = new GridLayout(fragmentContext.get().getContext());
		glTimetable.setColumnCount(cols);
		glTimetable.setRowCount(rows);
		glTimetable.setOrientation(GridLayout.HORIZONTAL);

		DisplayMetrics displayMetrics = new DisplayMetrics();

		((Activity) fragmentContext.get().getContext()).getWindowManager()
				.getDefaultDisplay()
				.getMetrics(displayMetrics);

		int totalWidth = Math.round(displayMetrics.widthPixels
				- fragmentContext.get().getResources().getDimension(R.dimen.left_sidebar_width));
		int dayWidth = totalWidth * 2 / cols;
		int lastDayWidth = totalWidth - (cols / 2 - 1) * dayWidth;
		int hourHeight = dp2px(60);

		boolean alternatingDays = getPrefBool(context, prefs, "preference_alternating_days");
		boolean alternatingHours = getPrefBool(context, prefs, "preference_alternating_hours");

		int alternativeBackgroundColor = Color.WHITE;
		if (fragmentContext.get().isAdded())
			alternativeBackgroundColor = fragmentContext.get().getResources().getInteger(R.integer.preference_alternating_color_default_light);
		if (getPrefBool(context, prefs, "preference_alternating_colors_use_custom"))
			alternativeBackgroundColor = getPrefInt(context, prefs, "preference_alternating_color");
		else if (prefs.getBoolean("preference_dark_theme", false) && fragmentContext.get().isAdded())
			alternativeBackgroundColor = fragmentContext.get().getResources().getInteger(R.integer.preference_alternating_color_default_dark);

		int defaultBackgroundColor = Color.WHITE;
		if (fragmentContext.get().isAdded())
			defaultBackgroundColor = fragmentContext.get().getResources().getColor(android.R.color.background_light);

		int colorRegular = getPrefInt(context, prefs, "preference_background_regular");
		int colorRegularPast = getPrefInt(context, prefs, "preference_background_regular_past");
		int colorIrregular = getPrefInt(context, prefs, "preference_background_irregular");
		int colorIrregularPast = getPrefInt(context, prefs, "preference_background_irregular_past");
		int colorCancelled = getPrefInt(context, prefs, "preference_background_cancelled");
		int colorCancelledPast = getPrefInt(context, prefs, "preference_background_cancelled_past");
		int colorExam = getPrefInt(context, prefs, "preference_background_exam");
		int colorExamPast = getPrefInt(context, prefs, "preference_background_exam_past");
		int colorFree = getPrefInt(context, prefs, "preference_background_free");
		int itemPadding = dp2px(getPrefInt(context, prefs, "preference_timetable_item_padding", true));
		int cornerRadius = dp2px(getPrefInt(context, prefs, "preference_timetable_item_corner_radius", true));
		int nameFontSize = getPrefInt(context, prefs, "preference_timetable_lesson_name_font_size", true);
		int infoFontSize = getPrefInt(context, prefs, "preference_timetable_lesson_info_font_size", true);

		boolean lightText = getPrefBool(context, prefs, "preference_timetable_item_text_light");
		boolean centeredInfo = getPrefBool(context, prefs, "preference_timetable_centered_lesson_info");
		boolean boldTitle = getPrefBool(context, prefs, "preference_timetable_bold_lesson_name");

		TypedValue typedValue = new TypedValue();

		TypedArray a = fragmentContext.get().main.obtainStyledAttributes(typedValue.data, new int[]{
				R.attr.colorPrimary,
				R.attr.colorPrimaryDark,
				android.R.attr.colorBackground
		});

		if (getPrefBool(context, prefs, "preference_dark_theme_amoled")
				&& getPrefBool(context, prefs, "preference_dark_theme")) {
			defaultBackgroundColor = Color.BLACK;
		} else {
			defaultBackgroundColor = a.getColor(2, defaultBackgroundColor);
		}

		if (getPrefBool(context, prefs, "preference_use_theme_background")) {
			colorRegular = a.getColor(0, colorRegular);
			colorRegularPast = a.getColor(1, colorRegularPast);
		}

		a.recycle();

		if (fragmentContext.get().userDataList == null)
			fragmentContext.get().userDataList = ListManager.getUserData(fragmentContext.get().listManager);

		JSONObject masterData = null;
		ArrayList<TimegridUnitManager.UnitData> units = null;

		try {
			masterData = fragmentContext.get().userDataList.getJSONObject("masterData");
			TimegridUnitManager unitManager = new TimegridUnitManager(masterData.getJSONObject("timeGrid").getJSONArray("days"));

			units = unitManager.getUnits();
		} catch (JSONException e) {
			e.printStackTrace();
		}

		JSONArray holidays = null;
		try {
			holidays = masterData.getJSONArray("holidays");
		} catch (JSONException e) {
			e.printStackTrace();
		}

		for (int day = 0; day < cols / 2; day++) {
			LinearLayout holidayItem = null;
			StringBuilder holidayLabelString = null;

			try {
				if (holidays != null)
					for (int i = 0; i < holidays.length(); i++) {
						if (isBetween(addDaysToInt(fragmentContext.get().startDateFromWeek, day),
								Integer.parseInt(holidays.getJSONObject(i)
										.getString("startDate")
										.replace("-", "")),
								Integer.parseInt(holidays.getJSONObject(i)
										.getString("endDate")
										.replace("-", "")))) {

							if (holidayItem == null) {
								holidayItem = new LinearLayout(context);

								GridLayout.Spec rowSpec = GridLayout.spec(0, rows);
								GridLayout.Spec colSpec = GridLayout.spec(day * 2, 2);
								GridLayout.LayoutParams params = new GridLayout.LayoutParams(rowSpec, colSpec);
								params.height = hourHeight * rows;
								if (day * 2 == cols - 2)
									params.width = lastDayWidth; // fill up all the remaining space to prevent a gap
								else
									params.width = dayWidth;

								holidayItem.setLayoutParams(params);
								holidayItem.setGravity(Gravity.CENTER_HORIZONTAL);
							}

							if (holidayLabelString == null) {
								holidayLabelString = new StringBuilder(holidays.getJSONObject(i).getString("longName"));
							} else {
								holidayLabelString.append(fragmentContext.get().getString(R.string.holiday_label_separator)).append(holidays.getJSONObject(i).getString("longName"));
							}
						}
					}
			} catch (JSONException e) {
				e.printStackTrace();
			}

			if (holidayItem != null) {
				VerticalTextView holidayLabel = new VerticalTextView(context);
				LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
						ViewGroup.LayoutParams.WRAP_CONTENT,
						ViewGroup.LayoutParams.MATCH_PARENT);
				labelParams.setMargins(0, dp2px(12), 0, 0);
				holidayLabel.setLayoutParams(labelParams);
				holidayLabel.setText(holidayLabelString);

				holidayItem.setBackgroundColor(colorFree);
				holidayItem.addView(holidayLabel);
				glTimetable.addView(holidayItem);
				continue;
			}

			int lastHourIndex = 0;
			for (int i = 0; i < rows; i++)
				if (timetable[0].has(day, i))
					lastHourIndex = i;

			for (int hour = 0; hour < rows; hour++) {
				final ArrayList<TimetableItemData> allItems = (ArrayList<TimetableItemData>) timetable[0].getItems(day, hour);
				if (allItems.size() == 0) { // A free hour
					LinearLayout emptyItem = new LinearLayout(context);

					GridLayout.Spec rowSpec = GridLayout.spec(hour, 1);
					GridLayout.Spec colSpec = GridLayout.spec(day * 2, 2);
					GridLayout.LayoutParams params = new GridLayout.LayoutParams(rowSpec, colSpec);
					params.height = hourHeight;
					if (day * 2 == cols - 2)
						params.width = lastDayWidth; // fill up all the remaining space to prevent a gap
					else
						params.width = dayWidth;

					emptyItem.setLayoutParams(params);

					if (shouldColorizeCell(day, hour, alternatingDays, alternatingHours))
						emptyItem.setBackgroundColor(alternativeBackgroundColor);

					if (shouldShowIndicatorForHour(prefs, hour, lastHourIndex)) {
						ImageView indicator = new ImageView(context);

						indicator.setImageDrawable(
								ActivityRoomFinder.getRoomStates(fragmentContext.get().getActivity(), prefs.getString("preference_room_to_display_in_free_lessons", null))
										.charAt(day * rows + hour) == '1' ?
										ContextCompat.getDrawable(fragmentContext.get().getActivity(), R.drawable.ic_room_occupied) :
										ContextCompat.getDrawable(fragmentContext.get().getActivity(), R.drawable.ic_room_available));

						LinearLayout.LayoutParams indicatorParams = new LinearLayout.LayoutParams(dp2px(16), dp2px(16));
						indicatorParams.setMargins(dp2px(4), 0, 0, dp2px(4));
						indicatorParams.gravity = Gravity.BOTTOM | Gravity.START;

						indicator.setLayoutParams(indicatorParams);

						emptyItem.addView(indicator);
					}

					glTimetable.addView(emptyItem);
				}

				for (int i = 0; i < allItems.size(); i++) { // An item
					if (i >= 2)
						continue;

					final TimetableItemData item = allItems.get(i);

					if (item.isHidden())
						continue;

					@SuppressLint("InflateParams") View view = fragmentContext.get().inflater
							.inflate(R.layout.table_item, null, false);

					TextView tvC = view.findViewById(R.id.tvC);
					TextView tvTL = view.findViewById(R.id.tvTL);
					TextView tvBR = view.findViewById(R.id.tvBR);

					view.setPadding(itemPadding, itemPadding, itemPadding, itemPadding);

					if (boldTitle)
						((TextView) view.findViewById(R.id.tvC)).setTypeface(null, Typeface.BOLD);

					if (centeredInfo) {
						RelativeLayout.LayoutParams paramsTL = (RelativeLayout.LayoutParams) tvTL.getLayoutParams();
						RelativeLayout.LayoutParams paramsBR = (RelativeLayout.LayoutParams) tvBR.getLayoutParams();
						paramsTL.addRule(RelativeLayout.ALIGN_PARENT_LEFT, 0);
						paramsTL.addRule(RelativeLayout.CENTER_HORIZONTAL);
						paramsBR.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 0);
						paramsBR.addRule(RelativeLayout.CENTER_HORIZONTAL);
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
							paramsTL.addRule(RelativeLayout.ALIGN_PARENT_START, 0);
							paramsBR.addRule(RelativeLayout.ALIGN_PARENT_END, 0);
						}
						tvTL.setLayoutParams(paramsTL);
						tvBR.setLayoutParams(paramsBR);
					}

					if (fragmentContext.get().isAdded()) {
						tvC.setTextSize(nameFontSize);
						tvTL.setTextSize(infoFontSize);
						tvBR.setTextSize(infoFontSize);

						int textColor;
						if (lightText) {
							textColor = fragmentContext.get().getResources().getColor(android.R.color.primary_text_dark);
						} else {
							textColor = fragmentContext.get().getResources().getColor(android.R.color.primary_text_light);
						}

						tvC.setTextColor(textColor);
						tvTL.setTextColor(textColor);
						tvBR.setTextColor(textColor);
					}

					int rowSpan = determineHours(item, units);

					if (rowSpan > 1) {
						for (int j = 0; j < rowSpan - 1; j++) {
							timetable[0].addOffset(day, hour + j + 1);
							timetable[0].addDummyItem(day, hour + j + 1, item);
						}
					}

					while (hour + rowSpan < rows) {
						ArrayList<TimetableItemData> nextItems = (ArrayList<TimetableItemData>) timetable[0].getItems(day, hour + rowSpan);
						if (item.mergeWith(nextItems)) {
							rowSpan++;
							timetable[0].addOffset(day, hour + rowSpan - 1);
						} else {
							break;
						}
					}

					int colSpan = 2;

					for (int j = 0; j < rowSpan; j++) {
						ArrayList<TimetableItemData> nextItems = (ArrayList<TimetableItemData>) timetable[0].getItems(day, hour + j);
						if (nextItems.size() >= 2 || timetable[0].getOffset(day, hour) >= 1)
							colSpan = 1;
					}

					TimetableItemBackground vBackground = view.findViewById(R.id.vBackground);

					GridLayout.Spec rowSpec = GridLayout.spec(hour, rowSpan);
					GridLayout.Spec colSpec = GridLayout.spec(day * 2 + i + timetable[0].getOffset(day, hour), colSpan);
					GridLayout.LayoutParams params = new GridLayout.LayoutParams(rowSpec, colSpec);
					params.height = hourHeight * rowSpan;
					if (day * 2 == cols - 2)
						params.width = colSpan * lastDayWidth / 2; // fill up all the remaining space to prevent a gap
					else
						params.width = colSpan * dayWidth / 2;

					view.setLayoutParams(params);

					if (context == null)
						return null;

					if (!getPrefBool(context, prefs, "preference_use_default_background")) {
						if (item.getCodes().contains(Constants.TimetableItem.CODE_EXAM))
							vBackground.setBottomColor(colorExam);
						else if (item.getCodes().contains(Constants.TimetableItem.CODE_IRREGULAR))
							vBackground.setBottomColor(colorIrregular);
						else if (item.getCodes().contains(Constants.TimetableItem.CODE_CANCELLED))
							vBackground.setBottomColor(colorCancelled);
						else
							vBackground.setBottomColor(colorRegular);
					} else {
						vBackground.setBottomColor(item.getBackColor());
					}

					if (!getPrefBool(context, prefs, "preference_use_default_background")) {
						if (item.getCodes().contains(Constants.TimetableItem.CODE_EXAM))
							vBackground.setTopColor(colorExamPast);
						else if (item.getCodes().contains(Constants.TimetableItem.CODE_IRREGULAR))
							vBackground.setTopColor(colorIrregularPast);
						else if (item.getCodes().contains(Constants.TimetableItem.CODE_CANCELLED))
							vBackground.setTopColor(colorCancelledPast);
						else
							vBackground.setTopColor(colorRegularPast);
					} else {
						vBackground.setTopColor(manipulateColor(item.getBackColor()));
					}

					vBackground.setDividerColor(getPrefInt(context, prefs, "preference_marker"));

					vBackground.setCornerRadius(cornerRadius);

					int height = getHeightPx(hourHeight * rowSpan,
							item.getStartDateTime(),
							item.getEndDateTime());

					vBackground.setDividerPosition(height);

					if (i >= 1 && allItems.size() >= 3)
						vBackground.setIndicatorColor(defaultBackgroundColor);

					setupItemText(view, item);

					view.setOnClickListener(view1 -> fragmentContext.get().showDetails(allItems));

					try {
						glTimetable.addView(view);
					} catch (IllegalArgumentException e) {
						e.printStackTrace();
						Log.e("FragmentTimetable",
								"View out of bounds at: Column: " + day +
										", Row: " + hour +
										", ColSpan: " + colSpan +
										", RowSpan: " + rowSpan +
										", Lesson: " + item.getSubjects(fragmentContext.get().userDataList).getName(true));
					}
				}
			}
		}
		return glTimetable;
	}

	private int determineHours(TimetableItemData item, ArrayList<TimegridUnitManager.UnitData> units) {
		if (item.isDummy())
			return 1;

		int startHour = 0, endHour = 0;
		int itemStart = DateOperations.getComparableTime(item.getStartDateTime());
		int itemEnd = DateOperations.getComparableTime(item.getEndDateTime());

		for (int i = 0; i < units.size(); i++) {
			int unitStart = DateOperations.getComparableTime(units.get(i).getStartTime());
			int unitEnd = DateOperations.getComparableTime(units.get(i).getEndTime());

			if (itemStart >= unitStart)
				startHour = i;
			if (itemEnd >= unitEnd)
				endHour = i;
			else
				break;
		}

		return endHour + 1 - startHour;
	}

	@Override
	protected void onPostExecute(GridLayout glTimetable) {
		if (glTimetable == null)
			return;

		((ViewGroup) fragmentContext.get().rootView.findViewById(R.id.rlRoot)).addView(glTimetable, 0,
				new ViewGroup.LayoutParams(
						ViewGroup.LayoutParams.MATCH_PARENT,
						ViewGroup.LayoutParams.MATCH_PARENT));

		if (fragmentContext.get().isCurrentWeek()) {
			fragmentContext.get().main.stopRefreshing();
			fragmentContext.get().main.setLastRefresh(fragmentContext.get().lastRefresh);
		}
		fragmentContext.get().pbLoading.setVisibility(View.GONE);
	}

	private boolean shouldColorizeCell(int day, int hour, boolean alternatingDays, boolean alternatingHours) {
		if (alternatingDays && alternatingHours)
			return day % 2 == 0 ^ hour % 2 == 0;
		else if (alternatingDays)
			return day % 2 == 0;
		else
			return alternatingHours && hour % 2 == 0;
	}

	private void setupItemText(View view, TimetableItemData item) {
		if (item.isHidden()) {
			((TextView) view.findViewById(R.id.tvTL))
					.setText(item.getHolidays(fragmentContext.get().userDataList)
							.getLongName(ElementName.SHORT));
		} else {
			if (SessionInfo.getElemTypeId(fragmentContext.get().main.sessionInfo.getElemType())
					== TEACHER)
				((TextView) view.findViewById(R.id.tvTL))
						.setText(item.getClasses(fragmentContext.get().userDataList)
								.getName(ElementName.SHORT));
			else
				((TextView) view.findViewById(R.id.tvTL))
						.setText(item.getTeachers(fragmentContext.get().userDataList)
								.getName(ElementName.SHORT));

			if (SessionInfo.getElemTypeId(fragmentContext.get().main.sessionInfo.getElemType())
					== ROOM)
				((TextView) view.findViewById(R.id.tvBR))
						.setText(item.getClasses(fragmentContext.get().userDataList)
								.getName(ElementName.SHORT));
			else
				((TextView) view.findViewById(R.id.tvBR))
						.setText(item.getRooms(fragmentContext.get().userDataList)
								.getName(ElementName.SHORT));

			((TextView) view.findViewById(R.id.tvC))
					.setText(item.getSubjects(fragmentContext.get().userDataList)
							.getName(ElementName.SHORT));
		}
	}

	private int getHeightPx(int elementHeightPx, String startDateTime, String endDateTime) {
		if (startDateTime == null || endDateTime == null)
			return 0;
		Calendar cNow = Calendar.getInstance();

		Calendar cStart = Calendar.getInstance();
		Calendar cEnd = Calendar.getInstance();

		try {
			SimpleDateFormat sourceFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.ENGLISH);
			cStart.setTime(sourceFormat.parse(startDateTime));
			cEnd.setTime(sourceFormat.parse(endDateTime));
		} catch (ParseException e) {
			e.printStackTrace();
			return 0;
		}

		if (cStart.getTimeInMillis() < cNow.getTimeInMillis()
				&& cNow.getTimeInMillis() < cEnd.getTimeInMillis()) {
			long diff = cEnd.getTimeInMillis() - cStart.getTimeInMillis();
			long offset = cNow.getTimeInMillis() - cStart.getTimeInMillis();

			float multiplier = (offset * 1.0f) / (diff * 1.0f);
			return (int) (elementHeightPx - elementHeightPx * multiplier);
		} else if (cStart.getTimeInMillis() < cNow.getTimeInMillis()
				&& cEnd.getTimeInMillis() < cNow.getTimeInMillis())
			return 0;
		else
			return elementHeightPx;
	}

	private boolean shouldShowIndicatorForHour(SharedPreferences prefs, int currentHourIndex, int lastHourIndex) {
		return fragmentContext.get().getActivity() != null
				&& !TextUtils.isEmpty(prefs.getString("preference_room_to_display_in_free_lessons", null))
				&& ActivityRoomFinder.getRooms(fragmentContext.get().getActivity(), false).contains(prefs.getString("preference_room_to_display_in_free_lessons", null))
				&& (!prefs.getBoolean("preference_room_to_display_in_free_lessons_trim", false)
				|| currentHourIndex < lastHourIndex);
	}
}
