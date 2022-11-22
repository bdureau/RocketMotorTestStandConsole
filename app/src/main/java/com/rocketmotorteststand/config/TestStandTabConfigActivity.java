package com.rocketmotorteststand.config;
/**
 * @description: Retrieve test stand configuration and show it in tabs
 * The user can then load it back to the test stand
 * @author: boris.dureau@neuf.fr
 **/
import android.os.Handler;
import android.os.Message;

import android.app.AlertDialog;
import android.content.DialogInterface;

import androidx.annotation.Nullable;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;

import android.os.AsyncTask;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.rocketmotorteststand.ConsoleApplication;
import com.rocketmotorteststand.Help.AboutActivity;
import com.rocketmotorteststand.Help.HelpActivity;
import com.rocketmotorteststand.R;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

//tooltip library
import com.github.florent37.viewtooltip.ViewTooltip;



public class TestStandTabConfigActivity extends AppCompatActivity {
    private static final String TAG = "TestStandTabConfigActivity";

    private ViewPager mViewPager;
    SectionsPageAdapter adapter;
    Tab2Fragment configPage2 = null;
    Tab3Fragment configPage3 = null;
    private Button btnDismiss, btnUpload, btnCalibrate;
    static ConsoleApplication myBT;
    private TestStandConfigData TestStandCfg = null;
    private ProgressDialog progress;
    private AlertDialog alert;
    private boolean calibrationComplete = false;

    private TextView[] dotsSlide;
    private LinearLayout linearDots;

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    // Value 1 contains the offset
                    Log.d("Calibration", "offset: "+(String) msg.obj );
                    if (((String) msg.obj).matches("^-?[0-9]\\d+(?:\\.\\d+)?")) {
                        //progress.
                        configPage3.CurrentOffset.setText((String) msg.obj);

                    }
                    break;
                case 2:
                    // Value 2 contains the calibration
                    Log.d("Calibration", "factor: "+(String) msg.obj );
                    if (((String) msg.obj).matches("^-?[0-9]\\d+(?:\\.\\d+)?")) {
                        configPage3.CalibrationFactor.setText((String) msg.obj);
                        alert.setMessage((String) msg.obj);
                    }
                    break;
                case 3:
                    // Value 3 contains the flag
                    if (((String) msg.obj).equals("Done"))
                        calibrationComplete=true;

                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //get the bluetooth connection pointer
        myBT = (ConsoleApplication) getApplication();

        readConfig();

        setContentView(R.layout.activity_teststand_tab_config);

        mViewPager = (ViewPager) findViewById(R.id.container);
        setupViewPager(mViewPager);
        myBT.setHandler(handler);
        btnCalibrate = (Button) findViewById(R.id.butCalibrate);
        btnCalibrate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                calibrationComplete =false;
                new Calibration().execute();
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
                sendConfig();
            }
        });
    }

    private void setupViewPager(ViewPager viewPager) {
        adapter = new SectionsPageAdapter(getSupportFragmentManager());

        configPage2 = new Tab2Fragment(TestStandCfg);
        configPage3 = new Tab3Fragment(TestStandCfg);

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
            try {
                while (myBT.getInputStream().available() <= 0) ;
            } catch (IOException e) {
                success = false;
            }
            String myMessage = "";
            myMessage = myBT.ReadResult(3000);
            if (myMessage.equals("OK")) {
                myBT.setDataReady(false);
                myBT.flush();
                myBT.clearInput();
                myBT.write("b;".toString());
                myBT.flush();

                //get the results
                //wait for the result to come back
                try {
                    while (myBT.getInputStream().available() <= 0) ;
                } catch (IOException e) {
                    success = false;
                }
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
                    try {
                        while (myBT.getInputStream().available() <= 0) ;
                    } catch (IOException e) {
                        success = false;
                    }
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


                //wait for the result to come back
                try {
                    while (myBT.getInputStream().available() <= 0) ;
                } catch (IOException e) {
                    success = false;
                }
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

        }

        if (configPage3.isViewCreated()) {
            TestStandCfg.setUnits(configPage3.getDropdownUnits());
            TestStandCfg.setCalibrationFactor(configPage3.getCalibrationFactor());
            TestStandCfg.setCurrentOffset(configPage3.getCurrentOffset());
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
                                //sendTestStandCfg();
                                sendTestStandCfgV2();
                                finish();
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
                //sendTestStandCfg();
                sendTestStandCfgV2();
                finish();
            }
        } else {
            //sendTestStandCfg();
            sendTestStandCfgV2();
            finish();
        }

        return true;
    }


    /*private void sendTestStandCfg() {
        String testStandCfgStr = "";

        testStandCfgStr = "s," +
                TestStandCfg.getUnits() + "," +
                TestStandCfg.getConnectionSpeed() + "," +
                TestStandCfg.getStopRecordingTime() + "," +
                TestStandCfg.getTestStandResolution() + "," +
                TestStandCfg.getEepromSize();

        testStandCfgStr = testStandCfgStr + "," + "0";//TestStandCfg.getStartRecordingThrustLevel();
        testStandCfgStr = testStandCfgStr + "," + TestStandCfg.getBatteryType();
        testStandCfgStr = testStandCfgStr + "," + TestStandCfg.getCalibrationFactor();
        testStandCfgStr = testStandCfgStr + "," + TestStandCfg.getCurrentOffset();
        testStandCfgStr = testStandCfgStr + "," + TestStandCfg.getPressureSensorType();

        String cfg = testStandCfgStr;
        cfg = cfg.replace("s", "");
        cfg = cfg.replace(",", "");
        Log.d("conftab", cfg.toString());

        testStandCfgStr = testStandCfgStr + "," + generateCheckSum(cfg) + ";";


        if (myBT.getConnected()) {
            myBT.setDataReady(false);
            myBT.flush();
            myBT.clearInput();
            //switch off the main loop before sending the config
            myBT.write("m0;".toString());
            Log.d("conftab", "switch off main loop");
            //wait for the result to come back
            try {
                while (myBT.getInputStream().available() <= 0) ;
            } catch (IOException e) {

            }
            String myMessage = "";
            myMessage = myBT.ReadResult(3000);
            if (myMessage.equals("OK")) {
                Log.d("conftab", "switch off main loop ok");
            }
            myBT.flush();
            myBT.clearInput();
            myBT.setDataReady(false);
            msg("Sent :" + testStandCfgStr.toString());
            //send back the config
            myBT.write(testStandCfgStr.toString());
            Log.d("conftab", testStandCfgStr.toString());
            myBT.flush();
            //get the results
            //wait for the result to come back
            try {
                while (myBT.getInputStream().available() <= 0) ;
            } catch (IOException e) {

            }
            myMessage = myBT.ReadResult(3000);
            if (myMessage.equals("OK")) {
                //msg("Sent OK:" + altiCfgStr.toString());
                Log.d("conftab", "config sent succesfully");

            } else {
                //  msg(myMessage);
            }
            if (myMessage.equals("KO")) {
                //msg(getResources().getString(R.string.msg2));
            }


            myBT.setDataReady(false);
            myBT.flush();
            myBT.clearInput();
            //switch on the main loop before sending the config
            myBT.write("m1;".toString());
            Log.d("conftab", "switch on main loop");

            //wait for the result to come back
            try {
                while (myBT.getInputStream().available() <= 0) ;
            } catch (IOException e) {

            }
            myMessage = "";
            myMessage = myBT.ReadResult(3000);
            msg(getResources().getString(R.string.msg3));

            myBT.flush();
        }

    }*/

    private void sendTestStandCfgV2() {

        if (myBT.getConnected()) {
            myBT.setDataReady(false);
            myBT.flush();
            myBT.clearInput();
            //switch off the main loop before sending the config
            myBT.write("m0;".toString());
            Log.d("conftab", "switch off main loop");
            //wait for the result to come back
            try {
                while (myBT.getInputStream().available() <= 0) ;
            } catch (IOException e) {

            }
            String myMessage = "";
            myMessage = myBT.ReadResult(3000);
            if (myMessage.equals("OK")) {
                Log.d("conftab", "switch off main loop ok");
            }
        }
       // String testStandCfgStr = "";

       // testStandCfgStr = "s," +
                //TestStandCfg.getUnits() + "," +
        SendParam("p,1,"+ TestStandCfg.getUnits());
                //TestStandCfg.getConnectionSpeed() + "," +
        SendParam("p,2,"+ TestStandCfg.getConnectionSpeed());
                //TestStandCfg.getStopRecordingTime() + "," +
        SendParam("p,3,"+ TestStandCfg.getStopRecordingTime());
                //TestStandCfg.getTestStandResolution() + "," +
        SendParam("p,4,"+ TestStandCfg.getTestStandResolution());
                //TestStandCfg.getEepromSize();
        SendParam("p,5,"+ TestStandCfg.getEepromSize());

        //testStandCfgStr = testStandCfgStr + "," + "0";//TestStandCfg.getStartRecordingThrustLevel();
        SendParam("p,6,0");
        //testStandCfgStr = testStandCfgStr + "," + TestStandCfg.getBatteryType();
        SendParam("p,7,"+ TestStandCfg.getBatteryType());
        //testStandCfgStr = testStandCfgStr + "," + TestStandCfg.getCalibrationFactor();
        SendParam("p,8,"+ TestStandCfg.getCalibrationFactor());
        //testStandCfgStr = testStandCfgStr + "," + TestStandCfg.getCurrentOffset();
        SendParam("p,9,"+ TestStandCfg.getCurrentOffset());
        //testStandCfgStr = testStandCfgStr + "," + TestStandCfg.getPressureSensorType();
        SendParam("p,10,"+ TestStandCfg.getPressureSensorType());


        if (myBT.getConnected()) {

            String myMessage = "";

            myBT.setDataReady(false);
            myBT.flush();
            myBT.clearInput();
            //Write the config structure
            myBT.write("q;".toString());
            Log.d("conftab", "write config");

            //wait for the result to come back
            try {
                while (myBT.getInputStream().available() <= 0) ;
            } catch (IOException e) {

            }
            myMessage = "";
            myMessage = myBT.ReadResult(3000);
            //msg(getResources().getString(R.string.msg3));

            myBT.setDataReady(false);
            myBT.flush();
            myBT.clearInput();
            //switch on the main loop before sending the config
            myBT.write("m1;".toString());
            Log.d("conftab", "switch on main loop");

            //wait for the result to come back
            try {
                while (myBT.getInputStream().available() <= 0) ;
            } catch (IOException e) {

            }
            myMessage = "";
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
            //msg("Sent :" + altiCfgStr.toString());
            //send back the config
            myBT.write(altiCfgStr.toString());
            Log.d("conftab", altiCfgStr.toString());
            myBT.flush();
            //get the results
            //wait for the result to come back
            try {
                while (myBT.getInputStream().available() <= 0) ;
            } catch (IOException e) {

            }
            myMessage = "";
            myMessage = myBT.ReadResult(3000);
            if (myMessage.equals("OK")) {
                //msg("Sent OK:" + altiCfgStr.toString());
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

        return new Long(checksum).intValue();

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


    public static class Tab2Fragment extends Fragment {
        private static final String TAG = "Tab2Fragment";
        private TestStandConfigData lTestStandCfg = null;
        private String[] itemsBaudRate;
        private String[] itemsTestStandResolution;
        private String[] itemsEEpromSize;
        private String[] itemsBatteryType;
        private String[] itemsSensorType;

        private Spinner dropdownBaudRate;

        private Spinner dropdownTestStandResolution, dropdownEEpromSize;

        private Spinner dropdownBatteryType;
        private Spinner dropdownSensorType;
        private EditText StopRecordingTime;

        private boolean ViewCreated = false;
        private TextView txtViewEEpromSize, txtViewSensorType;

        public Tab2Fragment(TestStandConfigData cfg) {
            lTestStandCfg = cfg;
        }

        public boolean isViewCreated() {
            return ViewCreated;
        }


        public int getTestStandResolution() {
            return (int) this.dropdownTestStandResolution.getSelectedItemId();
        }

        public void setTestStandResolution(int TestStandResolution) {
            this.dropdownTestStandResolution.setSelection(TestStandResolution);
        }

        public int getEEpromSize() {
            int ret;
            try {
                ret = Integer.parseInt(itemsEEpromSize[(int) dropdownEEpromSize.getSelectedItemId()]);
            } catch (Exception e) {
                ret = 0;
            }
            return ret;
        }

        public void setEEpromSize(int EEpromSize) {
            this.dropdownEEpromSize.setSelection(lTestStandCfg.arrayIndex(itemsEEpromSize, String.valueOf(EEpromSize)));
        }

        public long getBaudRate() {
            int ret;
            try {
                ret = Integer.parseInt(itemsBaudRate[(int) this.dropdownBaudRate.getSelectedItemId()]);
            } catch (Exception e) {
                ret = 0;
            }
            return ret;
        }

        public void setBaudRate(long BaudRate) {
            this.dropdownBaudRate.setSelection(lTestStandCfg.arrayIndex(itemsBaudRate, String.valueOf(BaudRate)));
        }


        public int getStopRecordingTime() {
            int ret;
            try {
                ret = Integer.parseInt(this.StopRecordingTime.getText().toString());
            } catch (Exception e) {
                ret = 0;
            }
            return ret;
        }

        public void setStopRecordingTime(int StopRecordingTime) {
            this.StopRecordingTime.setText(String.valueOf(StopRecordingTime));
        }


        public int getBatteryType() {
            return (int) this.dropdownBatteryType.getSelectedItemId();
        }

        public void setBatteryType(int BatteryType) {
            dropdownBatteryType.setSelection(BatteryType);
        }

        public int getSensorType() {
            return (int) this.dropdownSensorType.getSelectedItemId();
        }

        public void setSensorType(int SensorType) {
            dropdownSensorType.setSelection(SensorType);
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.tabconfigpart2_fragment, container, false);

            //baud rate
            dropdownBaudRate = (Spinner) view.findViewById(R.id.spinnerBaudRate);
            itemsBaudRate = new String[]{"300",
                    "1200",
                    "2400",
                    "4800",
                    "9600",
                    "14400",
                    "19200",
                    "28800",
                    "38400",
                    "57600",
                    "115200",
                    "230400"};
            ArrayAdapter<String> adapterBaudRate = new ArrayAdapter<String>(this.getActivity(),
                    android.R.layout.simple_spinner_dropdown_item, itemsBaudRate);
            dropdownBaudRate.setAdapter(adapterBaudRate);
            // Tool tip
            view.findViewById(R.id.txtViewBaudRate).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ViewTooltip
                            .on(v)
                            .color(Color.BLACK)
                            .position(ViewTooltip.Position.TOP)
                            //Choose the test stand baud rate. Be carefull you might not be able to communicate
                            .text(getResources().getString(R.string.txtViewBaudRate_tooltip))
                            .show();
                }
            });

            // test stand resolution
            dropdownTestStandResolution = (Spinner) view.findViewById(R.id.spinnerTestStandResolution);
            itemsTestStandResolution = new String[]{"ULTRALOWPOWER", "STANDARD", "HIGHRES", "ULTRAHIGHRES"};
            ArrayAdapter<String> adapterTestStandResolution = new ArrayAdapter<String>(this.getActivity(),
                    android.R.layout.simple_spinner_dropdown_item, itemsTestStandResolution);
            dropdownTestStandResolution.setAdapter(adapterTestStandResolution);

            // Tool tip
            view.findViewById(R.id.txtViewTestStandResolution).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ViewTooltip
                            .on(v)
                            .color(Color.BLACK)
                            .position(ViewTooltip.Position.TOP)
                            //Choose the altimeter resolution. The faster you rocket goes the lower it has to be
                            .text(getResources().getString(R.string.txtViewTestStandResolution_tooltip))
                            .show();
                }
            });
            //Test Stand external eeprom size
            txtViewEEpromSize = (TextView) view.findViewById(R.id.txtViewEEpromSize);
            dropdownEEpromSize = (Spinner) view.findViewById(R.id.spinnerEEpromSize);
            itemsEEpromSize = new String[]{"32", "64", "128", "256", "512", "1024"};
            ArrayAdapter<String> adapterEEpromSize = new ArrayAdapter<String>(this.getActivity(),
                    android.R.layout.simple_spinner_dropdown_item, itemsEEpromSize);
            dropdownEEpromSize.setAdapter(adapterEEpromSize);


            // Tool tip
            view.findViewById(R.id.txtViewEEpromSize).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ViewTooltip
                            .on(v)
                            .color(Color.BLACK)
                            .position(ViewTooltip.Position.TOP)
                            //Choose the test stand eeprom size depending on which eeprom is used
                            .text(getResources().getString(R.string.txtViewEEpromSize_tooltip))
                            .show();
                }
            });


            // nbr of newtons to stop recording thrust
            StopRecordingTime = (EditText) view.findViewById(R.id.editTxtStopRecordingTime);
            // Tool tip
            StopRecordingTime.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ViewTooltip
                            .on(v)
                            .color(Color.BLACK)
                            .position(ViewTooltip.Position.TOP)
                            //recording timeout
                            .text(getResources().getString(R.string.EndRecordTime_tooltip))
                            .show();
                }
            });

            dropdownBatteryType = (Spinner) view.findViewById(R.id.spinnerBatteryType);
            //"Unknown",
            itemsBatteryType = new String[]{getResources().getString(R.string.config_unknown),
                    "2S (7.4 Volts)", "9 Volts", "3S (11.1 Volts)"};
            ArrayAdapter<String> adapterBatteryType = new ArrayAdapter<String>(this.getActivity(),
                    android.R.layout.simple_spinner_dropdown_item, itemsBatteryType);
            dropdownBatteryType.setAdapter(adapterBatteryType);

            // Tool tip
            view.findViewById(R.id.txtViewBatteryType).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ViewTooltip
                            .on(v)
                            .color(Color.BLACK)
                            .position(ViewTooltip.Position.TOP)
                            //Enter battery type used to make sure that we do not discharge it too much
                            .text(getResources().getString(R.string.txtViewBatteryType_tooltip))
                            .show();
                }
            });

            txtViewSensorType = (TextView)  view.findViewById(R.id.txtViewSensorType);
            dropdownSensorType = (Spinner) view.findViewById(R.id.spinnerSensorType);
            //"Unknown",
            itemsSensorType = new String[]{getResources().getString(R.string.config_unknown),
                    "100 PSI", "150 PSI", "200 PSI", "300 PSI", "500 PSI", "1000 PSI", "1600 PSI"};
            ArrayAdapter<String> adapterSensorType = new ArrayAdapter<String>(this.getActivity(),
                    android.R.layout.simple_spinner_dropdown_item, itemsSensorType);
            dropdownSensorType.setAdapter(adapterSensorType);

            // Tool tip
            view.findViewById(R.id.txtViewSensorType).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ViewTooltip
                            .on(v)
                            .color(Color.BLACK)
                            .position(ViewTooltip.Position.TOP)
                            //Enter battery type used to make sure that we do not discharge it too much
                            .text(getResources().getString(R.string.txtViewSensorType_tooltip))
                            .show();
                }
            });

            if (myBT.getTestStandConfigData().getTestStandName().equals("TestStandSTM32V2")) {
                dropdownSensorType.setVisibility(View.VISIBLE);
                txtViewSensorType.setVisibility(View.VISIBLE);
            } else {
                dropdownSensorType.setVisibility(View.INVISIBLE);
                txtViewSensorType.setVisibility(View.INVISIBLE);
            }
            if (lTestStandCfg != null) {
                dropdownBaudRate.setSelection(lTestStandCfg.arrayIndex(itemsBaudRate, String.valueOf(lTestStandCfg.getConnectionSpeed())));

                dropdownTestStandResolution.setSelection(lTestStandCfg.getTestStandResolution());
                dropdownEEpromSize.setSelection(lTestStandCfg.arrayIndex(itemsEEpromSize, String.valueOf(lTestStandCfg.getEepromSize())));

                StopRecordingTime.setText(String.valueOf(lTestStandCfg.getStopRecordingTime()));

                dropdownBatteryType.setSelection(lTestStandCfg.getBatteryType());
                dropdownSensorType.setSelection(lTestStandCfg.getPressureSensorType());
            }
            ViewCreated = true;
            return view;
        }
    }

    public static class Tab3Fragment extends Fragment {
        private static final String TAG = "Tab3Fragment";

        private TextView testStandName,  CalibrationFactor, CurrentOffset;
        private Spinner dropdownUnits;
        private EditText calibrationWeight;


        private TestStandConfigData ltestStandNameCfg = null;

        public Tab3Fragment(TestStandConfigData cfg) {
            ltestStandNameCfg = cfg;
        }

        private boolean ViewCreated = false;

        public boolean isViewCreated() {
            return ViewCreated;
        }


        public void setTestStandName(String altiName) {
            this.testStandName.setText(altiName);
        }

        public String getTestStandName() {
            return (String) this.testStandName.getText();
        }

        public int getDropdownUnits() {
            return (int) this.dropdownUnits.getSelectedItemId();
        }

        public void setDropdownUnits(int Units) {
            this.dropdownUnits.setSelection(Units);
        }

        public int getCalibrationFactor() {
            int ret;
            try {
                ret = Integer.parseInt(this.CalibrationFactor.getText().toString());
            } catch (Exception e) {
                ret = 0;
            }
            return ret;
        }

        //getCurrentOffset
        public int getCurrentOffset() {
            int ret;
            try {
                ret = Integer.parseInt(this.CurrentOffset.getText().toString());
            } catch (Exception e) {
                ret = 0;
            }
            return ret;
        }
        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.tabconfigpart3_fragment, container, false);
            CalibrationFactor = (TextView) view.findViewById(R.id.txtCalibrationFactorValue);
            CurrentOffset = (TextView) view.findViewById(R.id.txtCalibrationOffsetValue);
            calibrationWeight = (EditText) view.findViewById(R.id.txtCalibrationWeightValue);
            //btnCalibrate = (Button) view.findViewById(R.id.butCalibrate);
            //units
            dropdownUnits = (Spinner) view.findViewById(R.id.spinnerUnit);
            //"kg", "pounds"
            String[] items2 = new String[]{getResources().getString(R.string.unit_kg),
                    getResources().getString(R.string.unit_pound), getResources().getString(R.string.unit_newton)};
            ArrayAdapter<String> adapter2 = new ArrayAdapter<String>(this.getActivity(),
                    android.R.layout.simple_spinner_dropdown_item, items2);
            dropdownUnits.setAdapter(adapter2);

            // Tool tip
            view.findViewById(R.id.txtTestStandUnit).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ViewTooltip
                            .on(v)
                            .color(Color.BLACK)
                            .position(ViewTooltip.Position.TOP)
                            //Choose the altitude units you are familiar with
                            .text(getResources().getString(R.string.txtTestStandUnit_tooltip))
                            .show();
                }
            });
            //Test Stand name
            testStandName = (TextView) view.findViewById(R.id.txtAltiNameValue);



            if (ltestStandNameCfg != null) {
                testStandName.setText(ltestStandNameCfg.getTestStandName() + " ver: " +
                        ltestStandNameCfg.getTestStandMajorVersion() + "." + ltestStandNameCfg.getTestStandMinorVersion());

                dropdownUnits.setSelection(ltestStandNameCfg.getUnits());
                CalibrationFactor.setText(String.valueOf(ltestStandNameCfg.getCalibrationFactor()));
                CurrentOffset.setText(String.valueOf(ltestStandNameCfg.getCurrentOffset()));

            }
            ViewCreated = true;
            return view;
        }

    }

    // calibration
    private class Calibration extends AsyncTask<Void, Void, Void>  // UI thread
    {
        private AlertDialog.Builder builder = null;
       // private AlertDialog alert;
        private Boolean canceled = false;

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
            myBT.write("c"+configPage3.calibrationWeight.getText()+";".toString());
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
