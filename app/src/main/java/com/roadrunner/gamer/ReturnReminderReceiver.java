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
 * Local re-engagement notification (same pattern many casual games use).
 * Not an alarm clock. Delay is set in MainActivity.
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

        NotificationChannel existing = manager.getNotificationChannel(CHANNEL_ID);
        if (existing != null) {
            // Recreate if an older low-importance channel was left behind
            if (existing.getImportance() < NotificationManager.IMPORTANCE_DEFAULT) {
                manager.deleteNotificationChannel(CHANNEL_ID);
            } else {
                return;
            }
        }

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Game reminders",
                NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription("Optional reminders to return to Road Runner");
        channel.enableVibration(true);
        channel.setShowBadge(true);
        manager.createNotificationChannel(channel);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "Return reminder received");
        if (intent == null) return;
        String action = intent.getAction();
        if (action != null && !ACTION_RETURN_REMINDER.equals(action)) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "Notification permission not granted");
                return;
            }
        }

        ensureNotificationChannel(context);

        Intent launchIntent = new Intent(context, MainActivity.class);
        launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent contentIntent = PendingIntent.getActivity(context, 4103, launchIntent, flags);

        int smallIcon = R.drawable.ic_stat_reminder;
        try {
            // Ensure resource exists; fall back to launcher if needed
            context.getResources().getDrawable(smallIcon, null);
        } catch (Exception e) {
            smallIcon = R.mipmap.ic_launcher;
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(smallIcon)
                .setContentTitle("Road Runner")
                .setContentText("Ready for another run? Beat your high score.")
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("Ready for another run? Beat your high score."))
                .setContentIntent(contentIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true);

        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, builder.build());
            Log.i(TAG, "Return reminder notification shown");
        }
    }
}
