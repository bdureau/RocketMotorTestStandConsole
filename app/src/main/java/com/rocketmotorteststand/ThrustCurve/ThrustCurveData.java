package com.rocketmotorteststand.ThrustCurve;


/**
 *   @description: This class has all the thrust Curve arrays and methods to add or remove thrust Curve data
 *   @author: boris.dureau@neuf.fr
 *
 **/
import android.content.Context;
import android.util.Log;

import com.rocketmotorteststand.R;

import java.util.*;


import org.afree.data.xy.XYSeries;
import org.afree.data.xy.XYSeriesCollection;

public class ThrustCurveData {
    //context so that we can use the translations
    private Context context;
    //pass the test stand  name so that we can do specific
    private String testStandName;
    // Create a hash map
    public static HashMap<String,XYSeriesCollection > hm;// = new HashMap<String,XYSeriesCollection>();
    public ThrustCurveData(Context current, String name)
    {
        this.context = current;
        this.testStandName = name;
        hm = new HashMap<String,XYSeriesCollection>();
    }
    //this might be a usefull function that I will write later
    public int getNbrOfThrustCurve()
    {
        return hm.entrySet().size();
    }

    public String getThrustCurveName(int ThrustCurveNumber)
    {
        String thrustCurveName = null;
        return thrustCurveName;
    }


    public List<String> getAllThrustCurveNames2()
    {
        List<String> thrustCurvesNames = new ArrayList<String>();

        Set set = hm.entrySet();
        // Get an iterator
        Iterator i = set.iterator();

        // Display elements
        while(i.hasNext()) {
            Map.Entry me = (Map.Entry)i.next();
            thrustCurvesNames.add((String)me.getKey());
        }
        return thrustCurvesNames;
    }

    public void ClearThrustCurve()
    {
        hm =null;
        hm = new HashMap();
    }

    public XYSeriesCollection  GetThrustCurveData(String thrustCurveName)
    {
        XYSeriesCollection  thrustCurveData=null;

        Set set = hm.entrySet();
        // Get an iterator
        Iterator i = set.iterator();


        // Display elements
        while(i.hasNext()) {
            Map.Entry me = (Map.Entry)i.next();
            if (me.getKey().equals(thrustCurveName))
                thrustCurveData= (XYSeriesCollection ) me.getValue();
        }
        return thrustCurveData;
    }
    public boolean ThrustCurveExist(String thrustCurveName)
    {
        boolean exist = false;

        Set set = hm.entrySet();
        // Get an iterator
        Iterator i = set.iterator();

        // Display elements
        while(i.hasNext()) {
            Map.Entry me = (Map.Entry)i.next();

            if (me.getKey().equals(thrustCurveName))
                exist=true;
        }

        return exist;
    }
    public void AddToThrustCurve (long X, long Y, String thrustCurveName)
    {
        //Find out if the flight exist
        //If it exist append the data to the flight and if not create a new flight
        XYSeriesCollection  thrustCurveData=null;
        if (!ThrustCurveExist(thrustCurveName))
        {
            //if the flight name does not exist let's create it first
            hm.put(thrustCurveName, createThrustCurve(thrustCurveName));
        }
        thrustCurveData = GetThrustCurveData(thrustCurveName);
        thrustCurveData.getSeries(0).add(X, Y);
    }
    public void AddToThrustCurve (long X, long Y, String thrustCurveName, int serie)
    {
        //Find out if the flight exist
        //If it exist append the data to the flight and if not create a new flight
        XYSeriesCollection  thrustCurveData=null;
        if (!ThrustCurveExist(thrustCurveName))
        {
            //if the flight name does not exist let's create it first
            hm.put(thrustCurveName, createThrustCurve(thrustCurveName));
        }

        thrustCurveData = GetThrustCurveData(thrustCurveName);
        thrustCurveData.getSeries(serie).add(X, Y);
    }
    //not sure that I will be using that one
    public void AddFlightData (XYSeriesCollection  thrustCurveData, String thrustCurveName)
    {
        hm.put(thrustCurveName, thrustCurveData);
    }

    public void AddData (int thrustCurveNbr , int X, int Y )
    {

    }


    private XYSeriesCollection  createThrustCurve(final String name) {
        XYSeriesCollection ret;
        //thrust
        final XYSeries series = new XYSeries(context.getResources().getString(R.string.curve_thrust)) ;
        ret = new XYSeriesCollection (series);

        if(testStandName.equals("TestStandSTM32V2") || testStandName.equals("TestStandESP32") ||
                testStandName.equals("TestStandSTM32V3") || testStandName.equals("TestStandESP32V3")) {
            ret.addSeries(new XYSeries(context.getResources().getString(R.string.curve_pressure)));
            Log.d("numberOfCurves", "Adding curve pressure in data" );
        }
        if(testStandName.equals("TestStandSTM32V3") || testStandName.equals("TestStandESP32V3")) {
            ret.addSeries(new XYSeries(context.getString(R.string.curve_pressure_ch2)));
            Log.d("numberOfCurves", "Adding curve pressure2 in data" );
        }
        return ret;
    }
}
