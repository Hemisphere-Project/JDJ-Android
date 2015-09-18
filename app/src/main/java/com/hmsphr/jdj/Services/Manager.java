package com.hmsphr.jdj.Services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.Binder;
import android.util.Log;

import com.hmsphr.jdj.Class.RemoteClient;

public class Manager extends Service {
    public Manager() {
    }

    public static final int MODE_OFF = 0;
    public static final int MODE_LOAD = 1;
    public static final int MODE_WELCOME = 2;
    public static final int MODE_PLAY = 3;

    /*
    APP STATE
     */
    private boolean SERVICE_BOUND = false;
    private int APP_MODE = MODE_OFF;

    private void setMode(int mode) {
        APP_MODE = mode;
    }

    /*
    REMOTE COMMUNICATION CLIENT
     */
    private RemoteClient remoteClient = new RemoteClient();

    /*
    SERVICE START HELPER
     */
    public static void start(Context context) {
        Intent intent = new Intent(context, Manager.class);
        context.startService(intent);
    }

    /*
    SERVICE BINDINGS
     */
    public class LocalBinder extends Binder {
        public Manager getService() {
            return Manager.this;
        }
    }
    private final IBinder mBinder = new LocalBinder();

    /*
    SERVICE OPERATIONS
     */
    @Override
    public void onCreate() {
        setMode(MODE_OFF);
        remoteClient.start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("LocalService", "Received start id " + startId + ": " + intent);
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {

        SERVICE_BOUND = true;
        if (intent.getExtras() != null) {
            // answerChannel = (Messenger) intent.getExtras().get("MESSENGER");
            setMode( intent.getIntExtra("MODE", MODE_OFF) );
        }

        Log.v("mgrlog", "Service bound with mode: " + APP_MODE);
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        SERVICE_BOUND = false;
        setMode(MODE_OFF);
        Log.v("mgrlog", "Service unbound ");
        return true;
    }

    @Override
    public void onRebind(Intent intent) {
        onBind(intent);
    }

    @Override
    public void onDestroy() {
        remoteClient.stop();
    }

}
