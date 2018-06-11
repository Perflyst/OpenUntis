package com.sapuseven.untis.adapter;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import com.sapuseven.untis.fragment.FragmentTimetableHeader;

public class AdapterTimetableHeader extends FragmentStatePagerAdapter {
	public AdapterTimetableHeader(FragmentManager fm) {
		super(fm);
	}

	@Override
	public Fragment getItem(int position) {
		FragmentTimetableHeader fragment = new FragmentTimetableHeader();
		Bundle args = new Bundle();
		args.putInt("position", position);
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public int getCount() {
		return 100;
	}
}