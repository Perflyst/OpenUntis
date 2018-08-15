package com.sapuseven.untis.activity;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.Button;

import com.sapuseven.untis.R;

public class ActivityLogin extends AppCompatActivity {

	private static final int REQUEST_SCAN_CODE = 0x00000001;
	private static final int REQUEST_LOGIN = 0x00000002;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);
		Toolbar toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		Button btnScanCode = findViewById(R.id.btnScanCode);
		Button btnManualDataInput = findViewById(R.id.btnManualDataInput);

		btnManualDataInput.setOnClickListener(v -> {
			Intent i = new Intent(ActivityLogin.this, ActivityLoginDataInput.class);
			startActivityForResult(i, REQUEST_LOGIN);
		});

		btnScanCode.setOnClickListener(v -> {
			try {
				Intent intent = new Intent("com.google.zxing.client.android.SCAN");
				intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
				startActivityForResult(intent, REQUEST_SCAN_CODE);
			} catch (ActivityNotFoundException | SecurityException e) {
					Intent i = new Intent(Intent.ACTION_VIEW);
					i.setData(Uri.parse("https://f-droid.org/en/packages/com.google.zxing.client.android/"));
					startActivity(i);
			}
		});
	}

	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		switch (requestCode) {
			case REQUEST_SCAN_CODE:
				if (resultCode == RESULT_OK) {
					Intent i = new Intent(ActivityLogin.this, ActivityLoginDataInput.class);
					i.setData(Uri.parse(intent.getStringExtra("SCAN_RESULT")));
					startActivityForResult(i, REQUEST_LOGIN);
				}
				break;
			case REQUEST_LOGIN:
				Intent i = new Intent(ActivityLogin.this, ActivityMain.class);
				startActivity(i);
				finish();
				break;
		}
	}
}