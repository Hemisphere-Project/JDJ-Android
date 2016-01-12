package com.hmsphr.jdj.Activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.widget.TextView;

import com.hmsphr.jdj.Class.ManagedActivity;
import com.hmsphr.jdj.R;
import com.hmsphr.jdj.Services.Manager;

public class WelcomeActivity extends ManagedActivity {

    protected static Class myClass = WelcomeActivity.class;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_welcome);

        MODE = Manager.MODE_WELCOME;
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

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setCancelable(false);

        // Version expired
        if (majorBreak) {
            alertDialogBuilder.setTitle(R.string.expired_title);
            alertDialogBuilder
                    .setMessage(Html.fromHtml("<font color='#EEEEEE'><b>"+R.string.expired_text1+"</b><br /><br />"
                            +R.string.expired_text2+"<br /><br />"
                            +R.string.expired_bye+"</font>"))
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            mail("application_stop").to(Manager.class).send();
                        }
                    });


        }
        // Update Available
        else {
            // TODO: Bouton de redirection vers Google Play
            alertDialogBuilder.setTitle(R.string.updatable_title);
            alertDialogBuilder
                    .setMessage(Html.fromHtml("<font color='#EEEEEE'><b>"+R.string.updatable_text1+"</b><br /><br />"
                            +R.string.updatable_text2+"<br /><br />"
                            +R.string.updatable_bye+"</font>"))
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {  }
                    });
        }

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();

    }

}
