package com.rocketmotorteststand;
/**
 * @description: Allow the user to reset the altimeter setting to factory default
 * as well as clearing the flight list
 * @author: boris.dureau@neuf.fr
 **/

import android.app.AlertDialog;
import android.content.DialogInterface;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.rocketmotorteststand.Help.AboutActivity;
import com.rocketmotorteststand.Help.HelpActivity;

public class ResetSettingsActivity extends AppCompatActivity {

    Button btnClearTestStandConfig, btnClearThrustCurves, btnClearLastThrustCurve, btnDismiss;
    ConsoleApplication myBT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //get the bluetooth Application pointer
        myBT = (ConsoleApplication) getApplication();
        //Check the local and force it if needed
        //getApplicationContext().getResources().updateConfiguration(myBT.getAppLocal(), null);
        setContentView(R.layout.activity_reset_settings);


        btnDismiss = (Button) findViewById(R.id.butDismiss);
        btnDismiss.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();      //exit the application configuration activity
            }
        });

        btnClearTestStandConfig = (Button) findViewById(R.id.butRestoreTestStandCfg);
        btnClearTestStandConfig.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearTestStandConfig();
            }
        });
        btnClearThrustCurves = (Button) findViewById(R.id.butClearThrustCurves);
        btnClearThrustCurves.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearThrustCurves();

            }
        });
        btnClearLastThrustCurve = (Button) findViewById(R.id.butDeleteLastCurve);
        btnClearLastThrustCurve.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearLastThrustCurves();

            }
        });

    }

    public void clearThrustCurves() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        //You are about to erase all flight data, are you sure you want to do it?
        builder.setMessage(getResources().getString(R.string.reset_msg1))
                .setCancelable(false)
                .setPositiveButton(getResources().getString(R.string.Yes), new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        dialog.cancel();
                        //clear altimeter config
                        if (myBT.getConnected()) {
                            //erase the config
                            myBT.write("e;".toString());
                            myBT.flush();
                        }

                    }
                })
                .setNegativeButton(getResources().getString(R.string.No), new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        dialog.cancel();

                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    public void clearTestStandConfig() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        //You are about to reset your altimeter config, are you sure you want to do it?
        builder.setMessage(getResources().getString(R.string.reset_msg2))
                .setCancelable(false)
                .setPositiveButton(getResources().getString(R.string.Yes), new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        dialog.cancel();
                        //clear altimeter config
                        if (myBT.getConnected()) {
                            //erase the config
                            myBT.write("d;".toString());
                            myBT.flush();
                        }

                    }
                })
                .setNegativeButton(getResources().getString(R.string.No), new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        dialog.cancel();

                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    public void clearLastThrustCurves() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        //You are about to erase your last flight data, are you sure you want to do it?
        builder.setMessage(getResources().getString(R.string.reset_msg3))
                .setCancelable(false)
                .setPositiveButton(getResources().getString(R.string.Yes), new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        dialog.cancel();
                        //clear altimeter config
                        if (myBT.getConnected())
                            //erase the config
                            myBT.write("x;".toString());
                        myBT.flush();

                    }
                })
                .setNegativeButton(getResources().getString(R.string.No), new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        dialog.cancel();

                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_application_config, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //share screen
        if (id == R.id.action_share) {
            ShareHandler.takeScreenShot(findViewById(android.R.id.content).getRootView(), this);
            return true;
        }

        //open help screen
        if (id == R.id.action_help) {
            Intent i = new Intent(ResetSettingsActivity.this, HelpActivity.class);
            i.putExtra("help_file", "help_telemetry");
            startActivity(i);
            return true;
        }

        if (id == R.id.action_about) {
            Intent i = new Intent(ResetSettingsActivity.this, AboutActivity.class);
            startActivity(i);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
