package com.hmsphr.jdj.Activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
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
import com.hmsphr.jdj.BuildConfig;
import com.hmsphr.jdj.Class.ManagedActivity;
import com.hmsphr.jdj.Class.Utils.Show;
import com.hmsphr.jdj.Class.Utils.ShowList;
import com.hmsphr.jdj.R;
import com.hmsphr.jdj.Services.Manager;

public class WelcomeActivity extends ManagedActivity {

    protected static Class myClass = WelcomeActivity.class;

    protected FrameLayout updateBox;
    protected FrameLayout registerBox;
    protected FrameLayout updateBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_welcome);

        MODE = Manager.MODE_WELCOME;

        // Update Handler
        updateBox = (FrameLayout) findViewById(R.id.updateBox);
        updateBox.setVisibility(View.GONE);

        // Register Handler
        registerBox = (FrameLayout) findViewById(R.id.registerBox);
        registerBox.setVisibility(View.GONE);

        // Update Handler
        updateBar = (FrameLayout) findViewById(R.id.updateBar);
        updateBar.setVisibility(View.GONE);

        // Set version number
        ((TextView) findViewById(R.id.versionText)).setText(BuildConfig.VERSION_NAME);

        // Exit Button
        Button menuExit = (Button) findViewById(R.id.menuExit);
        menuExit.setTypeface(this.defaultFont);
        menuExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mail("application_standby").to(Manager.class).send();
            }
        });

        // Info Button
        Button menuInfo = (Button) findViewById(R.id.menuInfo);
        menuInfo.setTypeface(this.defaultFont);
        menuInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                displayInfo();
            }
        });

        // Settings Button
        Button menuSettings = (Button) findViewById(R.id.menuSettings);
        menuSettings.setTypeface(this.defaultFont);
        menuSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertRegister();
            }
        });

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

    void displayInfo() {
        // TODO : show INFO Page
    }

    void alertExpired(final boolean majorBreak) {

        Log.d("WELCOME-activity", "BROKEN VERSION !");

        // Update Bar
        updateBar.setVisibility(View.GONE);
        updateBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertExpired(majorBreak);
            }
        });

        // Go to Google Play Button
        Button updateGO = (Button) findViewById(R.id.updateGO);
        updateGO.setTypeface(this.defaultFont);
        updateGO.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateBar.setVisibility(View.VISIBLE);
                updateBox.setVisibility(View.GONE);
                final String appPackageName = getPackageName(); // getPackageName() from Context or Activity object

                if (majorBreak) mail("application_stop").to(Manager.class).send();
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
                } catch (android.content.ActivityNotFoundException anfe) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
                }
                if (majorBreak) finish();
            }
        });

        // Cancel Button
        Button updateCANCEL = (Button) findViewById(R.id.updateCANCEL);
        updateCANCEL.setTypeface(this.defaultFont);
        updateCANCEL.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateBar.setVisibility(View.VISIBLE);
                updateBox.setVisibility(View.GONE);
            }
        });
        if (majorBreak) updateCANCEL.setVisibility(View.GONE);
        else updateCANCEL.setVisibility(View.VISIBLE);

        // Fonts
        TextView updateTitle = (TextView) findViewById(R.id.updateTitle);
        updateTitle.setTypeface(this.defaultFont);
        TextView updateText = (TextView) findViewById(R.id.updateText);
        updateText.setTypeface(this.defaultFont);
        TextView updateBarText = (TextView) findViewById(R.id.updateBarText);
        updateBarText.setTypeface(this.defaultFont);

        // Set Alpha Background (API 10 compat)
        FrameLayout dialogOverlay = (FrameLayout) findViewById(R.id.updateOverlay);
        AlphaAnimation animation = new AlphaAnimation(0.6f, 0.6f);
        animation.setDuration(0);
        animation.setFillAfter(true);
        dialogOverlay.startAnimation(animation);


        //mail("application_stop").to(Manager.class).send();

        updateBox.setVisibility(View.VISIBLE);
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
