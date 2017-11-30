package com.sapuseven.untis.adapter;


import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.sapuseven.untis.fragment.FragmentTimetableItemDetails;

import java.util.ArrayList;
import java.util.List;

public class AdapterItemDetails extends FragmentPagerAdapter {
	private final List<FragmentTimetableItemDetails> mFragmentCollection = new ArrayList<>();

	public AdapterItemDetails(FragmentManager fm) {
		super(fm);
	}

	public void addFragment(FragmentTimetableItemDetails fragment) {
		mFragmentCollection.add(fragment);
	}

	@Override
	public Fragment getItem(int position) {
		return mFragmentCollection.get(position);
	}

	@Override
	public int getCount() {
		return mFragmentCollection.size();
	}
}