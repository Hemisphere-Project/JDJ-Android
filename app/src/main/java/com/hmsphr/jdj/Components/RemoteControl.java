package com.hmsphr.jdj.Components;

import org.json.JSONException;
import org.json.JSONObject;
import org.zeromq.ZMQ;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.hmsphr.jdj.Activities.AudioActivity;
import com.hmsphr.jdj.Activities.VideoActivity;
import com.hmsphr.jdj.Activities.WebActivity;
import com.hmsphr.jdj.Activities.WelcomeActivity;
import com.hmsphr.jdj.Class.ThreadComponent;
import com.hmsphr.jdj.Services.Manager;


public class RemoteControl extends ThreadComponent {

    private int MODE = 0; // dispatch flag between player and notifier
    private Context appContext;

    private ZMQ.Context context = ZMQ.context(1);
    private ZMQ.Socket remote = context.socket(ZMQ.SUB);
    private ZMQ.Socket manager = context.socket(ZMQ.DEALER);
    private ZMQ.Poller poller = new ZMQ.Poller(100);

    private String address = null;
    private String data;

    private int PORT_PUB;
    private String IP_PUB;
    private String GROUP_PUB = "";

    public RemoteControl(Context ctx, String ip, int port) {
        appContext = ctx;
        PORT_PUB = port;
        IP_PUB = ip;
    }

    public void setMode(int mode) {
        MODE = mode;
    }

    // CLIENT LOOP: LISTEN ZMQ MESSAGES
    @Override
    protected void init() {

        Log.d("jdj-RemoteControl", "-- RemoteControl starting");

        // connect to proxy publisher
        remote.connect(String.format("tcp://%s:%d", IP_PUB, PORT_PUB));
        remote.subscribe("all".getBytes());

        //connect to Manager
        manager.connect("inproc://manager");

        // register socket in poll
        poller.register(remote, ZMQ.Poller.POLLIN);
    }

    @Override
    protected void loop()
    {
        if(poller.poll(100) > 0) {
            //if (poller.pollin(0)) { // check on first Poll register

            data = remote.recvStr();
            if (address != null)
            {
                // Log received instructions
                Log.v("RC-client", address + " : " + data);

                // Parse and execute order
                try {
                    JSONObject command = new JSONObject(data);

                    //TODO
                    // - check if file is available in local
                    // - use atTime for synced play
                    // - dispatch with MODE

                    if (MODE >= Manager.MODE_WELCOME) {

                        if (command.has("action")) {

                            Intent cmdIntent = new Intent(appContext, WelcomeActivity.class);

                            if (command.has("category"))
                            {
                                String cat = command.getString("category");
                                if (cat.equals("url"))
                                    cmdIntent = new Intent(appContext, WebActivity.class);
                                else if (cat.equals("video"))
                                    cmdIntent = new Intent(appContext, VideoActivity.class);
                                else if (cat.equals("audio"))
                                    cmdIntent = new Intent(appContext, AudioActivity.class);
                            }

                            cmdIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                            if (command.has("action"))
                                cmdIntent.putExtra("action", command.getString("action"));

                            // TODO CHECK IF LOCAL FILE EXIST !
                            if (command.has("url")) {
                                cmdIntent.putExtra("mode", "remote");
                                cmdIntent.putExtra("url", command.getString("url"));
                            }

                            appContext.startActivity(cmdIntent);
                        }
                        else Log.d("action missing ", command.toString());

                    }

                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }

                // Wait for next address..
                address = null;
            }
            else if (data.equals(GROUP_PUB) || data.equals("all")) address = data;
            else Log.v("RC-client unknown: ", data);
            //}
        }
    }

    @Override
    protected void close() {
        // exit socket
        remote.close();
        context.term();
        Log.d("jdj-RemoteControl", "-- RemoteControl stopped");
    }
}
