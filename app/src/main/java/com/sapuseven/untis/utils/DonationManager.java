package com.sapuseven.untis.utils;

import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Currency;

public class DonationManager {
	private final ArrayList<Donation> donations = new ArrayList<>();

	public DonationManager(JSONArray donations) throws JSONException {
		for (int i = 0; i < donations.length(); i++)
			this.donations.add(new Donation(donations.getJSONObject(i)));
	}

	public Donation get(int index) {
		return donations.get(index);
	}

	public int length() {
		return donations.size();
	}

	public void sort(Comparator<? super Donation> c) {
		Collections.sort(donations, c);
	}

	public static class ComparatorDonationAmount implements Comparator<Donation> {
		@Override
		public int compare(Donation d1, Donation d2) {
			return Double.compare(d2.getDonationAmount(), d1.getDonationAmount());
		}
	}

	public class Donation {
		private String name;
		private String displayName;
		private double donationAmount;
		private Currency currency;
		private Uri imageUrl;

		Donation(JSONObject donation) throws JSONException {
			name = donation.getString("name");
			displayName = donation.getString("displayName");
			donationAmount = donation.getDouble("donationAmount");
			currency = Currency.getInstance(donation.getString("currency"));
			if (donation.has("image") && donation.getString("image").length() > 0)
				imageUrl = new Uri.Builder()
						.scheme("https")
						.authority("data.sapuseven.com")
						.path("BetterUntis/api.php")
						.appendQueryParameter("method", "getImage")
						.appendQueryParameter("image", donation.getString("image"))
						.build();
		}

		public String getName() {
			return name;
		}

		public String getDisplayName() {
			return displayName;
		}

		public double getDonationAmount() {
			return donationAmount;
		}

		public Currency getCurrency() {
			return currency;
		}

		public Uri getImageUrl() {
			return imageUrl;
		}
	}
}
