package com.xcv58.joulerenergymanager;

/**
 * Created by xcv58 on 11/24/14.
 */
public class MyOption {
    private String name;
    private JoulerEnergyManageDaemon mService;

    public MyOption(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setService(JoulerEnergyManageDaemon serive) {
        mService = serive;
    }

    public boolean isSelected() {
        if (mService == null) {
            return false;
        }
        return mService.isChoice(name);
    }
}
