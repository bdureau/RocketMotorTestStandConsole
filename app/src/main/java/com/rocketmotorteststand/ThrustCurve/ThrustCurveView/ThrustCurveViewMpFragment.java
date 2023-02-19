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
        graphBackColor = myBT.getAppConf().ConvertColor(Integer.parseInt(myBT.getAppConf().getGraphBackColor()));
        fontSize = myBT.getAppConf().ConvertFont(Integer.parseInt(myBT.getAppConf().getFontSize()));
        axisColor = myBT.getAppConf().ConvertColor(Integer.parseInt(myBT.getAppConf().getGraphColor()));
        labelColor = Color.BLACK;
        nbrColor = Color.BLACK;
    }

    public void drawAllCurves(XYSeriesCollection allThrustCurveData) {
        dataSets = new ArrayList<>();
        dataSets.clear();

        //thrustCurveData = new XYSeriesCollection();
        for (int i = 0; i < curvesNames.length; i++) {
            Log.d("drawAllCurves", "i:" + i);
            Log.d("drawAllCurves", "curvesNames:" + curvesNames[i]);
            if (checkedItems[i]) {
                //thrustCurveData.addSeries(allThrustCurveData.getSeries(curvesNames[i]));

                int nbrData = allThrustCurveData.getSeries(i).getItemCount();

                ArrayList<Entry> yValues = new ArrayList<>();

                Log.d("drawAllCurves", "i:" + i);
                if (i == 0) {
                    for (int k = 0; k < nbrData; k++) {
                        if (myBT.getAppConf().getUnits().equals("0")) {
                            //kg
                            yValues.add(new Entry(allThrustCurveData.getSeries(i).getX(k).floatValue(), allThrustCurveData.getSeries(i).getY(k).floatValue() / 1000));
                        } else if (myBT.getAppConf().getUnits().equals("1")) {
                            //pound
                            yValues.add(new Entry(allThrustCurveData.getSeries(i).getX(k).floatValue(), (allThrustCurveData.getSeries(i).getY(k).floatValue() / 1000) * (float) 2.20462));
                        } else if (myBT.getAppConf().getUnits().equals("2")) {
                            //newton
                            yValues.add(new Entry(allThrustCurveData.getSeries(i).getX(k).floatValue(), (allThrustCurveData.getSeries(i).getY(k).floatValue() / 1000) * (float) 9.80665));
                        }
                    }
                }

                if (i == 1) {
                    for (int k = 0; k < nbrData; k++) {
                        if (myBT.getAppConf().getUnitsPressure().equals("0")) {
                            //PSI
                            yValues.add(new Entry(allThrustCurveData.getSeries(i).getX(k).floatValue(), allThrustCurveData.getSeries(i).getY(k).floatValue()));
                            Log.d("Thrust curve List", "val pressure:" + allThrustCurveData.getSeries(i).getY(k).floatValue());
                        } else if (myBT.getAppConf().getUnits().equals("1")) {
                            //bar divide by 14.504
                            yValues.add(new Entry(allThrustCurveData.getSeries(i).getX(k).floatValue(), allThrustCurveData.getSeries(i).getY(k).floatValue() / (float) 14.504));
                        } else if (myBT.getAppConf().getUnits().equals("2")) {
                            //K pascal multiply by 6.895
                            yValues.add(new Entry(allThrustCurveData.getSeries(i).getX(k).floatValue(), allThrustCurveData.getSeries(i).getY(k).floatValue() * (float) 6.895));
                        }
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

        ArrayList<Entry> yValues = new ArrayList<>();
        if (curveStart != -1 && curveMaxThrust != -1 && curveStop != -1) {
            for (int k = curveStart; k < curveStop; k++) {
                if (myBT.getAppConf().getUnits().equals("0")) {
                    //kg
                    yValues.add(new Entry(allThrustCurveData.getSeries(0).getX(k).floatValue() - allThrustCurveData.getSeries(0).getX(curveStart).floatValue(), allThrustCurveData.getSeries(0).getY(k).floatValue() / 1000));
                } else if (myBT.getAppConf().getUnits().equals("1")) {
                    //pound
                    yValues.add(new Entry(allThrustCurveData.getSeries(0).getX(k).floatValue() - allThrustCurveData.getSeries(0).getX(curveStart).floatValue(), (allThrustCurveData.getSeries(0).getY(k).floatValue() / 1000) * (float) 2.20462));
                } else if (myBT.getAppConf().getUnits().equals("2")) {
                    //newton
                    yValues.add(new Entry(allThrustCurveData.getSeries(0).getX(k).floatValue() - allThrustCurveData.getSeries(0).getX(curveStart).floatValue(), (allThrustCurveData.getSeries(0).getY(k).floatValue() / 1000) * (float) 9.80665));
                }
            }

            LineDataSet set1 = new LineDataSet(yValues, getResources().getString(R.string.unit_time));
            set1.setColor(colors[0]);

            set1.setDrawValues(false);
            set1.setDrawCircles(false);
            set1.setLabel(curvesNames[0] + " " + units[0]);
            set1.setValueTextColor(labelColor);

            set1.setValueTextSize(fontSize);
            dataSets.add(set1);


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
