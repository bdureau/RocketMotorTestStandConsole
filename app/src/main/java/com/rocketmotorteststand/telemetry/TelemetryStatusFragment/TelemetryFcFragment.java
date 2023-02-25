package com.rocketmotorteststand.telemetry.TelemetryStatusFragment;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.rocketmotorteststand.ConsoleApplication;
import com.rocketmotorteststand.R;
import com.rocketmotorteststand.ThrustCurve.ThrustCurveData;
import com.rocketmotorteststand.ThrustCurve.ThrustCurveView.ChartView;

import org.afree.chart.AFreeChart;
import org.afree.chart.ChartFactory;
import org.afree.chart.axis.NumberAxis;
import org.afree.chart.axis.ValueAxis;
import org.afree.chart.plot.PlotOrientation;
import org.afree.chart.plot.XYPlot;
import org.afree.data.xy.XYSeries;
import org.afree.data.xy.XYSeriesCollection;
import org.afree.graphics.SolidColor;
import org.afree.graphics.geom.Font;



public class TelemetryFcFragment extends Fragment {
    private ConsoleApplication myBT;
    private TextView txtCurrentThrust, txtCurrentPressure;
    private boolean ViewCreated = false;

    private String [] units;

    private XYPlot plot;
    ChartView chartView;

    public void setCurrentThrust(String value) {
        if(ViewCreated)
            txtCurrentThrust.setText(value);
    }
    public void setCurrentPressure(String value) {
        if(ViewCreated)
            txtCurrentPressure.setText(value);
    }
    public TelemetryFcFragment(ConsoleApplication pBT, String [] pUnits) {
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

        View view = inflater.inflate(R.layout.fragment_telemetry_fc, container, false);

        txtCurrentThrust = (TextView) view.findViewById(R.id.textViewCurrentThrust);
        txtCurrentPressure = (TextView) view.findViewById(R.id.textViewCurrentPressure);

        int graphBackColor = myBT.getAppConf().ConvertColor(myBT.getAppConf().getGraphBackColor());

        int fontSize = myBT.getAppConf().ConvertFont(myBT.getAppConf().getFontSize());

        int axisColor = myBT.getAppConf().ConvertColor(myBT.getAppConf().getGraphColor());

        int labelColor = Color.BLACK;

        int nbrColor = Color.BLACK;

        //font
        Font font = new Font("Dialog", Typeface.NORMAL, fontSize);

        String title;
        String yLabel;

        if (myBT.getTestStandConfigData().getTestStandName().equals("TestStand") ||
                myBT.getTestStandConfigData().getTestStandName().equals("TestStandSTM32")) {
            title = getResources().getString(R.string.Thrust_time);
            yLabel = getResources().getString(R.string.Thrust) + " (" + units[0] + ") ";

        } else {
            title = "Thrust and pressure / time";
            yLabel = getResources().getString(R.string.Thrust) + " (" + units[0] + ") " + "Pressure" + " (" + units[1] + ") ";
        }

        AFreeChart chart = ChartFactory.createXYLineChart(
                title,
                getResources().getString(R.string.Time_fv),
                yLabel,
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

        chartView = (ChartView) view.findViewById(R.id.telemetryChartView);
        chartView.setChart(chart);

        ViewCreated = true;
        return view;
    }
    public void plotThrust(XYSeries curveData) {
        XYSeriesCollection thrustCurveData = new XYSeriesCollection();
        thrustCurveData.addSeries(curveData);
        plot.setDataset(0, thrustCurveData);
    }

    public void plotThrustAndPressure(XYSeries curveThrustData, XYSeries curvePressureData) {
        XYSeriesCollection thrustCurveData = new XYSeriesCollection();
        thrustCurveData.addSeries(curveThrustData);
        thrustCurveData.addSeries(curvePressureData);
        plot.setDataset(0, thrustCurveData);
    }
}
