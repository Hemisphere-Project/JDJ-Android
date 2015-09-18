package com.hmsphr.jdj.Components;

import org.zeromq.ZMQ;
import android.util.Log;

import com.hmsphr.jdj.Class.ThreadComponent;


public class RemoteClient extends ThreadComponent {

    private ZMQ.Context context = ZMQ.context(1);
    private ZMQ.Socket socket = context.socket(ZMQ.SUB);
    private ZMQ.Poller poller = new ZMQ.Poller(100);

    // CLIENT LOOP: LISTEN ZMQ MESSAGES
    @Override
    protected void init() {

        Log.d("jdj-RemoteClient", "-- RemoteClient starting");

        // connect to proxy publisher
        socket.connect("tcp://10.0.2.2:5557");
        socket.subscribe("zenner".getBytes());

        // register socket in poll
        poller.register(socket, ZMQ.Poller.POLLIN);
    }

    @Override
    protected void loop()
    {
        String address = null;
        String data;

        if(poller.poll(100) > 0) {
            //if (poller.pollin(0)) { // check on first Poll register

            data = socket.recvStr();
            if (address != null)
            {
                // Log received instructions
                Log.v("mgrlog", address + " : " + data);

                // send to Commander
                // emmit( data );

                // Wait for next address..
                address = null;
            }
            else if (data.equals("zenner")) address = data;
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
