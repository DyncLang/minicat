package com.fanfou.app.hd;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextPaint;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.fanfou.app.hd.R;
import com.fanfou.app.hd.service.Constants;
import com.fanfou.app.hd.service.DownloadService;
import com.fanfou.app.hd.service.VersionInfo;
import com.fanfou.app.hd.util.Utils;

/**
 * @author mcxiaoke
 * @version 1.0 2011.10.25
 * @version 2.0 2011.10.27
 * @version 2.5 2011.10.31
 * 
 */
public class NewVersionPage extends Activity implements View.OnClickListener {
	private TextView mTitleView;
	private TextView mTextView;
	private Button mButton1;
	private Button mButton2;

	private VersionInfo mVersionInfo;

	protected void setBlurEffect() {
		Window window = getWindow();
		WindowManager.LayoutParams lp = window.getAttributes();
		// lp.alpha=0.8f;
		lp.dimAmount = 0.6f;
		window.setAttributes(lp);
		window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
		// window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Utils.initScreenConfig(this);
		parseIntent(getIntent());
		setLayout();
		updateUI();

	}

	private void parseIntent(Intent intent) {
		mVersionInfo = intent.getParcelableExtra(Constants.EXTRA_DATA);
	}

	private void setLayout() {
		setContentView(R.layout.newversion);

		mTitleView = (TextView) findViewById(R.id.title);
		TextPaint tp = mTitleView.getPaint();
		tp.setFakeBoldText(true);

		mTextView = (TextView) findViewById(R.id.text);

		mButton1 = (Button) findViewById(R.id.button1);
		mButton1.setText("立即升级");
		mButton1.setOnClickListener(this);

		mButton2 = (Button) findViewById(R.id.button2);
		mButton2.setText("以后再说");
		mButton2.setOnClickListener(this);

	}

	private void updateUI() {
		mTitleView.setText("发现新版本，是否升级？");
		mTextView.setText(buildText(mVersionInfo));
	}

	private static String buildText(VersionInfo info) {
		if (info == null) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		sb.append("\n最新版本： ").append(info.versionName).append("(Build")
				.append(info.versionCode).append(")");
		sb.append("\n更新日期：").append(info.releaseDate);
		sb.append("\n更新级别：").append(info.forceUpdate ? "重要更新" : "一般更新");
		sb.append("\n\n更新内容：\n").append(info.changelog);
		return sb.toString();
	}

	@Override
	public void onClick(View v) {
		int id = v.getId();
		switch (id) {
		case R.id.button1:
			if (mVersionInfo != null) {
				DownloadService.startDownload(this, mVersionInfo.downloadUrl);
			}
			finish();
			break;
		case R.id.button2:
			finish();
			break;
		default:
			break;
		}
	}

}
