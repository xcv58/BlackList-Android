package com.xcv58.blacklist;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;

/**
 * Created by xcv58 on 11/20/14.
 */
public class MyPackageInfo implements Comparable<MyPackageInfo> {
    private PackageInfo packageInfo;
    private JoulerEnergyManageService joulerEnergyManageService;
    private String appName = null;

    public MyPackageInfo(PackageInfo packageInfo, JoulerEnergyManageService joulerEnergyManageService, Context context) {
        this.packageInfo = packageInfo;
        this.joulerEnergyManageService = joulerEnergyManageService;
        this.getAppName(context);
    }

    public String getAppName(Context context) {
        if (appName == null) {
            appName = packageInfo.applicationInfo.loadLabel(context.getPackageManager()).toString();
        }
        return appName;
    }

    public String getAppName() {
        return appName;
    }

    public void setService(JoulerEnergyManageService joulerEnergyManageService) {
        this.joulerEnergyManageService = joulerEnergyManageService;
        return;
    }

    public String getPackageName() {
        return packageInfo.packageName;
    }

    public boolean inList() {
        if (joulerEnergyManageService == null) {
            return false;
        }
        return joulerEnergyManageService.inList(this.getPackageName());
    }


    @Override
    public int compareTo(MyPackageInfo otherPackage) {
        if (otherPackage.joulerEnergyManageService != null || this.joulerEnergyManageService != null) {
            JoulerEnergyManageService tmpService = this.joulerEnergyManageService != null ? this.joulerEnergyManageService : otherPackage.joulerEnergyManageService;
            boolean thisInList = tmpService.inList(this.getPackageName());
            if (thisInList ^ tmpService.inList(otherPackage.getPackageName())) {
                return thisInList ? -1 : 1;
            }
        }
        return (this.getAppName()).compareTo(otherPackage.getAppName());
    }

    public Drawable getIcon(PackageManager packageManager) throws PackageManager.NameNotFoundException {
        return packageManager.getApplicationIcon(getPackageName());
    }
}
