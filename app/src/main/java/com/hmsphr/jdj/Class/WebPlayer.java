package com.hmsphr.jdj.Class;


import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.hmsphr.jdj.R;

public class WebPlayer implements PlayerCompat {

    private Activity context;
    private WebView myWebView;
    private String url;

    /** WEBCLIENT: disable foreigns domains **/
    private class ProtectedViewClient extends WebViewClient {
        /*@Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (Uri.parse(url).getHost().equals(getResources().getString(R.string.web_domain))) {
                // This is my web site, so do not override; let my WebView load the page
                return false;
            }
            // Otherwise, the link is not for a page on my site, do nothing..
            return true;
        }*/
    }

    public WebPlayer(Activity ctx, WebView view) {

        context = ctx;
        myWebView = view;

        // Prevent external domains
        myWebView.setWebViewClient(new ProtectedViewClient());

        // Enable Javascript
        WebSettings webSettings = myWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);

        // Disable touch
        myWebView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });
    }

    @Override
    public void play(String url)
    {
        //myWebView.clearCache(true);
        myWebView.loadUrl(url);
        resume();
    }

    @Override
    public void resume()
    {
        myWebView.setVisibility(View.VISIBLE);
        context.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    @Override
    public void pause()
    {
        // Do nothing
    }

    @Override
    public void stop() {
        hide();
    }

    @Override
    public void hide() {
        myWebView.setVisibility(View.GONE);
    }

}
