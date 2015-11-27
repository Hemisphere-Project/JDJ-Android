package com.hmsphr.jdj.Class;

import android.content.Intent;
import android.os.Bundle;

import com.hmsphr.jdj.Components.Players.PlayerCompat;
import com.hmsphr.jdj.Services.Manager;

/**
 * Created by mgr on 26/10/15.
 */
public class MediaActivity extends ManagedActivity {

    // internal Player
    protected PlayerCompat player = null;

    // Intent with COMMAND
    @Override
    protected void onNewIntent (Intent intent) {
        super.onNewIntent(intent);

        // Check if player is available
        if (player != null) {
            String action = intent.getStringExtra("message");
            String url = intent.getStringExtra("url");

            // Execute command
            // STOP
            if (action.equals("stop")) {
                player.stop();
                finish();
            }

            // PLAY
            else if (action.equals("play")) {
                if (url == null) {error("Play action must provide an url");  return;}
                player.load( url );
                player.play();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MODE = Manager.MODE_PLAY;
    }

    // Fullscreen
    @Override
    protected void onResume() {
        super.onResume();
        // Player resume
        if (player != null) player.resume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (player != null) player.pause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (player != null) player.stop();
    }

}
