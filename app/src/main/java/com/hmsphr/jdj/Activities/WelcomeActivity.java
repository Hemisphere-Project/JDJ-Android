package com.hmsphr.jdj.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.hmsphr.jdj.Class.ManagedActivity;
import com.hmsphr.jdj.R;
import com.hmsphr.jdj.Services.Manager;

public class WelcomeActivity extends ManagedActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        // Set Manager Mode
        manager.setMode(Manager.MODE_WELCOME);
    }

    // GO button
    public void startPlayer(View view) {
        // Do something in response to button
        Intent intent = new Intent(this, PlayerActivity.class);
        startActivity(intent);
    }

}
