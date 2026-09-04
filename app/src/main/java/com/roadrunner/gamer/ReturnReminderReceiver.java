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

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

/** Posts a local reminder when the player has been away from the game (delay set in MainActivity). */
public class ReturnReminderReceiver extends BroadcastReceiver {

    public static final String ACTION_RETURN_REMINDER =
            "com.roadrunner.gamer.action.RETURN_REMINDER";
    public static final String CHANNEL_ID = "road_runner_return_reminders";
    private static final int NOTIFICATION_ID = 4102;

    public static void ensureNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) return;
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Road Runner reminders",
                NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription("Reminders to return to Ultimate Road Runner after a break");
        manager.createNotificationChannel(channel);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !ACTION_RETURN_REMINDER.equals(intent.getAction())) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        ensureNotificationChannel(context);

        Intent launchIntent = new Intent(context, MainActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingIntentFlags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent launchPendingIntent = PendingIntent.getActivity(
                context,
                4103,
                launchIntent,
                pendingIntentFlags);

        NotificationCompat.Builder notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Road Runner is ready")
                .setContentText("Come back and beat your best run.")
                .setContentIntent(launchPendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true);

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification.build());
    }
}
