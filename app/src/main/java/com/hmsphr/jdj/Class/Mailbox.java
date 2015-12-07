package com.hmsphr.jdj.Class;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by mgr on 27/11/15.
 */
public class Mailbox {
    private Intent intent;
    private Context context;
    private String message;
    private Class destination;

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
        return this;
    }
    public Mailbox add(String key, int value) {
        intent.putExtra(key, value);
        return this;
    }
    public Mailbox add(String key, String value) {
        intent.putExtra(key, value);
        return this;
    }
    public void send() {
        Log.d("Mailbox", "Message posted from: "+context.getClass().toString()+" To: " + destination.getSuperclass());
        if (isActivity()) context.startActivity(intent);
        else if (isService()) context.startService(intent);
        else Log.d("Mailbox", "Unknown destination");
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