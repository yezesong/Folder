package com.mq.folder.common.cache;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.Hashtable;
import java.util.Map;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.joy.network.impl.Service;
import com.joy.network.util.AsyncTask;
import com.joy.network.util.AsyncTask.CallBack;
import com.joy.util.Logger;
import com.joy.util.Util;
import com.mq.folder.common.Constants;

public class BitmapCaches {

	private static BitmapCaches cache;

	private Map<String, MySoftRef> hashRefs;

	private ReferenceQueue<Bitmap> queue;

	private Service mService;

	private class MySoftRef extends SoftReference<Bitmap> {
		public String key;

		public MySoftRef(Bitmap bmp, ReferenceQueue<Bitmap> queue, String key) {
			super(bmp, queue);
			this.key = key;
		}
	}

	private BitmapCaches() {
		hashRefs = new Hashtable<String, MySoftRef>();
		queue = new ReferenceQueue<Bitmap>();
		try {
			mService = Service.getInstance();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static BitmapCaches getInstance() {
		if (cache == null) {
			cache = new BitmapCaches();
		}
		return cache;
	}

	private void addCacheBitmap(String key, Bitmap bmp) {
		cleanCache();
		MySoftRef ref = new MySoftRef(bmp, queue, key);
		hashRefs.put(key, ref);
	}

	@SuppressWarnings("deprecation")
	private void setBitmap(View view, Bitmap bm) {
		if (view == null || bm == null) {
		} else if (view instanceof ImageView) {
			ImageView iv = (ImageView) view;
			iv.setImageBitmap(bm);
		} else if (view instanceof LinearLayout) {
			LinearLayout ll = (LinearLayout) view;
			ll.setBackgroundDrawable(new BitmapDrawable(bm));
		} else {
			Logger.warn(this, "setBitmap--!imageview|linearlayout");
		}
	}

	public void getBitmap(final String key, final ImageDownLoadCallback imageDownLoadCallback) {

		getBitmap(key, null, imageDownLoadCallback);
	}

	public void getBitmap(final String url, final View view, final ImageDownLoadCallback imageDownLoadCallback,
			final String suffix) {
		final String key = getFileNameByUrl(url, "?");
		Bitmap bm = null;
		if (hashRefs.containsKey(key)) {
			MySoftRef ref = hashRefs.get(key);
			bm = ref.get();
			if (bm != null) {
				Logger.info(this, "getBitmap,cache");
				setBitmap(view, bm);
				if (imageDownLoadCallback != null) {
					imageDownLoadCallback.imageDownLoaded(bm);
				}
				return;
			}
		}

		Integer resId = null;
		try {
			resId = Integer.parseInt(key);
		} catch (Exception e) {
		}
		if (resId != null) {
			bm = Util.getBitmapById(resId);
			if (bm != null) {
				Logger.info(this, "getBitmap,from drawable" + key);
				addCacheBitmap(key, bm);
				setBitmap(view, bm);
				if (imageDownLoadCallback != null) {
					imageDownLoadCallback.imageDownLoaded(bm);
				}
				return;
			}
		}

		String imageName = key + suffix;
		try {
			bm = BitmapFactory.decodeStream(new FileInputStream(Constants.DOWNLOAD_IMAGE_DIR + "/" + imageName));
		} catch (OutOfMemoryError e) {
			Logger.warn(this, "getBitmap--OutOfMemoryError:" + Constants.DOWNLOAD_IMAGE_DIR + "/" + imageName);
		} catch (Exception e) {
		}
		if (bm != null) {
			Logger.info(this, "getBitmap,sd" + key);
			addCacheBitmap(key, bm);
			setBitmap(view, bm);
			if (imageDownLoadCallback != null) {
				imageDownLoadCallback.imageDownLoaded(bm);
			}
			return;
		}

		AsyncTask.getInstance().run(new CallBack() {
			Bitmap bitmap = null;

			@Override
			public void onPreExecute() {

			}

			@Override
			public void onPostExecute() {
				setBitmap(view, bitmap);
				if (imageDownLoadCallback != null) {
					imageDownLoadCallback.imageDownLoaded(bitmap);
				}
			}

			@Override
			public void doInBackground() {
				bitmap = mService.getBitmapByUrl(url);
				if (bitmap != null) {
					Logger.info(this, "getBitmap,network:" + url);
					addCacheBitmap(key, bitmap);
					saveBitmapToSD(key, bitmap, suffix);
				} else {
				}
			}
		});
	}

	@SuppressLint("DefaultLocale")
	private void saveBitmapToSD(String key, Bitmap bm, String suffix) {

		if (Util.hasSdcard() && bm != null) {
			try {
				String fileName = key + suffix;
				File file = new File(Constants.DOWNLOAD_IMAGE_DIR + "/" + fileName);
				if (!file.getParentFile().exists()) {
					file.getParentFile().mkdirs();
				}
				FileOutputStream fos = new FileOutputStream(file);
				if (fileName.toUpperCase().endsWith(".PNG")) {
					bm.compress(CompressFormat.PNG, 100, fos);
				} else {
					bm.compress(CompressFormat.JPEG, 100, fos);
				}
			} catch (FileNotFoundException e) {
				Logger.info(this, "FileNotFoundException :" + e);
			}
		}

	}

	public void getBitmap(final String key, final View view, final ImageDownLoadCallback imageDownLoadCallback) {
		getBitmap(key, view, imageDownLoadCallback, ".png");
	}

	private void cleanCache() {
		MySoftRef ref = null;
		while ((ref = (MySoftRef) queue.poll()) != null) {
			hashRefs.remove(ref.key);
		}
	}

	private String getFileNameByUrl(String url, String split) {
		if (url == null || "".equals(url.trim())) {
			return null;
		}
		return url.substring(url.lastIndexOf(split) + 1);
	}

	public void clearCache() {
		cleanCache();
		hashRefs.clear();
		hashRefs = null;
		System.runFinalization();
	}
}
