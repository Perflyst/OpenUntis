package com.sapuseven.untis.fragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.ContextThemeWrapper;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.sapuseven.untis.R;
import com.sapuseven.untis.activity.ActivityMain;
import com.sapuseven.untis.activity.ActivityRoomFinder;
import com.sapuseven.untis.utils.DateOperations;
import com.sapuseven.untis.utils.ElementName;
import com.sapuseven.untis.utils.ListManager;
import com.sapuseven.untis.utils.SessionInfo;
import com.sapuseven.untis.utils.TimegridUnitManager;
import com.sapuseven.untis.utils.Timetable;
import com.sapuseven.untis.utils.TimetableItemData;

import org.jetbrains.annotations.Contract;
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
import static com.sapuseven.untis.utils.Constants.TimetableItem.CODE_REGULAR;
import static com.sapuseven.untis.utils.DateOperations.addDaysToInt;
import static com.sapuseven.untis.utils.ElementName.CLASS;
import static com.sapuseven.untis.utils.ElementName.ROOM;
import static com.sapuseven.untis.utils.ElementName.TEACHER;

public class FragmentTimetable extends Fragment {
	public static final String ID_GET_TIMETABLE = "2";
	private int startDateOffset;
	private ListManager listManager;
	private GridLayout glTimetable;
	private ActivityMain main;
	private float scale;
	private TimetableItemData[][] result;
	private AlertDialog dialog;
	private ProgressBar pbLoading;
	private JSONObject userDataList;
	private LayoutInflater inflater;
	private int startDateFromWeek;

	public FragmentTimetable() {
	}

	private static int manipulateColor(int color) {
		int a = Color.alpha(color);
		int r = Math.round(Color.red(color) * setTable.DARKNESS_FACTOR);
		int g = Math.round(Color.green(color) * setTable.DARKNESS_FACTOR);
		int b = Math.round(Color.blue(color) * setTable.DARKNESS_FACTOR);
		return Color.argb(a,
				Math.min(r, 255),
				Math.min(g, 255),
				Math.min(b, 255));
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// noinspection RestrictedApi
		final Context contextThemeWrapper = new ContextThemeWrapper(getActivity(), getActivity().getTheme());
		this.inflater = inflater.cloneInContext(contextThemeWrapper);
		startDateOffset = getArguments().getInt("position") - 50;
		ViewGroup rootView = (ViewGroup) this.inflater.inflate(R.layout.content_timetable, container, false);
		main = ((ActivityMain) getActivity());
		scale = main.getResources().getDisplayMetrics().density;
		if (main.sessionInfo == null)
			main.sessionInfo = new SessionInfo();
		listManager = new ListManager(main);
		try {
			userDataList = new JSONObject(listManager.readList("userData", false));
		} catch (JSONException e) {
			e.printStackTrace();
		}
		dialog = new AlertDialog.Builder(getActivity()).create();
		glTimetable = (GridLayout) rootView.findViewById(R.id.glTimetable);
		pbLoading = (ProgressBar) main.findViewById(R.id.pbLoading);
		if (!main.swipeRefresh.isRefreshing())
			pbLoading.setVisibility(View.VISIBLE);
		if (Math.abs(startDateOffset + 50 - main.currentViewPos) < 2)
			requestTimetable();


		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
		if (sharedPrefs.getBoolean("preference_dark_theme_amoled", false) && sharedPrefs.getBoolean("preference_dark_theme", false))
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

		Request request = new Request(prefs.getString("url", null));
		request.setSchool(prefs.getString("school", null));
		request.setParams("[{\"id\":\"" + main.sessionInfo.getElemId() + "\"," +
				"\"type\":\"" + main.sessionInfo.getElemType() + "\"," +
				"\"startDate\":" + startDateFromWeek + "," +
				"\"endDate\":" + addDaysToInt(startDateFromWeek, 4) + "," +
				"\"masterDataTimestamp\":" + System.currentTimeMillis() + "," +
				getAuthElement(prefs.getString("user", ""), prefs.getString("key", "")) +
				"}]");
		request.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	private void setTimetableData(JSONObject data) {
		Timetable timetable = new Timetable();
		timetable.setFromJsonObject(data.optJSONObject("timetable"));
		new setTable().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, timetable);
	}

	private void showDetails(TimetableItemData timetableItemData) {
		final AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity());
		@SuppressLint("InflateParams") View view = inflater.inflate(R.layout.dialog_timetable_item_details, null);
		if (timetableItemData.getTeachers(userDataList).isEmpty())
			view.findViewById(R.id.llTeachers).setVisibility(View.GONE);
		if (timetableItemData.getClasses(userDataList).isEmpty())
			view.findViewById(R.id.llClasses).setVisibility(View.GONE);
		if (timetableItemData.getRooms(userDataList).isEmpty())
			view.findViewById(R.id.llRooms).setVisibility(View.GONE);
		if (timetableItemData.getInfo().isEmpty())
			view.findViewById(R.id.llInfo).setVisibility(View.GONE);

		if ((timetableItemData.getTeachers(userDataList).isEmpty()
				&& timetableItemData.getClasses(userDataList).isEmpty()
				&& timetableItemData.getRooms(userDataList).isEmpty())
				|| timetableItemData.isFree())
			return;

		int[] attrs = new int[]{android.R.attr.textColorPrimary};
		TypedArray ta = main.obtainStyledAttributes(attrs);
		int color = ta.getColor(0, 0);
		ta.recycle();

		((TextView) view.findViewById(R.id.tvInfo)).setText(timetableItemData.getInfo());

		for (final String s : timetableItemData.getTeachers(userDataList).getNames()) {
			final ElementName elementName = new ElementName(TEACHER).setUserDataList(userDataList);
			TextView tv = new TextView(main.getApplicationContext());
			LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
			params.setMargins(0, 0, dp2px(12), 0);
			tv.setText(s);
			tv.setLayoutParams(params);
			tv.setTextColor(color);
			tv.setGravity(Gravity.CENTER_VERTICAL);
			tv.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					if (elementName.findFieldByValue("name", s, "id") != null)
						setTarget((Integer) elementName.findFieldByValue("name", s, "id"), TEACHER);
				}
			});
			((LinearLayout) view.findViewById(R.id.llTeacherList)).addView(tv);
		}

		for (final String s : timetableItemData.getClasses(userDataList).getNames()) {
			final ElementName elementName = new ElementName(CLASS).setUserDataList(userDataList);
			TextView tv = new TextView(main.getApplicationContext());
			LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
			params.setMargins(0, 0, dp2px(12), 0);
			tv.setText(s);
			tv.setLayoutParams(params);
			tv.setTextColor(color);
			tv.setGravity(Gravity.CENTER_VERTICAL);
			tv.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					if (elementName.findFieldByValue("name", s, "id") != null)
						setTarget((Integer) elementName.findFieldByValue("name", s, "id"), CLASS);
				}
			});
			((LinearLayout) view.findViewById(R.id.llClassList)).addView(tv);
		}

		for (final String s : timetableItemData.getRooms(userDataList).getNames()) {
			final ElementName elementName = new ElementName(ROOM).setUserDataList(userDataList);
			TextView tv = new TextView(main.getApplicationContext());
			LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
			params.setMargins(0, 0, dp2px(12), 0);
			tv.setText(s);
			tv.setLayoutParams(params);
			tv.setTextColor(color);
			tv.setGravity(Gravity.CENTER_VERTICAL);
			tv.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					if (elementName.findFieldByValue("name", s, "id") != null)
						setTarget((Integer) elementName.findFieldByValue("name", s, "id"), ROOM);
				}
			});
			((LinearLayout) view.findViewById(R.id.llRoomList)).addView(tv);
		}

		if (timetableItemData.getSubjects(userDataList).getLongNames().size() > 0) {
			StringBuilder titleBuilder = new StringBuilder(timetableItemData.getSubjects(userDataList).getLongNames().get(0));
			for (int i = 1; i < timetableItemData.getSubjects(userDataList).getLongNames().size(); i++) {
				titleBuilder.append(", ").append(timetableItemData.getSubjects(userDataList).getLongNames().get(i));
			}
			String title = titleBuilder.toString();
			if (timetableItemData.getCodes().contains(CODE_CANCELLED))
				title = getString(R.string.lesson_cancelled, title);
			if (timetableItemData.getCodes().contains(CODE_IRREGULAR))
				title = getString(R.string.lesson_irregular, title);
			if (timetableItemData.getCodes().contains(CODE_EXAM))
				title = getString(R.string.lesson_test, title);
			dialog.setTitle(title);
		}

		dialog.setView(view);
		this.dialog = dialog.create();
		this.dialog.setCanceledOnTouchOutside(true);
		this.dialog.show();
	}

	private void setTarget(int id, int type) {
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
					main.getSupportActionBar().setTitle(elementName.findFieldByValue("id", id, "firstName") + " " + elementName.findFieldByValue("id", id, "lastName"));
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
		if (dialog.isShowing())
			dialog.dismiss();
		main.refresh();
	}

	@Contract(pure = true)
	private int dp2px(int dp) {
		return (int) (dp * scale + 0.5f);
	}

	@Contract(pure = true)
	private boolean isBetween(int date, int startDate, int endDate) {
		return (date >= startDate && date <= endDate);
	}

	@SuppressWarnings("SameParameterValue")
	@NonNull
	private String fixedLength(String str, int length, char fillChar) {
		StringBuilder sb = new StringBuilder();
		while (sb.length() + str.length() < length) {
			sb.append(fillChar);
		}
		sb.append(str);
		return sb.toString();
	}

	private String getStringDateFromInt(int i) {
		String str = Integer.toString(i);
		return str.substring(0, 4) + "-" + str.substring(4, 6) + "-" + str.substring(6, 8);
	}

	private class setTable extends AsyncTask<Timetable, Void, Void> {
		private static final float DARKNESS_FACTOR = 0.8f;
		TimegridUnitManager unitManager;

		int dimY;
		int dimX;
		View[][] views;

		@SuppressLint("InflateParams")
		@Override
		protected Void doInBackground(Timetable... timetable) {
			timetable[0].prepareData(userDataList);
			ArrayList<TimetableItemData> timetableData = timetable[0].getData();
			JSONArray holidays = new JSONArray();
			try {
				unitManager = new TimegridUnitManager();
				unitManager.setList(new JSONObject(listManager.readList("userData", false)).getJSONObject("masterData").getJSONObject("timeGrid").getJSONArray("days"));
				holidays = new JSONObject(listManager.readList("userData", false)).getJSONObject("masterData").getJSONArray("holidays");
			} catch (JSONException e) {
				e.printStackTrace();
			}
			dimY = unitManager.getUnitCount();
			dimX = unitManager.getNumberOfDays();
			result = new TimetableItemData[dimY][dimX];
			views = new View[dimY][dimX];

			for (int x = 0; x < dimY; x++) {
				for (int y = 0; y < dimX; y++) {
					try {
						result[x][y] = new TimetableItemData();
						String startDateTime = getStringDateFromInt(addDaysToInt(startDateFromWeek, y)) + "T" + fixedLength(unitManager.getUnits().get(x).getDisplayStartTime(), 5, '0') + "Z";
						int index = timetable[0].indexOf(startDateTime, unitManager);
						if (index != -1) {
							result[x][y] = timetableData.get(index);
						} else {
							for (int i = 0; i < holidays.length(); i++) {
								if (isBetween(addDaysToInt(startDateFromWeek, y),
										Integer.parseInt(holidays.optJSONObject(i).optString("startDate").replace("-", "")),
										Integer.parseInt(holidays.optJSONObject(i).optString("endDate").replace("-", "")))) {
									TimetableItemData data = new TimetableItemData();
									data.addHoliday(holidays.optJSONObject(i).optInt("id"));
									data.setFree();
									result[x][y] = data;
									break;
								}
							}
						}

						if (result[x][y].isFree())
							views[x][y] = inflater.inflate(R.layout.table_item_free, null, false);
						else if (result[x][y].getCodes().contains(CODE_CANCELLED))
							views[x][y] = inflater.inflate(R.layout.table_item_cancelled, null, false);
						else if (result[x][y].getCodes().contains(CODE_IRREGULAR))
							views[x][y] = inflater.inflate(R.layout.table_item_irregular, null, false);
						else if (result[x][y].getCodes().contains(CODE_EXAM))
							views[x][y] = inflater.inflate(R.layout.table_item_exam, null, false);
						else
							views[x][y] = inflater.inflate(R.layout.table_item, null, false);
					} catch (IndexOutOfBoundsException e) {
						e.printStackTrace();
					}
				}
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void v) {
			SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(main.getApplicationContext());
			glTimetable.setColumnCount(dimX);
			glTimetable.setRowCount(dimY);
			for (int y = 0; y < dimX; y++) {
				for (int x = 0; x < dimY; x++) {
					int i = 1;
					while (x + i < dimY
							&& result[x][y].getTeachers(userDataList).getNames().equals(result[x + i][y].getTeachers(userDataList).getNames())
							&& result[x][y].getRooms(userDataList).getNames().equals(result[x + i][y].getRooms(userDataList).getNames())
							&& result[x][y].getSubjects(userDataList).getNames().equals(result[x + i][y].getSubjects(userDataList).getNames())
							&& result[x][y].getCodes().equals(result[x + i][y].getCodes())
							&& result[x][y].getInfo().equals(result[x + i][y].getInfo())
							&& (!result[x][y].isEmpty(userDataList)))
						i++;
					if (result[x + i - 1][y].getEndDateTime() != null)
						result[x][y].setEndDateTime(result[x + i - 1][y].getEndDateTime());
					GridLayout.Spec row = GridLayout.spec(x, i);
					GridLayout.Spec col = GridLayout.spec(y);
					GridLayout.LayoutParams params = new GridLayout.LayoutParams(row, col);
					params.height = dp2px(56) * i; // dp = height + divider!!
					if (y + 1 == dimX)
						params.width = glTimetable.getWidth() - (glTimetable.getWidth() / glTimetable.getColumnCount()) * y;
					else
						params.width = glTimetable.getWidth() / glTimetable.getColumnCount();
					views[x][y].setLayoutParams(params);
					views[x][y].setId(Integer.parseInt("1" + x + "2" + y));
					if (result[x][y].isFree()) {
						((TextView) views[x][y].findViewById(R.id.textView1)).setText(result[x][y].getHolidays(userDataList).getLongName(ElementName.SHORT));
					} else {
						if (SessionInfo.getElemTypeId(main.sessionInfo.getElemType()) == TEACHER)
							((TextView) views[x][y].findViewById(R.id.textView1)).setText(result[x][y].getClasses(userDataList).getName(ElementName.SHORT));
						else
							((TextView) views[x][y].findViewById(R.id.textView1)).setText(result[x][y].getTeachers(userDataList).getName(ElementName.SHORT));

						if (SessionInfo.getElemTypeId(main.sessionInfo.getElemType()) == ROOM)
							((TextView) views[x][y].findViewById(R.id.textView4)).setText(result[x][y].getClasses(userDataList).getName(ElementName.SHORT));
						else
							((TextView) views[x][y].findViewById(R.id.textView4)).setText(result[x][y].getRooms(userDataList).getName(ElementName.SHORT));

						((TextView) views[x][y].findViewById(R.id.textView5)).setText(result[x][y].getSubjects(userDataList).getName(ElementName.SHORT));
					}
					if (result[x][y].isEmpty(userDataList)) {
						if (showIndicatorForHour(sharedPrefs, x, y, dimY)) {
							views[x][y].findViewById(R.id.statusView).setVisibility(View.VISIBLE);

							((ImageView) views[x][y].findViewById(R.id.statusView)).setImageDrawable(
									ActivityRoomFinder.getRoomStates(getActivity(), sharedPrefs.getString("preference_room_to_display_in_free_lessons", null))
											.charAt(y * dimY + x) == '1' ?
											ContextCompat.getDrawable(getActivity(), R.drawable.ic_room_occupied) :
											ContextCompat.getDrawable(getActivity(), R.drawable.ic_room_available));
						} else {
							views[x][y].findViewById(R.id.vBackground).setVisibility(View.GONE);
						}
					}

					views[x][y].setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View view) {
							for (int x = 0; x < views.length; x++) {
								for (int y = 0; y < views[x].length; y++) {
									if (view.getId() == views[x][y].getId()) {
										showDetails(result[x][y]);
									}
								}
							}
						}
					});
					if (!result[x][y].isFree() && result[x][y].getCodes().contains(CODE_REGULAR)) {
						GradientDrawable bottomShape = new GradientDrawable();
						bottomShape.setCornerRadii(new float[]{dp2px(2), dp2px(2), dp2px(2), dp2px(2), dp2px(2), dp2px(2), dp2px(2), dp2px(2)});
						if (!sharedPrefs.getBoolean("preference_use_default_background", false)) {
							if (result[x][y].getCodes().contains("EXAM"))
								bottomShape.setColor(sharedPrefs.getInt("preference_background_exam", 0xFFFFFF22));
							else
								bottomShape.setColor(sharedPrefs.getInt("preference_background_regular", 0xFFE0E0E0));
						} else {
							bottomShape.setColor(result[x][y].getBackColor());
						}

						GradientDrawable topShape = new GradientDrawable();
						if (getHeightPx(dp2px(56) * i, result[x][y].getStartDateTime(), result[x][y].getEndDateTime()) > 2)
							topShape.setCornerRadii(new float[]{dp2px(2), dp2px(2), dp2px(2), dp2px(2), 0, 0, 0, 0});
						else
							topShape.setCornerRadii(new float[]{dp2px(2), dp2px(2), dp2px(2), dp2px(2), dp2px(2), dp2px(2), dp2px(2), dp2px(2)});
						if (!sharedPrefs.getBoolean("preference_use_default_background", false)) {
							if (result[x][y].getCodes().contains(CODE_EXAM))
								topShape.setColor(sharedPrefs.getInt("preference_background_exam_past", 0xFFEEEE00));
							else
								topShape.setColor(sharedPrefs.getInt("preference_background_regular_past", 0xFFBBBBBB));
						} else {
							topShape.setColor(manipulateColor(result[x][y].getBackColor()));
						}

						GradientDrawable divider = new GradientDrawable();
						if (getHeightPx(dp2px(56) * i, result[x][y].getStartDateTime(), result[x][y].getEndDateTime()) > 2)
							divider.setCornerRadii(new float[]{dp2px(3), dp2px(3), dp2px(3), dp2px(3), 0, 0, 0, 0});
						else
							divider.setCornerRadii(new float[]{dp2px(3), dp2px(3), dp2px(3), dp2px(3), dp2px(3), dp2px(3), dp2px(3), dp2px(3)});
						divider.setColor(sharedPrefs.getInt("preference_marker", 0xFFE00000));

						Drawable[] layers = {bottomShape, divider, topShape};
						LayerDrawable layerList = new LayerDrawable(layers);
						layerList.setLayerInset(0, 0, 0, 0, 0);
						layerList.setLayerInset(1, 0, 0, 0, max(getHeightPx(dp2px(56) * i, result[x][y].getStartDateTime(), result[x][y].getEndDateTime()) - dp2px(2)));
						layerList.setLayerInset(2, 0, 0, 0, getHeightPx(dp2px(56) * i, result[x][y].getStartDateTime(), result[x][y].getEndDateTime()));
						views[x][y].findViewById(R.id.vBackground).setBackground(layerList);
					} else if (result[x][y].isFree()) {
						views[x][y].findViewById(R.id.vBackground).setBackgroundColor(sharedPrefs.getInt("preference_background_free", 0xFFBBBBFF));
					} else if (!result[x][y].getCodes().contains("REGULAR") && !result[x][y].isFree() && result[x][y].getStartDateTime().length() > 0) {
						GradientDrawable bottomShape = new GradientDrawable();
						bottomShape.setCornerRadii(new float[]{dp2px(2), dp2px(2), dp2px(2), dp2px(2), dp2px(2), dp2px(2), dp2px(2), dp2px(2)});
						if (!sharedPrefs.getBoolean("preference_use_default_background", false)) {
							bottomShape.setColor(sharedPrefs.getInt("preference_background_irregular", 0xFFFFFFFF));
						} else {
							bottomShape.setColor(result[x][y].getBackColor());
						}

						GradientDrawable topShape = new GradientDrawable();
						if (getHeightPx(dp2px(56) * i, result[x][y].getStartDateTime(), result[x][y].getEndDateTime()) > 2)
							topShape.setCornerRadii(new float[]{dp2px(2), dp2px(2), dp2px(2), dp2px(2), 0, 0, 0, 0});
						else
							topShape.setCornerRadii(new float[]{dp2px(2), dp2px(2), dp2px(2), dp2px(2), dp2px(2), dp2px(2), dp2px(2), dp2px(2)});
						if (!sharedPrefs.getBoolean("preference_use_default_background", false)) {
							topShape.setColor(sharedPrefs.getInt("preference_background_irregular_past", 0xFFEEEEEE));
						} else {
							topShape.setColor(manipulateColor(result[x][y].getBackColor()));
						}

						GradientDrawable divider = new GradientDrawable();
						if (getHeightPx(dp2px(56) * i, result[x][y].getStartDateTime(), result[x][y].getEndDateTime()) > 2)
							divider.setCornerRadii(new float[]{dp2px(3), dp2px(3), dp2px(3), dp2px(3), 0, 0, 0, 0});
						else
							divider.setCornerRadii(new float[]{dp2px(3), dp2px(3), dp2px(3), dp2px(3), dp2px(3), dp2px(3), dp2px(3), dp2px(3)});
						divider.setColor(sharedPrefs.getInt("preference_marker", 0xFFE00000));

						Drawable[] layers = {bottomShape, divider, topShape};
						LayerDrawable layerList = new LayerDrawable(layers);
						layerList.setLayerInset(0, 0, 0, 0, 0);
						layerList.setLayerInset(1, 0, 0, 0, max(getHeightPx(dp2px(56) * i, result[x][y].getStartDateTime(), result[x][y].getEndDateTime()) - dp2px(2)));
						layerList.setLayerInset(2, 0, 0, 0, getHeightPx(dp2px(56) * i, result[x][y].getStartDateTime(), result[x][y].getEndDateTime()));
						views[x][y].findViewById(R.id.vBackground).setBackground(layerList);
					} else {
						boolean showDivider = getHeightPx(dp2px(56) * i, getStringDateFromInt(addDaysToInt(startDateFromWeek, y)) + "T" + unitManager.getUnits().get(x).getStartTime() + "Z", getStringDateFromInt(addDaysToInt(startDateFromWeek, y)) + "T" + unitManager.getUnits().get(x).getEndTime() + "Z") < dp2px(56);

						LayerDrawable layerList;
						if (showDivider) {
							GradientDrawable divider = new GradientDrawable();
							divider.setColor(sharedPrefs.getInt("preference_marker", 0xFFE00000));
							layerList = new LayerDrawable(new GradientDrawable[]{divider});
							layerList.setLayerInset(0,
									0,
									dp2px(56) * i - getHeightPx(dp2px(56) * i, getStringDateFromInt(addDaysToInt(startDateFromWeek, y)) + "T" +
											unitManager.getUnits().get(x).getStartTime() + "Z", getStringDateFromInt(addDaysToInt(startDateFromWeek, y)) + "T" +
											unitManager.getUnits().get(x).getEndTime() + "Z"),
									0,
									max(getHeightPx(dp2px(56) * i, getStringDateFromInt(addDaysToInt(startDateFromWeek, y)) + "T" +
											unitManager.getUnits().get(x).getStartTime() + "Z", getStringDateFromInt(addDaysToInt(startDateFromWeek, y)) + "T" +
											unitManager.getUnits().get(x).getEndTime() + "Z")) - dp2px(2));
						} else {
							layerList = new LayerDrawable(new GradientDrawable[]{});
						}
						views[x][y].setBackground(layerList);
					}

					glTimetable.addView(views[x][y], params);
					if (i > 1)
						x += i - 1;
				}
			}
			if (startDateOffset + 50 - main.currentViewPos == 0)
				main.stopRefreshing();
			pbLoading.setVisibility(View.GONE);
		}

		private boolean showIndicatorForHour(SharedPreferences sharedPrefs, int x, int y, int hoursPerDay) {
			if (getActivity() != null &&
					!TextUtils.isEmpty(sharedPrefs.getString("preference_room_to_display_in_free_lessons", null)) &&
					ActivityRoomFinder.getRooms(getActivity(), false)
							.contains(sharedPrefs.getString("preference_room_to_display_in_free_lessons", null))) {
				if (sharedPrefs.getBoolean("preference_room_to_display_in_free_lessons_trim", false)) {
					int length = hoursPerDay;
					while (length > 0) {
						if (result[length - 1][y].isEmpty(userDataList))
							length--;
						else
							break;
					}
					return x < length;
				} else
					return true;
			} else
				return false;
		}

		private int max(int value) {
			if (value < 0)
				return 0;
			return value;
		}

		private int getHeightPx(int elementHeightDp, String startDateTime, String endDateTime) {
			if (startDateTime == null || endDateTime == null)
				return 0;
			Calendar cNow = Calendar.getInstance();

			Calendar cStart = Calendar.getInstance();
			Calendar cEnd = Calendar.getInstance();

			try {
				cStart.setTime(DateOperations.parseFromISO(startDateTime));
				cEnd.setTime(DateOperations.parseFromISO(endDateTime));
			} catch (ParseException e) {
				e.printStackTrace();
			}

			if (cStart.getTimeInMillis() < cNow.getTimeInMillis() && cNow.getTimeInMillis() < cEnd.getTimeInMillis()) {
				long diff = cEnd.getTimeInMillis() - cStart.getTimeInMillis();
				long offset = cNow.getTimeInMillis() - cStart.getTimeInMillis();

				float multiplier = (offset * 1.0f) / (diff * 1.0f);
				return (int) (elementHeightDp - elementHeightDp * multiplier);
			} else if (cStart.getTimeInMillis() < cNow.getTimeInMillis() && cEnd.getTimeInMillis() < cNow.getTimeInMillis())
				return 0;
			else
				return elementHeightDp;
		}
	}

	private class Request extends AsyncTask<Void, Void, String> {
		private static final String jsonrpc = "2.0";
		private final String method = "getTimetable2017";
		private final String id = ID_GET_TIMETABLE;
		private String url = "";
		private String params = "{}";
		private String school = "";

		Request(String url) {
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
			String fileName = main.sessionInfo.getElemType() + "-" + main.sessionInfo.getElemId() + "-" + startDateFromWeek + "-" + addDaysToInt(startDateFromWeek, 4);
			if (listManager.exists(fileName, true))
				return listManager.readList(fileName, true);

			String result;
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
				if (response >= 200 && response <= 399)
					result = inputStreamToString(urlConnection.getInputStream());
				else
					result = "{}";
			} catch (Exception e) {
				result = "{\"id\":\"" + this.id + "\",\"error\":{\"message\":\"" + e.getMessage().replace("\"", "\\\"") + "\"}}";
			} finally {
				if (urlConnection != null)
					urlConnection.disconnect();
			}
			return result;
		}

		@Override
		protected void onPostExecute(String result) {
			try {
				JSONObject jsonObj = new JSONObject(result);
				if (jsonObj.has("error")) {
					if (Math.abs(startDateOffset + 50 - main.currentViewPos) == 0) {
						if (getView() != null)
							Snackbar.make(getView(), getString(R.string.snackbar_error, jsonObj.getJSONObject("error").getString("message")), Snackbar.LENGTH_LONG).setAction("OK", null).show();
						Log.w("error", jsonObj.toString());
						pbLoading.setVisibility(View.GONE);
						main.swipeRefresh.setRefreshing(false);
					}
				} else if (jsonObj.has("result")) {
					setTimetableData(jsonObj.getJSONObject("result"));
					String fileName = main.sessionInfo.getElemType() + "-" + main.sessionInfo.getElemId() + "-" + startDateFromWeek + "-" + addDaysToInt(startDateFromWeek, 4);
					listManager.saveList(fileName, result, true);
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
}