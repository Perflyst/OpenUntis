package de.perflyst.untis.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.ImageSpan;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;

import de.perflyst.untis.R;
import de.perflyst.untis.adapter.AdapterCheckBoxGridView;
import de.perflyst.untis.adapter.AdapterItemRoomFinder;
import de.perflyst.untis.adapter.AdapterRoomFinder;
import de.perflyst.untis.utils.Constants;
import de.perflyst.untis.utils.DateOperations;
import de.perflyst.untis.utils.ElementName;
import de.perflyst.untis.utils.ListManager;
import de.perflyst.untis.utils.SessionInfo;
import de.perflyst.untis.utils.connectivity.UntisRequest;
import de.perflyst.untis.utils.timetable.TimegridUnitManager;
import de.perflyst.untis.utils.timetable.Timetable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import de.perflyst.untis.utils.ThemeUtils;

import static de.perflyst.untis.utils.DateOperations.addDaysToInt;
import static de.perflyst.untis.utils.DateOperations.getStartDateFromWeek;
import static de.perflyst.untis.utils.ElementName.ElementType.ROOM;
import static de.perflyst.untis.utils.SessionInfo.getElemTypeName;
import static de.perflyst.untis.utils.connectivity.UntisAuthentication.getAuthObject;

public class ActivityRoomFinder extends AppCompatActivity implements View.OnClickListener {
	private int roomListMargins;
	private JSONObject userDataList;
	private AlertDialog dialog;
	private ArrayList<AdapterItemRoomFinder> roomList;
	private AdapterRoomFinder roomAdapter;
	private int currentHourIndex = -1;
	private int hourIndexOffset;
	private ArrayList<RequestModel> requestQueue;
	private RecyclerView recyclerView;
	private TextView currentHour;
	private int maxHourIndex = 0;

	public static ArrayList<String> getRooms(Context context, boolean includeDisable) {
		ArrayList<String> roomList = new ArrayList<>();

		if (includeDisable)
			roomList.add(context.getString(R.string.preference_note_disable));

		try {
			File file = context.getFileStreamPath("roomList.txt");
			if (file != null && file.exists()) {
				BufferedReader reader = new BufferedReader(new InputStreamReader(context.openFileInput("roomList.txt")));
				String name;
				while ((name = reader.readLine()) != null) {
					for (int i = 0; i < 2; i++)
						reader.readLine();
					roomList.add(name);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return roomList;
	}

	public static String getRoomStates(Context context, String name) {
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					context.openFileInput("roomList.txt")));
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.equals(name))
					return reader.readLine();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return "";
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		ThemeUtils.setupTheme(this, true);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_room_finder);

		roomListMargins = (int) (12 * getResources().getDisplayMetrics().density + 0.5f);

		userDataList = ListManager.getUserData(getApplicationContext());

		recyclerView = findViewById(R.id.lvRoomList);
		setupNoRoomsIndicator();
		setupRoomList(recyclerView);
		setupHourSelector();
	}

	private void setupHourSelector() {
		findViewById(R.id.btnNextHour).setOnClickListener(v -> {
			if (currentHourIndex + hourIndexOffset < maxHourIndex) {
				hourIndexOffset++;
				refreshRoomList();
			}
		});

		findViewById(R.id.btnPrevHour).setOnClickListener(v -> {
			if (currentHourIndex + hourIndexOffset > 0) {
				hourIndexOffset--;
				refreshRoomList();
			}
		});

		findViewById(R.id.tvCurrentHour).setOnClickListener(view -> {
			hourIndexOffset = 0;
			refreshRoomList();
		});
	}

	private void setupNoRoomsIndicator() {
		TextView tv = findViewById(R.id.tvNoRooms);
		String text = tv.getText().toString();
		if (text.contains("+")) {
			SpannableString ss = new SpannableString(text);
			Drawable img = ContextCompat.getDrawable(this, R.drawable.ic_add_circle);
			if (img != null) {
				img.setBounds(0, 0, img.getIntrinsicWidth(), img.getIntrinsicHeight());
				ss.setSpan(new ImageSpan(img, ImageSpan.ALIGN_BOTTOM),
						text.indexOf("+"), text.indexOf("+") + 1, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
			}
			tv.setText(ss);
		}
	}

	private void setupRoomList(RecyclerView listView) {
		roomList = new ArrayList<>();
		requestQueue = new ArrayList<>();

		listView.setLayoutManager(new LinearLayoutManager(this));
		roomAdapter = new AdapterRoomFinder(this, roomList);
		listView.setAdapter(roomAdapter);

		reload();

		FloatingActionButton myFab = findViewById(R.id.fabAddRoomWatcher);
		myFab.setOnClickListener(v -> showItemList());
	}

	private void reload() {
		roomList.clear();
		try {
			BufferedReader reader =
					new BufferedReader(new InputStreamReader(openFileInput("roomList.txt")));
			String name;
			while ((name = reader.readLine()) != null) {
				AdapterItemRoomFinder roomItem =
						new AdapterItemRoomFinder(this, name, isInRequestQueue(name));
				String binaryData = reader.readLine();
				boolean[] states = new boolean[binaryData.length()];
				for (int i = 0; i < states.length; i++)
					states[i] = binaryData.charAt(i) == '1';
				roomItem.setStates(states);
				maxHourIndex = states.length - 1;
				roomItem.setDate(Long.parseLong(reader.readLine()));
				roomList.add(roomItem);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		for (RequestModel r : requestQueue) {
			if (!r.isRefreshOnly())
				roomList.add(new AdapterItemRoomFinder(this, r.getDisplayName(), true));
			roomAdapter.notifyItemInserted(roomList.size() - 1);
		}

		refreshRoomList();
	}

	private boolean isInRequestQueue(String name) {
		for (RequestModel r : requestQueue)
			if (r.getDisplayName().equals(name))
				return true;
		return false;
	}

	private void refreshRoomList() {
		if (roomList.isEmpty())
			findViewById(R.id.tvNoRooms).setVisibility(View.VISIBLE);
		else
			findViewById(R.id.tvNoRooms).setVisibility(View.GONE);

		Collections.sort(roomList); // TODO: Due to multiple calls of getIndex(), this takes quite a bit of time. Better make this asynchronous and display a loading indicator
		roomAdapter.notifyDataSetChanged();
		displayCurrentHour();
	}

	private void showItemList() {
		try {
			final ElementName elementName = new ElementName(ROOM, userDataList);
			LinearLayout content = new LinearLayout(this);
			content.setOrientation(LinearLayout.VERTICAL);

			final List<String> list = new ArrayList<>();
			JSONArray roomList = userDataList.optJSONObject("masterData").optJSONArray("rooms");
			for (int i = 0; i < roomList.length(); i++)
				list.add(roomList.getJSONObject(i).getString("name"));
			Collections.sort(list, String::compareToIgnoreCase);

			final AdapterCheckBoxGridView adapter = new AdapterCheckBoxGridView(this, list);
			TextInputLayout titleContainer = new TextInputLayout(this);
			LinearLayout.LayoutParams searchFieldParams =
					new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
							ViewGroup.LayoutParams.WRAP_CONTENT);
			searchFieldParams.setMargins(roomListMargins, roomListMargins, roomListMargins, 0);
			titleContainer.setLayoutParams(searchFieldParams);

			GridView gridView = new GridView(this);
			LinearLayout.LayoutParams gridParams =
					new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1);
			gridView.setLayoutParams(gridParams);
			gridView.setChoiceMode(AbsListView.CHOICE_MODE_NONE);
			gridView.setAdapter(adapter);
			gridView.setNumColumns(3);

			TextInputEditText searchField = new TextInputEditText(this);
			searchField.setHint(R.string.hint_add_room);
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
			@SuppressLint("InflateParams") Button selectAll = (Button) getLayoutInflater()
					.inflate(R.layout.borderless_button, null);
			selectAll.setText(R.string.add);
			final ActivityRoomFinder context = this;
			selectAll.setOnClickListener(v -> {
				for (String item : adapter.getSelectedItems()) {
					try {
						addRoom(new AdapterItemRoomFinder(context, item, true),
								(Integer) elementName.findFieldByValue("name", item, "id"));
					} catch (JSONException e) {
						e.printStackTrace(); // Not expected to occur
					}
				}
				dialog.dismiss();
				executeRequestQueue();
			});

			content.addView(titleContainer);
			content.addView(gridView);
			content.addView(selectAll);

			dialog = new AlertDialog.Builder(this)
					.setView(content)
					.create();
			dialog.show();
		} catch (JSONException e) {
			e.printStackTrace();
			new AlertDialog.Builder(this)
					.setTitle(getString(R.string.error))
					.setMessage(e.getMessage())
					.setNeutralButton(R.string.ok, (dialog, which) -> dialog.dismiss())
					.show();
		}
	}

	private void addRoom(AdapterItemRoomFinder item, int roomID) {
		if (roomList.contains(item))
			return;

		requestQueue.add(new RequestModel(roomID, item.getName()));

		refreshRoomList();
	}

	private void executeRequestQueue() {
		reload();

		if (requestQueue.size() > 0)
			loadRoom(requestQueue.get(0));
	}

	private void loadRoom(RequestModel room) {
		JSONArray dayList = null;
		try {
			dayList = new JSONObject(new ListManager(getApplication())
					.readList("userData", false)).getJSONObject("masterData")
					.getJSONObject("timeGrid").getJSONArray("days");
		} catch (JSONException e) {
			e.printStackTrace();
		}

		TimegridUnitManager unitManager = new TimegridUnitManager(dayList);

		int days = unitManager.getNumberOfDays();
		int hours = unitManager.getMaxHoursPerDay();

		int startDateFromWeek = Integer.parseInt(new SimpleDateFormat("yyyyMMdd", Locale.ENGLISH)
				.format(getStartDateFromWeek(Calendar.getInstance(), 0).getTime()));

		SharedPreferences prefs = getSharedPreferences("login_data", MODE_PRIVATE);
		SessionInfo sessionInfo = new SessionInfo();
		sessionInfo.setElemId(room.getRoomID());
		sessionInfo.setElemType(getElemTypeName(ROOM));

		UntisRequest api = new UntisRequest(this, sessionInfo);

		UntisRequest.ResponseHandler handler = response -> {
			if (response == null) {
				Log.w("ActivityRoomFinder", "response is null");
				// TODO: Stop loading and show "unknown error: null";
				return;
			}
			try {
				if (response.has("error")) {
					Log.w("error", response.toString());
					Snackbar.make(recyclerView,
							getString(R.string.snackbar_error, response.getJSONObject("error")
									.getString("message")), Snackbar.LENGTH_LONG)
							.setAction("OK", null).show();
				} else if (response.has("result")) {
					RequestModel requestModel = requestQueue.get(0);

					Timetable timetable = new Timetable(response.getJSONObject("result"), PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));

					boolean[] states = new boolean[days * hours];

					for (int i = 0; i < states.length; i++) {
						int day = i / hours;
						int hour = i % hours;

						if (timetable.getItems(day, hour).size() > 0)
							states[day * hours + hour] = true;
					}

					StringBuilder binaryData = new StringBuilder();
					for (boolean value : states)
						binaryData.append(value ? '1' : '0');

					if (!TextUtils.isEmpty(binaryData.toString())) {
						if (requestModel.isRefreshOnly())
							deleteItem(requestModel.getDisplayName());

						BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
								openFileOutput("roomList.txt", MODE_APPEND), "UTF-8"));
						writer.write(requestModel.getDisplayName());
						writer.newLine();
						writer.write(binaryData.toString());
						writer.newLine();
						writer.write(String.valueOf(getStartDateFromWeek(Calendar.getInstance(), 0,
								true).getTimeInMillis()));
						writer.newLine();
						writer.close();
					}

					reload();
					refreshRoomList();
				}
			} catch (JSONException | IOException e) {
				e.printStackTrace();
			}
			requestQueue.remove(0);
			executeRequestQueue();
		};

		UntisRequest.UntisRequestQuery query = new UntisRequest.UntisRequestQuery();
		query.setMethod(Constants.UntisAPI.METHOD_GET_TIMETABLE);
		query.setUrl(prefs.getString("url", null));
		query.setSchool(prefs.getString("school", null));

		JSONObject params = new JSONObject();
		try {
			params
					.put("id", room.getRoomID())
					.put("type", getElemTypeName(ROOM))
					.put("startDate", startDateFromWeek)
					.put("endDate", addDaysToInt(startDateFromWeek, days))
					.put("masterDataTimestamp", System.currentTimeMillis())
					.put("auth", getAuthObject(prefs.getString("user", ""), prefs.getString("key", "")));
		} catch (JSONException e) {
			e.printStackTrace(); // TODO: Implment proper error handling (search for possible cases first)
		}
		query.setParams(new JSONArray().put(params));

		api.setCachingMode(UntisRequest.CachingMode.RETURN_CACHE_LOAD_LIVE);
		api.setResponseHandler(handler).submit(query);
	}

	public int getCurrentHourIndex() {
		if (currentHourIndex >= 0)
			return currentHourIndex + hourIndexOffset;

		int index = 0;

		JSONArray days = null;
		try {
			days = new JSONObject(new ListManager(getApplication())
					.readList("userData", false))
					.getJSONObject("masterData")
					.getJSONObject("timeGrid")
					.getJSONArray("days");
		} catch (JSONException e) {
			e.printStackTrace();
		}

		TimegridUnitManager unitManager = new TimegridUnitManager(days);

		Calendar cNow = Calendar.getInstance();
		Calendar cToCompare = Calendar.getInstance();

		int startDateFromWeek = Integer.parseInt(new SimpleDateFormat("yyyyMMdd", Locale.ENGLISH)
				.format(DateOperations.getStartDateFromWeek(Calendar.getInstance(), 0).getTime()));

		for (int i = 0; i < unitManager.getNumberOfDays() * unitManager.getMaxHoursPerDay(); i++) {
			String dateTime = addDaysToInt(startDateFromWeek, i / unitManager.getMaxHoursPerDay(),
					new SimpleDateFormat("yyyy-MM-dd'T'", Locale.ENGLISH))
					+ String.format("%1$5s", unitManager.getUnits().get(i % unitManager
					.getMaxHoursPerDay()).getDisplayEndTime()).replace(' ', '0') + "Z";

			try {
				cToCompare.setTime(DateOperations.parseFromISO(dateTime));
			} catch (ParseException e) {
				e.printStackTrace();
			}

			if (cNow.getTimeInMillis() > cToCompare.getTimeInMillis())
				index++;
			else
				break;
		}

		if (index == unitManager.getNumberOfDays() * unitManager.getMaxHoursPerDay())
			index = 0;

		Log.d("RoomFinder", "Current Hour Index: " + index);
		currentHourIndex = Math.max(index, 0);
		return currentHourIndex + hourIndexOffset;
	}

	public void showDeleteItemDialog(int position) {
		new AlertDialog.Builder(this)
				.setTitle(getString(R.string.delete_item_title, roomList.get(position).getName()))
				.setMessage(R.string.delete_item_text)
				.setPositiveButton(R.string.yes, (dialog, which) -> {
					try {
						if (deleteItem(roomList.get(position).getName())) {
							roomList.remove(position);
							roomAdapter.notifyItemRemoved(position);
							refreshRoomList();
						}
					} catch (IOException e) {
						Snackbar.make(recyclerView, getString(R.string.snackbar_error,
								e.getMessage()),
								Snackbar.LENGTH_LONG).setAction("OK", null).show();
					}
				})
				.setNegativeButton(R.string.no, (dialog, which) -> dialog.dismiss())
				.create()
				.show();
	}

	private boolean deleteItem(String name) throws IOException {
		File inputFile = new File(getFilesDir(), "roomList.txt");
		File tempFile = new File(getFilesDir(), "roomList.txt.tmp");

		BufferedReader reader = new BufferedReader(new InputStreamReader(
				openFileInput("roomList.txt"), "UTF-8"));
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
				openFileOutput("roomList.txt.tmp", MODE_APPEND), "UTF-8"));

		String currentLine;

		while ((currentLine = reader.readLine()) != null) {
			String trimmedLine = currentLine.trim();

			if (trimmedLine.equals(name)) {
				reader.readLine();
				reader.readLine();
				continue;
			}

			writer.write(currentLine);
			writer.newLine();
			writer.write(reader.readLine());
			writer.newLine();
			writer.write(reader.readLine());
			writer.newLine();
		}
		reader.close();
		writer.close();

		return inputFile.delete() && tempFile.renameTo(inputFile);
	}

	public void refreshItem(final int position) {
		new AlertDialog.Builder(this)
				.setTitle(R.string.refresh_item_title)
				.setMessage(getString(R.string.refresh_item_text))
				.setPositiveButton(R.string.refresh_this_item, (dialog, which) -> {
					refreshItemData(position);
					roomAdapter.notifyDataSetChanged();
					executeRequestQueue();
					dialog.dismiss();
				})
				.setNeutralButton(R.string.refresh_all_items, (dialog, which) -> {
					for (int i = 0; i < roomList.size(); i++)
						if (roomList.get(i).isOutdated())
							refreshItemData(i);
					roomAdapter.notifyDataSetChanged();
					executeRequestQueue();
					dialog.dismiss();
				})
				.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
				.create()
				.show();
	}

	private void refreshItemData(final int position) {
		roomList.get(position).setLoading();

		final ElementName elementName = new ElementName(ROOM, userDataList);

		try {
			requestQueue.add(new RequestModel((Integer) elementName.findFieldByValue("name",
					roomList.get(position).getName(), "id"), roomList.get(position).getName(), true));
		} catch (JSONException e) {
			e.printStackTrace(); // Not expected to occur
		}
	}

	private void displayCurrentHour() {
		if (currentHour == null)
			currentHour = findViewById(R.id.tvCurrentHour);

		if (hourIndexOffset < 0)
			currentHour.setText(getResources().getQuantityString(R.plurals.hour_index_last,
					Math.abs(hourIndexOffset), Math.abs(hourIndexOffset)));
		else if (hourIndexOffset > 0)
			currentHour.setText(getResources().getQuantityString(R.plurals.hour_index_next,
					hourIndexOffset, hourIndexOffset));
		else
			currentHour.setText(getString(R.string.hour_index_current));
	}

	@Override
	public void onClick(final View v) {
		int itemPosition = recyclerView.getChildLayoutPosition(v);
		AdapterItemRoomFinder item = roomList.get(itemPosition);

		Intent intent = new Intent();
		final ElementName elementName = new ElementName(ROOM, userDataList);
		try {
			intent.putExtra("elemId", (int) elementName.findFieldByValue("name", item.getName(), "id"));
		} catch (JSONException e) {
			e.printStackTrace(); // Not expected to occur
		}
		intent.putExtra("elemType", ROOM);
		intent.putExtra("displayName", getString(R.string.title_room, item.getName()));
		setResult(RESULT_OK, intent);
		finish();
	}

	private class RequestModel {
		private String displayName;
		private int roomID;
		private boolean refreshOnly;

		RequestModel(int roomID, String displayName) {
			this.roomID = roomID;
			this.displayName = displayName;
		}

		RequestModel(int roomID, String displayName, boolean refreshOnly) {
			this.roomID = roomID;
			this.displayName = displayName;
			this.refreshOnly = refreshOnly;
		}

		private String getDisplayName() {
			return displayName;
		}

		boolean isRefreshOnly() {
			return refreshOnly;
		}

		int getRoomID() {
			return roomID;
		}
	}
}