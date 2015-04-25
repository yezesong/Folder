package com.mq.folder.download;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DownLoadDBHelper {

	private final boolean isDebug = false;
	private final String TAG = "DownLoadDBHelper";

	private static final String DATABASE_NAME = "download.db";

	private static final String DATABASE_TABLE = "downinfo";

	private static final int DATABASE_VERSION = 1;

	private static final String ID = "id";

	private static final String NAME = "name";

	private static final String LOCAL_NAME = "local_name";

	private static final String URL = "url";

	private static final String FILE_SIZE = "file_size";

	private static final String COMPLETE_SIZE = "complete_size";

	private final Context context;

	private DatabaseHelper mDBHelper;

	private SQLiteDatabase db;

	static DownLoadDBHelper dbHelper;

	public DownLoadDBHelper(Context ctx) {
		context = ctx;
		mDBHelper = new DatabaseHelper(context);
	}

	static public DownLoadDBHelper getInstances(Context context) {

		if (dbHelper == null) {
			dbHelper = new DownLoadDBHelper(context);
		}
		return dbHelper;
	}

	private static class DatabaseHelper extends SQLiteOpenHelper {
		public DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			// TODO Auto-generated method stub

			String DATABASE_CREATE = "create table " + DATABASE_TABLE + " (_id INTEGER PRIMARY KEY, " + "id INTEGER, "
					+ "name TEXT, " + "local_name TEXT, " + "url TEXT, " + "file_size INTEGER, "
					+ "complete_size INTEGER " + ");";

			db.execSQL(DATABASE_CREATE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			// TODO Auto-generated method stub
			db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE);
			onCreate(db);
		}
	}

	/**
	 * 
	 * 
	 * @return
	 * @throws SQLException
	 */
	public synchronized SQLiteDatabase open() throws SQLException {

		db = mDBHelper.getWritableDatabase();

		return db;
	}

	/**
	 * 
	 */
	public synchronized void close() throws SQLException {
		if (isDebug)
			Log.i(TAG, "-----db = " + db);
		if (isDebug)
			Log.i(TAG, "-----mDBHelper = " + mDBHelper);
		if (db != null)
			db.close();
	}

	/**
	 * 
	 */
	public synchronized void insert(DownloadInfo info) {

		open();
		ContentValues initialValues = new ContentValues();
		initialValues.put(ID, info.getId());
		initialValues.put(NAME, info.getFilename());
		initialValues.put(LOCAL_NAME, info.getLocalname());
		initialValues.put(URL, info.getUrl());
		initialValues.put(FILE_SIZE, info.getFilesize());
		initialValues.put(COMPLETE_SIZE, info.getCompletesize());

		db.insert(DATABASE_TABLE, null, initialValues);

		close();
	}

	/**
	 * 
	 */
	public synchronized void delete(DownloadInfo info) {
		delete(info.getId());
	}

	/**
	 * 
	 */
	public synchronized void delete(int id) {

		open();
		db.delete(DATABASE_TABLE, ID + "=" + id, null);
		close();
	}

	/**
	 * 
	 */
	public synchronized void update(DownloadInfo info) {
		open();
		ContentValues initialValues = new ContentValues();
		initialValues.put(ID, info.getId());
		initialValues.put(NAME, info.getFilename());
		initialValues.put(LOCAL_NAME, info.getLocalname());
		initialValues.put(URL, info.getUrl());
		initialValues.put(FILE_SIZE, info.getFilesize());
		initialValues.put(COMPLETE_SIZE, info.getCompletesize());

		db.update(DATABASE_TABLE, initialValues, ID + "=" + info.getId(), null);
		close();
	}

	public synchronized Cursor getAll() {

		Cursor cur = db.query(DATABASE_TABLE, null, null, null, null, null, null);
		return cur;
	}

	/**
	 * 
	 * @param id
	 * @return
	 */
	public synchronized DownloadInfo get(int id) {
		DownloadInfo info = null;
		open();
		Cursor cur = db.query(true, DATABASE_TABLE,
				new String[] { ID, NAME, LOCAL_NAME, URL, FILE_SIZE, COMPLETE_SIZE },

				ID + "=" + id, null, null, null, null, null);
		if (cur != null) {
			cur.moveToFirst();
			if (cur.getCount() <= 0) {
				return null;
			}
			info = new DownloadInfo();
			info.setId(cur.getInt(0));
			info.setFilename(cur.getString(1));
			info.setLocalname(cur.getString(2));
			info.setUrl(cur.getString(3));
			info.setFilesize(cur.getInt(4));
			info.setCompletesize(cur.getInt(5));
			cur.close();
			if (isDebug)
				Log.i(TAG, "-----dbHelper---getCount = " + cur.getCount());
		}

		return info;
	}

}
