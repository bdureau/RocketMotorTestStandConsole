package com.rocketmotorteststand.telemetry;
/**
 * @description: This will display real time telemetry providing
 * that you have a telemetry long range module. This activity display the telemetry
 * using the AFreeChart library. If you are using Android 8 or greater you should
 * use the MPAndroidChart otherwise it will crash your application
 * @author: boris.dureau@neuf.fr
 **/

import android.content.Intent;
import android.os.Handler;
import android.os.Message;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;

import android.graphics.Typeface;

import org.afree.chart.ChartFactory;
import org.afree.chart.AFreeChart;
import org.afree.chart.axis.NumberAxis;
import org.afree.chart.axis.ValueAxis;
import org.afree.chart.plot.PlotOrientation;
import org.afree.chart.plot.XYPlot;

import org.afree.data.xy.XYSeriesCollection;

import org.afree.graphics.SolidColor;
import org.afree.graphics.geom.Font;


import android.graphics.Color;

import com.rocketmotorteststand.ConsoleApplication;
import com.rocketmotorteststand.Help.AboutActivity;
import com.rocketmotorteststand.Help.HelpActivity;
import com.rocketmotorteststand.ShareHandler;
import com.rocketmotorteststand.ThrustCurve.ThrustCurveView.ChartView;
import com.rocketmotorteststand.ThrustCurve.ThrustCurveData;
import com.rocketmotorteststand.R;

public class Telemetry extends AppCompatActivity {


    private TextView txtCurrentThrust;

    ConsoleApplication myBT;
    Thread rocketTelemetry;
    ChartView chartView;
    private ThrustCurveData myThrustCurve = null;
    XYPlot plot;
    //telemetry var
    private long LiftOffTime = 0;
    private int lastPlotTime = 0;
    //private int lastSpeakTime = 1000;
    private double FEET_IN_METER = 1;

    int altitudeTime = 0;
    //int altitude =0;

    boolean telemetry = true;


    Button dismissButton;

    //private TextToSpeech mTTS;

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    // Value 1 contain the current thrust
                    txtCurrentThrust.setText(String.valueOf((String) msg.obj));
                    if (((String) msg.obj).matches("\\d+(?:\\.\\d+)?")) {


                        //int altitudeTime = (int)(System.currentTimeMillis()-LiftOffTime);
                        int altitude = (int) (Integer.parseInt((String) msg.obj) * FEET_IN_METER);
                        myThrustCurve.AddToThrustCurve(altitudeTime, altitude, "Telemetry");

                        //plot every seconde
                        if ((altitudeTime - lastPlotTime) > 1000) {
                            lastPlotTime = altitudeTime;
                            XYSeriesCollection flightData;
                            flightData = myThrustCurve.GetThrustCurveData("Telemetry");
                            plot.setDataset(0, flightData);
                            //mTTS.speak("rocket has lift off", TextToSpeech.QUEUE_FLUSH,null);
                        }

                    }
                    break;


                case 2:
                    // Value 2 contain the sample time
                    if (((String) msg.obj).matches("\\d+(?:\\.\\d+)?")) {
                        if (Integer.parseInt((String) msg.obj) > 0) {
                            altitudeTime = Integer.parseInt((String) msg.obj);
                        }
                    }
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_telemetry);
        //get the bluetooth Application pointer
        myBT = (ConsoleApplication) getApplication();

        //init text to speech
        /*mTTS = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int result = 0;
                    if (Locale.getDefault().getLanguage() == "en")
                        result = mTTS.setLanguage(Locale.ENGLISH);
                    else if (Locale.getDefault().getLanguage() == "fr")
                        result = mTTS.setLanguage(Locale.FRENCH);
                    else
                        result = mTTS.setLanguage(Locale.ENGLISH);


                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e("TTS", "Language not supported");
                    } else {

                    }
                } else {
                    Log.e("TTS", "Init failed");
                }
            }
        });
        mTTS.setPitch(1.0f);
        mTTS.setSpeechRate(1.0f);*/


        txtCurrentThrust = (TextView) findViewById(R.id.textViewCurrentThrust);


        dismissButton = (Button) findViewById(R.id.butDismiss);

        myBT.setHandler(handler);
        //stopTelemetryButton.setEnabled(false);


        // Read the application config
        myBT.getAppConf().ReadConfig();
        int graphBackColor;//= Color.WHITE;
        graphBackColor = myBT.getAppConf().ConvertColor(myBT.getAppConf().getGraphBackColor());

        int fontSize;
        fontSize = myBT.getAppConf().ConvertFont(myBT.getAppConf().getFontSize());

        int axisColor;//=Color.BLACK;
        axisColor = myBT.getAppConf().ConvertColor(myBT.getAppConf().getGraphColor());

        int labelColor = Color.BLACK;

        int nbrColor = Color.BLACK;
        String myUnits = "";
        if (myBT.getAppConf().getUnits()==0)
            //Meters
            myUnits = getResources().getString(R.string.Kg_fview);
        else
            //Feet
            myUnits = getResources().getString(R.string.Pounds_fview);

        if (myBT.getAppConf().getUnitsValue().equals("Kg")) {
            FEET_IN_METER = 1;
        } else {
            FEET_IN_METER = 3.28084;
        }
        //font
        Font font = new Font("Dialog", Typeface.NORMAL, fontSize);

        AFreeChart chart = ChartFactory.createXYLineChart(
                getResources().getString(R.string.Thrust_time),
                getResources().getString(R.string.Time_fv),
                getResources().getString(R.string.Thrust) + " (" + myUnits + ")",
                null,
                PlotOrientation.VERTICAL, // orientation
                true,                     // include legend
                true,                     // tooltips?
                false                     // URLs?
        );

        // NOW DO SOME OPTIONAL CUSTOMISATION OF THE CHART...
        chart.getTitle().setFont(font);
        // set the background color for the chart...
        chart.setBackgroundPaintType(new SolidColor(graphBackColor));

        // get a reference to the plot for further customisation...
        plot = chart.getXYPlot();

        plot.setDomainGridlinesVisible(false);
        plot.setRangeGridlinesVisible(false);

        plot.setBackgroundPaintType(new SolidColor(graphBackColor));
        plot.setOutlinePaintType(new SolidColor(Color.YELLOW));
        plot.setDomainZeroBaselinePaintType(new SolidColor(Color.GREEN));
        plot.setRangeZeroBaselinePaintType(new SolidColor(Color.MAGENTA));

        final ValueAxis Xaxis = plot.getDomainAxis();
        Xaxis.setAutoRange(true);
        Xaxis.setAxisLinePaintType(new SolidColor(axisColor));

        final ValueAxis YAxis = plot.getRangeAxis();
        YAxis.setAxisLinePaintType(new SolidColor(axisColor));


        Xaxis.setTickLabelFont(font);
        Xaxis.setLabelFont(font);

        YAxis.setTickLabelFont(font);
        YAxis.setLabelFont(font);

        //Xaxis label color
        Xaxis.setLabelPaintType(new SolidColor(labelColor));

        Xaxis.setTickMarkPaintType(new SolidColor(axisColor));
        Xaxis.setTickLabelPaintType(new SolidColor(nbrColor));
        //Y axis label color
        YAxis.setLabelPaintType(new SolidColor(labelColor));
        YAxis.setTickLabelPaintType(new SolidColor(nbrColor));
        final NumberAxis rangeAxis2 = new NumberAxis("Range Axis 2");
        rangeAxis2.setAutoRangeIncludesZero(false);


        chartView = (ChartView) findViewById(R.id.telemetryChartView);
        chartView.setChart(chart);
        startTelemetry();
        dismissButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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

                finish();      //exit the activity
            }
        });
    }

    public void startTelemetry() {
        telemetry = true;
        //startTelemetryButton.setEnabled(false);
        //stopTelemetryButton.setEnabled(true);
        lastPlotTime = 0;
        myBT.initThrustCurveData();

        myThrustCurve = myBT.getThrustCurveData();
        LiftOffTime = 0;
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

    /*public void onClickStartTelemetry(View view) {

        telemetry = true;
        //startTelemetryButton.setEnabled(false);
        //stopTelemetryButton.setEnabled(true);
        lastPlotTime = 0;
        myBT.initThrustCurveData();

        myThrustCurve = myBT.getThrustCurveData();
        LiftOffTime = 0;
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

    public void onClickStopTelemetry(View view) {
        myBT.write("h;\n".toString());

        myBT.setExit(true);

        myThrustCurve.ClearThrustCurve();
        telemetry = false;

        myBT.clearInput();
        myBT.flush();

    }*/


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
            Intent i = new Intent(Telemetry.this, HelpActivity.class);
            i.putExtra("help_file", "help_telemetry");
            startActivity(i);
            return true;
        }

        if (id == R.id.action_about) {
            Intent i = new Intent(Telemetry.this, AboutActivity.class);
            startActivity(i);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
