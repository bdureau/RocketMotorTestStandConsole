package com.rocketmotorteststand.telemetry;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import androidx.appcompat.app.AppCompatActivity;

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
    Button btnDismiss, btnRecording;
    ConsoleApplication myBT;
    Thread testStandStatus;
    boolean status = true;
    boolean recording = false;

    private TextView txtViewBatteryVoltage;
    private TextView txtViewThrust, txtViewVoltage, txtViewLink,  txtEEpromUsage,txtNbrOfThrustCurve;
    private TextView  txtViewEEprom, txtViewThrustCurve;


    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {

                case 1:
                    String myUnits;
                    //Value 1 contains the current thrust
                    if (myBT.getAppConf().getUnits().equals("0"))
                        //Meters
                        myUnits = getResources().getString(R.string.Kg_fview);
                    else
                        //Feet
                        myUnits = getResources().getString(R.string.Pounds_fview);
                    txtViewThrust.setText((String) msg.obj + " " + myUnits);
                    break;
                case 13:
                    //Value 13 contains the battery voltage
                    String voltage = (String) msg.obj;
                    if (voltage.matches("\\d+(?:\\.\\d+)?")) {
                        /*double batVolt;

                        batVolt =  (3.1972*((Double.parseDouble(voltage) * 3300) / 4096)/1000);
                        txtViewVoltage.setText(String.format("%.2f",batVolt)+ " Volts");*/
                        txtViewVoltage.setText(voltage + " Volts");
                    } else {
                        txtViewVoltage.setText("NA");
                    }
                    break;


                case 15:
                    //Value 15 contains the EEprom usage
                    txtEEpromUsage.setText((String) msg.obj + " %");
                    break;

                case 16:
                    //Value 16 contains the number of thrust curves
                    txtNbrOfThrustCurve.setText((String) msg.obj );
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
        if (myBT.getTestStandConfigData().getTestStandName().equals("AltiGPS"))
            btnRecording.setVisibility(View.VISIBLE);
        else
            btnRecording.setVisibility(View.INVISIBLE);

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

        txtEEpromUsage= (TextView) findViewById(R.id.txtViewEEpromUsage);
        txtNbrOfThrustCurve= (TextView) findViewById(R.id.txtViewNbrOfThrustCurve);
        txtViewEEprom = (TextView) findViewById(R.id.txtViewEEprom);
        txtViewThrustCurve = (TextView) findViewById(R.id.txtViewThrustCurve);


        if (myBT.getTestStandConfigData().getTestStandName().equals("TestStandSTM32") ) {
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
