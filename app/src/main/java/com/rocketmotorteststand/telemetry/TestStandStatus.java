package com.rocketmotorteststand.telemetry;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.rocketmotorteststand.ConsoleApplication;
import com.rocketmotorteststand.GlobalConfig;
import com.rocketmotorteststand.Help.AboutActivity;
import com.rocketmotorteststand.Help.HelpActivity;
import com.rocketmotorteststand.R;
import com.rocketmotorteststand.ShareHandler;

import java.io.IOException;

public class TestStandStatus extends AppCompatActivity {
    Button btnDismiss, btnRecording, btnTare;
    ConsoleApplication myBT;
    Thread testStandStatus;
    boolean status = true;
    boolean recording = false;

    private TextView txtViewBatteryVoltage, txtViewCurrentPressure;
    private TextView txtViewThrust, txtViewVoltage, txtViewLink, txtEEpromUsage, txtNbrOfThrustCurve;
    private TextView txtViewEEprom, txtViewThrustCurve,txtViewCurrentPressureValue;


    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {

                case 1:
                    String myUnits = "";
                    double CONVERT = 1.0;
                    //Value 1 contains the current thrust
                    if (myBT.getAppConf().getUnits()== GlobalConfig.ThrustUnits.KG) {
                        //kg
                        myUnits = getResources().getString(R.string.Kg_fview);
                        CONVERT = 1.0;
                    }
                    else if (myBT.getAppConf().getUnits()== GlobalConfig.ThrustUnits.POUNDS) {
                        //pounds
                        myUnits = getResources().getString(R.string.Pounds_fview);
                        CONVERT = 2.20462;
                    }
                    else if (myBT.getAppConf().getUnits()== GlobalConfig.ThrustUnits.NEWTONS) {
                        //Newtons
                        myUnits = getResources().getString(R.string.unit_newtons);
                        CONVERT = 9.80665;
                    }
                    String thrust = (String) msg.obj;
                    if (thrust.matches("\\d+(?:\\.\\d+)?")) {
                        double temp;
                        temp = Double.parseDouble(thrust);
                        /*if (myBT.getAppConf().getUnits()== GlobalConfig.ThrustUnits.KG)
                            //kg
                            thrust = String.format("%.2f", (temp / 1000));
                        else if (myBT.getAppConf().getUnits()== GlobalConfig.ThrustUnits.POUNDS)
                            //pounds
                            thrust = String.format("%.2f", (temp / 1000) * 2.20462);
                        else if (myBT.getAppConf().getUnits()== GlobalConfig.ThrustUnits.NEWTONS)
                            //Newtons
                            thrust = String.format("%.2f", (temp / 1000) * 9.80665);*/
                        thrust = String.format("%.2f", (temp / 1000) * CONVERT);
                    }
                    txtViewThrust.setText(thrust + " " + myUnits);
                    break;
                case 3:
                    //Value 4 contains the battery voltage
                    String voltage = (String) msg.obj;
                    if (voltage.matches("\\d+(?:\\.\\d+)?")) {

                        txtViewVoltage.setText(voltage + " Volts");
                        if(Double.parseDouble(voltage)< 7.0){
                            txtViewVoltage.setTextColor(Color.RED);
                        }
                        else {
                            txtViewVoltage.setTextColor(txtViewThrust.getTextColors());
                        }
                    } else {
                        txtViewVoltage.setText("NA");
                    }
                    break;


                case 4:
                    //Value 4 contains the EEprom usage
                    String eepromUsage = (String) msg.obj;
                    txtEEpromUsage.setText(eepromUsage + " %");
                    if (eepromUsage.matches("\\d+(?:\\.\\d+)?")) {
                        // if eeprom usage is 100% put it in red to show that it is full
                        if (Integer.parseInt(eepromUsage)==100){
                            txtEEpromUsage.setTextColor(Color.RED);
                        }
                        else {
                            txtEEpromUsage.setTextColor(txtViewThrust.getTextColors());
                        }
                    }

                    break;

                case 5:
                    //Value 5 contains the number of thrust curves
                    String nbrOfThrustCurve = (String) msg.obj;
                    txtNbrOfThrustCurve.setText(nbrOfThrustCurve);
                    if (nbrOfThrustCurve.matches("\\d+(?:\\.\\d+)?")) {
                        // If we have the maximum of thrust curve put it in red
                        if (Integer.parseInt(nbrOfThrustCurve)==25){
                            txtNbrOfThrustCurve.setTextColor(Color.RED);
                            Log.d("TestStandStatus", "RED");
                        }
                        else {
                            txtNbrOfThrustCurve.setTextColor(txtViewThrust.getTextColors());
                            Log.d("TestStandStatus", "BLACK");
                        }
                    }

                    Log.d("TestStandStatus", (String) msg.obj);
                    break;

                case 6:
                    //Value 6 contains the current pressure
                    String myPressureUnits = "";
                    double CONVERT_PRESSURE = 1.0;
                    //Value 1 contains the current thrust
                    if (myBT.getAppConf().getUnitsPressure()== GlobalConfig.PressureUnits.PSI) {
                        //PSI
                        myPressureUnits = getString(R.string.pressure_unit_psi);
                        CONVERT_PRESSURE = 1.0;
                    }
                    else if (myBT.getAppConf().getUnitsPressure()== GlobalConfig.PressureUnits.BAR) {
                        //BAR
                        myPressureUnits = getString(R.string.pressure_unit_bar);
                        CONVERT_PRESSURE = 1.0f / 14.504f;
                    }
                    else if (myBT.getAppConf().getUnitsPressure()== GlobalConfig.PressureUnits.KPascal) {
                        //Kpascal
                        myPressureUnits = getString(R.string.pressure_unit_kpascal);
                        CONVERT_PRESSURE = 6.895;
                    }
                    String currentPressure = (String) msg.obj;
                    if (currentPressure.matches("\\d+(?:\\.\\d+)?")) {
                        double temp;
                        temp = Double.parseDouble(currentPressure);
                        currentPressure = String.format("%.2f", (temp / 1000) * CONVERT_PRESSURE);
                        txtViewCurrentPressureValue.setText(currentPressure + " " + myPressureUnits);
                    }

                    Log.d("TestStandStatus", (String) msg.obj);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teststand_status);
        myBT = (ConsoleApplication) getApplication();

        //getApplicationContext().getResources().updateConfiguration(myBT.getAppLocal(), null);
        setContentView(R.layout.activity_teststand_status);

        btnDismiss = (Button) findViewById(R.id.butDismiss);
        btnRecording = (Button) findViewById(R.id.butRecording);
        btnTare = (Button) findViewById(R.id.butTare);
        btnRecording.setVisibility(View.INVISIBLE);

        btnTare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //Send tare command
                myBT.flush();
                myBT.clearInput();
                myBT.write("j;\n".toString());

            }
        });

        btnDismiss.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*if (status) {

                    status = false;
                    myBT.write("h;\n".toString());

                    myBT.setExit(true);
                    myBT.clearInput();
                    myBT.flush();
                }

                //turn off telemetry
                myBT.flush();
                myBT.clearInput();
                myBT.write("y0;\n".toString());*/
                finish();      //exit the  activity
            }
        });

        btnRecording.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (recording) {
                    recording = false;
                    myBT.write("w0;\n".toString());

                    //myBT.setExit(true);
                    myBT.clearInput();
                    myBT.flush();
                    btnRecording.setText("Start Rec");
                } else {
                    recording = true;
                    myBT.write("w1;\n".toString());
                    myBT.clearInput();
                    myBT.flush();
                    btnRecording.setText("Stop");
                }
            }
        });

        txtViewThrust = (TextView) findViewById(R.id.txtViewThrust);
        txtViewVoltage = (TextView) findViewById(R.id.txtViewVoltage);
        txtViewLink = (TextView) findViewById(R.id.txtViewLink);
        txtViewBatteryVoltage = (TextView) findViewById(R.id.txtViewBatteryVoltage);
        txtEEpromUsage = (TextView) findViewById(R.id.txtViewEEpromUsage);
        txtNbrOfThrustCurve = (TextView) findViewById(R.id.txtViewNbrOfThrustCurve);
        txtViewEEprom = (TextView) findViewById(R.id.txtViewEEprom);
        txtViewThrustCurve = (TextView) findViewById(R.id.txtViewThrustCurve);
        txtViewCurrentPressureValue = (TextView) findViewById(R.id.txtViewCurrentPressureValue);
        txtViewCurrentPressure = (TextView) findViewById(R.id.txtViewCurrentPressure);

        txtViewVoltage.setVisibility(View.VISIBLE);
        txtViewBatteryVoltage.setVisibility(View.VISIBLE);

        if (myBT.getTestStandConfigData().getTestStandName().equals("TestStandSTM32V2")) {
            txtViewCurrentPressureValue.setVisibility(View.VISIBLE);
            txtViewCurrentPressure.setVisibility(View.VISIBLE);
        }else {
            txtViewCurrentPressureValue.setVisibility(View.INVISIBLE);
            txtViewCurrentPressure.setVisibility(View.INVISIBLE);
        }
        txtViewEEprom.setVisibility(View.VISIBLE);
        txtViewThrustCurve.setVisibility(View.VISIBLE);
        txtEEpromUsage.setVisibility(View.VISIBLE);
        txtNbrOfThrustCurve.setVisibility(View.VISIBLE);


        txtViewLink.setText(myBT.getConnectionType());


        myBT.setHandler(handler);

        Runnable r = new Runnable() {

            @Override
            public void run() {
                while (true) {
                    if (!status) break;
                    myBT.ReadResult(10000);
                }
            }
        };

        testStandStatus = new Thread(r);
        testStandStatus.start();

    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (status) {

            status = false;
            myBT.write("h;\n".toString());

            myBT.setExit(true);
            myBT.clearInput();
            myBT.flush();
        }

        //turn off telemetry
        myBT.flush();
        myBT.clearInput();
        myBT.write("y0;\n".toString());
    }
    @Override
    protected void onStop() {
        super.onStop();
        //msg("On stop");
        if (status) {
            status = false;
            myBT.write("h;\n".toString());

            myBT.setExit(true);
            myBT.clearInput();
            myBT.flush();
            //finish();
        }


        myBT.flush();
        myBT.clearInput();
        myBT.write("h;\n".toString());
        try {
            while (myBT.getInputStream().available() <= 0) ;
        } catch (IOException e) {

        }
        String myMessage = "";
        long timeOut = 10000;
        long startTime = System.currentTimeMillis();

        myMessage = myBT.ReadResult(10000);
    }


    // fast way to call Toast
    private void msg(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
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
            Intent i = new Intent(TestStandStatus.this, HelpActivity.class);
            i.putExtra("help_file", "help_telemetry");
            startActivity(i);
            return true;
        }

        if (id == R.id.action_about) {
            Intent i = new Intent(TestStandStatus.this, AboutActivity.class);
            startActivity(i);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
