package com.hmsphr.jdj.Activities;

import android.os.Bundle;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.VideoView;

import com.google.android.exoplayer.AspectRatioFrameLayout;
import com.google.android.exoplayer.text.SubtitleLayout;
import com.hmsphr.jdj.Class.MediaActivity;
import com.hmsphr.jdj.Components.Players.MediaPlayerClassic;
import com.hmsphr.jdj.Components.Players.MediaPlayerExo;
import com.hmsphr.jdj.R;
import com.hmsphr.jdj.Services.Manager;

public class VideoActivity extends MediaActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (android.os.Build.VERSION.SDK_INT >= 16)
        //if (false)
        {
            // Configure HLS movie player
            setContentView(R.layout.view_exoplayer);
            player = new MediaPlayerExo(this,
                    (AspectRatioFrameLayout) findViewById(R.id.videoView),
                    (SurfaceView) findViewById(R.id.videoSurface),
                    (ImageView) findViewById(R.id.audioShutter),
                    (FrameLayout) findViewById(R.id.loadShutter));
            info("ExoPlayer started");
        }
        else
        {
            // Configure Classic MediaPlayerCommon
            setContentView(R.layout.view_classicplayer);
            player = new MediaPlayerClassic(this,
                                (VideoView) findViewById(R.id.videoView),
                                (ImageView) findViewById(R.id.audioShutter),
                                (FrameLayout) findViewById(R.id.loadShutter));
            info("ClassicPlayer started");
        }

        // Set Replay menu
        player.setReplay(
                (FrameLayout) findViewById(R.id.replayShutter),
                (FrameLayout) findViewById(R.id.replayOverlay),
                (ImageButton) findViewById(R.id.replayBtn));


        // Transfer first intent
        onNewIntent(getIntent());
    }

    public void onVideoEnd() {
        mail("video_end").to(Manager.class).send();
    }

    public void onVideoFreeze() {
        mail("video_freeze").to(Manager.class).send();
    }
}
