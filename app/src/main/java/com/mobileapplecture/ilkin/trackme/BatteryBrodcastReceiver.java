package com.mobileapplecture.ilkin.trackme;

import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

/**
 * Created by Ilkin on 11-Jun-17.
 */

public class BatteryBrodcastReceiver extends Service {


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    boolean isServiceStarted = true;

    @Override
    public int onStartCommand(Intent intent, int flags, final int startId) {
        super.onStartCommand(intent, flags, startId);

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());


        final SharedPreferences.Editor editor = sharedPref.edit();


        BroadcastReceiver mBatteryReceiver = new BroadcastReceiver() {

            Intent battery_intent = new Intent("SendBatteryStatus");
            @Override
            public void onReceive(Context context, Intent intent) {
                int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
                int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);

                // locally broadcasting data - sending data to the main activity

                NotificationManager mNotificationManager;
                if (level < 15) {
                    try {
                        // stopping service
                        stopService(new Intent(getBaseContext(), LocationListenerService.class));

                        // changing settings - just update sharedPref
                        editor.putBoolean("key_onOff", false);
                        editor.apply();
                        battery_intent.putExtra("b_status",false);
                        // show notificaion to user
                        NotificationCompat.Builder mLowBatteryBuilder = new NotificationCompat.Builder(getBaseContext());
                        mLowBatteryBuilder.setSmallIcon(R.drawable.ic_disabling_tracking);
                        mLowBatteryBuilder.setContentTitle("Low battery level!");
                        mLowBatteryBuilder.setContentText("Your battery level is under 15%, tracking is disabled");
                        mLowBatteryBuilder.setTicker("Tracking is Disabled");
                        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

                        // notificationID allows you to update the notification later on.
                        mNotificationManager.notify(1, mLowBatteryBuilder.build());
                        isServiceStarted = false;

                    } catch (Exception e) {
                        Log.e("TESTGPS", String.valueOf(e));
                    }
                }

                // if phone is plugged in or battery level is over 15 continue charing
                else if (!isServiceStarted && ((level > 15) || (status == BatteryManager.BATTERY_STATUS_CHARGING))) {
                    try {
                        // starting service
                        startService(new Intent(getBaseContext(), LocationListenerService.class));

                        // changing settings - just update sharedPref
                        editor.putBoolean("key_onOff", true);
                        editor.apply();
                        battery_intent.putExtra("b_status",true);

                        // show notificaion to user
                        NotificationCompat.Builder mBatteryOkBuilder = new NotificationCompat.Builder(getBaseContext());
                        mBatteryOkBuilder.setSmallIcon(R.drawable.ic_disabling_tracking);
                        mBatteryOkBuilder.setContentTitle("Good job :)");
                        mBatteryOkBuilder.setContentText("Now I can continue to track you...");
                        mBatteryOkBuilder.setTicker("Tracking is Enabled");

                        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

                        // notificationID allows you to update the notification later on.
                        mNotificationManager.notify(1, mBatteryOkBuilder.build());
                        isServiceStarted = true;
                    } catch (Exception e) {
                        Log.e("TESTGPS", String.valueOf(e));
                    }
                }

                LocalBroadcastManager.getInstance(getBaseContext()).sendBroadcast(battery_intent);
            }

        };

        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(mBatteryReceiver, ifilter);

        return START_STICKY;
    }
}