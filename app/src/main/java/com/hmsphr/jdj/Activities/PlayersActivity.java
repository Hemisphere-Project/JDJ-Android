package com.hmsphr.jdj.Activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
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

public class PlayersActivity extends ManagedActivity {

    private WebPlayer webPlayer;
    private MediaPlayerExo moviePlayerExo;
    private PlayerCompat currentPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_players);

        // Set Manager Mode
        manager.setMode(Manager.MODE_PLAY);

        // Configure HLS movie player

        moviePlayerExo = new MediaPlayerExo(this, (AspectRatioFrameLayout) findViewById(R.id.playerVIDEO),
                                            (SurfaceView) findViewById(R.id.playerVIDEO_surface),
                                            (View) findViewById(R.id.playerVIDEO_shutter),
                                            (SubtitleLayout) findViewById(R.id.playerVIDEO_subtitles));

        // Configure WebView
        webPlayer = new WebPlayer(this, (WebView) findViewById(R.id.playerWEB) );

        // Transfer first intent
        onNewIntent(getIntent());
    }

    public void hideAll() {
        moviePlayerExo.hide();
        webPlayer.hide();
    }

    @Override
    protected void  onNewIntent (Intent intent) {
        super.onNewIntent(intent);
        Log.v("mgrlog", "Intent received.." + intent);

        hideAll();
        currentPlayer = null;
        if (intent.getDataString().endsWith("m3u8")) currentPlayer = moviePlayerExo;
        else currentPlayer = webPlayer;

        if (currentPlayer != null) currentPlayer.play(intent.getDataString());
    }

    @Override
    public void onResume() {
        super.onResume();
        currentPlayer.resume();
    }

    @Override
    public void onPause() {
        super.onPause();
        currentPlayer.pause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        currentPlayer.stop();
    }
}
