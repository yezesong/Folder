package com.mq.folder.common.view;

import java.util.ArrayList;
import java.util.HashMap;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.mq.folder_game_hub.R;
import com.mq.folder.adapter.DialogGridViewAdapter;
import com.mq.folder.common.callback.OnMyItemClickListener;
import com.mq.folder.common.entity.AppInfo;

public class AppListDialog extends Dialog {
	public static final String str = "添加到文件夹";

	public AppListDialog(Context context) {
		super(context);
	}

	public AppListDialog(Context context, int theme) {
		super(context, theme);
	}

	public AppListDialog(Context context, boolean cancelable, OnCancelListener cancelListener) {
		super(context, cancelable, cancelListener);
	}

	public static class Builder {

		private Context context;
		@SuppressWarnings("unused")
		private View contentView;
		private HashMap<String, AppInfo> userAppList;
		private ArrayList<AppInfo> appList = new ArrayList<AppInfo>();
		private ArrayList<String> changeList = new ArrayList<String>();
		private GridView gridView;
		private TextView title;
		int selectCount = 0;

		private DialogInterface.OnClickListener positiveButtonClickListener;
		private DialogInterface.OnClickListener negativeButtonClickListener;

		public Builder(Context context) {
			this.context = context;
		}

		public Builder setData(HashMap<String, AppInfo> userAppList) {
			this.userAppList = userAppList;
			return this;
		}

		public Builder setContentView(View v) {
			this.contentView = v;
			return this;
		}

		public Builder setPositiveButton(DialogInterface.OnClickListener listener) {
			if (listener == null) {
			}
			this.positiveButtonClickListener = listener;
			return this;
		}

		public Builder setNegativeButton(DialogInterface.OnClickListener listener) {
			this.negativeButtonClickListener = listener;
			return this;
		}

		public ArrayList<String> getChangeList() {
			return changeList;
		}

		@SuppressWarnings("deprecation")
		@SuppressLint("InflateParams")
		public AlertDialog create() {
			LayoutInflater inflater = null;
			if (context != null) {
				inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			} else {
				return null;
			}
			final AlertDialog dialog = new AlertDialog(context, R.style.OnlinefolderDialog);
			View layout = inflater.inflate(R.layout.dialog_onlinefolder_applist, null);
			gridView = (GridView) layout.findViewById(R.id.uufolder_installed_grid);
			title = (TextView) layout.findViewById(R.id.plus_dialog_title);
			dialog.addContentView(layout, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
			if (positiveButtonClickListener != null) {
				((Button) layout.findViewById(R.id.uufolder_plus_confirm))
						.setOnClickListener(new View.OnClickListener() {
							public void onClick(View v) {
								positiveButtonClickListener.onClick(dialog, DialogInterface.BUTTON_POSITIVE);
							}
						});
			}

			if (negativeButtonClickListener != null) {
				((Button) layout.findViewById(R.id.uufolder_plus_cancel))
						.setOnClickListener(new View.OnClickListener() {
							public void onClick(View v) {
								negativeButtonClickListener.onClick(dialog, DialogInterface.BUTTON_NEGATIVE);
							}
						});
			}
			if (userAppList != null && userAppList.size() > 0) {
				changeList = new ArrayList<String>();// 初始化变化列表
				for (String packageName : userAppList.keySet()) {
					AppInfo info = userAppList.get(packageName);
					appList.add(info);
					if (info.isFolderContains())
						selectCount++;
				}
				title.setText(str + "（" + selectCount + "/" + appList.size() + "）");
				DialogGridViewAdapter adapter = new DialogGridViewAdapter(context, appList);
				adapter.setOnItemClickListener(new OnMyItemClickListener() {

					@Override
					public void onItemClicked(View view, int position, AppInfo info) {
						String packageName = info.getPackageName();
						ImageView selectImg = (ImageView) view.findViewById(R.id.uu_folder_plus_flag_imageview);
						if (selectImg.getVisibility() == View.GONE) {
							selectImg.setVisibility(View.VISIBLE);
							selectCount++;
							title.setText(str + "（" + selectCount + "/" + appList.size() + "）");
						} else {
							selectImg.setVisibility(View.GONE);
							selectCount--;
							title.setText(str + "（" + selectCount + "/" + appList.size() + "）");
						}
						if (!changeList.contains(packageName))
							changeList.add(packageName);
						else
							changeList.remove(packageName);
					}

				});
				gridView.setAdapter(adapter);
			}
			dialog.setContentView(layout);
			dialog.setCanceledOnTouchOutside(false);
			return dialog;
		}
	}

}
