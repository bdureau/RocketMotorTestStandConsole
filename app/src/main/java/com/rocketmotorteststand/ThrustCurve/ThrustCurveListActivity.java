package com.rocketmotorteststand.ThrustCurve;
/**
 * @description: This retrieve the flight list from the teststand and store it in a
 * FlightData instance
 * @author: boris.dureau@neuf.fr
 **/

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.rocketmotorteststand.ConsoleApplication;
import com.rocketmotorteststand.R;

import org.afree.data.xy.XYSeries;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static java.lang.Math.abs;


public class ThrustCurveListActivity extends AppCompatActivity {
    public static String SELECTED_THRUSTCURVE = "myThrustCurve";

    ListView thrustCurveList = null;
    ConsoleApplication myBT;
    List<String> thrustCurveNames = null;
    private ThrustCurveData myThrustCurve = null;
    private AlertDialog alert;

    private Button buttonDismiss;

    private AdapterView.OnItemClickListener myListClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            // Get the thrust curve name
            String currentThrustCurve = ((TextView) v).getText().toString();
            Intent i;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O){
                //if android ver = 8 or greater use the MPlib
                i = new Intent(ThrustCurveListActivity.this, ThrustCurveViewTabActivity.class);
            } else {
                // Make an intent to start next activity.
                if (myBT.getAppConf().getGraphicsLibType().equals("0"))
                    i = new Intent(ThrustCurveListActivity.this, ThrustCurveViewActivity.class);
                else
                    i = new Intent(ThrustCurveListActivity.this, ThrustCurveViewTabActivity.class);
            }
            //Change the activity.
            i.putExtra(SELECTED_THRUSTCURVE, currentThrustCurve);
            startActivity(i);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //get the bluetooth Application pointer
        myBT = (ConsoleApplication) getApplication();
        //Check the local and force it if needed
        //getApplicationContext().getResources().updateConfiguration(myBT.getAppLocal(), null);

        setContentView(R.layout.activity_thrustcurve_list);
        buttonDismiss = (Button) findViewById(R.id.butDismiss);
        new RetrieveThrustCurves().execute();
        buttonDismiss.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();      //exit the activity
            }
        });
    }


    private void msg(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
    }


    private class RetrieveThrustCurves extends AsyncTask<Void, Void, Void>  // UI thread
    {
        private AlertDialog.Builder builder = null;

        private Boolean canceled = false;
        private String myMessage = "";
        private int NbrOfCurves = 0;

        @Override
        protected void onPreExecute() {


            builder = new AlertDialog.Builder(ThrustCurveListActivity.this);
            //Retrieving flights...
            builder.setMessage(getResources().getString(R.string.msg7))
                    .setTitle(getResources().getString(R.string.msg8))
                    .setCancelable(false)
                    .setNegativeButton(getResources().getString(R.string.flight_list_cancel), new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface dialog, final int id) {
                            myBT.setExit(true);
                            canceled = true;
                            dialog.cancel();
                        }
                    });
            alert = builder.create();
            alert.show();
        }

        @Override
        protected Void doInBackground(Void... devices) //while the progress dialog is shown, the connection is done in background
        {
            //get flights
            if (myBT.getConnected()) {
                //clear anything on the connection
                myBT.flush();
                myBT.clearInput();
                myBT.setNbrOfThrustCurves(0);
                myBT.getThrustCurveData().ClearThrustCurve();
                // Send command to retrieve the config

                myBT.write("n;".toString());
                myBT.flush();

                try {
                    //wait for data to arrive
                    while (myBT.getInputStream().available() <= 0) ;
                } catch (IOException e) {
                    // msg("Failed to retrieve flights");
                }

                myBT.setDataReady(false);
                myBT.initThrustCurveData();

                myMessage = myBT.ReadResult(60000);

                if (myMessage.equals("start nbrOfThrustCurve end")) {
                    NbrOfCurves = myBT.getNbrOfThrustCurves();
                }

                if (NbrOfCurves > 0) {

                    for (int j = 0; j < NbrOfCurves; j++) {
                        dialogAppend(getString(R.string.retrieving_thrust_curve) + (j + 1));
                        Log.d("FlightList", "Thrust curve:" + j);
                        myBT.flush();
                        myBT.clearInput();

                        myBT.write("r" + j + ";".toString());
                        myBT.flush();


                        try {
                            //wait for data to arrive
                            while (myBT.getInputStream().available() <= 0) ;
                        } catch (IOException e) {
                            // msg("Failed to retrieve Thrust curve");
                        }
                        myMessage = "";
                        myBT.setDataReady(false);

                        myMessage = myBT.ReadResult(60000);

                        if (myMessage.equals("start end")) {

                        }

                        if (canceled) {
                            Log.d("Thrust curve List", "Canceled retrieval");
                            j = NbrOfCurves;
                        }
                    }
                }
                Log.d("Thrust curve List",  "ready?" +myBT.getDataReady());

                Log.d("Thrust curve List", "myMessage:"+ myMessage);
                thrustCurveNames = new ArrayList<String>();

                myThrustCurve = myBT.getThrustCurveData();
                thrustCurveNames = myThrustCurve.getAllThrustCurveNames2();
                if (canceled) {
                    //order the names in the collection
                    Collections.sort(thrustCurveNames);
                    //remove the last Thrust curve which might have incomplete data
                    thrustCurveNames.remove(thrustCurveNames.size() - 1);
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) //after the doInBackground, it checks if everything went fine
        {
            super.onPostExecute(result);

            final ArrayAdapter adapter = new ArrayAdapter(ThrustCurveListActivity.this, android.R.layout.simple_list_item_1, thrustCurveNames);
            adapter.sort(new Comparator<String>() {
                public int compare(String object1, String object2) {
                    return object1.compareTo(object2);
                }
            });

            thrustCurveList = (ListView) findViewById(R.id.listViewFlightList);
            thrustCurveList.setAdapter(adapter);
            thrustCurveList.setOnItemClickListener(myListClickListener);


            alert.dismiss();

            if (canceled)
                msg(getResources().getString(R.string.flight_retrieval_canceled));

            if (myThrustCurve == null && !canceled)
                msg(getResources().getString(R.string.flight_have_been_recorded));
        }
    }

    Handler mHandler = new Handler();

    private void dialogAppend(CharSequence text) {
        final CharSequence ftext = text;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                alert.setMessage(ftext);
            }
        });
    }
}
