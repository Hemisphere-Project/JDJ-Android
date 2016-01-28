package com.hmsphr.jdj.Activities;

import android.os.Bundle;
import android.webkit.WebView;
import android.widget.FrameLayout;

import com.hmsphr.jdj.Class.MediaActivity;
import com.hmsphr.jdj.Components.Players.WebPlayer;
import com.hmsphr.jdj.R;

public class WebActivity extends MediaActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_webview);

        // Configure WebView
        player = new WebPlayer(this,
                            (WebView) findViewById(R.id.playerWEB),
                            (FrameLayout) findViewById(R.id.loadShutter));

        // Transfer first intent
        onNewIntent(getIntent());
    }


}
