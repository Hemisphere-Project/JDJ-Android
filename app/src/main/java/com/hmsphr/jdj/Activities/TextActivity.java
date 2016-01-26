package com.hmsphr.jdj.Activities;

import android.os.Bundle;
import android.webkit.WebView;
import android.widget.TextView;

import com.hmsphr.jdj.Class.MediaActivity;
import com.hmsphr.jdj.Components.Players.TextPlayer;
import com.hmsphr.jdj.Components.Players.WebPlayer;
import com.hmsphr.jdj.R;

public class TextActivity extends MediaActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_textview);

        // Configure WebView
        TextView txtArea = (TextView) findViewById(R.id.playerTEXT);
        txtArea.setTypeface(this.defaultFont);
        player = new TextPlayer(this, txtArea );

        // Transfer first intent
        onNewIntent(getIntent());
    }


}
