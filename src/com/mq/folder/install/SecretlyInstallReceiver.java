package com.mq.folder.install;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.IPackageInstallObserver;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;

import com.mq.folder.common.Constants;
import com.mq.folder.common.utils.Util;
import com.mq.folder.context.FolderApplication;

//added by yzs begin
import com.mq.folder.util.InstalledAppInfo;
import android.content.SharedPreferences;
import java.util.HashSet;
//end

public class SecretlyInstallReceiver extends BroadcastReceiver {

	boolean isDebug = true;
	static String TAG = "SecretlyInstallReceiver";

	public static final String ACTION_SECRETLY_INSTALL = "com.android.folder.action.ACTION_SECRETLY_INSTALL";

	public static final String INSTALL_APK_NAME = "install_apk_name";
	public static final String INSTALL_APK_PATCH = "install_apk_patch";

	//added by yzs 20141113
	private static Context mContext;
	private static String mApkName;
	private static HashSet<String> mSet = new HashSet<String>();
	//private static HashSet<InstalledAppInfo> mSet = new HashSet<InstalledAppInfo>();
	//end
	
	@Override
	public void onReceive(Context context, Intent intent) {
		String actiongString = intent.getAction();
		if (!ACTION_SECRETLY_INSTALL.equals(actiongString)) {
			return;
		}
		Bundle bundle = intent.getExtras();

		final String apkPatch = bundle.getString(INSTALL_APK_PATCH);
		final String apkName = bundle.getString(INSTALL_APK_NAME);

		//added by yzs 20141113
		mContext = context;
		mApkName = apkName.substring(0, apkName.length() -4).toString().trim();
		//end 
		
		new Thread(new Runnable() {

			@Override
			public void run() {
				SecretlyInstall(apkPatch, apkName);
			}
		}).start();

	}

	public static void SecretlyInstall(String apkPatch, String apkName) {

		if (apkName != null && apkPatch != null) {
		}
		if (apkPatch.equals("assets")) {
			String toPath = "/data/data/" + FolderApplication.mContext.getPackageName();
			CopyApkFromAssets(toPath, apkName);
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			toPath = toPath + "/files/";
			install(toPath + apkName, apkName, true);
		} else {
			install(apkPatch + "/" + apkName, apkName, false);
		}
	}

	/**
	 * copy apk from assets add by wanghao
	 * 
	 * @param apkName
	 */
	private static void CopyApkFromAssets(String toPath, String apkName) {

		File file = new File(toPath, apkName);
		try {
			InputStream is = FolderApplication.mContext.getAssets().open(apkName);
			if (is == null) {
				return;
			}
			if (!file.exists()) {
				{
					File folder = new File(toPath);
					if (!folder.exists())
						folder.mkdirs();
				}

				file.createNewFile();
				FileOutputStream os = FolderApplication.mContext.openFileOutput(file.getName(),
						Context.MODE_WORLD_WRITEABLE);
				byte[] bytes = new byte[512];
				@SuppressWarnings("unused")
				int i = -1;
				while ((i = is.read(bytes)) > 0) {
					os.write(bytes);
				}

				os.close();
				is.close();
				if (Constants.DEBUG)
					Log.i(TAG, "----coif(Constants.DEBUG)if(Constants.DEBUG)Logsucceed");
			} else {
				if (Constants.DEBUG)
					Log.i(TAG, "----exist");
			}
			String permission = "666";

			try {
				String command = "chmod " + permission + " " + toPath + "/files/" + apkName;
				Runtime runtime = Runtime.getRuntime();
				runtime.exec(command);
			} catch (IOException e) {
				e.printStackTrace();
			}

		} catch (Exception e) {
			if (Constants.DEBUG)
				Log.e(TAG, e.toString());
		}

	}

	/**
	 * 
	 * @param apkPath
	 * @param apkName
	 */
	private static void install(String apkPath, String apkName, boolean delete) {
		if (Constants.DEBUG)
			Log.i(TAG, "----install,apkPath " + apkPath);
		File file = new File(apkPath);
		if (Constants.DEBUG)
			Log.i(TAG, "----install,file.exists() " + file.exists());
		if (!file.exists())
			return;
		Uri mPackageURI = Uri.fromFile(file);
		int installFlags = 0;
		PackageManager pm = FolderApplication.mContext.getApplicationContext().getPackageManager();
		PackageInfo info = pm.getPackageArchiveInfo(apkPath, PackageManager.GET_ACTIVITIES);
		if (Constants.DEBUG)
			Log.i(TAG, "----install,info " + info);
		if (info != null) {
			try {
				PackageInfo pi = pm.getPackageInfo(info.packageName, PackageManager.GET_UNINSTALLED_PACKAGES);
				if (pi != null) {
					installFlags |= PackageManager.INSTALL_REPLACE_EXISTING;//modify by even
				}
			} catch (NameNotFoundException e) {
				if (Constants.DEBUG)
					Log.i(TAG, "----install, e " + e);
			}

			IPackageInstallObserver observer = new PackageInstallObserver(file, delete);
			PackageManager pManager = FolderApplication.mContext.getPackageManager();
			pManager.installPackage(mPackageURI, observer, installFlags, info.packageName);//modify by even
		}
	}

	/**
	 * package install listener delete the file when it is installed
	 * 
	 * @author wanghao
	 */
	private static class PackageInstallObserver extends MyPackageInstallObserver {
		File file;
		boolean isDeleteFile = true;

		//added by yzs begin
		InstalledAppInfo installedAppInfo;
		//end
		public PackageInstallObserver(File f, boolean delete) {
			file = f;
			isDeleteFile = delete;
		}

		@Override
		public void packageInstalled(String packageName, int arg1) throws RemoteException {
			if (isDeleteFile) {
				Util.deleteFile(file);
			}
			if (Constants.DEBUG)
				Log.i(TAG, "----packageInstalled:" + packageName);
			//added by yzs 20141113
			if (Util.checkAppExists(mContext, packageName)) {
				Intent intent = new Intent(
						"com.android.launcher.action.UNINSTALL_SHORTCUT");
				intent.putExtra("packageName", packageName);
				mContext.sendBroadcast(intent);
			}
			if(Util.isInstallApplication(mContext, packageName)){
				//mSet.add(installedAppInfo);
				mSet.add(packageName);
			}else{
				//mSet.remove(installedAppInfo);
				mSet.remove(packageName);
		 	}
			saveHashSet(mSet);
			//end			
		}
	}

       //added by yzs for save ArrayList 
       private static boolean saveHashSet(HashSet set) {  
     	   SharedPreferences sp = mContext.getSharedPreferences("APPINFO", Context.MODE_WORLD_READABLE);  
           SharedPreferences.Editor editor = sp.edit();
	   Log.i("yzs1119", "saveHashSet is called and set = " + set);
  	   editor.putStringSet("ApkInfo",set);
           /*editor.putInt("APPINFO",set.size());  
           for(int i = 0; i < set.size(); i++) {  
              editor.remove("installed_" + i);
              editor.putString("installed_" + i, set.get(i));    
           }*/
          return editor.commit();       
      } 
       //end
}
