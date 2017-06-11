package com.mobileapplecture.ilkin.trackme;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

public class LocationListenerService extends Service {
    private static final String TAG = "TESTGPS";
    private LocationManager mLocationManager = null;
    private int LOCATION_INTERVAL;
    private static final float LOCATION_DISTANCE = 1f;
    public static final int DEFAULT_GPS_FREQ = 1;
    public static final String KEY_GPS_FREQ = "key_gpsFreq";

    public void setLOCATION_INTERVAL(int LOCATION_INTERVAL) {
        this.LOCATION_INTERVAL = LOCATION_INTERVAL;
    }


    FeedIssuesDBHelper mDbHelper;
    SQLiteDatabase db;


    private class LocationListener implements android.location.LocationListener {

        Location mLastLocation;

        LocationListener(String provider) {
            Log.e(TAG, "LocationListener " + provider);
            mLastLocation = new Location(provider);


            try {
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
                setLOCATION_INTERVAL(sharedPreferences.getInt(KEY_GPS_FREQ, DEFAULT_GPS_FREQ) * 1000);
                Toast.makeText(getBaseContext(), "New Interval is " + LOCATION_INTERVAL, Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Log.e(TAG, String.valueOf(e));
            }
        }

        @Override
        public void onLocationChanged(Location location) {
            Log.e(TAG, "onLocationChanged: " + location);
            mLastLocation.set(location);

            // locally broadcasting data - sending data to the main activity
            Intent intent = new Intent("SendGPSData");
            intent.putExtra("lat", location.getLatitude());
            intent.putExtra("long", location.getLongitude());
            intent.putExtra("alt", location.getAltitude());
            intent.putExtra("speed", location.getSpeed());
//            Toast.makeText(getBaseContext(), "I'm sending "+location.getLatitude(),Toast.LENGTH_SHORT).show();

            // writing values to the db
            Log.e("DB", "Creating values...");
            final String SQL_ENTER_VALUES = "Insert into " + FeedReaderIssues.FeedEntry.TABLE_NAME +
                    " (time, longitude, latitude, altitude, velocity) " + "Values (datetime(), "
                    + location.getLongitude() + "," + location.getLatitude() + "," + location.getAltitude() + "," + location.getSpeed() + ")";

            try {
                db.execSQL(SQL_ENTER_VALUES);
                Log.e(TAG, SQL_ENTER_VALUES);

            } catch (SQLException e) {
                Log.e(TAG, e.toString());
            }

            // start broadcasting
            LocalBroadcastManager.getInstance(getBaseContext()).sendBroadcast(intent);
        }


        @Override
        public void onProviderDisabled(String provider) {
            Log.e(TAG, "onProviderDisabled: " + provider);
        }

        @Override
        public void onProviderEnabled(String provider) {
            Log.e(TAG, "onProviderEnabled: " + provider);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            Log.e(TAG, "onStatusChanged: " + provider);
        }


    }

    LocationListener[] mLocationListeners = new LocationListener[]{
            new LocationListener(LocationManager.GPS_PROVIDER),
//            new LocationListener(LocationManager.NETWORK_PROVIDER)
    };

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(TAG, "onStartCommand");
        super.onStartCommand(intent, flags, startId);

        // makes a problem if main activty killed
        if (intent != null) {
            setLOCATION_INTERVAL(intent.getIntExtra("GPS_Interval", 1) * 1000);
//            Toast.makeText(getBaseContext(), "Setting interval to " + LOCATION_INTERVAL, Toast.LENGTH_SHORT).show();
        }
        // Creating DB
        FeedIssuesDBHelper mDbHelper = FeedIssuesDBHelper.getInstance(this);
        db = mDbHelper.getWritableDatabase();
        db.execSQL(FeedReaderIssues.FeedEntry.SQL_CREATE_ENTRIES);
        initializeLocationManager();

        // finding location using network
      /*  try {
            mLocationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE,
                    mLocationListeners[1]);
        } catch (java.lang.SecurityException ex) {
            Log.i(TAG, "fail to request location update, ignore", ex);
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, "network provider does not exist, " + ex.getMessage());
        }*/
        try {
            mLocationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE,
                    mLocationListeners[0]);
        } catch (java.lang.SecurityException ex) {
            Log.i(TAG, "fail to request location update, ignore", ex);
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, "gps provider does not exist " + ex.getMessage());
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.e(TAG, "onDestroy at Service");
        super.onDestroy();

//        //TODO: DB - 3
//        db.close();
//
//        //TODO: Should we have this?
        if (mLocationManager != null) {
            for (LocationListener mLocationListener : mLocationListeners) {
                try {
                    mLocationManager.removeUpdates(mLocationListener);
                } catch (Exception ex) {
                    Log.i(TAG, "fail to remove location listners, ignore", ex);
                }
            }
        }
    }

    private void initializeLocationManager() {
        Log.e(TAG, "initializeLocationManager");
        if (mLocationManager == null) {
            mLocationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);

        }
    }


}