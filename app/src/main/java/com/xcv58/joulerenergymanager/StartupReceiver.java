package com.xcv58.joulerenergymanager;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by xcv58 on 11/25/14.
 */
public class StartupReceiver extends BroadcastReceiver {
    public final static String TAG = "JoulerEnergyManageStartup";
    public final static String START_MODE = "Start mode";
    public final static String BOOT = "Boot Completeed";
    public final static String SCHEDULED = "Scheduled";
    public final static String ACTIVITY = "From Activity";


    private final static long INTERVAL = 1000L * 60L * 60L;
//    private final static long INTERVAL = 1000L * 60L;
    private AlarmManager alarmMgr;

    @Override
    public void onReceive(Context context, Intent intent)
    {
        if (intent.getAction().equalsIgnoreCase(Intent.ACTION_BOOT_COMPLETED)) {
            if (context != null) {
                Intent serviceIntent = new Intent(context, JoulerEnergyManageDaemon.class);
                serviceIntent.putExtra(START_MODE, BOOT);
                context.startService(serviceIntent);
                Log.d(MainActivity.TAG, "Daemon start");
                setRepeatAlarm(context, serviceIntent);
            }
        }
    }

    private void setRepeatAlarm(Context context, Intent serviceIntent) {
        serviceIntent.putExtra(START_MODE, SCHEDULED);
        PendingIntent pendingIntent = PendingIntent.getService(context, 0, serviceIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmMgr.setInexactRepeating(alarmMgr.RTC, System.currentTimeMillis() + INTERVAL, INTERVAL, pendingIntent);
    }
}

