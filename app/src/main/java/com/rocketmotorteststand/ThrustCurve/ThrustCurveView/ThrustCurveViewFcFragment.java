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
import com.rocketmotorteststand.BoardCapabilities;
import com.rocketmotorteststand.ConsoleApplication;
import com.rocketmotorteststand.R;
import com.rocketmotorteststand.UnitsHelper;
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

/**
 * @description: Thrust curve chart tab drawn with the AFreeChart library (used on Android
 * versions older than 8 when selected in the settings). Shows the selected series over the
 * whole recording or zoomed on the burn.
 * @author: boris.dureau@neuf.fr
 **/
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


    /**
     * Empty constructor required by Android to recreate the fragment. A recreated instance
     * is discarded by the host activity (see FragmentRecreationHelper).
     */
    public ThrustCurveViewFcFragment() {
    }

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

        View view = inflater.inflate(R.layout.fragment_thrustcurve_view_fc, container, false);
        chartView = (ChartView) view.findViewById(R.id.chartView1);

        String myUnits = UnitsHelper.thrustLabel(getContext(), myBT.getAppConf());

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

    /**
     * Converts one series into the units chosen in the settings.
     *
     * @param serieIndex index of the series (thrust, pressure, pressure 2, filtered thrust).
     *                   The filtered thrust used to be converted as a pressure by mistake.
     * @param from       first sample index (inclusive)
     * @param to         last sample index (exclusive)
     * @param timeOffset time subtracted from each sample (ms), used by the zoomed view
     */
    private XYSeries convertSerie(XYSeries serie, int serieIndex, int from, int to, double timeOffset) {
        XYSeries currentCurve = new XYSeries(curvesNames[serieIndex] + " (" + units[serieIndex] + ")");
        double factor = UnitsHelper.seriesFactor(myBT.getAppConf(), serieIndex);
        // bounded by the size of each series (they may differ if a frame was lost)
        int end = Math.min(to, serie.getItemCount());
        for (int k = Math.max(0, from); k < end; k++) {
            currentCurve.add(serie.getX(k).doubleValue() - timeOffset,
                    serie.getY(k).doubleValue() * factor);
        }
        return currentCurve;
    }

    /**
     * Displays the selected curves over the whole recording.
     */
    public void drawAllCurves(XYSeriesCollection allThrustCurveData) {
        XYSeriesCollection thrustCurveData = new XYSeriesCollection();
        int count = Math.min(curvesNames.length, allThrustCurveData.getSeriesCount());
        for (int i = 0; i < count; i++) {
            if (checkedItems[i]) {
                XYSeries serie = allThrustCurveData.getSeries(i);
                thrustCurveData.addSeries(convertSerie(serie, i, 0, serie.getItemCount(), 0));
            }
        }
        plot.setDataset(0, thrustCurveData);
    }

    /**
     * Displays the selected curves during the motor burn only (from 5% of the maximum thrust
     * to 5% of the maximum thrust after the peak), with the time starting at 0.
     */
    public void zoomCurves() {
        XYSeriesCollection thrustCurveDataFinal = new XYSeriesCollection();
        XYSeries thrust = allThrustCurveData.getSeries(BoardCapabilities.SERIES_THRUST);

        ThrustCurveViewTabActivity.ThrustUtil tu = new ThrustCurveViewTabActivity.ThrustUtil();
        double maxThrust = thrust.getMaxY();
        double triggerThrust = maxThrust * (5.0 / 100.0);

        int curveStart = tu.searchX(thrust, triggerThrust);
        int curveMaxThrust = tu.searchX(thrust, maxThrust);
        int curveStop = tu.searchXFrom(thrust, curveMaxThrust, triggerThrust);

        if (curveStart != -1 && curveMaxThrust != -1 && curveStop != -1) {
            double timeOffset = thrust.getX(curveStart).doubleValue();
            int count = Math.min(curvesNames.length, allThrustCurveData.getSeriesCount());
            for (int i = 0; i < count; i++) {
                if (checkedItems[i]) {
                    thrustCurveDataFinal.addSeries(convertSerie(allThrustCurveData.getSeries(i), i,
                            curveStart, curveStop, timeOffset));
                }
            }
        }
        plot.setDataset(0, thrustCurveDataFinal);
    }
}
