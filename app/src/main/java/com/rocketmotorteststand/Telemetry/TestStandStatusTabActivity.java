package com.rocketmotorteststand.Telemetry;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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

import com.rocketmotorteststand.BoardCapabilities;
import com.rocketmotorteststand.ConsoleApplication;
import com.rocketmotorteststand.FragmentRecreationHelper;
import com.rocketmotorteststand.UnitsHelper;
import com.rocketmotorteststand.Help.AboutActivity;
import com.rocketmotorteststand.Help.HelpActivity;
import com.rocketmotorteststand.R;
import com.rocketmotorteststand.ShareHandler;
import com.rocketmotorteststand.Telemetry.TelemetryStatusFragment.TestStandStatusFragment;


import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * @description: Test stand status screen. A background thread reads the status sentences
 * (thrust, pressures, battery voltage, eeprom usage, number of curves) and shows them in the
 * status tab. Also allows a manual recording to be started.
 * @author: boris.dureau@neuf.fr
 **/
public class TestStandStatusTabActivity extends AppCompatActivity {
    private ViewPager mViewPager;
    private SectionsPageAdapter adapter;
    private TextView[] dotsSlide;
    private LinearLayout linearDots;
    private Button btnDismiss, btnRecording;
    private ConsoleApplication myBT;
    // Written by the UI thread, read by the reading thread
    private volatile Thread testStandStatus;
    private volatile boolean status = false;
    //boolean recording = false;
    private double CONVERT = 1;
    private double CONVERT_PRESSURE = 1;
    private String[] units = null;
    private TestStandStatusFragment statusPage1;
    private String TAG ="TestStandStatusTabActivity";


    /**
     * Receives the values decoded by ConsoleApplication.ReadResult on the UI thread.
     */
    private final Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if (statusPage1 == null)
                return;
            String value = (String) msg.obj;
            switch (msg.what) {
                case 1:
                    // Current thrust in grams, may be negative around zero. Negative values
                    // used to be displayed raw (in grams) next to the kg/lb/N unit.
                    if (ConsoleApplication.isSignedNumber(value)) {
                        double temp = ConsoleApplication.parseDoubleSafe(value, 0);
                        statusPage1.setThrust(String.format(Locale.getDefault(), "%.4f", temp * CONVERT)
                                + " " + units[0]);
                    }
                    break;
                case 3:
                    //Value 3 contains the battery voltage
                    if (ConsoleApplication.isUnsignedNumber(value)) {
                        statusPage1.setBatteryVoltage(value);
                    } else {
                        statusPage1.setBatteryVoltage("NA");
                    }
                    break;
                case 4:
                    //Value 4 contains the EEprom usage
                    if (ConsoleApplication.isUnsignedNumber(value)) {
                        statusPage1.setEEpromUsage(value);
                    }
                    break;

                case 5:
                    //Value 5 contains the number of thrust curves
                    if (ConsoleApplication.isUnsignedNumber(value)) {
                        statusPage1.setNbrOfThrustCurve(value);
                    }
                    break;

                case 6:
                    //Value 6 contains the current pressure
                    if (BoardCapabilities.hasPressure(myBT.getBoardName())
                            && ConsoleApplication.isSignedNumber(value)) {
                        double temp = ConsoleApplication.parseDoubleSafe(value, 0);
                        statusPage1.setPressure(String.format(Locale.getDefault(), "%.2f", temp * CONVERT_PRESSURE)
                                + " " + units[1]);
                    }
                    break;
                case 7:
                    //Value 7 contains the current pressure from the second sensor
                    if (BoardCapabilities.hasPressure2(myBT.getBoardName())
                            && ConsoleApplication.isSignedNumber(value)) {
                        double temp = ConsoleApplication.parseDoubleSafe(value, 0);
                        statusPage1.setPressure2(String.format(Locale.getDefault(), "%.2f", temp * CONVERT_PRESSURE)
                                + " " + units[1]);
                    }
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
        // The tab fragment is created below with its data: drop the empty one that Android
        // may have restored, it would crash
        FragmentRecreationHelper.discardRestoredFragments(this);
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

        // Unit labels and conversion factors. The pressure unit used to be compared with the
        // thrust unit setting by mistake, so bar and kPa were never applied.
        units = new String[2];
        units[0] = UnitsHelper.thrustLabel(this, myBT.getAppConf());
        units[1] = UnitsHelper.pressureLabel(this, myBT.getAppConf());
        CONVERT = UnitsHelper.thrustFactor(myBT.getAppConf());
        CONVERT_PRESSURE = UnitsHelper.pressureFactor(myBT.getAppConf());

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


        myBT.setHandler(handler);
        startStatusThread();
    }

    /**
     * Starts the background thread reading the status sentences sent by the board.
     */
    private void startStatusThread() {
        status = true;
        final ConsoleApplication.StopCondition stopWhenClosed = new ConsoleApplication.StopCondition() {
            @Override
            public boolean shouldStop() {
                return !status;
            }
        };
        Runnable r = new Runnable() {
            @Override
            public void run() {
                while (status) {
                    myBT.ReadResult(10000, stopWhenClosed);
                }
            }
        };
        testStandStatus = new Thread(r, "status-reader");
        testStandStatus.start();
    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume()");
        super.onResume();
        if (myBT.getConnected() && !status) {
            myBT.flush();
            myBT.clearInput();

            myBT.write("y1;".toString());
            myBT.setHandler(handler);
            startStatusThread();
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

        // Stop the reading thread, then (on the I/O background thread, once the reader has
        // ended so that two threads never read the same stream) switch the telemetry off
        status = false;
        myBT.setExit(true);
        final Thread reader = testStandStatus;
        testStandStatus = null;
        myBT.getIoExecutor().execute(new Runnable() {
            @Override
            public void run() {
                if (reader != null) {
                    try {
                        reader.join(1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
                if (!myBT.getConnected())
                    return;
                myBT.flush();
                myBT.clearInput();
                myBT.write("h;\n");
                myBT.flush();
                myBT.clearInput();
                //turn off telemetry
                myBT.write("y0;\n");
                myBT.flush();
            }
        });
        handler.removeCallbacksAndMessages(null);
        myBT.clearHandler(handler);
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
