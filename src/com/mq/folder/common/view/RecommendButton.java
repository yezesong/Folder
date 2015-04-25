package com.mq.folder.common.view;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.joy.network.RecommendShortcutInfo;
import com.mq.folder_game_hub.R;
import com.mq.folder.common.Constants;
import com.mq.folder.common.utils.Util;
import com.mq.folder.context.MainActivity;
import com.mq.folder.download.DownloadInfo;
import com.mq.folder.download.DownloadManager.DownLoadListener;

public class RecommendButton extends LinearLayout implements DownLoadListener {

	ImageView mIcon, pauseImg;
	TextView mTitle;
	RelativeLayout bgLayout;
	ProgressBarView progressBar;
	public DownloadInfo mDownloadInfo;
	public MainActivity fatherView;
	public RecommendShortcutInfo recommendInfo;
	Context mContext;

	public RecommendButton(Context context) {
		super(context);
		this.mContext = context;
	}

	public RecommendButton(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.mContext = context;
	}

	@SuppressLint("NewApi")
	public RecommendButton(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs);
		this.mContext = context;
	}

	public void setFatherView(MainActivity fatherView) {
		this.fatherView = fatherView;
		// fatherView.receiverList.add(appInstalledReceiver);
	}

	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();

		mIcon = (ImageView) findViewById(R.id.recommend_icon);
		mTitle = (TextView) findViewById(R.id.recommend_title);
		pauseImg = (ImageView) findViewById(R.id.recommend_pause);
		bgLayout = (RelativeLayout) findViewById(R.id.recommend_bg);
		progressBar = new ProgressBarView(getContext(), 0);
		bgLayout.addView(progressBar);
		IntentFilter filter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
		filter.addDataScheme("package");
		mContext.registerReceiver(appInstalledReceiver, filter);
		IntentFilter filter2 = new IntentFilter(Constants.ACTION_PROGRESS);
		mContext.registerReceiver(appInstalledReceiver, filter2);
		isRegister = true;
	}

	private boolean isRegister = false;

	public void onDestroy() {
		// 注销广播
		try {
			if (isRegister)
				mContext.unregisterReceiver(appInstalledReceiver);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.widget.LinearLayout#onMeasure(int, int)
	 */
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		@SuppressWarnings("unused")
		int mWith = MeasureSpec.getSize(widthMeasureSpec);
		@SuppressWarnings("unused")
		int mHeight = MeasureSpec.getSize(heightMeasureSpec);
		// Logger.debug(getContext(), "-----onMeasure " + mWith + " |mH " +
		// mHeight);
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		if (progressBar == null) {
			return;
		}

		progressBar.measure(MeasureSpec.makeMeasureSpec(mIcon.getWidth(), MeasureSpec.EXACTLY),
				MeasureSpec.makeMeasureSpec(mIcon.getHeight(), MeasureSpec.EXACTLY));
		progressBar.layout(mIcon.getLeft(), mIcon.getTop(), mIcon.getRight(), mIcon.getBottom());

	}

	public void setDownloadInfo(DownloadInfo dInfo) {
		mDownloadInfo = dInfo;
	}

	public void setShorcutInfo(RecommendShortcutInfo recommendInfo) {
		this.recommendInfo = recommendInfo;
	}

	public RecommendShortcutInfo getShorcutInfo() {
		return recommendInfo;
	}

	public void updateButton() {
		if (progressBar != null && mDownloadInfo != null) {
			progressBar.invalidate(mDownloadInfo, mDownloadInfo.getStatus());
		}
	}

	@Override
	public void downloadSucceed() {
		pauseImg.setVisibility(View.VISIBLE);
		pauseImg.setImageResource(R.drawable.uu_folder_install);
		pauseImg.setTag("success");
		Toast.makeText(getContext(), "downloadSucceed", Toast.LENGTH_SHORT).show();
		downloadUpdate();
		Util.installAPK(getContext(), Constants.DOWNLOAD_APK_DIR, mDownloadInfo.getLocalname());
	}

	@Override
	public void downloadFailed() {
		// Toast.makeText(getContext(), "downloadFailed",
		// Toast.LENGTH_SHORT).show();
		downloadUpdate();
		pauseImg.setVisibility(View.VISIBLE);
		pauseImg.setTag("pause");
	}

	@Override
	public void downloadUpdate() {
		updateButton();
	}

	BroadcastReceiver appInstalledReceiver = new BroadcastReceiver() {

		private static final int PACKAGE_NAME_START_INDEX = 8;

		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent == null)
				return;
			if (intent.getAction().equals(Intent.ACTION_PACKAGE_ADDED)) {
				String data = intent.getDataString();

				if (data == null || data.length() <= PACKAGE_NAME_START_INDEX) {
					return;
				}

				String packageName = data.substring(PACKAGE_NAME_START_INDEX);
				if (packageName.equals(recommendInfo.packageName)) {
					fatherView.onRecommendItemInstalled(recommendInfo);
				}
			} else if (intent.getAction().equals(Constants.ACTION_PROGRESS)) {// 更新进度
				if (Constants.DEBUG)
					Log.e("RecommendButton", "download receiver update...");
				DownloadInfo info = (DownloadInfo) intent.getParcelableExtra("downinfo");
				if (info.getId() != mDownloadInfo.getId())
					return;
				mDownloadInfo = info;
				int what = intent.getIntExtra("status", 0);
				switch (what) {
				case 1:
					downloadUpdate();
					break;
				case 3:
					downloadSucceed();
					break;
				case 2:
					downloadFailed();
					break;
				}
			}
		}
	};
}
