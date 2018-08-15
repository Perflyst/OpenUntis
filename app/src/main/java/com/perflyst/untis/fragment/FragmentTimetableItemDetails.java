package com.perflyst.untis.fragment;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.perflyst.untis.R;
import com.perflyst.untis.utils.ElementName;
import com.perflyst.untis.utils.ListManager;
import com.perflyst.untis.utils.timetable.TimetableItemData;

import org.json.JSONException;
import org.json.JSONObject;

import static com.perflyst.untis.utils.Conversions.dp2px;
import static com.perflyst.untis.utils.ElementName.ElementType.CLASS;
import static com.perflyst.untis.utils.ElementName.ElementType.ROOM;
import static com.perflyst.untis.utils.ElementName.ElementType.TEACHER;

public class FragmentTimetableItemDetails extends Fragment {
	private TimetableItemData timetableItemData;
	private JSONObject userDataList;
	private Context context;
	private FragmentTimetable fragment;

	public static FragmentTimetableItemDetails createInstance(FragmentTimetable fragment, Context context, TimetableItemData itemData) {
		FragmentTimetableItemDetails fragmentTimetableItemDetails = new FragmentTimetableItemDetails();
		fragmentTimetableItemDetails.setContext(context);
		fragmentTimetableItemDetails.setFragment(fragment);
		fragmentTimetableItemDetails.setTimetableItemData(itemData);
		fragmentTimetableItemDetails.setUserDataList(ListManager.getUserData(context));
		return fragmentTimetableItemDetails;
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		LinearLayout root = (LinearLayout) inflater.inflate(R.layout.dialog_timetable_item_detail_page, container, false);

		if (timetableItemData == null)
			return root;

		if (timetableItemData.getTeachers(userDataList).isEmpty())
			root.findViewById(R.id.llTeachers).setVisibility(View.GONE);
		if (timetableItemData.getClasses(userDataList).isEmpty())
			root.findViewById(R.id.llClasses).setVisibility(View.GONE);
		if (timetableItemData.getRooms(userDataList).isEmpty())
			root.findViewById(R.id.llRooms).setVisibility(View.GONE);

		if ((timetableItemData.getTeachers(userDataList).isEmpty()
				&& timetableItemData.getClasses(userDataList).isEmpty()
				&& timetableItemData.getRooms(userDataList).isEmpty()))
			return null;

		int[] attrs = new int[]{android.R.attr.textColorPrimary};
		TypedArray ta = context.obtainStyledAttributes(attrs);
		int color = ta.getColor(0, 0);
		ta.recycle();

		for (String info : timetableItemData.getInfos()) {
			LinearLayout infoView = (LinearLayout) inflater.inflate(R.layout.dialog_timetable_item_detail_page_info, root, false);
			((TextView) infoView.findViewById(R.id.tvInfo)).setText(info);
			root.addView(infoView);
		}

		LinearLayout list = root.findViewById(R.id.llTeacherList);
		for (final String s : timetableItemData.getTeachers(userDataList).getNames()) {
			final ElementName elementName = new ElementName(TEACHER, userDataList);
			TextView tv = new TextView(context.getApplicationContext());
			LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
					ViewGroup.LayoutParams.WRAP_CONTENT,
					ViewGroup.LayoutParams.MATCH_PARENT);
			params.setMargins(0, 0, dp2px(12), 0);
			tv.setText(s);
			tv.setLayoutParams(params);
			tv.setTextColor(color);
			tv.setGravity(Gravity.CENTER_VERTICAL);
			tv.setOnClickListener(view -> {
				try {
					if (elementName.findFieldByValue("name", s, "id") != null)
						setTarget((Integer) elementName.findFieldByValue("name", s, "id"), TEACHER);
				} catch (JSONException e) {
					e.printStackTrace(); // Not expected to occur
				}
			});
			list.addView(tv);
		}

		list = root.findViewById(R.id.llClassList);
		for (final String s : timetableItemData.getClasses(userDataList).getNames()) {
			final ElementName elementName = new ElementName(CLASS, userDataList);
			TextView tv = new TextView(context.getApplicationContext());
			LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
					ViewGroup.LayoutParams.WRAP_CONTENT,
					ViewGroup.LayoutParams.MATCH_PARENT);
			params.setMargins(0, 0, dp2px(12), 0);
			tv.setText(s);
			tv.setLayoutParams(params);
			tv.setTextColor(color);
			tv.setGravity(Gravity.CENTER_VERTICAL);
			tv.setOnClickListener(view -> {
				try {
					if (elementName.findFieldByValue("name", s, "id") != null)
						setTarget((Integer) elementName
								.findFieldByValue("name", s, "id"), CLASS);
				} catch (JSONException e) {
					e.printStackTrace(); // Not expected to occur
				}
			});
			list.addView(tv);
		}

		list = root.findViewById(R.id.llRoomList);
		for (final String s : timetableItemData.getRooms(userDataList).getNames()) {
			final ElementName elementName = new ElementName(ROOM, userDataList);
			TextView tv = new TextView(context.getApplicationContext());
			LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
					ViewGroup.LayoutParams.WRAP_CONTENT,
					ViewGroup.LayoutParams.MATCH_PARENT);
			params.setMargins(0, 0, dp2px(12), 0);
			tv.setText(s);
			tv.setLayoutParams(params);
			tv.setTextColor(color);
			tv.setGravity(Gravity.CENTER_VERTICAL);
			tv.setOnClickListener(view -> {
				try {
					if (elementName.findFieldByValue("name", s, "id") != null)
						setTarget((Integer) elementName.findFieldByValue("name", s, "id"), ROOM);
				} catch (JSONException e) {
					e.printStackTrace(); // Not expected to occur
				}
			});
			list.addView(tv);
		}

		if (timetableItemData.getSubjects(userDataList).getLongNames().size() > 0) {
			StringBuilder titleBuilder = new StringBuilder(timetableItemData
					.getSubjects(userDataList).getLongNames().get(0));
			for (int i = 1; i < timetableItemData.getSubjects(userDataList).getLongNames().size(); i++) {
				titleBuilder.append(", ").append(timetableItemData.getSubjects(userDataList).getLongNames().get(i));
			}
			String title = titleBuilder.toString();
			if (timetableItemData.isCancelled())
				title = getString(R.string.lesson_cancelled, title);
			if (timetableItemData.isIrregular())
				title = getString(R.string.lesson_irregular, title);
			if (timetableItemData.isExam())
				title = getString(R.string.lesson_test, title);
			((TextView) root.findViewById(R.id.title)).setText(title);
		}
		return root;
	}

	private void setTarget(int id, ElementName.ElementType type) throws JSONException {
		fragment.setTarget(id, type);
	}

	private void setTimetableItemData(TimetableItemData timetableItemData) {
		this.timetableItemData = timetableItemData;
	}

	private void setUserDataList(JSONObject userDataList) {
		this.userDataList = userDataList;
	}

	private void setContext(Context context) {
		this.context = context;
	}

	private void setFragment(FragmentTimetable fragment) {
		this.fragment = fragment;
	}
}