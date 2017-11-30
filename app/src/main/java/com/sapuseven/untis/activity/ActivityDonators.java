package com.sapuseven.untis.activity;

import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.sapuseven.untis.R;
import com.sapuseven.untis.adapter.AdapterDonators;
import com.sapuseven.untis.adapter.AdapterItemDonators;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Locale;

import static com.sapuseven.untis.utils.ThemeUtils.setupTheme;

public class ActivityDonators extends AppCompatActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setupTheme(this, true);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_donators);

		try {
			JSONArray donators = new JSONArray("[\n" +
					"{\"name\":\"Stefan Schindlauer\", \"displayName\":\"Schindi\", \"donationAmount\":1.2, \"currency\":\"EUR\"},\n" +
					"{\"name\":\"Valentin Girbl\", \"displayName\":\"Valle\", \"donationAmount\":10, \"currency\":\"EUR\"},\n" +
					"{\"name\":\"Tobias Buttinger\", \"displayName\":\"Buttinger\", \"donationAmount\":10.11, \"currency\":\"EUR\"},\n" +
					"{\"name\":\"Michael Übertsberger\", \"displayName\":\"Michi Ü.\", \"donationAmount\":1.44, \"currency\":\"EUR\"},\n" +
					"{\"name\":\"Jürgen Karer\", \"displayName\":\"Jochen\", \"donationAmount\":1.19, \"currency\":\"EUR\"},\n" +
					"{\"name\":\"Jonas Wechselberger\", \"displayName\":\"Jonas\", \"donationAmount\":1.02, \"currency\":\"EUR\"},\n" +
					"{\"name\":\"Philipp Kreuzbichler\", \"displayName\":\"Flipsi\", \"donationAmount\":0.41, \"currency\":\"EUR\"},\n" +
					"{\"name\":\"Bastian Thöny\", \"displayName\":\"Basti\", \"donationAmount\":1, \"currency\":\"EUR\"},\n" +
					"{\"name\":\"Benedict Hohenwallner\", \"displayName\":\"Bene\", \"donationAmount\":1, \"currency\":\"EUR\"}]");

			ViewGroup vgFirst = findViewById(R.id.vgFirst);
			ViewGroup vgSecond = findViewById(R.id.vgSecond);
			ViewGroup vgThird = findViewById(R.id.vgThird);

			((TextView) vgFirst.findViewById(R.id.tvRank)).setText("1");
			vgFirst.findViewById(R.id.ivAvatar).setLayoutParams(
					new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp2px(96)));
			((TextView) vgFirst.findViewById(R.id.tvName)).setText(donators.getJSONObject(0).getString("displayName"));
			((TextView) vgFirst.findViewById(R.id.tvDonationAmount)).setText(String.format(Locale.getDefault(), "%.2f €", donators.getJSONObject(0).getDouble("donationAmount")));

			((TextView) vgSecond.findViewById(R.id.tvRank)).setText("2");
			((TextView) vgSecond.findViewById(R.id.tvName)).setText(donators.getJSONObject(1).getString("displayName"));
			((TextView) vgSecond.findViewById(R.id.tvDonationAmount)).setText(String.format(Locale.getDefault(), "%.2f €", donators.getJSONObject(1).getDouble("donationAmount")));

			((TextView) vgThird.findViewById(R.id.tvRank)).setText("3");
			((TextView) vgThird.findViewById(R.id.tvName)).setText(donators.getJSONObject(2).getString("displayName"));
			((TextView) vgThird.findViewById(R.id.tvDonationAmount)).setText(String.format(Locale.getDefault(), "%.2f €", donators.getJSONObject(2).getDouble("donationAmount")));

			ArrayList<AdapterItemDonators> donatorList = new ArrayList<>();

			for (int i = 3; i < donators.length(); i++)
				donatorList.add(new AdapterItemDonators(this, String.valueOf(i + 1), donators.getJSONObject(i).getString("displayName"),
						String.format(Locale.getDefault(), "%.2f €", donators.getJSONObject(i).getDouble("donationAmount")),
						ContextCompat.getDrawable(this, R.drawable.ic_prefs_personal)));

			ListView lvTopDonators = findViewById(R.id.lvTopDonators);
			lvTopDonators.setAdapter(new AdapterDonators(this, donatorList));
			lvTopDonators.setDividerHeight(0);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	private int dp2px(int dp) {
		return (int) (dp * this.getResources().getDisplayMetrics().density + 0.5f);
	}
}