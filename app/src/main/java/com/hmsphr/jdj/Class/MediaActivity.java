package com.hmsphr.jdj.Class;

import android.content.Intent;
import android.os.Bundle;
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

        // Check if player is available
        if (player != null) {
            String action = intent.getStringExtra("message");
            String url = intent.getStringExtra("url");
            String mode = intent.getStringExtra("mode");

            // Shutter mode AUDIO / VIDEO
            if (mode != null) {
                ImageView audioShutter = (ImageView) findViewById(R.id.audioShutter);
                if (mode.equals("video")) audioShutter.setVisibility(View.GONE);
                else if (mode.equals("audio")) audioShutter.setVisibility(View.VISIBLE);
            }

            // Execute command
            // STOP
            if (action.equals("stop")) {
                player.stop();
                finish();
            }

            // PLAY
            else if (action.equals("play")) {
                if (url == null) {error("Play action must provide an url");  return;}
                player.load(url);
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
