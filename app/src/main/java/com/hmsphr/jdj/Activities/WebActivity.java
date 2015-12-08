package com.hmsphr.jdj.Activities;

import android.os.Bundle;
import android.webkit.WebView;

import com.hmsphr.jdj.Class.MediaActivity;
import com.hmsphr.jdj.Components.Players.WebPlayer;
import com.hmsphr.jdj.R;

public class WebActivity extends MediaActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_webview);

        // Configure WebView
        player = new WebPlayer(this, (WebView) findViewById(R.id.playerWEB) );

        // Transfer first intent
        onNewIntent(getIntent());
    }


}
