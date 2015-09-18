package com.hmsphr.jdj.Class;

import org.zeromq.ZMQ;
import android.util.Log;


public class RemoteClient {

    public RemoteClient() {
    }

    /*
     INTERNAL
     */
    private Boolean RUN = true;
    private Thread subscriber = new Thread() {
        @Override
        public void run() {
            subscribe();
        }
    };


    // START CLIENT IN A THREAD
    public void start() {
        stop();
        RUN = true;
        this.subscriber.start();
    }

    // STOP CLIENT THREAD
    public void stop() {
        RUN = false;
        try {
            this.subscriber.join(300); // wait for subscriber to finish
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        if (this.subscriber.isAlive()) this.subscriber.interrupt(); // force subscriber to stop
    }

    // CLIENT LOOP: LISTEN ZMQ MESSAGES
    private void subscribe() {

        Log.v("mgrlog", "-- RemoteClient starting");

        //ZMQ Receiver
        // Prepare our context and subscriber
        ZMQ.Context context = ZMQ.context(1);
        ZMQ.Socket socket = context.socket(ZMQ.SUB);

        // connect to proxy publisher
        socket.connect("tcp://10.0.2.2:5557");
        socket.subscribe("zenner".getBytes());

        // create Poller
        ZMQ.Poller poller = new ZMQ.Poller(100);

        // register socket in poller
        int PUB = 0;
        poller.register(socket, ZMQ.Poller.POLLIN);

        String address = null;
        String data;

        while(!Thread.currentThread().isInterrupted() && RUN)
        {
            if(poller.poll(1000) > 0) {
                //if (poller.pollin(0)) { // check on first Poller register

                data = socket.recvStr();
                if (address != null)
                {
                    // Log received instructions
                    Log.v("mgrlog", address + " : " + data);

                    // execute commands
                    // execute( data );

                    // Wait for next address..
                    address = null;
                }
                else if (data.equals("zenner")) address = data;
                //}
            }
        }

        // exit socket
        socket.close();
        context.term();

        Log.v("mgrlog", "-- RemoteClient stopped");
    }
}
