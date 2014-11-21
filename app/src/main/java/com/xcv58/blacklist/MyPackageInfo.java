package com.xcv58.blacklist;

import android.content.Context;
import android.content.pm.PackageInfo;

/**
 * Created by xcv58 on 11/20/14.
 */
public class MyPackageInfo {
    private PackageInfo packageInfo;
    private JoulerEnergyManageService joulerEnergyManageService;

    public MyPackageInfo(PackageInfo packageInfo, JoulerEnergyManageService joulerEnergyManageService) {
        this.packageInfo = packageInfo;
        this.joulerEnergyManageService = joulerEnergyManageService;
    }

    public String getAppName(Context context) {
        return packageInfo.applicationInfo.loadLabel(context.getPackageManager()).toString();
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
}
