package com.perflyst.untis.fragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.view.ContextThemeWrapper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.perflyst.untis.R;
import com.perflyst.untis.activity.ActivityMain;
import com.perflyst.untis.dialog.DialogItemDetailsFragment;
import com.perflyst.untis.utils.Constants;
import com.perflyst.untis.utils.DateOperations;
import com.perflyst.untis.utils.ElementName;
import com.perflyst.untis.utils.ListManager;
import com.perflyst.untis.utils.SessionInfo;
import com.perflyst.untis.utils.connectivity.UntisRequest;
import com.perflyst.untis.utils.timetable.TimegridUnitManager;
import com.perflyst.untis.utils.timetable.Timetable;
import com.perflyst.untis.utils.timetable.TimetableItemData;
import com.perflyst.untis.utils.timetable.TimetableSetup;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.Objects;

import static com.perflyst.untis.utils.Conversions.setScale;
import static com.perflyst.untis.utils.DateOperations.addDaysToInt;
import static com.perflyst.untis.utils.PreferenceUtils.getPrefBool;
import static com.perflyst.untis.utils.connectivity.UntisAuthentication.getAuthObject;

public class FragmentTimetable extends Fragment {
	public ListManager listManager;
	public ActivityMain main;
	public ProgressBar pbLoading;
	public JSONObject userDataList;
	public LayoutInflater inflater;
	public int startDateFromWeek;
	public ViewGroup rootView;
	public long lastRefresh = -1;
	private int startDateOffset;
	private DialogItemDetailsFragment itemDetailsDialog;

	public FragmentTimetable() {
	}

	public static boolean isBetween(int date, int startDate, int endDate) {
		return (date >= startDate && date <= endDate);
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		@SuppressLint("RestrictedApi") final Context contextThemeWrapper =
				new ContextThemeWrapper(getActivity(), Objects.requireNonNull(getActivity()).getTheme());
		this.inflater = inflater.cloneInContext(contextThemeWrapper);

		setScale(Objects.requireNonNull(getContext()));

		startDateOffset = (getArguments() != null ? getArguments().getInt("position") : 0) - 50;
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
		SharedPreferences loginDataPrefs = main.getLoginData();
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(main);

		if (main.sessionInfo.getElemId() == -1
				&& main.sessionInfo.getElemType().equals("")) {
			main.sessionInfo.setDataFromJsonObject(ListManager.getUserData(listManager).optJSONObject("userData"));
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

		startDateFromWeek = Integer.parseInt(new SimpleDateFormat("yyyyMMdd", Locale.ENGLISH)
				.format(DateOperations.getStartDateFromWeek(Calendar.getInstance(), startDateOffset * 7).getTime()));

		UntisRequest api = new UntisRequest(main, main.sessionInfo, startDateFromWeek);

		final FragmentTimetable context = this;
		UntisRequest.ResponseHandler handler = response -> {
			if (response == null) {
				Log.w("FragmentTimetable", "response is null");
				// TODO: Stop loading and show "unknown error: null";
				return;
			}
			try {
				if (response.has("error")) {
					if (Math.abs(context.startDateOffset + 50 - context.main.currentViewPos) == 0) {
						if (context.getView() != null)
							Snackbar.make(context.getView(),
									context.getString(R.string.snackbar_error,
											response.getJSONObject("error")
													.getString("message")),
									Snackbar.LENGTH_LONG)
									.setAction("OK", null).show();
						Log.w("error", response.toString());
						context.pbLoading.setVisibility(View.GONE);
						context.main.swipeRefresh.setRefreshing(false);
					}
				} else if (response.has("result")) {
					if (response.has("timeModified"))
						context.setLastRefresh(response.getLong("timeModified"));
					context.setTimetableData(response.getJSONObject("result"));
					int days = 4; // TODO: Move the day determination part to another dedicated class
					try {
						if (userDataList == null)
							userDataList = ListManager.getUserData(listManager);
						days = userDataList.getJSONObject("masterData").getJSONObject("timeGrid").getJSONArray("days").length();
					} catch (JSONException e) {
						e.printStackTrace();
					}
					String fileName = context.main.sessionInfo.getElemType() + "-"
							+ context.main.sessionInfo.getElemId() + "-"
							+ context.startDateFromWeek + "-"
							+ addDaysToInt(context.startDateFromWeek, days - 1);
					context.listManager.saveList(fileName, response.toString(), true);
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		};

		UntisRequest.UntisRequestQuery query = new UntisRequest.UntisRequestQuery();
		query.setMethod(Constants.UntisAPI.METHOD_GET_TIMETABLE);
		query.setUrl(loginDataPrefs.getString("url", null));
		query.setSchool(loginDataPrefs.getString("school", null));

		JSONArray days = null;
		try {
			days = new JSONObject(new ListManager(getContext())
					.readList("userData", false))
					.getJSONObject("masterData")
					.getJSONObject("timeGrid")
					.getJSONArray("days");
		} catch (JSONException e) {
			e.printStackTrace();
		}

		TimegridUnitManager unitManager = new TimegridUnitManager(days);

		JSONObject params = new JSONObject();
		try {
			params
					.put("id", main.sessionInfo.getElemId())
					.put("type", main.sessionInfo.getElemType())
					.put("startDate", startDateFromWeek)
					.put("endDate", addDaysToInt(startDateFromWeek, unitManager.getNumberOfDays() - 1))
					.put("masterDataTimestamp", System.currentTimeMillis())
					.put("auth", getAuthObject(loginDataPrefs.getString("user", ""), loginDataPrefs.getString("key", "")));
		} catch (JSONException e) {
			e.printStackTrace(); // TODO: Implement proper error handling (search for possible cases first)
		}
		query.setParams(new JSONArray().put(params));

		if (getPrefBool(context.getContext(), prefs, "preference_timetable_refresh_in_background"))
			api.setCachingMode(UntisRequest.CachingMode.RETURN_CACHE_LOAD_LIVE_RETURN_LIVE);
		else
			api.setCachingMode(UntisRequest.CachingMode.RETURN_CACHE);
		api.setResponseHandler(handler).submit(query);
	}

	private void setTimetableData(JSONObject data) {
		try {
			if (getContext() != null) {
				Timetable timetable = new Timetable(data, PreferenceManager.getDefaultSharedPreferences(getContext()));
				new TimetableSetup(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, timetable);
			}
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			// TODO: Stop reloading, show error and report the response to the server
		}
	}

	public void showDetails(ArrayList<TimetableItemData> timetableItemData) {
		itemDetailsDialog = new DialogItemDetailsFragment();
		itemDetailsDialog.show(getChildFragmentManager(), "dialog_timetable_item_details");
		itemDetailsDialog.setFragment(this);
		itemDetailsDialog.setMainActivity(main);
		itemDetailsDialog.setItems(timetableItemData);
	}

	void setTarget(int id, ElementName.ElementType type) throws JSONException {
		final ElementName elementName = new ElementName(type, userDataList);
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

	public boolean isCurrentWeek() {
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
}