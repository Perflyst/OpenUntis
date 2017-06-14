package com.sapuseven.untis.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.sapuseven.untis.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

import static com.sapuseven.untis.utils.ThemeUtils.setupTheme;

public class ActivityAppUpdate extends Activity {
	private static final long TIMEOUT = 10000;
	private final Handler mTimeout = new Handler();
	private ProgressBar mProgressBar;
	private TextView mTvProgress;
	private DownloadTask mDownloadTask;
	private Runnable mRunnable;
	private File mOutputFile;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setupTheme(this, true);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.outdated);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		final Button btnNotNow = (Button) findViewById(R.id.btnUpdateLater);
		final Button btnUpdate = (Button) findViewById(R.id.btnUpdate);

		btnNotNow.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				Intent intent = new Intent(getApplicationContext(), ActivityMain.class);
				intent.putExtra("disable_update_check", true);
				startActivity(intent);
				finish();
			}
		});
		btnUpdate.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				setContentView(R.layout.download);
				mProgressBar = (ProgressBar) findViewById(R.id.pbDownload);
				mTvProgress = (TextView) findViewById(R.id.tvDownload);

				mDownloadTask = new DownloadTask();
				mDownloadTask
						.execute("https://data.sapuseven.com/BetterUntis/download.php?redirect=1");
				mRunnable = new Runnable() {
					public void run() {
						cancel();
					}
				};
				mTimeout.postDelayed(mRunnable, TIMEOUT);
			}
		});
	}

	private void cancel() {
		mDownloadTask.cancel(true);
		setContentView(R.layout.download_failed);
		Button try_again = (Button) findViewById(R.id.btnTryAgain);
		Button manual = (Button) findViewById(R.id.btnManualUpdate);
		try_again.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				recreate();
			}
		});
		manual.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent browserIntent = new Intent(Intent.ACTION_VIEW,
						Uri.parse("http://sapuseven.com/BetterUntis/download.php"));
				startActivity(browserIntent);
			}
		});
	}

	private class DownloadTask extends AsyncTask<String, Integer, String> {

		private final Context context = getApplicationContext();

		@Override
		protected String doInBackground(String... sUrl) {
			InputStream input = null;
			OutputStream output = null;
			HttpURLConnection connection = null;
			try {
				URL tmpUrl = new URL(sUrl[0]);
				HttpURLConnection urlConnection = (HttpURLConnection) tmpUrl.openConnection();
				urlConnection.setInstanceFollowRedirects(false);
				String s = urlConnection.getHeaderField("Location");
				URL url = new URL(s);

				connection = (HttpURLConnection) url.openConnection();
				connection.connect();

				if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
					return "Server returned HTTP " + connection.getResponseCode()
							+ " " + connection.getResponseMessage();

				int fileLength = connection.getContentLength();

				File dir = new File(context.getExternalCacheDir() + "/update");
				if (!dir.exists() && !dir.mkdirs())
					ActivityAppUpdate.this.cancel();
				mOutputFile = new File(dir, "from-v" + getPackageManager()
						.getPackageInfo(getPackageName(), 0).versionCode + ".apk");
				input = connection.getInputStream();
				output = new FileOutputStream(mOutputFile);

				byte data[] = new byte[4096];
				long total = 0;
				int count;
				while ((count = input.read(data)) != -1) {
					if (isCancelled()) {
						input.close();
						return null;
					}
					total += count;
					if (fileLength > 0)
						publishProgress((int) (total * 100 / fileLength), (int) total, fileLength);
					output.write(data, 0, count);
				}
			} catch (Exception e) {
				ActivityAppUpdate.this.cancel();
				return e.toString();
			} finally {
				try {
					if (output != null)
						output.close();
					if (input != null)
						input.close();
				} catch (IOException ignored) {
				}

				if (connection != null)
					connection.disconnect();
			}
			openFile();
			return "OK";
		}

		@Override
		protected void onProgressUpdate(Integer... progress) {
			mTimeout.removeCallbacks(mRunnable);
			super.onProgressUpdate(progress);
			mProgressBar.setIndeterminate(false);
			mProgressBar.setMax(100);
			mProgressBar.setProgress(progress[0]);
			double progressSize = progress[1] / 1024.0 / 1024.0;
			progressSize = Math.round(progressSize * 100) / 100.0;
			double totalSize = progress[2] / 1024.0 / 1024.0;
			totalSize = Math.round(totalSize * 100) / 100.0;
			mTvProgress.setText(
					getString(R.string.download_progress,
							Integer.parseInt(Double.toString(progressSize).split("\\.")[0]),
							String.format(Locale.ENGLISH, "%02d", Integer.parseInt(Double.toString(progressSize).split("\\.")[1])),
							Integer.parseInt(Double.toString(totalSize).split("\\.")[0]),
							String.format(Locale.ENGLISH, "%02d", Integer.parseInt(Double.toString(totalSize).split("\\.")[1])))
			);
		}

		private void openFile() {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
				Uri apkUri = FileProvider.getUriForFile(ActivityAppUpdate.this,
						context.getPackageName() + ".provider", mOutputFile);
				Intent intent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
				intent.setData(apkUri);
				intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
				ActivityAppUpdate.this.startActivity(intent);
			} else {
				Uri apkUri = Uri.fromFile(mOutputFile);
				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				ActivityAppUpdate.this.startActivity(intent);
			}
			finish();
		}

		@Override
		protected void onPostExecute(String s) {
			Log.d("ActivityAppUpdate", "DownloadTask exited with message \"" + s + "\"");
		}
	}
}