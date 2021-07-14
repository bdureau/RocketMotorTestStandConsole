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
import com.rocketmotorteststand.R;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

public class TelemetryMp extends AppCompatActivity {


    private TextView txtCurrentThrust;

    ConsoleApplication myBT;
    Thread rocketTelemetry;

    private LineChart mChart;

    LineData data;
    ArrayList<ILineDataSet> dataSets;
    //telemetry var
    private long LiftOffTime = 0;
    private int lastPlotTime = 0;

    private double CONVERT = 1;
    ArrayList<Entry> yValues;
    int thrustTime = 0;

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

                        int thrust = (int) (Integer.parseInt((String) msg.obj) * CONVERT);

                        yValues.add(new Entry(thrustTime, thrust));

                        //plot every seconde
                        if ((thrustTime - lastPlotTime) > 1000) {
                            lastPlotTime = thrustTime;

                            LineDataSet set1 = new LineDataSet(yValues, "Thrust/Time");

                            set1.setDrawValues(false);
                            set1.setDrawCircles(false);
                            set1.setLabel("Thrust");

                            dataSets.clear();
                            dataSets.add(set1);

                            data = new LineData(dataSets);
                            mChart.clear();
                            mChart.setData(data);
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

        //font

        yValues = new ArrayList<>();
        yValues.add(new Entry(0, 0));
        //yValues.add(new Entry(1,0));

        LineDataSet set1 = new LineDataSet(yValues, "Thrust");
        mChart = (LineChart) findViewById(R.id.telemetryChartView);

        mChart.setDragEnabled(true);
        mChart.setScaleEnabled(true);
        mChart.setScaleMinima(0, 0);
        dataSets = new ArrayList<>();
        dataSets.add(set1);

        LineData data = new LineData(dataSets);
        mChart.setData(data);
        Description desc = new Description();
        desc.setText("Telemetry");
        mChart.setDescription(desc);
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

        lastPlotTime = 0;
        myBT.initThrustCurveData();

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

    public void onClickStartTelemetry(View view) {

        telemetry = true;

        lastPlotTime = 0;
        myBT.initThrustCurveData();

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

        telemetry = false;

        myBT.clearInput();
        myBT.flush();

    }


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
}
