package com.mobileapplecture.ilkin.trackme;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.location.LocationManager;
import android.os.BatteryManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.app.*;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, Fragment2Activity {

    final static String TAG = "DBread";
    private boolean isExpanded = false;

    private boolean initialDisplay = true;

    public boolean isInitialDisplay() {
        return initialDisplay;
    }

    public void setInitialDisplay(boolean initialDisplay) {
        this.initialDisplay = initialDisplay;
    }

    private GoogleMap mMap;

    // FAB buttons
    FloatingActionButton fab_expand_more;
    FloatingActionButton fab_favorites;
    FloatingActionButton fab_settings;
    FloatingActionButton fab_showMe;

    GoogleApiClient client;
    double cur_longitude = 0.0, cur_latitude = 0.0;

    LocationRequest mLocationRequest;
    PendingResult<LocationSettingsResult> result;

    FeedIssuesDBHelper mDbHelper;
    SQLiteDatabase db;

    FragmentSettings fragment_settings = new FragmentSettings();
    android.app.FragmentManager fragmentManager = getFragmentManager();
    FragmentSpeeds fragment_speeds = new FragmentSpeeds();

    Marker curLocationMarker;

    View container;
    private boolean isServiceActivated;
    private boolean isShowSpeedActivated;

    Map<String, Integer> frequentCoordinates = new HashMap();
    List<Double> velocities = new ArrayList<>();
    List<Double> longitudes = new ArrayList<>();
    List<Double> latitudes = new ArrayList<>();


    public void setServiceActivated(boolean serviceActivated) {
        isServiceActivated = serviceActivated;
    }

    public boolean isShowSpeedActivated() {
        return isShowSpeedActivated;
    }

    public void setShowSpeedActivated(boolean showSpeedActivated) {
        isShowSpeedActivated = showSpeedActivated;
    }

    Intent serviceIntent;

    //default values
    public static final boolean DEFAULT_ON_OFF = true;
    public static final boolean DEFAULT_SPEED = true;

    // key values
    public static final String KEY_ON_OFF = "key_onOff";
    public static final String KEY_SPEED = "key_speed";

    private float total_speed = 0;
    private double max_speed = 0;
    private float cur_speed = 0;
    private float avg_speed = 0;
    private int instance_count = 0;

    /************************ STARTING onCreate **************************************/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        client = new GoogleApiClient.Builder(this)
                .addApi(AppIndex.API)
                .addApi(LocationServices.API)
                .build();

        // requesting permission
        String permission = android.Manifest.permission.ACCESS_FINE_LOCATION;
        int requestCode = 1;

        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {

                //This is called if user has denied the permission before
                //In this case I am just asking the permission again
                ActivityCompat.requestPermissions(this, new String[]{permission}, requestCode);

            } else {

                ActivityCompat.requestPermissions(this, new String[]{permission}, requestCode);
            }
        } else {
            Toast.makeText(this, "" + permission + " is already granted.", Toast.LENGTH_SHORT).show();
        }

        // TODO: Should we start service here or onMapReady Method?
        // startService(new Intent(getBaseContext(), LocationListenerService.class));

        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter("SendGPSData"));


        // get old settings data from shared pref and apply them to the app
        SharedPreferences sharedPreferences = getPreferences(MODE_PRIVATE);
        setServiceActivated(sharedPreferences.getBoolean(KEY_ON_OFF, DEFAULT_ON_OFF));
        setShowSpeedActivated(sharedPreferences.getBoolean(KEY_SPEED, DEFAULT_SPEED));

        showSpeeds();

        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = this.registerReceiver(null, ifilter);
        Toast.makeText(this,"Battery status: "+ String.valueOf(batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL,-1)),Toast.LENGTH_SHORT).show();

        /*~~~~~~~~~~ Button Declerations and Clicks ~~~~~~~~`*/

        // accessing FAB buttons
        fab_expand_more = (FloatingActionButton) findViewById(R.id.fab_expand_more);
        fab_favorites = (FloatingActionButton) findViewById(R.id.fab_favoirtes);
        fab_settings = (FloatingActionButton) findViewById(R.id.fab_settings);
        fab_showMe = (FloatingActionButton) findViewById(R.id.fab_show_me);

        // if show me button clicked
        fab_showMe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMe(true);
            }
        });

        // expand menu when expand_more button clicked
        fab_expand_more.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // fab menu is not opened
                if (!isExpanded) {
                    fab_expand_more.animate().rotation(135);
                    fab_favorites.animate().translationY(-getResources().getDimension(R.dimen.standard_75));
                    fab_settings.animate().translationY(-getResources().getDimension(R.dimen.standard_150));

                    isExpanded = true;
                }
                // fab menu is expanded
                else {
                    expandLess();
                }
            }
        });

        // fab_setting on click listener
        fab_settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                container = findViewById(R.id.container_speed);

                FragmentTransaction transaction = fragmentManager.beginTransaction();
                if (fragmentManager.findFragmentByTag("FragmentSettings") == null) {

                    container.setAlpha((float) 0.3);

                    // Adding Fragment
                    transaction.setCustomAnimations(R.animator.enter, R.animator.exit);
                    transaction.add(R.id.container_settings, fragment_settings, "FragmentSettings");
                } else {
                    transaction.setCustomAnimations(R.animator.enter, R.animator.exit);
                    transaction.remove(fragment_settings);
                    container.setAlpha((float) 1.0);
                }
                transaction.commit();
                expandLess();
            }
        });


    }

    /*********************  END OF onCreate ************************/

    public void expandLess() {
        isExpanded = false;

        fab_favorites.animate().translationY(0);
        fab_settings.animate().translationY(0);
        fab_expand_more.animate().rotation(0);

    }


    /**
     * Receives Local Broadcasts
     * receives live location info from service
     */
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            // Get extra data included in the Intent
            cur_longitude = intent.getDoubleExtra("long", 0);
            cur_latitude = intent.getDoubleExtra("lat", 0);
            cur_speed = intent.getFloatExtra("speed", 0);
            Log.e("mal", cur_longitude + " , " + cur_latitude);

            if (isInitialDisplay()) {
                showMe(true);
                setInitialDisplay(false);
            }
            total_speed += cur_speed;
            Log.e(TAG, String.valueOf(cur_speed));
            ;
            instance_count++;
            avg_speed = total_speed / instance_count;

            if (max_speed < cur_speed)
                max_speed = cur_speed;

            fragment_speeds.setSpeed(avg_speed, cur_speed, max_speed);
        }
    };

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {

        askGPSPermission();
        mMap = googleMap;

        // setting some map settings
        mMap.getUiSettings().setRotateGesturesEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setZoomGesturesEnabled(true);
        mMap.getUiSettings().setScrollGesturesEnabled(true);


        // first launch of app, if user previously selected NOT to start service, don't do it
        ChangeGpsServiceStatus();

        // when clicked on the map - outside of the fab buttons or other fragments -
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                expandLess();
                FragmentTransaction transaction = fragmentManager.beginTransaction();

                if (fragmentManager.findFragmentByTag("FragmentSettings") != null) {
                    // Removing Fragment
                    transaction.setCustomAnimations(R.animator.enter, R.animator.exit);
                    transaction.remove(fragment_settings);

                    // fade background fragment
                    container.setAlpha((float) 1.0);
                }
                transaction.commit();
            }
        });

        // bring old data and draw on the map
        readFromDB(mMap);
        avg_speed = total_speed / instance_count;
        fragment_speeds.setSpeed(avg_speed, cur_speed, max_speed);

        drawOldLocations(mMap);
    }

    /**
     * when user enables/disables tracking, this method is called
     */
    private void ChangeGpsServiceStatus() {

        if (serviceIntent == null)
            serviceIntent = new Intent(getBaseContext(), LocationListenerService.class);

        if (isServiceActivated) {
            startService(serviceIntent);
            Toast.makeText(this, "Tracking is enabled", Toast.LENGTH_SHORT).show();
        } else {
            stopService(serviceIntent);
            Toast.makeText(this, "Tracking is disabled", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Ask user for permissions
     */
    private void askGPSPermission() {
        // open a dialog box to ask user to enable GPS
        final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(10000);
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(mLocationRequest);
        builder.setAlwaysShow(true);
        result = LocationServices.SettingsApi.checkLocationSettings(client, builder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult result) {
                final Status status = result.getStatus();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        try {
                            status.startResolutionForResult(MapsActivity.this, 1);
                        } catch (IntentSender.SendIntentException e) {

                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        break;
                }
            }
        });
    }

    /**
     * Show my current Location on the map
     * Continiously showing user was drammatically slowed app
     */
    public void showMe(boolean seriouslyShowMe) {
        // Add marker to show current location
        LatLng curLocation = new LatLng(cur_latitude, cur_longitude);
//        Toast.makeText(this, "Cur location: " + cur_latitude + " , " + cur_longitude, Toast.LENGTH_SHORT).show();
        try {
            curLocationMarker.remove();
        } catch (Exception e) {
            Log.e("mal", String.valueOf(e));
        }
        MarkerOptions markerOptions = new MarkerOptions().position(curLocation).title("You are here");
        curLocationMarker = mMap.addMarker(markerOptions);

        float zoomLevel = (float) 16.0;
        if (seriouslyShowMe) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(curLocation, zoomLevel));
        }
/*  // TODO: Modify this
        try {
            readFromDB();
            Log.e(TAG, "Function OK");
        } catch (Exception e) {
            Log.e(TAG, String.valueOf(e));
        }*/
    }

    /********************************* END OF showMe() ********************************************/

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (ActivityCompat.checkSelfPermission(this, permissions[0]) == PackageManager.PERMISSION_GRANTED) {

            Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
        }
    }


    // TODO: Modifiy this for global use

    /**
     * Reads old data from SQLite database and illustrates them
     * on the map depending on the speed
     *
     * @param mMap: GoogleMaps instance
     */
    public void readFromDB(GoogleMap mMap) {

        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        String[] projection = {
                FeedReaderIssues.FeedEntry._ID,
                FeedReaderIssues.FeedEntry.COLUMN_TIME,
                FeedReaderIssues.FeedEntry.COLUMN_LONG,
                FeedReaderIssues.FeedEntry.COLUMN_LAT,
                FeedReaderIssues.FeedEntry.COLUMN_ALT,
                FeedReaderIssues.FeedEntry.COLUMN_VEL,
        };

        // How you want the results sorted in the resulting Cursor
        String sortOrder = FeedReaderIssues.FeedEntry._ID + " DESC";

        Cursor cursor = db.query(
                FeedReaderIssues.FeedEntry.TABLE_NAME,    // The table to query
                projection,                               // The columns to return
                null,                                     // The columns for the WHERE clause
                null,                                     // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                sortOrder                                 // The sort order
        );
        int i = 0;


        while (cursor.moveToNext()) {

            String time = cursor.getString((cursor.getColumnIndex(FeedReaderIssues.FeedEntry.COLUMN_TIME)));
            Double longitute = Double.parseDouble(cursor.getString((cursor.getColumnIndex(FeedReaderIssues.FeedEntry.COLUMN_LONG))));
            Double latitude = Double.parseDouble(cursor.getString((cursor.getColumnIndex(FeedReaderIssues.FeedEntry.COLUMN_LAT))));
            Double velocity = Double.parseDouble(cursor.getString((cursor.getColumnIndex(FeedReaderIssues.FeedEntry.COLUMN_VEL))));

            longitudes.add(longitute);
            latitudes.add(latitude);
            velocities.add(velocity);


            // if longitute and latitute values are in the hashmap - increment frequency
            // else add new pair
            // key: "longitute,latitude", value: frequency
            String key = String.valueOf(longitute) + "," + String.valueOf(latitude);
            if (frequentCoordinates.containsKey(key))
                frequentCoordinates.put(key, frequentCoordinates.get(key) + 1);
            else
                frequentCoordinates.put(key, 1);

            if (velocity > max_speed)
                max_speed = velocity;

            total_speed += velocity;
            instance_count++;

        /* drawing on the map based on the speed
         0.0 - 0.49 m/s not moving - green
         0.5 - 1.69 m/s walking - yellow
         1.7 m/s <  fast moving - red circles
        */

            Log.e(TAG, "Values from db: " + time + "/ " + latitude + " " + longitute);
            int circleColor;

       /*     if (velocity < 0.5) {

                circleColor = Color.GREEN;

            } else if (velocity >= 0.5 && velocity < 1.7) {
                circleColor = Color.YELLOW;
            } else {
                circleColor = Color.RED;
            }

            CircleOptions circle = new CircleOptions();
            circle.center(new LatLng(latitude, longitute))
                    .radius(5)
                    .strokeColor(circleColor)
                    .fillColor(circleColor);
            try {
                mMap.addCircle(circle);
                i++;
            } catch (Exception e) {
                Log.e(TAG, String.valueOf(e));
            }*/
        }
        // drawing using hashMap

/*            Double tmp_long, tmp_lat;
            int freq;

            int circleColor = 0xFFFFFF00;
            for (Map.Entry<String, Integer> entry : frequentCoordinates.entrySet()) {


                String[] s = entry.getKey().split(",");
                tmp_long = Double.valueOf(s[0]);
                tmp_lat = Double.valueOf(s[1]);
                freq = entry.getValue();

                CircleOptions circle = new CircleOptions();
                circle.center(new LatLng(tmp_lat, tmp_long))
                        .radius(5)
                        .strokeColor(circleColor - freq)
                        .fillColor(circleColor);
                mMap.addCircle(circle);
            }*/

        cursor.close();
    }


    private void drawOldLocations(GoogleMap mMap) {

        mMap.clear();
        int circleColor = Color.YELLOW;

        int alpha = 255;
        // draw circles based on speed
        if (isShowSpeedActivated) {

            for (int i = 0; i < velocities.size(); i++) {
                if (velocities.get(i) < 0.5) {

                    circleColor = Color.GREEN;

                } else if (velocities.get(i) >= 0.5 && velocities.get(i) < 1.7) {
                    circleColor = Color.YELLOW;
                } else {
                    circleColor = Color.RED;
                }

                CircleOptions circle = new CircleOptions();
                circle.center(new LatLng(latitudes.get(i), longitudes.get(i)))
                        .radius(5)
                        .strokeColor(circleColor)
                        .fillColor(circleColor);
                try {
                    mMap.addCircle(circle);
                    i++;
                } catch (Exception e) {
                    Log.e(TAG, String.valueOf(e));
                }
            }
        }


        // if speed is not activated draw based on frequency
        else {
            Double tmp_long, tmp_lat;
            int freq;
            for (Map.Entry<String, Integer> entry : frequentCoordinates.entrySet()) {

                String[] s = entry.getKey().split(",");
                tmp_long = Double.valueOf(s[0]);
                tmp_lat = Double.valueOf(s[1]);
                freq = entry.getValue();
                freq = freq*5 > 255 ? 255 : freq*5;
                circleColor = Color.argb(freq, 255, 0, 0);

                CircleOptions circle = new CircleOptions();
                circle.center(new LatLng(tmp_lat, tmp_long))
                        .radius(5)
                        .strokeColor(circleColor)
                        .fillColor(circleColor);
                mMap.addCircle(circle);
            }
        }

        showMe(false);

    }


    /**
     * Show speeds fragment
     */
    public void showSpeeds() {

        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();


        if (isShowSpeedActivated()) {
            fragmentTransaction.add(R.id.container_speed, fragment_speeds, "Speeds");
            if (mMap != null)
                drawOldLocations(mMap);
        } else {
            fragmentTransaction.remove(fragment_speeds);
            if (mMap != null)
                drawOldLocations(mMap);
        }
        fragmentTransaction.commit();
    }


    @Override
    public void seekBarData(int val) {
        serviceIntent.putExtra("GPS_Interval", val);
        startService(serviceIntent);
    }

    /**
     * Getting data from setting Fragment
     */
    @Override
    public void serviceStatus(boolean status) {
        isServiceActivated = status;
        ChangeGpsServiceStatus();
    }


    @Override
    public void speedStatus(boolean status) {
        setShowSpeedActivated(status);
        showSpeeds();
    }


    @Override
    public void onStart() {
        super.onStart();
        client.connect();
    }

    @Override
    public void onStop() {
        Log.e("TESTGPS", "onStop at MapsActivity");
        super.onStop();
        client.disconnect();
    }

    @Override
    public void onResume() {
        Log.e("TESTGPS", "onResume at MapsActivity");


        try {
            // Accessing to the DB
            mDbHelper = FeedIssuesDBHelper.getInstance(this);
            db = mDbHelper.getReadableDatabase();
        } catch (Exception e) {
            Log.e("TESTGPS", String.valueOf(e));
        }
        super.onResume();
    }

    @Override
    public void onPause() {
        Log.e("TESTGPS", "onPause at MapsActivity");
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        super.onPause();
    }

    public void onDestroy() {
        Log.e("TESTGPS", "onDestroy at MapsActivity");
        super.onDestroy();

    }

}



