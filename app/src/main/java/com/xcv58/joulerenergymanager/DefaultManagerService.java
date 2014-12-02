package com.xcv58.joulerenergymanager;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

/**
 * Created by xcv58 on 12/2/14.
 */
public class DefaultManagerService  extends Service {
    private static final int notificationId = 1;
    private NotificationCompat.Builder notificationBuilder;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        foreground();
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(getBaseContext(), "Default policy set successfully", Toast.LENGTH_SHORT).show();
        return START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {
        stopForeground();
        super.onDestroy();
    }

    private void foreground() {
        Intent intent = new Intent(getBaseContext(), DefaultManagerService.class);
        PendingIntent pendingIntent = PendingIntent.getService(getBaseContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        notificationBuilder = new NotificationCompat.Builder(getBaseContext())
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.notification_icon)
                .setContentTitle(getResources().getString(R.string.notification_title))
                .setContentText(MainActivity.DEFAULT + getResources().getString(R.string.notification_suffix));
        startForeground(notificationId, notificationBuilder.build());
        return;
    }

    private void stopForeground() {
        stopForeground(true);
    }
}
