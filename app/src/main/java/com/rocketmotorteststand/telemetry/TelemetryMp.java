package com.rocketmotorteststand.telemetry;
/**
 * @description: This will display real time telemetry providing
 * that you have a telemetry long range module. This activity display the telemetry
 * using the MPAndroidChart library.
 * @author: boris.dureau@neuf.fr
 **/

import android.os.Handler;
import android.os.Message;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

import android.content.Intent;

import android.graphics.Typeface;

import android.os.Bundle;

import org.afree.chart.ChartFactory;
import org.afree.chart.AFreeChart;
import org.afree.chart.axis.NumberAxis;
import org.afree.chart.axis.ValueAxis;
import org.afree.chart.plot.PlotOrientation;
import org.afree.chart.plot.XYPlot;

import org.afree.data.category.DefaultCategoryDataset;
import org.afree.data.xy.XYSeriesCollection;

import org.afree.graphics.SolidColor;
import org.afree.graphics.geom.Font;


import android.graphics.Color;

import com.rocketmotorteststand.ConsoleApplication;
import com.rocketmotorteststand.Help.AboutActivity;
import com.rocketmotorteststand.Help.HelpActivity;
import com.rocketmotorteststand.R;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.rocketmotorteststand.ShareHandler;
import com.rocketmotorteststand.config.TestStandTabConfigActivity;

public class TelemetryMp extends AppCompatActivity {


    private TextView txtCurrentThrust;

    ConsoleApplication myBT;
    Thread rocketTelemetry;

    private LineChart mChart;

    LineData data;
    ArrayList<ILineDataSet> dataSets;
    //telemetry var
    private int lastPlotTime = 0;

    private double CONVERT = 1;
    ArrayList<Entry> yValuesThrust;
    ArrayList<Entry> yValuesPressure;

    int thrustTime = 0;

    boolean telemetry = true;

    Button dismissButton;

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    // Value 1 contain the current thrust
                    txtCurrentThrust.setText(String.valueOf((String) msg.obj));
                    if (((String) msg.obj).matches("\\d+(?:\\.\\d+)?")) {

                        int thrust = (int) (Integer.parseInt((String) msg.obj) * CONVERT);

                        yValuesThrust.add(new Entry(thrustTime, thrust));

                        //plot every seconde
                        if ((thrustTime - lastPlotTime) > 1000) {
                            lastPlotTime = thrustTime;
                            if(myBT.getTestStandConfigData().getTestStandName().equals("TestStand") ||
                                    myBT.getTestStandConfigData().getTestStandName().equals("TestStandSTM32")) {
                                LineDataSet set1 = new LineDataSet(yValuesThrust, "Thrust/Time");
                                set1.setDrawValues(false);
                                set1.setDrawCircles(false);
                                set1.setLabel("Thrust");
                                set1.setValueTextColor(Color.BLACK);
                                set1.setColor(Color.RED);
                                //set1.setValueTextSize(fontSize);

                                dataSets.clear();
                                dataSets.add(set1);
                                data = new LineData(dataSets);
                                mChart.clear();
                                mChart.setData(data);
                            }
                        }
                    }
                    break;

                case 2:
                    // Value 2 contain the sample time
                    if (((String) msg.obj).matches("\\d+(?:\\.\\d+)?")) {
                        if (Integer.parseInt((String) msg.obj) > 0) {
                            thrustTime = Integer.parseInt((String) msg.obj);
                        }
                    }
                    break;
                case 6:
                    //Value 6 contains the current pressure
                    String currentPressure = (String) msg.obj;
                    if (((String) msg.obj).matches("\\d+(?:\\.\\d+)?")) {
                        int pressure = (int) (Integer.parseInt((String) msg.obj) );
                        yValuesPressure.add(new Entry(thrustTime, pressure));

                        //plot every seconde
                        if ((thrustTime - lastPlotTime) > 1000) {
                            lastPlotTime = thrustTime;
                            if(myBT.getTestStandConfigData().getTestStandName().equals("TestStandSTM32V2")) {
                                LineDataSet set1 = new LineDataSet(yValuesThrust, "Thrust/Time");
                                set1.setDrawValues(false);
                                set1.setDrawCircles(false);
                                set1.setLabel("Thrust");
                                set1.setValueTextColor(Color.BLACK);
                                set1.setColor(Color.RED);
                                //set1.setValueTextSize(fontSize);

                                LineDataSet set2 = new LineDataSet(yValuesPressure, "pressure/Time");
                                set2.setDrawValues(false);
                                set2.setDrawCircles(false);
                                set2.setLabel("pressure");
                                set2.setValueTextColor(Color.BLACK);
                                set2.setColor(Color.BLUE);
                                //set2.setValueTextSize(fontSize);
                                dataSets.clear();
                                dataSets.add(set1);
                                dataSets.add(set2);
                                data = new LineData(dataSets);
                                mChart.clear();
                                mChart.setData(data);
                            }
                        }
                    }
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_telemetry_mp);
        //get the bluetooth Application pointer
        myBT = (ConsoleApplication) getApplication();


        txtCurrentThrust = (TextView) findViewById(R.id.textViewCurrentThrust);


        dismissButton = (Button) findViewById(R.id.butDismiss);

        myBT.setHandler(handler);

        // Read the application config
        myBT.getAppConf().ReadConfig();


        int graphBackColor;//= Color.WHITE;
        graphBackColor = myBT.getAppConf().ConvertColor(Integer.parseInt(myBT.getAppConf().getGraphBackColor()));

        int fontSize;
        fontSize = myBT.getAppConf().ConvertFont(Integer.parseInt(myBT.getAppConf().getFontSize()));

        int axisColor;//=Color.BLACK;
        axisColor = myBT.getAppConf().ConvertColor(Integer.parseInt(myBT.getAppConf().getGraphColor()));

        int labelColor = Color.BLACK;

        int nbrColor = Color.BLACK;
        String myUnits = "";
        if (myBT.getAppConf().getUnits().equals("0")) {
            //kg
            myUnits = getResources().getString(R.string.Kg_fview);
            CONVERT = 1000;
        }
        else if (myBT.getAppConf().getUnits().equals("1")) {
            //pounds
            myUnits = getResources().getString(R.string.Pounds_fview);
            CONVERT = 2.20462/1000;
        }
        else if (myBT.getAppConf().getUnits().equals("2")) {
            //newtons
            myUnits = getResources().getString(R.string.config_unit_newtons);
            CONVERT = 9.80665/1000;
        }

        mChart = (LineChart) findViewById(R.id.telemetryChartView);

        mChart.setDragEnabled(true);
        mChart.setScaleEnabled(true);
        mChart.setScaleMinima(0, 0);
        dataSets = new ArrayList<>();

        yValuesThrust = new ArrayList<>();
        yValuesThrust.add(new Entry(0, 0));
        yValuesPressure = new ArrayList<>();
        yValuesPressure.add(new Entry(0, 0));

        LineDataSet set1 = new LineDataSet(yValuesThrust, getString(R.string.telemetry_thrust));
        LineDataSet set2 = new LineDataSet(yValuesThrust, "Pressure");
        set1.setValueTextColor(Color.RED);
        set1.setValueTextSize(fontSize);

        set2.setValueTextColor(Color.BLUE);
        set2.setValueTextSize(fontSize);

        dataSets.add(set1);
        dataSets.add(set2);

        LineData data = new LineData(dataSets);
        mChart.setData(data);
        Description desc = new Description();
        desc.setText(getString(R.string.tel_telemetry));
        mChart.setDescription(desc);
        mChart.setBackgroundColor(graphBackColor);
        startTelemetry();
        dismissButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*if (telemetry) {
                    telemetry = false;
                    myBT.write("h;\n".toString());

                    myBT.setExit(true);
                    myBT.clearInput();
                    myBT.flush();
                }
                //turn off telemetry
                myBT.flush();
                myBT.clearInput();
                myBT.write("y0;\n".toString());
*/
                finish();      //exit the activity
            }
        });
    }

    public void startTelemetry() {
        telemetry = true;

        lastPlotTime = 0;
        myBT.initThrustCurveData();

        //LiftOffTime = 0;
        Runnable r = new Runnable() {

            @Override
            public void run() {
                while (true) {
                    if (!telemetry) break;
                    myBT.ReadResult(100000);
                }
            }
        };
        rocketTelemetry = new Thread(r);
        rocketTelemetry.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (telemetry) {
            telemetry = false;
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
   /* public void onClickStartTelemetry(View view) {
        telemetry = true;
        lastPlotTime = 0;
        myBT.initThrustCurveData();

        Runnable r = new Runnable() {

            @Override
            public void run() {
                while (true) {
                    if (!telemetry) break;
                    myBT.ReadResult(100000);
                }
            }
        };

        rocketTelemetry = new Thread(r);
        rocketTelemetry.start();
    }*/

   /* public void onClickStopTelemetry(View view) {
        myBT.write("h;\n".toString());

        myBT.setExit(true);

        telemetry = false;

        myBT.clearInput();
        myBT.flush();

    }
*/

    @Override
    protected void onStop() {
        //msg("On stop");
        super.onStop();
        if (telemetry) {
            telemetry = false;
            myBT.write("h;\n".toString());

            myBT.setExit(true);
            myBT.clearInput();
            myBT.flush();
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

        myMessage = myBT.ReadResult(100000);
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
            Intent i = new Intent(TelemetryMp.this, HelpActivity.class);
            i.putExtra("help_file", "help_telemetry");
            startActivity(i);
            return true;
        }

        if (id == R.id.action_about) {
            Intent i = new Intent(TelemetryMp.this, AboutActivity.class);
            startActivity(i);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
