package com.mobileapplecture.ilkin.trackme;


import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


/**
 * Created by Ilkin on 03-Jun-17.
 */

public class FragmentSpeeds extends Fragment {

    TextView txt_avg;
    TextView txt_cur;
    TextView txt_max;



    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_speeds, container, false);
        txt_avg = (TextView) v.findViewById(R.id.txt_avgSpeed);
        txt_cur = (TextView) v.findViewById(R.id.txt_curSpeed);
        txt_max = (TextView) v.findViewById(R.id.txt_maxSpeed);


        return v;
    }

    public void setSpeed(float avg_speed, float cur_speed, double max_speed)
    {
        if (txt_cur != null && txt_max != null && txt_avg !=null)
        {
        txt_max.setText(String.valueOf(max_speed) + " m/s");
        txt_avg.setText(String.valueOf(avg_speed) + " m/s");
        txt_cur.setText(String .valueOf(cur_speed)+ " m/s");}
    }
}
