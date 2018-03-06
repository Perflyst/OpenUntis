package com.sapuseven.untis.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.sapuseven.untis.R;
import com.sapuseven.untis.activity.ActivityDonations;

import java.util.ArrayList;

public class AdapterDonations extends BaseAdapter {
	private final LayoutInflater inflater;
	private final ArrayList<AdapterItemDonations> donationList;

	public AdapterDonations(ActivityDonations activity, ArrayList<AdapterItemDonations> donatorList) {
		this.donationList = donatorList;
		inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override
	public int getCount() {
		return donationList.size();
	}

	@Override
	public Object getItem(int position) {
		return donationList.get(position);
	}

	@Override
	public long getItemId(int position) {
		return donationList.get(position).hashCode();
	}

	@Override
	public View getView(final int position, final View convertView, ViewGroup parent) {
		View view = convertView;
		if (view == null)
			view = inflater.inflate(R.layout.list_item_donations, parent, false);

		final TextView tvRank = view.findViewById(R.id.tvRank);
		final TextView tvName = view.findViewById(R.id.tvName);
		final TextView tvDonationAmount = view.findViewById(R.id.tvDonationAmount);
		final ImageView ivAvatar = view.findViewById(R.id.ivAvatar);

		tvRank.setText(String.valueOf(donationList.get(position).getRank()));
		tvName.setText(donationList.get(position).getDonation().getDisplayName());
		tvDonationAmount.setText(ActivityDonations.formatDonationAmount(donationList.get(position).getDonation()));
		if (donationList.get(position).getDonation().getImageUrl() != null)
			donationList.get(position).getImageLoader().displayImage(
					donationList.get(position).getDonation().getImageUrl().toString(),
					ivAvatar,
					R.drawable.ic_prefs_personal);
		else
			ivAvatar.setImageResource(R.drawable.ic_prefs_personal);
		return view;
	}
}