package com.hmsphr.jdj.Activities;

import android.os.Bundle;
import android.view.SurfaceView;
import android.view.View;

import com.google.android.exoplayer.AspectRatioFrameLayout;
import com.google.android.exoplayer.text.SubtitleLayout;
import com.hmsphr.jdj.Class.ManagedActivity;
import com.hmsphr.jdj.Class.MediaPlayerExo;
import com.hmsphr.jdj.R;
import com.hmsphr.jdj.Services.Manager;

public class VideoActivity extends ManagedActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);

        // Set Manager Mode
        manager.setMode(Manager.MODE_PLAY);

        // Configure HLS movie player
        player = new MediaPlayerExo(this, (AspectRatioFrameLayout) findViewById(R.id.playerVIDEO),
                                            (SurfaceView) findViewById(R.id.playerVIDEO_surface),
                                            (View) findViewById(R.id.playerVIDEO_shutter),
                                            (SubtitleLayout) findViewById(R.id.playerVIDEO_subtitles));

        // Transfer first intent
        onNewIntent(getIntent());
    }



}
