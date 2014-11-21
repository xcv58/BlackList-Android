package com.xcv58.blacklist;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.io.StreamCorruptedException;
import java.util.HashMap;
import java.util.Random;

/**
 * Created by xcv58 on 11/20/14.
 */
public class JoulerEnergyManageService extends Service {
    public static final String TAG = "JoulerEnergyManageService";
    public static final String LIST_MAP = "LIST_MAP";

    private final IBinder mBinder = new LocalBinder();

    private HashMap<String, Integer> listMap;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Toast.makeText(context, intent.getAction(), Toast.LENGTH_SHORT).show();
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_RESUME_ACTIVITY);
        intentFilter.addAction(Intent.ACTION_PAUSE_ACTIVITY);
        registerReceiver(broadcastReceiver, intentFilter);
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

    public HashMap<String, Integer> readListMap() {
        try {
            FileInputStream fis = openFileInput(LIST_MAP);
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
            FileOutputStream fos = openFileOutput(LIST_MAP, MODE_PRIVATE);
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
        JoulerEnergyManageService getService() {
            // Return this instance of LocalService so clients can call public methods
            return JoulerEnergyManageService.this;
        }
    }

    private void initMap() {
        if (this.listMap == null) {
            listMap = this.readListMap();
        }
    }
}
