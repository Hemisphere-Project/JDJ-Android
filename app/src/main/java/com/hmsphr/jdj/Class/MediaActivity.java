package com.hmsphr.jdj.Class;

import android.content.Intent;
import android.os.Bundle;

import com.hmsphr.jdj.Components.Players.PlayerCompat;

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
            // Parse Intent
            Bundle extras = intent.getExtras();
            if (extras == null) {
                error("Intent must provide extras");
                return;
            }
            String action = extras.getString("action");
            if (action == null) {
                error("Intent must provide an action");
                return;
            }

            // Execute command
            // STOP
            if (action.equals("stop")) {
                player.stop();
                finish();
            }

            // PLAY
            else if (action.equals("play")) {
                String url = extras.getString("url");
                if (url == null) {error("Play action must provide an url");  return;}
                player.load( url );
                player.play();
            }
        }
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
