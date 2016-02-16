package com.hmsphr.jdj.Class;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;

/**
 * Created by mgr on 27/11/15.
 */
public class Mailbox {

    public static boolean enable = true;

    private Intent intent;
    private Context context;
    private String message;
    private Class destination;
    private long timestamp = 0;

    public Mailbox() { }
    public Mailbox put(String msg)  {
        message = msg;
        return this;
    }
    public Mailbox from(Context ctx) {
        context = ctx;
        return this;
    }
    public Mailbox to(Class cls) {
        destination = cls;
        intent = new Intent(context, cls);
        if (isActivity()) intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("message", message);
        intent.putExtra("from", context.getClass());
        return this;
    }
    public Mailbox at(long ts) {
        if (ts > 0) timestamp = ts;
        else timestamp = 0;
        return this;
    }
    public Mailbox add(String key, int value) {
        intent.putExtra(key, value);
        return this;
    }
    public Mailbox add(String key, long value) {
        intent.putExtra(key, value);
        return this;
    }
    public Mailbox add(String key, String value) {
        intent.putExtra(key, value);
        return this;
    }
    public Mailbox add(String key, boolean value) {
        intent.putExtra(key, value);
        return this;
    }
    public void send() {

        // Timestamp
        this.add("_timestamp", timestamp);

        if (timestamp > SystemClock.elapsedRealtime()) {
            Log.d("jdj-Mailbox", "delayed message: " + (timestamp - SystemClock.elapsedRealtime()));
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    doSend();
                }
            }, Math.max(0, timestamp - SystemClock.elapsedRealtime()));
        }
        else doSend();

    }

    private void doSend() {
        //if (!Mailbox.enable && isService()) return;
        Log.d("jdj-Mailbox", "Message posted from: " + context.getClass() + " To: " + destination.getSuperclass() + " :: " + message);
        if (isActivity()) context.startActivity(intent);
        else if (isService()) context.startService(intent);
        else Log.e("jdj-Mailbox", "Unknown destination");
    }

    private boolean isActivity() {
        return (destination.getSuperclass().equals(ManagedActivity.class)
                || destination.getSuperclass().equals(Activity.class)
                || destination.getSuperclass().equals(MediaActivity.class));
    }

    private boolean isService() {
        return (destination.getSuperclass().equals(Service.class));
    }
}
