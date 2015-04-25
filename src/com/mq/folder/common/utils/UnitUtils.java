package com.mq.folder.common.utils;

import android.app.Activity;
import android.content.Context;

/**
 * 
 * @author yarine.gao
 * @date 2012-7-3
 * @version 1.0.0
 */
public class UnitUtils {

	private static float scale = -1f;

	// private static WindowManager wm = null;

	public static UnitUtils getInstance(Context context) {
		if (scale == -1f) {
			scale = context.getResources().getDisplayMetrics().density;
		}

		return new UnitUtils();
	}

	public float getScale(Context context) {
		if (scale == -1f) {
			scale = context.getResources().getDisplayMetrics().density;
		}
		return scale;
	}

	public int px2dip(int px) {
		return (int) (px / scale + 0.5f);
	}

	public int dip2px(int dip) {
		return (int) (dip * scale + 0.5f);
	}

	@SuppressWarnings("deprecation")
	public static int getDisplayWidth(Activity context) {
		return context.getWindowManager().getDefaultDisplay().getWidth();
	}

	@SuppressWarnings("deprecation")
	public static int getDisplayHeight(Activity context) {
		return context.getWindowManager().getDefaultDisplay().getHeight();
	}
}
