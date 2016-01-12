package com.hmsphr.jdj.Components;

import org.json.JSONException;
import org.json.JSONObject;
import org.zeromq.ZMQ;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.SystemClock;
import android.util.Log;

import com.github.nkzawa.emitter.Emitter;
import com.hmsphr.jdj.Activities.WelcomeActivity;
import com.hmsphr.jdj.BuildConfig;
import com.hmsphr.jdj.Class.Mailbox;
import com.hmsphr.jdj.Class.ThreadComponent;
import com.hmsphr.jdj.R;
import com.hmsphr.jdj.Services.Manager;

import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import java.net.URISyntaxException;
import java.util.concurrent.locks.ReentrantLock;


public class RemoteControl extends ThreadComponent {

    private long APP_TIMEOUT = 24*60*60*1000;  //Default value: 24H
    private long lastactivity = 0;

    private int MODE = 0; // dispatch flag between player and notifier

    private ZMQ.Context context = ZMQ.context(1);
    private ZMQ.Socket remotePublisher = context.socket(ZMQ.SUB);
    private ZMQ.Poller poller = new ZMQ.Poller(100);

    private Socket mSocket;
    private ReentrantLock LOCK = new ReentrantLock();

    private String data;
    private int last_stamp = 0;
    private JSONObject cmd_buffer = null;

    private boolean CONNECTED = false;
    private boolean CONNECTING = false;
    private boolean PREPARED = false;
    private int STATE = -1;

    // CONSTRUCTOR
    public RemoteControl(Context ctx) {
        super(ctx);
    }

    // MODE
    public void setMode(int mode) {
        MODE = mode;
    }

    // STATE
    private void setState(int state) {
        STATE = state;
        if (MODE >= Manager.MODE_WELCOME) mail("update_state").to(WelcomeActivity.class).add("state", STATE).send();
    }

    // CHECK NETWORK AVAILABILITY
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    // PREPARE THE SOCKET FOR CONNECTION
    // and Bind events
    private void prepare() {

        if (PREPARED) return;

        // Configure WebSocket
        try {
            mSocket = IO.socket(String.format("http://%s:%d",
                    appContext.getResources().getString(R.string.IP_PROXY),
                    appContext.getResources().getInteger(R.integer.PORT_INFO)));
        } catch (URISyntaxException e) {}

        // ON CONNECT
        mSocket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                CONNECTED = true;
                Log.d("RC-client", "Connected !");
            }

        })

        // ON HELLO
        .on("hello", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                JSONObject obj = (JSONObject) args[0];
                LOCK.lock();
                processHello(obj);
                LOCK.unlock();
            }

        })

        // ON COMMAND
        .on("task", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                JSONObject obj = (JSONObject) args[0];
                LOCK.lock();
                processCommand(obj);
                LOCK.unlock();
            }

        })

        // ON DISCONNECT
        .on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                CONNECTED = false;
                Log.d("RC-client", "Disconnected !");
                setState(Manager.STATE_INIT);
            }

        });

        PREPARED = true;
    }

    // START TRYING TO CONNECT
    private void connect() {
        if (!CONNECTING) {
            setState(Manager.STATE_INIT);
            mSocket.connect();
            CONNECTING = true;
        }

        // connect to proxy publisher
        /*Log.d("RC-client", "Connecting zmq Publisher");
        remotePublisher.connect(String.format("tcp://%s:%d",
                appContext.getResources().getString(R.string.IP_PROXY),
                appContext.getResources().getInteger(R.integer.PORT_PUB)));
        remotePublisher.subscribe("".getBytes());
        poller.register(remotePublisher, ZMQ.Poller.POLLIN);*/

    }

    // DISCONNECT && STOP TRYING TO CONNECT
    private void disconnect() {
        if (CONNECTING) {
            mSocket.disconnect();
            CONNECTING = false;
        }
    }

    // Re-Try CONNECT while not successful
    private void checkConnect() {
        if (!CONNECTED) {
            disconnect();
            connect();
        }
    }

    private void tryBuffer() {
        // APP now available: execute stored commands
        if (cmd_buffer != null && MODE >= Manager.MODE_WELCOME)
            processCommand(cmd_buffer);
    }


    // CLIENT LOOP: LISTEN ZMQ MESSAGES
    @Override
    protected void init() {
        Log.d("RC-client", "-- RemoteControl starting");

        SystemClock.sleep(1000);
        lastactivity = System.currentTimeMillis();
        APP_TIMEOUT = appContext.getResources().getInteger(R.integer.APP_TIMEOUT_NOACTIVITY)*60*1000;

        prepare();
    }

    @Override
    protected void loop()
    {
        // EXIT IF NOTHING HAPPENS..
        if (System.currentTimeMillis()-lastactivity > APP_TIMEOUT)
            mail("application_timeout").to(Manager.class).send();

        // CHECK LINK AND BUFFER
        if (isNetworkAvailable()) {
            checkConnect();
            tryBuffer();
        }

        // NO NETWORK: wait disconnected
        else {
            disconnect();
            setState(Manager.STATE_NONET);
        }

        // THREAD LOOP DO NOTHING: it should sleep
        SystemClock.sleep(500);

        // Poll the subscriber stack
        /*if(poller.poll(100) > 0) {
            //if (poller.pollin(0)) { // check on first Poll register

            data = remotePublisher.recvStr();

            LOCK.lock();
            processCommand(data);
            LOCK.unlock();
        }
        */


    }

    @Override
    protected void close() {
        // exit sockets
        //remotePublisher.close();
        //context.term();

        disconnect();
        mSocket.close();
        Log.d("jdj-RemoteControl", "-- RemoteControl stopped");
    }


    public void processHello(JSONObject obj) {
        Log.d("RC-client", "WS hello: " + obj);
        try {
            // CHECK Version
            String[] version = BuildConfig.VERSION_NAME.split("\\.");
            if (version.length >= 2) {
                if (obj.getJSONObject("version").getInt("major") > Integer.parseInt(version[0]))
                    mail("version_major_outdated").to(Manager.class).send();

                if (obj.getJSONObject("version").getInt("minor") > Integer.parseInt(version[1]))
                    mail("version_minor_outdated").to(Manager.class).send();
            }

            // Check SHOW STATE
            int showState = 0;
            Long deltaTime = obj.getLong("nextshow") - System.currentTimeMillis();
            Log.d("RC-client", "Delta Next show: NOW: " + System.currentTimeMillis() + " SHOW: " + obj.getLong("nextshow") + " DELTA: " + deltaTime);
            if (deltaTime < 0) showState = Manager.STATE_SHOWPAST;
            else if (deltaTime < 24*60*60*1000) showState = Manager.STATE_SHOWTIME;
            else showState = Manager.STATE_SHOWFUTURE;
            mail("update_state").to(Manager.class).add("state", showState).send();

            // PARSE LVC
            if (obj.has("lvc"))
                cmd_buffer = obj.getJSONObject("lvc");

            // DOWNLOAD AVAILABLE MEDIA
            if (obj.has("medialist") && showState == Manager.STATE_SHOWFUTURE) {
                // TODO: get media list and enqueue download tasks
            }

        }
        catch (JSONException e) { Log.d("RC-client", "invalid JSON: "+e.toString()); }
    }

    protected void processCommand(JSONObject task) {
        //TODO
        // - check if file is available in local
        // - use atTime for synced play
        // - use GROUP_PUB

        Log.d("RC-client", "WS task: " + task);

        //Something is happening
        lastactivity = System.currentTimeMillis();

        try {

            //APP is not available
            //
            if (MODE < Manager.MODE_WELCOME) {
                // Store command in Cache Buffer
                if (task.has("cache") && task.getBoolean("cache")) cmd_buffer = task;
                mail("application_need_attention").to(Manager.class).send();
                return;
            }

            //APP is available: execute command now !
            //
            // Check if command has an expiration timestamp
            if (task.has("timestamp")) {
                int ts = task.getInt("timestamp");
                if (ts <= last_stamp) {
                    Log.d("RC-client", "command already executed");
                    return;
                }
                else last_stamp = ts;
            }

            // Parse "action"
            if (task.has("action")) {

                Mailbox msg = mail("command").to(Manager.class).add("controller", "remote");

                // ACTION TO PERFORM
                msg.add("action", task.getString("action"));

                // PLAYER ENGINE SELECTOR: web | audio | video | phone
                if (task.has("category")) msg.add("engine", task.getString("category"));
                else msg.add("param1", "none");

                // EXTRA PARAMETER
                if (task.has("param1")) msg.add("param1", task.getString("param1"));
                else msg.add("param1", "");

                // FILE PATH SELECTOR (WORST TO BEST)
                String filepath = null;
                int medialevel = 0;

                // PROGRESSIVE STREAMING
                if (task.has("url")) {
                    filepath = task.getString("url");
                    medialevel = 1;
                }

                // ADAPTATIVE STREAMING
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
                    if (task.has("hls")) {
                        filepath = task.getString("hls");
                        medialevel = 2;
                    }
                }

                // LOCAL FILE
                if (task.has("filename")) {
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

        //Empty command buffer
        cmd_buffer = null;
    }


}
