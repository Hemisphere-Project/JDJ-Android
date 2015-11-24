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
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Set Manager Mode
        manager.setMode(Manager.MODE_WELCOME);
    }

    // GO button
    public void startPlayer(View view) {
        // Do something in response to button
        Intent mpdIntent = new Intent(this, VideoActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra("action", "wait");
        startActivity(mpdIntent);
    }

}
