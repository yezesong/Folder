package com.mq.folder.common.cache;

import java.lang.ref.SoftReference;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.LruCache;

public class ImageMemoryCache {

	private final static int SOFT_CACHE_SIZE = 15;
	private LruCache<String, Bitmap> mLruCache;
	private Map<String, SoftReference<Bitmap>> mSoftCache;

	public ImageMemoryCache(Context context) {
		int cacheSize = (int) (Runtime.getRuntime().maxMemory() / 8);
		mLruCache = new LruCache<String, Bitmap>(cacheSize) {

			@Override
			protected void entryRemoved(boolean evicted, String key, Bitmap oldValue, Bitmap newValue) {
				if (evicted && oldValue != null && !oldValue.isRecycled()) {
					oldValue.recycle();
					oldValue = null;
				}

			}

			@Override
			protected int sizeOf(String key, Bitmap value) {
				if (value != null) {
					int size = value.getRowBytes() * value.getHeight();
					return size;
				} else
					return 0;
			}

		};

		mSoftCache = Collections.synchronizedMap(new LinkedHashMap<String, SoftReference<Bitmap>>() {
			private static final long serialVersionUID = 6040103833179403725L;

			@Override
			protected boolean removeEldestEntry(Entry<String, SoftReference<Bitmap>> eldest) {
				if (size() > SOFT_CACHE_SIZE) {

					return true;
				}
				return false;
			}
		});

	}

	public Bitmap getBitmapFromCache(String url) {
		Bitmap bitmap;
		bitmap = mLruCache.get(url);
		if (bitmap != null) {
			mLruCache.remove(url);
			mLruCache.put(url, bitmap);
			return bitmap;
		}
		SoftReference<Bitmap> bitmapReference = mSoftCache.get(url);
		if (bitmapReference != null) {
			bitmap = bitmapReference.get();
			if (bitmap != null) {
				mLruCache.put(url, bitmap);
				mSoftCache.remove(url);
				return bitmap;
			} else {
				mSoftCache.remove(url);
			}
		}
		return null;
	}

	public void addBitmapToCache(String url, Bitmap bitmap) {
		if (bitmap != null) {
			mLruCache.put(url, bitmap);
		}
	}

	public void clearCache() {
		if (mSoftCache != null)
			mSoftCache.clear();
		if (mLruCache != null)
			mLruCache.evictAll();
	}

	public String toString() {
		return mLruCache.size() + "   " + mSoftCache.size();
	}

}
