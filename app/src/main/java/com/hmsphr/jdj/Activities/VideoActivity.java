package com.hmsphr.jdj.Activities;

import android.os.Bundle;
import android.view.SurfaceView;
import android.view.View;
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

        // Set Manager Mode
        manager.setMode(Manager.MODE_PLAY);

        int currentapiVersion = android.os.Build.VERSION.SDK_INT;
        if (currentapiVersion >= android.os.Build.VERSION_CODES.JELLY_BEAN)
        {
            // Configure HLS movie player
            setContentView(R.layout.activity_exo);
            player = new MediaPlayerExo(this, (AspectRatioFrameLayout) findViewById(R.id.playerVIDEO),
                    (SurfaceView) findViewById(R.id.playerVIDEO_surface),
                    (View) findViewById(R.id.playerVIDEO_shutter),
                    (SubtitleLayout) findViewById(R.id.playerVIDEO_subtitles));

            info("ExoPlayer started");
        }
        else
        {
            // Configure Classic MediaPlayer
            setContentView(R.layout.activity_video);
            player = new MediaPlayerClassic(this, (VideoView) findViewById(R.id.videoView));

            info("ClassicPlayer started");
        }


        // Transfer first intent
        onNewIntent(getIntent());
    }



}
