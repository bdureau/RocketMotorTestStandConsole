package com.rocketmotorteststand.ThrustCurve;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;

import com.rocketmotorteststand.ConsoleApplication;
import com.rocketmotorteststand.R;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;


import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.LayoutInflater;

import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import org.afree.data.xy.XYSeries;
import org.afree.data.xy.XYSeriesCollection;
import org.afree.graphics.geom.Font;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class ThrustCurveViewTabActivity extends AppCompatActivity {
    private ThrustCurveData mythrustCurve=null;
    private ViewPager mViewPager;
    SectionsPageAdapter adapter;
    private Tab1Fragment ThrustCurvePage1 = null;
    private Tab2Fragment ThrustCurvePage2 = null;
    private Button btnDismiss,butSelectCurves;
    private static ConsoleApplication myBT;

    private static String curvesNames[] = null;
    private static String currentCurvesNames[] =null;
    private static boolean[] checkedItems = null;
    private XYSeriesCollection allThrustCurveData=null;
    private static XYSeriesCollection thrustCurveData = null;
    private static ArrayList<ILineDataSet> dataSets;
    static int colors []= {Color.RED, Color.BLUE, Color.BLACK,
            Color.GREEN, Color.CYAN, Color.GRAY, Color.MAGENTA, Color.YELLOW,Color.RED,
            Color.BLUE, Color.BLACK,
            Color.GREEN, Color.CYAN, Color.GRAY, Color.MAGENTA, Color.YELLOW, Color.RED, Color.BLUE, Color.BLACK,
            Color.GREEN, Color.CYAN, Color.GRAY, Color.MAGENTA, Color.YELLOW};
    static Font font;
    private static String ThrustCurveName = null;

    private static String[] units= null;
    public static String SELECTED_THRUSTCURVE = "MyThrustCurve";
    int numberOfCurves =0;

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putStringArray("CURRENT_CURVES_NAMES_KEY", currentCurvesNames);
        outState.putBooleanArray("CHECKED_ITEMS_KEY",checkedItems);

    }
    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        currentCurvesNames = savedInstanceState.getStringArray("CURRENT_CURVES_NAMES_KEY");
        checkedItems = savedInstanceState.getBooleanArray("CHECKED_ITEMS_KEY");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // recovering the instance state
        if (savedInstanceState != null) {
            currentCurvesNames = savedInstanceState.getStringArray("CURRENT_CURVES_NAMES_KEY");
            checkedItems = savedInstanceState.getBooleanArray("CHECKED_ITEMS_KEY");
        }

        //get the bluetooth connection pointer
        myBT = (ConsoleApplication) getApplication();

        //Check the local and force it if needed
        //getApplicationContext().getResources().updateConfiguration(myBT.getAppLocal(), null);

        setContentView(R.layout.activity_thrustcurve_view_tab);
        mViewPager = (ViewPager) findViewById(R.id.container);


        btnDismiss = (Button) findViewById(R.id.butDismiss);

        butSelectCurves = (Button) findViewById(R.id.butSelectCurves);

            numberOfCurves=1;

        Intent newint = getIntent();
        ThrustCurveName = newint.getStringExtra(ThrustCurveListActivity.SELECTED_THRUSTCURVE);
        mythrustCurve = myBT.getThrustCurveData();
        // get all the data that we have recorded for the current thrustCurve
        allThrustCurveData=new XYSeriesCollection();
        allThrustCurveData = mythrustCurve.GetThrustCurveData(ThrustCurveName);

        // by default we will display the altitude
        // but then the user will be able to change the data
        thrustCurveData = new XYSeriesCollection();
        //altitude
        thrustCurveData.addSeries(allThrustCurveData.getSeries(getResources().getString(R.string.curve_thrust)));

        // get a list of all the curves that have been recorded
        //int numberOfCurves = allThrustCurveData.getSeries().size();

        Log.d("numberOfCurves", "numberOfCurves:"+allThrustCurveData.getSeries().size());
        curvesNames = new String[numberOfCurves];
        units = new String[numberOfCurves];
        for (int i = 0; i < numberOfCurves; i++) {
               curvesNames[i] = allThrustCurveData.getSeries(i).getKey().toString();
        }

        // Read the application config
        myBT.getAppConf().ReadConfig();
        if (myBT.getAppConf().getUnits().equals("0")) {
            //Meters
            units[0] = "(" + getResources().getString(R.string.Meters_fview) + ")";

        }
        else {
            //Feet
            units[0] = getResources().getString(R.string.Feet_fview);

        }

        if (currentCurvesNames == null) {
            //This is the first time so only display the altitude
            dataSets = new ArrayList<>();
            currentCurvesNames = new String[curvesNames.length];
            currentCurvesNames[0] =this.getResources().getString(R.string.curve_thrust);//"thrust";
            checkedItems = new boolean[curvesNames.length];
            checkedItems[0] = true;
        }
        setupViewPager(mViewPager);



        btnDismiss.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                allThrustCurveData=null;
                finish();      //exit the application configuration activity
            }
        });

        butSelectCurves.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int numberOfCurves = thrustCurveData.getSeries().size();
                currentCurvesNames = new String[numberOfCurves];

                for (int i = 0; i < numberOfCurves; i++) {
                    currentCurvesNames[i] = thrustCurveData.getSeries(i).getKey().toString();
                }
                // Set up the alert builder
                AlertDialog.Builder builder = new AlertDialog.Builder(ThrustCurveViewTabActivity.this);

                checkedItems = new boolean[curvesNames.length];
                // Add a checkbox list
                for (int i = 0; i < curvesNames.length; i++) {
                    if (Arrays.asList(currentCurvesNames).contains(curvesNames[i]))
                        checkedItems[i] = true;
                    else
                        checkedItems[i] = false;
                }


                builder.setMultiChoiceItems(curvesNames, checkedItems, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                        // The user checked or unchecked a box
                    }
                });
                // Add OK and Cancel buttons
                builder.setPositiveButton(getResources().getString(R.string.fv_ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // The user clicked OK
                        ThrustCurvePage1.drawGraph();
                        ThrustCurvePage1.drawAllCurves(allThrustCurveData);
                    }
                });
                //cancel
                builder.setNegativeButton(getResources().getString(R.string.fv_cancel), null);

                // Create and show the alert dialog
                AlertDialog dialog = builder.create();
                dialog.show();
            }

        });

    }

    private void setupViewPager(ViewPager viewPager) {
        adapter = new SectionsPageAdapter(getSupportFragmentManager());
        ThrustCurvePage1 = new Tab1Fragment(allThrustCurveData);
        ThrustCurvePage2 = new Tab2Fragment(mythrustCurve);

        adapter.addFragment(ThrustCurvePage1, "TAB1");
        adapter.addFragment(ThrustCurvePage2, "TAB2");

        viewPager.setAdapter(adapter);
    }

    public class SectionsPageAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList();
        private final List<String> mFragmentTitleList = new ArrayList();

        public void addFragment(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        public SectionsPageAdapter(FragmentManager fm) {
            super(fm);
        }


        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            return super.getPageTitle(position);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }
    }

    public static class Tab1Fragment extends Fragment {
        private LineChart mChart;
        public XYSeriesCollection allThrustCurveData;

        int graphBackColor, fontSize, axisColor, labelColor, nbrColor;
        public Tab1Fragment(XYSeriesCollection data) {
            this.allThrustCurveData =data;
        }
        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            View view = inflater.inflate(R.layout.tabthrustcurve_view_mp_fragment, container, false);

            mChart  = (LineChart) view.findViewById(R.id.linechart);

            mChart.setDragEnabled(true);
            mChart.setScaleEnabled(true);
            drawGraph();
            drawAllCurves(allThrustCurveData);

            return view;
        }
        private void drawGraph() {

            graphBackColor =myBT.getAppConf().ConvertColor(Integer.parseInt(myBT.getAppConf().getGraphBackColor()));


            fontSize = myBT.getAppConf().ConvertFont(Integer.parseInt(myBT.getAppConf().getFontSize()));

            axisColor=myBT.getAppConf().ConvertColor(Integer.parseInt(myBT.getAppConf().getGraphColor()));

            labelColor= Color.BLACK;

            nbrColor=Color.BLACK;

        }
        private void drawAllCurves(XYSeriesCollection allThrustCurveData) {
            dataSets.clear();

            thrustCurveData = new XYSeriesCollection();
            for (int i = 0; i < curvesNames.length; i++) {
                Log.d("drawAllCurves", "i:" +i);
                Log.d("drawAllCurves", "curvesNames:" +curvesNames[i]);
                if (checkedItems[i]) {
                    thrustCurveData.addSeries(allThrustCurveData.getSeries(curvesNames[i]));

                    int nbrData = allThrustCurveData.getSeries(i).getItemCount();

                    ArrayList<Entry> yValues = new ArrayList<>();

                    for (int k = 0; k < nbrData; k++) {
                        yValues.add(new Entry(allThrustCurveData.getSeries(i).getX(k).floatValue(), allThrustCurveData.getSeries(i).getY(k).floatValue()));
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
    }

    /*
    This is the thrustCurve information tab
     */
    public static class Tab2Fragment extends Fragment {

        private ThrustCurveData mythrustCurve;

        private TextView nbrOfSamplesValue, thrustCurveNbrValue;
        private TextView apogeeAltitudeValue, thrustCurveDurationValue, burnTimeValue, maxVelociyValue, maxAccelerationValue;

        public Tab2Fragment (ThrustCurveData data) {
            mythrustCurve = data;
        }
        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            View view = inflater.inflate(R.layout.tabthrustcurve_info_fragment, container, false);

            //apogeeAltitudeValue = view.findViewById(R.id.apogeeAltitudeValue);
            thrustCurveDurationValue = view.findViewById(R.id.thrustCurveDurationValue);
            burnTimeValue = view.findViewById(R.id.burnTimeValue);
            maxVelociyValue = view.findViewById(R.id.maxVelociyValue);
            maxAccelerationValue = view.findViewById(R.id.maxAccelerationValue);

            nbrOfSamplesValue= view.findViewById(R.id.nbrOfSamplesValue);
            thrustCurveNbrValue = view.findViewById(R.id.thrustCurveNbrValue);

            XYSeriesCollection thrustCurveData;

            thrustCurveData = mythrustCurve.GetThrustCurveData(ThrustCurveName);
            int nbrData = thrustCurveData.getSeries(0).getItemCount();

            // ThrustCurve nbr
            thrustCurveNbrValue.setText(ThrustCurveName + "");

            //nbr of samples
            nbrOfSamplesValue.setText(nbrData +"");

            //ThrustCurve duration
            double thrustCurveDuration = thrustCurveData.getSeries(0).getMaxX()/1000;
            thrustCurveDurationValue.setText(thrustCurveDuration +" secs");

            return view;
        }

        /*
        Return the position of the first X value it finds from the beginning
         */
        public int searchX (XYSeries serie, double searchVal) {
            int nbrData = serie.getItemCount();
            int pos = -1;
            for (int i = 1; i < nbrData; i++) {
                if((searchVal >= serie.getY(i-1).doubleValue()  )&& (searchVal <= serie.getY(i).doubleValue() )) {
                    pos =i;
                    break;
                }
            }
            return pos;
        }

        /*
        Return the position of the first Y value it finds from the beginning
         */
        public int searchY (XYSeries serie, double searchVal) {
            int nbrData = serie.getItemCount();
            int pos = -1;
            for (int i = 1; i < nbrData; i++) {
                if((searchVal >= serie.getX(i-1).doubleValue()  )&& (searchVal <= serie.getX(i).doubleValue() )) {
                    pos =i;
                    break;
                }
            }
            return pos;
        }
    }
}