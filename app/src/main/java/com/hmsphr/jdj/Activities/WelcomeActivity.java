package com.hmsphr.jdj.Activities;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.hmsphr.jdj.Class.ManagedActivity;
import com.hmsphr.jdj.Class.Utils.Show;
import com.hmsphr.jdj.Class.Utils.ShowList;
import com.hmsphr.jdj.Components.RemoteControl;
import com.hmsphr.jdj.R;
import com.hmsphr.jdj.Services.Manager;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class WelcomeActivity extends ManagedActivity {

    protected static Class myClass = WelcomeActivity.class;

    protected FrameLayout dialogBox;
    protected FrameLayout registerBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_welcome);

        MODE = Manager.MODE_WELCOME;

        // Dialog Handler
        dialogBox = (FrameLayout) findViewById(R.id.dialogBox);
        dialogBox.setVisibility(View.GONE);

        // Register Handler
        registerBox = (FrameLayout) findViewById(R.id.registerBox);
        registerBox.setVisibility(View.GONE);

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
        text.setTypeface(this.defaultFont);
        //Log.d("WelcomeActivity", "State: "+State);

        if (State == Manager.STATE_INIT) {
            text.setText(R.string.welcome_connecting);
        }
        else if (State == Manager.STATE_NONET) {
            text.setText(R.string.welcome_nonetwork);
        }
        else if (State == Manager.STATE_NOSERV) {
            text.setText(R.string.welcome_noserver);
        }
        else if (State == Manager.STATE_NOUSER) {
            text.setText(R.string.welcome_nouser);
            alertRegister();
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

        // Get Widgets
        Button dialogOK = (Button) findViewById(R.id.dialogOK);
        TextView dialogTitle = (TextView) findViewById(R.id.dialogTitle);
        dialogTitle.setTypeface(this.defaultFont);
        TextView dialogText = (TextView) findViewById(R.id.dialogText);
        dialogText.setTypeface(this.defaultFont);

        // Set Alpha Background (API 10 compat)
        FrameLayout dialogOverlay = (FrameLayout) findViewById(R.id.dialogOverlay);
        AlphaAnimation animation = new AlphaAnimation(0.6f, 0.6f);
        animation.setDuration(0);
        animation.setFillAfter(true);
        dialogOverlay.startAnimation(animation);

        if (majorBreak) {
            dialogTitle.setText(getResources().getString(R.string.expired_title));
            dialogText.setText(Html.fromHtml("<b>"+getResources().getString(R.string.expired_text1)+"<br /><br />"
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
            dialogText.setText(Html.fromHtml("<b>"+getResources().getString(R.string.updatable_text1)+"<br /><br />"
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

    void alertRegister() {

        // Get Widgets
        final EditText registerPhone = (EditText) findViewById(R.id.registerPhone);
        final Spinner registerShow = (Spinner) findViewById(R.id.registerShow);
        Button registerOK = (Button) findViewById(R.id.registerOK);

        // Set Alpha Background (API 10 compat)
        FrameLayout registerOverlay = (FrameLayout) findViewById(R.id.registerOverlay);
        AlphaAnimation animation = new AlphaAnimation(0.6f, 0.6f);
        animation.setDuration(0);
        animation.setFillAfter(true);
        registerOverlay.startAnimation(animation);

        // pre-fill phone number
        String mPhoneNumber = "";
        try {
            TelephonyManager tMgr = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
            mPhoneNumber = tMgr.getLine1Number().replace("+33","0");
        } catch (SecurityException e) {}
        String phone = settings().getString("com.hmsphr.jdj.phone", mPhoneNumber);
        registerPhone.setText(phone);

        // populate show list
        Gson gson = new Gson();
        String showlist_import = settings().getString("com.hmsphr.jdj.show_list", null);
        final ShowList showlist = new ShowList();
        showlist.inflate(showlist_import);
        Show myShow = Show.inflate( settings().getString("com.hmsphr.jdj.show", null) );

        Spinner spinner = (Spinner) findViewById(R.id.registerShow);
        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(
                this, R.layout.spinner_item, showlist.itemsList());
        spinnerArrayAdapter.setDropDownViewResource( R.layout.spinner_dropdown_item );
        spinner.setAdapter(spinnerArrayAdapter);
        if (myShow != null) spinner.setSelection(spinnerArrayAdapter.getPosition( myShow.label() ));


        // display error
        String error = settings().getString("com.hmsphr.jdj.error_user", "");
        TextView disclaimer = (TextView) findViewById(R.id.registerText3);
        disclaimer.setTypeface(this.defaultFont);
        if (error.equals("")) {
            disclaimer.setText(getResources().getString(R.string.register_text3));
            disclaimer.setTextColor(Color.WHITE);
        }
        else {
            disclaimer.setText(error);
            disclaimer.setTextColor(Color.RED);
        }

        registerOK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: Check phone number validity !

                // FIND SHOW
                Show myShow = showlist.find( registerShow.getSelectedItem().toString() );

                // SAVE Locally
                settings().edit().putString("com.hmsphr.jdj.phone", registerPhone.getText().toString())
                    .putString("com.hmsphr.jdj.show", myShow.export())
                    .commit();


                // HIDE Box
                registerBox.setVisibility(View.GONE);

                // SEND to server
                mail("do_register").to(Manager.class).send();
            }
        });

        // Set FONT
        ((TextView) findViewById(R.id.registerText1)).setTypeface(defaultFont);
        ((TextView) findViewById(R.id.registerText2)).setTypeface(defaultFont);
        ((TextView) findViewById(R.id.registerText3)).setTypeface(defaultFont);
        ((TextView) findViewById(R.id.registerTitle)).setTypeface(defaultFont);

        registerBox.setVisibility(View.VISIBLE);
    }

}
