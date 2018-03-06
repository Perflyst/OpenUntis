package com.sapuseven.untis.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.sapuseven.untis.R;
import com.sapuseven.untis.adapter.AdapterDonations;
import com.sapuseven.untis.adapter.AdapterItemDonations;
import com.sapuseven.untis.utils.ApiRequest;
import com.sapuseven.untis.utils.DonationManager;
import com.sapuseven.untis.utils.lazyload.ImageLoader;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static com.sapuseven.untis.utils.Conversions.dp2px;
import static com.sapuseven.untis.utils.ThemeUtils.setupTheme;

public class ActivityDonations extends AppCompatActivity {
	public static String formatDonationAmount(DonationManager.Donation donation) {
		return String.format(Locale.getDefault(), "%.2f " + donation.getCurrency().getSymbol(), donation.getDonationAmount());
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setupTheme(this, true);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_donations);

		ImageLoader imageLoader = new ImageLoader(this);

		ApiRequest api = new ApiRequest(this);

		Map<String, String> params = new HashMap<>();
		params.put("method", "getDonations");

		ApiRequest.ResponseHandler handler = response -> {
			try {
				DonationManager donations = new DonationManager(new JSONObject(response).getJSONObject("result").getJSONArray("donations"));
				donations.sort(new DonationManager.ComparatorDonationAmount());

				int[] viewGroups = {R.id.vgFirst, R.id.vgSecond, R.id.vgThird};

				for (int i = 0; i < 3; i++) {
					ViewGroup vg = findViewById(viewGroups[i]);

					((TextView) vg.findViewById(R.id.tvRank)).setText(String.format("%1$s", i + 1));
					((TextView) vg.findViewById(R.id.tvName)).setText(donations.get(i).getDisplayName());
					((TextView) vg.findViewById(R.id.tvDonationAmount)).setText(formatDonationAmount(donations.get(i)));

					if (i == 0)
						vg.findViewById(R.id.ivAvatar).setLayoutParams(
								new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp2px(96)));

					if (donations.get(i).getImageUrl() != null)
						imageLoader.displayImage(donations.get(i).getImageUrl().toString(), vg.findViewById(R.id.ivAvatar), R.drawable.ic_prefs_personal);
				}

				ArrayList<AdapterItemDonations> donationList = new ArrayList<>();

				for (int i = 3; i < donations.length(); i++)
					donationList.add(new AdapterItemDonations(i + 1, donations.get(i), imageLoader));

				ListView lvTopDonations = findViewById(R.id.lvTopDonations);
				lvTopDonations.setAdapter(new AdapterDonations(this, donationList));
				lvTopDonations.setDividerHeight(0);
			} catch (JSONException e) {
				e.printStackTrace();
			}

			setDonationVisibility(true);
		};

		api.setResponseHandler(handler).submit(params);
	}

	public void setDonationVisibility(boolean visibility) {
		findViewById(R.id.pbLoading).setVisibility(visibility ? View.GONE : View.VISIBLE);
		findViewById(R.id.lvDonations).setVisibility(visibility ? View.VISIBLE : View.GONE);
	}
}