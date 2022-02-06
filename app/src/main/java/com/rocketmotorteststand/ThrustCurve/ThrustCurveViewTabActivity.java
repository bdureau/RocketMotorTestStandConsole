package com.rocketmotorteststand.ThrustCurve;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;

import com.rocketmotorteststand.ConsoleApplication;
import com.rocketmotorteststand.Help.AboutActivity;
import com.rocketmotorteststand.Help.HelpActivity;
import com.rocketmotorteststand.R;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.rocketmotorteststand.ShareHandler;


import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import org.afree.data.xy.XYSeries;
import org.afree.data.xy.XYSeriesCollection;
import org.afree.graphics.geom.Font;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class ThrustCurveViewTabActivity extends AppCompatActivity {
    private ThrustCurveData mythrustCurve = null;
    private ViewPager mViewPager;
    SectionsPageAdapter adapter;
    private Tab1Fragment ThrustCurvePage1 = null;
    private Tab2Fragment ThrustCurvePage2 = null;
    private Button btnDismiss, butSelectCurves, butZoom;
    private static ConsoleApplication myBT;


    private static String curvesNames[] = null;
    private static String currentCurvesNames[] = null;
    private static boolean[] checkedItems = null;
    private XYSeriesCollection allThrustCurveData = null;
    private static XYSeriesCollection thrustCurveData = null;
    private static ArrayList<ILineDataSet> dataSets;
    static int colors[] = {Color.RED, Color.BLUE, Color.BLACK,
            Color.GREEN, Color.CYAN, Color.GRAY, Color.MAGENTA, Color.YELLOW, Color.RED,
            Color.BLUE, Color.BLACK,
            Color.GREEN, Color.CYAN, Color.GRAY, Color.MAGENTA, Color.YELLOW, Color.RED, Color.BLUE, Color.BLACK,
            Color.GREEN, Color.CYAN, Color.GRAY, Color.MAGENTA, Color.YELLOW};
    static Font font;
    private static String ThrustCurveName = null;

    private static String[] units = null;
    public static String SELECTED_THRUSTCURVE = "MyThrustCurve";
    int numberOfCurves = 0;
    private static double CONVERT = 1;

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
        outState.putBooleanArray("CHECKED_ITEMS_KEY", checkedItems);

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

        if (Build.VERSION.SDK_INT >= 23) {
            int REQUEST_CODE_ASK_PERMISSIONS = 123;
            int hasWriteContactsPermission = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (hasWriteContactsPermission != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_CODE_ASK_PERMISSIONS);

            }
        }
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

        butZoom = (Button) findViewById(R.id.butZoom);
        numberOfCurves = 1;
        if(myBT.getTestStandConfigData().getTestStandName().equals("TestStandSTM32V2")) {
            numberOfCurves =2;
        }

        Intent newint = getIntent();
        ThrustCurveName = newint.getStringExtra(ThrustCurveListActivity.SELECTED_THRUSTCURVE);
        mythrustCurve = myBT.getThrustCurveData();
        // get all the data that we have recorded for the current thrustCurve
        allThrustCurveData = new XYSeriesCollection();
        allThrustCurveData = mythrustCurve.GetThrustCurveData(ThrustCurveName);

        // by default we will display the thrust
        // but then the user will be able to change the data
        thrustCurveData = new XYSeriesCollection();
        //thrust
        thrustCurveData.addSeries(allThrustCurveData.getSeries(getResources().getString(R.string.curve_thrust)));

        if(myBT.getTestStandConfigData().getTestStandName().equals("TestStandSTM32V2")) {
            Log.d("numberOfCurves", "Adding curve pressure" );
            thrustCurveData.addSeries(allThrustCurveData.getSeries(getResources().getString(R.string.curve_pressure)));

        }

        Log.d("numberOfCurves", "testStandName:" + myBT.getTestStandConfigData().getTestStandName());
        // get a list of all the curves that have been recorded
        //int numberOfCurves = allThrustCurveData.getSeries().size();

        Log.d("numberOfCurves", "numberOfCurves:" + allThrustCurveData.getSeries().size());
        curvesNames = new String[numberOfCurves];
        units = new String[numberOfCurves];
        for (int i = 0; i < numberOfCurves; i++) {
            curvesNames[i] = allThrustCurveData.getSeries(i).getKey().toString();
            Log.d("numberOfCurves",allThrustCurveData.getSeries(i).getKey().toString());
        }

        // Read the application config
        myBT.getAppConf().ReadConfig();
        if (myBT.getAppConf().getUnits().equals("0")) {
            //kg
            units[0] = "(" + getResources().getString(R.string.Kg_fview) + ")";
            CONVERT = 1.0/1000;

        } else if (myBT.getAppConf().getUnits().equals("1")) {
            //pounds
            units[0] = getResources().getString(R.string.Pounds_fview);
            CONVERT = 2.20462 / 1000;

        } else if (myBT.getAppConf().getUnits().equals("2")) {
            //newtons
            units[0] = getResources().getString(R.string.Newtons_fview);
            CONVERT = 9.80665 / 1000;
        }

        if (numberOfCurves > 1) {
            if (myBT.getAppConf().getUnitsPressure().equals("0")) {
                //PSI
                units[1] = "(" + "PSI" + ")";


            } else if (myBT.getAppConf().getUnits().equals("1")) {
                //BAR
                units[1] = "BAR";


            } else if (myBT.getAppConf().getUnits().equals("2")) {
                //Kpascal
                units[1] = "Kpascal";

            }
        }

        if (currentCurvesNames == null) {
            //This is the first time so only display the thrust
            dataSets = new ArrayList<>();
            currentCurvesNames = new String[curvesNames.length];
            currentCurvesNames[0] = this.getResources().getString(R.string.curve_thrust);
            checkedItems = new boolean[curvesNames.length];
            checkedItems[0] = true;
        }
        setupViewPager(mViewPager);


        btnDismiss.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                allThrustCurveData = null;
                finish();      //exit the application configuration activity
            }
        });

        butZoom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ThrustCurvePage1.zoomCurves();
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

    public static class ThrustUtil {
        public String motorClass(double totImpulse) {
            if (totImpulse > 1.26 && totImpulse < 2.5)
                return "A";
            else if (totImpulse > 2.5 && totImpulse < 5.0)
                return "B";
            else if (totImpulse > 5.0 && totImpulse < 10.0)
                return "c";
            else if (totImpulse > 10.0 && totImpulse < 20.0)
                return "D";
            else if (totImpulse > 20.0 && totImpulse < 40.0)
                return "E";
            else if (totImpulse > 40.0 && totImpulse < 80.0)
                return "F";
            else if (totImpulse > 80.0 && totImpulse < 160.0)
                return "G";
            else if (totImpulse > 160.0 && totImpulse < 320.0)
                return "H";
            else if (totImpulse > 320.0 && totImpulse < 640.0)
                return "I";
            else if (totImpulse > 640.0 && totImpulse < 1280.0)
                return "J";
            else if (totImpulse > 1280.0 && totImpulse < 2560.0)
                return "K";
            else if (totImpulse > 2560.0 && totImpulse < 5120.0)
                return "L";
            else if (totImpulse > 5120.0 && totImpulse < 10240.0)
                return "M";
            else if (totImpulse > 10240.0 && totImpulse < 20480.0)
                return "N";
            else if (totImpulse > 20480.0 && totImpulse < 40960.0)
                return "O";
            else if (totImpulse > 40960.0 && totImpulse < 81920.0)
                return "P";
            else if (totImpulse > 81920.0 && totImpulse < 163840.0)
                return "Q";
            else
                return "unknown";
        }

        /*
        Return the position of the first X value it finds from the beginning
         */
        public int searchX(XYSeries serie, double searchVal) {
            int nbrData = serie.getItemCount();
            int pos = -1;
            for (int i = 1; i < nbrData; i++) {
                if ((searchVal >= serie.getY(i - 1).doubleValue()) && (searchVal <= serie.getY(i).doubleValue())) {
                    pos = i;
                    break;
                }
            }
            return pos;
        }

        /*
        Return the position of the first Y value it finds from the beginning
         */
        public int searchY(XYSeries serie, double searchVal) {
            int nbrData = serie.getItemCount();
            int pos = -1;
            for (int i = 1; i < nbrData; i++) {

                if ((searchVal >= serie.getX(i - 1).doubleValue()) && (searchVal <= serie.getX(i).doubleValue())) {
                    pos = i;
                    break;
                }
            }
            return pos;
        }

        /*
        Return the position of the first Y value it finds from the beginning
         */
        public int searchYFrom(XYSeries serie, int start, double searchVal) {
            int nbrData = serie.getItemCount();
            int pos = -1;
            for (int i = start; i < nbrData; i++) {
                if ((searchVal >= serie.getX(i - 1).doubleValue()) && (searchVal <= serie.getX(i).doubleValue())) {
                    pos = i;
                    break;
                }
            }
            return pos;
        }

        /*
        Return the position of the first X value it finds from the beginning
         */
        public int searchXFrom(XYSeries serie, int start, double searchVal) {
            int nbrData = serie.getItemCount();
            int pos = -1;
            for (int i = start; i < nbrData; i++) {

                if ((searchVal <= serie.getY(i - 1).doubleValue()) && (searchVal >= serie.getY(i).doubleValue())) {
                    pos = i;
                    break;
                }
            }
            return pos;
        }

    }

    public static class Tab1Fragment extends Fragment {
        private LineChart mChart;
        public XYSeriesCollection allThrustCurveData;
        int graphBackColor, fontSize, axisColor, labelColor, nbrColor;
        public Tab1Fragment(XYSeriesCollection data) {
            this.allThrustCurveData = data;
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            View view = inflater.inflate(R.layout.tabthrustcurve_view_mp_fragment, container, false);

            mChart = (LineChart) view.findViewById(R.id.linechart);

            mChart.setDragEnabled(true);
            mChart.setScaleEnabled(true);

            drawGraph();
            drawAllCurves(allThrustCurveData);

            return view;
        }

        private void drawGraph() {
            graphBackColor = myBT.getAppConf().ConvertColor(Integer.parseInt(myBT.getAppConf().getGraphBackColor()));
            fontSize = myBT.getAppConf().ConvertFont(Integer.parseInt(myBT.getAppConf().getFontSize()));
            axisColor = myBT.getAppConf().ConvertColor(Integer.parseInt(myBT.getAppConf().getGraphColor()));
            labelColor = Color.BLACK;
            nbrColor = Color.BLACK;
        }

        private void drawAllCurves(XYSeriesCollection allThrustCurveData) {
            dataSets.clear();

            thrustCurveData = new XYSeriesCollection();
            for (int i = 0; i < curvesNames.length; i++) {
                Log.d("drawAllCurves", "i:" + i);
                Log.d("drawAllCurves", "curvesNames:" + curvesNames[i]);
                if (checkedItems[i]) {
                    thrustCurveData.addSeries(allThrustCurveData.getSeries(curvesNames[i]));

                    int nbrData = allThrustCurveData.getSeries(i).getItemCount();

                    ArrayList<Entry> yValues = new ArrayList<>();

                    if (i ==0) {
                        for (int k = 0; k < nbrData; k++) {
                            if (myBT.getAppConf().getUnits().equals("0")) {
                                //kg
                                yValues.add(new Entry(allThrustCurveData.getSeries(i).getX(k).floatValue(), allThrustCurveData.getSeries(i).getY(k).floatValue() / 1000));
                            } else if (myBT.getAppConf().getUnits().equals("1")) {
                                //pound
                                yValues.add(new Entry(allThrustCurveData.getSeries(i).getX(k).floatValue(), (allThrustCurveData.getSeries(i).getY(k).floatValue() / 1000) * (float) 2.20462));
                            }
                            if (myBT.getAppConf().getUnits().equals("2")) {
                                //newton
                                yValues.add(new Entry(allThrustCurveData.getSeries(i).getX(k).floatValue(), (allThrustCurveData.getSeries(i).getY(k).floatValue() / 1000) * (float) 9.80665));
                            }
                        }
                    }
                    else {
                        for (int k = 0; k < nbrData; k++) {
                            if (myBT.getAppConf().getUnitsPressure().equals("0")) {
                            //PSI
                            yValues.add(new Entry(allThrustCurveData.getSeries(i).getX(k).floatValue(), allThrustCurveData.getSeries(i).getY(k).floatValue()));
                            } else if (myBT.getAppConf().getUnits().equals("1")) {
                                //bar divide by 14.504
                                yValues.add(new Entry(allThrustCurveData.getSeries(i).getX(k).floatValue(), allThrustCurveData.getSeries(i).getY(k).floatValue()/(float)14.504));
                            }
                            if (myBT.getAppConf().getUnits().equals("2")) {
                                //K pascal multiply by 6.895
                                yValues.add(new Entry(allThrustCurveData.getSeries(i).getX(k).floatValue(), allThrustCurveData.getSeries(i).getY(k).floatValue()*(float)6.895));
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
            /*ThrustUtil tu = new ThrustUtil();
            double maxThrust =  thrustCurveData.getSeries(0).getMaxY();
            double triggerThrust = maxThrust *(5.0/100.0);

            int curveStart = tu.searchX (thrustCurveData.getSeries(0), triggerThrust);
            int curveMaxThrust = tu.searchX (thrustCurveData.getSeries(0), maxThrust);
            int curveStop = tu.searchXFrom (thrustCurveData.getSeries(0), curveMaxThrust, triggerThrust);*/
            // thrust duration
            //double thrustDuration = ((double)thrustCurveData.getSeries(0).getX(curveStop )- (double)thrustCurveData.getSeries(0).getX(curveStart))/1000.0;

            // mChart.setVisibleXRange((int)thrustCurveData.getSeries(0).getX(curveStart).intValue(),(int)thrustCurveData.getSeries(0).getX(curveStop ).intValue());

        }

        private void zoomCurves() {
            dataSets.clear();

            thrustCurveData = new XYSeriesCollection();

            thrustCurveData.addSeries(allThrustCurveData.getSeries(0));


            ThrustUtil tu = new ThrustUtil();
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
                    }
                    if (myBT.getAppConf().getUnits().equals("2")) {
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

    /*
    This is the thrustCurve information tab
     */
    public static class Tab2Fragment extends Fragment {

        private ThrustCurveData mythrustCurve;

        private TextView nbrOfSamplesValue, thrustCurveNbrValue;
        private TextView recordingDurationValue, thrustTimeValue, maxThrustValue, averageThrustValue, motorClassValue, totalImpulseValue;
        private Button buttonExportToCsv, buttonExportToEng;
        double totalImpulse = 0;
        String motorClass="";
        double thrustDuration =0;
        double averageThrust=0;
        private AlertDialog.Builder builder = null;
        private AlertDialog alert;
        boolean SavedCurvesOK = false;

        public Tab2Fragment(ThrustCurveData data) {
            mythrustCurve = data;
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            View view = inflater.inflate(R.layout.tabthrustcurve_info_fragment, container, false);

            buttonExportToCsv= (Button) view.findViewById(R.id.butExportToCsv);
            buttonExportToEng= (Button) view.findViewById(R.id.butExportToEng);
            recordingDurationValue = view.findViewById(R.id.thrustCurveDurationValue);
            thrustTimeValue = view.findViewById(R.id.thrustTimeValue);
            maxThrustValue = view.findViewById(R.id.maxThrustValue);
            averageThrustValue = view.findViewById(R.id.averageThrustValue);

            nbrOfSamplesValue = view.findViewById(R.id.nbrOfSamplesValue);
            thrustCurveNbrValue = view.findViewById(R.id.thrustCurveNbrValue);
            motorClassValue = view.findViewById(R.id.motorClassValue);
            totalImpulseValue =view.findViewById(R.id.totalImpulseValue);

            XYSeriesCollection thrustCurveData;

            thrustCurveData = mythrustCurve.GetThrustCurveData(ThrustCurveName);
            int nbrData = thrustCurveData.getSeries(0).getItemCount();

            // ThrustCurve nbr
            thrustCurveNbrValue.setText(ThrustCurveName + "");

            //nbr of samples
            nbrOfSamplesValue.setText(nbrData + "");

            //Recording duration
            double recordingDuration = thrustCurveData.getSeries(0).getMaxX() / 1000;
            recordingDurationValue.setText(String.format("%.3f ",recordingDuration) + " secs");


            ThrustUtil tu = new ThrustUtil();
            double maxThrust = thrustCurveData.getSeries(0).getMaxY();
            double triggerThrust = maxThrust * (5.0 / 100.0);

            int curveStart = tu.searchX(thrustCurveData.getSeries(0), triggerThrust);
            int curveMaxThrust = tu.searchX(thrustCurveData.getSeries(0), maxThrust);
            int curveStop = tu.searchXFrom(thrustCurveData.getSeries(0), curveMaxThrust, triggerThrust);
            if (curveStart != -1 && curveMaxThrust != -1 && curveStop != -1) {
                // thrust duration
                thrustDuration = ((double) thrustCurveData.getSeries(0).getX(curveStop) - (double) thrustCurveData.getSeries(0).getX(curveStart)) / 1000.0;
                thrustTimeValue.setText(String.format("%.2f ",thrustDuration) + " secs");
                double totThurst = 0;
                for (int i = curveStart; i < curveStop; i++) {
                    totThurst = totThurst + (double) thrustCurveData.getSeries(0).getY(i);
                }
                averageThrust = totThurst / ((double) (curveStop - curveStart));

                //max thrust
                maxThrustValue.setText(String.format("%.1f ", maxThrust * CONVERT) + units[0]);
                //average thrust
                averageThrustValue.setText(String.format("%.1f ", averageThrust * CONVERT) + units[0]);


                //tot impulse
                //double totalImpulse = 0;
                totalImpulse = averageThrust * (9.80665 / 1000) * thrustDuration;
                totalImpulseValue.setText(String.format("%.0f ",totalImpulse) + " Newtons");
                //motor class
                motorClass =tu.motorClass(totalImpulse) + String.format("%.0f ", averageThrust * (9.80665 / 1000));
                motorClassValue.setText(tu.motorClass(totalImpulse) + String.format("%.0f ", averageThrust * (9.80665 / 1000)));
            }
            buttonExportToCsv.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    exportToCSV();      //export the data to a csv file
                }
            });

            buttonExportToEng.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    exportToEng(motorClass);      //export the data to an eng file
                }
            });

            return view;
        }
        private void exportToCSV(){


            String csv_data = "time,thrust" + units[0]+ "\n";/// your csv data as string;

            ThrustUtil tu = new ThrustUtil();
            double maxThrust = thrustCurveData.getSeries(0).getMaxY();
            double triggerThrust = maxThrust * (5.0 / 100.0);

            int curveStart = tu.searchX(thrustCurveData.getSeries(0), triggerThrust);
            int curveMaxThrust = tu.searchX(thrustCurveData.getSeries(0), maxThrust);
            int curveStop = tu.searchXFrom(thrustCurveData.getSeries(0), curveMaxThrust, triggerThrust);

            //ArrayList<Entry> yValues = new ArrayList<>();
            if (curveStart != -1 && curveMaxThrust != -1 && curveStop != -1) {
                for (int k = curveStart; k < curveStop; k++) {

                    //newton
                    //yValues.add(new Entry(thrustCurveData.getSeries(0).getX(k).floatValue() - thrustCurveData.getSeries(0).getX(curveStart).floatValue(),
                    // (thrustCurveData.getSeries(0).getY(k).floatValue() / 1000) * (float) 9.80665));
                    String curTime = String.format("%.3f ",(thrustCurveData.getSeries(0).getX(k).floatValue() - thrustCurveData.getSeries(0).getX(curveStart).floatValue())/1000);
                    if(curTime.contains(","))
                        curTime = curTime+";";
                    else
                        curTime = curTime+",";
                    String currData="";
                    if (myBT.getAppConf().getUnits().equals("0")) {
                        //kg
                        currData = String.format("%.1f ", (thrustCurveData.getSeries(0).getY(k).floatValue() / 1000) );
                    }else if (myBT.getAppConf().getUnits().equals("1")) {
                        //pound
                        currData = String.format("%.1f ", (thrustCurveData.getSeries(0).getY(k).floatValue() / 1000) * (float) 2.20462);
                    }
                    if (myBT.getAppConf().getUnits().equals("2")) {
                        //newton
                        currData = String.format("%.1f ", (thrustCurveData.getSeries(0).getY(k).floatValue() / 1000) * (float) 9.80665);
                    }

                    csv_data = csv_data + curTime+ currData+"\n";


                }
            }

            File root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

            //if you want to create a sub-dir
            root = new File(root, "RocketMotorTestStand");
            root.mkdir();

            SimpleDateFormat sdf = new SimpleDateFormat("_dd-MM-yyyy_hh-mm-ss");
            String date = sdf.format(System.currentTimeMillis());

            // select the name for your file
            root = new File(root , ThrustCurveName+ date +".csv");

            try {
                FileOutputStream fout = new FileOutputStream(root);
                fout.write(csv_data.getBytes());

                fout.close();
                //Confirmation message
                builder = new AlertDialog.Builder(Tab2Fragment.this.getContext());
                //Running Saving commands
                builder.setMessage(getString(R.string.not_saved_msg)+ Environment.DIRECTORY_DOWNLOADS+ "\\RocketMotorTestStand\\"+ThrustCurveName +".csv")
                        .setTitle(R.string.not_saved_title)
                        .setCancelable(false)
                        .setPositiveButton(R.string.not_saved_ok, new DialogInterface.OnClickListener() {
                            public void onClick(final DialogInterface dialog, final int id) {
                                dialog.cancel();
                            }
                        });

                alert = builder.create();
                alert.show();
            } catch (FileNotFoundException e) {
                e.printStackTrace();

                boolean bool = false;
                try {
                    // try to create the file
                    bool = root.createNewFile();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }

                if (bool){
                    // call the method again
                    exportToCSV();
                }else {
                    //throw new IllegalStateException(getString(R.string.failed_to_create_csv));
                    SavedCurvesOK=false;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void exportToEng(String motorClass) {

            String motorfile_data = ";File generated by RocketMotorTestStand \n;\n";
            //K1530 54 714 0 1.287998 1.499998 TruCore

            motorfile_data = motorfile_data +"\n";
            motorfile_data = motorfile_data + motorClass + " " + "motorDiam" + " "+ "casinglength"
                    + " " + "delay" + " " + "propellantWeight" + " " + "totalWeight" + " " +"manufacturer" + "\n";

            ThrustUtil tu = new ThrustUtil();
            double maxThrust = thrustCurveData.getSeries(0).getMaxY();
            double triggerThrust = maxThrust * (5.0 / 100.0);

            int curveStart = tu.searchX(thrustCurveData.getSeries(0), triggerThrust);
            int curveMaxThrust = tu.searchX(thrustCurveData.getSeries(0), maxThrust);
            int curveStop = tu.searchXFrom(thrustCurveData.getSeries(0), curveMaxThrust, triggerThrust);

            if (curveStart != -1 && curveMaxThrust != -1 && curveStop != -1) {
                for (int k = curveStart; k < curveStop; k++) {

                    //newton
                    //yValues.add(new Entry(thrustCurveData.getSeries(0).getX(k).floatValue() - thrustCurveData.getSeries(0).getX(curveStart).floatValue(),
                    // (thrustCurveData.getSeries(0).getY(k).floatValue() / 1000) * (float) 9.80665));
                    String currData = String.format("%.3f ",(thrustCurveData.getSeries(0).getX(k).floatValue() - thrustCurveData.getSeries(0).getX(curveStart).floatValue())/1000) +" " +
                            String.format("%.1f ",(thrustCurveData.getSeries(0).getY(k).floatValue() / 1000) * (float) 9.80665);
                    currData =currData.replace(",",".");
                    motorfile_data = motorfile_data +  currData+"\n";
                }
            }

            File root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            int nbrData = thrustCurveData.getSeries(0).getItemCount();

            motorfile_data = motorfile_data +";\n";

            //if you want to create a sub-dir
            root = new File(root, "RocketMotorTestStand");
            root.mkdir();

            SimpleDateFormat sdf = new SimpleDateFormat("_dd-MM-yyyy_hh-mm-ss");
            String date = sdf.format(System.currentTimeMillis());
            // select the name for your file
            root = new File(root , ThrustCurveName +date+".eng");

            try {
                FileOutputStream fout = new FileOutputStream(root);
                fout.write(motorfile_data.getBytes());

                fout.close();
                //Confirmation message
                builder = new AlertDialog.Builder(Tab2Fragment.this.getContext());
                //Running Saving commands
                builder.setMessage(getString(R.string.file_saved_msg1)+ Environment.DIRECTORY_DOWNLOADS+
                        "\\RocketMotorTestStand\\"+ThrustCurveName +".eng" +
                        getString(R.string.file_saved_msg2))
                        .setTitle("Info")
                        .setCancelable(false)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(final DialogInterface dialog, final int id) {
                                dialog.cancel();
                            }
                        });

                alert = builder.create();
                alert.show();
            } catch (FileNotFoundException e) {
                e.printStackTrace();

                boolean bool = false;
                try {
                    // try to create the file
                    bool = root.createNewFile();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }

                if (bool){
                    // call the method again
                    exportToEng(motorClass);
                }else {
                    //throw new IllegalStateException(getString(R.string.failed_to_create_eng));
                    SavedCurvesOK = false;
                    //msg()
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_thrust_curves, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //open application settings screen
        if (id == R.id.action_share) {
            ShareHandler.share(ShareHandler.takeScreenshot(findViewById(android.R.id.content).getRootView()), this.getApplicationContext());
            return true;
        }
        //open help screen
        if (id == R.id.action_help) {
            Intent i= new Intent(this, HelpActivity.class);
            i.putExtra("help_file", "help_curve");
            startActivity(i);
            return true;
        }
        //open about screen
        if (id == R.id.action_about) {
            Intent i= new Intent(this, AboutActivity.class);
            startActivity(i);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}