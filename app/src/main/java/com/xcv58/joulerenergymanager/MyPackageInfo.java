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
    private JoulerEnergyManageBlackWhiteListService joulerEnergyManageBlackWhiteListService;
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

    public void setService(JoulerEnergyManageBlackWhiteListService joulerEnergyManageBlackWhiteListService) {
        this.joulerEnergyManageBlackWhiteListService = joulerEnergyManageBlackWhiteListService;
        return;
    }

    public String getPackageName() {
        return packageName;
    }

    public Drawable getIcon() {
        return icon;
    }

    public boolean inList() {
        if (joulerEnergyManageBlackWhiteListService == null) {
            return false;
        }
        return joulerEnergyManageBlackWhiteListService.inList(this.getPackageName());
    }


    @Override
    public int compareTo(MyPackageInfo otherPackage) {
        if (otherPackage.joulerEnergyManageBlackWhiteListService != null || this.joulerEnergyManageBlackWhiteListService != null) {
            JoulerEnergyManageBlackWhiteListService tmpService = this.joulerEnergyManageBlackWhiteListService != null ? this.joulerEnergyManageBlackWhiteListService : otherPackage.joulerEnergyManageBlackWhiteListService;
            boolean thisInList = tmpService.inList(this.getPackageName());
            if (thisInList ^ tmpService.inList(otherPackage.getPackageName())) {
                return thisInList ? -1 : 1;
            }
        }
        return (this.getAppName()).compareTo(otherPackage.getAppName());
    }
}
