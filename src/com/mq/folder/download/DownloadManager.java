package com.mq.folder.download;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.widget.Toast;

import com.joy.network.impl.Service;
import com.joy.util.Logger;
import com.joy.util.Util;
import com.mq.folder.common.Constants;

public class DownloadManager {

	private ExecutorService pool = Executors.newFixedThreadPool(3);

	public static Map<String, DownLoadTask> map = new HashMap<String, DownLoadTask>();

	private Service mService;

	static DownloadManager mDownloadManager;

	Context mContext;

	UpateHandler mUpateHandler;

	public static final int STATUS_NONE = 0;
	public static final int STATUS_DOWNLOAD = 1;
	public static final int STATUS_PAUSE = 2;
	public static final int STATUS_STOP = 3;

	private DownloadManager(Context context) {
		mContext = context;
		mService = Service.getInstance();
		mUpateHandler = new UpateHandler();
	}

	public static DownloadManager getInstance() {
		if (mDownloadManager == null) {
			Logger.error("DownloadManager", "mDownloadManager is null");
		}
		return mDownloadManager;
	}

	public static DownloadManager getInstance(Context context) {
		if (mDownloadManager == null) {
			mDownloadManager = new DownloadManager(context);
		}
		return mDownloadManager;
	}

	private DownLoadDBHelper helper;

	public void createTask(DownLoadListener listener, DownloadInfo dInfo, boolean secretly) {
		if (!Util.hasSdcard()) {
			if (!secretly) {
				Toast.makeText(mContext, "insert_sd_card", Toast.LENGTH_SHORT).show();
			}
			return;
		}
		if (dInfo == null) {
			return;
		}
		if (isDownloading(dInfo)) {
			Logger.info(this, "is downloading,please wait for a moment");
			return;
		}

		Logger.info(this, "getCompletesize start:" + dInfo.getCompletesize());

		if (dInfo.getCompletesize() == 0) {

			File localfile = new File(Constants.DOWNLOAD_APK_DIR + "/" + dInfo.getFilename() + ".apk");
			localfile = Util.getCleverFileName(localfile);
			dInfo.setLocalname(localfile.getName());
			// 写入数据库
			helper = DownLoadDBHelper.getInstances(mContext);
			helper.insert(dInfo);
		}

		File file = new File(Constants.DOWNLOAD_APK_DIR + "/" + dInfo.getLocalname());

		RandomAccessFile rf = null;
		try {
			rf = new RandomAccessFile(file, "rwd");
			rf.setLength(dInfo.getFilesize() * 1024);
			rf.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		DownLoadTask downloader = new DownLoadTask(dInfo, file, listener, secretly);

		map.put(String.valueOf(dInfo.getId()), downloader);

		pool.execute(downloader);
	}

	public void setDownloadListener(DownloadInfo dInfo, DownLoadListener listener) {
		DownLoadTask task = map.get(String.valueOf(dInfo.getId()));
		if (task != null) {
			task.setDownloadListener(dInfo, listener);
		}
	}

	public class DownLoadTask extends Thread {

		private boolean isSecretly;
		private File file;
		private DownloadInfo downinfo;
		private DownLoadListener mListener;
		public boolean isPause = false;

		public DownLoadTask(DownloadInfo downinfo, File file, DownLoadListener listener, boolean secretly) {
			this.downinfo = downinfo;
			this.file = file;
			this.mListener = listener;
			isSecretly = secretly;
			downinfo.setStatus(STATUS_DOWNLOAD);
			mUpateHandler.update(mListener, downinfo, STATUS_DOWNLOAD);
		}

		public DownloadInfo getDownloadInfo() {
			return downinfo;
		}

		public void setDownloadListener(DownloadInfo dInfo, DownLoadListener listener) {
			if (downinfo != dInfo) {
				downinfo = dInfo;
			}
			if (mListener != listener) {
				mListener = listener;
			}
		}

		public boolean isSecretly() {
			return isSecretly;
		}

		public void run() {

			InputStream is = null;
			RandomAccessFile randomAccessFile = null;
			try {
				int startPos = downinfo.getCompletesize() * 1024;
				int endPos = downinfo.getFilesize() * 1024;
				is = mService.getDownLoadInputStream(downinfo.getUrl(), startPos, endPos);
				if (is == null) {
					return;
				}
				randomAccessFile = new RandomAccessFile(file, "rwd");

				boolean isBreakPoint = mService.getIsBreakPoint(downinfo.getUrl());
				if (!isBreakPoint) {
					downinfo.setCompletesize(0);
					startPos = 0;
				}

				randomAccessFile.seek(startPos);

				final int length = 1024 * 8;
				byte[] b = new byte[length];
				int len = -1;
				int pool = 0;

				int tempLen = startPos;
				// mUpateHandler.update(mListener, downinfo, STATUS_DOWNLOAD);
				while ((len = is.read(b)) != -1) {
					if (isPause) {
						return;
					}

					randomAccessFile.write(b, 0, len);

					pool += len;
					if (pool >= 50 * 1024) {
						Logger.info(this, "downloading");
						updateDownLoadInfo(downinfo);
						pool = 0;
					}
					tempLen += len;
					downinfo.setCompletesize(tempLen / 1024);
					mUpateHandler.update(mListener, downinfo, STATUS_DOWNLOAD);
					if (pool != 0) {
						updateDownLoadInfo(downinfo);
					}
				}
			} catch (Exception e) {
				Logger.info(this, "DownLoadTask error " + e);
			} finally {
				Logger.info(this, "download over");

				if (downinfo.getCompletesize() >= downinfo.getFilesize()) {
					Logger.info(this, "download finsh");
					downinfo.setStatus(STATUS_STOP);
					mUpateHandler.update(mListener, downinfo, STATUS_STOP);
				} else {
					downinfo.setStatus(STATUS_PAUSE);
					mUpateHandler.update(mListener, downinfo, STATUS_PAUSE);
				}
				updateDownLoadInfo(downinfo);
				map.remove(String.valueOf(downinfo.getId()));
				if (is != null) {
					try {
						is.close();
					} catch (IOException e) {
					}
				}
				if (randomAccessFile != null) {
					try {
						randomAccessFile.close();
					} catch (IOException e) {
					}
				}
			}
		}
	}

	class UpateHandler {
		DownLoadListener mListener;

		public void update(DownLoadListener mListener, DownloadInfo downinfo, int what) {
			this.mListener = mListener;
			// TODO ...用广播来通知刷新进度条，取消回调的方式
			// if (mListener != null)
			// mHandler.sendEmptyMessage(what);
			Intent intent = new Intent(Constants.ACTION_PROGRESS);
			intent.putExtra("downinfo", downinfo);
			intent.putExtra("status", what);
			mContext.sendBroadcast(intent);

			try {
				if (helper == null)
					helper = DownLoadDBHelper.getInstances(mContext);
				helper.update(downinfo);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		@SuppressLint("HandlerLeak")
		private Handler mHandler = new Handler() {

			public void handleMessage(android.os.Message msg) {
				switch (msg.what) {
				case STATUS_DOWNLOAD:
					mListener.downloadUpdate();
					break;
				case STATUS_STOP:
					mListener.downloadSucceed();
					break;
				case STATUS_PAUSE:
					mListener.downloadFailed();
					break;
				}
			};
		};
	}

	// public synchronized void insertDownLoadInfo(DownloadInfo info) {
	//
	// ContentValues initialValues = new ContentValues();
	// initialValues.put(LauncherDBOperations.TABLE_DOWNLOAD_KEY_ID,
	// info.getId());
	// initialValues.put(LauncherDBOperations.TABLE_DOWNLOAD_KEY_NAME,
	// info.getFilename());
	// initialValues.put(LauncherDBOperations.TABLE_DOWNLOAD_KEY_LOCAL_NAME,
	// info.getLocalname());
	// initialValues.put(LauncherDBOperations.TABLE_DOWNLOAD_KEY_URL,
	// info.getUrl());
	// initialValues.put(LauncherDBOperations.TABLE_DOWNLOAD_KEY_FILE_SIZE,
	// info.getFilesize());
	// initialValues.put(LauncherDBOperations.TABLE_DOWNLOAD_KEY_COMPLETE_SIZE,
	// info.getCompletesize());
	// initialValues.put(LauncherDBOperations.TABLE_DOWNLOAD_KEY_COMPLETE_STATUS,
	// info.getStatus());
	// LauncherDBOperations.insert(LauncherDBOperations.TABLE_DOWNLOAD,
	// initialValues);
	// Logger.info(this, "--insertDownLoadInfo : " + info.toString());
	// }

	public synchronized void updateDownLoadInfo(DownloadInfo info) {

		// ContentValues initialValues = new ContentValues();
		// initialValues.put(LauncherDBOperations.TABLE_DOWNLOAD_KEY_ID,
		// info.getId());
		// initialValues.put(LauncherDBOperations.TABLE_DOWNLOAD_KEY_NAME,
		// info.getFilename());
		// initialValues.put(LauncherDBOperations.TABLE_DOWNLOAD_KEY_LOCAL_NAME,
		// info.getLocalname());
		// initialValues.put(LauncherDBOperations.TABLE_DOWNLOAD_KEY_URL,
		// info.getUrl());
		// initialValues.put(LauncherDBOperations.TABLE_DOWNLOAD_KEY_FILE_SIZE,
		// info.getFilesize());
		// initialValues.put(LauncherDBOperations.TABLE_DOWNLOAD_KEY_COMPLETE_SIZE,
		// info.getCompletesize());
		// initialValues.put(LauncherDBOperations.TABLE_DOWNLOAD_KEY_COMPLETE_STATUS,
		// info.getStatus());
		// LauncherDBOperations.update(LauncherDBOperations.TABLE_DOWNLOAD,
		// initialValues,
		// LauncherDBOperations.TABLE_DOWNLOAD_KEY_ID + "=" + info.getId());
		Logger.info(this, "--updateDownLoadInfo : " + info.toString());
	}

	// public synchronized DownloadInfo getDownLoadInfo(int id) {
	// if (id < 0) {
	// return null;
	// }
	// DownloadInfo info = null;
	// Cursor cursor =
	// LauncherDBOperations.query(LauncherDBOperations.TABLE_DOWNLOAD,
	// LauncherDBOperations.TABLE_DOWNLOAD_KEY_ID + "=" + id);
	// if (cursor != null) {
	// cursor.moveToFirst();
	// if (cursor.getCount() <= 0) {
	// cursor.close();
	// return null;
	// }
	// info = new DownloadInfo();
	// info.setId(cursor.getInt(cursor.getColumnIndex(LauncherDBOperations.TABLE_DOWNLOAD_KEY_ID)));
	// info.setFilename(cursor.getString(cursor.getColumnIndex(LauncherDBOperations.TABLE_DOWNLOAD_KEY_NAME)));
	// info.setLocalname(cursor.getString(cursor
	// .getColumnIndex(LauncherDBOperations.TABLE_DOWNLOAD_KEY_LOCAL_NAME)));
	// info.setUrl(cursor.getString(cursor.getColumnIndex(LauncherDBOperations.TABLE_DOWNLOAD_KEY_URL)));
	// info.setFilesize(cursor.getInt(cursor.getColumnIndex(LauncherDBOperations.TABLE_DOWNLOAD_KEY_FILE_SIZE)));
	// info.setCompletesize(cursor.getInt(cursor
	// .getColumnIndex(LauncherDBOperations.TABLE_DOWNLOAD_KEY_COMPLETE_SIZE)));
	// info.setStatus(cursor.getInt(cursor.getColumnIndex(LauncherDBOperations.TABLE_DOWNLOAD_KEY_COMPLETE_STATUS)));
	// cursor.close();
	// Logger.info(this, "--getDownLoadInfo : " + info.toString());
	// }
	// return info;
	// }

	public synchronized DownloadInfo createDownloadInfo(int id, String fileName, String url, int filesize) {
		// DownloadInfo dInfo = getDownLoadInfo(id);
		// if (dInfo != null) {
		// return dInfo;
		// }
		DownloadInfo dInfo;
		dInfo = new DownloadInfo();
		dInfo.setId(id);
		dInfo.setFilename(fileName);
		dInfo.setLocalname(fileName);
		dInfo.setUrl(url);
		dInfo.setCompletesize(0);
		dInfo.setFilesize(filesize);
		dInfo.setStatus(0);
		// insertDownLoadInfo(dInfo);
		return dInfo;
	}

	/**
	 * 根据下载id是否存在于下载map集合中
	 * 
	 * @param dInfo
	 * @return
	 */
	public boolean isDownloading(DownloadInfo dInfo) {
		DownLoadTask task = map.get(String.valueOf(dInfo.getId()));
		if (task != null) {
			return true;
		}
		return false;
	}

	/**
	 * 停止一个下载线程
	 * 
	 * @param dInfo
	 * @return是否成功
	 */
	public boolean stopDownload(DownloadInfo dInfo) {
		DownLoadTask task = map.get(String.valueOf(dInfo.getId()));
		if (task == null) {
			return false;
		}
		task.isPause = true;
		return true;
	}

	/**
	 * 判断是否下载完成
	 * 
	 * @param dInfo
	 * @return
	 */
	public synchronized boolean isCompleted(DownloadInfo dInfo) {
		if (dInfo != null && dInfo.getCompletesize() >= dInfo.getFilesize()) {
			return true;
		}
		return false;
	}

	// public synchronized boolean isCompleted(int id) {
	// final DownloadInfo dInfo = getDownLoadInfo(id);
	// return isCompleted(dInfo);
	// }

	public interface DownLoadListener {
		public void downloadSucceed();

		public void downloadFailed();

		public void downloadUpdate();
	}

	public void onDestroy() {
		pool.shutdown();
	}
}
