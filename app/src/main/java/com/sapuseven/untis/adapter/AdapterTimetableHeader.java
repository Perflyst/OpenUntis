package com.sapuseven.untis.adapter;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import com.sapuseven.untis.fragment.FragmentTimetableHeader;

import org.json.JSONObject;

public class AdapterTimetableHeader extends FragmentStatePagerAdapter {
	private JSONObject userData;

	public AdapterTimetableHeader(FragmentManager fm, JSONObject userData) {
		super(fm);
		this.userData = userData;
	}

	@Override
	public Fragment getItem(int position) {
		FragmentTimetableHeader fragment = new FragmentTimetableHeader();
		fragment.setUserData(userData);
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