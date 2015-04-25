package com.mq.folder.common.callback;

import com.mq.folder.common.entity.AppInfo;

import android.view.View;

public interface OnMyItemClickListener {
	void onItemClicked(View v, int position, AppInfo info);
}
