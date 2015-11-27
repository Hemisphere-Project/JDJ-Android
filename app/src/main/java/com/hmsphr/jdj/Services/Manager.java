package com.hmsphr.jdj.Services;


import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.Binder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.hmsphr.jdj.Activities.AudioActivity;
import com.hmsphr.jdj.Activities.VideoActivity;
import com.hmsphr.jdj.Activities.WebActivity;
import com.hmsphr.jdj.Activities.WelcomeActivity;
import com.hmsphr.jdj.Class.Mailbox;
import com.hmsphr.jdj.Components.RemoteControl;
import com.hmsphr.jdj.Components.TimeSync;
import com.hmsphr.jdj.R;

import java.util.Timer;
import java.util.TimerTask;

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
        Log.v("jdj-Manager", "Manager started.. ");
    }

    /*
    MESSAGE SENDER
     */
    private Mailbox mail(String msg) {
        Mailbox mail = new Mailbox();
        return mail.put(msg).from(this);
    }

    /*
    APP STATE & MODES
     */
    protected int ACTIVITY_LINKED = 0;
    private int APP_MODE = 0;
    private int APP_STATE = 0;

    public static final int MODE_STOP = -2;
    public static final int MODE_BROKEN = -1;
    public static final int MODE_STANDBY = 0;
    public static final int MODE_LOADING = 1;
    public static final int MODE_WELCOME = 2;
    public static final int MODE_PLAY = 3;

    public static final int STATE_INIT = 0;
    public static final int STATE_NONET = 1;
    public static final int STATE_NOSERV = 2;
    public static final int STATE_SHOWPAST = 10;
    public static final int STATE_SHOWFUTURE = 11;
    public static final int STATE_SHOWTIME = 12;

    public void setMode(int mode) {

        if (APP_MODE == mode) return;

        if (APP_MODE != MODE_BROKEN)
        {
            APP_MODE = mode;
            remoteControl.setMode(APP_MODE);
            setState(APP_STATE);
            if (APP_MODE == MODE_WELCOME) remoteControl.start();
        }

        if (APP_MODE == MODE_BROKEN) mail("broken_version").to(WelcomeActivity.class).send();

        // LOG
        if (APP_MODE == MODE_STANDBY) Log.v("jdj-Manager", "Manager disconnected: STANDBY ");
        else Log.d("jdj-Manager", "Manager MODE: " + APP_MODE);
    }

    public void setState(int state) {
        APP_STATE = state;
        Log.d("jdj-Manager", "Manager STATE: " + APP_STATE);

        if (APP_MODE == MODE_WELCOME)
            mail("update_state").to(WelcomeActivity.class).add("state", APP_STATE).send();
    }



    /*
    STOP APP
     */
    public void stopApp() {
        sendBroadcast(new Intent("exit_jdj"));
        this.stopSelf();
    }

    /*
    NOTIFICATIONS
     */
    private int NOTIF_ID = 0;
    private Timer notifCancelTimer = null;

    public void notifyEvent() {

        if (APP_MODE != MODE_STANDBY) return;

        // Remove previous notification
        clearNotification();

        // Intent to Activity
        Intent resultIntent = new Intent(this, WelcomeActivity.class);
        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(this, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        // Notif Builder
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle("Il se passe quelque chose !")
                        .setContentText("Cliquez pour suivre l'aventure en direct..")
                        .setContentIntent(resultPendingIntent)
                        .setAutoCancel(true);

        // Issue Notification
        NOTIF_ID=1;
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(NOTIF_ID, mBuilder.build());

        // Set Notification auto destroy
        notifCancelTimer = new Timer();
        notifCancelTimer.schedule(new TimerTask() {
            @Override
            public void run() { clearNotification(); }
        }, this.getResources().getInteger(R.integer.NOTIFICATION_TIMEOUT) * 1000);
    }

    public void clearNotification() {
        if (notifCancelTimer != null) {
            notifCancelTimer.cancel();
            notifCancelTimer = null;
        }
        if (NOTIF_ID > 0) {
            NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.cancel(NOTIF_ID);
            NOTIF_ID = 0;
        }

    }

    /*
    VERSION UPDATE
     */
    private boolean UPDATE_INFO = true;

    public void advertiseUpdate() {
        if (APP_MODE == MODE_WELCOME) {
            if (UPDATE_INFO) mail("update_available").to(WelcomeActivity.class).send();
            UPDATE_INFO = false;
        }
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

        // Process intents
        if (intent.hasExtra("message")) {
            String info = intent.getStringExtra("message");

                // ACTIVITY RESUMED
            if (info.equals("activity_connect")) {
                ACTIVITY_LINKED++;
                Log.d("jdj-Manager", "mode set by activity: "+intent.getIntExtra("mode", MODE_STANDBY));
                setMode(intent.getIntExtra("mode", MODE_STANDBY));
            }

                // ACTIVITY PAUSED
            else if (info.equals("activity_disconnect")) {
                ACTIVITY_LINKED = Math.max(ACTIVITY_LINKED-1, 0);
                if (ACTIVITY_LINKED == 0) setMode(MODE_STANDBY);
            }

                // COMMAND
            else if (info.equals("command")) {

                String action = intent.getStringExtra("action");
                String type = intent.getStringExtra("type");

                // PLAY
                if (action.equals("play"))
                {
                    Mailbox msgPlay = mail("play");
                    boolean valid = true;

                    if (type.equals("web")) msgPlay.to(WebActivity.class);
                    else if (type.equals("video")) msgPlay.to(VideoActivity.class);
                    else if (type.equals("audio")) msgPlay.to(AudioActivity.class);
                    else { Log.d("jdj-Manager", "No Player found for type: "+type); valid = false; }

                    if (valid) {
                        Log.d("jdj-Manager", "New command sent to: " + type);
                        setMode(MODE_PLAY);
                        msgPlay.add("url", intent.getStringExtra("url")).send();
                    }
                }
                // STOP
                else if (action.equals("stop"))
                    mail("stop").to(WelcomeActivity.class).send();

            }

                // CLOSE APP & SERVICE
            else if (info.equals("application_timeout") || info.equals("application_stop")) stopApp();

                // DISPLAY NOTIFICATION
            else if (info.equals("application_need_attention")) notifyEvent();

                // DISPLAY NOTIFICATION
            else if (info.equals("application_standby")) clearNotification();

                // MAJOR VERSION OUTDATED
            else if (info.equals("version_major_outdated")) setMode(MODE_BROKEN);

                // MINOR VERSION OUTDATED
            else if (info.equals("version_minor_outdated")) advertiseUpdate();

            // UPDATE STATE
            else if (info.equals("update_state"))  setState(intent.getIntExtra("state", STATE_INIT));

        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        remoteControl.stop();
        //commander.stop();
        clock.stop();
        Log.v("jdj-Manager", "Manager is closing.. Goodbye ! ");
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

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return true;
    }

    @Override
    public void onRebind(Intent intent) {
        onBind(intent);
    }

}


