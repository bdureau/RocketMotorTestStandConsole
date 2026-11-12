package com.rocketmotorteststand.config;
/**
 * @description: Retrieve test stand configuration and show it in tabs
 * The user can then load it back to the test stand
 * @author: boris.dureau@neuf.fr
 **/
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import android.app.AlertDialog;
import android.content.DialogInterface;

import androidx.annotation.Nullable;

import android.app.ProgressDialog;
import android.content.Intent;

import android.os.AsyncTask;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.rocketmotorteststand.BoardCapabilities;
import com.rocketmotorteststand.ConsoleApplication;
import com.rocketmotorteststand.FragmentRecreationHelper;
import com.rocketmotorteststand.Help.AboutActivity;
import com.rocketmotorteststand.Help.HelpActivity;
import com.rocketmotorteststand.R;

import java.util.ArrayList;
import java.util.List;

//tooltip library
import com.rocketmotorteststand.ShareHandler;
import com.rocketmotorteststand.config.TestStandConfig.TestStandConfigTab1Fragment;
import com.rocketmotorteststand.config.TestStandConfig.TestStandConfigTab2Fragment;


public class TestStandTabConfigActivity extends AppCompatActivity {
    private static final String TAG = "TestStandTabConfigActivity";

    private ViewPager mViewPager;
    SectionsPageAdapter adapter;
    private TestStandConfigTab1Fragment configPage2 = null;
    private TestStandConfigTab2Fragment configPage3 = null;
    private Button btnDismiss, btnUpload, btnCalibrate;
    private ConsoleApplication myBT;
    private TestStandConfigData TestStandCfg = null;
    private ProgressDialog progress;
    private AlertDialog alert;
    private boolean calibrationComplete = false;

    private TextView[] dotsSlide;
    private LinearLayout linearDots;

    /**
     * Receives the calibration values decoded by ConsoleApplication.ReadResult.
     */
    private final Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if (configPage3 == null)
                return;
            String value = (String) msg.obj;
            switch (msg.what) {
                case 1:
                    // Value 1 contains the offset (single digit and negative values accepted)
                    Log.d("Calibration", "offset: " + value);
                    if (ConsoleApplication.isSignedNumber(value)) {
                        configPage3.setCurrentOffset(value);
                    }
                    break;
                case 2:
                    // Value 2 contains the calibration
                    Log.d("Calibration", "factor: " + value);
                    if (ConsoleApplication.isSignedNumber(value)) {
                        configPage3.setCalibrationFactor(value);
                        if (alert != null)
                            alert.setMessage(value);
                    }
                    break;
                case 3:
                    // Value 3 contains the flag
                    if ("Done".equals(value))
                        calibrationComplete = true;
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // The tab fragments are created below with their data: drop the empty ones that
        // Android may have restored, they would crash
        FragmentRecreationHelper.discardRestoredFragments(this);
        //get the bluetooth connection pointer
        myBT = (ConsoleApplication) getApplication();

        setContentView(R.layout.activity_teststand_tab_config);

        mViewPager = (ViewPager) findViewById(R.id.container);
        myBT.setHandler(handler);
        btnCalibrate = (Button) findViewById(R.id.butCalibrate);
        btnCalibrate.setVisibility(View.INVISIBLE);
        btnCalibrate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                calibrationComplete =false;

                if(configPage3.getCalibrationWeight().equals("0")) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(TestStandTabConfigActivity.this);
                    //Display info message
                    builder.setMessage(R.string.calibration_weight_message)
                            .setTitle(R.string.calibration_weight_warning)
                            .setCancelable(false)
                            .setPositiveButton(R.string.calibration_weight_ok, new DialogInterface.OnClickListener() {
                                public void onClick(final DialogInterface dialog, final int id) {
                                    dialog.cancel();
                                }
                            });

                    AlertDialog warning = builder.create();
                    warning.show();
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(TestStandTabConfigActivity.this);
                    //Display info message
                    builder.setMessage(R.string.remove_any_weight_and_click_ok)
                            .setTitle("Info")
                            .setCancelable(false)
                            .setPositiveButton(R.string.calibration_weight_ok, new DialogInterface.OnClickListener() {
                                public void onClick(final DialogInterface dialog, final int id) {
                                    dialog.cancel();
                                    myBT.flush();
                                    myBT.clearInput();
                                    // tare teststand
                                    myBT.write("j;");


                                    AlertDialog.Builder builder2 = new AlertDialog.Builder(TestStandTabConfigActivity.this);
                                    //Display info message
                                    builder2.setMessage(R.string.put_your_calibration_weight_and_click_ok)
                                            .setTitle("Info")
                                            .setCancelable(false)
                                            .setPositiveButton(R.string.calibration_weight_ok, new DialogInterface.OnClickListener() {
                                                public void onClick(final DialogInterface dialog, final int id) {
                                                    dialog.cancel();
                                                    new Calibration().execute();
                                                }
                                            });

                                    AlertDialog info2 = builder2.create();
                                    info2.show();
                                }
                            });

                    AlertDialog info = builder.create();
                    info.show();

                }
            }
        });
        btnDismiss = (Button) findViewById(R.id.butDismiss);
        btnDismiss.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();      //exit the application configuration activity
            }
        });

        btnUpload = (Button) findViewById(R.id.butUpload);
        btnUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //send back the config to the test Stand and exit if successful
                if (configPage2 != null && configPage3 != null)
                    sendConfig();
            }
        });

        // Reading the configuration blocks for several seconds: do it on the I/O background
        // thread (it used to run on the UI thread and could freeze the application), then
        // build the tabs once the configuration is known.
        btnUpload.setEnabled(false);
        btnCalibrate.setEnabled(false);
        final AlertDialog loading = new AlertDialog.Builder(this)
                .setMessage(R.string.msg6)
                .setCancelable(false)
                .create();
        loading.show();
        myBT.getIoExecutor().execute(new Runnable() {
            @Override
            public void run() {
                readConfig();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (isFinishing() || isDestroyed())
                            return;
                        if (loading.isShowing())
                            loading.dismiss();
                        if (TestStandCfg == null) {
                            // Reading failed: fall back on the configuration read at connection
                            TestStandCfg = myBT.getTestStandConfigData();
                        }
                        setupViewPager(mViewPager);
                        btnUpload.setEnabled(true);
                        btnCalibrate.setEnabled(true);
                    }
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        myBT.clearHandler(handler);
    }

    private void setupViewPager(ViewPager viewPager) {
        adapter = new SectionsPageAdapter(getSupportFragmentManager());

        configPage2 = new TestStandConfigTab1Fragment(myBT,TestStandCfg);
        configPage3 = new TestStandConfigTab2Fragment(TestStandCfg);

        adapter.addFragment(configPage2, "TAB2");
        adapter.addFragment(configPage3, "TAB3");

        linearDots=findViewById(R.id.idTestStandConfigLinearDots);
        agregaIndicateDots(0, adapter.getCount());
        viewPager.setAdapter(adapter);

        viewPager.addOnPageChangeListener(viewListener);
    }

    public void agregaIndicateDots(int pos, int nbr){
        dotsSlide =new TextView[nbr];
        linearDots.removeAllViews();

        for (int i=0; i< dotsSlide.length; i++){
            dotsSlide[i]=new TextView(this);
            dotsSlide[i].setText(Html.fromHtml("&#8226;"));
            dotsSlide[i].setTextSize(35);
            dotsSlide[i].setTextColor(getResources().getColor(R.color.colorWhiteTransparent));
            linearDots.addView(dotsSlide[i]);
        }

        if(dotsSlide.length>0){
            dotsSlide[pos].setTextColor(getResources().getColor(R.color.colorWhite));
        }

    }

    ViewPager.OnPageChangeListener viewListener=new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int i, float v, int i1) {
        }

        @Override
        public void onPageSelected(int i) {
            agregaIndicateDots(i, adapter.getCount());
            if(i ==0) {
                btnCalibrate.setVisibility(View.INVISIBLE);
            } else {
                btnCalibrate.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onPageScrollStateChanged(int i) {
        }
    };

    public boolean readConfig() {
        // ask for config
        boolean success = false;
        if (myBT.getConnected()) {

            myBT.setDataReady(false);
            myBT.flush();
            myBT.clearInput();
            //switch off the main loop before sending the config
            myBT.write("m0;".toString());

            //wait for the result to come back
            String myMessage = "";
            //wait (bounded) for the result to come back
            if (myBT.waitForData(5000))
                myMessage = myBT.ReadResult(3000);
            if (myMessage.equals("OK")) {
                myBT.setDataReady(false);
                myBT.flush();
                myBT.clearInput();
                myBT.write("b;".toString());
                myBT.flush();

                //get the results
                //wait for the result to come back
                myMessage = "";
                if (myBT.waitForData(5000))
                    myMessage = myBT.ReadResult(3000);
                //reading the config
                if (myMessage.equals("start teststandconfig end")) {
                    Log.d("TestStandConfig", "attempt 1");
                    try {
                        TestStandCfg = myBT.getTestStandConfigData();
                        success = true;
                    } catch (Exception e) {
                        //  msg("pb ready data");
                        success = false;
                    }
                } else {
                    //try again
                    myBT.setDataReady(false);
                    myBT.flush();
                    myBT.clearInput();
                    myBT.write("b;".toString());
                    myBT.flush();
                    //get the results
                    //wait for the result to come back
                    myMessage = "";
                    if (myBT.waitForData(5000))
                        myMessage = myBT.ReadResult(3000);
                    //reading the config
                    if (myMessage.equals("start teststandconfig end")) {
                        Log.d("TestStandConfig", "attempt 2");
                        try {
                            TestStandCfg = myBT.getTestStandConfigData();
                            if (TestStandCfg == null)
                                Log.d("TestStandConfig", "TestStandCfg is null");
                            success = true;
                        } catch (Exception e) {
                            //  msg("pb ready data");
                            success = false;
                        }
                    }
                }
                myBT.setDataReady(false);
                myBT.flush();
                myBT.clearInput();
                //switch on the main loop before sending the config
                myBT.write("m1;".toString());


                //wait (bounded) for the result to come back
                if (myBT.waitForData(5000))
                    myMessage = myBT.ReadResult(3000);
                myBT.flush();
            }
        }

        return success;
    }

    private void msg(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
    }

    private boolean sendConfig() {
        //final boolean exit_no_save = false;
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);

        long prevBaudRate = TestStandCfg.getConnectionSpeed();

        // check if the baud rate has changed
        if (configPage2.isViewCreated()) {
            // TestStandCfg.setConnectionSpeed(configPage2.getBaudRate());
            TestStandCfg.setTestStandResolution(configPage2.getTestStandResolution());
            TestStandCfg.setEepromSize(configPage2.getEEpromSize());
            TestStandCfg.setStopRecordingTime(configPage2.getStopRecordingTime());
            TestStandCfg.setBatteryType(configPage2.getBatteryType());
            TestStandCfg.setPressureSensorType(configPage2.getSensorType());
            TestStandCfg.setTelemetryType(configPage2.getTelemetryType());
            TestStandCfg.setPressureSensorType2(configPage2.getSensorType2());

        }

        if (configPage3.isViewCreated()) {
            TestStandCfg.setUnits(configPage3.getDropdownUnits());
            TestStandCfg.setCalibrationFactor(configPage3.getCalibrationFactor());
            TestStandCfg.setCurrentOffset(configPage3.getCurrentOffset());
            TestStandCfg.setBluetoothName(configPage3.getBluetoothName());
        }

        if (configPage2.isViewCreated()) {
            if (TestStandCfg.getConnectionSpeed() != configPage2.getBaudRate()) {
                //final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                //You are about to change the baud rate, are you sure you want to do it?
                builder.setMessage(getResources().getString(R.string.msg9))
                        .setCancelable(false)
                        .setPositiveButton(getResources().getString(R.string.Yes), new DialogInterface.OnClickListener() {
                            public void onClick(final DialogInterface dialog, final int id) {
                                dialog.cancel();
                                TestStandCfg.setConnectionSpeed(configPage2.getBaudRate());
                                saveConfigAndFinish();
                            }
                        })
                        .setNegativeButton(getResources().getString(R.string.No), new DialogInterface.OnClickListener() {
                            public void onClick(final DialogInterface dialog, final int id) {
                                dialog.cancel();
                            }
                        });
                final AlertDialog alert = builder.create();
                alert.show();
            } else {
                saveConfigAndFinish();
            }
        } else {
            saveConfigAndFinish();
        }
        return true;
    }

    /**
     * Sends the configuration on the I/O background thread (about 15 commands, each waiting
     * for the board answer: this used to freeze the UI thread), then closes the screen.
     */
    private void saveConfigAndFinish() {
        btnUpload.setEnabled(false);
        final AlertDialog saving = new AlertDialog.Builder(this)
                .setMessage(R.string.msg6)
                .setCancelable(false)
                .create();
        saving.show();
        myBT.getIoExecutor().execute(new Runnable() {
            @Override
            public void run() {
                sendTestStandCfgV2();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (isFinishing() || isDestroyed())
                            return;
                        if (saving.isShowing())
                            saving.dismiss();
                        finish();
                    }
                });
            }
        });
    }

    /**
     * Sends every configuration parameter to the board. Must run on a background thread.
     */
    private void sendTestStandCfgV2() {
        if (myBT.getConnected()) {
            myBT.setDataReady(false);
            myBT.flush();
            myBT.clearInput();
            //switch off the main loop before sending the config
            myBT.write("m0;".toString());
            Log.d("conftab", "switch off main loop");
            //wait (bounded) for the result to come back
            String myMessage = "";
            if (myBT.waitForData(5000))
                myMessage = myBT.ReadResult(3000);
            if (myMessage.equals("OK")) {
                Log.d("conftab", "switch off main loop ok");
            }
        }

        SendParam("p,1,"+ TestStandCfg.getUnits());

        SendParam("p,2,"+ TestStandCfg.getConnectionSpeed());

        SendParam("p,3,"+ TestStandCfg.getStopRecordingTime());

        SendParam("p,4,"+ TestStandCfg.getTestStandResolution());

        SendParam("p,5,"+ TestStandCfg.getEepromSize());

        SendParam("p,6,0");

        SendParam("p,7,"+ TestStandCfg.getBatteryType());

        SendParam("p,8,"+ TestStandCfg.getCalibrationFactor());

        SendParam("p,9,"+ TestStandCfg.getCurrentOffset());

        SendParam("p,10,"+ TestStandCfg.getPressureSensorType());
        SendParam("p,11,"+ TestStandCfg.getTelemetryType());
        // The TestStandESP32V4 was missing here, so its second pressure sensor type and its
        // Bluetooth name were never saved
        if (BoardCapabilities.hasPressure2(myBT.getBoardName())) {
            SendParam("p,12," + TestStandCfg.getPressureSensorType2());
        }
        if (BoardCapabilities.hasBluetoothName(myBT.getBoardName())) {
            SendParam("z,"+TestStandCfg.getBluetoothName());
        }

        if (myBT.getConnected()) {
            String myMessage = "";

            myBT.setDataReady(false);
            myBT.flush();
            myBT.clearInput();
            //Write the config structure
            myBT.write("q;".toString());
            Log.d("conftab", "write config");

            //wait (bounded) for the result to come back
            myMessage = "";
            if (myBT.waitForData(5000))
                myMessage = myBT.ReadResult(3000);
            //msg(getResources().getString(R.string.msg3));

            myBT.setDataReady(false);
            myBT.flush();
            myBT.clearInput();
            //switch on the main loop before sending the config
            myBT.write("m1;".toString());
            Log.d("conftab", "switch on main loop");

            //wait (bounded) for the result to come back
            myMessage = "";
            if (myBT.waitForData(5000))
                myMessage = myBT.ReadResult(3000);
            //msg(getResources().getString(R.string.msg3));

            myBT.flush();
        }
    }

    private void SendParam (String altiCfgStr) {
        String cfg = altiCfgStr;
        cfg = cfg.replace("p", "");
        cfg = cfg.replace(",", "");
        Log.d("conftab", cfg.toString());

        altiCfgStr = altiCfgStr + "," + generateCheckSum(cfg) + ";";

        if (myBT.getConnected()) {

            String myMessage = "";

            myBT.flush();
            myBT.clearInput();
            myBT.setDataReady(false);

            //send back the config
            myBT.write(altiCfgStr.toString());
            Log.d("conftab", altiCfgStr.toString());
            myBT.flush();
            //get the results
            //wait (bounded) for the result to come back
            myMessage = "";
            if (myBT.waitForData(5000))
                myMessage = myBT.ReadResult(3000);
            if (myMessage.equals("OK")) {
                Log.d("conftab", "config sent succesfully");

            } else {
                //  msg(myMessage);
                Log.d("conftab", "config not sent succesfully");
                Log.d("conftab", myMessage);
            }
            if (myMessage.equals("KO")) {
                //   msg(getResources().getString(R.string.msg2));
            }
        }
    }
    public static Integer generateCheckSum(String value) {

        byte[] data = value.getBytes();
        long checksum = 0L;

        for (byte b : data) {
            checksum += b;
        }

        checksum = checksum % 256;

        return (int) checksum;

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

    // calibration
    private class Calibration extends AsyncTask<Void, Void, Void>  // UI thread
    {
        private AlertDialog.Builder builder = null;
       // private AlertDialog alert;
        private boolean canceled = false;

        @Override
        protected void onPreExecute() {
            //"Calibration in progress..."
            //"Please wait!!!"
            builder = new AlertDialog.Builder(TestStandTabConfigActivity.this);

            builder.setMessage(R.string.calibration_msg)
                    .setTitle(R.string.calibration_title)
                    .setCancelable(false)
                    .setNegativeButton(R.string.cancel_calibration, new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface dialog, final int id) {
                            myBT.setExit(true);
                            canceled = true;
                            dialog.cancel();
                        }
                    });
            alert = builder.create();
            alert.show();
        }

        @Override
        protected Void doInBackground(Void... devices) //while the progress dialog is shown, the connection is done in background
        {

            myBT.flush();
            myBT.clearInput();

            myBT.write("c"+configPage3.getCalibrationWeight()+";");
            Log.d("conftab", "c"+configPage3.getCalibrationWeight()+";");
            //wait for ok and put the result back
            String myMessage = "";


            myMessage = myBT.ReadResult(30000);
            if (myMessage.equals("OK")) {
                //getParentFragment().readConfig();
                //readConfig();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) //after the doInBackground, it checks if everything went fine
        {
            super.onPostExecute(result);
            if (!canceled) {
                alert.dismiss();
            }
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
            Intent i = new Intent(TestStandTabConfigActivity.this, HelpActivity.class);
            i.putExtra("help_file", "help_config_teststand");
            startActivity(i);
            return true;
        }

        if (id == R.id.action_about) {
            Intent i = new Intent(TestStandTabConfigActivity.this, AboutActivity.class);
            startActivity(i);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
