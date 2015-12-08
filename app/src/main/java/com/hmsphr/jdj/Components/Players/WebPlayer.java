package com.hmsphr.jdj.Components.Players;


import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class WebPlayer implements PlayerCompat {

    private Activity context;
    private WebView myWebView;
    private String contentUri;

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

        //myWebView.getSettings().setLoadWithOverviewMode(true);
        //myWebView.getSettings().setUseWideViewPort(true);


        // Prevent external domains
        myWebView.setWebViewClient(new WebViewClient());

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

    public void load(String url) {
        contentUri = url;
    }

    public void play()
    {
        //myWebView.clearCache(true);
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

}
