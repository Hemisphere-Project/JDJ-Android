package com.hmsphr.jdj.Activities;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.hmsphr.jdj.Class.Mailbox;
import com.hmsphr.jdj.Class.ManagedActivity;
import com.hmsphr.jdj.R;
import com.hmsphr.jdj.Services.Manager;

import java.util.Iterator;
import java.util.Set;

public class WelcomeActivity extends ManagedActivity {

    protected static Class myClass = WelcomeActivity.class;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        MODE = Manager.MODE_WELCOME;
    }

    @Override
    protected void onNewIntent(Intent intent) {

        debug("Intent received @WELCOME: "+intent.getStringExtra("message"));

        if (intent.hasExtra("message")) {
            String msg = intent.getStringExtra("message");

            // New STATE
            if (msg.equals("update_state"))  updateState(intent.getIntExtra("state", -1));

            // Version expired
                else if (msg.equals("broken_version")) {
                Log.d("WELCOME-activity", "BROKEN VERSION !");

                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

                alertDialogBuilder.setTitle("Version expirée :'(");
                alertDialogBuilder
                        .setMessage(Html.fromHtml("<font color='#EEEEEE'><b>Vous utilisez une version de l'application qui n'est plus compatible avec le spectacle.</b><br /><br />"
                                + "Merci de mettre à jour votre application via Google Play ou en la réinstallant manuellement.<br /><br />A bientôt !</font>"))
                        .setCancelable(false)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                mail("application_stop").to(Manager.class).send();
                            }
                        });

                // create alert dialog
                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();
            }

            // Update Available
            else if (msg.equals("update_available")) {
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

    void updateState(int State) {

        TextView title = (TextView) findViewById(R.id.welcomeTitle);
        TextView text = (TextView) findViewById(R.id.welcomeText);

        if (State == Manager.STATE_INIT) {
            title.setText("Bienvenue !");
            text.setText("Connection au serveur de Spectacle en cours..");
        }
        else if (State == Manager.STATE_NONET) {
            title.setText("Aucune connexion au réseau..");
            text.setText("Vérifiez que vous êtes connecté à internet via Wifi ou 3G/4G !");
        }
        else if (State == Manager.STATE_SHOWPAST) {
            title.setText("Le spectacle est terminé..");
            text.setText("Merci de nous avoir suivi ! Et à bientôt pour de nouvelles aventures...");
        }
        else if (State == Manager.STATE_SHOWFUTURE) {
            title.setText("Le spectacle commencera bientôt !");
            text.setText("Vous serez alerté en direct des évènements à venir..");
        }
        else if (State == Manager.STATE_SHOWTIME) {
            title.setText("Bienvenue !");
            text.setText("Le spectacle est en cours, restez connecté !");
        }
        else {
            title.setText("Bienvenue !");
            text.setText("");
        }
    }

}
