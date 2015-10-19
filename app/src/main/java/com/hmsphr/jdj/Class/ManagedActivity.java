package com.hmsphr.jdj.Class;


import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
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

    // internal Player
    protected PlayerCompat player = null;

    // Connector
    public class ManagerConnector {

        public Manager service;

        protected int MODE = Manager.MODE_OFF;

        private boolean mIsBound = false;

        private ServiceConnection mConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName className, IBinder remote_service) {
                service = ((Manager.LocalBinder)remote_service).getService();
                service.setMode(MODE);
            }

            public void onServiceDisconnected(ComponentName className) {
                service = null;
            }
        };

        public void connect(Context ctx) {
            Log.v("jdj-ManagedActivity", "Activity connecting to manager with mode: " + MODE);
            Intent intent = new Intent(ctx, Manager.class);
            //intent.putExtra("MODE", MODE);
            ctx.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
            mIsBound = true;
        }

        public void disconnect(Context ctx) {
            if (mIsBound) {
                // Detach our existing connection.
                Log.v("jdj-ManagedActivity", "Activity disconnecting from manager");
                ctx.unbindService(mConnection);
                mIsBound = false;
            }
        }

        public void setMode(int mode) {
            MODE = mode;
        }
    }

    // Manager connector link
    protected ManagerConnector manager = new ManagerConnector();

    // Auto-start Manager if not already existing
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Start Manager if it does not exist already
        ActivityManager aManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        boolean exist = false;
        for (ActivityManager.RunningServiceInfo service : aManager.getRunningServices(Integer.MAX_VALUE))
            if (service.service.getClassName().equals(Manager.class.getName())) exist = true;
        if (!exist)  {
            Log.v("jdj-ManagedActivity", "Starting Manager");
            Manager.start(this);
        }
        //else Log.v("mgrlog", "Manager already started");

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    // Bind to Manager
    @Override
    protected void onStart() {
        super.onStart();

        // Bind to Manager
        manager.connect(this);
    }

    // unBind to Manager
    @Override
    protected void onStop() {
        super.onStop();

        // unBind to Manager
        manager.disconnect(this);

    }

    @Override
    protected void onNewIntent (Intent intent) {
        super.onNewIntent(intent);

        // Check if player is available
        if (player != null) {
            // Parse Intent
            Bundle extras = intent.getExtras();
            if (extras == null) {
                error("Intent must provide extras");
                return;
            }
            String action = extras.getString("action");
            if (action == null) {
                error("Intent must provide an action");
                return;
            }

            // Execute command
            // STOP
            if (action.equals("stop")) {
                player.stop();
                finish();
            }

            // PLAY
            else if (action.equals("play")) {
                String url = extras.getString("url");
                if (url == null) {error("Play action must provide an url");  return;}
                player.play( url );
            }
        }
    }

    // Fullscreen
    @Override
    protected void onResume() {
        super.onResume();
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
        // Player resume
        if (player != null) player.resume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (player != null) player.pause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (player != null) player.stop();
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
