package com.hmsphr.jdj.Components.Players;


import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;

public class WebPlayer implements PlayerCompat {

    private Activity context;
    private WebView myWebView;
    private String contentUri;
    private String mode;
    private FrameLayout loadShutter;

    public static final int STATE_STOP = 0;
    public static final int STATE_LOAD = 1;
    public static final int STATE_READY = 2;
    public static final int STATE_PLAY = 3;
    private int playerState = STATE_STOP;

    public WebPlayer(Activity ctx, WebView view, FrameLayout shutter) {

        context = ctx;
        myWebView = view;
        myWebView.getSettings().setJavaScriptEnabled(true);
        loadShutter = shutter;

        loadShutter.setVisibility(View.VISIBLE);

        // Disable touch
        /*myWebView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });*/
    }

    public void play() {
        play(contentUri);
    }

    public void play(String url) {
        play(url, 0);
    }

    public void play(String url, long atTime) {
        stop();

        contentUri = url;

        Log.d("jdj-WebPlayer", "Player LOADING");
        playerState = STATE_LOAD;
        //myWebView.clearCache(true);
        myWebView.setWebViewClient(new ProtectedViewClient(contentUri));
        myWebView.loadUrl(contentUri);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (playerState == STATE_STOP) return;

                else if (playerState >= STATE_READY) Log.d("jdj-WebPlayer", "Player PLAY - SYNC");
                else Log.d("jdj-WebPlayer", "Player PLAY - OUT OF SYNC.. (not ready yet)");

                loadShutter.setVisibility(View.GONE);
                playerState = STATE_PLAY;
            }
        }, Math.max(0, atTime - SystemClock.elapsedRealtime()));
    }

    public void stop() {
        loadShutter.setVisibility(View.VISIBLE);
        //myWebView.loadUrl("about:blank");
        myWebView.loadDataWithBaseURL(null, "<html><head></head><body bgcolor=\"black\"></body></html>", "text/html", "utf-8", null);
        playerState = STATE_STOP;
        Log.d("jdj-WebPlayer", "Player STOP");
    }


    public void resume()
    {
        play();
    }

    public void pause() {
        stop();
    }

    // UNUSED INTERFACE
    public void setMode(String m) { mode = m; }
    public void setReplay(FrameLayout replayV, FrameLayout replayO, ImageButton replayB) {}

    /** WEBCLIENT: disable navigation **/
    private class ProtectedViewClient extends WebViewClient {

        private String currentUrl;

        public ProtectedViewClient(String currentUrl){
            this.currentUrl = currentUrl;
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Log.d("jdj-WebPlayer", "trying to acces: "+url);

            if( url.startsWith("http:") || url.startsWith("https:") ) {
                return false;
            }

            // Otherwise allow the OS to handle things like tel, mailto, etc.
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            context.startActivity( intent );
            return true;

        }

        @Override
        public void onPageFinished(WebView view, String url) {
            Log.d("jdj-WebPlayer", "Player READY");
            if (playerState == STATE_LOAD) playerState = STATE_READY;
            else if (playerState == STATE_PLAY) loadShutter.setVisibility(View.GONE);
        }

    }

}
