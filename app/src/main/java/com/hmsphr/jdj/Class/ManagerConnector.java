package com.hmsphr.jdj.Class;


import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import com.hmsphr.jdj.Services.Manager;

public class ManagerConnector {

    public Manager service;

    private boolean mIsBound = false;

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder remote_service) {
            service = ((Manager.LocalBinder)remote_service).getService();
        }

        public void onServiceDisconnected(ComponentName className) {
            service = null;
        }
    };

    public void connect(Context ctx, int mode) {
        Intent intent = new Intent(ctx, Manager.class);
        intent.putExtra("MODE", mode);
        ctx.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }

    public void disconnect(Context ctx) {
        if (mIsBound) {
            // Detach our existing connection.
            ctx.unbindService(mConnection);
            mIsBound = false;
        }
    }
}
