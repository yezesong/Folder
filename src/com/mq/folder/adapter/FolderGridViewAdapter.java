package com.mq.folder.adapter;

import java.util.ArrayList;
import java.util.HashMap;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.mq.folder_game_hub.R;
import com.mq.folder.common.Constants;
import com.mq.folder.common.utils.Util;
import com.mq.folder.context.MainActivity;

public class FolderGridViewAdapter extends BaseAdapter {

	private Context context;
	private ArrayList<HashMap<String, Object>> lstImageItem;
	private LayoutInflater mInflater;
	private int itemWidth;
	private boolean checkedState = false;
	private Animation anim;
	@SuppressWarnings("unused")
	private int count;

	public FolderGridViewAdapter(Context c, ArrayList<HashMap<String, Object>> lstImageItem, int itemWidth) {
		super();
		this.context = c;
		if (lstImageItem == null) {
			this.lstImageItem = new ArrayList<HashMap<String, Object>>();
		} else {
			this.lstImageItem = lstImageItem;
		}
		mInflater = LayoutInflater.from(context);
		this.itemWidth = itemWidth;
		if (anim == null)
			anim = AnimationUtils.loadAnimation(c, R.anim.item_rotate);
		count = lstImageItem.size();
	}

	public int getCount() {
		return lstImageItem.size();
	}

	public void setCheckedState(boolean checkedState) {
		this.checkedState = checkedState;
		notifyDataSetChanged();
	}

	public boolean getCheckedState() {
		return checkedState;
	}

	@Override
	public Object getItem(int index) {
		return lstImageItem.get(index);
	}

	@Override
	public long getItemId(int index) {
		return index;
	}

	public void update(ArrayList<HashMap<String, Object>> lstImageItem) {
		this.lstImageItem = lstImageItem;
	}

	public void updateCount(int size) {
		count = size;
	}

	@SuppressLint("InflateParams")
	@Override
	public View getView(final int index, View convertView, ViewGroup parent) {
		final GridHolder holder;
		if (convertView == null) {
			convertView = mInflater.inflate(R.layout.item_onlinefolder_appicon, null);
			holder = new GridHolder();
			holder.shortcutImg = (ImageView) convertView.findViewById(R.id.uu_folder_icon_imageview);
			holder.bgLayout = (RelativeLayout) convertView.findViewById(R.id.uu_folder_icon_bg_panel);
			LayoutParams params = holder.bgLayout.getLayoutParams();
			params.height = itemWidth;
			params.width = itemWidth;
			holder.bgLayout.setLayoutParams(params);
			holder.shortcutName = (TextView) convertView.findViewById(R.id.uu_folder_icon_title);
			holder.deleteImg = (ImageView) convertView.findViewById(R.id.uu_folder_delete_flag_imageview);
			convertView.setTag(holder);
		} else {
			holder = (GridHolder) convertView.getTag();
		}
		final HashMap<String, Object> info = lstImageItem.get(index);
		if (info != null) {
			holder.shortcutName.setText(info.get("itemName").toString());
			// holder.shortcutName.setTag(index);
			Object obj = info.get("itemImage");
			if (obj != null)

				holder.shortcutImg.setImageDrawable((Drawable) obj);
			// if (info.get("selectImg") != null) {
			// holder.selectImg.setImageResource((Integer)
			// (info.get("selectImg")));
			// }
			holder.deleteImg.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					Util.delectApk(context, info.get("itemPackageName").toString());
				}
			});
			// if (info.get("itemBackground") != null) {
			// holder.bgLayout.setBackgroundColor((Integer)
			// info.get("itemBackground"));
			// }
		}
		// if (index == count - 1) {
		// holder.bgLayout.setBackgroundColor(Color.TRANSPARENT);
		// }
		convertView.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				//del by yzs 20141113
				/*if (index == getCount() - 1) {
					return false;
				}*/
				setCheckedState(true);
				return true;
			}
		});
		if (checkedState ){//&& index != getCount() - 1) {//modified by yzs 20141113
			holder.deleteImg.setVisibility(View.VISIBLE);
			if (holder.bgLayout.getAnimation() == null) {
				holder.bgLayout.setAnimation(anim);
			}
		} else {
			holder.deleteImg.setVisibility(View.GONE);
			if (holder.bgLayout.getAnimation() != null) {
				holder.bgLayout.clearAnimation();
			}
		}
		convertView.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (checkedState )//&& index != getCount() - 1)//modified by yzs 20141113
					return;
				//del by yzs 20141113
				/*if (index == getCount() - 1) {
					((MainActivity) context).showAppListDialog();
					return;
				}*/
				String packageName = info.get("itemPackageName").toString();
				try {
					Util.openApp(packageName, context);
				} catch (Exception e) {
					e.printStackTrace();
					if (Constants.DEBUG)
						Log.e("folderAdapter", "open app error,name not found..");
					Toast.makeText(context, R.string.app_uninstall, Toast.LENGTH_SHORT).show();
				}
			}
		});
		return convertView;
	}

	private class GridHolder {
		ImageView shortcutImg, deleteImg;
		TextView shortcutName;
		RelativeLayout bgLayout;
	}
}
