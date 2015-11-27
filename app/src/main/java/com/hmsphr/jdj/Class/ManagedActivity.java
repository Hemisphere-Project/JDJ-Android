package com.hmsphr.jdj.Class;


import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.app.ActivityManager;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

import com.hmsphr.jdj.Services.Manager;

/*
PREPARED ACTIVITY WHICH AUTO BIND/UNBIND TO THE MAIN MANAGER SERVICE
 */
public class ManagedActivity extends Activity {

    protected static Class myClass = ManagedActivity.class;
    protected int MODE = Manager.MODE_STANDBY;

    /*
    MESSAGE SENDER
     */
    protected Mailbox mail(String msg) {
        Mailbox mail = new Mailbox();
        return mail.put(msg).from(this);
    }

    /*
    EXIT BROADCAST RECEIVER
     */
    private final BroadcastReceiver exitsignal = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            finish();
        }
    };


    /*
    INTENT MESSAGE FORGE

    public static messageToActivity Message(Context ctx, String msg) {
        return new messageToActivity(ctx, msg);
    }

    public static class messageToActivity {
        private Intent intent;
        private Context context;
        public messageToActivity(Context ctx, String msg) {
            context = ctx;
            intent = new Intent(context, myClass);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra("message", msg);
        }
        public void send() {
            Log.e("CLASS", getClass().toString());
            context.startActivity(intent);
        }
        public messageToActivity put(String key, int value) {
            intent.putExtra(key, value);
            return this;
        }
        public messageToActivity put(String key, String value) {
            intent.putExtra(key, value);
            return this;
        }
    }*/


    // Auto-start Manager if not already existing
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Subscribe to exit signal
        registerReceiver(exitsignal, new IntentFilter("exit_jdj"));

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    // Connect Manager
    @Override
    protected void onStart() {
        super.onStart();

        debug("Activity started");

        // Send Mode to Manager
        mail("activity_connect").to(Manager.class).add("mode", MODE).send();

    }

    // Fullscreen
    @Override
    protected void onResume() {
        super.onResume();

        debug("Activity resumed");


        // Fullscreen
        if (Build.VERSION.SDK_INT>10) {
            // Hide both the navigation bar and the status bar.
            View decorView = getWindow().getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            decorView.setSystemUiVisibility(uiOptions);
        }
    }

    // unBind the Manager
    @Override
    protected void onStop() {
        super.onStop();

        // Send DISCONNECT to Manager
        mail("activity_disconnect").to(Manager.class).send();
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(exitsignal);
    }

    protected void error(String err) {
        Log.e("Activity", err);
    }
    protected void warning(String err) {
        Log.w("Activity", err);
    }

    protected void info(String err) {
        Log.i("Activity", err);
    }

    protected void debug(String err) {
        Log.d("Activity", err);
    }
}
