package com.sapuseven.untis.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.sapuseven.untis.R;
import com.sapuseven.untis.utils.DisplayChangelog;
import com.sapuseven.untis.utils.connectivity.DownloadTask;

import java.io.File;
import java.util.Locale;

import static com.sapuseven.untis.utils.ThemeUtils.setupTheme;

public class ActivityAppUpdate extends Activity {
	private static final long TIMEOUT = 10000;
	private final Handler timeout = new Handler();
	private ProgressBar progressBar;
	private TextView tvProgress;
	private Runnable runnable;
	private DownloadTask downloadTask;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setupTheme(this, true);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.outdated);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		final Button btnNotNow = findViewById(R.id.btnUpdateLater);
		final Button btnUpdate = findViewById(R.id.btnUpdate);
		final Button btnViewChangelog = findViewById(R.id.btnViewChangelog);
		final TextView tvCurrentVersion = findViewById(R.id.tvCurrentVersion);
		final TextView tvNewVersion = findViewById(R.id.tvNewVersion);

		btnNotNow.setOnClickListener(view -> {
			Intent intent = new Intent(getApplicationContext(), ActivityMain.class);
			intent.putExtra("disable_update_check", true);
			startActivity(intent);
			finish();
		});

		btnUpdate.setOnClickListener((View view) -> {
			setContentView(R.layout.download);
			progressBar = findViewById(R.id.pbDownload);
			tvProgress = findViewById(R.id.tvDownload);

			try {
				File dir = new File(getExternalCacheDir() + "/update");
				//if (!dir.exists() && !dir.mkdirs())
				//return "Failed to create update-dir";
				File outputFile = new File(dir, "from-v" + getPackageManager()
						.getPackageInfo(getPackageName(), 0).versionCode + ".apk");


				downloadTask = new DownloadTask();

				DownloadTask.ResponseHandler handler = new DownloadTask.ResponseHandler() {
					@Override
					public void onProgressUpdate(Integer... progress) {
						timeout.removeCallbacks(runnable);
						progressBar.setIndeterminate(false);
						progressBar.setMax(100);
						progressBar.setProgress(progress[0]);
						double progressSize = progress[1] / 1024.0 / 1024.0;
						progressSize = Math.round(progressSize * 100) / 100.0;
						double totalSize = progress[2] / 1024.0 / 1024.0;
						totalSize = Math.round(totalSize * 100) / 100.0;
						tvProgress.setText(
								getString(R.string.download_progress,
										Integer.parseInt(Double.toString(progressSize).split("\\.")[0]),
										String.format(Locale.ENGLISH, "%02d", Integer.parseInt(Double.toString(progressSize).split("\\.")[1])),
										Integer.parseInt(Double.toString(totalSize).split("\\.")[0]),
										String.format(Locale.ENGLISH, "%02d", Integer.parseInt(Double.toString(totalSize).split("\\.")[1])))
						);
					}

					@Override
					public void onFileReceived(String response) {
						timeout.removeCallbacks(runnable);
						Log.d("ActivityAppUpdate", "DownloadTask exited with message \"" + response + "\"");
						if (!response.equals("OK"))
							ActivityAppUpdate.this.cancel();
						openFile(outputFile);
					}
				};

				downloadTask.setResponseHandler(handler).submit("https://data.sapuseven.com/BetterUntis/download.php?redirect=1", outputFile.getAbsolutePath());

				runnable = this::cancel;
				timeout.postDelayed(runnable, TIMEOUT);
			} catch (PackageManager.NameNotFoundException e) {
				e.printStackTrace();
			}
		});

		btnViewChangelog.setOnClickListener(view -> new DisplayChangelog(ActivityAppUpdate.this)
				.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, getIntent().getIntExtra("currentVersionCode", 0)));

		tvCurrentVersion.setText(getIntent().getStringExtra("currentVersion"));
		tvNewVersion.setText(getIntent().getStringExtra("newVersion"));
	}

	private void cancel() {
		downloadTask.cancel(true);
		setContentView(R.layout.download_failed);
		Button try_again = findViewById(R.id.btnTryAgain);
		Button manual = findViewById(R.id.btnManualUpdate);
		try_again.setOnClickListener(v -> recreate());
		manual.setOnClickListener(v -> {
			Intent browserIntent = new Intent(Intent.ACTION_VIEW,
					Uri.parse("http://data.sapuseven.com/BetterUntis/download.php"));
			startActivity(browserIntent);
		});
	}

	private void openFile(File file) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			Uri apkUri = FileProvider.getUriForFile(ActivityAppUpdate.this,
					getPackageName() + ".provider", file);
			Intent intent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
			intent.setData(apkUri);
			intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
			ActivityAppUpdate.this.startActivity(intent);
		} else {
			Uri apkUri = Uri.fromFile(file);
			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			ActivityAppUpdate.this.startActivity(intent);
		}
		finish();
	}
}