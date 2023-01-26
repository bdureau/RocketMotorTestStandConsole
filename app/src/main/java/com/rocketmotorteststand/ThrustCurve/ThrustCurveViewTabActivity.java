package com.rocketmotorteststand.ThrustCurve;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
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
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Environment;
//import android.provider.MediaStore;
import android.text.Html;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.afree.data.xy.XYSeries;
import org.afree.data.xy.XYSeriesCollection;
import org.afree.graphics.geom.Font;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


public class ThrustCurveViewTabActivity extends AppCompatActivity {
    private ThrustCurveData mythrustCurve = null;
    private ViewPager mViewPager;
    SectionsPageAdapter adapter;

    private TextView[] dotsSlide;
    private LinearLayout linearDots;

    private Tab1Fragment ThrustCurvePage1 = null;
    private Tab2Fragment ThrustCurvePage2 = null;
    private Button btnDismiss, butSelectCurves, butZoom;
    private static ConsoleApplication myBT;
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static final String[] PERMISSION_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
    };

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
    private static double CONVERT = 1.0, CONVERT_PRESSURE = 1.0;

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

        /*if (Build.VERSION.SDK_INT >= 23) {
            //int REQUEST_CODE_ASK_PERMISSIONS = 123;
            //int hasWriteContactsPermission = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            int hasWriteContactsPermission = ActivityCompat.checkSelfPermission(ThrustCurveViewTabActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

            if (hasWriteContactsPermission != PackageManager.PERMISSION_GRANTED) {*/
                /*requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_CODE_ASK_PERMISSIONS);*/
               /* ActivityCompat.requestPermissions(
                        ThrustCurveViewTabActivity.this,
                        PERMISSION_STORAGE,
                        REQUEST_EXTERNAL_STORAGE);

            }
        }*/

        if (Build.VERSION.SDK_INT >= 23)
            verifyStoragePermission(ThrustCurveViewTabActivity.this);
        /*int permission = ActivityCompat.checkSelfPermission(ThrustCurveViewTabActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    ThrustCurveViewTabActivity.this,
                    PERMISSION_STORAGE,
                    REQUEST_EXTERNAL_STORAGE);
        }*/

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
        if (myBT.getTestStandConfigData().getTestStandName().equals("TestStandSTM32V2")) {
            numberOfCurves = 2;
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

        if (myBT.getTestStandConfigData().getTestStandName().equals("TestStandSTM32V2")) {
            Log.d("numberOfCurves", "Adding curve pressure");
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
            Log.d("numberOfCurves", allThrustCurveData.getSeries(i).getKey().toString());
        }

        // Read the application config
        myBT.getAppConf().ReadConfig();
        if (myBT.getAppConf().getUnits().equals("0")) {
            //kg
            units[0] = "(" + getResources().getString(R.string.Kg_fview) + ")";
            CONVERT = 1.0 / 1000;

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
                CONVERT_PRESSURE = 1;
            } else if (myBT.getAppConf().getUnits().equals("1")) {
                //BAR
                units[1] = "BAR";
                CONVERT_PRESSURE = 0.0689476;
            } else if (myBT.getAppConf().getUnits().equals("2")) {
                //Kpascal
                units[1] = "Kpascal";
                CONVERT_PRESSURE = 6.89476;
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

    public static void verifyStoragePermission(Activity activity) {
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSION_STORAGE,
                    REQUEST_EXTERNAL_STORAGE);
        }
    }

    private void setupViewPager(ViewPager viewPager) {
        adapter = new SectionsPageAdapter(getSupportFragmentManager());
        ThrustCurvePage1 = new Tab1Fragment(allThrustCurveData);
        ThrustCurvePage2 = new Tab2Fragment(mythrustCurve);

        adapter.addFragment(ThrustCurvePage1, "TAB1");
        adapter.addFragment(ThrustCurvePage2, "TAB2");

        linearDots = findViewById(R.id.idThrustCurveLinearDots);
        agregaIndicateDots(0, adapter.getCount());
        viewPager.setAdapter(adapter);
        viewPager.addOnPageChangeListener(viewListener);
    }

    public void agregaIndicateDots(int pos, int nbr) {
        dotsSlide = new TextView[nbr];
        linearDots.removeAllViews();

        for (int i = 0; i < dotsSlide.length; i++) {
            dotsSlide[i] = new TextView(this);
            dotsSlide[i].setText(Html.fromHtml("&#8226;"));
            dotsSlide[i].setTextSize(35);
            dotsSlide[i].setTextColor(getResources().getColor(R.color.colorWhiteTransparent));
            linearDots.addView(dotsSlide[i]);
        }

        if (dotsSlide.length > 0) {
            dotsSlide[pos].setTextColor(getResources().getColor(R.color.colorWhite));
        }
    }

    ViewPager.OnPageChangeListener viewListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int i, float v, int i1) {
        }

        @Override
        public void onPageSelected(int i) {
            agregaIndicateDots(i, adapter.getCount());
        }

        @Override
        public void onPageScrollStateChanged(int i) {
        }
    };

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
            if (start != -1) {
                for (int i = start; i < nbrData; i++) {
                    /*Log.d("search 1", i + "");
                    Log.d("search 1", serie.getY(i).doubleValue() + "");
                    Log.d("search 1", serie.getY(i - 1).doubleValue() + "");*/
                    if ((searchVal <= serie.getY(i - 1).doubleValue()) && (searchVal >= serie.getY(i).doubleValue())) {
                        pos = i;
                        break;
                    }
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

    /*
    This is the thrustCurve information tab
     */
    public static class Tab2Fragment extends Fragment {

        private ThrustCurveData mythrustCurve;

        private TextView nbrOfSamplesValue, thrustCurveNbrValue;
        private TextView recordingDurationValue, thrustTimeValue, maxThrustValue, averageThrustValue;
        private TextView motorClassValue, totalImpulseValue, maxPressureValue;
        private TextView maxPressure;
        private Button buttonExportToCsv, buttonExportToCsvFull, buttonExportToEng, buttonShareEng,
                butShareZip;
        double totalImpulse = 0;
        String motorClass = "";
        double thrustDuration = 0;
        double averageThrust = 0;
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

            buttonExportToCsv = (Button) view.findViewById(R.id.butExportToCsv);
            buttonShareEng = (Button) view.findViewById(R.id.butShareEng);
            buttonExportToCsvFull = (Button) view.findViewById(R.id.butExportToCsvFull);
            buttonExportToEng = (Button) view.findViewById(R.id.butExportToEng);
            butShareZip = (Button) view.findViewById(R.id.butShareZip);
            recordingDurationValue = view.findViewById(R.id.thrustCurveDurationValue);
            thrustTimeValue = view.findViewById(R.id.thrustTimeValue);
            maxThrustValue = view.findViewById(R.id.maxThrustValue);
            averageThrustValue = view.findViewById(R.id.averageThrustValue);

            nbrOfSamplesValue = view.findViewById(R.id.nbrOfSamplesValue);
            thrustCurveNbrValue = view.findViewById(R.id.thrustCurveNbrValue);
            motorClassValue = view.findViewById(R.id.motorClassValue);
            totalImpulseValue = view.findViewById(R.id.totalImpulseValue);
            maxPressureValue = view.findViewById(R.id.maxPressureValue);
            maxPressure = view.findViewById(R.id.maxPressure);

            if (myBT.getTestStandConfigData().getTestStandName().equals("TestStandSTM32V2")) {
                maxPressureValue.setVisibility(View.VISIBLE);
                maxPressure.setVisibility(View.VISIBLE);
            } else {
                maxPressureValue.setVisibility(View.INVISIBLE);
                maxPressure.setVisibility(View.INVISIBLE);
            }

            XYSeriesCollection lThrustCurveData;

            lThrustCurveData = mythrustCurve.GetThrustCurveData(ThrustCurveName);
            int nbrData = lThrustCurveData.getSeries(0).getItemCount();

            // ThrustCurve nbr
            thrustCurveNbrValue.setText(ThrustCurveName + "");

            //nbr of samples
            nbrOfSamplesValue.setText(nbrData + "");

            //Recording duration
            double recordingDuration = lThrustCurveData.getSeries(0).getMaxX() / 1000;
            recordingDurationValue.setText(String.format("%.3f ", recordingDuration) + " secs");

            ThrustUtil tu = new ThrustUtil();
            double maxThrust = lThrustCurveData.getSeries(0).getMaxY();
            double triggerThrust = maxThrust * (5.0 / 100.0);

            int curveStart = tu.searchX(lThrustCurveData.getSeries(0), triggerThrust);
            int curveMaxThrust = tu.searchX(lThrustCurveData.getSeries(0), maxThrust);
            int curveStop = tu.searchXFrom(lThrustCurveData.getSeries(0), curveMaxThrust, triggerThrust);
            if (curveStart != -1 && curveMaxThrust != -1 && curveStop != -1) {
                // thrust duration
                thrustDuration = ((double) lThrustCurveData.getSeries(0).getX(curveStop) - (double) lThrustCurveData.getSeries(0).getX(curveStart)) / 1000.0;
                thrustTimeValue.setText(String.format("%.2f ", thrustDuration) + " secs");
                double totThurst = 0;
                for (int i = curveStart; i < curveStop; i++) {
                    totThurst = totThurst + (double) lThrustCurveData.getSeries(0).getY(i);
                }
                averageThrust = totThurst / ((double) (curveStop - curveStart));

                //max thrust
                maxThrustValue.setText(String.format("%.1f ", maxThrust * CONVERT) + units[0]);
                //average thrust
                averageThrustValue.setText(String.format("%.1f ", averageThrust * CONVERT) + units[0]);

                //tot impulse
                //double totalImpulse = 0;
                totalImpulse = averageThrust * (9.80665 / 1000) * thrustDuration;
                totalImpulseValue.setText(String.format("%.0f ", totalImpulse) + " Newtons");
                //motor class
                motorClass = tu.motorClass(totalImpulse) + String.format("%.0f ", averageThrust * (9.80665 / 1000));
                motorClassValue.setText(tu.motorClass(totalImpulse) + String.format("%.0f ", averageThrust * (9.80665 / 1000)));
            }

            if (myBT.getTestStandConfigData().getTestStandName().equals("TestStandSTM32V2")) {
                double maxPres = lThrustCurveData.getSeries(1).getMaxY();
                maxPressureValue.setText(maxPres + " PSI");
            }
            buttonExportToCsv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    exportToCSV(motorClass, lThrustCurveData);      //export the data to a csv file
                }
            });

            buttonExportToCsvFull.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    exportToCSVFull(motorClass, lThrustCurveData);      //export the data to a csv file
                }
            });

            buttonExportToEng.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    exportToEng(motorClass, lThrustCurveData, getString(R.string.file_saved_msg2));      //export the data to an eng file
                }
            });

            buttonShareEng.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String currentEng = exportToEng(motorClass, lThrustCurveData, "");
                    Log.d("File:", currentEng);

                    File engFile = new File(Environment.getExternalStoragePublicDirectory
                            (Environment.DIRECTORY_DOWNLOADS), currentEng);
                    Toast.makeText(getContext(), currentEng, Toast.LENGTH_SHORT).show();
                    shareFile(engFile);
                }
            });
            butShareZip.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    // Create a file for the zip file
                    File zipFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "motorData.zip");
                    ArrayList<String> fileNames = new ArrayList<>();
                    fileNames.add(exportThrustToCSV(lThrustCurveData));
                    if (myBT.getTestStandConfigData().getTestStandName().equals("TestStandSTM32V2")) {
                        fileNames.add(exportPressureToCSV(lThrustCurveData));
                    }
                    fileNames.add(exportToEng(motorClass, lThrustCurveData, ""));
                    try {
                        // Create a zip output stream to write to the zip file
                        FileOutputStream fos = new FileOutputStream(zipFile);
                        ZipOutputStream zos = new ZipOutputStream(fos);

                        for (String fileName : fileNames) {
                            ZipEntry ze = new ZipEntry(fileName);
                            // Add the zip entry to the zip output stream
                            zos.putNextEntry(ze);
                            // Read the file and write it to the zip output stream
                            File filetoZip = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),  fileName);
                            FileInputStream fis = new FileInputStream(filetoZip);
                            byte[] buffer = new byte[1024];
                            int len;
                            while ((len = fis.read(buffer)) > 0) {
                                zos.write(buffer, 0, len);
                            }
                            // Close the zip entry and the file input stream
                            zos.closeEntry();
                            fis.close();
                        }
                        // Close the zip output stream
                        zos.close();
                        fos.close();
                    }catch (Exception e) {
                        e.printStackTrace();
                        Log.d("error", "we have an issue");
                    }

                    //Toast.makeText(getContext(), currentEng, Toast.LENGTH_SHORT).show();
                    shareFile(zipFile);
                }

            });

            return view;
        }


        private void exportToCSV(String motorClass, XYSeriesCollection lThrustCurveData) {

            /*String csv_data = "time,thrust" + units[0] + "\n";/// your csv data as string;

            ThrustUtil tu = new ThrustUtil();
            double maxThrust = lThrustCurveData.getSeries(0).getMaxY();
            double triggerThrust = maxThrust * (5.0 / 100.0);

            int curveStart = tu.searchX(lThrustCurveData.getSeries(0), triggerThrust);
            int curveMaxThrust = tu.searchX(lThrustCurveData.getSeries(0), maxThrust);
            int curveStop = tu.searchXFrom(lThrustCurveData.getSeries(0), curveMaxThrust, triggerThrust);
            // Export Thrust
            if (curveStart != -1 && curveMaxThrust != -1 && curveStop != -1) {
                for (int k = curveStart; k < curveStop; k++) {

                    String curTime = String.format("%.3f ", (lThrustCurveData.getSeries(0).getX(k).floatValue() - lThrustCurveData.getSeries(0).getX(curveStart).floatValue()) / 1000);
                    if (curTime.contains(","))
                        curTime = curTime + ";";
                    else
                        curTime = curTime + ",";
                    String currData = "";
                    if (myBT.getAppConf().getUnits().equals("0")) {
                        //kg
                        currData = String.format("%.1f ", (lThrustCurveData.getSeries(0).getY(k).floatValue() / 1000));
                    } else if (myBT.getAppConf().getUnits().equals("1")) {
                        //pound
                        currData = String.format("%.1f ", (lThrustCurveData.getSeries(0).getY(k).floatValue() / 1000) * (float) 2.20462);
                    } else if (myBT.getAppConf().getUnits().equals("2")) {
                        //newton
                        currData = String.format("%.1f ", (lThrustCurveData.getSeries(0).getY(k).floatValue() / 1000) * (float) 9.80665);
                    }
                    csv_data = csv_data + curTime + currData + "\n";
                }
            }

            SimpleDateFormat sdf = new SimpleDateFormat("_dd-MM-yyyy_hh-mm-ss");
            String date = sdf.format(System.currentTimeMillis());
            createFile(ThrustCurveName + "_" + motorClass + "_" + date + ".csv", csv_data, "");

            //export pressure
            if (myBT.getTestStandConfigData().getTestStandName().equals("TestStandSTM32V2")) {
                String csv_data_pressure = "time,pressure" + units[1] + "\n";/// your csv data as string;

                double maxPressure = lThrustCurveData.getSeries(1).getMaxY();
                double minPressure = lThrustCurveData.getSeries(1).getMinY();
                double triggerPressure = (maxPressure * (5.0 / 100.0)) + minPressure;

                int curvePressureStart = tu.searchX(lThrustCurveData.getSeries(1), triggerPressure);
                int curveMaxPressure = tu.searchX(lThrustCurveData.getSeries(1), maxPressure);
                int curvePressureStop = tu.searchXFrom(lThrustCurveData.getSeries(1), curveMaxPressure, triggerPressure);

                if (curvePressureStart != -1 && curveMaxPressure != -1 && curvePressureStop != -1) {
                    for (int k = curvePressureStart; k < curvePressureStop; k++) {

                        String curTime = String.format("%.3f ", (lThrustCurveData.getSeries(1).getX(k).floatValue() - lThrustCurveData.getSeries(1).getX(curveStart).floatValue()) / 1000);
                        if (curTime.contains(","))
                            curTime = curTime + ";";
                        else
                            curTime = curTime + ",";
                        String currData = "";
                        if (myBT.getAppConf().getUnitsPressure().equals("0")) {
                            //PSI
                            currData = String.format("%.1f ", (lThrustCurveData.getSeries(1).getY(k).floatValue()));
                        } else if (myBT.getAppConf().getUnitsPressure().equals("1")) {
                            //bar
                            currData = String.format("%.1f ", lThrustCurveData.getSeries(1).getY(k).floatValue() / (float) 14.504);
                        } else if (myBT.getAppConf().getUnitsPressure().equals("2")) {
                            //Kpascal
                            currData = String.format("%.1f ", lThrustCurveData.getSeries(1).getY(k).floatValue() * (float) 6.895);
                        }
                        csv_data_pressure = csv_data_pressure + curTime + currData + "\n";
                    }
                    createFile(ThrustCurveName + "_pressure_" + motorClass + "_" + date + ".csv", csv_data_pressure, "");
                }
            }*/
            String fileNames="";
            String thrustFileName = exportThrustToCSV(lThrustCurveData);
            fileNames = thrustFileName;
            if (myBT.getTestStandConfigData().getTestStandName().equals("TestStandSTM32V2")) {
                String pressureFileName = exportPressureToCSV(lThrustCurveData);
                fileNames = fileNames + "\n" + pressureFileName;
            }
            Toast.makeText(getContext(), fileNames, Toast.LENGTH_SHORT).show();
        }

        private String exportThrustToCSV(XYSeriesCollection lThrustCurveData) {
            SimpleDateFormat sdf = new SimpleDateFormat("_dd-MM-yyyy_hh-mm-ss");
            String date = sdf.format(System.currentTimeMillis());
            String csv_data = "time,thrust" + units[0] + "\n";/// your csv data as string;

            ThrustUtil tu = new ThrustUtil();
            double maxThrust = lThrustCurveData.getSeries(0).getMaxY();
            double triggerThrust = maxThrust * (5.0 / 100.0);

            int curveStart = tu.searchX(lThrustCurveData.getSeries(0), triggerThrust);
            int curveMaxThrust = tu.searchX(lThrustCurveData.getSeries(0), maxThrust);
            int curveStop = tu.searchXFrom(lThrustCurveData.getSeries(0), curveMaxThrust, triggerThrust);
            // Export Thrust
            if (curveStart != -1 && curveMaxThrust != -1 && curveStop != -1) {
                for (int k = curveStart; k < curveStop; k++) {

                    String curTime = String.format("%.3f ", (lThrustCurveData.getSeries(0).getX(k).floatValue() - lThrustCurveData.getSeries(0).getX(curveStart).floatValue()) / 1000);
                    if (curTime.contains(","))
                        curTime = curTime + ";";
                    else
                        curTime = curTime + ",";
                    String currData = "";
                    if (myBT.getAppConf().getUnits().equals("0")) {
                        //kg
                        currData = String.format("%.1f ", (lThrustCurveData.getSeries(0).getY(k).floatValue() / 1000));
                    } else if (myBT.getAppConf().getUnits().equals("1")) {
                        //pound
                        currData = String.format("%.1f ", (lThrustCurveData.getSeries(0).getY(k).floatValue() / 1000) * (float) 2.20462);
                    } else if (myBT.getAppConf().getUnits().equals("2")) {
                        //newton
                        currData = String.format("%.1f ", (lThrustCurveData.getSeries(0).getY(k).floatValue() / 1000) * (float) 9.80665);
                    }
                    csv_data = csv_data + curTime + currData + "\n";
                }
            }

            //SimpleDateFormat sdf = new SimpleDateFormat("_dd-MM-yyyy_hh-mm-ss");
            //String date = sdf.format(System.currentTimeMillis());
            String fileName = ThrustCurveName + "_" + motorClass + "_" + date + ".csv";
            createFile(fileName, csv_data, "");
            return "RocketMotorTestStand/" +fileName;
        }

        private String exportPressureToCSV(XYSeriesCollection lThrustCurveData) {
            SimpleDateFormat sdf = new SimpleDateFormat("_dd-MM-yyyy_hh-mm-ss");
            String date = sdf.format(System.currentTimeMillis());
            String fileName = "";
            ThrustUtil tu = new ThrustUtil();
            double maxThrust = lThrustCurveData.getSeries(0).getMaxY();
            double triggerThrust = maxThrust * (5.0 / 100.0);

            int curveStart = tu.searchX(lThrustCurveData.getSeries(0), triggerThrust);
            String csv_data_pressure = "time,pressure" + units[1] + "\n";/// your csv data as string;

            double maxPressure = lThrustCurveData.getSeries(1).getMaxY();
            double minPressure = lThrustCurveData.getSeries(1).getMinY();
            double triggerPressure = (maxPressure * (5.0 / 100.0)) + minPressure;

            int curvePressureStart = tu.searchX(lThrustCurveData.getSeries(1), triggerPressure);
            int curveMaxPressure = tu.searchX(lThrustCurveData.getSeries(1), maxPressure);
            int curvePressureStop = tu.searchXFrom(lThrustCurveData.getSeries(1), curveMaxPressure, triggerPressure);

            if (curvePressureStart != -1 && curveMaxPressure != -1 && curvePressureStop != -1) {
                for (int k = curvePressureStart; k < curvePressureStop; k++) {

                    String curTime = String.format("%.3f ", (lThrustCurveData.getSeries(1).getX(k).floatValue() - lThrustCurveData.getSeries(1).getX(curveStart).floatValue()) / 1000);
                    if (curTime.contains(","))
                        curTime = curTime + ";";
                    else
                        curTime = curTime + ",";
                    String currData = "";
                    if (myBT.getAppConf().getUnitsPressure().equals("0")) {
                        //PSI
                        currData = String.format("%.1f ", (lThrustCurveData.getSeries(1).getY(k).floatValue()));
                    } else if (myBT.getAppConf().getUnitsPressure().equals("1")) {
                        //bar
                        currData = String.format("%.1f ", lThrustCurveData.getSeries(1).getY(k).floatValue() / (float) 14.504);
                    } else if (myBT.getAppConf().getUnitsPressure().equals("2")) {
                        //Kpascal
                        currData = String.format("%.1f ", lThrustCurveData.getSeries(1).getY(k).floatValue() * (float) 6.895);
                    }
                    csv_data_pressure = csv_data_pressure + curTime + currData + "\n";
                }
                fileName = ThrustCurveName + "_pressure_" + motorClass + "_" + date + ".csv";
                createFile(fileName, csv_data_pressure, "");
            }
            return "RocketMotorTestStand/" +fileName;
        }

        private void exportToCSVFull(String motorClass, XYSeriesCollection lThrustCurveData) {

            String csv_data = "time,thrust" + units[0] + "\n";/// your csv data as string;

            for (int k = 0; k < lThrustCurveData.getSeries(0).getItemCount(); k++) {
                String curTime = String.format("%.3f ", (lThrustCurveData.getSeries(0).getX(k).floatValue()) / 1000);
                if (curTime.contains(","))
                    curTime = curTime + ";";
                else
                    curTime = curTime + ",";
                String currData = "";
                if (myBT.getAppConf().getUnits().equals("0")) {
                    //kg
                    currData = String.format("%.1f ", (lThrustCurveData.getSeries(0).getY(k).floatValue() / 1000));
                } else if (myBT.getAppConf().getUnits().equals("1")) {
                    //pound
                    currData = String.format("%.1f ", (lThrustCurveData.getSeries(0).getY(k).floatValue() / 1000) * (float) 2.20462);
                } else if (myBT.getAppConf().getUnits().equals("2")) {
                    //newton
                    currData = String.format("%.1f ", (lThrustCurveData.getSeries(0).getY(k).floatValue() / 1000) * (float) 9.80665);
                }
                csv_data = csv_data + curTime + currData + "\n";
            }


            SimpleDateFormat sdf = new SimpleDateFormat("_dd-MM-yyyy_hh-mm-ss");
            String date = sdf.format(System.currentTimeMillis());
            createFile(ThrustCurveName + "_full_" + motorClass + "_" + date + ".csv", csv_data, "");

            //export pressure
            if (myBT.getTestStandConfigData().getTestStandName().equals("TestStandSTM32V2")) {
                String csv_data_pressure = "time,pressure" + units[1] + "\n";

                for (int k = 0; k < lThrustCurveData.getSeries(1).getItemCount(); k++) {
                    String curTime = String.format("%.3f ", (lThrustCurveData.getSeries(1).getX(k).floatValue()) / 1000);
                    if (curTime.contains(","))
                        curTime = curTime + ";";
                    else
                        curTime = curTime + ",";
                    String currData = "";
                    if (myBT.getAppConf().getUnitsPressure().equals("0")) {
                        //PSI
                        currData = String.format("%.1f ", (lThrustCurveData.getSeries(1).getY(k).floatValue()));
                    } else if (myBT.getAppConf().getUnitsPressure().equals("1")) {
                        //bar
                        currData = String.format("%.1f ", lThrustCurveData.getSeries(1).getY(k).floatValue() / (float) 14.504);
                    } else if (myBT.getAppConf().getUnitsPressure().equals("2")) {
                        //Kpascal
                        currData = String.format("%.1f ", lThrustCurveData.getSeries(1).getY(k).floatValue() * (float) 6.895);
                    }
                    csv_data_pressure = csv_data_pressure + curTime + currData + "\n";
                }
                createFile(ThrustCurveName + "_pressure_full_" + motorClass + "_" + date + ".csv", csv_data_pressure, "");
            }
        }

        private void createFile(String fileName, String csv_data, String msg) {
            File root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

            //if you want to create a sub-dir
            root = new File(root, "RocketMotorTestStand");
            root.mkdir();

            //SimpleDateFormat sdf = new SimpleDateFormat("_dd-MM-yyyy_hh-mm-ss");
            //String date = sdf.format(System.currentTimeMillis());

            // select the name for your file
            root = new File(root, fileName);

            try {
                FileOutputStream fout = new FileOutputStream(root);
                fout.write(csv_data.getBytes());

                fout.close();
                if (!msg.equals("")) {
                    //Confirmation message
                    builder = new AlertDialog.Builder(Tab2Fragment.this.getContext());
                    //Running Saving commands
                    builder.setMessage(getString(R.string.not_saved_msg) + Environment.DIRECTORY_DOWNLOADS + "\\RocketMotorTestStand\\" + fileName + msg)
                            .setTitle(R.string.not_saved_title)
                            .setCancelable(false)
                            .setPositiveButton(R.string.not_saved_ok, new DialogInterface.OnClickListener() {
                                public void onClick(final DialogInterface dialog, final int id) {
                                    dialog.cancel();
                                }
                            });

                    alert = builder.create();
                    alert.show();
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();

                boolean bool = false;
                try {
                    // try to create the file
                    bool = root.createNewFile();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }

                if (bool) {
                    // call the method again
                    createFile(fileName, csv_data, msg);
                } else {
                    //throw new IllegalStateException(getString(R.string.failed_to_create_csv));
                    SavedCurvesOK = false;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private String exportToEng(String motorClass, XYSeriesCollection lThrustCurveData, String msg) {

            String motorfile_data = ";File generated by RocketMotorTestStand \n;\n";
            //K1530 54 714 0 1.287998 1.499998 TruCore

            motorfile_data = motorfile_data + "\n";
            motorfile_data = motorfile_data + motorClass + " " + "motorDiam" + " " + "casinglength"
                    + " " + "delay" + " " + "propellantWeight" + " " + "totalWeight" + " " + "manufacturer" + "\n";

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
                    String currData = String.format("%.3f ", (thrustCurveData.getSeries(0).getX(k).floatValue() - thrustCurveData.getSeries(0).getX(curveStart).floatValue()) / 1000) + " " +
                            String.format("%.1f ", (thrustCurveData.getSeries(0).getY(k).floatValue() / 1000) * (float) 9.80665);
                    currData = currData.replace(",", ".");
                    motorfile_data = motorfile_data + currData + "\n";
                }
            }

            motorfile_data = motorfile_data + ";\n";
            SimpleDateFormat sdf = new SimpleDateFormat("_dd-MM-yyyy_hh-mm-ss");
            String date = sdf.format(System.currentTimeMillis());
            createFile(ThrustCurveName + "_" + motorClass + "_" + date + ".eng", motorfile_data, msg);
            return "RocketMotorTestStand/" + ThrustCurveName + "_" + motorClass + "_" + date + ".eng";
        }

        //Share file
        private void shareFile(File file) {

            Uri uri = FileProvider.getUriForFile(
                    getContext(),
                    getContext().getPackageName() +  ".provider",
                    file);

            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_SEND);
            intent.setType("file/*");
            intent.putExtra(android.content.Intent.EXTRA_TEXT, "MotorTestStand has shared with you some info");
            intent.putExtra(Intent.EXTRA_STREAM, uri);


            Intent chooser = Intent.createChooser(intent, "Share File");

            List<ResolveInfo> resInfoList = getContext().getPackageManager().queryIntentActivities(chooser, PackageManager.MATCH_DEFAULT_ONLY);

            for (ResolveInfo resolveInfo : resInfoList) {
                String packageName = resolveInfo.activityInfo.packageName;
                getContext().grantUriPermission(packageName, uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }

            try {
                this.startActivity(chooser);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(getContext(), "No App Available", Toast.LENGTH_SHORT).show();
            }
        }

    }

    private void takeScreenShot(View view) {
        Date date = new Date();
        CharSequence format = DateFormat.format("MM-dd-yyyy_hh:mm:ss", date);

        try {
            File mainDir = new File(
                    this.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "FilShare");
            if (!mainDir.exists()) {
                boolean mkdir = mainDir.mkdir();
            }

            String path = mainDir + "/" + "MotorCurve" + "-" + format + ".jpeg";
            view.setDrawingCacheEnabled(true);
            Bitmap bitmap = Bitmap.createBitmap(view.getDrawingCache());
            view.setDrawingCacheEnabled(false);


            File imageFile = new File(path);
            FileOutputStream fileOutputStream = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, fileOutputStream);
            fileOutputStream.flush();
            fileOutputStream.close();
            shareScreenShot(imageFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Share ScreenShot
    private void shareScreenShot(File imageFile) {

        Uri uri = FileProvider.getUriForFile(
                this,
                this.getPackageName() + "." + getLocalClassName() + ".provider",
                imageFile);


        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.setType("image/*");
        intent.putExtra(android.content.Intent.EXTRA_TEXT, "MotorTestStand has shared with you some info");
        intent.putExtra(Intent.EXTRA_STREAM, uri);

        try {
            this.startActivity(Intent.createChooser(intent, "Share With"));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No App Available", Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
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

        //share current screen with other app
        if (id == R.id.action_share) {
            takeScreenShot(getWindow().getDecorView());
        }

        /*if (id == R.id.action_share) {
            String currentEng = exportToEng(motorClass, lThrustCurveData);
            takeScreenShot(getWindow().getDecorView());
        }*/
        //open help screen
        if (id == R.id.action_help) {
            Intent i = new Intent(this, HelpActivity.class);
            i.putExtra("help_file", "help_curve");
            startActivity(i);
            return true;
        }
        //open about screen
        if (id == R.id.action_about) {
            Intent i = new Intent(this, AboutActivity.class);
            startActivity(i);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}