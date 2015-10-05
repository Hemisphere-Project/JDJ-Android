package com.hmsphr.jdj.Components;

import android.os.SystemClock;
import android.util.Log;

import com.hmsphr.jdj.Class.ThreadComponent;

import org.zeromq.ZMQ;

public class Commander extends ThreadComponent {

    private int MODE = 0; // dispatch flag between player and notifier

    public void setMode(int mode) {
        MODE = mode;
    }

    @Override
    protected void init() {
        Log.d("jdj-Commander", "-- Commander starting");

    }

    @Override
    protected void loop()
    {
        SystemClock.sleep(2000);
        //Log.v("jdj-Commander", "Commander waiting in mode: "+MODE);
    }

    @Override
    protected void close() {

        Log.d("jdj-Commander", "-- Controller stopped");
    }

}
