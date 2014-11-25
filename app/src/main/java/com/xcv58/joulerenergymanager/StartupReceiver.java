package com.xcv58.joulerenergymanager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by xcv58 on 11/25/14.
 */
public class StartupReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent)
    {
        if (intent.getAction().equalsIgnoreCase(Intent.ACTION_BOOT_COMPLETED)) {
            if (context != null) {
                Intent serviceIntent = new Intent(context, JoulerEnergyManageDeamon.class);
                context.startService(serviceIntent);
                Log.d(MainActivity.TAG, "Daemon start");
            }
        }
    }
}

