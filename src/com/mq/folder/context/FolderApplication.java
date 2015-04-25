package com.mq.folder.context;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import com.joy.network.impl.Service;
import com.joy.network.util.SystemInfo;
import com.mq.folder.common.Constants;
import com.mq.folder.common.utils.UnitUtils;
import com.mq.folder.common.utils.Util;
import com.mq.folder.download.DownloadManager;

public class FolderApplication extends Application {
	public static Context mContext;
	private UnitUtils unit;
	public static int screenWidth, screenHeight, bestItemDb, bestItemPx;

	@Override
	public void onCreate() {
		super.onCreate();
		mContext = this;
		getBestWidth();
		Service.getInstance(this, false);
		DownloadManager.getInstance(this);
		SystemInfo.getInstance(Constants.DEFAULT_CHANNL, 0, false);
		final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
		if (sp.getBoolean("isActive", false))
			new Thread() {
				public void run() {
					if (Util.isNetworkConnected(mContext)) {
						boolean isActive = Service.getInstance().activateLauncher();
						if (isActive) {
							sp.edit().putBoolean("isActive", true).commit();
						}
					}
				};
			}.start();
	}

	private void getBestWidth() {
		DisplayMetrics metrics = new DisplayMetrics();
		WindowManager windowMgr = (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
		windowMgr.getDefaultDisplay().getMetrics(metrics);
		screenHeight = metrics.heightPixels;
		screenWidth = metrics.widthPixels;
		unit = UnitUtils.getInstance(mContext);
		bestItemDb = (unit.px2dip(screenWidth) - 100) / 4;
		bestItemDb = unit.dip2px(bestItemDb);
		bestItemPx = screenWidth / 4 - unit.dip2px(30);
	}
}
