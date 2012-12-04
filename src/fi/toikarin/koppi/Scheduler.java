package fi.toikarin.koppi;

import java.util.Date;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Log;

public class Scheduler {
    private static final String TAG = "KoppiScheduler";

    public static void start(Context context) {
        Log.i(TAG, "Starting scheduler.");

        Main main = (Main) context.getApplicationContext();
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, SchedulerBroadcastReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);

        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(),
                main.getUpdateInterval(), pendingIntent);
    }

    public static void stop(Context context) {
        Log.i(TAG, "Stopping scheduler.");

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(context, SchedulerBroadcastReceiver.class);
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);

        alarmManager.cancel(sender);
    }

    public static class SchedulerBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(final Context context, Intent intent) {
            Log.i(TAG, "Alarm went off: " + new Date());

            Intent serviceIntent = new Intent(context, UpdateService.class);
            serviceIntent.putExtra(UpdateService.RESULT_RECEIVER_ID, new ResultReceiver(null) {
                @Override
                protected void onReceiveResult(int resultCode, Bundle resultData) {
                    Log.d(TAG, "Received results.");

                    Response response = resultData.getParcelable(UpdateService.RESPONSE_ID);

                    Intent intent = new Intent(MainActivity.UPDATE_EVENT_ID);
                    intent.putExtra(UpdateService.RESPONSE_ID, response);

                    context.sendBroadcast(intent);
                }
            });
            context.startService(serviceIntent);
        }
    }

    private Scheduler() {}
}
