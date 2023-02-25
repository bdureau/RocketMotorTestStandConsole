package com.rocketmotorteststand.ThrustCurve.ThrustCurveView;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.rocketmotorteststand.ConsoleApplication;
import com.rocketmotorteststand.GlobalConfig;
import com.rocketmotorteststand.R;
import com.rocketmotorteststand.ThrustCurve.ThrustCurveViewTabActivity;

import org.afree.data.xy.XYSeriesCollection;

import java.util.ArrayList;

public class ThrustCurveViewMpFragment extends Fragment {
    private LineChart mChart;
    public XYSeriesCollection allThrustCurveData;
    private ConsoleApplication myBT;
    int graphBackColor, fontSize, axisColor, labelColor, nbrColor;
    private ArrayList<ILineDataSet> dataSets;

    private String[] units = null;
    private String curvesNames[] = null;
    private boolean[] checkedItems = null;

    static int colors[] = {Color.RED, Color.BLUE, Color.BLACK,
            Color.GREEN, Color.CYAN, Color.GRAY, Color.MAGENTA, Color.YELLOW, Color.RED,
            Color.BLUE, Color.BLACK,
            Color.GREEN, Color.CYAN, Color.GRAY, Color.MAGENTA, Color.YELLOW, Color.RED, Color.BLUE, Color.BLACK,
            Color.GREEN, Color.CYAN, Color.GRAY, Color.MAGENTA, Color.YELLOW};

    private XYSeriesCollection thrustCurveData = null;

    public ThrustCurveViewMpFragment(XYSeriesCollection pAllThrustCurveData,
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
        dataSets = new ArrayList<>();
        View view = inflater.inflate(R.layout.fragment_thrustcurve_view_mp, container, false);

        mChart = (LineChart) view.findViewById(R.id.linechart);
        mChart.setDragEnabled(true);
        mChart.setScaleEnabled(true);
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
    }

    public void drawAllCurves(XYSeriesCollection allThrustCurveData) {
        dataSets = new ArrayList<>();
        dataSets.clear();

        for (int i = 0; i < curvesNames.length; i++) {
            Log.d("drawAllCurves", "i:" + i);
            Log.d("drawAllCurves", "curvesNames:" + curvesNames[i]);
            if (checkedItems[i]) {
                int nbrData = allThrustCurveData.getSeries(i).getItemCount();
                ArrayList<Entry> yValues = new ArrayList<>();

                Log.d("drawAllCurves", "i:" + i);
                if (i == 0) {
                    float CONVERT =1.0f;
                    if (myBT.getAppConf().getUnits()== GlobalConfig.ThrustUnits.KG) {
                        //kg
                        CONVERT =1.0f;
                    } else if (myBT.getAppConf().getUnits()== GlobalConfig.ThrustUnits.POUNDS) {
                        //pound
                        CONVERT =(float) 2.20462;
                    } else if (myBT.getAppConf().getUnits()== GlobalConfig.ThrustUnits.NEWTONS) {
                        //newton
                        CONVERT =(float) 9.80665;
                    }
                    for (int k = 0; k < nbrData; k++) {
                        yValues.add(new Entry(allThrustCurveData.getSeries(i).getX(k).floatValue(),
                                (allThrustCurveData.getSeries(i).getY(k).floatValue() / 1000) * CONVERT));
                    }
                }

                if (i == 1) {
                    float CONVERT_PRESSURE = 1.0f;
                    if (myBT.getAppConf().getUnitsPressure()== GlobalConfig.PressureUnits.PSI) {
                        //PSI
                        CONVERT_PRESSURE = 1.0f;
                    } else if (myBT.getAppConf().getUnitsPressure()== GlobalConfig.PressureUnits.BAR) {
                        //bar divide by 14.504
                        CONVERT_PRESSURE =  1.0f/14.504f;
                    } else if (myBT.getAppConf().getUnitsPressure()== GlobalConfig.PressureUnits.KPascal) {
                        //K pascal multiply by 6.895
                        CONVERT_PRESSURE = (float) 6.895;
                    }
                    for (int k = 0; k < nbrData; k++) {
                        yValues.add(new Entry(allThrustCurveData.getSeries(i).getX(k).floatValue(),
                                allThrustCurveData.getSeries(i).getY(k).floatValue() * CONVERT_PRESSURE));
                    }
                }

                LineDataSet set1 = new LineDataSet(yValues, "Time");
                set1.setColor(colors[i]);

                set1.setDrawValues(false);
                set1.setDrawCircles(false);
                set1.setLabel(curvesNames[i] + " " + units[i]);
                set1.setValueTextColor(labelColor);

                set1.setValueTextSize(fontSize);
                dataSets.add(set1);
            }
        }

        LineData data = new LineData(dataSets);
        mChart.clear();
        mChart.setData(data);
        mChart.setBackgroundColor(graphBackColor);

        Description desc = new Description();
        //time (ms)
        desc.setText(getResources().getString(R.string.unit_time));
        mChart.setDescription(desc);
    }

    public void zoomCurves() {
        dataSets.clear();

        thrustCurveData = new XYSeriesCollection();
        thrustCurveData.addSeries(allThrustCurveData.getSeries(0));

        ThrustCurveViewTabActivity.ThrustUtil tu = new ThrustCurveViewTabActivity.ThrustUtil();
        double maxThrust = thrustCurveData.getSeries(0).getMaxY();
        double triggerThrust = maxThrust * (5.0 / 100.0);

        int curveStart = tu.searchX(thrustCurveData.getSeries(0), triggerThrust);
        int curveMaxThrust = tu.searchX(thrustCurveData.getSeries(0), maxThrust);
        int curveStop = tu.searchXFrom(thrustCurveData.getSeries(0), curveMaxThrust, triggerThrust);

        for (int i = 0; i < curvesNames.length; i++) {
            if (checkedItems[i]) {
                ArrayList<Entry> yValues = new ArrayList<>();
                if (i == 0) {
                    if (curveStart != -1 && curveMaxThrust != -1 && curveStop != -1) {
                        float CONVERT = 1;
                        if (myBT.getAppConf().getUnits() == GlobalConfig.ThrustUnits.KG) {
                            //kg
                            CONVERT = 1;
                        } else if (myBT.getAppConf().getUnits()==GlobalConfig.ThrustUnits.POUNDS) {
                            //pound
                            CONVERT = 2.20462f;
                        } else if (myBT.getAppConf().getUnits()==GlobalConfig.ThrustUnits.NEWTONS) {
                            //newton
                            CONVERT = 9.80665f;
                        }
                        for (int k = curveStart; k < curveStop; k++) {
                            yValues.add(new Entry(allThrustCurveData.getSeries(i).getX(k).floatValue()
                                    - allThrustCurveData.getSeries(i).getX(curveStart).floatValue(),
                                    (allThrustCurveData.getSeries(i).getY(k).floatValue() / 1000) * CONVERT));
                        }
                    }
                }
                if (i == 1) {
                    if (curveStart != -1 && curveMaxThrust != -1 && curveStop != -1) {
                        float CONVERT_PRESSURE = 1.0f;
                        if (myBT.getAppConf().getUnitsPressure()== GlobalConfig.PressureUnits.PSI) {
                            //PSI
                            CONVERT_PRESSURE = 1.0f;
                        } else if (myBT.getAppConf().getUnitsPressure()== GlobalConfig.PressureUnits.BAR) {
                            //bar divide by 14.504
                            CONVERT_PRESSURE = 1.0f / 14.504f;
                        } else if (myBT.getAppConf().getUnitsPressure()== GlobalConfig.PressureUnits.KPascal) {
                            //K pascal multiply by 6.895
                            CONVERT_PRESSURE = (float) 6.895;
                        }
                        for (int k = curveStart; k < curveStop; k++) {
                            yValues.add(new Entry(allThrustCurveData.getSeries(i).getX(k).floatValue()
                                    - allThrustCurveData.getSeries(i).getX(curveStart).floatValue(),
                                    (allThrustCurveData.getSeries(i).getY(k).floatValue() ) * CONVERT_PRESSURE));
                        }
                    }
                }

                LineDataSet set1 = new LineDataSet(yValues, getResources().getString(R.string.unit_time));
                set1.setColor(colors[i]);

                set1.setDrawValues(false);
                set1.setDrawCircles(false);
                set1.setLabel(curvesNames[i] + " " + units[i]);
                set1.setValueTextColor(labelColor);

                set1.setValueTextSize(fontSize);
                dataSets.add(set1);
            }
            LineData data = new LineData(dataSets);
            mChart.clear();
            mChart.setData(data);
            mChart.setBackgroundColor(graphBackColor);

            Description desc = new Description();
            //time (ms)
            desc.setText(getResources().getString(R.string.unit_time));
            mChart.setDescription(desc);
        }
    }
}
