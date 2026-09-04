package com.roadrunner.gamer;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

/**
 * Shows a local notification so the player returns after inactivity.
 * Delay is controlled by MainActivity.RETURN_REMINDER_DELAY_MS.
 */
public class ReturnReminderReceiver extends BroadcastReceiver {

    public static final String ACTION_RETURN_REMINDER =
            "com.roadrunner.gamer.action.RETURN_REMINDER";
    public static final String CHANNEL_ID = "road_runner_return_reminders";
    private static final int NOTIFICATION_ID = 4102;
    private static final String TAG = "URR_Reminder";

    public static void ensureNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Road Runner reminders",
                NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("Reminders to return to Road Runner after a break");
        channel.enableVibration(true);
        channel.setShowBadge(true);
        manager.createNotificationChannel(channel);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "Return reminder fired");
        if (intent == null) return;
        String action = intent.getAction();
        if (action != null
                && !ACTION_RETURN_REMINDER.equals(action)
                && !"android.intent.action.BOOT_COMPLETED".equals(action)) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "POST_NOTIFICATIONS not granted — cannot show reminder");
                return;
            }
        }

        ensureNotificationChannel(context);

        Intent launchIntent = new Intent(context, MainActivity.class);
        launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        int piFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            piFlags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent launchPendingIntent = PendingIntent.getActivity(
                context, 4103, launchIntent, piFlags);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_reminder)
                .setContentTitle("Road Runner")
                .setContentText("Come back and beat your high score!")
                .setContentIntent(launchPendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL);

        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, builder.build());
            Log.i(TAG, "Notification posted");
        }
    }
}
