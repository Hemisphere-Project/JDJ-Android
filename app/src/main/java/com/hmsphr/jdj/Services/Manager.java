package com.hmsphr.jdj.Services;

import android.app.ActivityManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.Binder;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.hmsphr.jdj.Activities.WelcomeActivity;
import com.hmsphr.jdj.Components.Commander;
import com.hmsphr.jdj.Components.RemoteControl;
import com.hmsphr.jdj.Components.TimeSync;
import com.hmsphr.jdj.R;

import java.util.List;

public class Manager extends Service {


    /*
    REMOTE COMMUNICATION CLIENT
     */
    private RemoteControl remoteControl = new RemoteControl(this);

    /*
    PARSER / CHECKER / DISPATCHER
     */
    //private Commander commander = new Commander();

    /*
    TIME SYNCHRONISATION OVER NETWORK
     */
    private TimeSync clock = new TimeSync(this);

    /*
    CONSTRUCTOR
     */
    public Manager() {
        setMode(MODE_STANDBY);
    }

    public static final int MODE_STOP = -2;
    public static final int MODE_BROKEN = -1;
    public static final int MODE_STANDBY = 0;
    public static final int MODE_LOADING = 1;
    public static final int MODE_WELCOME = 2;
    public static final int MODE_PLAY = 3;

    /*
    APP STATE
     */
    protected boolean SERVICE_BOUND = false;
    private int APP_MODE;

    public void setMode(int mode) {
        if (APP_MODE != MODE_BROKEN)
        {
            APP_MODE = mode;
            Log.d("jdj-Manager", "App state: " + APP_MODE);

            remoteControl.setMode(APP_MODE);
            if (APP_MODE == MODE_WELCOME) remoteControl.start();
        }
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

    public void stopApp() {
        sendBroadcast(new Intent("exit_jdj"));
        this.stopSelf();
    }

    public void brokenVersion() {
        setMode(MODE_BROKEN);
        Intent intent = new Intent(this, WelcomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("broken_version", 1);
        this.startActivity(intent);
    }

    public void updateAvailable() {
        Intent intent = new Intent(this, WelcomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("update_available", 1);
        this.startActivity(intent);
    }

    public void needAttention() {
        int notifyID = 1;
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.notif_icon)
                        .setContentTitle("Il se passe quelque chose !")
                        .setContentText("Cliquez pour suivre l'aventure en direct..");
        // Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(this, WelcomeActivity.class);

        /*
        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(WelcomeActivity.class);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder.setContentIntent(resultPendingIntent);*/
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        // mId allows you to update the notification later on.
        mNotificationManager.notify(notifyID, mBuilder.build());
    }

    /*
    SERVICE OPERATIONS
     */
    @Override
    public void onCreate() {
        super.onCreate();
        setMode(MODE_STANDBY);

        //commander.start();
        clock.start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        //Log.v("jdj-Manager", "Received start id " + startId + ": " + intent);

        // Process intents from RemoteControl
        if (intent.hasExtra("RemoteControl")) {
            String info = intent.getStringExtra("RemoteControl");

            // CLOSE APP & SERVICE
            if (info.equals("application_timeout")) this.stopApp();

            // DISPLAY NOTIFICATION
            else if (info.equals("application_need_attention") && APP_MODE == MODE_STANDBY) this.needAttention();

            // MAJOR VERSION OUTDATED
            else if (info.equals("version_major_outdated")) this.brokenVersion();

            // MINOR VERSION OUTDATED
            else if (info.equals("version_minor_outdated") && APP_MODE == MODE_WELCOME) this.updateAvailable();
        }

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
        setMode(MODE_STANDBY);
        Log.v("jdj-Manager", "Manager disconnected: standby ");
        return true;
    }

    @Override
    public void onRebind(Intent intent) {
        onBind(intent);
    }

    @Override
    public void onDestroy() {
        remoteControl.stop();
        //commander.stop();
        clock.stop();
        Log.v("jdj-Manager", "Manager is closing.. Goodbye ! ");
    }

}
