package com.hmsphr.jdj.Class;


import android.content.Context;
import android.os.SystemClock;
import android.util.Log;

/*
PREPARED COMPONENT WITH THREADED ACTION
 */
abstract public class ThreadComponent {

    // APP CONTEXT
    protected Context appContext;

    // CONSTRUCTOR
    public ThreadComponent(Context ctx) {
        appContext = ctx;
    }

    // MESSAGE SENDER
    protected Mailbox mail(String msg) {
        Mailbox mail = new Mailbox();
        return mail.put(msg).from(appContext);
    }

    // THREAD TOOLS
    protected Boolean RUN = false;
    private Thread mThread = new Thread() {
        @Override
        public void run() {
            action();
        }
    };

    // START Thread action
    public void start() {
        if (!isRunning()) this.mThread.start();
    }

    // STOP Thread action
    public void stop() {
        if (isRunning()) {
            RUN = false;
            try {
                this.mThread.join(300); // wait for subscriber to finish
            } catch (InterruptedException e) {
                //throw new RuntimeException(e);
            }
            if (this.mThread.isAlive()) this.mThread.interrupt(); // force subscriber to stop
        }
    }

    // is RUNning ?
    public boolean isRunning() {
        return RUN;
    }

    // THREAD sequence
    private void action() {
        RUN = true;
        init();
        while(!Thread.currentThread().isInterrupted() && RUN) {
            loop();
        }
        close();
        RUN = false;
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
        Log.v("jdj-ThreadComponent", "a stupid thread is running...");
    }

    // THREAD init -> Override !
    protected void close() {
        // Implement me
    }
}
