package com.rocketmotorteststand.ThrustCurve;


/**
 *   @description: Holds all the thrust curves downloaded from the test stand. Each curve is an
 *   XYSeriesCollection (time in ms on X) whose series are: thrust (grams), then, depending on
 *   the board, casing pressure, second casing pressure and filtered thrust.
 *   @author: boris.dureau@neuf.fr
 *
 **/
import android.content.Context;
import android.util.Log;

import com.rocketmotorteststand.BoardCapabilities;
import com.rocketmotorteststand.R;

import java.util.*;


import org.afree.data.xy.XYSeries;
import org.afree.data.xy.XYSeriesCollection;

public class ThrustCurveData {
    //context so that we can use the translations
    private final Context context;
    //board name, used to know which series each curve contains
    private final String testStandName;
    // Curves indexed by name. This used to be a static field reset by every constructor
    // call, so creating any new ThrustCurveData silently erased the downloaded curves.
    private Map<String, XYSeriesCollection> hm = new HashMap<>();

    public ThrustCurveData(Context current, String name) {
        this.context = current;
        this.testStandName = name;
    }

    public int getNbrOfThrustCurve() {
        return hm.size();
    }

    public String getThrustCurveName(int ThrustCurveNumber) {
        return null;
    }

    public List<String> getAllThrustCurveNames2() {
        return new ArrayList<>(hm.keySet());
    }

    public void ClearThrustCurve() {
        hm = new HashMap<>();
    }

    public XYSeriesCollection GetThrustCurveData(String thrustCurveName) {
        return hm.get(thrustCurveName);
    }

    public boolean ThrustCurveExist(String thrustCurveName) {
        return hm.containsKey(thrustCurveName);
    }

    public void AddToThrustCurve(long X, long Y, String thrustCurveName) {
        AddToThrustCurve(X, Y, thrustCurveName, BoardCapabilities.SERIES_THRUST);
    }

    /**
     * Appends one point to a series of a curve, creating the curve if needed. Points for a
     * series that this board model does not have are ignored instead of crashing.
     */
    public void AddToThrustCurve(long X, long Y, String thrustCurveName, int serie) {
        XYSeriesCollection thrustCurveData = hm.get(thrustCurveName);
        if (thrustCurveData == null) {
            thrustCurveData = createThrustCurve(thrustCurveName);
            hm.put(thrustCurveName, thrustCurveData);
        }
        if (serie < 0 || serie >= thrustCurveData.getSeriesCount())
            return;
        thrustCurveData.getSeries(serie).add(X, Y);
    }

    public void AddFlightData(XYSeriesCollection thrustCurveData, String thrustCurveName) {
        hm.put(thrustCurveName, thrustCurveData);
    }

    public void AddData(int thrustCurveNbr, int X, int Y) {
    }


    private XYSeriesCollection createThrustCurve(final String name) {
        XYSeriesCollection ret;
        //thrust
        final XYSeries series = new XYSeries(context.getResources().getString(R.string.curve_thrust));
        ret = new XYSeriesCollection(series);

        if (BoardCapabilities.hasPressure(testStandName)) {
            ret.addSeries(new XYSeries(context.getResources().getString(R.string.curve_pressure)));
            Log.d("numberOfCurves", "Adding curve pressure in data");
        }
        if (BoardCapabilities.hasPressure2(testStandName)) {
            ret.addSeries(new XYSeries(context.getString(R.string.curve_pressure_ch2)));
            Log.d("numberOfCurves", "Adding curve pressure2 in data");
            ret.addSeries(new XYSeries("Filtered thrust"));
        }
        return ret;
    }
}
