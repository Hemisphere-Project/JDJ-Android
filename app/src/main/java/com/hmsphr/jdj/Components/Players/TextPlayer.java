package com.hmsphr.jdj.Components.Players;


import android.app.Activity;
import android.os.Handler;
import android.os.SystemClock;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

public class TextPlayer implements PlayerCompat {

    private Activity context;
    private TextView myTextView;
    private String mode;
    private String content;
    private FrameLayout loadShutter;

    public TextPlayer(Activity ctx, TextView view, FrameLayout shutter) {
        context = ctx;
        myTextView = view;
        loadShutter = shutter;

        //loadShutter.setVisibility(View.VISIBLE);
        loadShutter.setVisibility(View.GONE);
    }

    public void play() {
        play(content);
    }

    public void play(String txt) {
        play(txt, 0);
    }

    public void play(String txt, long atTime) {
        stop();
        content = txt;
        //myTextView.setText(content);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                //loadShutter.setVisibility(View.GONE);
                myTextView.setText(content);
            }
        }, Math.max(0, atTime - SystemClock.elapsedRealtime()));
    }

    public void stop() { hide(); }

    public void resume()
    {
        play();
    }

    public void pause() {
        stop();
    }

    public void hide() {
        //loadShutter.setVisibility(View.VISIBLE);
        myTextView.setText("");
    }

    // UNUSED INTERFACE
    public void setMode(String m) { mode = m; }
    public void setReplay(FrameLayout replayV, FrameLayout replayO, ImageButton replayB) {}

}
