package com.hmsphr.jdj.Activities;

import android.os.Bundle;
import android.view.SurfaceView;
import android.view.View;

import com.google.android.exoplayer.AspectRatioFrameLayout;
import com.google.android.exoplayer.text.SubtitleLayout;
import com.hmsphr.jdj.Class.MediaActivity;
import com.hmsphr.jdj.Components.Players.MediaPlayerExo;
import com.hmsphr.jdj.R;
import com.hmsphr.jdj.Services.Manager;

public class AudioActivity extends MediaActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio);

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
