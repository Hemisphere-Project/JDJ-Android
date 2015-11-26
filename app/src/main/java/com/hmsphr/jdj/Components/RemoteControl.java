package com.hmsphr.jdj.Components;

import org.json.JSONException;
import org.json.JSONObject;
import org.zeromq.ZMQ;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.SystemClock;
import android.util.Log;

import com.github.nkzawa.emitter.Emitter;
import com.hmsphr.jdj.Activities.AudioActivity;
import com.hmsphr.jdj.Activities.VideoActivity;
import com.hmsphr.jdj.Activities.WebActivity;
import com.hmsphr.jdj.Activities.WelcomeActivity;
import com.hmsphr.jdj.Class.ThreadComponent;
import com.hmsphr.jdj.R;
import com.hmsphr.jdj.Services.Manager;

import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import java.net.URISyntaxException;
import java.util.concurrent.locks.ReentrantLock;


public class RemoteControl extends ThreadComponent {

    private long APP_TIMEOUT = 24*60*60*1000;
    private long lastactivity = 0;

    private int MODE = 0; // dispatch flag between player and notifier
    private Context appContext;

    private ZMQ.Context context = ZMQ.context(1);
    private ZMQ.Socket remotePublisher = context.socket(ZMQ.SUB);
    private ZMQ.Socket manager = context.socket(ZMQ.DEALER);
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

        //connect to Manager
        manager.connect("inproc://manager");

        // init lastactivity timestamp
        lastactivity = System.currentTimeMillis();

        APP_TIMEOUT = appContext.getResources().getInteger(R.integer.APP_TIMEOUT_NOACTIVITY)*60*1000;
    }

    @Override
    protected void loop()
    {
        // EXIT IF NOTHING HAPPENS..
        // TODO
        if (System.currentTimeMillis()-lastactivity > APP_TIMEOUT) informManager("application_timeout");

        // check if network is available
        if (!isNetworkAvailable()) {
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
                Log.v("RC-client", "received JSON: " + data);

                LOCK.lock();
                // App is available to execute now
                if (MODE >= Manager.MODE_WELCOME) {
                    processCommand(command);
                    storedcommand = null;
                }

                // App is not available: storing command
                else {
                    storedcommand = command;
                    informManager("application_need_attention");
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
        manager.close();
        remotePublisher.close();
        context.term();
        Log.d("jdj-RemoteControl", "-- RemoteControl stopped");
    }

    public void informManager(String msg) {
        Intent mIntent = new Intent(appContext, Manager.class);
        mIntent.putExtra("RemoteControl", msg);
        appContext.startService(mIntent);
    }

    public void onHello(JSONObject obj) {
        Log.d("RC-client", "WS hello: " + obj.toString());
        try {
            // CHECK Version
            if (obj.getJSONObject("version").getInt("major") > appContext.getResources().getInteger(R.integer.VERSION_MAJOR))
                informManager("version_major_outdated");

            if (obj.getJSONObject("version").getInt("minor") > appContext.getResources().getInteger(R.integer.VERSION_MINOR))
                informManager("version_minor_outdated");

            // PARSE LVC
            if (obj.has("lvc")) {
                LOCK.lock();
                storedcommand = new JSONObject(obj.getString("lvc"));
                LOCK.unlock();
            }

        }
        catch (JSONException e) { Log.d("RC-client", "invalid JSON: "+e.toString()); }
    }

    protected void processCommand(JSONObject command) {
        //TODO
        // - check if file is available in local
        // - use atTime for synced play
        // - dispatch with MODE
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

                // STOP ANYWAY: GO BACK TO WELCOME PAGE
                Intent cmdIntent = new Intent(appContext, WelcomeActivity.class);
                cmdIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                cmdIntent.putExtra("action", command.getString("action"));
                appContext.startActivity(cmdIntent);

                // ACTION: LAUNCH NEW PLAYER
                if (command.has("category"))
                {
                    String cat = command.getString("category");
                    if (cat.equals("url"))
                        cmdIntent = new Intent(appContext, WebActivity.class);
                    else if (cat.equals("video"))
                        cmdIntent = new Intent(appContext, VideoActivity.class);
                    else if (cat.equals("audio"))
                        cmdIntent = new Intent(appContext, AudioActivity.class);

                    cmdIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    cmdIntent.putExtra("action", command.getString("action"));

                    // TODO CHECK IF LOCAL FILE EXIST !
                    if (command.has("url")) {
                        cmdIntent.putExtra("mode", "remote");
                        cmdIntent.putExtra("url", command.getString("url"));
                    }

                    appContext.startActivity(cmdIntent);
                }

            }
            else Log.d("RC-client", "action missing ");
        }
        catch (JSONException e) { Log.d("RC-client", "invalid JSON "); }
    }



}
