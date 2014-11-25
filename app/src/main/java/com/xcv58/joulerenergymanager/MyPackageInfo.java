package com.xcv58.joulerenergymanager;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;

/**
 * Created by xcv58 on 11/20/14.
 */
public class MyPackageInfo implements Comparable<MyPackageInfo> {
    private JoulerEnergyManageServiceBlackList joulerEnergyManageServiceBlackList;
    private String appName = null;
    private String packageName = null;
    private Drawable icon = null;

    public MyPackageInfo(PackageInfo packageInfo, Context context) {
        PackageManager pm = context.getPackageManager();
        appName = packageInfo.applicationInfo.loadLabel(pm).toString();
        packageName = packageInfo.packageName;
        this.initIcon(pm);
    }

    public MyPackageInfo(ResolveInfo resolveInfo, Context context) {
        PackageManager pm = context.getPackageManager();
        appName = resolveInfo.activityInfo.applicationInfo.loadLabel(pm).toString();
        packageName = resolveInfo.activityInfo.packageName;
        this.initIcon(pm);
    }

    private void initIcon(PackageManager pm) {
        try {
            icon = pm.getApplicationIcon(getPackageName());
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    public String getAppName() {
        return appName;
    }

    public void setService(JoulerEnergyManageServiceBlackList joulerEnergyManageServiceBlackList) {
        this.joulerEnergyManageServiceBlackList = joulerEnergyManageServiceBlackList;
        return;
    }

    public String getPackageName() {
        return packageName;
    }

    public Drawable getIcon() {
        return icon;
    }

    public boolean inList() {
        if (joulerEnergyManageServiceBlackList == null) {
            return false;
        }
        return joulerEnergyManageServiceBlackList.inList(this.getPackageName());
    }


    @Override
    public int compareTo(MyPackageInfo otherPackage) {
        if (otherPackage.joulerEnergyManageServiceBlackList != null || this.joulerEnergyManageServiceBlackList != null) {
            JoulerEnergyManageServiceBlackList tmpService = this.joulerEnergyManageServiceBlackList != null ? this.joulerEnergyManageServiceBlackList : otherPackage.joulerEnergyManageServiceBlackList;
            boolean thisInList = tmpService.inList(this.getPackageName());
            if (thisInList ^ tmpService.inList(otherPackage.getPackageName())) {
                return thisInList ? -1 : 1;
            }
        }
        return (this.getAppName()).compareTo(otherPackage.getAppName());
    }
}
