package com.mobileapplecture.ilkin.trackme;

import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Created by Ilkin on 03-Jun-17.
 */

public class FragmentSettings extends Fragment {


    // defining shared pref proporties

    //default values
    public static final boolean DEFAULT_ON_OFF = true;
    public static final int DEFAULT_GPS_FREQ = 0;
    public static final boolean DEFAULT_SPEED = true;

    // key values
    public static final String KEY_ON_OFF = "key_onOff";
    public static final String KEY_GPS_FREQ = "key_gpsFreq";
    public static final String KEY_SPEED = "key_speed";

    double longt = 0.0;
    double lat = 0.0;

    SeekBar gps_interval;
    TextView txt_gpsInterval;
    Switch switchTrackingOnOff;
    Switch switchSpeedOnOff;

    // interface which helps to send data from fragment to main activity
    Fragment2Activity fragment2Activity;
    SharedPreferences sharedPref;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_settings, container, false);

        gps_interval = (SeekBar) v.findViewById(R.id.seekbar_GPSInterval);
        txt_gpsInterval = (TextView) v.findViewById(R.id.txtGPSInterval);
        switchTrackingOnOff = (Switch) v.findViewById(R.id.switchOnOff);
        switchSpeedOnOff = (Switch) v.findViewById(R.id.switchSpeed);

        fragment2Activity = (Fragment2Activity) getActivity();

        // initializing shared pref
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this.getActivity());

        // set old settings data from SharedPref
        gps_interval.setProgress(sharedPref.getInt(KEY_GPS_FREQ, DEFAULT_GPS_FREQ) - 1);
        txt_gpsInterval.setText(String.valueOf(sharedPref.getInt(KEY_GPS_FREQ, DEFAULT_GPS_FREQ)));

        switchTrackingOnOff.setChecked(sharedPref.getBoolean(KEY_ON_OFF, DEFAULT_ON_OFF));
        switchSpeedOnOff.setChecked(sharedPref.getBoolean(KEY_SPEED, DEFAULT_SPEED));


        // sending seek bar data
        gps_interval.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                txt_gpsInterval.setText(String.valueOf(progress + 1));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

                fragment2Activity.seekBarData(seekBar.getProgress() + 1);
                // after user setted gps frequency, write it to sharedPref

                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putInt(KEY_GPS_FREQ, seekBar.getProgress() + 1);
                editor.apply();
            }
        });

        // sending switch data
        switchTrackingOnOff.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                fragment2Activity.serviceStatus(isChecked);

                // add switch Status to the sharedPref
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putBoolean(KEY_ON_OFF, isChecked);
                editor.apply();

                if (isChecked)
                    Toast.makeText(getActivity(), "Tracking is activated", Toast.LENGTH_SHORT).show();

            }
        });

        // sending and saving speed fragment status
        switchSpeedOnOff.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                fragment2Activity.speedStatus(isChecked);

                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putBoolean(KEY_SPEED, isChecked);
                editor.apply();
            }
        });

        return v;
    }


}
