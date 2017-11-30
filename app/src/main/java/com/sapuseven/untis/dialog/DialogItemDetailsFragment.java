package com.sapuseven.untis.dialog;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.sapuseven.untis.R;
import com.sapuseven.untis.activity.ActivityMain;
import com.sapuseven.untis.adapter.AdapterItemDetails;
import com.sapuseven.untis.fragment.FragmentTimetable;
import com.sapuseven.untis.fragment.FragmentTimetableItemDetails;
import com.sapuseven.untis.utils.TimetableItemData;

import java.util.ArrayList;
import java.util.List;

public class DialogItemDetailsFragment extends DialogFragment implements ViewPager.OnPageChangeListener {
	private ViewPager mPager;
	private LinearLayout mPagerIndicator;
	private ImageView[] dots;
	private List<TimetableItemData> mItems = new ArrayList<>();
	private FragmentTimetable mFragment;
	private ActivityMain mMainActivity;

	public void setItems(ArrayList<TimetableItemData> timetableItemDataArray) {
		mItems = timetableItemDataArray;
	}

	public void setFragment(FragmentTimetable fragment) {
		mFragment = fragment;
	}


	public void setMainActivity(ActivityMain main) {
		mMainActivity = main;
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.dialog_timetable_item_details, container, false);
		mPager = rootView.findViewById(R.id.viewpager);
		mPagerIndicator = rootView.findViewById(R.id.viewpagerIndicator);
		AdapterItemDetails adapter = new AdapterItemDetails(getChildFragmentManager());
		for (TimetableItemData item : mItems)
			adapter.addFragment(FragmentTimetableItemDetails.createInstance(mFragment, mMainActivity, item));
		mPager.setAdapter(adapter);
		if (mItems.size() > 1)
			setupPagerIndicator();
		else
			mPagerIndicator.setVisibility(View.GONE);
		return rootView;
	}

	private void setupPagerIndicator() {
		mPager.addOnPageChangeListener(this);
		dots = new ImageView[mItems.size()];

		for (int i = 0; i < mItems.size(); i++) {
			dots[i] = new ImageView(this.getContext());
			dots[i].setImageResource(R.drawable.non_selected_item_indicator_dot);
			LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.WRAP_CONTENT,
					LinearLayout.LayoutParams.WRAP_CONTENT
			);

			params.setMargins(6, 6, 6, 6);
			mPagerIndicator.addView(dots[i], params);
		}

		dots[0].setImageResource(R.drawable.selected_item_indicator_dot);
	}

	@Override
	public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

	}

	@Override
	public void onPageSelected(int position) {
		for (int i = 0; i < mItems.size(); i++)
			dots[i].setImageResource(R.drawable.non_selected_item_indicator_dot);

		dots[position].setImageResource(R.drawable.selected_item_indicator_dot);
	}

	@Override
	public void onPageScrollStateChanged(int state) {

	}
}
