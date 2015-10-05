package com.hmsphr.jdj.Components;

import org.zeromq.ZMQ;
import android.util.Log;

import com.hmsphr.jdj.Class.ThreadComponent;


public class RemoteClient extends ThreadComponent {

    private ZMQ.Context context = ZMQ.context(1);
    private ZMQ.Socket socket = context.socket(ZMQ.SUB);
    private ZMQ.Poller poller = new ZMQ.Poller(100);

    private String address = null;
    private String data;

    private int PORT_PUB;
    private String IP_PUB;
    private String SUBJECT_PUB = "";

    public RemoteClient(String ip, int port, String subject) {
        PORT_PUB = port;
        IP_PUB = ip;
        SUBJECT_PUB = subject;
    }

    // CLIENT LOOP: LISTEN ZMQ MESSAGES
    @Override
    protected void init() {

        Log.d("jdj-RemoteClient", "-- RemoteClient starting");

        // connect to proxy publisher
        socket.connect(String.format("tcp://%s:%d", IP_PUB, PORT_PUB));
        socket.subscribe(SUBJECT_PUB.getBytes());

        // register socket in poll
        poller.register(socket, ZMQ.Poller.POLLIN);
    }

    @Override
    protected void loop()
    {
        if(poller.poll(100) > 0) {
            //if (poller.pollin(0)) { // check on first Poll register

            data = socket.recvStr();


            if (address != null)
            {
                // Log received instructions
                Log.v("mgrlog", address + " : " + data);

                // send to Commander

                // Wait for next address..
                address = null;
            }
            else if (data.equals(SUBJECT_PUB)) address = data;
            //else Log.v("mgrlog", data);
            //}
        }
    }

    @Override
    protected void close() {
        // exit socket
        socket.close();
        context.term();
        Log.d("jdj-RemoteClient", "-- RemoteClient stopped");
    }
}
