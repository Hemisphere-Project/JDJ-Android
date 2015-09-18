package com.hmsphr.jdj.Class;


import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.app.ActivityManager;
import android.util.Log;

import com.hmsphr.jdj.Services.Manager;

/*
PREPARED ACTIVITY WHICH AUTO BIND/UNBIND TO THE MAIN MANAGER SERVICE
 */
public class ManagedActivity extends AppCompatActivity {

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

}
