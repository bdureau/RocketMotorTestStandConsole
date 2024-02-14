package com.rocketmotorteststand.telemetry.TelemetryStatusFragment;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.rocketmotorteststand.ConsoleApplication;
import com.rocketmotorteststand.R;

import java.util.ArrayList;

public class TelemetryMpFragment extends Fragment {
    private ConsoleApplication myBT;
    private boolean ViewCreated = false;
    private TextView txtCurrentThrust, txtCurrentPressure, txtCurrentPressure2, textViewlblCurrentPressure2;
    private LineChart mChart;
    LineData data;
    private double CONVERT = 1;
    ArrayList<Entry> yValuesThrust;
    ArrayList<Entry> yValuesPressure;
    ArrayList<Entry> yValuesPressure2;
    ArrayList<ILineDataSet> dataSets;
    int graphBackColor ;
    int fontSize;
    int axisColor;
    int labelColor;
    int nbrColor;
    private String [] units;

    public void setCurrentThrust(String value) {
        if(ViewCreated)
            txtCurrentThrust.setText(value);
    }
    public void setCurrentPressure(String value) {
        if(ViewCreated)
            txtCurrentPressure.setText(value);
    }
    public void setCurrentPressure2(String value) {
        if(ViewCreated)
            txtCurrentPressure2.setText(value);
    }
    public TelemetryMpFragment(ConsoleApplication pBT, String [] pUnits) {
        myBT = pBT;
        units = pUnits;
    }
    public boolean isViewCreated() {
        return ViewCreated;
    }
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        View view = inflater.inflate(R.layout.fragment_telemetry_mp, container, false);

        txtCurrentThrust = (TextView) view.findViewById(R.id.textViewCurrentThrust);
        txtCurrentPressure = (TextView) view.findViewById(R.id.textViewCurrentPressure);
        txtCurrentPressure2 = (TextView) view.findViewById(R.id.textViewCurrentPressure2);
        textViewlblCurrentPressure2 = (TextView) view.findViewById(R.id.textViewlblCurrentPressure2);

        if(myBT.getTestStandConfigData().getTestStandName().equals("TestStandSTM32V3") ||
                myBT.getTestStandConfigData().getTestStandName().equals("TestStandESP32V3")) {
            txtCurrentPressure2.setVisibility(View.VISIBLE);
            textViewlblCurrentPressure2.setVisibility(View.VISIBLE);
        } else {
            txtCurrentPressure2.setVisibility(View.INVISIBLE);
            textViewlblCurrentPressure2.setVisibility(View.INVISIBLE);
        }

        graphBackColor = myBT.getAppConf().ConvertColor(myBT.getAppConf().getGraphBackColor());
        fontSize = myBT.getAppConf().ConvertFont(myBT.getAppConf().getFontSize());
        axisColor = myBT.getAppConf().ConvertColor(myBT.getAppConf().getGraphColor());
        labelColor = Color.BLACK;
        nbrColor = Color.BLACK;

        mChart = (LineChart) view.findViewById(R.id.telemetryChartView);

        mChart.setDragEnabled(true);
        mChart.setScaleEnabled(true);
        mChart.setScaleMinima(0, 0);
        dataSets = new ArrayList<>();

        yValuesThrust = new ArrayList<>();
        yValuesThrust.add(new Entry(0, 0));
        yValuesPressure = new ArrayList<>();
        yValuesPressure.add(new Entry(0, 0));

        yValuesPressure2 = new ArrayList<>();
        yValuesPressure2.add(new Entry(0, 0));

        LineDataSet set1 = new LineDataSet(yValuesThrust, getString(R.string.telemetry_thrust));
        LineDataSet set2 = new LineDataSet(yValuesThrust, getString(R.string.curve_pressure));



        set1.setValueTextColor(labelColor);
        set1.setValueTextSize(fontSize);

        set2.setValueTextColor(labelColor);
        set2.setValueTextSize(fontSize);



        dataSets.add(set1);
        dataSets.add(set2);
        if(myBT.getTestStandConfigData().getTestStandName().equals("TestStandSTM32V3") ||
                myBT.getTestStandConfigData().getTestStandName().equals("TestStandESP32V3")) {
            LineDataSet set3 = new LineDataSet(yValuesThrust, getString(R.string.curve_pressure_ch2));
            set3.setValueTextColor(labelColor);
            set3.setValueTextSize(fontSize);
            dataSets.add(set3);
        }

        LineData data = new LineData(dataSets);
        mChart.setData(data);
        Description desc = new Description();
        desc.setText(getString(R.string.tel_telemetry));
        mChart.setDescription(desc);
        mChart.setBackgroundColor(graphBackColor);

        ViewCreated = true;
        return view;
    }


    public void plotThrust(ArrayList<Entry> yValuesThrust) {
        LineDataSet set1 = new LineDataSet(yValuesThrust, getString(R.string.telemetry_thrust_time));
        set1.setDrawValues(false);
        set1.setDrawCircles(false);
        set1.setLabel("Thrust");
        set1.setValueTextColor(Color.BLACK);
        set1.setColor(Color.RED);
        //set1.setValueTextSize(fontSize);
        set1.setValueTextColor(labelColor);
        set1.setValueTextSize(fontSize);

        dataSets.clear();
        dataSets.add(set1);
        this.data = new LineData(dataSets);
        //data = new LineData(dataSets);
        this.mChart.clear();
        this.mChart.setData(this.data);
    }

    public void plotThrustAndPressure(ArrayList<Entry> yValuesThrust,ArrayList<Entry> yValuesPressure, ArrayList<Entry> yValuesPressure2  ) {
        LineDataSet set1 = new LineDataSet(yValuesThrust, getResources().getString(R.string.Thrust)  +" (" + units[0] +")/" + getString(R.string.telemetry_time));
        set1.setDrawValues(false);
        set1.setDrawCircles(false);
        set1.setLabel(getResources().getString(R.string.Thrust));
        set1.setValueTextColor(Color.BLACK);
        set1.setColor(Color.RED);
        //set1.setValueTextSize(fontSize);

        LineDataSet set2 = new LineDataSet(yValuesPressure, getString(R.string.telemetry_pressure) +" (" + units[1] +")/" +getString(R.string.telemetry_time));
        set2.setDrawValues(false);
        set2.setDrawCircles(false);
        set2.setLabel(getString(R.string.telemetry_pressure));
        set2.setValueTextColor(Color.BLACK);
        set2.setColor(Color.BLUE);
        //set2.setValueTextSize(fontSize);
        LineDataSet set3 =null;
        if(myBT.getTestStandConfigData().getTestStandName().equals("TestStandSTM32V3") ||
          myBT.getTestStandConfigData().getTestStandName().equals("TestStandESP32V3")) {
            set3 = new LineDataSet(yValuesPressure2, getString(R.string.telemetry_pressure) + " (" + units[2] +")/" +getString(R.string.telemetry_time));
            set3.setDrawValues(false);
            set3.setDrawCircles(false);
            set3.setLabel(getString(R.string.telemetry_pressure));
            set3.setValueTextColor(Color.BLACK);
            set3.setColor(Color.GREEN);
        }
        dataSets.clear();
        dataSets.add(set1);
        dataSets.add(set2);
        if(myBT.getTestStandConfigData().getTestStandName().equals("TestStandSTM32V3") ||
                myBT.getTestStandConfigData().getTestStandName().equals("TestStandESP32V3")) {
            dataSets.add(set3);
        }
        data = new LineData(dataSets);
        mChart.clear();
        mChart.setData(data);
    }
}
