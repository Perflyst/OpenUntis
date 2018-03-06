package com.sapuseven.untis.activity;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.DialogFragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ShareEvent;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.sapuseven.untis.BuildConfig;
import com.sapuseven.untis.R;
import com.sapuseven.untis.adapter.AdapterGridView;
import com.sapuseven.untis.adapter.AdapterTimetable;
import com.sapuseven.untis.adapter.AdapterTimetableHeader;
import com.sapuseven.untis.fragment.FragmentDatePicker;
import com.sapuseven.untis.notification.StartupReceiver;
import com.sapuseven.untis.utils.ApiRequest;
import com.sapuseven.untis.utils.AutoUpdater;
import com.sapuseven.untis.utils.Conversions;
import com.sapuseven.untis.utils.DateOperations;
import com.sapuseven.untis.utils.ElementName;
import com.sapuseven.untis.utils.ListManager;
import com.sapuseven.untis.utils.SessionInfo;
import com.sapuseven.untis.utils.TimegridUnitManager;
import com.sapuseven.untis.utils.UserRegistration;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;

import io.fabric.sdk.android.Fabric;

import static com.sapuseven.untis.utils.Conversions.dp2px;
import static com.sapuseven.untis.utils.ElementName.CLASS;
import static com.sapuseven.untis.utils.ElementName.ROOM;
import static com.sapuseven.untis.utils.ElementName.TEACHER;
import static com.sapuseven.untis.utils.PreferenceUtils.getPrefBool;
import static com.sapuseven.untis.utils.PreferenceUtils.getPrefInt;
import static com.sapuseven.untis.utils.ThemeUtils.restartApplication;
import static com.sapuseven.untis.utils.ThemeUtils.setupTheme;

public class ActivityMain extends AppCompatActivity
		implements NavigationView.OnNavigationItemSelectedListener {
	private static final int REQUEST_CODE_ROOM_FINDER = 1;
	private static final long MINUTE_MILLIS = 60 * 1000;
	private static final long HOUR_MILLIS = 60 * 60 * 1000;
	private static final long DAY_MILLIS = 24 * 60 * 60 * 1000;
	private static final String CONTENT_ID_SHARE = "drawer-share";
	public SwipeRefreshLayout swipeRefresh;
	public SessionInfo sessionInfo;
	public int currentViewPos = 50;
	private TextView lastRefresh;
	private ViewPager mPagerHeader;
	private ViewPager mPagerTable;
	private ListManager mListManager;
	private AdapterTimetableHeader mPagerHeaderAdapter;
	private AdapterTimetable mPagerTableAdapter;
	private AlertDialog mDialog;
	private Calendar mLastCalendar;
	private JSONObject mUserDataList;
	private long mLastBackPress;
	private int mItemListMargins;
	private FirebaseRemoteConfig firebaseRemoteConfig;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Fabric.with(this, new Crashlytics());
		setupTheme(this, false);
		super.onCreate(savedInstanceState);

		Conversions.setScale(this);

		firebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
		firebaseRemoteConfig.setConfigSettings(new FirebaseRemoteConfigSettings.Builder().build());
		firebaseRemoteConfig.setDefaults(R.xml.remote_config_defaults);

		setupRemoteConfig();

		mItemListMargins = (int) (12 * getResources().getDisplayMetrics().density + 0.5f);

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		if (!checkLoginState()) {
			Intent i = new Intent(this, ActivityLogin.class);
			startActivity(i);
			finish();
		} else {
			if (BuildConfig.DEBUG)
				new UserRegistration().submit(this);

			Intent intent = new Intent(this, StartupReceiver.class);
			sendBroadcast(intent);

			setContentView(R.layout.activity_main);
			Toolbar toolbar = findViewById(R.id.toolbar);
			setSupportActionBar(toolbar);

			DrawerLayout drawer = findViewById(R.id.drawer_layout);
			ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
					this, drawer, toolbar, R.string.navigation_drawer_open,
					R.string.navigation_drawer_close);
			drawer.addDrawerListener(toggle);
			toggle.syncState();
			mListManager = new ListManager(getApplicationContext());
			mDialog = new AlertDialog.Builder(this).create();
			try {
				mUserDataList = new JSONObject(mListManager.readList("userData", false));

				logUser(mUserDataList.getJSONObject("userData").optInt("elemId", -1),
						mUserDataList.getJSONObject("userData")
								.optString("displayName", "BetterUntis"));
			} catch (JSONException e) {
				e.printStackTrace();
			}

			lastRefresh = findViewById(R.id.tvLastRefresh);
			lastRefresh.setText(getString(R.string.last_refreshed, getString(R.string.never)));

			swipeRefresh = findViewById(R.id.swipeRefresh);
			swipeRefresh.setOnRefreshListener(() -> {
				int startDate = Integer.parseInt(new SimpleDateFormat("yyyyMMdd", Locale.US)
						.format(DateOperations.getStartDateFromWeek(Calendar.getInstance(),
								(currentViewPos - 50) * 7).getTime()));
				mListManager.delete(sessionInfo.getElemType() + "-" + sessionInfo.getElemId() +
						"-" + startDate + "-" + addDaysToInt(startDate, 4), true);
				refresh();
			});

			Calendar c = Calendar.getInstance();
			if (c.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY
					|| (c.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
					&& c.getFirstDayOfWeek() == Calendar.MONDAY))
				currentViewPos++;

			mPagerHeader = findViewById(R.id.viewpagerHeader);
			mPagerHeaderAdapter = new AdapterTimetableHeader(getSupportFragmentManager());
			mPagerHeader.setAdapter(mPagerHeaderAdapter);

			mPagerTable = findViewById(R.id.viewpagerTimegrid);
			mPagerTableAdapter = new AdapterTimetable(getSupportFragmentManager());
			mPagerTable.setAdapter(mPagerTableAdapter);

			//noinspection AndroidLintClickableViewAccessibility
			mPagerTable.setOnTouchListener((v, event) -> {
				if (event.getAction() == MotionEvent.ACTION_MOVE
						&& !swipeRefresh.isRefreshing())
					swipeRefresh.setEnabled(false);
				else if (!swipeRefresh.isRefreshing())
					swipeRefresh.setEnabled(true);
				return false;
			});

			mPagerHeader.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {

				private int scrollState = ViewPager.SCROLL_STATE_IDLE;

				@Override
				public void onPageScrolled(final int position, final float positionOffset,
				                           final int positionOffsetPixels) {
					if (scrollState == ViewPager.SCROLL_STATE_IDLE) {
						return;
					}
					mPagerTable.scrollTo(mPagerHeader.getScrollX(), mPagerTable.getScrollY());
				}

				@Override
				public void onPageSelected(final int position) {
					currentViewPos = position;
				}

				@Override
				public void onPageScrollStateChanged(final int state) {
					scrollState = state;
					if (state == ViewPager.SCROLL_STATE_IDLE) {
						mPagerTable.setCurrentItem(mPagerHeader.getCurrentItem(), false);
					}
				}
			});

			mPagerTable.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {

				private int scrollState = ViewPager.SCROLL_STATE_IDLE;

				@Override
				public void onPageScrolled(final int position, final float positionOffset,
				                           final int positionOffsetPixels) {
					if (scrollState == ViewPager.SCROLL_STATE_IDLE) {
						return;
					}
					mPagerHeader.scrollTo(mPagerTable.getScrollX(), mPagerHeader.getScrollY());
				}

				@Override
				public void onPageSelected(final int position) {
					currentViewPos = position;
					//setLastRefresh(((FragmentTimetable) mPagerTableAdapter.getItem(position)).getLastRefresh());
				}

				@Override
				public void onPageScrollStateChanged(final int state) {
					scrollState = state;
					if (state == ViewPager.SCROLL_STATE_IDLE) {
						mPagerHeader.setCurrentItem(mPagerTable.getCurrentItem(), false);
					}
				}
			});

			mLastCalendar = Calendar.getInstance();

			ImageView ivSelectDate = findViewById(R.id.ivSelectDate);
			ivSelectDate.setOnClickListener(view -> {
				DialogFragment fragment = new FragmentDatePicker();
				Bundle args = new Bundle();
				args.putInt("year", mLastCalendar.get(Calendar.YEAR));
				args.putInt("month", mLastCalendar.get(Calendar.MONTH));
				args.putInt("day", mLastCalendar.get(Calendar.DAY_OF_MONTH));
				fragment.setArguments(args);
				fragment.show(getSupportFragmentManager(), "datePicker");
			});

			try {
				TimegridUnitManager unitManager = new TimegridUnitManager(
						mUserDataList.getJSONObject("masterData").getJSONObject("timeGrid")
								.getJSONArray("days"));
				ArrayList<TimegridUnitManager.UnitData> units = unitManager.getUnits();

				boolean alternatingHours = prefs.getBoolean("preference_alternating_hours", false);

				int alternativeBackgroundColor = getResources().getInteger(R.integer.preference_alternating_color_default_light);
				if (getPrefBool(this, prefs, "preference_alternating_colors_use_custom"))
					alternativeBackgroundColor = getPrefInt(this, prefs, "preference_alternating_color");
				else if (prefs.getBoolean("preference_dark_theme", false))
					alternativeBackgroundColor = getResources().getInteger(R.integer.preference_alternating_color_default_dark);

				for (int i = 0; i < units.size(); i++)
					addHour(units.get(i), i + 1, alternatingHours, alternativeBackgroundColor);
			} catch (JSONException e) {
				e.printStackTrace();
			}

			NavigationView navigationView = findViewById(R.id.nav_view);
			navigationView.setNavigationItemSelectedListener(this);
			navigationView.setCheckedItem(R.id.nav_show_personal);

			((TextView) navigationView.getHeaderView(0).findViewById(R.id.nav_drawer_header_line1))
					.setText(mUserDataList.optJSONObject("userData")
							.optString("displayName", getString(R.string.app_name)));
			((TextView) navigationView.getHeaderView(0).findViewById(R.id.nav_drawer_header_line2))
					.setText(mUserDataList.optJSONObject("userData")
							.optString("schoolName", getString(R.string.contact_email)));

			mPagerHeader.setCurrentItem(currentViewPos);
			mPagerTable.setCurrentItem(currentViewPos);

			checkVersion();
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
				checkForNewFeatures();

			final int oldVersion = prefs.getInt("last_version", 0);
			if (oldVersion < BuildConfig.VERSION_CODE) {
				/*
				RE-ENABLE FOR A 'NEW VERSION'-DIALOG

				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setMessage(getString(R.string.new_version_message))
						.setCancelable(false)
						.setNeutralButton(R.string.view_changelog,
						 new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialogInterface, int i) {
								dialogInterface.dismiss();
								new DisplayChangelog(ActivityMain.this)
								.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, oldVersion);
							}
						})
						.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface mDialog, int id) {
								mDialog.dismiss();
							}
						});
				AlertDialog alertDialog = builder.create();
				alertDialog.show();

				final Button button = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
				button.setEnabled(false);

				new Handler().postDelayed(new Runnable() {
					@Override
					public void run() {
						button.setEnabled(true);
					}
				}, 3000);*/

				SharedPreferences.Editor editor = prefs.edit();
				editor.putInt("last_version", BuildConfig.VERSION_CODE);
				editor.apply();
			}
			setupBackgroundColor();

			Intent appLinkIntent = getIntent();
			Uri appLinkData = appLinkIntent.getData();
			if (appLinkData != null && !TextUtils.isEmpty(appLinkData.getQuery())) {
				try {
					if (!TextUtils.isEmpty(appLinkData.getQueryParameter("room")))
						setTarget((int) new ElementName(ROOM).setUserDataList(mUserDataList)
										.findFieldByValue("name", appLinkData
												.getQueryParameter("room"), "id"), ROOM,
								getString(R.string.title_room, appLinkData.getQueryParameter("room")));
					else if (!TextUtils.isEmpty(appLinkData.getQueryParameter("teacher")))
						setTarget((int) new ElementName(TEACHER).setUserDataList(mUserDataList)
										.findFieldByValue("name", appLinkData
												.getQueryParameter("teacher"), "id"), TEACHER,
								getTeacherTitleByName(appLinkData.getQueryParameter("teacher")));
					else if (!TextUtils.isEmpty(appLinkData.getQueryParameter("class")))
						setTarget((int) new ElementName(CLASS).setUserDataList(mUserDataList)
										.findFieldByValue("name", appLinkData
												.getQueryParameter("class"), "id"), CLASS,
								getString(R.string.title_class,
										URLDecoder.decode(appLinkData.getQueryParameter("class"),
												"UTF-8")));
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				} catch (NoSuchElementException e) {
					e.printStackTrace();

					AlertDialog.Builder builder = new AlertDialog.Builder(this);

					builder.setTitle(R.string.error);
					builder.setMessage(R.string.error_item_not_found);
					builder.setNeutralButton(R.string.ok, (dialogInterface, i) -> dialogInterface.dismiss());

					mDialog = builder.create();
					mDialog.setCanceledOnTouchOutside(true);
					mDialog.show();
				}
				// TODO: Add parameter for date selection
				// TODO: Add parameter for school (for compatibility)
			}
		}
	}

	private void setupRemoteConfig() {
		firebaseRemoteConfig.fetch(3600)
				.addOnCompleteListener(this, task -> {
					if (task.isSuccessful())
						firebaseRemoteConfig.activateFetched();
				});
	}

	private String getTeacherTitleByName(String teacherName) {
		ElementName teacher = new ElementName(TEACHER).setUserDataList(mUserDataList);
		return getString(R.string.title_teacher,
				teacher.findFieldByValue("name", teacherName, "firstName"),
				teacher.findFieldByValue("name", teacherName, "lastName"));
	}

	private void logUser(int id, String name) {
		Crashlytics.setUserIdentifier(String.valueOf(id));
		Crashlytics.setUserName(name);
	}

	@Override
	protected void onResume() {
		super.onResume();
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

		if (sharedPrefs.getBoolean("restart", false)) {
			SharedPreferences.Editor editor = sharedPrefs.edit();
			editor.putBoolean("restart", false);
			editor.apply();
			restartApplication(this);
		}
	}

	public void goTo(Calendar c1) {
		c1.set(Calendar.HOUR_OF_DAY, 0);
		c1.set(Calendar.MINUTE, 0);
		c1.set(Calendar.SECOND, 0);
		c1.set(Calendar.MILLISECOND, 0);
		Calendar c2 = Calendar.getInstance();
		c2.set(Calendar.DAY_OF_WEEK, c1.get(Calendar.DAY_OF_WEEK));
		c2.set(Calendar.HOUR_OF_DAY, 0);
		c2.set(Calendar.MINUTE, 0);
		c2.set(Calendar.SECOND, 0);
		c2.set(Calendar.MILLISECOND, 0);
		currentViewPos = (int) (50L + (c1.getTimeInMillis() - c2.getTimeInMillis())
				/ (7 * 24 * 60 * 60 * 1000));
		mPagerHeader.setCurrentItem(currentViewPos);
		mPagerTable.setCurrentItem(currentViewPos);
	}

	public void refresh() {
		if (mDialog.isShowing())
			mDialog.dismiss();

		mPagerTable.setAdapter(mPagerTableAdapter);
		mPagerHeader.setAdapter(mPagerHeaderAdapter);
		mPagerTable.setCurrentItem(currentViewPos);
		mPagerHeader.setCurrentItem(currentViewPos);

		setupBackgroundColor();
	}

	private void setupBackgroundColor() {
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

		if (sharedPrefs.getBoolean("preference_dark_theme_amoled", false)
				&& sharedPrefs.getBoolean("preference_dark_theme", false)) {
			findViewById(R.id.input_date).setBackgroundColor(Color.BLACK);
			findViewById(R.id.hour_view_sidebar).setBackgroundColor(Color.BLACK);
		}
	}

	@Override
	public void onBackPressed() {
		DrawerLayout drawer = findViewById(R.id.drawer_layout);
		if (drawer.isDrawerOpen(GravityCompat.START)) {
			drawer.closeDrawer(GravityCompat.START);
		} else {
			if (sessionInfo != null && sessionInfo.getElemId()
					!= mUserDataList.optJSONObject("userData").optInt("elemId", -1)) {
				setTarget(
						mUserDataList.optJSONObject("userData").optInt("elemId", -1),
						SessionInfo.getElemTypeId(mUserDataList.optJSONObject("userData")
								.optString("elemType", "")),
						mUserDataList.optJSONObject("userData")
								.optString("displayName", "BetterUntis"));
			} else {
				if (System.currentTimeMillis() - 2000 > mLastBackPress) {
					Snackbar.make(findViewById(R.id.content_main),
							R.string.snackbar_press_back_double, 2000).show();
					mLastBackPress = System.currentTimeMillis();
				} else {
					super.onBackPressed();
				}
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();

		switch (id) {
			case R.id.action_settings:
				Intent i = new Intent(ActivityMain.this, ActivityPreferences.class);
				startActivity(i);
				break;
			case R.id.action_refresh:
				refresh();
				break;
		}

		return super.onOptionsItemSelected(item);
	}

	@SuppressWarnings("StatementWithEmptyBody")
	@Override
	public boolean onNavigationItemSelected(@NonNull MenuItem item) {
		// Handle navigation view item clicks here.
		switch (sessionInfo.getElemType()) {
			case "CLASS":
				((NavigationView) findViewById(R.id.nav_view))
						.setCheckedItem(R.id.nav_show_classes);
				break;
			case "TEACHER":
				((NavigationView) findViewById(R.id.nav_view))
						.setCheckedItem(R.id.nav_show_teachers);
				break;
			case "ROOM":
				((NavigationView) findViewById(R.id.nav_view))
						.setCheckedItem(R.id.nav_show_rooms);
				break;
			default:
				((NavigationView) findViewById(R.id.nav_view))
						.setCheckedItem(R.id.nav_show_personal);
		}

		switch (item.getItemId()) {
			case R.id.nav_show_personal:
				setTarget(
						mUserDataList.optJSONObject("userData").optInt("elemId", -1),
						SessionInfo.getElemTypeId(mUserDataList.optJSONObject("userData")
								.optString("elemType", "")),
						mUserDataList.optJSONObject("userData")
								.optString("displayName", "BetterUntis"));
				break;
			case R.id.nav_show_classes:
				//noinspection SpellCheckingInspection
				showItemList(CLASS, R.string.hint_search_classes, R.string.title_class, "klassen");
				break;
			case R.id.nav_show_teachers:
				showItemList(TEACHER, R.string.hint_search_teachers, -1, "teachers");
				break;
			case R.id.nav_show_rooms:
				showItemList(ROOM, R.string.hint_search_rooms, R.string.title_room, "rooms");
				break;
			case R.id.nav_settings:
				Intent i1 = new Intent(ActivityMain.this, ActivityPreferences.class);
				startActivity(i1);
				break;
			case R.id.nav_suggested_features:
				Intent i2 = new Intent(ActivityMain.this, ActivityFeatures.class);
				startActivity(i2);
				break;
			case R.id.nav_free_rooms:
				Intent i3 = new Intent(ActivityMain.this, ActivityRoomFinder.class);
				startActivityForResult(i3, REQUEST_CODE_ROOM_FINDER);
				break;
			case R.id.nav_donations:
				// TODO: Fully implement this feature
				Intent i4 = new Intent(ActivityMain.this, ActivityDonations.class);
				startActivity(i4);
				break;
			case R.id.nav_share:
				Answers.getInstance().logShare(new ShareEvent()
						.putMethod("Share via Intent")
						.putContentName("Share the BetterUntis download link")
						.putContentType("share")
						.putContentId(CONTENT_ID_SHARE));

				Intent i = new Intent(Intent.ACTION_SEND);
				i.setType("text/plain");
				i.putExtra(Intent.EXTRA_SUBJECT, firebaseRemoteConfig.getString("recommendation_subject"));
				i.putExtra(Intent.EXTRA_TEXT, firebaseRemoteConfig.getString("recommendation_text"));
				startActivity(Intent.createChooser(i, getString(R.string.link_sending_caption, getString(R.string.app_name))));
				break;
		}

		DrawerLayout drawer = findViewById(R.id.drawer_layout);
		drawer.closeDrawer(GravityCompat.START);
		return true;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
			case REQUEST_CODE_ROOM_FINDER:
				if (resultCode == RESULT_OK) {
					setTarget(data.getIntExtra("elemId", 0),
							data.getIntExtra("elemType", ElementName.ROOM),
							data.getStringExtra("displayName"));
				}
		}
	}

	private void showItemList(final int elementType, @StringRes int searchFieldHint,
	                          final int targetPageTitle, String masterDataField) {
		DialogInterface.OnCancelListener cancelListener = dialogInterface -> {
			if (getSupportActionBar() != null)
				getSupportActionBar().setTitle(sessionInfo.getDisplayName());
			switch (sessionInfo.getElemType()) {
				case "CLASS":
					((NavigationView) findViewById(R.id.nav_view))
							.setCheckedItem(R.id.nav_show_classes);
					break;
				case "TEACHER":
					((NavigationView) findViewById(R.id.nav_view))
							.setCheckedItem(R.id.nav_show_teachers);
					break;
				case "ROOM":
					((NavigationView) findViewById(R.id.nav_view))
							.setCheckedItem(R.id.nav_show_rooms);
					break;
				default:
					((NavigationView) findViewById(R.id.nav_view))
							.setCheckedItem(R.id.nav_show_personal);
			}
		};

		try {
			final ElementName elementName = new ElementName(elementType)
					.setUserDataList(mUserDataList);
			LinearLayout content = new LinearLayout(this);
			content.setOrientation(LinearLayout.VERTICAL);

			final List<String> list = new ArrayList<>();
			JSONArray roomList = mUserDataList.optJSONObject("masterData")
					.optJSONArray(masterDataField);
			for (int i = 0; i < roomList.length(); i++)
				list.add(roomList.getJSONObject(i).getString("name"));
			Collections.sort(list, String::compareToIgnoreCase);

			final AdapterGridView adapter = new AdapterGridView(this, list);
			TextInputLayout titleContainer = new TextInputLayout(this);
			LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			params.setMargins(mItemListMargins, mItemListMargins, mItemListMargins, 0);
			titleContainer.setLayoutParams(params);

			GridView gridView = new GridView(this);
			gridView.setAdapter(adapter);
			gridView.setNumColumns(3);
			gridView.setOnItemClickListener((parent, view, position, id) -> {
				if (targetPageTitle == -1)
					setTarget((int) elementName
									.findFieldByValue("name", list.get(position), "id"),
							elementType,
							elementName.findFieldByValue("name",
									list.get(position), "firstName") + " "
									+ elementName.findFieldByValue("name", list.get(position),
									"lastName"));
				else
					setTarget((Integer) elementName.findFieldByValue("name", list.get(position),
							"id"),
							elementType,
							getString(targetPageTitle, list.get(position)));
			});
			AlertDialog.Builder builder = new AlertDialog.Builder(this);

			TextInputEditText searchField = new TextInputEditText(this);
			searchField.setHint(searchFieldHint);
			searchField.addTextChangedListener(new TextWatcher() {
				@Override
				public void onTextChanged(CharSequence s, int start, int before, int count) {
					adapter.getFilter().filter(s.toString());
				}

				@Override
				public void beforeTextChanged(CharSequence s, int start, int count, int after) {
				}

				@Override
				public void afterTextChanged(Editable s) {
				}
			});
			titleContainer.addView(searchField);

			content.addView(titleContainer);
			content.addView(gridView);
			builder.setView(content);
			mDialog = builder.create();
			mDialog.setOnCancelListener(cancelListener);
			mDialog.setCanceledOnTouchOutside(true);
			mDialog.show();
		} catch (JSONException e) {
			Snackbar.make(mPagerTable, getString(R.string.snackbar_error, e.getMessage()), Snackbar.LENGTH_LONG)
					.setAction("OK", null).show();
			swipeRefresh.setRefreshing(false);
			e.printStackTrace();
		}
	}

	private void setTarget(int elemId, int elemType, String displayName) {
		if (sessionInfo == null)
			sessionInfo = new SessionInfo();
		sessionInfo.setElemId(elemId);
		sessionInfo.setElemType(SessionInfo.getElemTypeName(elemType));
		sessionInfo.setDisplayName(displayName);
		if (getSupportActionBar() != null)
			getSupportActionBar().setTitle(displayName);

		switch (elemType) {
			case CLASS:
				((NavigationView) findViewById(R.id.nav_view))
						.setCheckedItem(R.id.nav_show_classes);
				break;
			case TEACHER:
				((NavigationView) findViewById(R.id.nav_view))
						.setCheckedItem(R.id.nav_show_teachers);
				break;
			case ROOM:
				((NavigationView) findViewById(R.id.nav_view))
						.setCheckedItem(R.id.nav_show_rooms);
				break;
		}
		refresh();
	}

	private boolean checkLoginState() {
		SharedPreferences prefs = getSharedPreferences("login_data", MODE_PRIVATE);
		return !(prefs.getString("url", "N/A").equals("N/A") ||
				prefs.getString("school", "N/A").equals("N/A") ||
				prefs.getString("user", "N/A").equals("N/A") ||
				prefs.getString("key", "N/A").equals("N/A"));
	}

	private void addHour(TimegridUnitManager.UnitData unitData, int index, boolean alternatingHours, int alternativeBackgroundColor) {
		LinearLayout sidebar = findViewById(R.id.hour_view_sidebar);
		@SuppressLint("InflateParams") View v = getLayoutInflater()
				.inflate(R.layout.item_hour, null);
		v.setLayoutParams(new RelativeLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, dp2px(60)));
		((TextView) v.findViewById(R.id.tvTimeStart)).setText(unitData.getDisplayStartTime());
		((TextView) v.findViewById(R.id.tvTimeEnd)).setText(unitData.getDisplayEndTime());
		((TextView) v.findViewById(R.id.tvHourIndex)).setText(String.format(Locale.US, "%d", index));

		if (alternatingHours && index % 2 == 1)
			v.setBackgroundColor(alternativeBackgroundColor);

		sidebar.addView(v);
	}

	public SharedPreferences getLoginData() {
		return getSharedPreferences("login_data", MODE_PRIVATE);
	}

	public void stopRefreshing() {
		swipeRefresh.setRefreshing(false);
	}

	private void checkVersion() {
		Intent i = getIntent();
		if (i.getBooleanExtra("disable_update_check", false))
			return;
		AutoUpdater au = new AutoUpdater() {
			@Override
			public void onAppVersionOutdated(int oldVersion, String oldVersionName, int newVersion, String newVersionName) {
				Intent intent = new Intent(getApplicationContext(), ActivityAppUpdate.class);
				intent.putExtra("currentVersion", oldVersionName);
				intent.putExtra("currentVersionCode", oldVersion);
				intent.putExtra("newVersion", newVersionName);
				startActivity(intent);
				finish();
			}
		};
		au.setVersionURL("https://data.sapuseven.com/BetterUntis/api.php?method=getVersion");
		try {
			au.setPackageInfo(getPackageManager(), getPackageName());
		} catch (PackageManager.NameNotFoundException e) {
			e.printStackTrace();
		}
		au.startAutoUpdate(this);
	}

	@SuppressWarnings("SameParameterValue")
	private int addDaysToInt(int startDate, int days) {
		try {
			Calendar c = Calendar.getInstance();
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.US);
			c.setTime(sdf.parse(Integer.toString(startDate)));
			c.add(Calendar.DATE, days);
			return Integer.parseInt(new SimpleDateFormat("yyyyMMdd", Locale.US)
					.format(c.getTime()));
		} catch (ParseException e) {
			e.printStackTrace();
			return startDate;
		}
	}

	public void setLastRefresh(long time) {
		if (time == -1)
			lastRefresh.setText(getString(R.string.last_refreshed, getString(R.string.never)));
		else
			lastRefresh.setText(getString(R.string.last_refreshed, formatTimeDiff(System.currentTimeMillis() - time)));
	}

	private String formatTimeDiff(long diff) {
		if (diff < MINUTE_MILLIS)
			return getString(R.string.time_diff_just_now);
		else if (diff < 50 * MINUTE_MILLIS)
			return getResources().getQuantityString(R.plurals.time_diff_minutes, (int) (diff / MINUTE_MILLIS), diff / MINUTE_MILLIS);
		else if (diff < 24 * HOUR_MILLIS)
			return getResources().getQuantityString(R.plurals.time_diff_hours, (int) (diff / HOUR_MILLIS), diff / HOUR_MILLIS);
		else
			return getResources().getQuantityString(R.plurals.time_diff_days, (int) (diff / DAY_MILLIS), diff / DAY_MILLIS);
	}

	private void checkForNewFeatures() {
		SharedPreferences prefs = getLoginData();
		String user = prefs.getString("user", "");
		String school = prefs.getString("school", "");

		ApiRequest api = new ApiRequest(this);

		Map<String, String> params = new HashMap<>();
		params.put("method", "checkForNewFeatures");
		params.put("school", school);
		params.put("name", user);

		ApiRequest.ResponseHandler handler = response -> {
			try {
				JSONObject list = new JSONObject(response);
				if (list.getJSONObject("result").getBoolean("newFeatures")) {
					Snackbar.make(findViewById(R.id.content_main), R.string.new_feature_planned,
							Snackbar.LENGTH_INDEFINITE).setAction(R.string.show, view -> {
						Intent i = new Intent(ActivityMain.this, ActivityFeatures.class);
						startActivity(i);
					}).show();
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		};

		api.setResponseHandler(handler).submit(params);
	}
}