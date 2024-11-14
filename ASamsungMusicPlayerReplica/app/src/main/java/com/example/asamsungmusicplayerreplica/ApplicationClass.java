package com.example.asamsungmusicplayerreplica;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.util.Log;

public class ApplicationClass extends Application {

    // Task:
    // Build Notification Channel
    // https://developer.android.com/reference/android/app/Application

    public static final String CHANNEL_ID_1 = "channel1";
    public static final String CHANNEL_ID_2 = "channel2";
    public static final String ACTION_PREVIOUS = "actionPrevious";
    public static final String ACTION_PLAY = "actionPlay";
    public static final String ACTION_NEXT = "actionNext";
    public static final String ACTION_PAUSE = "actionPause";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    // https://developer.android.com/develop/ui/views/notifications/channels?hl=zh-tw#java
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel channel1 = new NotificationChannel(
                    CHANNEL_ID_1, "channel(1)", NotificationManager.IMPORTANCE_HIGH);
            channel1.setDescription("Channel 1");

            NotificationChannel channel2 = new NotificationChannel(
                    CHANNEL_ID_2, "Music Player Service Channel", NotificationManager.IMPORTANCE_HIGH);
            channel2.setDescription("Music Player Service Channel");

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel1);
            Log.d("NotificationChannel", "Channel created: " + CHANNEL_ID_1);

            notificationManager.createNotificationChannel(channel2);
            Log.d("NotificationChannel", "Channel created: " + CHANNEL_ID_2);

        }
    }
}
