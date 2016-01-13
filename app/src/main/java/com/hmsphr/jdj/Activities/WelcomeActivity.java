package com.hmsphr.jdj.Activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.hmsphr.jdj.Class.ManagedActivity;
import com.hmsphr.jdj.R;
import com.hmsphr.jdj.Services.Manager;

public class WelcomeActivity extends ManagedActivity {

    protected static Class myClass = WelcomeActivity.class;

    protected FrameLayout dialogBox;
    protected FrameLayout dialogOverlay;
    protected Button dialogOK;
    protected TextView dialogTitle;
    protected TextView dialogText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_welcome);

        MODE = Manager.MODE_WELCOME;

        // Dialog Handlers
        dialogBox = (FrameLayout) findViewById(R.id.dialogBox);
        dialogOverlay = (FrameLayout) findViewById(R.id.dialogOverlay);
        dialogOK = (Button) findViewById(R.id.dialogOK);
        dialogTitle = (TextView) findViewById(R.id.dialogTitle);
        dialogText = (TextView) findViewById(R.id.dialogText);

        // Hide Dialog box
        dialogBox.setVisibility(View.GONE);

        // Set Alpha Background (API 10 compat)
        AlphaAnimation animation = new AlphaAnimation(0.6f, 0.6f);
        animation.setDuration(0);
        animation.setFillAfter(true);
        dialogOverlay.startAnimation(animation);

    }

    @Override
    protected void onNewIntent(Intent intent) {

        //debug("Intent received @WELCOME: "+intent.getStringExtra("message"));

        if (intent.hasExtra("message")) {
            String msg = intent.getStringExtra("message");

            // New STATE
            if (msg.equals("update_state")) updateState(intent.getIntExtra("state", -1));

            // Version expired
            else if (msg.equals("broken_version")) alertExpired(intent.getBooleanExtra("major", false));
        }

    }

    void updateState(int State) {

        TextView text = (TextView) findViewById(R.id.welcomeText);
        //Log.d("WelcomeActivity", "State: "+State);

        if (State == Manager.STATE_INIT) {
            text.setText(R.string.welcome_connecting);
        }
        else if (State == Manager.STATE_NONET) {
            text.setText(R.string.welcome_nonetwork);
        }
        else if (State == Manager.STATE_SHOWPAST) {
            text.setText(R.string.welcome_showpast);
        }
        else if (State == Manager.STATE_SHOWFUTURE) {
            text.setText(R.string.welcome_showfuture);
        }
        else if (State == Manager.STATE_SHOWTIME) {
            text.setText(R.string.welcome_showtime);
        }
        else {
            text.setText("...");
        }
    }

    void alertExpired(boolean majorBreak) {

        Log.d("WELCOME-activity", "BROKEN VERSION !");
        // TODO: Bouton de redirection vers Google Play


        if (majorBreak) {
            dialogTitle.setText(getResources().getString(R.string.expired_title));
            dialogText.setText(Html.fromHtml("<font color='#EEEEEE'><b>"+getResources().getString(R.string.expired_text1)+"</b><br /><br />"
                    +getResources().getString(R.string.expired_text2)+"<br /><br />"
                    +getResources().getString(R.string.expired_bye)+"</font>"));
            dialogOK.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mail("application_stop").to(Manager.class).send();
                }
            });
        }
        else {
            dialogTitle.setText(getResources().getString(R.string.updatable_title));
            dialogText.setText(Html.fromHtml("<font color='#EEEEEE'><b>"+getResources().getString(R.string.updatable_text1)+"</b><br /><br />"
                    +getResources().getString(R.string.updatable_text2)+"<br /><br />"
                    +getResources().getString(R.string.updatable_bye)+"</font>"));
            dialogOK.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialogBox.setVisibility(View.GONE);
                }
            });
        }

        dialogBox.setVisibility(View.VISIBLE);

    }

}
