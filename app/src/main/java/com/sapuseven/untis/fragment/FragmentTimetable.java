package com.sapuseven.untis.fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.view.ContextThemeWrapper;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.sapuseven.untis.R;
import com.sapuseven.untis.activity.ActivityMain;
import com.sapuseven.untis.activity.ActivityRoomFinder;
import com.sapuseven.untis.dialog.DialogItemDetailsFragment;
import com.sapuseven.untis.utils.DateOperations;
import com.sapuseven.untis.utils.ElementName;
import com.sapuseven.untis.utils.ListManager;
import com.sapuseven.untis.utils.SessionInfo;
import com.sapuseven.untis.utils.Timetable;
import com.sapuseven.untis.utils.TimetableItemData;
import com.sapuseven.untis.view.TimetableItemBackground;
import com.sapuseven.untis.view.VerticalTextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import static com.sapuseven.untis.utils.Authentication.getAuthElement;
import static com.sapuseven.untis.utils.Constants.TimetableItem.CODE_CANCELLED;
import static com.sapuseven.untis.utils.Constants.TimetableItem.CODE_EXAM;
import static com.sapuseven.untis.utils.Constants.TimetableItem.CODE_IRREGULAR;
import static com.sapuseven.untis.utils.Conversions.dp2px;
import static com.sapuseven.untis.utils.Conversions.setScale;
import static com.sapuseven.untis.utils.DateOperations.addDaysToInt;
import static com.sapuseven.untis.utils.ElementName.CLASS;
import static com.sapuseven.untis.utils.ElementName.ROOM;
import static com.sapuseven.untis.utils.ElementName.TEACHER;
import static com.sapuseven.untis.utils.PreferenceUtils.getPrefBool;
import static com.sapuseven.untis.utils.PreferenceUtils.getPrefInt;

public class FragmentTimetable extends Fragment {
	public static final String ID_GET_TIMETABLE = "2";
	private int startDateOffset;
	private ListManager listManager;
	private ActivityMain main;
	private ProgressBar pbLoading;
	private JSONObject userDataList;
	private LayoutInflater inflater;
	private int startDateFromWeek;
	private ViewGroup rootView;
	private DialogItemDetailsFragment itemDetailsDialog;
	private long lastRefresh = -1;

	public FragmentTimetable() {
	}

	private static int manipulateColor(int color) {
		int a = Color.alpha(color);
		int r = Math.round(Color.red(color) * TableSetup.DARKNESS_FACTOR);
		int g = Math.round(Color.green(color) * TableSetup.DARKNESS_FACTOR);
		int b = Math.round(Color.blue(color) * TableSetup.DARKNESS_FACTOR);
		return Color.argb(a,
				Math.min(r, 255),
				Math.min(g, 255),
				Math.min(b, 255));
	}

	private static boolean isBetween(int date, int startDate, int endDate) {
		return (date >= startDate && date <= endDate);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		@SuppressLint("RestrictedApi") final Context contextThemeWrapper =
				new ContextThemeWrapper(getActivity(), getActivity().getTheme());
		this.inflater = inflater.cloneInContext(contextThemeWrapper);

		setScale(getContext());

		startDateOffset = getArguments().getInt("position") - 50;
		rootView = (ViewGroup) this.inflater.inflate(R.layout.content_timetable, container, false);
		main = ((ActivityMain) getActivity());
		if (main.sessionInfo == null)
			main.sessionInfo = new SessionInfo();
		listManager = new ListManager(main);
		pbLoading = main.findViewById(R.id.pbLoading);

		if (!main.swipeRefresh.isRefreshing())
			pbLoading.setVisibility(View.VISIBLE);
		if (Math.abs(startDateOffset + 50 - main.currentViewPos) < 2)
			requestTimetable();

		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
		if (sharedPrefs.getBoolean("preference_dark_theme_amoled", false)
				&& sharedPrefs.getBoolean("preference_dark_theme", false))
			rootView.setBackgroundColor(Color.BLACK);

		return rootView;
	}

	private void requestTimetable() {
		SharedPreferences prefs = main.getLoginData();
		try {
			if (main.sessionInfo.getElemId() == -1
					&& main.sessionInfo.getElemType().equals("")) {
				main.sessionInfo.setDataFromJsonObject(new JSONObject(listManager.readList("userData", false)).optJSONObject("userData"));
				if (main.getSupportActionBar() != null)
					main.getSupportActionBar().setTitle(main.sessionInfo.getDisplayName());
				switch (main.sessionInfo.getElemType()) {
					case "CLASS":
						((NavigationView) main.findViewById(R.id.nav_view)).setCheckedItem(R.id.nav_show_classes);
						break;
					case "TEACHER":
						((NavigationView) main.findViewById(R.id.nav_view)).setCheckedItem(R.id.nav_show_teachers);
						break;
					case "ROOM":
						((NavigationView) main.findViewById(R.id.nav_view)).setCheckedItem(R.id.nav_show_rooms);
						break;
					default:
						((NavigationView) main.findViewById(R.id.nav_view)).setCheckedItem(R.id.nav_show_personal);
						break;
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}

		startDateFromWeek = Integer.parseInt(new SimpleDateFormat("yyyyMMdd", Locale.US)
				.format(DateOperations.getStartDateFromWeek(Calendar.getInstance(), startDateOffset * 7).getTime()));

		Request request = new Request(this, prefs.getString("url", null));
		request.setSchool(prefs.getString("school", null));
		request.setParams("[{\"id\":\"" + main.sessionInfo.getElemId() + "\"," +
				"\"type\":\"" + main.sessionInfo.getElemType() + "\"," +
				"\"startDate\":" + startDateFromWeek + "," +
				"\"endDate\":" + addDaysToInt(startDateFromWeek, 4) + "," +
				"\"masterDataTimestamp\":" + System.currentTimeMillis() + "," +
				getAuthElement(prefs.getString("user", ""),
						prefs.getString("key", "")) +
				"}]");
		request.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	private void setTimetableData(JSONObject data) {
		try {
			Timetable timetable = new Timetable(data, PreferenceManager.getDefaultSharedPreferences(getContext()));
			new TableSetup(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, timetable);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			// TODO: Stop reloading, show error and report the response to the server
		}
	}

	private void showDetails(ArrayList<TimetableItemData> timetableItemData) {
		itemDetailsDialog = new DialogItemDetailsFragment();
		itemDetailsDialog.show(getChildFragmentManager(), "dialog_timetable_item_details");
		itemDetailsDialog.setFragment(this);
		itemDetailsDialog.setMainActivity(main);
		itemDetailsDialog.setItems(timetableItemData);
	}

	void setTarget(int id, int type) {
		final ElementName elementName = new ElementName(type).setUserDataList(userDataList);
		main.sessionInfo.setElemId(id);
		main.sessionInfo.setElemType(SessionInfo.getElemTypeName(type));
		if (main.getSupportActionBar() != null)
			switch (type) {
				case CLASS:
					main.getSupportActionBar().setTitle(getString(R.string.title_class, elementName.findFieldByValue("id", id, "name")));
					((NavigationView) main.findViewById(R.id.nav_view)).setCheckedItem(R.id.nav_show_classes);
					break;
				case TEACHER:
					main.getSupportActionBar().setTitle(
							elementName.findFieldByValue("id", id, "firstName") + " "
									+ elementName.findFieldByValue("id", id, "lastName"));
					((NavigationView) main.findViewById(R.id.nav_view)).setCheckedItem(R.id.nav_show_teachers);
					break;
				case ROOM:
					main.getSupportActionBar().setTitle(getString(R.string.title_room, elementName.findFieldByValue("id", id, "name")));
					((NavigationView) main.findViewById(R.id.nav_view)).setCheckedItem(R.id.nav_show_rooms);
					break;
			}
		refresh();
	}

	private void refresh() {
		if (itemDetailsDialog.isVisible())
			itemDetailsDialog.dismiss();
		main.refresh();
	}

	private boolean isCurrentWeek() {
		return startDateOffset + 50 - main.currentViewPos == 0;
	}

	private void setLastRefresh(long time) {
		Log.d("FragmentTimetable", "LastRefresh change requested from " + lastRefresh + " to " + time + " for " + startDateFromWeek);
		lastRefresh = time;
	}

	@Override
	public void setUserVisibleHint(boolean isVisibleToUser) {
		super.setUserVisibleHint(isVisibleToUser);

		if (main != null && isVisibleToUser)
			main.setLastRefresh(lastRefresh);
	}

	private static class Request extends AsyncTask<Void, Void, String> {
		private static final String jsonrpc = "2.0";
		private final String method = "getTimetable2017";
		private final String id = ID_GET_TIMETABLE;
		private final FragmentTimetable context;
		private String url = "";
		private String params = "{}";
		private String school = "";

		Request(FragmentTimetable context, String url) {
			this.context = context;
			this.url = "https://" + url + "/WebUntis/jsonrpc_intern.do";
		}

		void setSchool(String school) {
			this.school = school;
		}

		void setParams(String params) {
			this.params = params;
		}

		@Override
		protected String doInBackground(Void... p1) {
			String fileName = context.main.sessionInfo.getElemType() + "-"
					+ context.main.sessionInfo.getElemId() + "-"
					+ context.startDateFromWeek + "-"
					+ addDaysToInt(context.startDateFromWeek, 4);
			if (context.listManager.exists(fileName, true))
				return context.listManager.readList(fileName, true);

			HttpURLConnection urlConnection = null;
			try {
				String url = this.url;
				if (this.school.length() > 0)
					url += "?school=" + this.school;
				urlConnection = (HttpURLConnection) new URL(url).openConnection();

				JSONObject jsonObject = new JSONObject();
				jsonObject.put("id", this.id);
				jsonObject.put("method", this.method);
				jsonObject.put("params", new JSONArray(this.params));
				jsonObject.put("jsonrpc", jsonrpc);

				urlConnection.setDoOutput(true);
				urlConnection.setRequestMethod("POST");
				urlConnection.setRequestProperty("Content-Type", "application/json");
				urlConnection.setRequestProperty("Accept", "application/json");
				urlConnection.connect();

				DataOutputStream wr = new DataOutputStream(urlConnection.getOutputStream());
				wr.writeBytes(jsonObject.toString());
				wr.flush();
				wr.close();

				int response = urlConnection.getResponseCode();

				if (response >= 200 && response <= 399) {
					JSONObject resultJson = new JSONObject(inputStreamToString(urlConnection.getInputStream()));
					resultJson.put("timeModified", System.currentTimeMillis());
					Log.e("FragmentTimetable", "New timeModified saved");
					return resultJson.toString();
				} else {
					return "{}";
				}
			} catch (Exception e) {
				return "{\"id\":\"" + this.id + "\",\"error\":{\"message\":\"" + e.getMessage()
						.replace("\"", "\\\"") + "\"}}";
			} finally {
				if (urlConnection != null)
					urlConnection.disconnect();
			}
		}

		@Override
		protected void onPostExecute(String result) {
			try {
				JSONObject jsonObj = new JSONObject(result);
				if (jsonObj.has("error")) {
					if (Math.abs(context.startDateOffset + 50 - context.main.currentViewPos) == 0) {
						if (context.getView() != null)
							Snackbar.make(context.getView(),
									context.getString(R.string.snackbar_error,
											jsonObj.getJSONObject("error")
													.getString("message")),
									Snackbar.LENGTH_LONG)
									.setAction("OK", null).show();
						Log.w("error", jsonObj.toString());
						context.pbLoading.setVisibility(View.GONE);
						context.main.swipeRefresh.setRefreshing(false);
					}
				} else if (jsonObj.has("result")) {
					if (jsonObj.has("timeModified"))
						context.setLastRefresh(jsonObj.getLong("timeModified"));
					context.setTimetableData(jsonObj.getJSONObject("result"));
					String fileName = context.main.sessionInfo.getElemType() + "-"
							+ context.main.sessionInfo.getElemId() + "-"
							+ context.startDateFromWeek + "-"
							+ addDaysToInt(context.startDateFromWeek, 4);
					context.listManager.saveList(fileName, jsonObj.toString(), true);
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}

		private String inputStreamToString(InputStream inputStream) throws IOException {
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
			String line;
			StringBuilder result = new StringBuilder();
			while ((line = bufferedReader.readLine()) != null)
				result.append(line);

			inputStream.close();
			return result.toString();
		}
	}

	private class TableSetup extends AsyncTask<Timetable, Void, Void> {
		private static final float DARKNESS_FACTOR = 0.8f;
		private final FragmentTimetable fragmentContext;
		int rows;
		int cols;
		private GridLayout glTimetable;

		TableSetup(FragmentTimetable context) {
			this.fragmentContext = context;
		}

		@SuppressLint("InflateParams")
		@Override
		protected Void doInBackground(Timetable... timetable) {
			Context context = this.fragmentContext.getContext();

			if (context == null)
				return null;

			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(main);

			rows = timetable[0].getHoursPerDay();
			cols = timetable[0].getNumberOfDays() * 2;

			glTimetable = new GridLayout(getContext());
			glTimetable.setColumnCount(cols);
			glTimetable.setRowCount(rows);
			glTimetable.setOrientation(GridLayout.HORIZONTAL);

			DisplayMetrics displayMetrics = new DisplayMetrics();

			((Activity) getContext()).getWindowManager()
					.getDefaultDisplay()
					.getMetrics(displayMetrics);

			int totalWidth = Math.round(displayMetrics.widthPixels
					- getResources().getDimension(R.dimen.left_sidebar_width));
			int dayWidth = totalWidth * 2 / cols;
			int lastDayWidth = totalWidth - (cols / 2 - 1) * dayWidth;
			int hourHeight = dp2px(60);

			boolean alternatingDays = getPrefBool(context, prefs, "preference_alternating_days");
			boolean alternatingHours = getPrefBool(context, prefs, "preference_alternating_hours");

			int alternativeBackgroundColor = Color.WHITE;
			if (isAdded())
				alternativeBackgroundColor = getResources().getInteger(R.integer.preference_alternating_color_default_light);
			if (getPrefBool(context, prefs, "preference_alternating_colors_use_custom"))
				alternativeBackgroundColor = getPrefInt(context, prefs, "preference_alternating_color");
			else if (prefs.getBoolean("preference_dark_theme", false) && isAdded())
				alternativeBackgroundColor = getResources().getInteger(R.integer.preference_alternating_color_default_dark);

			int defaultBackgroundColor = Color.WHITE;
			if (isAdded())
				defaultBackgroundColor = getResources().getColor(android.R.color.background_light);

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

			TypedArray a = main.obtainStyledAttributes(typedValue.data, new int[]{
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

			for (int day = 0; day < cols / 2; day++) {
				try {
					userDataList = new JSONObject(listManager.readList("userData", false));
					JSONArray holidays = userDataList.getJSONObject("masterData").getJSONArray("holidays");
					LinearLayout holidayItem = null;
					StringBuilder holidayLabelString = null;
					for (int i = 0; i < holidays.length(); i++) {
						if (isBetween(addDaysToInt(fragmentContext.startDateFromWeek, day),
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
								holidayLabelString.append(getString(R.string.holiday_label_separator)).append(holidays.getJSONObject(i).getString("longName"));
							}
						}
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
				} catch (JSONException e) {
					e.printStackTrace();
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
									ActivityRoomFinder.getRoomStates(getActivity(), prefs.getString("preference_room_to_display_in_free_lessons", null))
											.charAt(day * rows + hour) == '1' ?
											ContextCompat.getDrawable(getActivity(), R.drawable.ic_room_occupied) :
											ContextCompat.getDrawable(getActivity(), R.drawable.ic_room_available));

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

						View view = this.fragmentContext.inflater
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

						if (isAdded()) {
							tvC.setTextSize(nameFontSize);
							tvTL.setTextSize(infoFontSize);
							tvBR.setTextSize(infoFontSize);

							int textColor;
							if (lightText) {
								textColor = getResources().getColor(android.R.color.primary_text_dark);
							} else {
								textColor = getResources().getColor(android.R.color.primary_text_light);
							}

							tvC.setTextColor(textColor);
							tvTL.setTextColor(textColor);
							tvBR.setTextColor(textColor);
						}

						int rowSpan = 1;
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
							if (nextItems.size() >= 2)
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
							if (item.getCodes().contains(CODE_EXAM))
								vBackground.setBottomColor(colorExam);
							else if (item.getCodes().contains(CODE_IRREGULAR))
								vBackground.setBottomColor(colorIrregular);
							else if (item.getCodes().contains(CODE_CANCELLED))
								vBackground.setBottomColor(colorCancelled);
							else
								vBackground.setBottomColor(colorRegular);
						} else {
							vBackground.setBottomColor(item.getBackColor());
						}

						if (!getPrefBool(context, prefs, "preference_use_default_background")) {
							if (item.getCodes().contains(CODE_EXAM))
								vBackground.setTopColor(colorExamPast);
							else if (item.getCodes().contains(CODE_IRREGULAR))
								vBackground.setTopColor(colorIrregularPast);
							else if (item.getCodes().contains(CODE_CANCELLED))
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

						view.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View view) {
								fragmentContext.showDetails(allItems);
							}
						});

						try {
							glTimetable.addView(view);
						} catch (IllegalArgumentException e) {
							e.printStackTrace();
							Log.e("FragmentTimetable",
									"View out of bounds at: Column: " + day +
											", Row: " + hour +
											", ColSpan: " + colSpan +
											", RowSpan: " + rowSpan +
											", Lesson: " + item.getSubjects(userDataList).getName(true));
						}
					}
				}
			}
			return null;
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
						.setText(item.getHolidays(fragmentContext.userDataList)
								.getLongName(ElementName.SHORT));
			} else {
				if (SessionInfo.getElemTypeId(fragmentContext.main.sessionInfo.getElemType())
						== TEACHER)
					((TextView) view.findViewById(R.id.tvTL))
							.setText(item.getClasses(fragmentContext.userDataList)
									.getName(ElementName.SHORT));
				else
					((TextView) view.findViewById(R.id.tvTL))
							.setText(item.getTeachers(fragmentContext.userDataList)
									.getName(ElementName.SHORT));

				if (SessionInfo.getElemTypeId(fragmentContext.main.sessionInfo.getElemType())
						== ROOM)
					((TextView) view.findViewById(R.id.tvBR))
							.setText(item.getClasses(fragmentContext.userDataList)
									.getName(ElementName.SHORT));
				else
					((TextView) view.findViewById(R.id.tvBR))
							.setText(item.getRooms(fragmentContext.userDataList)
									.getName(ElementName.SHORT));

				((TextView) view.findViewById(R.id.tvC))
						.setText(item.getSubjects(fragmentContext.userDataList)
								.getName(ElementName.SHORT));
			}
		}

		@Override
		protected void onPostExecute(Void v) {
			if (glTimetable == null)
				return;

			((ViewGroup) rootView.findViewById(R.id.rlRoot)).addView(glTimetable, 0,
					new ViewGroup.LayoutParams(
							ViewGroup.LayoutParams.MATCH_PARENT,
							ViewGroup.LayoutParams.MATCH_PARENT));

			if (isCurrentWeek()) {
				fragmentContext.main.stopRefreshing();
				fragmentContext.main.setLastRefresh(lastRefresh);
			}
			fragmentContext.pbLoading.setVisibility(View.GONE);
		}

		private int getHeightPx(int elementHeightPx, String startDateTime, String endDateTime) {
			if (startDateTime == null || endDateTime == null)
				return 0;
			Calendar cNow = Calendar.getInstance();

			Calendar cStart = Calendar.getInstance();
			Calendar cEnd = Calendar.getInstance();

			try {
				SimpleDateFormat sourceFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US);
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
			return getActivity() != null
					&& !TextUtils.isEmpty(prefs.getString("preference_room_to_display_in_free_lessons", null))
					&& ActivityRoomFinder.getRooms(getActivity(), false).contains(prefs.getString("preference_room_to_display_in_free_lessons", null))
					&& (!prefs.getBoolean("preference_room_to_display_in_free_lessons_trim", false)
					|| currentHourIndex < lastHourIndex);
		}
	}
}