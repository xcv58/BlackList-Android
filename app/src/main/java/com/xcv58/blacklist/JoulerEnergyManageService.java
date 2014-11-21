package com.xcv58.blacklist;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.util.HashMap;
import java.util.Random;

/**
 * Created by xcv58 on 11/20/14.
 */
public class JoulerEnergyManageService extends Service {
    public static final String TAG = "JoulerEnergyManageService";

    private final IBinder mBinder = new LocalBinder();

    private HashMap<String, Integer> listMap;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate() executed");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
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
        Log.d(TAG, "onDestroy() executed");
    }

    public int getRandomNumber() {
        Log.d(TAG, "getRandomNumber() executed");
        return new Random().nextInt(100);
    }

    public boolean inList(String packageName) {
        if (!listMap.containsKey(packageName)) {
            listMap.put(packageName, 0);
            return false;
        }
        return listMap.get(packageName) == 1;
    }

    public void select(String packageName) {
        Integer num = listMap.get(packageName);
        if (num == null || num == 0) {
            listMap.put(packageName, 1);
        } else {
            listMap.put(packageName, 0);
        }
        return;
    }

    public class LocalBinder extends Binder {
        JoulerEnergyManageService getService() {
            // Return this instance of LocalService so clients can call public methods
            return JoulerEnergyManageService.this;
        }
    }

    private void initMap() {
        if (this.listMap == null) {
            listMap = new HashMap<String, Integer>();
        }
    }
}
