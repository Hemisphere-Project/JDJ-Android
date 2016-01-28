package com.hmsphr.jdj.Components;


import android.content.Context;
import android.os.SystemClock;
import android.util.Log;

import com.hmsphr.jdj.Class.ThreadComponent;
import com.hmsphr.jdj.Class.*;
import com.hmsphr.jdj.R;

import org.zeromq.ZMQ;

import java.net.UnknownHostException;

public class TimeSync extends ThreadComponent {

    private TimePool timePool;

    protected int POOL_SIZE = 100;          // Number of valid sample exchanged with the server
    protected int REQUEST_TIMEOUT = 500;    // Timeout (ms) for server answers
    protected int MAX_RETRY = 3;            // Max instant retry if timeout hit
    protected int TIME_PAUSE = 5;           // Time to sleep (s) if timeout MAX_RETRY times
    protected int IGNORE_LEADING_VALUES = 2; // Ignore N leading values (considering the first values ar late..)

    protected ZMQ.Context context;
    protected ZMQ.Socket socket;
    protected boolean connected = true;

    protected int retriesLeft;
    protected int validated;
    protected int leadingValuesToIgnore;

    // CONSTRUCTOR
    public TimeSync(Context ctx) {
        super(ctx);
    }

    // return waiting time before reaching server timestamp
    public long timeTo(long timestamp) {
        return (timestamp - timePool.getTimeMilliSynced());
    }

    // translate server timestamp to local timestamp
    public long translateToLocal(long ts) {
        return ts - timePool.getTimeShift();
    }

    protected void connect() {
        // Connect time server
        try {
            context = ZMQ.context(1);
            socket = context.socket(ZMQ.REQ);
            socket.connect(String.format("tcp://%s:%d",
                    appContext.getResources().getString(R.string.IP_PROXY),
                    appContext.getResources().getInteger(R.integer.PORT_TIME)));
            SystemClock.sleep(200);
            Log.v("jdj-TimeSync", "Connected to Server");
            leadingValuesToIgnore = IGNORE_LEADING_VALUES; // new connection: rearm leading value ignore
            connected = true;
        }
        catch (IllegalArgumentException e) {
            Log.v("jdj-TimeSync", "Can't connect to the server... try using IP instead of hostname ! "+e);
            connected = false;
        }

    }

    protected void disconnect() {
        // Disconnect Time server
        socket.close();
        context.term();
        connected = false;
        Log.v("jdj-TimeSync", "Disconnected from Server");
    }

    @Override
    protected void init() {
        Log.d("jdj-TimeSync", "-- TimeSync starting");

        timePool = new TimePool();
        timePool.MIN_PROCESS_SIZE = POOL_SIZE/2;

        connect();
        retriesLeft = MAX_RETRY;
        validated = 0;
    }

    @Override
    protected void loop()
    {
        if (!connected) {RUN = false; return;} //  Interrupted

        // Init Sample and send request to server
        TimePool.TimeSample sample = timePool.newSample();
        socket.send(sample.initLT1().toString().getBytes());

        //  Poll socket for a reply, with timeout
        ZMQ.PollItem items[] = {new ZMQ.PollItem(socket, ZMQ.Poller.POLLIN)};
        int rc = ZMQ.poll(items, REQUEST_TIMEOUT);
        if (rc == -1) {RUN = false; return;}       //  Interrupted

        // Something valid has been received
        if (items[0].isReadable()) {
            // Ignore the first samples of a bulk: it suffers additional delays.
            if (leadingValuesToIgnore > 0) {
                // Log.v("jdj-TimeSync", "Ignored first sample of the bulk.. (avoid first sample delay)");
                socket.recvStr();
                leadingValuesToIgnore--;
            }
            // Record sample in the Pool
            else {
                sample.setST(Long.parseLong(socket.recvStr(), 10));
                timePool.add(sample);
                // The pool is full: we can stop the thread !
                if (++validated == POOL_SIZE) RUN = false;
                // Log.v("jdj-TimeSync", "Added Sample " + validated + ": RTD= " + sample.getRTD() + " / TS= " + sample.getTS());
            }
            // This was a success, reset retries left.
            retriesLeft = MAX_RETRY;
        }
        // no answer (Timeout)
        else {
            Log.v("jdj-TimeSync", "Server timeout... ");
            if (--retriesLeft == 0) {
                // Log.d("jdj-TimeSync", "Too many timeouts.. sync paused..");
                SystemClock.sleep(TIME_PAUSE * 1000);
                retriesLeft = MAX_RETRY;
            }
            disconnect();
            connect();
        }
    }

    @Override
    protected void close() {
        disconnect();
        Log.d("jdj-TimeSync", "-- TimeSync stopped");
        Log.v("jdj-TimeSync", "TimeShift: " + timePool.getTimeShift() + "ms"
                + " Accuracy: " + timePool.getAccuracy() + "ms");
        Log.v("jdj-TimeSync", "InternalClock: " + System.currentTimeMillis() + "ms"
                + " SyncedClock: " + Long.valueOf(timePool.getTimeMilliSynced()).toString() + "ms"
                + " Delta: " + Long.valueOf(System.currentTimeMillis() - timePool.getTimeMilliSynced()).toString()+ "ms");

    }
}
