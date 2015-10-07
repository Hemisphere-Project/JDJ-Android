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

import org.json.JSONException;
import org.json.JSONObject;

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

    public void stopAll() {
        moviePlayerExo.stop();
        webPlayer.stop();
    }

    @Override
    protected void  onNewIntent (Intent intent) {
        super.onNewIntent(intent);

        // Parse Intent
        Bundle extras = intent.getExtras();
        String action = extras.getString("action");
        String category = extras.getString("category");
        String url = extras.getString("url");

        // Execute command
        if(action != null && category != null && url != null)
        {
            // STOP
            if (action.equals("stop")) stopAll();

            // PLAY
            else if (action.equals("play")) {
                stopAll();
                currentPlayer = null;

                if (category.equals("url")) currentPlayer = webPlayer;
                else if (category.equals("audio")) currentPlayer = moviePlayerExo;
                else if (category.equals("video")) currentPlayer = moviePlayerExo;

                if (currentPlayer != null) currentPlayer.play( url );
            }
        }
        else Log.e("PlayersActivity", "Malformed command: action="+action+" cat="+category+" url="+url);

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
