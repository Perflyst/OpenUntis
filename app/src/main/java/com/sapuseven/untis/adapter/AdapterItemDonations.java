package com.sapuseven.untis.adapter;

import com.sapuseven.untis.utils.DonationManager;
import com.sapuseven.untis.utils.lazyload.ImageLoader;

public class AdapterItemDonations {
	private final DonationManager.Donation donation;
	private final int rank;
	private final ImageLoader imageLoader;

	public AdapterItemDonations(int rank, DonationManager.Donation donation, ImageLoader imageLoader) {
		this.rank = rank;
		this.donation = donation;
		this.imageLoader = imageLoader;
	}

	DonationManager.Donation getDonation() {
		return donation;
	}

	ImageLoader getImageLoader() {
		return imageLoader;
	}

	@Override
	public int hashCode() {
		return donation.getName().hashCode();
	}

	int getRank() {
		return rank;
	}
}
