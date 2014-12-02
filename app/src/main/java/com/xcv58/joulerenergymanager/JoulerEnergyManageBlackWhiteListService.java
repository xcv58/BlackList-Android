package com.xcv58.joulerenergymanager;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.DropBoxManager;
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

import org.json.JSONException;
import org.json.JSONObject;

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

    private static final String ENTER_SAVE_MODE = "Enter save mode";
    private static final String LEAVE_SAVE_MODE = "Leave save mode";
    private static final String ON_START_COMMAND = "Enter save mode";

    public static final int LOW_BRIGHTNESS = 10;
    public static final int LOW_PRIORITY = 20;
    public static final String WHICH_LIST = "List mode";
    public static final String BLACK = "Black";
    public static final String WHITE = "White";
    private static final String ENERGY_DETAIL = "Energy detail";
    private static int option;
    private String listMapLocation;

    private boolean brightnessSetted = false;
    private int previousBrightness;
    private int previousBrightnessMode;
    private HashMap<Integer, Integer> priorityMap;

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
//                    Log.d(TAG, "Enter energy save mode by Black rule, " + packageName);
                    saveMode(uid, packageName);
                }
                if (!isBlackList() && !inList(packageName)) {
//                    Log.d(TAG, "Enter energy save mode by White rule, " + packageName);
                    saveMode(uid, packageName);
                }
            } else if (action.equals(Intent.ACTION_PAUSE_ACTIVITY)) {
                if (isBlackList() && inList(packageName)) {
//                    Log.d(TAG, "Reset brightness, BLACK");
                    resetBrightness(packageName);
                }
                if (!isBlackList() && !inList(packageName)) {
//                    Log.d(TAG, "Reset brightness, WHITE");
                    resetBrightness(packageName);
                }
            }
//            Log.d(TAG, intent.getAction() + "," + System.currentTimeMillis() + ", " + sb.toString() + ", Energy usage: " + getEnergy(uid));
        }
    };

    BroadcastReceiver onBatteryChange = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL;
            try {
                JSONObject json = new JSONObject();
                json.put("currentBatteryLevel", level);
                json.put("isCharging", isCharging);
                json.put(WHICH_LIST, (isBlackList() ? BLACK : WHITE));
                json.put(ENERGY_DETAIL, getJsonDetail());
                Log.i(TAG, json.toString());
            }catch (JSONException e) {
                Log.i(TAG, "Error @ onBatteryChange receiver: "+e.getMessage());
            }
        }
    };

    BroadcastReceiver screenReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                JSONObject json = new JSONObject();
                if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                    json.put("Screen", "OFF");
                } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                    json.put("Screen", "ON");
                }
                json.put(WHICH_LIST, (isBlackList() ? BLACK : WHITE));
                Log.i(TAG, json.toString());
            }catch (JSONException e) {
                Log.i(TAG, "Error @ onBatteryChange receiver: "+e.getMessage());
            }
        }
    };

    private JSONObject getJsonDetail() {
        JSONObject json = new JSONObject();
        try {
            byte[] bytes = joulerPolicy.getStatistics();
            Parcel parcel = Parcel.obtain();
            parcel.unmarshall(bytes, 0, bytes.length);
            parcel.setDataPosition(0); // this is extremely important!
            JoulerStats joulerStats = new JoulerStats(parcel);
            for (int i = 0; i < joulerStats.mUidArray.size(); i++) {
                UidStats u = joulerStats.mUidArray.valueAt(i);
                if (u.packageName == null) {
                    continue;
                }
                json.put(u.packageName, getJSON(u));
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    private JSONObject getJSON(UidStats u) {
        JSONObject json = new JSONObject();
        try {
            json.put("packagename", u.packageName);
            json.put("FgEnergy", u.getFgEnergy());
            json.put("BgEnergy", u.getBgEnergy());
            json.put("CPU", u.getCpuEnergy());
            json.put("Wakelock", u.getWakelockEnergy());
            json.put("Wifi", u.getWifiEnergy());
            json.put("Mobile Data", u.getMobileDataEnergy());
            json.put("Wifi Data", u.getWifiDataEnergy());
            json.put("Video", u.getVideoEnergy());
            json.put("Video", u.getVideoEnergy());
            json.put("Frames", u.getFrame());
            json.put("Launches", u.getCount());
            json.put("Usage time", u.getUsageTime());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
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


    private void saveMode(int uid, String packagename) {
        log(ENTER_SAVE_MODE, packagename);
//        Log.d(TAG, "Enable saveMode, brightness: " + LOW_BRIGHTNESS);

        setBrightness(LOW_BRIGHTNESS);
        resetPriority(uid, packagename);
    }

    private void resetPriority(int uid, String packagename) {
        if (!priorityMap.containsKey(uid)) {
//            int previousPriority = joulerPolicy.getPriority(uid);
            int previousPriority = 0;
            priorityMap.put(uid, previousPriority);
            joulerPolicy.resetPriority(uid, LOW_PRIORITY);
//            Log.d(TAG, "Set priority " + uid + " " + packagename + " to " + LOW_PRIORITY + ". Previous priority: " + previousPriority);
        }
        return;
    }

    private void resetPriority() {
        Iterator<Map.Entry<Integer, Integer>> iterator = priorityMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, Integer> entry = iterator.next();
            int uid = entry.getKey();
            int priority = entry.getValue();
            joulerPolicy.resetPriority(uid, priority);
        }
    }

    private void log(String key, String value) {
        JSONObject json = new JSONObject();
        try {
            json.put(key, value);
            json.put(WHICH_LIST, (isBlackList() ? BLACK : WHITE));
            if (!key.equals(ON_START_COMMAND)) {
                json.put(ENERGY_DETAIL, getJsonDetail());
            }
        } catch (JSONException e1) {
            e1.printStackTrace();
        }
        Log.i(TAG, json.toString());
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
//            Log.d(TAG, "Previous brightness is: " + previousBrightness + ". Mode is: " + previousBrightnessMode);
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }

//        Log.d(TAG, "Set brightness to: " + brightness);
        android.provider.Settings.System.putInt(getContentResolver(),
                android.provider.Settings.System.SCREEN_BRIGHTNESS,
                brightness);
        android.provider.Settings.System.putInt(getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
        brightnessSetted = true;
    }

    private void resetBrightness(String packagename) {
        log(LEAVE_SAVE_MODE, packagename);
        this.resetBrightness();
    }

    private void resetBrightness() {
        if (!brightnessSetted) {
            return;
        }
//
//         (previousBrightnessMode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC ? "auto" : "manual"));
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

//        Log.d(TAG, "get JoulerPolicy");
        joulerPolicy = (android.os.JoulerPolicy)getSystemService(JOULER_SERVICE);
        joulerStats = new JoulerStats();

        priorityMap = new HashMap<Integer, Integer>();

        IntentFilter batteryChangeIntentFilter = new IntentFilter();
        batteryChangeIntentFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(onBatteryChange, batteryChangeIntentFilter);
        IntentFilter screenOnOffIntentFilter = new IntentFilter();
        screenOnOffIntentFilter.addAction(Intent.ACTION_SCREEN_ON);
        screenOnOffIntentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(screenReceiver, screenOnOffIntentFilter);

//        Log.d(TAG, "onCreate() executed");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String startMode = "null intent";
        if (intent != null) {
            Bundle bundle = intent.getExtras();
            if (bundle == null) {
                startMode = "null bundle";
            } else {
                option = bundle.getInt(JoulerEnergyManageBlackWhiteListService.whichList);
                if (option == BLACK_LIST_INTENT) {
                    startMode = BLACK;
                } else if (option == WHITE_LIST_INTENT) {
                    startMode = WHITE;
                }
            }
        }
        log(ON_START_COMMAND, startMode);

        listMapLocation = (option == JoulerEnergyManageBlackWhiteListService.BLACK_LIST_INTENT) ? BLACK_LIST_MAP : WHITE_LIST_MAP;
        listMap = readListMap();
        if (isWhiteList()) {
            // set itself in white list and all other non luncher app as whitelist.
            putAllNonLuncherInList();
            putAllLuncherInList();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        this.initMap();
//        Log.d(TAG, "onBind() executed");
        return mBinder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(broadcastReceiver);
        unregisterReceiver(screenReceiver);
        unregisterReceiver(onBatteryChange);
        flush();
        resetBrightness();
        resetPriority();
//        Log.d(TAG, "onDestroy() executed " + getListName() + " " + listMapLocation);
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
                listMap.put(entry.getKey(), 1);
            }
        }
        listMap.put(getPackageName(), 1);
    }

    private void putAllLuncherInList() {
        PackageManager pm =  getPackageManager();
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_HOME);
        List<ResolveInfo> resolveInfoList = pm.queryIntentActivities(mainIntent, 0);

        for (ResolveInfo resolveInfo : resolveInfoList) {
            listMap.put(resolveInfo.activityInfo.packageName, 1);
        }
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
//        Log.d(TAG, "read map from file: " + listMapLocation);
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
