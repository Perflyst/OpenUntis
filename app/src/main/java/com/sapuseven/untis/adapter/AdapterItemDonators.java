package com.sapuseven.untis.adapter;

import android.graphics.drawable.Drawable;

import com.sapuseven.untis.activity.ActivityDonators;

public class AdapterItemDonators {
	private final String rank;
	private final String name;
	private final String donationAmount;
	private final Drawable avatar;
	private final ActivityDonators activity;

	public AdapterItemDonators(ActivityDonators activity, String rank, String name, String donationAmount, Drawable avatar) {
		this.activity = activity;
		this.rank = rank;
		this.name = name;
		this.donationAmount = donationAmount;
		this.avatar = avatar;
	}

	public String getRank() {
		return rank;
	}

	public String getName() {
		return name;
	}

	public String getDonationAmount() {
		return donationAmount;
	}

	public Drawable getAvatar() {
		return avatar;
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}
}
