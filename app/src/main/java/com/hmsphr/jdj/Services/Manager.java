package com.hmsphr.jdj.Services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.Binder;
import android.util.Log;

import com.hmsphr.jdj.Components.Commander;
import com.hmsphr.jdj.Components.RemoteClient;
import com.hmsphr.jdj.Components.TimeSync;

public class Manager extends Service {

    /*
    REMOTE COMMUNICATION CLIENT
     */
    private RemoteClient remoteClient = new RemoteClient("10.0.2.2", 8081, "zenner");

    /*
    PARSER / CHECKER / DISPATCHER
     */
    private Commander commander = new Commander();

    /*
    TIME SYNCHRONISATION OVER NETWORK
     */
    private TimeSync clock = new TimeSync("10.0.2.2", 8082);

    /*
    CONSTRUCTOR
     */
    public Manager() {
        setMode(MODE_OFF);
    }

    public static final int MODE_OFF = 0;
    public static final int MODE_LOAD = 1;
    public static final int MODE_WELCOME = 2;
    public static final int MODE_PLAY = 3;

    /*
    APP STATE
     */
    protected boolean SERVICE_BOUND = false;
    private int APP_MODE;

    public void setMode(int mode) {
        APP_MODE = mode;
        Log.d("jdj-Manager", "App state: " + APP_MODE);
        commander.setMode(APP_MODE);
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
    SERVICE START HELPER
     */
    public static void start(Context context) {
        Intent intent = new Intent(context, Manager.class);
        context.startService(intent);
    }

    /*
    SERVICE OPERATIONS
     */
    @Override
    public void onCreate() {
        super.onCreate();
        setMode(MODE_OFF);
        remoteClient.start();
        commander.start();
        clock.start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.v("jdj-Manager", "Received start id " + startId + ": " + intent);
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {

        SERVICE_BOUND = true;
        Log.v("jdj-Manager", "Manager connected");
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        SERVICE_BOUND = false;
        setMode(MODE_OFF);
        Log.v("jdj-Manager", "Manager disconnected: standby ");
        return true;
    }

    @Override
    public void onRebind(Intent intent) {
        onBind(intent);
    }

    @Override
    public void onDestroy() {
        remoteClient.stop();
        commander.stop();
        clock.stop();
    }

}
