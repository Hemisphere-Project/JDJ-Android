package com.hmsphr.jdj;

import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.hmsphr.jdj.Class.ManagerConnector;
import com.hmsphr.jdj.Services.Manager;

public class LoadActivity extends AppCompatActivity {

    // Splash screen timer
    private final static int SPLASH_TIME_OUT = 3000;

    // Manager link
    private ManagerConnector managerLink = new ManagerConnector();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //set content view AFTER ABOVE sequence (to avoid crash)
        this.setContentView(R.layout.activity_load);

        // Fullscreen
        if (Build.VERSION.SDK_INT>10) {
            // Hide both the navigation bar and the status bar.
            View decorView = getWindow().getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_FULLSCREEN;
            decorView.setSystemUiVisibility(uiOptions);
        }

        // Start Manager
        Manager.start(this);

    }

    @Override
    protected void onStart() {
        super.onStart();

        // Bind to Manager
        managerLink.connect(this, Manager.MODE_LOAD);
    }

    @Override
    protected void onStop() {
        super.onStop();

        // unBind to Manager
        managerLink.disconnect(this);
    }

}
