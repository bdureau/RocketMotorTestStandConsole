package com.rocketmotorteststand.telemetry;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.rocketmotorteststand.ConsoleApplication;
import com.rocketmotorteststand.R;

import java.io.IOException;

public class TestStandStatus extends AppCompatActivity {
    Button btnDismiss, btnRecording, btnTare;
    ConsoleApplication myBT;
    Thread testStandStatus;
    boolean status = true;
    boolean recording = false;

    private TextView txtViewBatteryVoltage;
    private TextView txtViewThrust, txtViewVoltage, txtViewLink, txtEEpromUsage, txtNbrOfThrustCurve;
    private TextView txtViewEEprom, txtViewThrustCurve,txtViewCurrentPressureValue;


    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {

                case 1:
                    String myUnits = "";
                    //Value 1 contains the current thrust
                    if (myBT.getAppConf().getUnits().equals("0"))
                        //kg
                        myUnits = getResources().getString(R.string.Kg_fview);
                    else if (myBT.getAppConf().getUnits().equals("1"))
                        //pounds
                        myUnits = getResources().getString(R.string.Pounds_fview);
                    else if (myBT.getAppConf().getUnits().equals("2"))
                        //Newtons
                        myUnits = getResources().getString(R.string.unit_newtons);
                    String thrust = (String) msg.obj;
                    if (thrust.matches("\\d+(?:\\.\\d+)?")) {
                        double temp;
                        temp = Double.parseDouble(thrust);
                        if (myBT.getAppConf().getUnits().equals("0"))
                            //kg
                            thrust = String.format("%.2f", (temp / 1000));
                        else if (myBT.getAppConf().getUnits().equals("1"))
                            //pounds
                            thrust = String.format("%.2f", (temp / 1000) * 2.20462);
                        else if (myBT.getAppConf().getUnits().equals("2"))
                            //Newtons
                            thrust = String.format("%.2f", (temp / 1000) * 9.80665);
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
                            txtViewVoltage.setTextColor(Color.BLACK);
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
                        if (Integer.parseInt(eepromUsage)==100){
                            txtEEpromUsage.setTextColor(Color.RED);
                        }
                        else {
                            txtEEpromUsage.setTextColor(Color.BLACK);
                        }
                    }

                    break;

                case 5:
                    //Value 5 contains the number of thrust curves
                    String nbrOfThrustCurve = (String) msg.obj;
                    txtNbrOfThrustCurve.setText(nbrOfThrustCurve);
                    if (nbrOfThrustCurve.matches("\\d+(?:\\.\\d+)?")) {
                        if (Integer.parseInt(nbrOfThrustCurve)==25){
                            //txtNbrOfThrustCurve.setHighlightColor(Color.RED);
                            txtNbrOfThrustCurve.setTextColor(Color.RED);
                            Log.d("TestStandStatus", "RED");
                        }
                        else {
                            //txtNbrOfThrustCurve.setHighlightColor(Color.BLACK);
                            txtNbrOfThrustCurve.setTextColor(Color.BLACK);
                            Log.d("TestStandStatus", "BLACK");
                        }
                    }

                    Log.d("TestStandStatus", (String) msg.obj);
                    break;

                case 6:
                    //Value 6 contains the current pressure
                    String currentPressure = (String) msg.obj;
                    txtViewCurrentPressureValue.setText(currentPressure+ " PSI");

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

        if (myBT.getTestStandConfigData().getTestStandName().equals("TestStandSTM32")) {
            txtViewVoltage.setVisibility(View.VISIBLE);
            txtViewBatteryVoltage.setVisibility(View.VISIBLE);
        } else {
            txtViewVoltage.setVisibility(View.INVISIBLE);
            txtViewBatteryVoltage.setVisibility(View.INVISIBLE);
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
}
