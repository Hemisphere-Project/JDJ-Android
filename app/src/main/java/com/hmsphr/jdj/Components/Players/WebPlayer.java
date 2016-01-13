package com.hmsphr.jdj.Components.Players;


import android.app.Activity;
import android.content.pm.ActivityInfo;
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

    /** WEBCLIENT: disable navigation **/
    private class ProtectedViewClient extends WebViewClient {

        private String currentUrl;

        public ProtectedViewClient(String currentUrl){
            this.currentUrl = currentUrl;
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Log.d("jdj-WebPlayer", "trying to acces: "+url);
            return (!url.equals(currentUrl));
        }
    }

    public WebPlayer(Activity ctx, WebView view) {

        context = ctx;
        myWebView = view;
        myWebView.getSettings().setJavaScriptEnabled(true);

        // Disable touch
        /*myWebView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });*/
    }

    public void load(String url) {
        contentUri = url;
    }

    public void play()
    {
        //myWebView.clearCache(true);
        myWebView.setWebViewClient(new ProtectedViewClient(contentUri));
        myWebView.loadUrl(contentUri);
        myWebView.setVisibility(View.VISIBLE);
        context.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }

    public void stop() {
        hide();
        //myWebView.loadUrl("about:blank");
        myWebView.loadDataWithBaseURL(null, "<html><head></head><body bgcolor=\"black\"></body></html>", "text/html", "utf-8", null);
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
        myWebView.setVisibility(View.GONE);
    }

    // UNUSED INTERFACE
    public void setAudioShutter(ImageView audioS) {}
    public void showAudioShutter() {}
    public void hideAudioShutter() {}
    public void setReplayMenu(FrameLayout replayV, FrameLayout replayO, ImageButton replayB) {}
    public void enableReplay() {}
    public void disableReplay() {}

}
