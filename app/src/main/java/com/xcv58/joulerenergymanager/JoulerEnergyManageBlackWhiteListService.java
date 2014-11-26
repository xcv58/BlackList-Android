package com.xcv58.joulerenergymanager;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.JoulerPolicy;
import android.os.Parcel;
import android.provider.Settings;
import android.util.Log;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import android.os.JoulerStats;
import android.os.JoulerStats.UidStats;
import android.os.RemoteException;

/**
 * Created by xcv58 on 11/20/14.
 */
public class JoulerEnergyManageBlackWhiteListService extends Service {
    public static final String TAG = "JoulerEnergyManageBlackWhiteListService";
    public static final String BLACK_LIST_MAP = "BLACK_LIST_MAP";
    public static final String WHITE_LIST_MAP = "WHITE_LIST_MAP";
    public static final String PACKAGE = "Package";
    public static final String USERID = "UserId";

    public static final int BLACK_LIST_INTENT = 1;
    public static final int WHITE_LIST_INTENT = 2;
    public static final String whichList = "Which List";

    private int option;
    private String listMapLocation;

    private boolean brightnessSetted = false;
    private int previousBrightness;
    private int previousBrightnessMode;

    private JoulerPolicy joulerPolicy;
    private JoulerStats joulerStats;

    private final IBinder mBinder = new LocalBinder();

    private HashMap<String, Integer> listMap;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            StringBuilder sb = new StringBuilder();

            String packageName = bundle.getString(PACKAGE);
            sb.append(PACKAGE);
            sb.append(": ");
            sb.append(packageName);
            sb.append("; ");
            int uid = bundle.getInt(USERID);
            sb.append(USERID);
            sb.append(": ");
            sb.append(uid);
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_RESUME_ACTIVITY)) {
                if (isBlackList() && inList(packageName)) {
                    Log.d(TAG, "Enter energy save mode by Black rule, " + packageName);
                    saveMode(uid);
                }
                if (!isBlackList() && !inList(packageName)) {
                    Log.d(TAG, "Enter energy save mode by White rule, " + packageName);
                    saveMode(uid);
                }
            } else if (action.equals(Intent.ACTION_PAUSE_ACTIVITY)) {
                if (isBlackList() && inList(packageName)) {
                    Log.d(TAG, "Reset brightness, BLACK");
                    resetBrightness();
                }
                if (!isBlackList() && !inList(packageName)) {
                    Log.d(TAG, "Reset brightness, WHITE");
                    resetBrightness();
                }
            }
            Log.d(TAG, intent.getAction() + "," + System.currentTimeMillis() + ", " + sb.toString() + ", Energy usage: " + getEnergy(uid));
        }
    };

    private void print() {
        Log.d(TAG, "START PRINT");
        try {
            byte[] bytes = joulerPolicy.getStatistics();
            Parcel parcel = Parcel.obtain();
            parcel.unmarshall(bytes, 0, bytes.length);
            parcel.setDataPosition(0); // this is extremely important!
            JoulerStats joulerStats = new JoulerStats(parcel);
            Log.d(TAG, "SIZE: " + joulerStats.mUidArray.size());
            for( int i = 0; i < joulerStats.mUidArray.size(); i++){
                UidStats u = joulerStats.mUidArray.valueAt(i);
                Log.i(TAG, "Uid: "+u.getUid()+" Pkg: "+u.packageName);
                Log.i(TAG, "Uid: "+u.getUid()+"Fg= "+u.getFgEnergy()+" Bg= "+u.getBgEnergy()+" Cpu= "+u.getCpuEnergy()+" Wakelock= "+u.getWakelockEnergy()+" Wifi= "+u.getWifiEnergy()
                        + " Mobile Data= "+u.getMobileDataEnergy()+" Wifi Data= "+u.getWifiDataEnergy()+" Video= "+u.getVideoEnergy());
                Log.i(TAG, "Uid: "+u.getUid()+" Frames= "+u.getFrame()+" Launches= "+u.getCount()+" Usage= "+u.getUsageTime());
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        Log.d(TAG, "END PRINT");
    }

    private double getEnergy(int uid) {
        try {
            byte[] bytes = joulerPolicy.getStatistics();
            Parcel parcel = Parcel.obtain();
            parcel.unmarshall(bytes, 0, bytes.length);
            parcel.setDataPosition(0); // this is extremely important!
            JoulerStats joulerStats = new JoulerStats(parcel);
            for( int i = 0; i < joulerStats.mUidArray.size(); i++){
                UidStats u = joulerStats.mUidArray.valueAt(i);
                if (u.getUid() == uid) {
                    return u.getFgEnergy() + u.getBgEnergy();
                }
//                Log.i(TAG, "Uid: "+u.getUid()+" Pkg: "+u.packageName);
//                Log.i(TAG, "Uid: "+u.getUid()+"Fg= "+u.getFgEnergy()+" Bg= "+u.getBgEnergy()+" Cpu= "+u.getCpuEnergy()+" Wakelock= "+u.getWakelockEnergy()+" Wifi= "+u.getWifiEnergy()
//                        + " Mobile Data= "+u.getMobileDataEnergy()+" Wifi Data= "+u.getWifiDataEnergy()+" Video= "+u.getVideoEnergy());
//                Log.i(TAG, "Uid: "+u.getUid()+" Frames= "+u.getFrame()+" Launches= "+u.getCount()+" Usage= "+u.getUsageTime());
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return -1.0;
    }


    private void saveMode(int uid) {
        int brightness = 1;
        Log.d(TAG, "Enable saveMode, brightness: " + brightness);
        setBrightness(brightness);
        joulerPolicy.resetPriority(uid, 20);
    }


    private void setBrightness(int brightness) {
        if (brightnessSetted) {
            return;
        }
        try {
            previousBrightness = android.provider.Settings.System.getInt(
                    getContentResolver(), android.provider.Settings.System.SCREEN_BRIGHTNESS);
            previousBrightnessMode = android.provider.Settings.System.getInt(
                    getContentResolver(), android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE);
            Log.d(TAG, "Previous brightness is: " + previousBrightness + ". Mode is: " + previousBrightnessMode);
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }

        Log.d(TAG, "Set brightness to: " + brightness);
        android.provider.Settings.System.putInt(getContentResolver(),
                android.provider.Settings.System.SCREEN_BRIGHTNESS,
                brightness);
        android.provider.Settings.System.putInt(getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
        brightnessSetted = true;
    }

    private void resetBrightness() {
        if (!brightnessSetted) {
            return;
        }
        Log.d(TAG, "reset brightness to: "+ previousBrightness
                + ", mode to: " +
                (previousBrightnessMode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC ? "auto" : "manual"));
        android.provider.Settings.System.putInt(getContentResolver(),
                android.provider.Settings.System.SCREEN_BRIGHTNESS,
                previousBrightness);
        android.provider.Settings.System.putInt(getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                previousBrightnessMode);
        brightnessSetted = false;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_RESUME_ACTIVITY);
        intentFilter.addAction(Intent.ACTION_PAUSE_ACTIVITY);
        registerReceiver(broadcastReceiver, intentFilter);

        Log.d(TAG, "get JoulerPolicy");
        joulerPolicy = (android.os.JoulerPolicy)getSystemService(JOULER_SERVICE);
        joulerStats = new JoulerStats();

        Log.d(TAG, "onCreate() executed");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        option = intent.getExtras().getInt(JoulerEnergyManageBlackWhiteListService.whichList);
        Log.d(TAG, "Start service with " + ((option == JoulerEnergyManageBlackWhiteListService.BLACK_LIST_INTENT) ? "black list" : "white list"));
        listMapLocation = (option == JoulerEnergyManageBlackWhiteListService.BLACK_LIST_INTENT) ? BLACK_LIST_MAP : WHITE_LIST_MAP;
        listMap = readListMap();
        if (isWhiteList()) {
            // set itself in white list and all other non luncher app as whitelist.
            putAllNonLuncherInList();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        this.initMap();
        Log.d(TAG, "onBind() executed");
        return mBinder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(broadcastReceiver);
        flush();
        resetBrightness();
        Log.d(TAG, "onDestroy() executed " + getListName() + " " + listMapLocation);
    }

    private String getListName() {
        return (option == BLACK_LIST_INTENT) ? BLACK_LIST_MAP : WHITE_LIST_MAP;
    }

    public boolean inList(String packageName) {
        this.initMap();
        if (!listMap.containsKey(packageName)) {
            listMap.put(packageName, 0);
            return false;
        }
        return listMap.get(packageName) == 1;
    }

    private void putAllNonLuncherInList() {
        PackageManager pm =  getPackageManager();
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> resolveInfoList = pm.queryIntentActivities(mainIntent, 0);
        List<PackageInfo> packageInfoList = pm.getInstalledPackages(0);
        HashMap<String, Integer> map = new HashMap<String, Integer>();
        for (PackageInfo packageInfo : packageInfoList) {
            map.put(packageInfo.packageName, 0);
        }
        for (ResolveInfo resolveInfo : resolveInfoList) {
            map.put(resolveInfo.activityInfo.packageName, map.get(resolveInfo.activityInfo.packageName) + 1);
        }
        Iterator e = map.entrySet().iterator();
        while (e.hasNext()) {
            Map.Entry<String, Integer> entry = (Map.Entry<String, Integer>) e.next();
            if (entry.getValue() == 0) {
                Log.d(TAG, "Set " + entry.getKey() + " in white list");
                listMap.put(entry.getKey(), 1);
            }
        }
        listMap.put(getPackageName(), 1);
        listMap.put("com.google.android.googlequicksearchbox", 1);
    }

    public boolean isBlackList() {
        return option == BLACK_LIST_INTENT;
    }

    public boolean isWhiteList() {
        return option == WHITE_LIST_INTENT;
    }

    public void select(String packageName) {
        this.initMap();
        Integer num = listMap.get(packageName);
        if (num == null || num == 0) {
            listMap.put(packageName, 1);
        } else {
            listMap.put(packageName, 0);
        }
        return;
    }

    public HashMap<String, Integer> readListMap() {
        Log.d(TAG, "read map from file: " + listMapLocation);
        try {
            FileInputStream fis = openFileInput(listMapLocation);
            ObjectInputStream is = new ObjectInputStream(fis);
            HashMap<String, Integer> map = (HashMap<String, Integer>) is.readObject();
            is.close();
            return map;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new HashMap<String, Integer>();
    }


    public boolean flush() {
//  write listMap to file
        try {
            FileOutputStream fos = openFileOutput(listMapLocation, MODE_PRIVATE);
            ObjectOutputStream os = new ObjectOutputStream(fos);
            os.writeObject(listMap);
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public class LocalBinder extends Binder {
        JoulerEnergyManageBlackWhiteListService getService() {
            // Return this instance of LocalService so clients can call public methods
            return JoulerEnergyManageBlackWhiteListService.this;
        }
    }

    private void initMap() {
        if (this.listMap == null) {
            listMap = this.readListMap();
        }
    }
}
