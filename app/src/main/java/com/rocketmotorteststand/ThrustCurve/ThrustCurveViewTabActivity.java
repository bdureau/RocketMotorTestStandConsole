package com.rocketmotorteststand.ThrustCurve;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import com.rocketmotorteststand.ConsoleApplication;
import com.rocketmotorteststand.GlobalConfig;
import com.rocketmotorteststand.Help.AboutActivity;
import com.rocketmotorteststand.Help.HelpActivity;
import com.rocketmotorteststand.R;
import com.rocketmotorteststand.ShareHandler;
import com.rocketmotorteststand.ThrustCurve.ThrustCurveView.ThrustCurveViewFcFragment;
import com.rocketmotorteststand.ThrustCurve.ThrustCurveView.ThrustCurveViewInfoFragment;
import com.rocketmotorteststand.ThrustCurve.ThrustCurveView.ThrustCurveViewMpFragment;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;

import android.text.Html;
import android.util.Log;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.afree.data.xy.XYSeries;
import org.afree.data.xy.XYSeriesCollection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class ThrustCurveViewTabActivity extends AppCompatActivity {
    private ThrustCurveData mythrustCurve = null;
    private ViewPager mViewPager;
    SectionsPageAdapter adapter;
    private boolean zoom = false;
    private TextView[] dotsSlide;
    private LinearLayout linearDots;

    // These are the activity tabs
    private ThrustCurveViewMpFragment ThrustCurvePage1 = null;
    private ThrustCurveViewFcFragment ThrustCurvePage1bis = null;
    private ThrustCurveViewInfoFragment ThrustCurvePage2 = null;

    //buttons
    private Button btnDismiss, butSelectCurves, butZoom;

    //ref to the application
    private ConsoleApplication myBT;

    private String curvesNames[] = null;
    private String currentCurvesNames[] = null;
    private boolean[] checkedItems = null;
    private XYSeriesCollection allThrustCurveData = null;

    private String ThrustCurveName = null;
    private String[] units = null;

    int numberOfCurves = 0;

    //Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static final String[] PERMISSION_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
    };


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

        if (Build.VERSION.SDK_INT >= 23)
            verifyStoragePermission(ThrustCurveViewTabActivity.this);

        // recovering the instance state
        if (savedInstanceState != null) {
            currentCurvesNames = savedInstanceState.getStringArray("CURRENT_CURVES_NAMES_KEY");
            checkedItems = savedInstanceState.getBooleanArray("CHECKED_ITEMS_KEY");
        }

        //get the bluetooth connection pointer
        myBT = (ConsoleApplication) getApplication();

        setContentView(R.layout.activity_thrustcurve_view_tab);
        mViewPager = (ViewPager) findViewById(R.id.container);

        btnDismiss = (Button) findViewById(R.id.butDismiss);
        butSelectCurves = (Button) findViewById(R.id.butSelectCurves);
        butZoom = (Button) findViewById(R.id.butZoom);
        butZoom.setCompoundDrawablesWithIntrinsicBounds(R.drawable.zoom_30x30_trans,0,0,0);

        numberOfCurves = 1;
        if (myBT.getTestStandConfigData().getTestStandName().equals("TestStandSTM32V2") ||
                myBT.getTestStandConfigData().getTestStandName().equals("TestStandESP32")) {
            numberOfCurves = 2;
        }
        if (myBT.getTestStandConfigData().getTestStandName().equals("TestStandSTM32V3") ||
                myBT.getTestStandConfigData().getTestStandName().equals("TestStandESP32V3")) {
            numberOfCurves = 3;
        }

        Intent newint = getIntent();
        ThrustCurveName = newint.getStringExtra(ThrustCurveListActivity.SELECTED_THRUSTCURVE);
        mythrustCurve = myBT.getThrustCurveData();
        // get all the data that we have recorded for the current thrustCurve
        allThrustCurveData = mythrustCurve.GetThrustCurveData(ThrustCurveName);

        // by default we will display the thrust
        // but then the user will be able to change the data
        Log.d("numberOfCurves", "numberOfCurves:" + allThrustCurveData.getSeries().size());
        curvesNames = new String[numberOfCurves];
        units = new String[numberOfCurves];
        for (int i = 0; i < numberOfCurves; i++) {
            curvesNames[i] = allThrustCurveData.getSeries(i).getKey().toString();
            Log.d("numberOfCurves", allThrustCurveData.getSeries(i).getKey().toString());
        }

        // Read the application config
        myBT.getAppConf().ReadConfig();
        if (myBT.getAppConf().getUnits()== GlobalConfig.ThrustUnits.KG) {
            //kg
            units[0] = "(" + getResources().getString(R.string.Kg_fview) + ")";
            //CONVERT = 1.0 / 1000;

        } else if (myBT.getAppConf().getUnits()== GlobalConfig.ThrustUnits.POUNDS) {
            //pounds
            units[0] = getResources().getString(R.string.Pounds_fview);
            //CONVERT = 2.20462 / 1000;

        } else if (myBT.getAppConf().getUnits()== GlobalConfig.ThrustUnits.NEWTONS) {
            //newtons
            units[0] = getResources().getString(R.string.Newtons_fview);
            //CONVERT = 9.80665 / 1000;
        }

        if (numberOfCurves > 1) {
            if (myBT.getAppConf().getUnitsPressure()== GlobalConfig.PressureUnits.PSI) {
                //PSI
                units[1] = "(" + "PSI" + ")";
                //CONVERT_PRESSURE = 1;
            } else if (myBT.getAppConf().getUnits()== GlobalConfig.PressureUnits.BAR) {
                //BAR
                units[1] = "BAR";
                //CONVERT_PRESSURE = 0.0689476;
            } else if (myBT.getAppConf().getUnits()== GlobalConfig.PressureUnits.KPascal) {
                //Kpascal
                units[1] = "Kpascal";
                //CONVERT_PRESSURE = 6.89476;
            }
        }

        if (numberOfCurves > 2) {
            if (myBT.getAppConf().getUnitsPressure()== GlobalConfig.PressureUnits.PSI) {
                //PSI
                units[2] = "(" + "PSI" + ")";
            } else if (myBT.getAppConf().getUnits()== GlobalConfig.PressureUnits.BAR) {
                //BAR
                units[2] = "BAR";
            } else if (myBT.getAppConf().getUnits()== GlobalConfig.PressureUnits.KPascal) {
                //Kpascal
                units[2] = "Kpascal";
            }
        }

        if (currentCurvesNames == null) {
            //This is the first time so only display the thrust
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
                if(zoom)
                    zoom = false;
                else
                    zoom = true;
                if ((myBT.getAppConf().getGraphicsLibType()==0 ) &
                        (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O)) {
                    if(zoom) {
                        ThrustCurvePage1bis.zoomCurves();
                        butZoom.setCompoundDrawablesWithIntrinsicBounds(R.drawable.zoom_out_30x30_trans,
                                0,0,0);
                    }
                    else {
                        ThrustCurvePage1bis.setCheckedItems(checkedItems);
                        ThrustCurvePage1bis.drawGraph();
                        ThrustCurvePage1bis.drawAllCurves(allThrustCurveData);
                        butZoom.setCompoundDrawablesWithIntrinsicBounds(R.drawable.zoom_30x30_trans,
                                0,0,0);
                    }
                } else {
                    if(zoom) {
                        ThrustCurvePage1.zoomCurves();
                        butZoom.setCompoundDrawablesWithIntrinsicBounds(R.drawable.zoom_out_30x30_trans,
                                0,0,0);

                    }
                    else {
                        ThrustCurvePage1.setCheckedItems(checkedItems);
                        ThrustCurvePage1.drawGraph();
                        ThrustCurvePage1.drawAllCurves(allThrustCurveData);
                        butZoom.setCompoundDrawablesWithIntrinsicBounds(R.drawable.zoom_30x30_trans,
                                0,0,0);
                    }
                }
            }
        });
        butSelectCurves.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Set up the alert builder
                AlertDialog.Builder builder = new AlertDialog.Builder(ThrustCurveViewTabActivity.this);
                if(curvesNames.length>0)
                    checkedItems = new boolean[curvesNames.length];
                else {
                    checkedItems = new boolean[1];
                    checkedItems[0]= true;
                }
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
                        if ((myBT.getAppConf().getGraphicsLibType()==0 ) &
                                (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O)) {
                            ThrustCurvePage1bis.setCheckedItems(checkedItems);
                        } else {
                            ThrustCurvePage1.setCheckedItems(checkedItems);
                        }

                        int nbrOfItems =0;
                        for (int i = 0; i < checkedItems.length; i++) {

                            //count nbr of selected
                            if(checkedItems[i]) {
                                nbrOfItems++;
                            }
                        }
                        if(nbrOfItems > 0)
                            currentCurvesNames = new String[nbrOfItems];
                        else {
                            currentCurvesNames = new String[1];
                            checkedItems[0] = true;
                        }

                        int k=0;
                        for(int j=0; j< curvesNames.length; j++){
                            if(checkedItems[j]){
                                currentCurvesNames[k] = curvesNames[j];
                                k++;
                            }
                        }
                        if ((myBT.getAppConf().getGraphicsLibType()==0 ) &
                                (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O)) {
                            ThrustCurvePage1bis.drawGraph();
                            ThrustCurvePage1bis.drawAllCurves(allThrustCurveData);
                        } else {
                            ThrustCurvePage1.drawGraph();
                            ThrustCurvePage1.drawAllCurves(allThrustCurveData);
                        }
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

        if ((myBT.getAppConf().getGraphicsLibType()==0 ) &
                (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O)) {
            ThrustCurvePage1bis = new ThrustCurveViewFcFragment(allThrustCurveData,
                    myBT,
                    curvesNames,
                    checkedItems,
                    units);
            adapter.addFragment(ThrustCurvePage1bis, "TAB1");
        } else {
            ThrustCurvePage1 = new ThrustCurveViewMpFragment(allThrustCurveData,
                    myBT,
                    curvesNames,
                    checkedItems,
                    units);
            adapter.addFragment(ThrustCurvePage1, "TAB1");
        }
        ThrustCurvePage2 = new ThrustCurveViewInfoFragment(mythrustCurve,
                myBT,
                units,
                ThrustCurveName);

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
            if(i ==0) {
                butSelectCurves.setVisibility(View.VISIBLE);
                butZoom.setVisibility(View.VISIBLE);
            } else {
                butSelectCurves.setVisibility(View.INVISIBLE);
                butZoom.setVisibility(View.INVISIBLE);
            }
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

                    if ((searchVal <= serie.getY(i - 1).doubleValue()) && (searchVal >= serie.getY(i).doubleValue())) {
                        pos = i;
                        break;
                    }
                }
            }
            return pos;
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
            ShareHandler.takeScreenShot(findViewById(android.R.id.content).getRootView(), this);
        }

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