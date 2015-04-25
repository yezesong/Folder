package com.mq.folder.common.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

import com.joy.util.Logger;
import com.mq.folder_game_hub.R;
import com.mq.folder.common.utils.UnitUtils;
import com.mq.folder.download.DownloadInfo;
import com.mq.folder.download.DownloadManager;

public class ProgressBarView extends View {

	// private Drawable progressbar;
	private Drawable progressbar_groove;
	private Drawable progressbar_cursor;
	private Drawable progressbar_bg;
	final int offest = 2;

	DownloadInfo downloadInfo;
	public UnitUtils unit;

	int downState;

	public ProgressBarView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		// TODO Auto-generated constructor stub
		unit = UnitUtils.getInstance(context);
	}

	public ProgressBarView(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
		unit = UnitUtils.getInstance(context);
	}

	public ProgressBarView(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
		unit = UnitUtils.getInstance(context);
	}

	public ProgressBarView(Context context, int state) {
		super(context);
		loadImage();
		this.downState = state;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		switch (downState) {
		case DownloadManager.STATUS_NONE:
			drawBackground(canvas);
			break;
		case DownloadManager.STATUS_DOWNLOAD:
			drawProgressBar(canvas, downloadInfo);
			break;
		case DownloadManager.STATUS_PAUSE:
			drawBackground(canvas);
		case DownloadManager.STATUS_STOP:
		default:
			break;
		}
	}

	private void loadImage() {
		// progressbar =
		// getResources().getDrawable(R.drawable.onlinefolder_download_progressbar);
		progressbar_bg = getResources().getDrawable(R.drawable.onlinefolder_download_progressbar_bg);
		progressbar_groove = getResources().getDrawable(R.drawable.onlinefolder_download_progressbar_groove);
		progressbar_cursor = getResources().getDrawable(R.drawable.onlinefolder_download_progressbar_cursor);
	}

	public void invalidate(DownloadInfo downloadInfo, int state) {
		this.downloadInfo = downloadInfo;
		this.downState = state;
		this.invalidate();
	}

	private void drawBackground(Canvas canvas) {
		canvas.save();
		// progressbar.setBounds(0, 0, getMeasuredWidth(), getMeasuredHeight());
		// progressbar.draw(canvas);
		// canvas.restore();
	}

	private void drawProgressBar(Canvas canvas, DownloadInfo downloadInfo) {
		if (downloadInfo == null) {
			return;
		}
		canvas.save();
		int width = getMeasuredWidth();
		int height = getMeasuredHeight();
		progressbar_bg.setBounds(0, 0, width, height);
		progressbar_bg.draw(canvas);

		int maxw = width - 2 * offest;
		int maxh = height / 8;

		int progressbar_left = offest;
		int progressbar_top = (height - progressbar_cursor.getIntrinsicHeight()) / 2;
		int progressbar_right = maxw;
		int progressbar_bottom = progressbar_top + maxh;
		progressbar_groove.setBounds(progressbar_left, progressbar_top, progressbar_right, progressbar_bottom);
		progressbar_groove.draw(canvas);

		Logger.info(this, "-----downloadInfo.getCompletesize() : " + downloadInfo.getCompletesize());
		int w = maxw * downloadInfo.getCompletesize() / downloadInfo.getFilesize();
		if (w < progressbar_cursor.getIntrinsicWidth()) {
			canvas.restore();
			return;
		} else if (w >= maxw) {
			w = maxw;
		}
		progressbar_cursor.setBounds(progressbar_left, progressbar_top, w, progressbar_bottom);
		progressbar_cursor.draw(canvas);
		canvas.restore();
	}

}
