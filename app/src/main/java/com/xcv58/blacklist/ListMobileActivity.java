package com.xcv58.blacklist;

import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
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

        Intent intent = new Intent(this, JoulerEnergyManageService.class);
        Log.d(TAG, "bind service");
        startService(intent);
        bindService(intent, mConnection, this.BIND_AUTO_CREATE);

        PackageManager packageManager = getPackageManager();
        List<PackageInfo> list = packageManager.getInstalledPackages(0);
        filteredList = new ArrayList<MyPackageInfo>();
        for (PackageInfo packageInfo : list) {
            if (packageInfo.applicationInfo.sourceDir.startsWith("/data/app/")) {
                //Non-system app
            } else {
                //System app
            }
            if ((packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                filteredList.add(new MyPackageInfo(packageInfo, mService));
            }
        }
        Log.d(TAG, "end establish list");
        mobileArrayAdapter = new MobileArrayAdapter(this, filteredList);
        setListAdapter(mobileArrayAdapter);
        Log.d(TAG, "end onCreate");
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
    protected void onPause() {
        Log.d(TAG, "on Pause");
        mService.flush();
        super.onPause();
        return;
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "on Stop");
        super.onStop();
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }
}
