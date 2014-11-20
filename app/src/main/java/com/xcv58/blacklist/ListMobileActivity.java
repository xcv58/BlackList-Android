package com.xcv58.blacklist;

import android.app.ListActivity;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by xcv58 on 11/19/14.
 */
public class ListMobileActivity extends ListActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PackageManager packageManager = getPackageManager();
        List<PackageInfo> list = packageManager.getInstalledPackages(0);
        List<PackageInfo> filterList = new ArrayList<PackageInfo>();
        for (PackageInfo packageInfo : list) {
            if (packageInfo.applicationInfo.sourceDir.startsWith("/data/app/")) {
                //Non-system app
            } else {
                //System app
            }
            if ((packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                filterList.add(packageInfo);
            }
        }
        setListAdapter(new MobileArrayAdapter(this, filterList));

    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        PackageInfo selectedPackage = (PackageInfo) getListAdapter().getItem(position);
        Toast.makeText(this, selectedPackage.applicationInfo.loadLabel(getPackageManager()), Toast.LENGTH_SHORT).show();
        return;
    }
}
