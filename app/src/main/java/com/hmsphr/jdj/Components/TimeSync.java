package com.hmsphr.jdj.Components;


import android.os.SystemClock;
import android.util.Log;

import com.hmsphr.jdj.Class.ThreadComponent;

public class TimeSync extends ThreadComponent {

    @Override
    protected void init() {
        Log.d("jdj-TimeSync", "-- TimeSync starting");

    }

    @Override
    protected void loop()
    {
        SystemClock.sleep(3000);
        Log.v("jdj-TimeSync", "TimeSync waiting");
    }

    @Override
    protected void close() {

        Log.d("jdj-TimeSync", "-- TimeSync stopped");
    }
}
