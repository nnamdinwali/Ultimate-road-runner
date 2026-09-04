package com.roadrunner.gamer;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

/**
 * Receives FCM pushes (same path big games use for "come back" messages).
 * Does not touch gameplay or ads.
 */
public class RoadRunnerMessagingService extends FirebaseMessagingService {

    private static final String TAG = "URR_FCM";
    private static final String CHANNEL_ID = "road_runner_push";
    private static final int NOTIFICATION_ID = 4201;

    @Override
    public void onNewToken(@NonNull String token) {
        Log.i(TAG, "FCM token refreshed");
        // Hand off to MainActivity helper when process is alive; also persist for next launch
        try {
            getSharedPreferences("rockcity_push", MODE_PRIVATE)
                    .edit()
                    .putString("provider", "fcm")
                    .putString("token", token)
                    .apply();
        } catch (Exception e) {
            Log.w(TAG, "Could not store FCM token", e);
        }
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        Log.i(TAG, "FCM message from: " + message.getFrom());

        String title = "Road Runner";
        String body = "Come back and beat your high score!";

        if (message.getNotification() != null) {
            if (message.getNotification().getTitle() != null) {
                title = message.getNotification().getTitle();
            }
            if (message.getNotification().getBody() != null) {
                body = message.getNotification().getBody();
            }
        } else if (message.getData() != null) {
            if (message.getData().containsKey("title")) title = message.getData().get("title");
            if (message.getData().containsKey("body")) body = message.getData().get("body");
        }

        showNotification(title, body);
    }

    private void showNotification(String title, String body) {
        ensureChannel();

        Intent launch = new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pi = PendingIntent.getActivity(this, 4202, launch, flags);

        int icon = R.drawable.ic_stat_reminder;
        try {
            getResources().getDrawable(icon, null);
        } catch (Exception e) {
            icon = R.mipmap.ic_launcher;
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(icon)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setContentIntent(pi)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.notify(NOTIFICATION_ID, builder.build());
        }
    }

    private void ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return;
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Game updates",
                NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription("Updates and return reminders for Road Runner");
        nm.createNotificationChannel(channel);
    }
}
