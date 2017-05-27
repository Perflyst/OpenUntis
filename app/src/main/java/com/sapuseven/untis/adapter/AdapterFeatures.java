package com.sapuseven.untis.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.os.AsyncTask;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.sapuseven.untis.R;
import com.sapuseven.untis.utils.FeatureInfo;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import static android.content.Context.MODE_PRIVATE;
import static android.os.AsyncTask.THREAD_POOL_EXECUTOR;
import static com.sapuseven.untis.utils.ThemeUtils.tintDrawable;

public class AdapterFeatures extends BaseAdapter {

	private static LayoutInflater inflater = null;
	private final Context context;
	private final List<FeatureInfo> data;

	public AdapterFeatures(Context context, List<FeatureInfo> data) {
		this.context = context;
		this.data = data;
		inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override
	public int getCount() {
		return data.size();
	}

	@Override
	public Object getItem(int position) {
		return data.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@SuppressLint("SetTextI18n")
	@Override
	public View getView(final int position, final View convertView, ViewGroup parent) {
		View v = convertView;
		if (v == null)
			v = inflater.inflate(R.layout.list_item_features, parent, false);
		final TextView tvHeader = (TextView) v.findViewById(R.id.tvTitle);
		final TextView tvDesc = (TextView) v.findViewById(R.id.tvDesc);
		final TextView tvLikes = (TextView) v.findViewById(R.id.tvLikes);
		final ImageButton btnLike = (ImageButton) v.findViewById(R.id.btnLike);
		final ImageButton btnDislike = (ImageButton) v.findViewById(R.id.btnDislike);
		tvHeader.setText(data.get(position).getTitle());
		tvDesc.setText(data.get(position).getDesc());
		final int voteOffset = data.get(position).getHasVoted();
		tvLikes.setText(Integer.toString(data.get(position).getLikes()));

		if (data.get(position).getHasVoted() == 1)
			tintDrawable(context, btnLike.getDrawable(), R.attr.colorPrimary);
		else if (data.get(position).getHasVoted() == -1)
			tintDrawable(context, btnDislike.getDrawable(), R.attr.colorPrimary);

		//noinspection ResourceType
		btnLike.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (data.get(position).getHasVoted() != 1) {
					int[] attrs = new int[]{R.attr.colorPrimary, android.R.attr.textColorPrimary};
					TypedArray ta = context.obtainStyledAttributes(attrs);
					int colored = ta.getColor(0, 0);
					int uncolored = ta.getColor(1, 0);
					ta.recycle();
					DrawableCompat.setTint(btnLike.getDrawable(), colored);
					DrawableCompat.setTint(btnDislike.getDrawable(), uncolored);
					tvLikes.setText(Integer.toString(data.get(position).getLikes() - voteOffset + 1));
					new vote().executeOnExecutor(THREAD_POOL_EXECUTOR, data.get(position).getId(), 1);
					data.get(position).setHasVoted(1);
				}
			}
		});
		//noinspection ResourceType
		btnDislike.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (data.get(position).getHasVoted() != -1) {
					int[] attrs = new int[]{R.attr.colorPrimary, android.R.attr.textColorPrimary};
					TypedArray ta = context.obtainStyledAttributes(attrs);
					int colored = ta.getColor(0, 0);
					int uncolored = ta.getColor(1, 0);
					ta.recycle();
					DrawableCompat.setTint(btnLike.getDrawable(), uncolored);
					DrawableCompat.setTint(btnDislike.getDrawable(), colored);
					tvLikes.setText(Integer.toString(data.get(position).getLikes() - voteOffset - 1));
					new vote().executeOnExecutor(THREAD_POOL_EXECUTOR, data.get(position).getId(), -1);
					data.get(position).setHasVoted(-1);
				}
			}
		});
		return v;
	}

	private String readStream(InputStream is) {
		try {
			ByteArrayOutputStream bo = new ByteArrayOutputStream();
			int i = is.read();
			while (i != -1) {
				bo.write(i);
				i = is.read();
			}
			return bo.toString();
		} catch (IOException e) {
			e.printStackTrace();
			return "";
		}
	}

	private class vote extends AsyncTask<Integer, Void, Boolean> {
		@Override
		protected Boolean doInBackground(Integer... integers) {
			try {
				SharedPreferences prefs = context.getSharedPreferences("login_data", MODE_PRIVATE);
				String user = prefs.getString("user", "");
				URL url = new URL("https://data.sapuseven.com/BetterUntis/api.php?method=addVoteToFeature&id=" + integers[0] + "&vote=" + integers[1] + "&name=" + user);
				HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
				BufferedInputStream in = new BufferedInputStream(urlConnection.getInputStream());
				String str = readStream(in);
				JSONObject list = new JSONObject(str);
				urlConnection.disconnect();
				return list.optString("result").equals("OK");
			} catch (JSONException | IOException e) {
				e.printStackTrace();
				return false;
			}
		}

		@Override
		protected void onPostExecute(Boolean success) {
			if (success) {
				Toast.makeText(context, R.string.toast_vote_counted, Toast.LENGTH_SHORT).show();
			} else {
				Toast.makeText(context, R.string.toast_error_occurred, Toast.LENGTH_SHORT).show();
			}
		}
	}
}