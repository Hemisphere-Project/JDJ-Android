package com.hmsphr.jdj.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.SurfaceView;
import android.view.View;
import android.webkit.WebView;

import com.google.android.exoplayer.AspectRatioFrameLayout;
import com.google.android.exoplayer.text.SubtitleLayout;
import com.hmsphr.jdj.Class.ManagedActivity;
import com.hmsphr.jdj.Class.MediaPlayerExo;
import com.hmsphr.jdj.Class.PlayerCompat;
import com.hmsphr.jdj.Class.WebPlayer;
import com.hmsphr.jdj.R;
import com.hmsphr.jdj.Services.Manager;

public class WebActivity extends ManagedActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web);

        // Set Manager Mode
        manager.setMode(Manager.MODE_PLAY);

        // Configure WebView
        player = new WebPlayer(this, (WebView) findViewById(R.id.playerWEB) );

        // Transfer first intent
        onNewIntent(getIntent());
    }

}
