package com.hmsphr.jdj.Services;


import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;

import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.os.Binder;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.hmsphr.jdj.Activities.TextActivity;
import com.hmsphr.jdj.Activities.VideoActivity;
import com.hmsphr.jdj.Activities.WebActivity;
import com.hmsphr.jdj.Activities.WelcomeActivity;
import com.hmsphr.jdj.Class.Mailbox;
import com.hmsphr.jdj.Components.RemoteControl;
import com.hmsphr.jdj.Components.TimeSync;
import com.hmsphr.jdj.R;

import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class Manager extends Service {

    /*
    REMOTE COMMUNICATION CLIENT
     */
    private RemoteControl remoteControl = new RemoteControl(this);

    private Intent cacheIntent = null;

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
    private Timer replayTimeout;

    /*
    CAMERA
     */
    private Camera camera;
    private boolean lightIsOn = false;
    private Timer lightStrober;

    /*
    MESSAGE SENDER
     */
    private Mailbox mail(String msg) {
        Mailbox mail = new Mailbox();
        if (APP_MODE <= MODE_STANDBY) mail.lock();
        return mail.put(msg).from(this);
    }

    /*
    APP STATE & MODES
     */
    protected int ACTIVITY_LINKED = 0;
    private int APP_MODE = 0;
    private int APP_STATE = -1;

    public static final int MODE_BROKEN = -1;
    public static final int MODE_STANDBY = 0;
    public static final int MODE_LOADING = 1;
    public static final int MODE_WELCOME = 2;
    public static final int MODE_PLAY = 3;

    public static final int STATE_NONE = -1;
    public static final int STATE_INIT = 0;
    public static final int STATE_NONET = 1;
    public static final int STATE_NOSERV = 2;
    public static final int STATE_NOUSER = 3;
    public static final int STATE_READY = 10;
    public static final int STATE_SHOWPAST = 11;
    public static final int STATE_SHOWFUTURE = 12;
    public static final int STATE_SHOWTIME = 13;

    public void setMode(int mode) {

        if (APP_MODE == mode || APP_MODE == MODE_BROKEN) return;

        // back from Standby: reset connection and LVC
        boolean comeback = false;
        if (APP_MODE < MODE_WELCOME) comeback = true;

        if (APP_MODE >= MODE_STANDBY)
        {
            APP_MODE = mode;
            setState(APP_STATE);
            if (APP_MODE == MODE_WELCOME) {
                if (!remoteControl.isRunning()) remoteControl.start();
                else if (comeback) remoteControl.reset();
            }
            remoteControl.MODE = APP_MODE;
        }

        if (APP_MODE == MODE_BROKEN) mail("broken_version").to(WelcomeActivity.class).add("major", true).send();

        // LOG
        Log.d("jdj-Manager", "Manager MODE: " + APP_MODE);

        // Inform RemoteControl of player state
        remoteControl.playerReady( this.readyToPlay() );

        // Replay
        /*if (replay && cacheIntent != null) {
            this.startService(cacheIntent);
        }*/
    }

    public void setState(int state) {
        //if (APP_STATE == state) return;

        APP_STATE = state;
        Log.d("jdj-Manager", "Manager STATE: " + APP_STATE);

        // Send State to Welcome activity (if Active)
        if (APP_MODE == MODE_WELCOME)
            mail("inform_state").to(WelcomeActivity.class).add("state", APP_STATE).send();

        // Inform RemoteControl of player state
        remoteControl.playerReady(this.readyToPlay());
    }

    public boolean readyToPlay() {
        return (APP_STATE >= STATE_READY) && (APP_MODE >= MODE_WELCOME);
    }



    /*
    STOP APP
     */
    public void stopApp() {
        Mailbox.enable = false;
        this.standbyApp();
        remoteControl.stop();
        //commander.stop();
        clock.stop();
        this.stopSelf();
    }

    /*
    STOP APP
     */
    public void standbyApp() {
        setMode(MODE_STANDBY);
        sendBroadcast(new Intent("exit_jdj"));
    }

    /*
    NOTIFICATIONS
     */
    private int NOTIF_ID = 0;
    private Timer notifCancelTimer = null;
    private MediaPlayer notifSound;

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
                        .setSmallIcon(R.drawable.ic_notifsmall)
                        .setContentTitle("Il se passe quelque chose !")
                        .setContentText("Cliquez pour suivre l'aventure en direct..")
                        .setContentIntent(resultPendingIntent)
                        .setAutoCancel(true);

        Bitmap bm = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
        mBuilder.setLargeIcon(bm);

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

        // vibrate + sound
        vibrate(200);

        notifSound = MediaPlayer.create(this, R.raw.hepappok);
        notifSound.start();
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
            mail("broken_version").to(WelcomeActivity.class).add("popup", UPDATE_INFO).send();
            UPDATE_INFO = false;
        }
    }

    /*
    FLASHLIGHT
     */
    private void lightSwitchOn() {
        if (!lightIsOn) {

            // API < 23
            if (android.os.Build.VERSION.SDK_INT < 23) {
                try {
                    if (camera == null) camera = Camera.open();
                    Camera.Parameters p = camera.getParameters();
                    p.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                    camera.setParameters(p);
                    camera.startPreview();
                    lightIsOn = true;
                } catch (Exception e) {
                    Log.e("Manager", "Camera ON error: " + e);
                }
            }
            else {

                // API >= 23
                try {
                    CameraManager camManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
                    String[] cameraId = camManager.getCameraIdList();
                    for (int i = 0; i < cameraId.length; i++){
                        boolean hasFlash = camManager.getCameraCharacteristics(cameraId[i]).get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                        if (hasFlash) camManager.setTorchMode(cameraId[i], true);
                    }
                    lightIsOn = true;
                } catch (Exception e) {
                    Log.e("Manager", "Camera ON error: " + e);
                }
            }

        }
    }

    private void lightSwitchOff() {
        // API < 23
        if (android.os.Build.VERSION.SDK_INT < 23) {
            if (lightIsOn) {
                try {
                    if (camera == null) camera = Camera.open();
                    Camera.Parameters p = camera.getParameters();
                    p.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                    camera.setParameters(p);
                    camera.stopPreview();
                    camera.release();
                    camera = null;
                    lightIsOn = false;
                } catch (Exception e) {
                    Log.e("Manager", "Camera OFF error: " + e);
                }
            }
        }

        // API >= 23
        else {
            try {
                CameraManager camManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
                String[] cameraId = camManager.getCameraIdList();
                for (int i = 0; i < cameraId.length; i++){
                    boolean hasFlash = camManager.getCameraCharacteristics(cameraId[i]).get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                    if (hasFlash) camManager.setTorchMode(cameraId[i], false);
                }
                lightIsOn = false;
            } catch (Exception e) {
                Log.e("Manager", "Camera OFF error: " + e);
            }
        }
    }

    public void lightOn() {
        lightStrobeStop();
        lightSwitchOn();
    }

    public void lightOff() {
        lightStrobeStop();
        lightSwitchOff();
    }

    public void lightToggle() {
        if (!lightIsOn) lightSwitchOn();
        else lightSwitchOff();
    }

    public void lightStrobeStart() {
        lightStrober = new Timer();
        lightStrober.scheduleAtFixedRate(new TimerTask(){
            @Override
            public void run(){
                lightToggle();
            }
        },0,300);
    }

    public void lightStrobeStop() {
        if (lightStrober != null) {
            lightStrober.cancel();
            lightStrober = null;
        }
    }

    /*
    VIBRATION
     */
    public void vibrate(int ms) {
        Vibrator v = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
        // Vibrate for 500 milliseconds
        v.vibrate(ms);
    }

    /*
    SERVICE OPERATIONS
     */
    @Override
    public void onCreate() {
        super.onCreate();
        setMode(MODE_STANDBY);

        // Start sync
        clock.start();

        // Set Volume to 80%
        final AudioManager mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)*8/10, 0);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);



        if (APP_MODE == MODE_BROKEN) {
            stopApp();
            return START_NOT_STICKY;
        }

        // Process intents
        if (intent != null && intent.hasExtra("message")) {
            String msg = intent.getStringExtra("message");
            //Log.e("Manager", msg);

            // ACTIVITY RESUMED
            if (msg.equals("activity_connect")) {
                ACTIVITY_LINKED++;
                Log.d("jdj-Manager", "mode set by activity: "+intent.getIntExtra("mode", MODE_STANDBY));
                setMode(intent.getIntExtra("mode", MODE_STANDBY));
            }

            // ACTIVITY PAUSED
            else if (msg.equals("activity_disconnect")) {
                ACTIVITY_LINKED = Math.max(ACTIVITY_LINKED-1, 0);
                if (ACTIVITY_LINKED == 0) setMode(MODE_STANDBY);
            }

            // COMMAND
            else if (msg.equals("command")) {

                String action = intent.getStringExtra("action");
                String engine = intent.getStringExtra("engine");
                String payload = intent.getStringExtra("payload");
                String param1 = intent.getStringExtra("param1");
                long atTime = intent.getLongExtra("atTime", 0);
                boolean cache = intent.getBooleanExtra("cache", false);

                // CACHE CMD
                if (cache) cacheIntent = intent;

                // TRANSLATE atTime to local timeif (replayTimeout != null) replayTimeout.cancel();
                if (atTime > 0) atTime = clock.translateToLocal(atTime);

                // Cancel replay Timeout
                if (replayTimeout != null) replayTimeout.cancel();

                // PLAY
                if (action.equals("play") && readyToPlay())
                {
                    Mailbox msgPlay = mail("play");
                    boolean sendToActivity = true;

                    // SELECT ENGINE
                    if (engine.equals("web")) msgPlay.to(WebActivity.class);
                    else if (engine.equals("video")) msgPlay.to(VideoActivity.class);
                    else if (engine.equals("audio")) msgPlay.to(VideoActivity.class).add("mode", "audio");
                    else if (engine.equals("text")) msgPlay.to(TextActivity.class);
                    else if (engine.equals("phone"))
                    {
                        if (param1.equals("lightOn")) lightOn();
                        else if (param1.equals("lightOff")) lightOff();
                        else if (param1.equals("lightStrobe")) lightStrobeStart();
                        else if (param1.equals("vibre")) vibrate(300);
                        sendToActivity = false;
                    }
                    else {
                        Log.d("jdj-Manager", "No Engine found for: "+engine);
                        sendToActivity = false;
                    }

                    // ADD PAYLOAD & atTIME
                    if (sendToActivity) {
                        Log.d("jdj-Manager", "New command sent to: " + engine);
                        setMode(MODE_PLAY);
                        msgPlay.add("payload", payload).add("atTime",atTime).send();
                    }
                }
                // STOP
                else if (action.equals("stop")) {
                    mail("stop").to(WelcomeActivity.class).add("atTime",atTime).send();
                    //lightOff();
                }


            }

            else if (msg.equals("video_start") ) {
                // cancel replay timeout
                if (replayTimeout != null) replayTimeout.cancel();
            }

            else if (msg.equals("video_end") ) {
                // connection lost: go back home
                if (APP_STATE < STATE_READY)
                    mail("inform_state").to(WelcomeActivity.class).add("state", APP_STATE).send();

                // start replay timeout before going back home
                if (replayTimeout != null) replayTimeout.cancel();
                replayTimeout = new Timer();
                replayTimeout.schedule(new TimerTask(){
                    @Override
                    public void run(){
                        mail("inform_state").to(WelcomeActivity.class).add("state", APP_STATE).send();
                    }
                },30000);
            }


            else if (msg.equals("video_freeze") ) {
                mail("inform_state").to(WelcomeActivity.class).add("state", APP_STATE).send();
            }

                // CLOSE APP & SERVICE
            else if (msg.equals("application_timeout") || msg.equals("application_stop")) {
                stopApp();
                return START_NOT_STICKY;
            }

                // STANDBY APP
            else if (msg.equals("application_standby")) standbyApp();

                // DISPLAY NOTIFICATION
            else if (msg.equals("application_need_attention")) {
                clearNotification();
                if (intent.getStringExtra("action").equals("play")) notifyEvent();
            }

            // MAJOR VERSION OUTDATED
            else if (msg.equals("version_major_outdated")) setMode(MODE_BROKEN);

                // MINOR VERSION OUTDATED
            else if (msg.equals("version_minor_outdated")) advertiseUpdate();

                // UPDATE STATE
            else if (msg.equals("change_state")) setState(intent.getIntExtra("state", STATE_INIT));

                // REGISTRATION
            else if (msg.equals("do_register")) remoteControl.registrationReady(true);

        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.v("jdj-Manager", "Manager is closing.. Goodbye ! ");
        android.os.Process.killProcess(android.os.Process.myPid());
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


