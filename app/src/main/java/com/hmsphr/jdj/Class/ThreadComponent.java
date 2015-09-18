package com.hmsphr.jdj.Class;


import android.os.SystemClock;
import android.util.Log;

/*
PREPARED COMPONENT WITH THREADED ACTION
 */
abstract public class ThreadComponent {
    public ThreadComponent() {
    }

    private Boolean RUN = true;
    private Thread mThread = new Thread() {
        @Override
        public void run() {
            action();
        }
    };


    // START Thread action
    public void start() {
        stop();
        RUN = true;
        this.mThread.start();
    }

    // STOP Thread action
    public void stop() {
        RUN = false;
        try {
            this.mThread.join(300); // wait for subscriber to finish
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        if (this.mThread.isAlive()) this.mThread.interrupt(); // force subscriber to stop
    }

    // THREAD sequence
    private void action() {
        init();
        while(!Thread.currentThread().isInterrupted() && RUN) {
            loop();
        }
        close();
    }

    // THREAD init -> Override !
    protected void init() {
        // Implement me
    }

    // THREAD loop -> Override !
    protected void loop() {
        // Implement me
        // Dummy Loop:
        SystemClock.sleep(1000);
        Log.v("mgrlog", "a stupid thread is running...");
    }

    // THREAD init -> Override !
    protected void close() {
        // Implement me
    }
}
