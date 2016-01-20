package com.hmsphr.jdj.Components.Players;


import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

public class TextPlayer implements PlayerCompat {

    private Activity context;
    private TextView myTextView;
    private String content;

    /** WEBCLIENT: disable navigation **/
    private class ProtectedViewClient extends WebViewClient {

    }

    public TextPlayer(Activity ctx, TextView view) {

        context = ctx;
        myTextView = view;
    }

    public void load(String txt) {
        myTextView.setText(txt);
    }

    public void play() {
        myTextView.setVisibility(View.VISIBLE);
    }

    public void stop() {
        hide();
    }


    public void resume()
    {
        play();
    }

    public void pause()
    {
        stop();
    }

    public void hide() {
        myTextView.setVisibility(View.GONE);
    }

    // UNUSED INTERFACE
    public void setAudioShutter(ImageView audioS) {}
    public void showAudioShutter() {}
    public void hideAudioShutter() {}
    public void setReplayMenu(FrameLayout replayV, FrameLayout replayO, ImageButton replayB) {}
    public void enableReplay() {}
    public void disableReplay() {}

}
