package com.rocketmotorteststand.Telemetry;

/**
 * @description: Live telemetry screen. A background thread reads the "telemetry" sentences
 * sent by the test stand while a motor is fired; the values are converted to the units chosen
 * in the settings, displayed and plotted every second.
 * @author: boris.dureau@neuf.fr
 **/

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.Html;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
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

import com.rocketmotorteststand.BoardCapabilities;
import com.rocketmotorteststand.ConsoleApplication;
import com.rocketmotorteststand.FragmentRecreationHelper;
import com.rocketmotorteststand.Help.AboutActivity;
import com.rocketmotorteststand.Help.HelpActivity;
import com.rocketmotorteststand.R;
import com.rocketmotorteststand.ShareHandler;
import com.rocketmotorteststand.Telemetry.TelemetryStatusFragment.TelemetryFcFragment;
import com.rocketmotorteststand.Telemetry.TelemetryStatusFragment.TelemetryMpFragment;
import com.rocketmotorteststand.UnitsHelper;

import org.afree.data.xy.XYSeries;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class TestStandTelemetryTabActivity extends AppCompatActivity {
    private ViewPager mViewPager;
    private SectionsPageAdapter adapter;
    private TextView[] dotsSlide;
    private LinearLayout linearDots;
    private TelemetryMpFragment telemetryPage1;
    private TelemetryFcFragment telemetryPage1bis;
    private volatile Thread rocketTelemetry;
    //telemetry var
    private int lastPlotTime = 0;
    private double CONVERT = 1.0;
    private double CONVERT_PRESSURE = 1.0;
    private String[] units = null;
    private ArrayList<Entry> yValuesThrust;
    private ArrayList<Entry> yValuesPressure;
    private ArrayList<Entry> yValuesPressure2;

    private XYSeries thrustSerie = null; //for afreeChart
    private XYSeries pressureSerie = null; //for afreeChart
    private XYSeries pressureSerie2 = null; //for afreeChart
    private int thrustTime = 0;
    // Written by the UI thread, read by the reading thread
    private volatile boolean telemetry = false;
    private boolean useAfreeChart = false;
    private String boardName;


    //buttons
    private Button dismissButton;

    //ref to the application
    private ConsoleApplication myBT;

    /**
     * Receives the values decoded by ConsoleApplication.ReadResult on the UI thread.
     */
    private final Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            String value = (String) msg.obj;
            switch (msg.what) {
                case 1:
                    // Value 1 contains the current thrust (grams, may be negative)
                    if (ConsoleApplication.isSignedNumber(value)) {
                        // float: the former (int) cast truncated the value, e.g. 850 g -> 0 kg
                        float thrust = (float) (ConsoleApplication.parseDoubleSafe(value, 0) * CONVERT);
                        setCurrentThrust(" " + format(thrust) + " " + units[0]);

                        // for MPChart
                        yValuesThrust.add(new Entry(thrustTime, thrust));
                        // for afreeChart
                        thrustSerie.add(thrustTime, thrust);

                        //plot every second
                        if ((thrustTime - lastPlotTime) > 1000) {
                            lastPlotTime = thrustTime;
                            if (!BoardCapabilities.hasPressure(boardName)) {
                                if (useAfreeChart) {
                                    if (telemetryPage1bis != null)
                                        telemetryPage1bis.plotThrust(thrustSerie);
                                } else if (telemetryPage1 != null) {
                                    telemetryPage1.plotThrust(yValuesThrust);
                                }
                            }
                        }
                    }
                    break;

                case 2:
                    // Value 2 contain the sample time
                    if (ConsoleApplication.isUnsignedNumber(value)) {
                        int time = ConsoleApplication.parseIntSafe(value, 0);
                        if (time > 0) {
                            thrustTime = time;
                        }
                    }
                    break;
                case 6:
                    //Value 6 contains the current pressure
                    if (BoardCapabilities.hasPressure(boardName) && ConsoleApplication.isSignedNumber(value)) {
                        float pressure = (float) (ConsoleApplication.parseDoubleSafe(value, 0) * CONVERT_PRESSURE);
                        setCurrentPressure(" " + format(pressure) + " " + units[1]);
                        // for MpChart
                        yValuesPressure.add(new Entry(thrustTime, pressure));
                        // for afreeChart
                        pressureSerie.add(thrustTime, pressure);

                        //plot every second
                        if ((thrustTime - lastPlotTime) > 1000) {
                            lastPlotTime = thrustTime;
                            plotThrustAndPressure();
                        }
                    }
                    break;
                case 7:
                    //Value 7 contains the current pressure of the second sensor
                    if (BoardCapabilities.hasPressure2(boardName) && ConsoleApplication.isSignedNumber(value)) {
                        float pressure2 = (float) (ConsoleApplication.parseDoubleSafe(value, 0) * CONVERT_PRESSURE);
                        setCurrentPressure2(" " + format(pressure2) + " " + units[2]);
                        // for MpChart
                        yValuesPressure2.add(new Entry(thrustTime, pressure2));
                        // for afreeChart
                        pressureSerie2.add(thrustTime, pressure2);

                        //plot every second
                        if ((thrustTime - lastPlotTime) > 1000) {
                            lastPlotTime = thrustTime;
                            plotThrustAndPressure();
                        }
                    }
                    break;
            }
        }
    };

    /** Formats a live value with 2 decimals. */
    private static String format(float value) {
        return String.format(Locale.getDefault(), "%.2f", value);
    }

    private void setCurrentThrust(String text) {
        if (useAfreeChart) {
            if (telemetryPage1bis != null) telemetryPage1bis.setCurrentThrust(text);
        } else if (telemetryPage1 != null) {
            telemetryPage1.setCurrentThrust(text);
        }
    }

    private void setCurrentPressure(String text) {
        if (useAfreeChart) {
            if (telemetryPage1bis != null) telemetryPage1bis.setCurrentPressure(text);
        } else if (telemetryPage1 != null) {
            telemetryPage1.setCurrentPressure(text);
        }
    }

    private void setCurrentPressure2(String text) {
        if (useAfreeChart) {
            if (telemetryPage1bis != null) telemetryPage1bis.setCurrentPressure2(text);
        } else if (telemetryPage1 != null) {
            telemetryPage1.setCurrentPressure2(text);
        }
    }

    private void plotThrustAndPressure() {
        if (useAfreeChart) {
            if (telemetryPage1bis != null)
                telemetryPage1bis.plotThrustAndPressure(thrustSerie, pressureSerie, pressureSerie2);
        } else if (telemetryPage1 != null) {
            telemetryPage1.plotThrustAndPressure(yValuesThrust, yValuesPressure, yValuesPressure2);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // The tab fragments are created below with their data: drop the empty ones that
        // Android may have restored, they would crash
        FragmentRecreationHelper.discardRestoredFragments(this);

        //get the bluetooth connection pointer
        myBT = (ConsoleApplication) getApplication();
        // Read the application config
        myBT.getAppConf().ReadConfig();
        myBT.setHandler(handler);
        boardName = myBT.getBoardName();
        useAfreeChart = (myBT.getAppConf().getGraphicsLibType() == 0) &&
                (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O);

        setContentView(R.layout.activity_teststand_tab_telemetry);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); //prevent the phone screen to switch off when in telemetry
        dismissButton = (Button) findViewById(R.id.butDismiss);

        // One label per value: thrust, pressure, pressure 2. The array used to be created
        // only for known boards, so it stayed null for the TestStandESP32V4 and crashed.
        units = new String[3];
        units[0] = UnitsHelper.thrustLabel(this, myBT.getAppConf());
        units[1] = UnitsHelper.pressureLabel(this, myBT.getAppConf());
        units[2] = units[1];
        CONVERT = UnitsHelper.thrustFactor(myBT.getAppConf());
        // The pressure unit used to be compared with the thrust unit setting by mistake
        CONVERT_PRESSURE = UnitsHelper.pressureFactor(myBT.getAppConf());

        yValuesThrust = new ArrayList<>();
        yValuesPressure = new ArrayList<>();
        yValuesPressure2 = new ArrayList<>();
        //for afreeChart
        thrustSerie = new XYSeries("thrust");
        pressureSerie = new XYSeries("pressure");
        pressureSerie2 = new XYSeries("pressure2");

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
        if (useAfreeChart) {
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

    /**
     * Starts the background thread that reads the telemetry sentences.
     * The downloaded thrust curves are no longer erased here (the former call to
     * initThrustCurveData() wiped them every time the telemetry screen was opened).
     */
    public void startTelemetry() {
        if (telemetry)
            return;
        telemetry = true;
        lastPlotTime = 0;

        final ConsoleApplication.StopCondition stopWhenClosed = new ConsoleApplication.StopCondition() {
            @Override
            public boolean shouldStop() {
                return !telemetry;
            }
        };
        Runnable r = new Runnable() {
            @Override
            public void run() {
                while (telemetry) {
                    myBT.ReadResult(100000, stopWhenClosed);
                }
            }
        };
        rocketTelemetry = new Thread(r, "telemetry-reader");
        rocketTelemetry.start();
    }

    /**
     * Stops the reading thread and, on the I/O background thread, waits for it to end before
     * sending the given commands. The former code did this on the UI thread with an
     * unbounded wait (application not responding) and let two threads read the same stream.
     *
     * @param commands commands to send once the reading thread has stopped
     * @param waitAnswer true to wait (at most 3 s) for the board answer
     */
    private void stopTelemetry(final String[] commands, final boolean waitAnswer) {
        telemetry = false;
        myBT.setExit(true);
        final Thread reader = rocketTelemetry;
        rocketTelemetry = null;
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
                for (String command : commands) {
                    myBT.flush();
                    myBT.clearInput();
                    myBT.write(command);
                    if (waitAnswer && myBT.waitForData(3000)) {
                        myBT.ReadResult(3000);
                    }
                }
                myBT.flush();
            }
        });
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        // Resume the telemetry when coming back to the screen (e.g. after sharing a screenshot)
        if (myBT.getConnected() && !telemetry) {
            myBT.getIoExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    myBT.flush();
                    myBT.clearInput();
                    myBT.write("y1;");
                }
            });
            myBT.setHandler(handler);
            startTelemetry();
        }
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (telemetry) {
            stopTelemetry(new String[]{"h;\n"}, true);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //turn off telemetry
        stopTelemetry(new String[]{"y0;\n"}, false);
        handler.removeCallbacksAndMessages(null);
        myBT.clearHandler(handler);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_application_config, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
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
