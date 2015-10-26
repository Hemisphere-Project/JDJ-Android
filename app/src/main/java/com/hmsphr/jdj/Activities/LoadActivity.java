package com.hmsphr.jdj.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import com.hmsphr.jdj.Class.ManagedActivity;
import com.hmsphr.jdj.R;
import com.hmsphr.jdj.Services.Manager;

public class LoadActivity extends ManagedActivity {

    // Splash screen timer
    private final static int SPLASH_TIME_OUT = 1000;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_load);

        // Set Manager Mode
        manager.setMode(Manager.MODE_LOAD);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Switch to Welcome screen after Timeout
        (new Handler()).postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(getApplicationContext(), WelcomeActivity.class);
                startActivity(intent);
            }
        }, SPLASH_TIME_OUT);
    }

}
