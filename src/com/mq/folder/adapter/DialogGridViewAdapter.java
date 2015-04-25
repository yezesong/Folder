package com.mq.folder.adapter;

import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.mq.folder_game_hub.R;
import com.mq.folder.common.callback.OnMyItemClickListener;
import com.mq.folder.common.entity.AppInfo;

public class DialogGridViewAdapter extends BaseAdapter {

	private Context context;
	private ArrayList<AppInfo> lstImageItem;
	private LayoutInflater mInflater;
	private OnMyItemClickListener callback;
	private boolean[] checks;

	public DialogGridViewAdapter(Context c, ArrayList<AppInfo> lstImageItem) {
		super();
		this.context = c;
		if (lstImageItem == null) {
			this.lstImageItem = new ArrayList<AppInfo>();
		} else {
			this.lstImageItem = lstImageItem;
		}
		mInflater = LayoutInflater.from(context);
		checks = new boolean[lstImageItem.size()];
		for (int i = 0; i < lstImageItem.size(); i++) {
			checks[i] = lstImageItem.get(i).isFolderContains();
		}
	}

	public void setOnItemClickListener(OnMyItemClickListener callback) {
		this.callback = callback;
	}

	public int getCount() {
		// TODO Auto-generated method stub
		return lstImageItem.size();
	}

	@Override
	public Object getItem(int index) {

		return lstImageItem.get(index);
	}

	@Override
	public long getItemId(int index) {
		return index;
	}

	@SuppressLint("InflateParams")
	@Override
	public View getView(final int index, View convertView, ViewGroup parent) {
		final GridHolder holder;
		if (convertView == null) {
			convertView = mInflater.inflate(R.layout.item_onlinefolder_appicon_all, null);
			holder = new GridHolder();
			holder.shortcutImg = (ImageView) convertView.findViewById(R.id.uu_folder_icon_imageview);
			holder.shortcutName = (TextView) convertView.findViewById(R.id.uu_folder_icon_title);
			holder.selectImg = (ImageView) convertView.findViewById(R.id.uu_folder_plus_flag_imageview);
			convertView.setTag(holder);
		} else {
			holder = (GridHolder) convertView.getTag();
		}
		final AppInfo info = lstImageItem.get(index);
		if (info != null) {
			holder.shortcutName.setText(info.getAppName());
			holder.shortcutImg.setImageDrawable(info.getIcon());
			// if (info.isFolderContains()) {
			// holder.selectImg.setVisibility(View.VISIBLE);
			// }
		}
		final int pos = index; // pos必须声明为final
		convertView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (callback != null)
					callback.onItemClicked(v, index, info);
				checks[pos] = !checks[pos];
				if (checks[pos]) {
					holder.selectImg.setVisibility(View.VISIBLE);
				} else {
					holder.selectImg.setVisibility(View.GONE);
				}
			}
		});
		if (checks[pos]) {
			holder.selectImg.setVisibility(View.VISIBLE);
		} else {
			holder.selectImg.setVisibility(View.GONE);
		}
		return convertView;
	}

	private class GridHolder {
		ImageView shortcutImg, selectImg;
		TextView shortcutName;
	}
}
