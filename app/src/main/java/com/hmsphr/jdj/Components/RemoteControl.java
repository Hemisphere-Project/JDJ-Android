package com.hmsphr.jdj.Components;

import org.json.JSONException;
import org.json.JSONObject;
import org.zeromq.ZMQ;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;

import com.github.nkzawa.emitter.Emitter;
import com.hmsphr.jdj.Activities.AudioActivity;
import com.hmsphr.jdj.Activities.VideoActivity;
import com.hmsphr.jdj.Activities.WebActivity;
import com.hmsphr.jdj.Activities.WelcomeActivity;
import com.hmsphr.jdj.Class.Mailbox;
import com.hmsphr.jdj.Class.ThreadComponent;
import com.hmsphr.jdj.R;
import com.hmsphr.jdj.Services.Manager;

import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import java.io.File;
import java.net.URISyntaxException;
import java.util.concurrent.locks.ReentrantLock;


public class RemoteControl extends ThreadComponent {

    private long APP_TIMEOUT = 24*60*60*1000;
    private long lastactivity = 0;

    private int MODE = 0; // dispatch flag between player and notifier
    private Context appContext;

    private ZMQ.Context context = ZMQ.context(1);
    private ZMQ.Socket remotePublisher = context.socket(ZMQ.SUB);
    private ZMQ.Poller poller = new ZMQ.Poller(100);

    private Socket mSocket;
    private ReentrantLock LOCK = new ReentrantLock();

    private String data;
    private int laststamp = 0;
    private JSONObject storedcommand = null;

    private boolean CONNECTED = false;


    public RemoteControl(Context ctx) {
        appContext = ctx;
    }

    /*
    MESSAGE SENDER
     */
    private Mailbox mail(String msg) {
        Mailbox mail = new Mailbox();
        return mail.put(msg).from(appContext);
    }

    public void setMode(int mode) {
        MODE = mode;
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void connect() {

        // No Network available
        if (!isNetworkAvailable()) return;

        // Already connected
        if (CONNECTED) return;

        // connect to proxy websocket
        Log.d("RC-client", "Connecting info WebSocket");
        try {
            mSocket = IO.socket(String.format("http://%s:%d",
                    appContext.getResources().getString(R.string.IP_PROXY),
                    appContext.getResources().getInteger(R.integer.PORT_INFO)));
        } catch (URISyntaxException e) {}

        mSocket.on("hello", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                JSONObject obj = (JSONObject) args[0];
                onHello(obj);
            }
        });
        mSocket.connect();

        // connect to proxy publisher
        remotePublisher.connect(String.format("tcp://%s:%d",
                appContext.getResources().getString(R.string.IP_PROXY),
                appContext.getResources().getInteger(R.integer.PORT_PUB)));
        remotePublisher.subscribe("".getBytes());
        poller.register(remotePublisher, ZMQ.Poller.POLLIN);
        CONNECTED = true;
        Log.d("RC-client", "Connected !");

    }

    // CLIENT LOOP: LISTEN ZMQ MESSAGES
    @Override
    protected void init() {
        Log.d("RC-client", "-- RemoteControl starting");

        SystemClock.sleep(1000);

        // init lastactivity timestamp
        lastactivity = System.currentTimeMillis();

        APP_TIMEOUT = appContext.getResources().getInteger(R.integer.APP_TIMEOUT_NOACTIVITY)*60*1000;
    }

    @Override
    protected void loop()
    {
        // EXIT IF NOTHING HAPPENS..
        // TODO
        if (System.currentTimeMillis()-lastactivity > APP_TIMEOUT)
            mail("application_timeout").to(Manager.class).send();

        // check if network is available
        if (!isNetworkAvailable()) {
            mail("update_state").to(Manager.class).add("state", Manager.STATE_NONET).send();
            SystemClock.sleep(1000);
            return;
        }

        // re-connect if necessary
        connect();

        // Poll the subscriber stack
        if(poller.poll(100) > 0) {
            //if (poller.pollin(0)) { // check on first Poll register

            data = remotePublisher.recvStr();

            // Parse and execute order
            try
            {
                JSONObject command = new JSONObject(data);

                // Log received instructions
                Log.d("RC-client", "received JSON: " + data);

                LOCK.lock();
                // App is available to execute now
                if (MODE >= Manager.MODE_WELCOME) {
                    processCommand(command);
                    storedcommand = null;
                }

                // App is not available: storing command
                else {
                    if (command.has("cache") && command.getBoolean("cache")) storedcommand = command;
                    mail("application_need_attention").to(Manager.class).send();
                }
                LOCK.unlock();
            }
            catch (JSONException e) { Log.d("RC-client", "invalid JSON: "+data); }

        }

        // APP now available: execute stored commands
        LOCK.lock();
        if (storedcommand != null && MODE >= Manager.MODE_WELCOME) {
            processCommand(storedcommand);
            storedcommand = null;
            Log.d("RC-client", "Stored command processed ");
        }
        LOCK.unlock();
    }

    @Override
    protected void close() {
        // exit sockets
        remotePublisher.close();
        context.term();
        Log.d("jdj-RemoteControl", "-- RemoteControl stopped");
    }


    public void onHello(JSONObject obj) {
        Log.d("RC-client", "WS hello: " + obj.toString());
        try {
            // CHECK Version
            if (obj.getJSONObject("version").getInt("major") > appContext.getResources().getInteger(R.integer.VERSION_MAJOR))
                mail("version_major_outdated").to(Manager.class).send();

            if (obj.getJSONObject("version").getInt("minor") > appContext.getResources().getInteger(R.integer.VERSION_MINOR))
                mail("version_minor_outdated").to(Manager.class).send();

            // Check SHOW STATE
            int showState = 0;
            Long deltaTime = obj.getLong("nextshow") - System.currentTimeMillis();
            Log.d("RC-client", "Delta Next show: NOW: " + System.currentTimeMillis() + " SHOW: " + obj.getLong("nextshow") + " DELTA: " + deltaTime);
            if (deltaTime < 0) showState = Manager.STATE_SHOWPAST;
            else if (deltaTime < 24*60*60*1000) showState = Manager.STATE_SHOWTIME;
            else showState = Manager.STATE_SHOWFUTURE;
            mail("update_state").to(Manager.class).add("state", showState).send();

            // PARSE LVC
            if (obj.has("lvc")) {
                LOCK.lock();
                storedcommand = new JSONObject(obj.getString("lvc"));
                LOCK.unlock();
            }

            // DOWNLOAD AVAILABLE MEDIA
            if (obj.has("medialist") && showState == Manager.STATE_SHOWFUTURE) {
                // TODO: get media list and enqueue download tasks
            }

        }
        catch (JSONException e) { Log.d("RC-client", "invalid JSON: "+e.toString()); }
    }

    protected void processCommand(JSONObject command) {
        //TODO
        // - check if file is available in local
        // - use atTime for synced play
        // - use GROUP_PUB

        //Something is happening
        lastactivity = System.currentTimeMillis();

        try
        {
            // Check if command is a new one
            if (command.has("timestamp")) {
                int ts = command.getInt("timestamp");
                if (ts <= laststamp) {
                    Log.d("RC-client", "command already executed");
                    return;
                }
                else laststamp = ts;
            }

            if (command.has("action")) {

                Mailbox msg = mail("command").to(Manager.class).add("controller", "remote");

                // ACTION TO PERFORM
                msg.add("action", command.getString("action"));

                // PLAYER ENGINE SELECTOR: web | audio | video | phone
                if (command.has("category")) msg.add("engine", command.getString("category"));
                else msg.add("param1", "none");

                // EXTRA PARAMETER
                if (command.has("param1")) msg.add("param1", command.getString("param1"));
                else msg.add("param1", "");

                // FILE PATH SELECTOR (WORST TO BEST)
                String filepath = null;
                int medialevel = 0;

                // PROGRESSIVE STREAMING
                if (command.has("url")) {
                    filepath = command.getString("url");
                    medialevel = 1;
                }

                // ADAPTATIVE STREAMING
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
                    if (command.has("hls")) {
                        filepath = command.getString("hls");
                        medialevel = 2;
                    }
                }

                // LOCAL FILE
                if (command.has("filename")) {
                    // TODO CHECK IF FILE AVAILABLE LOCALLY
                    /*File media = getMediaFile(appContext, command.getString("filename"));
                    if (media.exists()) {
                        filepath = media.getPath();
                        medialevel = 3;
                    }*/
                }

                // ADD FILE
                Log.d("RC-client", "media path: "+filepath+" level: "+medialevel);
                msg.add("medialevel", medialevel);
                msg.add("filepath", filepath);


                msg.send();
            }
            else Log.d("RC-client", "action missing ");
        }
        catch (JSONException e) { Log.d("RC-client", "invalid JSON "); }
    }


}
