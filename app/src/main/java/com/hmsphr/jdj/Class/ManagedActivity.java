package com.hmsphr.jdj.Class;


import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.app.ActivityManager;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

import com.hmsphr.jdj.R;
import com.hmsphr.jdj.Services.Manager;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/*
PREPARED ACTIVITY WHICH AUTO BIND/UNBIND TO THE MAIN MANAGER SERVICE
 */
public class ManagedActivity extends Activity {

    protected static Class myClass = ManagedActivity.class;
    protected int MODE = Manager.MODE_STANDBY;
    protected Typeface defaultFont;
    protected boolean CONNECTED = true;

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
            Log.d("jdj-ManagedActivity", "exit signal received");
            finish();
        }
    };

    // SETTINGS
    protected SharedPreferences settings() {
        return this.getSharedPreferences(
                this.getString(R.string.settings_file), this.MODE_PRIVATE);
    }

    // SERVICE CHECK
    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }


    // Auto-start Manager if not already existing
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Subscribe to exit signal
        registerReceiver(exitsignal, new IntentFilter("exit_jdj"));

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        defaultFont = this.getFont(R.raw.opensans_regular);
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
        if (isMyServiceRunning(Manager.class))
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

    protected Typeface getFont(int resource)
    {
        Typeface tf = null;
        InputStream is = getResources().openRawResource(resource);
        String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/gmg_underground_tmp";
        File f = new File(path);
        if (!f.exists() && !f.mkdirs()) return null;
        String outPath = path + "/tmp.raw";
        try
        {
            byte[] buffer = new byte[is.available()];
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(outPath));
            int l = 0;
            while((l = is.read(buffer)) > 0) bos.write(buffer, 0, l);
            bos.close();
            tf = Typeface.createFromFile(outPath);
            File f2 = new File(outPath);
            f2.delete();
        }
        catch (IOException e) { return null; }
        return tf;
    }
}
