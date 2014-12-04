package com.xcv58.joulerenergymanager;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.BatteryManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.JoulerPolicy;
import android.os.Parcel;
import android.provider.Settings;
import android.util.Log;
import android.support.v4.app.NotificationCompat;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

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

    private static final int NOT_INL_IST = 0;
    private static final int IN_LIST = 1;
    private static final int OUT_LIST = 2;

    private static final String ENTER_SAVE_MODE = "Enter save mode";
    private static final String LEAVE_SAVE_MODE = "Leave save mode";
    private static final String ON_START_COMMAND = "Enter save mode";

    private static final String NUM_IN_LIST = "Num in list";
    private static final String TOTAL_CONSUMPTION_IN_LIST = "Total consumption in list";
    private static final String TOTAL_FG_CONSUMPTION_IN_LIST = "Total fg consumption in list";
    private static final String TOTAL_BG_CONSUMPTION_IN_LIST = "Total bg consumption in list";
    private static final String NUM_NOT_IN_LIST = "Num not in list";
    private static final String TOTAL_CONSUMPTION_NOT_IN_LIST = "Total consumption not in list";
    private static final String TOTAL_FG_CONSUMPTION_NOT_IN_LIST = "Total fg consumption not in list";
    private static final String TOTAL_BG_CONSUMPTION_NOT_IN_LIST = "Total bg consumption not in list";

//    public static final int LOW_BRIGHTNESS = 10;
//    public static final int LOW_PRIORITY = 20;
    public static final double MAX_THRESHOLD = 1.0;
    public static final double MIN_THRESHOLD = 0.2;
    public static final String WHICH_LIST = "List mode";
    public static final String BLACK = "Black";
    public static final String WHITE = "White";
    private static final String ENERGY_DETAIL = "Energy detail";
    private static int option;
    private String listMapLocation;

//    private boolean brightnessSetted = false;
    private HashMap<Integer, Integer> priorityMap;
    private static final int DEFAULT_PRIORITY = 0;

    private static final int notificationId = 1;
    private static final int BRIGHT_NOTIFICATION_ID = 2;
    private NotificationCompat.Builder notificationBuilder;

    private BlackWhiteListMetaData metaData;

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
                    if (!outList(packageName)) {
                        saveMode(uid, packageName);
                    }
                }
            } else if (action.equals(Intent.ACTION_PAUSE_ACTIVITY)) {
                if (isBlackList() && inList(packageName)) {
//                    Log.d(TAG, "Reset brightness, BLACK");
                    leaveMode(uid, packageName);
//                    resetBrightness(packageName);
                }
                if (!isBlackList() && !inList(packageName)) {
                    if (!outList(packageName)) {
                        leaveMode(uid, packageName);
                    }
//                    Log.d(TAG, "Reset brightness, WHITE");
//                    resetBrightness(packageName);
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

            if (!metaData.isLevelChanged(level)) {
                return;
            }
            JSONObject detail = getJsonDetail();
            try {
                double totalInList = detail.getDouble(TOTAL_CONSUMPTION_IN_LIST);
                double totalNotInList = detail.getDouble(TOTAL_CONSUMPTION_NOT_IN_LIST);
                int numInList = detail.getInt(NUM_IN_LIST);
                int numNotINList = detail.getInt(NUM_NOT_IN_LIST);
                double meanInList = 0.0;
                double meanNotInList = 0.0;
                if (numInList != 0 && totalInList > 0.0) {
                    meanInList = totalInList / numInList;
                }
                if (numNotINList != 0 && totalNotInList > 0.0) {
                    meanNotInList = totalNotInList / numNotINList;
                }
                double ratio = meanInList / meanNotInList;
                makeNotification("Battery Level change", "Level: " + level + ", ratio: " + ratio);
                if (!isBlackList()) {
                    ratio = meanNotInList / meanInList;
                }
                if (ratio >= MAX_THRESHOLD) {
                    punish();
                }
                if (ratio <= MIN_THRESHOLD) {
                    forgive();
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };

    private void punish() {
        int priority = metaData.getGlobalPriority();
        makeNotification("Punish", "Punish priority: " + priority);
        if (priority == 20) {
            metaData.setGlobalPriority(priority + 1);
            // rateLimit
            setRateLimit();
        } else if (priority == 21) {
            // do nothing but put notification;
            makeNotification((isBlackList() ? MainActivity.BLACK_LIST : MainActivity.WHITE_LIST), (isBlackList() ? "Apps in BlackList use too much energy" : "Apps not in WhiteList use too much energy"));
        } else {
            priority++;
            metaData.setGlobalPriority(priority);
            setPriorityForAll();
        }
        return;
    }

    private void forgive() {
        int priority = metaData.getGlobalPriority();
        makeNotification("Forgive", "Forgive priority: " + priority);
        if (priority == 21) {
            // rateLimit
            metaData.setGlobalPriority(priority - 1);
            restoreRateLimit();
        } else if (priority == 0) {
            // do nothing but put notification;
            makeNotification((isBlackList() ? MainActivity.BLACK_LIST : MainActivity.WHITE_LIST), (isBlackList() ? "Apps in BlackList use few energy" : "Apps not in WhiteList use few energy"));
        } else {
            priority--;
            metaData.setGlobalPriority(priority);
            setPriorityForAll();
        }
        return;
    }

    private void setRateLimit() {
        metaData.setRateLimitFlag(true);
        synchronized(joulerStats) {
            try{
                for(int i=0; i< joulerStats.mUidArray.size(); i++) {
                    UidStats u = joulerStats.mUidArray.valueAt(i);
                    if(u.getUid() < 10000)
                        continue;
                    if (outList(u.packageName)) {
                        continue;
                    }
                    if ((isBlackList() && inList(u.packageName))
                        || (!isBlackList() && !inList(u.packageName))) {
                        if (!metaData.alreadySetRateLimit(u.packageName)) {
                            joulerPolicy.rateLimitForUid(u.getUid());
                            metaData.setRateLimit(u.packageName);
                        }
                    }
                }
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
        return;
    }

    private void restoreRateLimit() {
        metaData.setRateLimitFlag(false);
        synchronized(joulerStats) {
            try{
                for(int i=0; i< joulerStats.mUidArray.size(); i++) {
                    UidStats u = joulerStats.mUidArray.valueAt(i);
                    if(u.getUid() < 10000)
                        continue;
                    if (outList(u.packageName)) {
                        continue;
                    }
                    if ((isBlackList() && inList(u.packageName))
                            || (!isBlackList() && !inList(u.packageName))) {
                        if (metaData.alreadySetRateLimit(u.packageName)) {
                            joulerPolicy.rateLimitForUid(u.getUid());
                            metaData.removeRateLimit(u.packageName);
                        }
                    }
                }
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
        return;
    }

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
            joulerStats = new JoulerStats(parcel);
            double fgConsumptionInlist = 0.0;
            double bgConsumptionInlist = 0.0;
            double fgConsumptionNotInlist = 0.0;
            double bgConsumptionNotInlist = 0.0;
            int numInlistPackage = 0;
            int numNotInlistPackage = 0;

            for (int i = 0; i < joulerStats.mUidArray.size(); i++) {
                UidStats u = joulerStats.mUidArray.valueAt(i);
                if (u.packageName == null) {
                    continue;
                }
                json.put(u.packageName, getJSON(u));
                if (outList(u.packageName)) {
                    continue;
                }
                if (inList(u.packageName)) {
                    numInlistPackage++;
                    fgConsumptionInlist += u.getFgEnergy();
                    bgConsumptionInlist += u.getBgEnergy();
                } else {
                    numNotInlistPackage++;
                    fgConsumptionNotInlist += u.getFgEnergy();
                    bgConsumptionNotInlist += u.getBgEnergy();
                }
            }
            json.put(NUM_IN_LIST, numInlistPackage);
            json.put(TOTAL_CONSUMPTION_IN_LIST, fgConsumptionInlist + bgConsumptionInlist);
            json.put(TOTAL_FG_CONSUMPTION_IN_LIST, fgConsumptionInlist);
            json.put(TOTAL_BG_CONSUMPTION_IN_LIST, bgConsumptionInlist);
            json.put(NUM_NOT_IN_LIST, numNotInlistPackage);
            json.put(TOTAL_CONSUMPTION_NOT_IN_LIST, fgConsumptionNotInlist + bgConsumptionNotInlist);
            json.put(TOTAL_FG_CONSUMPTION_NOT_IN_LIST, fgConsumptionNotInlist);
            json.put(TOTAL_BG_CONSUMPTION_NOT_IN_LIST, bgConsumptionNotInlist);
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
            json.put("inList", inList(u.packageName));
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
        makeNotification(ENTER_SAVE_MODE, packagename);
//        Log.d(TAG, "Enable saveMode, brightness: " + LOW_BRIGHTNESS);

        joulerPolicy.resetPriority(uid, metaData.getGlobalPriority() - 10);
        metaData.addForegroundPriority(uid);
        if (metaData.alreadySetRateLimit(packagename)) {
            joulerPolicy.rateLimitForUid(uid);
            metaData.setRateLimit(packagename);
        }
        setBrightness();
//        setPriority(uid, packagename);
    }

    private void leaveMode(int uid, String packagename) {
        log(LEAVE_SAVE_MODE, packagename);
        if (metaData.isRateLimited()) {
            if (!metaData.alreadySetRateLimit(packagename)) {
                joulerPolicy.rateLimitForUid(uid);
                metaData.setRateLimit(packagename);
            }
        }
        this.resetBrightness();
        if (metaData.isForegroundPriority(uid)) {
            joulerPolicy.resetPriority(uid, metaData.getGlobalPriority());
        }
    }

    public void setPriorityForAll() {
        if (joulerPolicy == null) {
            joulerPolicy = (android.os.JoulerPolicy)getSystemService(JOULER_SERVICE);
        }
        try {
            byte[] bytes = joulerPolicy.getStatistics();
            Parcel parcel = Parcel.obtain();
            parcel.unmarshall(bytes, 0, bytes.length);
            parcel.setDataPosition(0); // this is extremely important!
            JoulerStats joulerStats = new JoulerStats(parcel);
            for (int i = 0; i < joulerStats.mUidArray.size(); i++) {
                UidStats u = joulerStats.mUidArray.valueAt(i);
                if (u.getUid() < 10000 || u.packageName == null || u.packageName.isEmpty()) {
                    continue;
                }
                if (outList(u.packageName)) {
                    continue;
                }
                if ((isBlackList() && inList(u.packageName)) || (!isBlackList() && !inList(u.packageName))) {
                    Log.d(TAG, u.getUid() + ", " + u.packageName);
                    joulerPolicy.resetPriority(u.getUid(), metaData.getGlobalPriority());
                    if (!priorityMap.containsKey(u.getUid())) {
                        priorityMap.put(u.getUid(), DEFAULT_PRIORITY);
                    }
                }
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void resetPriority() {
        Iterator<Map.Entry<Integer, Integer>> iterator = priorityMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, Integer> entry = iterator.next();
            int uid = entry.getKey();
            int priority = entry.getValue();
            Log.d(TAG, uid + ", ");
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


    private void setBrightness() {
        if (metaData.isBrightnessSet()) {
            return;
        }

        try {
            int previousBrightness = android.provider.Settings.System.getInt(
                    getContentResolver(), android.provider.Settings.System.SCREEN_BRIGHTNESS);
            int previousBrightnessMode = android.provider.Settings.System.getInt(
                    getContentResolver(), android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE);
            metaData.setPreviousBrightness(previousBrightness, previousBrightnessMode);
//            Log.d(TAG, "Previous brightness is: " + previousBrightness + ". Mode is: " + previousBrightnessMode);
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }

//        Log.d(TAG, "Set brightness to: " + brightness);
        makeNotification("Brightness", metaData.getPreviousBrightness() + "->" + metaData.getLowBrightness(), BRIGHT_NOTIFICATION_ID);
        android.provider.Settings.System.putInt(getContentResolver(),
                android.provider.Settings.System.SCREEN_BRIGHTNESS,
                metaData.getLowBrightness());
        android.provider.Settings.System.putInt(getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
        metaData.setBrightness();
    }

    private void resetBrightness() {
        if (!metaData.isBrightnessSet()) {
            return;
        }
//
//         (previousBrightnessMode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC ? "auto" : "manual"));
        android.provider.Settings.System.putInt(getContentResolver(),
                android.provider.Settings.System.SCREEN_BRIGHTNESS,
                metaData.getPreviousBrightness());
        android.provider.Settings.System.putInt(getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                metaData.getPreviousBrightnessMode());
        metaData.resetBrightness();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        metaData = new BlackWhiteListMetaData();

        foreground();

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

        updateNotification();
        listMapLocation = (option == JoulerEnergyManageBlackWhiteListService.BLACK_LIST_INTENT) ? BLACK_LIST_MAP : WHITE_LIST_MAP;
        listMap = readListMap();
        if (isWhiteList()) {
            // set itself in white list and all other non luncher app as whitelist.
            putAllNonLuncherInList();
            putAllLuncherInList();
        }
        return START_REDELIVER_INTENT;
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
        stopForeground();
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
            listMap.put(packageName, NOT_INL_IST);
            return false;
        }
        return listMap.get(packageName) == IN_LIST;
    }

    private boolean outList(String packageName) {
        this.initMap();
        if (!listMap.containsKey(packageName)) {
            return false;
        }
        return listMap.get(packageName) == OUT_LIST;
    }


    public void makeNotification(String title, String text) {
        makeNotification(title,text, metaData.getNotificationId());
        return;
    }

    public void makeNotification(String title, String text, int id) {
        NotificationCompat.Builder mBuilder = new NotificationCompat.
                Builder(getBaseContext())
                .setSmallIcon(R.drawable.notification_icon)
                .setContentTitle(title)
                .setContentText(text);
        NotificationManager mNotifyMgr =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mNotifyMgr.notify(id, mBuilder.build());
        return;
    }

    private void foreground() {
        notificationBuilder = new NotificationCompat.
                Builder(getBaseContext())
                .setSmallIcon(R.drawable.notification_icon);
        startForeground(notificationId, notificationBuilder.build());
        return;
    }

    private void updateNotification() {
        Intent intent = new Intent(getBaseContext(), BlackWhiteListActivity.class);
        intent.putExtra(JoulerEnergyManageBlackWhiteListService.whichList, option);
        PendingIntent pendingIntent = PendingIntent.getActivity(getBaseContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        notificationBuilder.setContentIntent(pendingIntent)
                .setContentTitle(getResources().getString(R.string.notification_title))
                .setContentText((isBlackList() ? MainActivity.BLACK_LIST : MainActivity.WHITE_LIST) + getResources().getString(R.string.notification_suffix));
        startForeground(notificationId, notificationBuilder.build());
    }

    private void stopForeground() {
        stopForeground(true);
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
                listMap.put(entry.getKey(), OUT_LIST);
            }
        }
        listMap.put(getPackageName(), OUT_LIST);
    }

    private void putAllLuncherInList() {
        PackageManager pm =  getPackageManager();
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_HOME);
        List<ResolveInfo> resolveInfoList = pm.queryIntentActivities(mainIntent, 0);

        for (ResolveInfo resolveInfo : resolveInfoList) {
            listMap.put(resolveInfo.activityInfo.packageName, OUT_LIST);
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
        if (num == null) {
            listMap.put(packageName, NOT_INL_IST);
        } else if (num == IN_LIST) {
            listMap.put(packageName, NOT_INL_IST);
        } else if (num == NOT_INL_IST) {
            listMap.put(packageName, IN_LIST);
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
