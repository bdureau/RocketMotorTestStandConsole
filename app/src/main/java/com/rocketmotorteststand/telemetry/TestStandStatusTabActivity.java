package com.rocketmotorteststand.telemetry;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.rocketmotorteststand.ConsoleApplication;
import com.rocketmotorteststand.GlobalConfig;
import com.rocketmotorteststand.Help.AboutActivity;
import com.rocketmotorteststand.Help.HelpActivity;
import com.rocketmotorteststand.R;
import com.rocketmotorteststand.ShareHandler;
import com.rocketmotorteststand.telemetry.TelemetryStatusFragment.TestStandStatusFragment;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TestStandStatusTabActivity extends AppCompatActivity {
    private ViewPager mViewPager;
    private SectionsPageAdapter adapter;
    private TextView[] dotsSlide;
    private LinearLayout linearDots;
    private Button btnDismiss, btnRecording;
    private ConsoleApplication myBT;
    Thread testStandStatus;
    boolean status = true;
    //boolean recording = false;
    private double CONVERT = 1;
    private double CONVERT_PRESSURE = 1;
    private String[] units = null;
    private TestStandStatusFragment statusPage1;
    private String TAG ="TestStandStatusTabActivity";

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    String thrust = (String) msg.obj;
                    if (thrust.matches("\\d+(?:\\.\\d+)?")) {
                        double temp;
                        temp = Double.parseDouble(thrust);
                        thrust = String.format("%.2f", (temp ) * CONVERT);
                    }
                    statusPage1.setThrust(thrust + " " + units[0]);
                    break;
                case 3:
                    //Value 4 contains the battery voltage
                    String voltage = (String) msg.obj;
                    if (voltage.matches("\\d+(?:\\.\\d+)?")) {
                        statusPage1.setBatteryVoltage(voltage);
                    } else {
                        statusPage1.setBatteryVoltage("NA");
                    }
                    break;
                case 4:
                    //Value 4 contains the EEprom usage
                    String eepromUsage = (String) msg.obj;
                    if (eepromUsage.matches("\\d+(?:\\.\\d+)?")) {
                        // if eeprom usage is 100% put it in red to show that it is full
                        statusPage1.setEEpromUsage(eepromUsage);
                    }

                    break;

                case 5:
                    //Value 5 contains the number of thrust curves
                    String nbrOfThrustCurve = (String) msg.obj;
                    if (nbrOfThrustCurve.matches("\\d+(?:\\.\\d+)?")) {
                        // If we have the maximum of thrust curve put it in red
                        statusPage1.setNbrOfThrustCurve(nbrOfThrustCurve);
                    }
                    //Log.d("TestStandStatus", (String) msg.obj);
                    break;

                case 6:
                    //Value 6 contains the current pressure
                    String currentPressure = (String) msg.obj;
                    if (myBT.getTestStandConfigData().getTestStandName().equals("TestStandSTM32V2") ||
                            myBT.getTestStandConfigData().getTestStandName().equals("TestStandSTM32V2")) {
                        if (currentPressure.matches("\\d+(?:\\.\\d+)?")) {
                            double temp;
                            temp = Double.parseDouble(currentPressure);
                            currentPressure = String.format("%.2f", (temp) * CONVERT_PRESSURE);
                            statusPage1.setPressure(currentPressure + " " + units[1]);
                        }
                    }
                    //Log.d("TestStandStatus", (String) msg.obj);
                    break;
            }
        }
    };


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
        setContentView(R.layout.activity_teststand_tab_status);
        myBT = (ConsoleApplication) getApplication();
        // Read the application config
        myBT.getAppConf().ReadConfig();


        btnDismiss = (Button) findViewById(R.id.butDismiss);
        btnRecording = (Button) findViewById(R.id.butRecording);
        //btnRecording.setVisibility(View.INVISIBLE);

        if ( myBT.getAppConf().getManualRecording())
            btnRecording.setVisibility(View.VISIBLE);
        else
            btnRecording.setVisibility(View.INVISIBLE);

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
                myBT.getTestStandConfigData().getTestStandName().equals("TestStandSTM32V2")) {
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

        mViewPager = (ViewPager) findViewById(R.id.container);
        setupViewPager(mViewPager);

        btnDismiss.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();      //exit the  activity
            }
        });

        btnRecording.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myBT.write("w;\n".toString());
                myBT.clearInput();
                myBT.flush();
                Log.d(TAG, "recording");
                msg("Recording for few seconds");
            }
        });


        Runnable r = new Runnable() {
            @Override
            public void run() {
                while (true) {
                    if (!status) break;
                    myBT.ReadResult(10000);
                }
            }
        };
        testStandStatus = new Thread(r);
        testStandStatus.start();
        myBT.setHandler(handler);
    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume()");
        super.onResume();
        if (myBT.getConnected() && !status) {
            myBT.flush();
            myBT.clearInput();

            myBT.write("y1;".toString());
            status = true;

            Log.d(TAG, "onResume2()");
            Runnable r = new Runnable() {

                @Override
                public void run() {
                    while (true) {
                        if (!status) break;
                        myBT.ReadResult(10000);
                    }
                }
            };
            Log.d(TAG, "onResume3()");
            testStandStatus = new Thread(r);
            testStandStatus.start();
            Log.d(TAG, "onResume4()");
        }
    }
    @Override
    public void onRestart() {
        Log.d(TAG, "onRestart()");
        super.onRestart();
    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause()");
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "onBackPressed()");
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy()");
        super.onDestroy();

        if (status) {
            status = false;
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
        super.onStop();
       //msg("On stop");
    }

    // fast way to call Toast
    private void msg(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
    }
    private void setupViewPager(ViewPager viewPager) {
        adapter = new SectionsPageAdapter(getSupportFragmentManager());

        statusPage1 = new TestStandStatusFragment(myBT, units);
        adapter.addFragment(statusPage1, "TAB1");


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
            Intent i = new Intent(TestStandStatusTabActivity.this, HelpActivity.class);
            i.putExtra("help_file", "help_telemetry");
            startActivity(i);
            return true;
        }

        if (id == R.id.action_about) {
            Intent i = new Intent(TestStandStatusTabActivity.this, AboutActivity.class);
            startActivity(i);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
