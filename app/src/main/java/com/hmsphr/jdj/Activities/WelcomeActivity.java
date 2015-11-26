package com.hmsphr.jdj.Activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
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

    @Override
    protected void onNewIntent(Intent intent) {

        // Version expired
        if (intent.hasExtra("broken_version")) {
            Log.d("WELCOME-activity", "BROKEN VERSION !");

            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

            alertDialogBuilder.setTitle("Version expirée :'(");
            alertDialogBuilder
                    .setMessage(Html.fromHtml("<font color='#EEEEEE'><b>Vous utilisez une version de l'application qui n'est plus compatible avec le spectacle.</b><br /><br />"
                            + "Merci de mettre à jour votre application via Google Play ou en la réinstallant manuellement.<br /><br />A bientôt !</font>"))
                    .setCancelable(false)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            manager.stopApp();
                        }
                    });

            // create alert dialog
            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();
        }

        // Update Available
        else if (intent.hasExtra("update_available")) {
            Log.d("WELCOME-activity", "BROKEN VERSION !");

            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

            // TODO: Bouton de redirection vers Google Play

            alertDialogBuilder.setTitle("Mise à jour disponible !");
            alertDialogBuilder
                    .setMessage(Html.fromHtml("<font color='#EEEEEE'><b>Une mise à jour est disponible pour l'application.</b><br /><br />"
                            + "Vous pouvez l'appliquer depuis Google Play ou en la téléchargeant manuellement.<br /><br />Bon spectacle !</font>"))
                    .setCancelable(false)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {

                        }
                    });

            // create alert dialog
            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();
        }



    }

}
