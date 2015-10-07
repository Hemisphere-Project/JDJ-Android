package com.hmsphr.jdj.Components;

import android.os.SystemClock;
import android.util.Log;

import com.hmsphr.jdj.Class.ThreadComponent;

import org.zeromq.ZMQ;

public class Commander extends ThreadComponent {

    private int MODE = 0; // dispatch flag between player and notifier

    private ZMQ.Context context = ZMQ.context(1);
    private ZMQ.Socket socket = context.socket(ZMQ.ROUTER);
    private ZMQ.Poller poller = new ZMQ.Poller(100);

    public void setMode(int mode) {
        MODE = mode;
    }

    @Override
    protected void init() {
        Log.d("jdj-Commander", "-- Commander starting");

        // bind to inproc addr
        socket.bind("inproc://commander");

        // register socket in poll
        poller.register(socket, ZMQ.Poller.POLLIN);
    }

    @Override
    protected void loop()
    {
        SystemClock.sleep(2000);
        //Log.v("jdj-Commander", "Commander waiting in mode: "+MODE);
    }

    @Override
    protected void close() {
        socket.close();
        context.term();
        Log.d("jdj-Commander", "-- Controller stopped");
    }

}
