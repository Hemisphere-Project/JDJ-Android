package com.hmsphr.jdj.Components;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.SystemClock;
import android.util.Log;

import com.github.nkzawa.emitter.Emitter;
import com.hmsphr.jdj.BuildConfig;
import com.hmsphr.jdj.Class.Mailbox;
import com.hmsphr.jdj.Class.ThreadComponent;
import com.hmsphr.jdj.Class.Utils.Show;
import com.hmsphr.jdj.Class.Utils.ShowList;
import com.hmsphr.jdj.R;
import com.hmsphr.jdj.Services.Manager;

import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import java.net.URISyntaxException;
import java.util.concurrent.locks.ReentrantLock;


public class RemoteControl extends ThreadComponent {

    private long APP_TIMEOUT = 24*60*60*1000;  //Default value: 24H
    private long lastactivity = 0;

    private boolean PLAYER_READY = false; // dispatch flag between player and notifier
    private boolean PERFORM_REGISTRATION = false; // DO REGISTER FLAG

    private Socket mSocket;
    private ReentrantLock LOCK = new ReentrantLock();

    private String data;
    private int last_stamp = 0;

    private JSONObject cmd_buffer = null;

    private boolean CONNECTED = false;
    private boolean CONNECTING = false;
    private boolean PREPARED = false;
    private int CONFAIL = 100;
    private int STATE = -1;
    public int MODE = 0;

    // CONSTRUCTOR
    public RemoteControl(Context ctx) {
        super(ctx);
    }

    // MODE
    public void playerReady(boolean isReady) {
        PLAYER_READY = isReady;
    }

    // REGISTER
    public void registrationReady(boolean isReady) {
        PERFORM_REGISTRATION = isReady;
    }

    // STATE (to Manager)
    private void setState(int state) {
        //if (STATE == state) return;

        STATE = state;
        if (isMyServiceRunning(Manager.class))
            mail("change_state").to(Manager.class).add("state", STATE).send();
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
                    appContext.getResources().getInteger(R.integer.PORT_CMD)));
        } catch (URISyntaxException e) {}

        // ON CONNECT
        mSocket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                CONNECTED = true;
                CONFAIL = 0;
                Log.d("RC-client", "Connected !");
            }

        })

        .on(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Log.e("RC-client", "socketIO Conn Error ! " + args[0]);
            }

        })
        .on(Socket.EVENT_CONNECT_TIMEOUT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Log.e("RC-client", "socketIO Timeout ! " + args[0]);
            }

        })

        // ON WHOAREYOU
        .on("whoareyou", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                //send iam with userId
                int userId = settings().getInt("com.hmsphr.jdj.userid", -1);
                JSONObject obj = new JSONObject();
                try {
                    if (userId >= 0) obj.put("userid", userId);
                } catch (JSONException e) {
                }
                mSocket.emit("iam", obj);
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

        Log.d("RC-client", "WS prepared ");
        PREPARED = true;
    }

    // RESET LINK
    public void reset() {
        last_stamp = 0;
        disconnect();
        connect();
    }

    // START TRYING TO CONNECT
    private void connect() {
        if (!CONNECTING) {
            Log.d("RC-client", "Connecting.. ");
            CONNECTING = true;
            setState(Manager.STATE_INIT);
            mSocket.connect();
        }
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
        if (!CONNECTED) CONFAIL++;

        int timeout = 10;
        if (MODE == Manager.MODE_PLAY) timeout = 20;

        if (CONFAIL > timeout) {
            CONFAIL = 0;
            disconnect();
            connect();
        }
    }

    private void storeTask(JSONObject task) {
        // Store command in Cache Buffer
        if (task == null) {
            cmd_buffer = null;
            return;
        }
        try {
            if (task.has("timestamp")) task.remove("timestamp");
            if (task.has("cache") && task.getBoolean("cache")) cmd_buffer = task;
            if (task.has("action"))
                mail("application_need_attention").to(Manager.class).add("action", task.getString("action")).send();
        } catch (JSONException e) { Log.d("RC-client", "invalid JSON: "+e.toString()); }
    }

    private void tryBuffer() {
        // APP now available: execute stored commands
        if (cmd_buffer != null && PLAYER_READY)
            processCommand(cmd_buffer);
    }

    private void doRegister() {
        if (PERFORM_REGISTRATION && CONNECTED) {
            PERFORM_REGISTRATION = false;
            JSONObject obj = new JSONObject();
            try {
                int userId = settings().getInt("com.hmsphr.jdj.userid", -1);
                if (userId >= 0) obj.put("userid", userId);

                String phone = settings().getString("com.hmsphr.jdj.phone", null);
                obj.put("number", phone);

                String s = settings().getString("com.hmsphr.jdj.show", null);
                Show theshow = Show.inflate( s );
                if (theshow != null) obj.put("showid", theshow.getId());
                obj.put("os", "android-"+android.os.Build.VERSION.SDK_INT);

            } catch (JSONException e) {}

            mSocket.emit("subscribe", obj);
        }
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
            doRegister();
        }

        // NO NETWORK: wait disconnected
        else {
            disconnect();
            if (STATE != Manager.STATE_NONET)
                setState(Manager.STATE_NONET);
        }

        // THREAD LOOP DO NOTHING: it should sleep
        SystemClock.sleep(1000);

    }

    @Override
    protected void close() {
        disconnect();
        mSocket.close();
        Log.d("jdj-RemoteControl", "-- RemoteControl stopped");
    }


    public void processHello(JSONObject obj) {
        Log.d("RC-client", "WS hello: " + obj);

        // USER
        Integer userId = null;
        String errorUser = null;
        boolean fullHello = false;

        // VERSION
        int serverVersion[] = {0,0,0};

        // LVC
        JSONObject lvcTask = null;

        // PARSE Hello Object
        try {

            // Make show list
            if (obj.has("showlist") && !obj.isNull("showlist")) {

                ShowList showlist = new ShowList();
                JSONArray list = obj.getJSONArray("showlist");
                for (int i = 0; i < list.length(); i++) {
                    JSONObject row = list.getJSONObject(i);
                    showlist.addShow(row.getInt("id"), row.getString("date"), row.getString("place"));
                }
                //Log.e("jdj-RC", list_export.toString());

                settings().edit()
                        .putString("com.hmsphr.jdj.show_list", showlist.export())
                        .commit();
            }

            // Get LVC Task
            if (obj.has("lvc")) lvcTask = obj.getJSONObject("lvc");

            // INFO to display
            if (obj.has("info"))
                settings().edit().putString("com.hmsphr.jdj.info", obj.getString("info")).commit();

            // RETRIEVE user info
            if (obj.has("user"))
            {
                JSONObject user = obj.getJSONObject("user");

                // User id
                if (!user.isNull("id")) {
                    userId = user.getInt("id");
                    settings().edit().putInt("com.hmsphr.jdj.userid", userId).commit();
                }

                // Phone number
                if (!user.isNull("number")) {
                    settings().edit()
                            .putString("com.hmsphr.jdj.phone", user.getString("number"))
                            .commit();
                }

                // Event selected
                if (!user.isNull("event")) {
                    Show theShow = new Show(user.getJSONObject("event").getInt("id"),
                            user.getJSONObject("event").getString("date"),
                            user.getJSONObject("event").getString("place"));
                    settings().edit()
                            .putString("com.hmsphr.jdj.show", theShow.export())
                            .commit();
                }

                // ERRORS
                if (!user.isNull("error")) {
                    Log.d("jdj-RC", "error found: " + user.getString("number"));
                    errorUser = user.getString("error");
                    settings().edit().putString("com.hmsphr.jdj.error_user", errorUser).commit();
                } else settings().edit().putString("com.hmsphr.jdj.error_user", "").commit();

                // WILL DISPLAY REGISTER if necessary
                fullHello = true;

                // GROUP & SECTIONS
                settings().edit().putString("com.hmsphr.jdj.group", user.getString("group")).commit();
                settings().edit().putBoolean("com.hmsphr.jdj.section.A", user.getJSONObject("section").getBoolean("A")).commit();
                settings().edit().putBoolean("com.hmsphr.jdj.section.B", user.getJSONObject("section").getBoolean("B")).commit();
                settings().edit().putBoolean("com.hmsphr.jdj.section.C", user.getJSONObject("section").getBoolean("C")).commit();

                // Get Version
                serverVersion[0] = obj.getJSONObject("version").getInt("main");
                serverVersion[1] = obj.getJSONObject("version").getInt("major");
                serverVersion[2] = obj.getJSONObject("version").getInt("android-minor");
            }

        } catch (JSONException e) { Log.d("RC-client", "invalid JSON: "+e.toString()); }

        // App version fetching
        String[] version = BuildConfig.VERSION_NAME.split("\\.");
        int appVersion[] = {0,0,0};
        if (version.length >= 3) {
            appVersion[0] = Integer.parseInt(version[0]);
            appVersion[1] = Integer.parseInt(version[1]);
            appVersion[2] = Integer.parseInt(version[2]);
        }

        // CHECK If Major version break
        if ((serverVersion[0] > appVersion[0]) || (serverVersion[1] > appVersion[1])) {
            mail("version_major_outdated").to(Manager.class).send();
        }

        // USER INFO INCOMPLETE: Trigger Register FORM
        else if (fullHello && (userId == null || errorUser != null)) {
            setState(Manager.STATE_NOUSER);
        }

        // USER INFO COMPLETE: Proceed
        else
        {
            // Minor VERSION Checking
            if (serverVersion[2] > appVersion[2])
                    mail("version_minor_outdated").to(Manager.class).send();

            // Check SHOW STATE / INFO
            int showState = Manager.STATE_SHOWTIME;
            setState(showState);

            // STORE LVC
            processCommand(lvcTask);

            // DOWNLOAD AVAILABLE MEDIA
            /*if (obj.has("medialist") && showState == Manager.STATE_SHOWFUTURE) {
                // TODO: get media list and enqueue download tasks
            }*/
        }

    }

    protected void processCommand(JSONObject task) {

        if (task == null) return;
        Log.d("RC-client", "WS task: " + task);

        //Something is happening
        lastactivity = System.currentTimeMillis();

        //Clear command buffer
        storeTask(null);

        try {

            // Check if command has just been executed
            if (task.has("timestamp")) {
                int ts = task.getInt("timestamp");
                if (ts == last_stamp) {
                    Log.d("RC-client", "command already executed");
                    return;
                }
                else last_stamp = ts;
            }

            // Check GROUP
            if (task.has("group") && !task.getString("group").equals(settings().getString("com.hmsphr.jdj.group", ""))) {
                Log.d("RC-client", "not in the group.. ignoring");
                return;
            }
            // Check SECTION
            if (task.has("section") && !settings().getBoolean("com.hmsphr.jdj.section."+task.getString("section"), false) ) {
                Log.d("RC-client", "not in the section.. ignoring");
                return;
            }

            //APP is not available
            //

            if (!PLAYER_READY) {
                storeTask(task);
                return;
            }

            //APP is available: execute command now !
            //

            // Parse "action"
            if (task.has("action")) {

                Mailbox msg = mail("command").to(Manager.class).add("controller", "remote");

                // ACTION TO PERFORM
                msg.add("action", task.getString("action"));

                // PLAYER ENGINE SELECTOR: web | audio | video | phone | text
                if (task.has("category")) msg.add("engine", task.getString("category"));
                else msg.add("engine", "");

                // ATTIME PARAMETER
                if (task.has("atTime")) msg.add("atTime", task.getLong("atTime"));
                else msg.add("atTime", 0);

                // EXTRA PARAMETER
                if (task.has("param1")) msg.add("param1", task.getString("param1"));
                else msg.add("param1", "");

                // STORE CACHE
                if (task.has("cache") && task.getBoolean("cache")) msg.add("cache", true);
                else msg.add("cache", false);

                // PAYLOAD (content or url)
                String payload = null;
                int medialevel = 0;

                    // STATIC CONTENT
                    if (task.has("content")) {
                        // MULTIPLE CONTENT: PICK ONE based on USERID % MSGNBR
                        String[] contents = task.getString("content").split("@%%#");
                        if (contents.length > 1) payload = contents[ settings().getInt("com.hmsphr.jdj.userid", 0) % contents.length ];
                        else payload = task.getString("content");
                    }
                    else {
                        // PROGRESSIVE STREAMING
                        if (task.has("url")) {
                            payload = task.getString("url");
                            medialevel = 1;
                        }

                        // ADAPTATIVE STREAMING
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
                            if (task.has("hls")) {
                                payload = task.getString("hls");
                                medialevel = 2;
                            }
                        }

                        // LOCAL FILE
                        if (task.has("filename")) {
                            // TODO CHECK IF FILE AVAILABLE LOCALLY
                        /*File media = getMediaFile(appContext, command.getString("filename"));
                        if (media.exists()) {
                            payload = media.getPath();
                            medialevel = 3;
                        }*/
                        }
                    }

                // ADD FILE
                Log.d("RC-client", "payload: "+payload+" level: "+medialevel);
                msg.add("medialevel", medialevel);
                msg.add("payload", payload);


                msg.send();
            }
            else Log.d("RC-client", "action missing ");

        }
        catch (JSONException e) { Log.d("RC-client", "invalid JSON "); }

    }


}
