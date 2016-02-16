package com.hmsphr.jdj.Class;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.VideoView;

import com.google.android.exoplayer.AspectRatioFrameLayout;
import com.google.android.exoplayer.text.SubtitleLayout;
import com.hmsphr.jdj.Class.ManagedActivity;
import com.hmsphr.jdj.Components.Players.MediaPlayerClassic;
import com.hmsphr.jdj.Components.Players.MediaPlayerExo;
import com.hmsphr.jdj.Components.Players.PlayerCompat;
import com.hmsphr.jdj.Components.TimeSync;
import com.hmsphr.jdj.R;
import com.hmsphr.jdj.Services.Manager;

/**
 * Created by mgr on 26/10/15.
 */
public class MediaActivity extends ManagedActivity {

    // internal Player
    protected PlayerCompat player = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MODE = Manager.MODE_PLAY;
    }


    // Intent with COMMAND
    @Override
    protected void onNewIntent (Intent intent) {
        super.onNewIntent(intent);

        Log.e("jdj-MediaActivity", "Processing intent "+intent.getStringExtra("message"));
        // Check if player is available
        if (player != null) {
            String action = intent.getStringExtra("message");
            String payload = intent.getStringExtra("payload");
            String mode = intent.getStringExtra("mode");
            long atTime = intent.getLongExtra("atTime", 0);

            // STOP
            //if (action.equals("stop")) doStop(atTime);

            // PLAY
            if (action.equals("play")) {

                // Check PAYLOAD
                if (payload == null) {error("Play action must provide a payload");  return;}

                // Set player mode
                player.setMode(mode);

                // Play MEDIA
                player.play(payload, atTime);
            }
        }
    }

    private void waitFor(long timestamp) {
        if (timestamp - SystemClock.elapsedRealtime() < 1000)
            while(timestamp > SystemClock.elapsedRealtime()) ;
    }

    private void doStop(long atTime) {
        // Sync with atTime
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                player.stop();
                finish();
            }
        }, Math.max(0, atTime - SystemClock.elapsedRealtime()));
    }

    @Override
    public void onRestart() {
        super.onRestart();
        Log.e("jdj-MediaActivity", "Restart");
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
