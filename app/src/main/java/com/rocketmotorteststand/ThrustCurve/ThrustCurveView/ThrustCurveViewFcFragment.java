package com.rocketmotorteststand.ThrustCurve.ThrustCurveView;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.data.Entry;
import com.rocketmotorteststand.ConsoleApplication;
import com.rocketmotorteststand.GlobalConfig;
import com.rocketmotorteststand.R;
import com.rocketmotorteststand.ThrustCurve.ThrustCurveViewTabActivity;

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

import java.util.ArrayList;

public class ThrustCurveViewFcFragment extends Fragment {
    private ChartView chartView;
    private AFreeChart mChart = null;
    private XYPlot plot;
    public XYSeriesCollection allThrustCurveData;
    private ConsoleApplication myBT;
    int graphBackColor, fontSize, axisColor, labelColor, nbrColor;

    private String[] units = null;
    private String curvesNames[] = null;
    private boolean[] checkedItems = null;
    static int colors[] = {Color.RED, Color.BLUE, Color.BLACK,
            Color.GREEN, Color.CYAN, Color.GRAY, Color.MAGENTA, Color.YELLOW, Color.RED,
            Color.BLUE, Color.BLACK,
            Color.GREEN, Color.CYAN, Color.GRAY, Color.MAGENTA, Color.YELLOW, Color.RED, Color.BLUE, Color.BLACK,
            Color.GREEN, Color.CYAN, Color.GRAY, Color.MAGENTA, Color.YELLOW};


    public ThrustCurveViewFcFragment(XYSeriesCollection pAllThrustCurveData,
                                     ConsoleApplication pBT,
                                     String pCurvesNames[],
                                     boolean pCheckedItems[],
                                     String pUnits[]
    ) {
        this.allThrustCurveData = pAllThrustCurveData;
        this.myBT = pBT;
        this.curvesNames = pCurvesNames;
        this.checkedItems = pCheckedItems;
        this.units = pUnits;
    }

    public void setCheckedItems(boolean[] checkedItems) {
        this.checkedItems = checkedItems;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        View view = inflater.inflate(R.layout.fragment_thrustcurve_view_fc, container, false);
        chartView = (ChartView) view.findViewById(R.id.chartView1);

        String myUnits = "";
        if (myBT.getAppConf().getUnits()== GlobalConfig.ThrustUnits.KG)
            //kg
            myUnits = getResources().getString(R.string.Kg_fview);
        else if (myBT.getAppConf().getUnits()== GlobalConfig.ThrustUnits.POUNDS)
            //Pounds
            myUnits = getResources().getString(R.string.Pounds_fview);
        else if (myBT.getAppConf().getUnits()== GlobalConfig.ThrustUnits.NEWTONS)
            //newtons
            myUnits = getResources().getString(R.string.unit_newtons);

        mChart = ChartFactory.createXYLineChart(
                getResources().getString(R.string.Thrust_time),
                getResources().getString(R.string.Time_fv),
                getResources().getString(R.string.Thrust) + " (" + myUnits + ")",
                null,
                PlotOrientation.VERTICAL, // orientation
                true,                     // include legend
                true,                     // tooltips?
                false                     // URLs?
        );

        chartView.setChart(mChart);
        drawGraph();
        drawAllCurves(allThrustCurveData);

        return view;
    }

    public void drawGraph() {
        graphBackColor = myBT.getAppConf().ConvertColor(myBT.getAppConf().getGraphBackColor());
        fontSize = myBT.getAppConf().ConvertFont(myBT.getAppConf().getFontSize());
        axisColor = myBT.getAppConf().ConvertColor(myBT.getAppConf().getGraphColor());
        labelColor = Color.BLACK;
        nbrColor = Color.BLACK;
        //font
        Font font = new Font("Dialog", Typeface.NORMAL, fontSize);
        mChart.getTitle().setFont(font);
        // set the background color for the chart...
        mChart.setBackgroundPaintType(new SolidColor(graphBackColor));
        // get a reference to the plot for further customisation...
        plot = mChart.getXYPlot();
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

        //X axis label color
        Xaxis.setLabelPaintType(new SolidColor(labelColor));
        Xaxis.setTickMarkPaintType(new SolidColor(axisColor));
        Xaxis.setTickLabelPaintType(new SolidColor(nbrColor));
        //Y axis label color
        YAxis.setLabelPaintType(new SolidColor(labelColor));
        YAxis.setTickLabelPaintType(new SolidColor(nbrColor));
        final NumberAxis rangeAxis2 = new NumberAxis("Range Axis 2");
        rangeAxis2.setAutoRangeIncludesZero(false);
    }

    public void drawAllCurves(XYSeriesCollection allThrustCurveData) {

        XYSeriesCollection thrustCurveData = new XYSeriesCollection();

        for (int i = 0; i < curvesNames.length; i++) {
            Log.d("drawAllCurves", "i:" + i);
            Log.d("drawAllCurves", "curvesNames:" + curvesNames[i]);
            if (checkedItems[i]) {
                int nbrData = allThrustCurveData.getSeries(i).getItemCount();
                Log.d("drawAllCurves", "i:" + i);
                if (i == 0) {
                    XYSeries currentCurve = new XYSeries(0);
                    float CONVERT = 1;
                    if (myBT.getAppConf().getUnits()== GlobalConfig.ThrustUnits.KG) {
                        //kg
                        CONVERT = 1;
                    } else if (myBT.getAppConf().getUnits()== GlobalConfig.ThrustUnits.POUNDS) {
                        //pound
                        CONVERT = 2.20462f;
                    } else if (myBT.getAppConf().getUnits()== GlobalConfig.ThrustUnits.NEWTONS) {
                        //newton
                        CONVERT = 9.80665f;
                    }
                    for (int k = 0; k < nbrData; k++) {
                        currentCurve.add(allThrustCurveData.getSeries(i).getX(k).floatValue(),
                                (allThrustCurveData.getSeries(i).getY(k).floatValue() / 1000) * CONVERT);
                    }
                    thrustCurveData.addSeries(currentCurve);
                }

                if (i == 1) {
                    XYSeries currentCurve = new XYSeries(0);
                    float CONVERT = 1;
                    if (myBT.getAppConf().getUnitsPressure()== GlobalConfig.PressureUnits.PSI) {
                        //PSI
                        CONVERT = 1;
                    } else if (myBT.getAppConf().getUnitsPressure()== GlobalConfig.PressureUnits.BAR) {
                        //bar divide by 14.504
                        CONVERT = 1.0f / 14.504f;
                    } else if (myBT.getAppConf().getUnitsPressure()== GlobalConfig.PressureUnits.KPascal) {
                        //K pascal multiply by 6.895
                        CONVERT = (float) 6.895;
                    }
                    for (int k = 0; k < nbrData; k++) {
                        currentCurve.add(allThrustCurveData.getSeries(i).getX(k).floatValue(),
                                allThrustCurveData.getSeries(i).getY(k).floatValue() * CONVERT);
                    }
                    thrustCurveData.addSeries(currentCurve);
                }
                plot.setDataset(0, thrustCurveData);
            }
        }
    }

    public void zoomCurves() {
        XYSeriesCollection thrustCurveDataFinal = new XYSeriesCollection();
        XYSeriesCollection thrustCurveData = new XYSeriesCollection();
        thrustCurveData.addSeries(allThrustCurveData.getSeries(0));

        ThrustCurveViewTabActivity.ThrustUtil tu = new ThrustCurveViewTabActivity.ThrustUtil();
        double maxThrust = thrustCurveData.getSeries(0).getMaxY();
        double triggerThrust = maxThrust * (5.0 / 100.0);

        int curveStart = tu.searchX(thrustCurveData.getSeries(0), triggerThrust);
        int curveMaxThrust = tu.searchX(thrustCurveData.getSeries(0), maxThrust);
        int curveStop = tu.searchXFrom(thrustCurveData.getSeries(0), curveMaxThrust, triggerThrust);

        for (int i = 0; i < curvesNames.length; i++) {
            if (checkedItems[i]) {
                if (i == 0) {
                    if (curveStart != -1 && curveMaxThrust != -1 && curveStop != -1) {
                        XYSeries currentCurve = new XYSeries(0);
                        float CONVERT = 1;
                        if (myBT.getAppConf().getUnits()== GlobalConfig.ThrustUnits.KG) {
                            //kg
                            CONVERT = 1;
                        } else if (myBT.getAppConf().getUnits()== GlobalConfig.ThrustUnits.POUNDS) {
                            //pound
                            CONVERT = 2.20462f;
                        } else if (myBT.getAppConf().getUnits()== GlobalConfig.ThrustUnits.NEWTONS) {
                            //newton
                            CONVERT = 9.80665f;
                        }

                        for (int k = curveStart; k < curveStop; k++) {
                            currentCurve.add(allThrustCurveData.getSeries(i).getX(k).floatValue()
                                    - allThrustCurveData.getSeries(i).getX(curveStart).floatValue(),
                                    (allThrustCurveData.getSeries(i).getY(k).floatValue() / 1000) * CONVERT);
                        }
                        thrustCurveDataFinal.addSeries(currentCurve);
                    }
                }
                if(i == 1){
                    if (curveStart != -1 && curveMaxThrust != -1 && curveStop != -1) {
                        XYSeries currentCurve = new XYSeries(0);
                        float CONVERT = 1;
                        if (myBT.getAppConf().getUnitsPressure()== GlobalConfig.PressureUnits.PSI) {
                            //PSI
                            CONVERT = 1;
                        } else if (myBT.getAppConf().getUnitsPressure()== GlobalConfig.PressureUnits.BAR) {
                            //bar divide by 14.504
                            CONVERT = 1.0f / 14.504f;
                        } else if (myBT.getAppConf().getUnitsPressure()== GlobalConfig.PressureUnits.KPascal) {
                            //K pascal multiply by 6.895
                            CONVERT = (float) 6.895;
                        }

                        for (int k = curveStart; k < curveStop; k++) {
                            currentCurve.add(allThrustCurveData.getSeries(i).getX(k).floatValue()
                                    - allThrustCurveData.getSeries(i).getX(curveStart).floatValue(),
                                    (allThrustCurveData.getSeries(i).getY(k).floatValue() / 1000) * CONVERT);
                        }
                        thrustCurveDataFinal.addSeries(currentCurve);
                    }
                }
            }
            plot.setDataset(0, thrustCurveDataFinal);
        }
    }
}
