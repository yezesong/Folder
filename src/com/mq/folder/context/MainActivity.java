package com.mq.folder.context;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.GridView;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.SlidingDrawer;
import android.widget.SlidingDrawer.OnDrawerCloseListener;
import android.widget.SlidingDrawer.OnDrawerOpenListener;
import android.widget.TextView;
import android.widget.Toast;

import com.joy.network.RecommendShortcutInfo;
import com.joy.network.handler.RecommendListHandler;
import com.joy.network.impl.Service;
import com.joy.network.util.AsyncTask;
import com.joy.network.util.AsyncTask.CallBack;
import com.mq.folder_game_hub.R;
import com.mq.folder.adapter.FolderGridViewAdapter;
import com.mq.folder.common.Constants;
import com.mq.folder.common.cache.BitmapCaches;
import com.mq.folder.common.entity.AppInfo;
import com.mq.folder.common.utils.UnitUtils;
import com.mq.folder.common.utils.Util;
import com.mq.folder.common.view.AlertDialog;
import com.mq.folder.common.view.AppListDialog;
import com.mq.folder.common.view.RecommendButton;
import com.mq.folder.download.DownLoadDBHelper;
import com.mq.folder.download.DownloadInfo;
import com.mq.folder.download.DownloadManager;

@SuppressWarnings("deprecation")
public class MainActivity extends Activity implements OnClickListener {
	private ImageView imgHandle;

	private ImageView mRefresh, notifyLeft, notifyRight;

	private LinearLayout linearRecommend;
	private RelativeLayout relativeGuide;

	private HorizontalScrollView scrollView;

	private SlidingDrawer sd;

	private GridView gridView;

	private SharedPreferences sp;

	private final static String FOLDER_APP_LIST = "folder_app_list";

	private Context context;

	private final static String TAG = "folderMain";

	public UnitUtils unit;
	private boolean isCacheInfo;
	private final static String KEY_HAS_OPEN = "has_open";
	private AssetManager assertManager;
	private String[] imgList;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.onlinefolder_layout);
		context = this;
		assertManager = getResources().getAssets();
		try {
			imgList = assertManager.list("img");
			Log.e(TAG, "imgList.toString: " + Arrays.toString(imgList));
		} catch (IOException e) {
			e.printStackTrace();
		}
		initView();
		initFolderApp();
		if (Util.isNetworkConnected(context)) {
			sd.animateOpen();
			getRecommendApkListFirstTime();
		} else {
			Toast.makeText(getApplicationContext(), getResources().getText(R.string.uu_download_no_networking_text),
					Toast.LENGTH_SHORT).show();
		}
		IntentFilter filter = new IntentFilter(Intent.ACTION_PACKAGE_REMOVED);
		filter.addDataScheme("package");
		registerReceiver(myReceiver, filter);
		if (sp == null)
			sp = PreferenceManager.getDefaultSharedPreferences(context);
		if (!sp.getBoolean(KEY_HAS_OPEN, false)) {
			relativeGuide.setVisibility(View.VISIBLE);
			notifyLeft.startAnimation(AnimationUtils.loadAnimation(context, R.anim.notify_left));
			notifyRight.startAnimation(AnimationUtils.loadAnimation(context, R.anim.notify_right));
			for (String str : Constants.APPS) {
				folderAppAdd(str);
			}
		}
		List<RecommendShortcutInfo> apps = readShortcutInfo();
		// 缓存的推荐列表为空时，显示出场就推荐的几个应用
		if (apps.size() == 0) {
			// TODO 需要内置的应用在这
			String json = Util.getStringFromAssets(context, "json.txt");
			try {
				apps = new RecommendListHandler().getAppList(new JSONObject(json));
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		addApkToRecommend(apps);
		isCacheInfo = true;
	}

	private int scrollHeight = 0;
	private Handler handler;

	private void initView() {
		getBestWidth();
		imgHandle = (ImageView) findViewById(R.id.uu_folder_navigator);
		// imgHandle.setImageResource(R.drawable.handle_down);
		linearRecommend = (LinearLayout) findViewById(R.id.folder_recommend_container);
		mRefresh = (ImageView) findViewById(R.id.uu_folder_refresh_imageview);
		mRefresh.setOnClickListener(this);
		gridView = (GridView) findViewById(R.id.uu_folder_content_list);
		handler = new Handler();
		handler.postDelayed(new Runnable() {

			@Override
			public void run() {
				if (gridView.getChildAt(0) == null) {
					handler.postDelayed(this, 100);
					return;
				} else {
					int height = gridView.getChildAt(0).getHeight();
					LayoutParams params = (LayoutParams) gridView.getLayoutParams();
					params.height = height * 4;
					gridView.setLayoutParams(params);
				}
			}
		}, 100);
		// LayoutParams params = (LayoutParams) gridView.getLayoutParams();
		// params.setMargins(unit.dip2px(12), unit.dip2px(40), unit.dip2px(12),
		// unit.dip2px(148));
		// gridView.setLayoutParams(params);
		sd = (SlidingDrawer) findViewById(R.id.uu_folder_virtual_panel);
		relativeGuide = (RelativeLayout) findViewById(R.id.folder_guide);
		notifyLeft = (ImageView) findViewById(R.id.notify_left);
		notifyRight = (ImageView) findViewById(R.id.notify_right);
		sd.setOnDrawerOpenListener(new OnDrawerOpenListener() {

			@Override
			public void onDrawerOpened() {
				// if (!Util.isNetworkConnected(context)) {
				// sd.animateClose();
				// Toast.makeText(getApplicationContext(),
				// getResources().getText(R.string.uu_download_no_networking_text),
				// Toast.LENGTH_LONG).show();
				// return;
				// }
				if (linearRecommend.getChildCount() == 0 && mRefresh.getAnimation() == null)
					getRecommendApkListFirstTime();
				imgHandle.setBackgroundResource(R.drawable.uu_folder_down_arrow);
				// imgHandle.setImageResource(R.drawable.handle_down);
				// LayoutParams params = (LayoutParams)
				// gridView.getLayoutParams();
				// params.setMargins(unit.dip2px(12), unit.dip2px(40),
				// unit.dip2px(12), unit.dip2px(148));
				// gridView.setLayoutParams(params);
			}
		});
		sd.setOnDrawerCloseListener(new OnDrawerCloseListener() {

			@Override
			public void onDrawerClosed() {
				imgHandle.setBackgroundResource(R.drawable.uu_folder_up_arrow);
				// LayoutParams params = (LayoutParams)
				// gridView.getLayoutParams();
				// params.setMargins(unit.dip2px(12), unit.dip2px(40),
				// unit.dip2px(12), unit.dip2px(40));
				// gridView.setLayoutParams(params);
			}
		});
		scrollView = (HorizontalScrollView) findViewById(R.id.folder_recommend_container_brackground);
		scrollView.setOnTouchListener(new OnTouchListener() {

			@SuppressLint("ClickableViewAccessibility")
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction()) {
				case MotionEvent.ACTION_MOVE:
					// View view = ((HorizontalScrollView) v).getChildAt(0);
					// if (view.getMeasuredWidth() <= v.getScrollX() +
					// v.getWidth() + 2) {
					// getRecommendApkList();
					// } else {
					// hideProgressBar();
					// }
					break;
				default:
					break;

				}
				return false;
			}
		});
	}

	private void getBestWidth() {
		// DisplayMetrics metrics = new DisplayMetrics();
		// getWindowManager().getDefaultDisplay().getMetrics(metrics);
		// screenWidth = metrics.widthPixels;
		unit = UnitUtils.getInstance(context);
		// bestItemDb = (unit.px2dip(screenWidth) - 100) / 4;
		// bestItemDb = unit.dip2px(bestItemDb);
		// bestItemPx = screenWidth / 4 - 40;
	}

	public HashMap<String, AppInfo> userAppList = new HashMap<String, AppInfo>();

	private ArrayList<String> folderPackageNameList = new ArrayList<String>();

	private ArrayList<HashMap<String, Object>> folderList = new ArrayList<HashMap<String, Object>>();

	private FolderGridViewAdapter adapter;

	// private Animation anim;

	private void initFolderApp() {
		sp = PreferenceManager.getDefaultSharedPreferences(context);
		String listJsonStr = sp.getString(FOLDER_APP_LIST, "");
		// listJsonStr =
		// "[\"com.UCMobile\",\"com.bmob.im.demo\",\"com.bti.myPiano\",\"com.dewmobile.kuaiya\",\"com.example.testnetwork\",\"com.fanli.android.apps\"]";
		userAppList = userAppList(context);
		if (listJsonStr != null && listJsonStr != "") {
			try {
				JSONArray listJson = new JSONArray(listJsonStr);
				for (int i = 0; i < listJson.length(); i++) {
					String packageName = listJson.getString(i);
					if (userAppList.containsKey(packageName)) {
						folderPackageNameList.add(packageName);

						HashMap<String, Object> appMap = new HashMap<String, Object>();
						appMap.put("itemName", userAppList.get(packageName).getAppName());
						appMap.put("itemImage", userAppList.get(packageName).getIcon());
						appMap.put("itemPackageName", userAppList.get(packageName).getPackageName());
						folderList.add(appMap);
						userAppList.get(packageName).setFolderContains(true);
					}
				}
			} catch (JSONException e) {
				e.printStackTrace();
				if (Constants.DEBUG)
					Log.e(TAG, "parser json error..");
			}
		}
		// 设置加号那张图没有背景
		HashMap<String, Object> appMap = new HashMap<String, Object>();
		appMap.put("itemName", " ");
		appMap.put("itemImage", context.getResources().getDrawable(R.drawable.uu_plus_item_selector));
		// appMap.put("itemImage",
		// context.getResources().getDrawable(R.drawable.transparent));
		// appMap.put("itemImage", null);
		appMap.put("itemPackageName", "add");
		// appMap.put("itemBackground",
		// context.getResources().getDrawable(R.drawable.plus_normal));
		appMap.put("itemBackground", Color.TRANSPARENT);
		//folderList.add(appMap);//del by yzs 20141113
		adapter = new FolderGridViewAdapter(context, folderList, FolderApplication.bestItemPx);
		gridView.setAdapter(adapter);

		// View addItem = gridView.getChildAt(gridView.getChildCount() - 1);
		// RelativeLayout bg = (RelativeLayout)
		// addItem.findViewById(R.id.uu_folder_icon_bg_panel);
		// bg.setBackgroundColor(color.transparent);
		// gridView.setOnItemClickListener(new OnItemClickListener() {
		//
		// @Override
		// public void onItemClick(AdapterView<?> parent, View view, int
		// position, long id) {
		// if (position == folderList.size() - 1) {
		// showAppListDialog();
		// return;
		// }
		// String packageName = folderPackageNameList.get(position);
		// try {
		// Util.openApp(packageName, context);
		// } catch (Exception e) {
		// e.printStackTrace();
		// if (Constants.DEBUG)
		// Log.e(TAG, "open app error,name not found..");
		// Toast.makeText(context, "åºçšæªå®è£?", Toast.LENGTH_SHORT).show();
		// }
		// }
		// });
		// gridView.setOnItemLongClickListener(new OnItemLongClickListener() {
		//
		// @Override
		// public boolean onItemLongClick(AdapterView<?> parent, View view, int
		// position, long id) {
		// if (position == folderList.size() - 1) {
		// return false;
		// }
		// adapter.setCheckedState(!adapter.getCheckedState());
		// int count = gridView.getChildCount();
		// for (int i = 0; i < count - 1; i++) {
		// View chikdView = gridView.getChildAt(i);
		// ImageView deleteView = (ImageView)
		// chikdView.findViewById(R.id.uu_folder_delete_flag_imageview);
		// RelativeLayout itemLayout = (RelativeLayout)
		// chikdView.findViewById(R.id.uu_folder_icon_bg_panel);
		// deleteView.setVisibility(View.VISIBLE);
		// if (anim == null)
		// anim = AnimationUtils.loadAnimation(MainActivity.this,
		// R.anim.item_rotate);
		// itemLayout.startAnimation(anim);
		// }
		// return true;
		// }
		// });
	}

	@Override
	public void onBackPressed() {
		// if (gridView.getChildCount() == 0 || anim == null) {
		// super.onBackPressed();
		// return;
		// }
		// boolean isSuper = true;
		// int count = gridView.getChildCount();
		// for (int i = 0; i < count - 1; i++) {
		// View chikdView = gridView.getChildAt(i);
		// ImageView deleteView = (ImageView)
		// chikdView.findViewById(R.id.uu_folder_delete_flag_imageview);
		// deleteView.setVisibility(View.INVISIBLE);
		// RelativeLayout itemLayout = (RelativeLayout)
		// chikdView.findViewById(R.id.uu_folder_icon_bg_panel);
		// if (itemLayout.getAnimation() != null) {
		// itemLayout.clearAnimation();
		// isSuper = false;
		// }
		// }
		// if (isSuper)
		// super.onBackPressed();

		if (adapter.getCheckedState())
			adapter.setCheckedState(false);
		else
			super.onBackPressed();
	}

	private AppListDialog appListDialog;
	private GridView applistGridView;

	/**
	 * 显示应用程序列表对话框
	 * 
	 * @author maqj
	 */
	public void showAppListDialog() {
		if (appListDialog != null && appListDialog.isShowing() || context == null || MainActivity.this.isFinishing()) {
			return;
		}
		final AppListDialog.Builder alertBuilder = new AppListDialog.Builder(context);
		alertBuilder.setData(userAppList).setPositiveButton(new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				ArrayList<String> changeList = alertBuilder.getChangeList();
				for (String packageName : changeList) {
					AppInfo info = userAppList.get(packageName);
					if (!info.isFolderContains()) {
						folderAppAdd(packageName);
						if (Constants.DEBUG)
							Log.e(TAG, "add app: " + info.toString());
					} else {
						folderAppRemove(packageName);
						if (Constants.DEBUG)
							Log.e(TAG, "delete app: " + info.toString());
					}
				}
				if (changeList.size() > 0) {
					// adapter.notifyDataSetChanged();
					notifyDataChange();
				}
			}
		}).setNegativeButton(new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		remindDialog = alertBuilder.create();
		remindDialog.show();
		View layout = LayoutInflater.from(context).inflate(R.layout.dialog_onlinefolder_applist, null);
		applistGridView = (GridView) layout.findViewById(R.id.uufolder_installed_grid);
		handler.postDelayed(new Runnable() {

			@Override
			public void run() {
				getWidthCount++;
				Log.e(TAG, "getChildCount: " + applistGridView.getChildCount());
				if (userAppList.size() == 0 || getWidthCount >= 20)
					return;
				if (applistGridView.getChildAt(0) == null) {
					handler.postDelayed(this, 100);
					return;
				} else {
					int height = applistGridView.getChildAt(0).getHeight();
					LayoutParams params = (LayoutParams) applistGridView.getLayoutParams();
					params.height = height * 4;
					applistGridView.setLayoutParams(params);
				}
			}
		}, 100);
	}

	int getWidthCount = 0;

	/**
	 *在文件夹中增加一个图标
	 * 
	 * @param packageName增加程序到包名
	 * @author maqj
	 */
	private void folderAppAdd(String packageName) {
		AppInfo info = userAppList.get(packageName);
		if (info == null) {
			try {
				info = getAppInfo(packageName);
			} catch (NameNotFoundException e) {
				e.printStackTrace();
				if (Constants.DEBUG)
					Log.e(TAG, "add app error, packageName is null..");
				return;
			}
			if (Constants.DEBUG)
				Log.e(TAG, "new add recommend app " + info.toString());
		}
		info.setFolderContains(true);
		userAppList.put(packageName, info);
		// 2.save in SharedPreferences
		folderPackageNameList.add(packageName);
		JSONArray ja = new JSONArray();
		for (String str : folderPackageNameList) {
			ja.put(str);
		}
		sp.edit().putString(FOLDER_APP_LIST, ja.toString()).commit();
		// 3.change gridview data..
		HashMap<String, Object> appMap = new HashMap<String, Object>();
		appMap.put("itemName", info.getAppName());
		appMap.put("itemImage", info.getIcon());
		appMap.put("itemPackageName", info.getPackageName());
		//modified by yzs 20141113
		//folderList.add(folderList.size() - 1, appMap);
		folderList.add(folderList.size(), appMap);
		//end
	}

	/**
	 * 删除文件夹中一个图标
	 * 
	 * @param packageName需要删除的包名
	 * @author maqj
	 */
	private void folderAppRemove(String packageName) {
		if (packageName == null || packageName == "")
			return;
		if (userAppList.containsKey(packageName))
			userAppList.get(packageName).setFolderContains(false);
		// 2.update SharedPreferences
		if (folderPackageNameList.contains(packageName))
			folderPackageNameList.remove(packageName);
		JSONArray ja = new JSONArray();
		for (String str : folderPackageNameList) {
			ja.put(str);
		}
		sp.edit().putString(FOLDER_APP_LIST, ja.toString()).commit();
		// 3.remove app in folder
		for (int i = 0; i < folderList.size(); i++) {
			HashMap<String, Object> map = folderList.get(i);
			if (map.get("itemPackageName") != null) {
				String ItemPackageName = map.get("itemPackageName").toString();
				if (packageName.equals(ItemPackageName)) {
					folderList.remove(i);
				}
			}
		}
	}

	/**
	 * 通知更新文件夹应用列表
	 * 
	 * @author maqj
	 */
	private void notifyDataChange() {
		adapter.notifyDataSetChanged();
		// View addItem = gridView.getChildAt(gridView.getChildCount() - 1);
		// RelativeLayout bg = (RelativeLayout)
		// addItem.findViewById(R.id.uu_folder_icon_bg_panel);
		// bg.setBackgroundColor(color.transparent);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.uu_folder_refresh_imageview:
			getRecommendApkList();
			break;
		case R.id.uu_folder_refresh_imageview_bg:
			getRecommendApkList();
			break;

		case R.id.btn_folder_guide:
			if (sp == null)
				sp = PreferenceManager.getDefaultSharedPreferences(context);
			// if (sp.getBoolean("frist", false)) {
			Editor editor = sp.edit();
			editor.putBoolean(KEY_HAS_OPEN, true);
			editor.commit();
			relativeGuide.setVisibility(View.GONE);
			break;
		default:
			break;
		}
	}

	/**
	 *  显示推荐应用上到进度条
	 */
	private void showProgressBar() {
		if (((AnimationDrawable) mRefresh.getBackground()) == null
				|| !((AnimationDrawable) mRefresh.getBackground()).isRunning()) {

			mRefresh.setImageBitmap(null);
			mRefresh.setBackgroundResource(R.anim.onlinefolder_recommend_refresh);
			((AnimationDrawable) mRefresh.getBackground()).start();
			// mRefresh.animate().alpha(1).setDuration(400).start();
		}
	}

	/**
	 *隐藏进度条
	 */
	private void hideProgressBar() {
		if (((AnimationDrawable) mRefresh.getBackground()) != null
				&& ((AnimationDrawable) mRefresh.getBackground()).isRunning()) {
			((AnimationDrawable) mRefresh.getBackground()).stop();
			mRefresh.setBackgroundDrawable(null);
			// mRefresh.animate().alpha(0).setDuration(400).start();
			mRefresh.setImageResource(R.drawable.uu_folder_refresh);
		}
	}

	/**
	 * 当推荐列表到应用安装完成时调用
	 * 
	 * @param recommendInfo
	 * @author maqj
	 */
	public void onRecommendItemInstalled(RecommendShortcutInfo recommendInfo) {
		// 1.Add shortcuts to onlinefolder
		folderAppAdd(recommendInfo.packageName);
		// 2.Delete item from download list
		int count = linearRecommend.getChildCount();
		for (int i = 0; i < count; i++) {
			RecommendButton button = (RecommendButton) linearRecommend.getChildAt(i);
			if (recommendInfo.packageName.equals(button.recommendInfo.packageName)) {
				linearRecommend.removeViewAt(i);
				if (Constants.DEBUG)
					Log.e("recommendView", "remove id: " + i);
				// 3.注销广播
				button.onDestroy();
				break;
			}
		}
		// 4. adapter.notifyDataSetChanged();
		notifyDataChange();
	}

	private Dialog remindDialog = null;

	/**
	 *创建一个推荐应用列表到图标
	 * 
	 * @param recommendInfo
	 * @return
	 * @author maqj
	 */
	@SuppressLint("InflateParams")
	private RecommendButton createRecommendButton(final RecommendShortcutInfo recommendInfo) {

		final RecommendButton button = (RecommendButton) LayoutInflater.from(context).inflate(
				R.layout.onlinefolder_recommend_icon, null);
		RelativeLayout bg = (RelativeLayout) button.findViewById(R.id.recommend_bg);
		ImageView icon = (ImageView) button.findViewById(R.id.recommend_icon);
		TextView title = (TextView) button.findViewById(R.id.recommend_title);
		final ImageView pauseImg = (ImageView) button.findViewById(R.id.recommend_pause);

		// 判断之前是不是下载没成功过
		DownLoadDBHelper helper = DownLoadDBHelper.getInstances(context);
		DownloadInfo dbInfo = helper.get(recommendInfo.id);
		if (dbInfo == null) {
			if (Constants.DEBUG)
				Log.e(TAG, "id=" + recommendInfo.id + "  dbInfo == null");
			dbInfo = DownloadManager.getInstance().createDownloadInfo(recommendInfo.id, recommendInfo.name,
					recommendInfo.url, recommendInfo.size);
		} else {//数据库不为空
			if (Constants.DEBUG)
				Log.e(TAG, "id=" + recommendInfo.id + "  dbInfo not null..");
			if (!DownloadManager.getInstance().isDownloading(dbInfo)) {
				pauseImg.setVisibility(View.VISIBLE);
				pauseImg.setTag("pause");
			}
			if (DownloadManager.getInstance().isCompleted(dbInfo))
				pauseImg.setVisibility(View.GONE);
		}
		final DownloadInfo dInfo = dbInfo;

		button.setDownloadInfo(dInfo);
		button.setShorcutInfo(recommendInfo);
		button.updateButton();
		button.setFatherView(this);
		// 设置推荐列表图标尺寸
		android.view.ViewGroup.LayoutParams params = bg.getLayoutParams();
		params.height = FolderApplication.bestItemPx;
		params.width = FolderApplication.bestItemPx;
		bg.setLayoutParams(params);
		ViewGroup.LayoutParams titleParams = title.getLayoutParams();
		titleParams.width = FolderApplication.bestItemPx + 20;
		title.setText(recommendInfo.name);
		boolean isAssertHas = false;
		for (String str : imgList) {
			if (str.startsWith(recommendInfo.icon.substring(1))) {
				isAssertHas = true;
				break;
			}
		}
		if (isAssertHas) {
			try {
				InputStream is = assertManager.open("img/" + recommendInfo.icon.substring(1) + ".png");
				icon.setImageBitmap(BitmapFactory.decodeStream(is));
				is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			// 缓存图标
			BitmapCaches.getInstance().getBitmap(recommendInfo.icon, icon, null);
		}
		button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				boolean isNeedDiloag = false;
				if (DownloadManager.getInstance().isCompleted(dInfo)) {
					if (Constants.DEBUG)
						Log.d(TAG, "download completed..");
					if (Util.isInstallApplication(context, recommendInfo.packageName)) {
						Intent intent = context.getPackageManager()
								.getLaunchIntentForPackage(recommendInfo.packageName);
						context.startActivity(intent);
						return;
					} else if (Util.fileIsExist(new File(Constants.DOWNLOAD_APK_DIR, dInfo.getLocalname())
							.getAbsolutePath())) {
//						pauseImg.setVisibility(View.VISIBLE);
//						pauseImg.setTag("success");
//						pauseImg.setImageResource(R.drawable.uu_folder_install);
//						Util.installAPK(context, Constants.DOWNLOAD_APK_DIR, dInfo.getLocalname());
						if (remindDialog != null && remindDialog.isShowing() || context == null
								|| MainActivity.this.isFinishing()) {
							return;
						}
						AlertDialog.Builder alertBuilder = new AlertDialog.Builder(context);
						alertBuilder.setMessage(R.string.config_install + recommendInfo.name + "?").setPositiveButton(R.string.config, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								dialog.dismiss();
//								pauseImg.setVisibility(View.VISIBLE);
//								pauseImg.setTag("success");
//								pauseImg.setImageResource(R.drawable.uu_folder_install);
								Util.installAPK(context, Constants.DOWNLOAD_APK_DIR, dInfo.getLocalname());
							}
						}).setNegativeButton(R.string.uu_folder_cancel_action,new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								dialog.dismiss();
							}
						});
						remindDialog = alertBuilder.create();
						remindDialog.show();
						return;
					} else {// 没安装美奂存，只有数据库有
						// 清空数据库
						dInfo.setCompletesize(0);
						DownloadManager.getInstance().updateDownLoadInfo(dInfo);
						pauseImg.setVisibility(View.GONE);
						// DownloadManager.getInstance().createTask(button,
						// dInfo, false);//这样就没有提示框
						isNeedDiloag = true;
					}
				}
				if (!Util.isNetworkConnected(context)) {
					Toast.makeText(getApplicationContext(),
							getResources().getText(R.string.uu_download_no_networking_text), Toast.LENGTH_LONG).show();
					return;
				}
				if (pauseImg.getVisibility() == View.GONE || isNeedDiloag) {
					boolean isPause = false;
					if (DownloadManager.getInstance().isDownloading(dInfo)) {
						isPause = true;
						Log.e(TAG, "id:" + dInfo.getId() + " is downloading..");
					}
					final boolean isPause2 = isPause;
					if (remindDialog != null && remindDialog.isShowing() || context == null
							|| MainActivity.this.isFinishing()) {
						return;
					}
					AlertDialog.Builder alertBuilder = new AlertDialog.Builder(context);
					String notifyStr = getResources().getText(R.string.config_undown).toString();
					if (!isPause2)
						notifyStr = getResources().getText(R.string.run_before) + recommendInfo.name + " (" + recommendInfo.size / 1000 + "."
								+ (recommendInfo.size / 100) % 10 + "MB)" + getResources().getText(R.string.please_download_before);

					alertBuilder.setMessage(notifyStr).setPositiveButton(R.string.config, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
							if (!isPause2)
								DownloadManager.getInstance().createTask(button, dInfo, false);
							else {
								DownloadManager.getInstance().stopDownload(dInfo);
								// pauseImg.setVisibility(View.VISIBLE);在下载结束后会判断长度，如果长度不对则返回下载失败
							}
						}
					}).setNegativeButton(R.string.uu_folder_cancel_action, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
						}
					});
					remindDialog = alertBuilder.create();
					remindDialog.show();
				} else if (pauseImg.getVisibility() == View.VISIBLE) {
					pauseImg.setVisibility(View.GONE);
					DownloadManager.getInstance().createTask(button, dInfo, false);
				}
			}
		});
		return button;
	}

	/**
	 * recommend item regist brocast list
	 * 
	 * @author maqj
	 */
	// public ArrayList<BroadcastReceiver> receiverList = new
	// ArrayList<BroadcastReceiver>();

	@Override
	protected void onDestroy() {
		super.onDestroy();
		// if (receiverList.size() > 0) {
		// for (BroadcastReceiver receiver : receiverList) {
		// unregisterReceiver(receiver);
		// }
		// }
		ArrayList<RecommendShortcutInfo> infoList = new ArrayList<RecommendShortcutInfo>();
		int count = linearRecommend.getChildCount();
		for (int i = 0; i < count; i++) {
			RecommendButton button = (RecommendButton) linearRecommend.getChildAt(i);
			button.onDestroy();
			// 保存已有到推荐列表
			RecommendShortcutInfo info = button.getShorcutInfo();
			infoList.add(info);
		}
		// TODO保存
		if (infoList.size() > 0)
			saveShortcutInfo(infoList);
		unregisterReceiver(myReceiver);
	}

	private int mRecommendIndex = 0;

	private int mRecommendNum = 1;

	private boolean isLoading;

	private final long MIN_UPDATE_TIME = 2000L;

	// private static final int ANIMATION_DURATION = 600;

	private boolean isFirstTimeRefresh() {
		if (linearRecommend.getChildCount() > 0) {
			return false;
		}
		return true;
	}

	private void getRecommendApkListFirstTime() {
		if (!isFirstTimeRefresh()) {
			return;
		}
		mRecommendIndex = 0;
		getRecommendApkList();
	}

	/**
	 * 增加APP到推荐列表
	 * 
	 * @param list
	 * @author maqj
	 */
	private void addApkToRecommend(List<RecommendShortcutInfo> list) {
		if (list == null) {
			return;
		}
		// if (isCacheInfo) {// 显示的是缓存器
		int counts = linearRecommend.getChildCount();
		for (int i = 0; i < counts; i++) {
			RecommendButton button = (RecommendButton) linearRecommend.getChildAt(i);
			button.onDestroy();
		}
		linearRecommend.removeAllViews();
		isCacheInfo = false;
		// }
		for (int i = 0; i < list.size(); i++) {
			final RecommendShortcutInfo recommendInfo = list.get(i);
			mRecommendIndex = recommendInfo.index;
			mRecommendNum = recommendInfo.num;
			if (Util.isInstallApplication(context, recommendInfo.packageName))
				continue;
			RecommendButton button = createRecommendButton(recommendInfo);
			// 判断是否存在
			int count = linearRecommend.getChildCount();
			boolean isExsit = false;
			for (int m = 0; m < count; m++) {
				RecommendButton button2 = (RecommendButton) linearRecommend.getChildAt(i);
				if (button2 != null && button.recommendInfo.packageName.equals(button2.recommendInfo.packageName)) {
					isExsit = true;
				}
			}
			if (!isExsit)
				linearRecommend.addView(button);
		}
	}

	/**
	 * 获取推荐列表
	 * 
	 * @author maqj
	 */
	private void getRecommendApkList() {
		if (isLoading) {
			return;
		}
		if (!Util.isNetworkConnected(getApplicationContext())) {
			Toast.makeText(getApplicationContext(), getResources().getText(R.string.uu_download_no_networking_text),
					Toast.LENGTH_SHORT).show();
			return;
		}
		mRecommendIndex++;
		final long lastTime = System.currentTimeMillis();
		AsyncTask.getInstance().run(new CallBack() {
			// List<RecommendShortcutInfo> list = new
			// List<RecommendShortcutInfo>();
			List<RecommendShortcutInfo> list = null;

			@Override
			public void onPreExecute() {
				showProgressBar();
				isLoading = true;
			}

			@Override
			public void onPostExecute() {

				hideProgressBar();
				if (list != null) {
					addApkToRecommend(list);
				}
				isLoading = false;
			}

			@Override
			public void doInBackground() {

				// if (mRecommendIndex <= mRecommendNum) {
				// modify 77 -> 2 by ligeng advise.
				// list = Service.getInstance().getRecommendList(2,
				// mRecommendIndex, 6);
				list = Service.getInstance().getRecommendList(2, 1, 10);
				// list = generateTestRecommendShortcutInfo(list);
				// }
				long updateTime = System.currentTimeMillis() - lastTime;
				if (updateTime < MIN_UPDATE_TIME) {
					try {
						Thread.sleep(MIN_UPDATE_TIME - updateTime);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		});
	}

	/**
	 * 接收应用到删除广播
	 * 
	 * @author maqj
	 */
	BroadcastReceiver myReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent == null)
				return;
			if (intent.getAction().equals(Intent.ACTION_PACKAGE_REMOVED)) {
				String packageName = intent.getData().getSchemeSpecificPart();
				if (Constants.DEBUG)
					Log.e(TAG, "delete " + packageName + " sucess..");
				userAppList.remove(packageName);
				if (folderPackageNameList.contains(packageName)) {
					folderAppRemove(packageName);
					// TDDO ..
					// if (adapter.getCheckedState())
					// adapter.setCheckedState(false);
					adapter.notifyDataSetChanged();
				}
			}
		}
	};

	// /**
	// * get app info by packageName
	// *
	// * @param context
	// * @param packageName
	// * @return
	// * @author maqj
	// */
	// private AppInfo getAppInfo(Context context, String packageName) {
	// if (packageName == null || context == null)
	// return null;
	// PackageManager pm = context.getPackageManager();
	// List<PackageInfo> packages = pm.getInstalledPackages(0);
	//
	// for (int i = 0; i < packages.size(); i++) {
	// PackageInfo packageInfo = packages.get(i);
	// if (packageInfo.packageName.equals(packageName)) {
	// AppInfo info = new AppInfo();
	// info.setAppName(packageInfo.applicationInfo.loadLabel(pm)
	// .toString());
	// info.setPackageName(packageInfo.packageName);
	// info.setIcon(packageInfo.applicationInfo.loadIcon(pm));
	// return info;
	// }
	// }
	// return null;
	// }

	/**
	 * get user app list
	 * 
	 * @param context
	 * @return list
	 * @author maqj
	 */
	private HashMap<String, AppInfo> userAppList(Context context) {
		HashMap<String, AppInfo> appList = new HashMap<String, AppInfo>(); // 用来缓存获取的应用到数据信息
		PackageManager pm = context.getPackageManager();
		List<PackageInfo> packages = pm.getInstalledPackages(0);
		for (int i = 0; i < packages.size(); i++) {
			PackageInfo packageInfo = packages.get(i);
			if (Util.isUserApp(packageInfo) && !packageInfo.packageName.equals(context.getPackageName())) {
				AppInfo info = new AppInfo();
				String appName = packageInfo.applicationInfo.loadLabel(pm).toString();
				info.setAppName(appName == null ? "" : appName);
				info.setPackageName(packageInfo.packageName);
				info.setIcon(packageInfo.applicationInfo.loadIcon(pm));
				appList.put(packageInfo.packageName, info);// 如果非系统应用，则添加到appList
			}
		}
		if (Constants.APPS.length > 0)
			for (String str : Constants.APPS) {
				try {
					appList.put(str, getAppInfo(str));
				} catch (NameNotFoundException e) {
					e.printStackTrace();
				}
			}
		return appList;
	}

	/**
	 * get app info by packageName
	 * 
	 * @param context
	 * @param packageName
	 * @return
	 * @author maqj
	 */
	private AppInfo getAppInfo(String packageName) throws NameNotFoundException {
		AppInfo info = new AppInfo();
		PackageManager pm = context.getPackageManager();
		PackageInfo packageInfo = pm.getPackageInfo(packageName, 0);
		String appName = packageInfo.applicationInfo.loadLabel(pm).toString();
		info.setAppName(appName == null ? "" : appName);
		info.setPackageName(packageInfo.packageName);
		info.setIcon(packageInfo.applicationInfo.loadIcon(pm));
		return info;
	}

	@Override
	protected void onStop() {
		super.onStop();
		if (adapter != null && adapter.getCheckedState())
			adapter.setCheckedState(false);
	}

	private void saveShortcutInfo(ArrayList<RecommendShortcutInfo> infoList) {
		JSONArray arr = new JSONArray();
		for (RecommendShortcutInfo info : infoList) {
			JSONObject obj = new JSONObject();
			try {
				obj.put("id", info.id);
				obj.put("index", info.index);
				obj.put("num", info.num);
				obj.put("size", info.size);
				obj.put("icon", info.icon);
				obj.put("name", info.name);
				obj.put("packageName", info.packageName);
				obj.put("url", info.url);
			} catch (JSONException e) {
				e.printStackTrace();
				Log.i(TAG, "save error!");
			}
			arr.put(obj);
		}
		sp.edit().putString("shorcutList", arr.toString()).commit();
	}

	private ArrayList<RecommendShortcutInfo> readShortcutInfo() {
		String json = sp.getString("shorcutList", "");
		ArrayList<RecommendShortcutInfo> infoList = new ArrayList<RecommendShortcutInfo>();
		if (json != "") {
			try {
				JSONArray arr = new JSONArray(json);
				int lenth = arr.length();
				for (int i = 0; i < lenth; i++) {
					RecommendShortcutInfo info = new RecommendShortcutInfo();
					JSONObject obj = arr.getJSONObject(i);
					info.id = obj.optInt("id");
					info.index = obj.optInt("index");
					info.num = obj.optInt("num");
					info.size = obj.optInt("size");
					info.icon = obj.optString("icon");
					info.name = obj.optString("name");
					info.packageName = obj.optString("packageName");
					info.url = obj.optString("url");
					infoList.add(info);
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return infoList;
	}
}
