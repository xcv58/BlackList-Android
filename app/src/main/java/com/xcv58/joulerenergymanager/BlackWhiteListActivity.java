package com.xcv58.joulerenergymanager;

import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * Created by xcv58 on 11/19/14.
 */
public class BlackWhiteListActivity extends ListActivity {
    private final static String TAG = "JoulerEnergyManageListActivity";
    private JoulerEnergyManageBlackWhiteListService mService;
    private boolean mBound = false;
    private List<MyPackageInfo> filteredList;
    private MobileArrayAdapter mobileArrayAdapter;
    private Intent blackWhiteListServiceIntent;

    private static int option;

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            Log.d(TAG, "ServiceConnection");
            JoulerEnergyManageBlackWhiteListService.LocalBinder binder = (JoulerEnergyManageBlackWhiteListService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
            for (MyPackageInfo myPackageInfo : filteredList) {
                myPackageInfo.setService(mService);
            }
            Collections.sort(filteredList);
            mobileArrayAdapter.notifyDataSetChanged();
            Log.d(TAG, "bind service successful");
            log("Enter", getListMode());
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        option = getIntent().getExtras().getInt(JoulerEnergyManageBlackWhiteListService.whichList);
        Log.d(TAG, "onCreate activity with " + ((option == JoulerEnergyManageBlackWhiteListService.BLACK_LIST_INTENT) ? "black list" : "white list"));
        setTitle(((option == JoulerEnergyManageBlackWhiteListService.BLACK_LIST_INTENT) ? R.string.blacklist : R.string.whitelist));


        blackWhiteListServiceIntent = new Intent(this, JoulerEnergyManageBlackWhiteListService.class);
        blackWhiteListServiceIntent.putExtra(JoulerEnergyManageBlackWhiteListService.whichList, option);
        startService(blackWhiteListServiceIntent);
    }

    private List<MyPackageInfo> getFilteredList(List<PackageInfo> list) {
        List<MyPackageInfo> resultList = new ArrayList<MyPackageInfo>();
        String myPackageName = getPackageName();
        for (PackageInfo packageInfo : list) {
//            if ((packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_EXTERNAL_STORAGE) == 0) {
//                resultList.add(new MyPackageInfo(packageInfo, mService, this));
//            }
            if (packageInfo.applicationInfo.sourceDir.startsWith("/data/app/")) {
//                Non-system app
                Log.d(TAG, packageInfo.applicationInfo.sourceDir);
                if (!packageInfo.packageName.equals(myPackageName)) {
                    resultList.add(new MyPackageInfo(packageInfo, this));
                }
            } else {
//                System app
            }
        }
        return resultList;
    }

    private List<MyPackageInfo> getFilteredList(List<ResolveInfo> list, PackageManager packageManager) {
        List<MyPackageInfo> resultList = new ArrayList<MyPackageInfo>();
        HashSet<String> set = getDupicateHashSet();
        String myPackageName = getPackageName();
        for (ResolveInfo resolveInfo : list) {
            String packageName = resolveInfo.activityInfo.packageName;
            if (!packageName.equals(myPackageName) && !packageName.equals("com.google.android.googlequicksearchbox") && !set.contains(packageName)) {
                resultList.add(new MyPackageInfo(resolveInfo, this));
                set.add(resolveInfo.activityInfo.packageName);
            }
        }
        return resultList;
    }

    private HashSet<String> getDupicateHashSet() {
        HashSet<String> set = new HashSet<String>();
        if (!isBlackMode()) {
            PackageManager pm =  getPackageManager();
            Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
            mainIntent.addCategory(Intent.CATEGORY_HOME);
            List<ResolveInfo> resolveInfoList = pm.queryIntentActivities(mainIntent, 0);
            for (ResolveInfo resolveInfo : resolveInfoList) {
                set.add(resolveInfo.activityInfo.packageName);
            }
        }
        return set;
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {

        MyPackageInfo selectedPackage = (MyPackageInfo) getListAdapter().getItem(position);
//        Toast.makeText(this, selectedPackage.getAppName(this), Toast.LENGTH_SHORT).show();
        mService.select(selectedPackage.getPackageName());
        mobileArrayAdapter.notifyDataSetChanged();
        return;
    }

    @Override
    public void onResume() {
        super.onResume();

        bindService(blackWhiteListServiceIntent, mConnection, this.BIND_AUTO_CREATE);

        Log.d(TAG, "onResume activity");
//        filteredList = getFilteredList(getPackageManager().getInstalledPackages(0));
        PackageManager pm =  getPackageManager();
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        filteredList = getFilteredList(pm.queryIntentActivities(mainIntent, 0), pm);

        mobileArrayAdapter = new MobileArrayAdapter(this, filteredList);
        setListAdapter(mobileArrayAdapter);
        mobileArrayAdapter.notifyDataSetChanged();
        return;
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "on Pause");
        mService.flush();
        super.onPause();
        log("Leave", getListMode());
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
        return;
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "on Stop");
        super.onStop();
    }

    private String getListMode() {
        if (option == JoulerEnergyManageBlackWhiteListService.BLACK_LIST_INTENT) {
            return JoulerEnergyManageBlackWhiteListService.BLACK;
        }
        if (option == JoulerEnergyManageBlackWhiteListService.WHITE_LIST_INTENT) {
            return JoulerEnergyManageBlackWhiteListService.WHITE;
        }
        return "UNKNOWN";
    }

    private boolean isBlackMode() {
        return option == JoulerEnergyManageBlackWhiteListService.BLACK_LIST_INTENT;
    }

    private JSONObject getListDetail() {
        JSONObject jsonObject = new JSONObject();
        try {
            for (MyPackageInfo myPackageInfo : filteredList) {
                jsonObject.put(myPackageInfo.getPackageName(), myPackageInfo.inList());
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    private void log(String key, String value) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(key, value);
            jsonObject.put("List mode", getListMode());
            jsonObject.put("List detail", getListDetail());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.i(TAG, jsonObject.toString());
    }
}
