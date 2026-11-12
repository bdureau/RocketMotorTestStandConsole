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
import com.rocketmotorteststand.BoardCapabilities;
import com.rocketmotorteststand.ConsoleApplication;
import com.rocketmotorteststand.R;
import com.rocketmotorteststand.UnitsHelper;
import com.rocketmotorteststand.ThrustCurve.ThrustCurveViewTabActivity;

import org.afree.data.xy.XYSeries;
import org.afree.data.xy.XYSeriesCollection;

import java.util.ArrayList;

/**
 * @description: Thrust curve chart tab drawn with the MPAndroidChart library. Shows the selected
 * series (thrust, pressures, filtered thrust) over the whole recording or zoomed on the burn.
 * @author: boris.dureau@neuf.fr
 **/
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

    /**
     * Empty constructor required by Android to recreate the fragment. A recreated instance
     * is discarded by the host activity (see FragmentRecreationHelper).
     */
    public ThrustCurveViewMpFragment() {
    }

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

    /**
     * Converts one series into chart entries in the units chosen in the settings.
     *
     * @param serieIndex index of the series (thrust, pressure, pressure 2, filtered thrust)
     * @param from       first sample index (inclusive)
     * @param to         last sample index (exclusive)
     * @param timeOffset time subtracted from each sample (ms), used by the zoomed view
     */
    private ArrayList<Entry> toEntries(XYSeries serie, int serieIndex, int from, int to, float timeOffset) {
        ArrayList<Entry> yValues = new ArrayList<>();
        float factor = (float) UnitsHelper.seriesFactor(myBT.getAppConf(), serieIndex);
        // Each series is bounded by its own size: they may not all have the same number of
        // samples if a frame was lost, which used to crash with an IndexOutOfBoundsException
        int end = Math.min(to, serie.getItemCount());
        for (int k = Math.max(0, from); k < end; k++) {
            yValues.add(new Entry(serie.getX(k).floatValue() - timeOffset,
                    serie.getY(k).floatValue() * factor));
        }
        return yValues;
    }

    /**
     * Adds a line for the given entries to the chart data sets.
     */
    private void addDataSet(ArrayList<Entry> yValues, int i) {
        LineDataSet set1 = new LineDataSet(yValues, getResources().getString(R.string.unit_time));
        set1.setColor(colors[i]);
        set1.setDrawValues(false);
        set1.setDrawCircles(false);
        set1.setLabel(curvesNames[i] + " (" + units[i] + ")");
        set1.setValueTextColor(labelColor);
        set1.setValueTextSize(fontSize);
        dataSets.add(set1);
    }

    /**
     * Displays the selected curves over the whole recording.
     */
    public void drawAllCurves(XYSeriesCollection allThrustCurveData) {
        dataSets = new ArrayList<>();

        int count = Math.min(curvesNames.length, allThrustCurveData.getSeriesCount());
        for (int i = 0; i < count; i++) {
            if (checkedItems[i]) {
                XYSeries serie = allThrustCurveData.getSeries(i);
                addDataSet(toEntries(serie, i, 0, serie.getItemCount(), 0f), i);
            }
        }
        showDataSets();
    }

    /**
     * Displays the selected curves during the motor burn only (from 5% of the maximum thrust
     * to 5% of the maximum thrust after the peak), with the time starting at 0.
     */
    public void zoomCurves() {
        dataSets = new ArrayList<>();
        XYSeries thrust = allThrustCurveData.getSeries(BoardCapabilities.SERIES_THRUST);

        ThrustCurveViewTabActivity.ThrustUtil tu = new ThrustCurveViewTabActivity.ThrustUtil();
        double maxThrust = thrust.getMaxY();
        double triggerThrust = maxThrust * (5.0 / 100.0);

        int curveStart = tu.searchX(thrust, triggerThrust);
        int curveMaxThrust = tu.searchX(thrust, maxThrust);
        int curveStop = tu.searchXFrom(thrust, curveMaxThrust, triggerThrust);

        if (curveStart != -1 && curveMaxThrust != -1 && curveStop != -1) {
            float timeOffset = thrust.getX(curveStart).floatValue();
            int count = Math.min(curvesNames.length, allThrustCurveData.getSeriesCount());
            for (int i = 0; i < count; i++) {
                if (checkedItems[i]) {
                    addDataSet(toEntries(allThrustCurveData.getSeries(i), i,
                            curveStart, curveStop, timeOffset), i);
                }
            }
        }
        showDataSets();
    }

    private void showDataSets() {
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
