package com.rocketmotorteststand.telemetry;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;
import androidx.annotation.Nullable;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.github.mikephil.charting.data.Entry;

import com.rocketmotorteststand.ConsoleApplication;
import com.rocketmotorteststand.GlobalConfig;
import com.rocketmotorteststand.Help.AboutActivity;
import com.rocketmotorteststand.Help.HelpActivity;
import com.rocketmotorteststand.R;
import com.rocketmotorteststand.ShareHandler;
import com.rocketmotorteststand.telemetry.TelemetryStatusFragment.TelemetryFcFragment;
import com.rocketmotorteststand.telemetry.TelemetryStatusFragment.TelemetryMpFragment;
import org.afree.data.xy.XYSeries;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class TestStandTelemetryTabActivity extends AppCompatActivity {
    private ViewPager mViewPager;
    private SectionsPageAdapter adapter;
    private TextView[] dotsSlide;
    private LinearLayout linearDots;
    private TelemetryMpFragment telemetryPage1;
    private TelemetryFcFragment telemetryPage1bis;
    private Thread rocketTelemetry;
    //telemetry var
    private int lastPlotTime = 0;
    private double CONVERT = 1.0;
    private double CONVERT_PRESSURE = 1.0;
    private String[] units = null;
    private ArrayList<Entry> yValuesThrust;
    private ArrayList<Entry> yValuesPressure;

    private XYSeries thrustSerie =null; //for afreeChart
    private XYSeries pressureSerie =null; //for afreeChart
    private int thrustTime = 0;
    private boolean telemetry = true;


    //buttons
    private Button dismissButton;

    //ref to the application
    private ConsoleApplication myBT;

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    // Value 1 contain the current thrust
                    if (((String) msg.obj).matches("\\d+(?:\\.\\d+)?")) {
                        int thrust = (int) (Integer.parseInt((String) msg.obj) * CONVERT);
                        if ((myBT.getAppConf().getGraphicsLibType() == 0) &
                                (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O)) {
                            telemetryPage1bis.setCurrentThrust(" " + thrust + " " + units[0]);
                        } else {
                            telemetryPage1.setCurrentThrust(" " + thrust + " " + units[0]);
                        }

                        // for MPChart
                        yValuesThrust.add(new Entry(thrustTime, thrust));
                        // for afreeChart
                        thrustSerie.add(thrustTime, thrust);

                        //plot every seconde
                        if ((thrustTime - lastPlotTime) > 1000) {
                            lastPlotTime = thrustTime;
                            if (myBT.getTestStandConfigData().getTestStandName().equals("TestStand") ||
                                    myBT.getTestStandConfigData().getTestStandName().equals("TestStandSTM32")) {
                                if ((myBT.getAppConf().getGraphicsLibType() == 0) &
                                        (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O)) {
                                    telemetryPage1bis.plotThrust(thrustSerie);
                                } else {
                                    telemetryPage1.plotThrust(yValuesThrust);
                                }
                            }
                        }
                    }
                    break;

                case 2:
                    // Value 2 contain the sample time
                    if (((String) msg.obj).matches("\\d+(?:\\.\\d+)?")) {
                        if (Integer.parseInt((String) msg.obj) > 0) {
                            thrustTime = Integer.parseInt((String) msg.obj);
                        }
                    }
                    break;
                case 6:
                    //Value 6 contains the current pressure
                    if (myBT.getTestStandConfigData().getTestStandName().equals("TestStandSTM32V2") ||
                            myBT.getTestStandConfigData().getTestStandName().equals("TestStandESP32")) {
                        if (((String) msg.obj).matches("\\d+(?:\\.\\d+)?")) {
                            int pressure = (int) (Integer.parseInt((String) msg.obj) * CONVERT_PRESSURE);
                            if ((myBT.getAppConf().getGraphicsLibType() == 0) &
                                    (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O)) {
                                telemetryPage1bis.setCurrentPressure(" " + pressure + " " + units[1]);
                            } else {
                                telemetryPage1.setCurrentPressure(" " + pressure + " " + units[1]);
                            }
                            // for MpChart
                            yValuesPressure.add(new Entry(thrustTime, pressure));
                            // for afreeChart
                            pressureSerie.add(thrustTime, pressure);

                            //plot every seconde
                            if ((thrustTime - lastPlotTime) > 1000) {
                                lastPlotTime = thrustTime;
                                if (myBT.getTestStandConfigData().getTestStandName().equals("TestStandSTM32V2") ||
                                        myBT.getTestStandConfigData().getTestStandName().equals("TestStandESP32")) {
                                    if ((myBT.getAppConf().getGraphicsLibType() == 0) &
                                            (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O)) {
                                        telemetryPage1bis.plotThrustAndPressure(thrustSerie, pressureSerie);
                                    } else {
                                        telemetryPage1.plotThrustAndPressure(yValuesThrust, yValuesPressure);
                                    }
                                }
                            }
                        }
                    }
                    break;
            }
        }
    };



    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //get the bluetooth connection pointer
        myBT = (ConsoleApplication) getApplication();
        // Read the application config
        myBT.getAppConf().ReadConfig();
        myBT.setHandler(handler);

        setContentView(R.layout.activity_teststand_tab_telemetry);

        dismissButton = (Button) findViewById(R.id.butDismiss);



        if (myBT.getTestStandConfigData().getTestStandName().equals("TestStand") ||
                myBT.getTestStandConfigData().getTestStandName().equals("TestStandSTM32"))
            units = new String[1];
        else
            units = new String[2];

        if (myBT.getAppConf().getUnits()==GlobalConfig.ThrustUnits.KG) {
            //kg
            units[0] = getResources().getString(R.string.Kg_fview);
            CONVERT = 1.0/1000.0;
        }
        else if (myBT.getAppConf().getUnits()==GlobalConfig.ThrustUnits.POUNDS) {
            //pounds
            units[0] = getResources().getString(R.string.Pounds_fview);
            CONVERT = 2.20462/1000.0;
        }
        else if (myBT.getAppConf().getUnits()==GlobalConfig.ThrustUnits.NEWTONS) {
            //newtons
            units[0] = getResources().getString(R.string.config_unit_newtons);
            CONVERT = 9.80665/1000.0;
        }

        if (myBT.getTestStandConfigData().getTestStandName().equals("TestStandSTM32V2") ||
                myBT.getTestStandConfigData().getTestStandName().equals("TestStandESP32")) {
            if (myBT.getAppConf().getUnitsPressure() == GlobalConfig.PressureUnits.PSI) {
                //PSI
                units[1] = "(" + "PSI" + ")";
                CONVERT_PRESSURE = 1.0;
            } else if (myBT.getAppConf().getUnits() == GlobalConfig.PressureUnits.BAR) {
                //BAR
                units[1] = "BAR";
                CONVERT_PRESSURE = 0.0689476;
            } else if (myBT.getAppConf().getUnits() == GlobalConfig.PressureUnits.KPascal) {
                //Kpascal
                units[1] = "Kpascal";
                CONVERT_PRESSURE = 6.89476;
            }
        }

        yValuesThrust = new ArrayList<>();
        yValuesPressure = new ArrayList<>();
        //for afreeChart
        thrustSerie = new XYSeries("thrust");
        pressureSerie = new XYSeries("pressure");

        mViewPager = (ViewPager) findViewById(R.id.container);
        setupViewPager(mViewPager);
        startTelemetry();
        dismissButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();      //exit the activity
            }
        });
    }

    private void setupViewPager(ViewPager viewPager) {
        adapter = new SectionsPageAdapter(getSupportFragmentManager());
        if ((myBT.getAppConf().getGraphicsLibType() == 0) &
                (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O)) {
            telemetryPage1bis = new TelemetryFcFragment(myBT, units);
            adapter.addFragment(telemetryPage1bis, "TAB1");
        } else {
            telemetryPage1 = new TelemetryMpFragment(myBT, units);
            adapter.addFragment(telemetryPage1, "TAB1");
        }

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

    public void startTelemetry() {
        telemetry = true;

        lastPlotTime = 0;
        myBT.initThrustCurveData();


        //myThrustCurve = myBT.getThrustCurveData(); //for afreeChart

        Runnable r = new Runnable() {

            @Override
            public void run() {
                while (true) {
                    if (!telemetry) break;
                    myBT.ReadResult(100000);
                }
            }
        };
        rocketTelemetry = new Thread(r);
        rocketTelemetry.start();
    }

    @Override
    public void onBackPressed() {
        finish();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (telemetry) {
            telemetry = false;
            myBT.write("h;\n".toString());

            myBT.setExit(true);
            myBT.clearInput();
            myBT.flush();
        }
        //turn off telemetry
        myBT.flush();
        myBT.clearInput();
        myBT.write("y0;\n".toString());

    }
    @Override
    protected void onStop() {
        //msg("On stop");
        super.onStop();
        if (telemetry) {
            telemetry = false;
            myBT.write("h;\n".toString());

            myBT.setExit(true);
            myBT.clearInput();
            myBT.flush();
        }


        myBT.flush();
        myBT.clearInput();
        myBT.write("h;\n".toString());
        try {
            while (myBT.getInputStream().available() <= 0) ;
        } catch (IOException e) {

        }
        String myMessage = "";
        long timeOut = 10000;
        long startTime = System.currentTimeMillis();

        myMessage = myBT.ReadResult(100000);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_application_config, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //share screen
        if (id == R.id.action_share) {
            ShareHandler.takeScreenShot(findViewById(android.R.id.content).getRootView(), this);
            return true;
        }

        //open help screen
        if (id == R.id.action_help) {
            Intent i = new Intent(TestStandTelemetryTabActivity.this, HelpActivity.class);
            i.putExtra("help_file", "help_telemetry");
            startActivity(i);
            return true;
        }

        if (id == R.id.action_about) {
            Intent i = new Intent(TestStandTelemetryTabActivity.this, AboutActivity.class);
            startActivity(i);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}