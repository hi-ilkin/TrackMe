package com.mobileapplecture.ilkin.trackme;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.location.Geocoder;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
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
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, Fragment2Activity {

    final static String TAG = "MapsActvity";

    private boolean isExpanded = false;
    private boolean initialDisplay = true;
    private boolean isServiceActivated;
    private boolean isShowSpeedActivated;
    private boolean isFavLocationsActive = false;

    // FAB buttons
    FloatingActionButton fab_expand_more;
    FloatingActionButton fab_favorites;
    FloatingActionButton fab_settings;
    FloatingActionButton fab_showMe;

    GoogleApiClient client;
    LocationRequest mLocationRequest;
    PendingResult<LocationSettingsResult> result;

    FeedIssuesDBHelper mDbHelper;
    SQLiteDatabase db;

    View container;
    Intent serviceIntent;


    /*~~~~~~~~~~~~~~~~~~~~~~ Fragments ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
    android.app.FragmentManager fragmentManager = getFragmentManager();
    FragmentSettings fragment_settings = new FragmentSettings();
    FragmentSpeeds fragment_speeds = new FragmentSpeeds();


    /*~~~~~~~~~~~~~~~~~~~~~~ Map Instances ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
    private GoogleMap mMap;
    Marker[] fav_markers = new Marker[5];
    Geocoder geocoder;
    List<android.location.Address> addresses;
    Map<String, Integer> frequentCoordinates = new HashMap<>();
    List<Double> velocities = new ArrayList<>();
    List<Double> longitudes = new ArrayList<>();
    List<Double> latitudes = new ArrayList<>();
    Circle curLocationCircle;
    double cur_longitude = 0.0, cur_latitude = 0.0;


    /*~~~~~~~~~~~~~~~~~~~~~~ Shared Preferences ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
    //default values
    public static final boolean DEFAULT_ON_OFF = true;
    public static final boolean DEFAULT_SPEED = true;
    public static final int DEFAULT_GPS_FREQ = 1;

    // key values
    public static final String KEY_ON_OFF = "key_onOff";
    public static final String KEY_SPEED = "key_speed";
    public static final String KEY_GPS_FREQ = "key_gpsFreq";

    private float total_speed = 0;
    private double max_speed = 0;
    private float cur_speed = 0;
    private float avg_speed = 0;
    private int instance_count = 0;


    /*~~~~~~~~~~~~~~~~~~~~~~ Getter and Setters ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
    public void setServiceActivated(boolean serviceActivated) {
        isServiceActivated = serviceActivated;
    }

    public boolean isShowSpeedActivated() {
        return isShowSpeedActivated;
    }

    public void setShowSpeedActivated(boolean showSpeedActivated) {
        isShowSpeedActivated = showSpeedActivated;
    }

    public boolean isInitialDisplay() {
        return initialDisplay;
    }

    public void setInitialDisplay(boolean initialDisplay) {
        this.initialDisplay = initialDisplay;
    }


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

        if (ContextCompat.checkSelfPermission(this, permission)
                != PackageManager.PERMISSION_GRANTED) {
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

        // starting services
        try {
            // first launch of app, if user previously selected NOT to start service, don't do it
            ChangeGpsServiceStatus();
            startService(new Intent(getBaseContext(), BatteryBrodcastReceiver.class));
        } catch (Exception e) {
            Log.e(TAG, String.valueOf(e));
        }

        /*~~~~~~~~~~~~~~ First load all data from Shared Preferences ~~~~~~~~~~~~~~~~~~~~~~~~ */

        // get old settings data from shared pref and apply them to the app
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        setServiceActivated(sharedPreferences.getBoolean(KEY_ON_OFF, DEFAULT_ON_OFF));
        setShowSpeedActivated(sharedPreferences.getBoolean(KEY_SPEED, DEFAULT_SPEED));
        int val = sharedPreferences.getInt(KEY_GPS_FREQ, DEFAULT_GPS_FREQ);


        Log.e(TAG, "I got: " + isServiceActivated + ", " + isShowSpeedActivated() + ", " + val);
        try {
            showSpeeds();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Registering Local Broadcasts
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter("SendGPSData"));
        LocalBroadcastManager.getInstance(this).registerReceiver(mBatteryReceiver, new IntentFilter("SendBatteryStatus"));


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
                    fab_settings.animate().translationY(-getResources().getDimension(R.dimen.standard_75));
                    fab_settings.animate().translationX(-getResources().getDimension(R.dimen.standard_75));
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

        fab_favorites.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mMap != null) {
                    createFavPlaces();
                } else {
                    AlertDialog.Builder alert = new AlertDialog.Builder(MapsActivity.this);
                    alert.setTitle("Map is not Ready for drawing");
                    alert.setMessage("Please wait for map to prepare itself and try again");
                    alert.setCancelable(false);
                    alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    alert.create().show();
                }
                expandLess();
            }
        });
    }


    /*********************  END OF onCreate ************************/

    public void expandLess() {
        isExpanded = false;

        fab_favorites.animate().translationY(0);
        fab_settings.animate().translationY(0);
        fab_settings.animate().translationX(0);
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

            if (isInitialDisplay()) {
                showMe(true);
                setInitialDisplay(false);
            }
            total_speed += cur_speed;
            Log.e(TAG, String.valueOf(cur_speed));

            instance_count++;
            avg_speed = total_speed / instance_count;

            if (max_speed < cur_speed)
                max_speed = cur_speed;

            fragment_speeds.setSpeed(avg_speed, cur_speed, max_speed);
        }
    };


    private BroadcastReceiver mBatteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean status = intent.getBooleanExtra("b_status", true);
            Log.e(TAG, "Data from batterBroadcast " + status);
            setServiceActivated(status);
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
        readFromDB();
        avg_speed = total_speed / instance_count;
        fragment_speeds.setSpeed(avg_speed, cur_speed, max_speed);

        try {
            drawOldLocations(mMap);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * when user enables/disables tracking, this method is called
     * status obtained using interface
     */
    private void ChangeGpsServiceStatus() {

        if (serviceIntent == null)
            serviceIntent = new Intent(getBaseContext(), LocationListenerService.class);

        if (isServiceActivated) {
            startService(serviceIntent);
        } else {
            stopService(serviceIntent);
        }
    }

    /**
     * Ask user for permissions
     */
    private void askGPSPermission() {
        // open a dialog box to ask user to enable GPS
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(10000);
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(mLocationRequest);
        builder.setAlwaysShow(true);
        result = LocationServices.SettingsApi.checkLocationSettings(client, builder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(@NonNull LocationSettingsResult result) {
                final Status status = result.getStatus();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        try {
                            status.startResolutionForResult(MapsActivity.this, 1);
                        } catch (IntentSender.SendIntentException e) {
                            Log.e(TAG,String.valueOf(e));
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
     * Continiously showing user was drammatically slowed app that's why disabled
     * blue circle is you
     */
    public void showMe(boolean focusOnMe) {

        LatLng curLocation = new LatLng(cur_latitude, cur_longitude);

        if (curLocationCircle != null) {
            curLocationCircle.setCenter(curLocation);
        } else {
            CircleOptions curCircleOptions = new CircleOptions();
            curCircleOptions.fillColor(Color.argb(220, 0, 200, 255));
            curCircleOptions.strokeColor(Color.rgb(0, 150, 255));
            curCircleOptions.strokeWidth(2);
            curCircleOptions.radius(10);

            curCircleOptions.center(curLocation);
            curLocationCircle = mMap.addCircle(curCircleOptions);
        }
        float zoomLevel = (float) 16.0;
        if (focusOnMe) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(curLocation, zoomLevel));
        }

    }

    /********************************* END OF showMe() ********************************************/

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (ActivityCompat.checkSelfPermission(this, permissions[0]) == PackageManager.PERMISSION_GRANTED) {

            Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
        }
    }


    /**
     * Reads old data from SQLite database and illustrates them
     * on the map depending on the speed
     *
     */
    public void readFromDB() {

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

        while (cursor.moveToNext()) {

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

        }
        cursor.close();
    }


    private void drawOldLocations(GoogleMap mMap) throws IOException {

        // clear map
        mMap.clear();

        // as map cleared, favorite locations also gone
        isFavLocationsActive = false;
        int circleColor;

        // draw circles based on speed
        if (isShowSpeedActivated) {

        /* drawing on the map based on the speed
         0.0 - 0.49 m/s not moving - green
         0.5 - 1.99 m/s walking - yellow
         2.0 m/s <  fast moving - red circles
        */

            for (int i = 0; i < velocities.size(); i++) {
                if (velocities.get(i) < 0.5) {

                    circleColor = Color.GREEN;

                } else if (velocities.get(i) >= 0.5 && velocities.get(i) < 2.0) {
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
            frequentCoordinates = sortFrequentPlaces(frequentCoordinates);
            for (Map.Entry<String, Integer> entry : frequentCoordinates.entrySet()) {

                String[] s = entry.getKey().split(",");
                tmp_long = Double.valueOf(s[0]);
                tmp_lat = Double.valueOf(s[1]);
                freq = entry.getValue();

                freq = freq + 10 > 255 ? 255 : freq + 10;
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

    private void createFavPlaces() {



        if (isFavLocationsActive) {
            int i = 0;
            try {
                while ((i < 5) && (fav_markers[i] != null)) {
                    fav_markers[i].remove();
                    i++;
                }
            } catch (Exception e) {
                Log.e(TAG, String.valueOf(e));
            }

            isFavLocationsActive = false;
        } else {
            int fav_locations_count = 0;
            @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
            ArrayList fav_locations = new ArrayList();

            for (Map.Entry<String, Integer> entry : frequentCoordinates.entrySet()) {

                if (fav_locations_count == 5)
                    break;
                else {
                    try {
                        String[] s = entry.getKey().split(",");
                        Double tmp_long = Double.valueOf(s[0]);
                        Double tmp_lat = Double.valueOf(s[1]);

                        // converting coordinates to address
                        geocoder = new Geocoder(this, Locale.getDefault());
                        addresses = geocoder.getFromLocation(tmp_lat, tmp_long, 1);
                        String temp_adr = addresses.get(0).getAddressLine(0) + " " + addresses.get(0).getAddressLine(1);
                        if (!fav_locations.contains(temp_adr)) {

                            MarkerOptions markerOptions = new MarkerOptions();
                            markerOptions.position(new LatLng(tmp_lat, tmp_long))
                                    .title(temp_adr);
                            fav_markers[fav_locations_count] = mMap.addMarker(markerOptions);

                            fav_locations_count++;
                        }

                    } catch (Exception e) {
                        Log.e(TAG, String.valueOf(e));
                    }
                    isFavLocationsActive = true;
                }
            }
        }
    }


    /**
     * Sort HashMap based on values
     */
    public Map<String, Integer> sortFrequentPlaces(Map<String, Integer> aMap) {
        Set<Map.Entry<String, Integer>> mapEntries = aMap.entrySet();

        // used linked list to sort, because insertion of elements in linked list is faster than an array list.
        List<Map.Entry<String, Integer>> aList = new LinkedList<>(mapEntries);

        // sorting the List
        Collections.sort(aList, new Comparator<Map.Entry<String, Integer>>() {

            @Override
            public int compare(Map.Entry<String, Integer> ele1,
                               Map.Entry<String, Integer> ele2) {

                return ele2.getValue().compareTo(ele1.getValue());
            }
        });

        // Storing the list into Linked HashMap to preserve the order of insertion.
        Map<String, Integer> aMap2 = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : aList) {
            aMap2.put(entry.getKey(), entry.getValue());
        }

        return aMap2;
    }


    /**
     * Show speeds fragment
     */
    public void showSpeeds() throws IOException {

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

    /**
     * Getting data from setting Fragment
     */
    @Override
    public void seekBarData(int val) {
        if (isServiceActivated) {
            startService(serviceIntent);
        }
    }

    @Override
    public void serviceStatus(boolean status) {
        isServiceActivated = status;
        ChangeGpsServiceStatus();
    }


    @Override
    public void speedStatus(boolean status) {
        setShowSpeedActivated(status);
        try {
            showSpeeds();
        } catch (IOException e) {
            e.printStackTrace();
        }
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

        super.onPause();
    }


    public void onDestroy() {
        Log.e("TESTGPS", "onDestroy at MapsActivity");
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        super.onDestroy();

    }
}


// TODO: 2 - isFavLocationsActive true olduktan sonra false olmuyor


