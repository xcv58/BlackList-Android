package com.xcv58.blacklist;

import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

/**
 * Created by xcv58 on 11/19/14.
 */
public class ListMobileActivity extends ListActivity {
    private final static String TAG = "blackList";
    private JoulerEnergyManageService mService;
    private boolean mBound = false;
    private List<MyPackageInfo> filteredList;
    private MobileArrayAdapter mobileArrayAdapter;
    private Intent joulerEnergyManageServiceIntent;

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            Log.d(TAG, "ServiceConnection");
            JoulerEnergyManageService.LocalBinder binder = (JoulerEnergyManageService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
            for (MyPackageInfo myPackageInfo : filteredList) {
                myPackageInfo.setService(mService);
            }
            Collections.sort(filteredList);
            mobileArrayAdapter.notifyDataSetChanged();
            Log.d(TAG, "bind service successful");
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        joulerEnergyManageServiceIntent = new Intent(this, JoulerEnergyManageService.class);
        startService(joulerEnergyManageServiceIntent);
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
                    resultList.add(new MyPackageInfo(packageInfo, mService, this));
                }
            } else {
//                System app
            }
        }
        return resultList;
    }

    private List<MyPackageInfo> getFilteredList(List<ResolveInfo> list, PackageManager packageManager) {
        List<MyPackageInfo> resultList = new ArrayList<MyPackageInfo>();
        HashSet<String> set = new HashSet<String>();
        String myPackageName = getPackageName();
        for (ResolveInfo resolveInfo : list) {
            String packageName = resolveInfo.activityInfo.packageName;
            if (!packageName.equals(myPackageName) && !set.contains(packageName)) {
                try {
                    PackageInfo packageInfo = packageManager.getPackageInfo(packageName, 0);
                    resultList.add(new MyPackageInfo(packageInfo, mService, this));
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
                set.add(resolveInfo.activityInfo.packageName);
            }
        }
        return resultList;
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
        Log.d(TAG, "bind service");

        bindService(joulerEnergyManageServiceIntent, mConnection, this.BIND_AUTO_CREATE);

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
}
