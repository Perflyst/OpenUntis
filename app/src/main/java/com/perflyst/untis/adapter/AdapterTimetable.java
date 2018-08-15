package com.perflyst.untis.adapter;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import com.perflyst.untis.fragment.FragmentTimetable;

public class AdapterTimetable extends FragmentStatePagerAdapter {
	public AdapterTimetable(FragmentManager fm) {
		super(fm);
	}

	@Override
	public int getItemPosition(@NonNull Object object) {
		return POSITION_NONE;
	}

	@Override
	public Fragment getItem(int position) {
		FragmentTimetable fragment = new FragmentTimetable();
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
