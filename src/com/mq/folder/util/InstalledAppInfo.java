package com.mq.folder.util;

public class InstalledAppInfo {
	private String mAppName;
	private String mPackageName;

	public InstalledAppInfo(String appName, String packageName) {
		this.mAppName = appName;
		this.mPackageName = packageName;
	}

	@Override
	public boolean equals(Object object) {
		if (null == object) {
			return false;
		}
		if (this == object) {
			return true;
		}
		if (object instanceof InstalledAppInfo) {
			InstalledAppInfo installedApp = (InstalledAppInfo) object;
			if (this.mAppName.equals(installedApp.mAppName)
					&& this.mPackageName.equals(installedApp.mPackageName)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public int hashCode() {
		int prime = 31;
		int result = 1;
		return prime * result + mAppName.hashCode();
	}

	@Override
	public String toString() {
		return mAppName + ":" + mPackageName;
	}
}
