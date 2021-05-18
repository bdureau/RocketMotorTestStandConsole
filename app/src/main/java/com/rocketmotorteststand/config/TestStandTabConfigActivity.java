package com.rocketmotorteststand.config;
/**
 * @description: Retrieve test stand configuration and show it in tabs
 * The user can then load it back to the test stand
 * @author: boris.dureau@neuf.fr
 **/

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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
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
    private Button btnDismiss, btnUpload;
    ConsoleApplication myBT;
    private TestStandConfigData TestStandCfg = null;
    private ProgressDialog progress;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //get the bluetooth connection pointer
        myBT = (ConsoleApplication) getApplication();

        readConfig();
        //Assync config does not work so do not use it for now
        //new RetrieveConfig().execute();
        //Check the local and force it if needed
        //getApplicationContext().getResources().updateConfiguration(myBT.getAppLocal(), null);
        setContentView(R.layout.activity_teststand_tab_config);

        mViewPager = (ViewPager) findViewById(R.id.container);
        setupViewPager(mViewPager);


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

        viewPager.setAdapter(adapter);
    }


    private boolean readConfig() {
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

            TestStandCfg.setStopRecordingThrustLevel(configPage2.getStopRecordingThrustLevel());
            TestStandCfg.setStartRecordingThrustLevel(configPage2.getStartRecordingThrustLevel());
            TestStandCfg.setBatteryType(configPage2.getBatteryType());
        }

        if (configPage3.isViewCreated()) {
            TestStandCfg.setUnits(configPage3.getDropdownUnits());
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
                                sendTestStandCfg();
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
                sendTestStandCfg();
                finish();
            }
        } else {
            sendTestStandCfg();
            finish();
        }

        return true;
    }

    private void sendTestStandCfg() {
        String testStandCfgStr = "";

        testStandCfgStr = "s," +
                TestStandCfg.getUnits() + "," +
                TestStandCfg.getConnectionSpeed() + "," +
                TestStandCfg.getStopRecordingThrustLevel() + "," +
                TestStandCfg.getTestStandResolution() + "," +
                TestStandCfg.getEepromSize();

        testStandCfgStr = testStandCfgStr + "," + TestStandCfg.getStartRecordingThrustLevel();
        testStandCfgStr = testStandCfgStr + "," + TestStandCfg.getBatteryType();


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

        private Spinner dropdownBaudRate;

        private Spinner dropdownTestStandResolution, dropdownEEpromSize;

        private Spinner dropdownBatteryType;
        private EditText StopRecordingThrustLevel;
        private EditText StartRecordingThrustLevel;
        private boolean ViewCreated = false;
        private TextView txtViewEEpromSize;

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


        public int getStopRecordingThrustLevel() {
            int ret;
            try {
                ret = Integer.parseInt(this.StopRecordingThrustLevel.getText().toString());
            } catch (Exception e) {
                ret = 0;
            }
            return ret;
        }

        public void setStopRecordingThrustLevel(int StopRecordingThrustLevel) {
            this.StopRecordingThrustLevel.setText(String.valueOf(StopRecordingThrustLevel));
        }

        public int getStartRecordingThrustLevel() {
            int ret;
            try {
                ret = Integer.parseInt(this.StartRecordingThrustLevel.getText().toString());
            } catch (Exception e) {
                ret = 0;
            }
            return ret;
        }

        public void setStartRecordingThrustLevel(int StartRecordingThrustLevel) {
            this.StartRecordingThrustLevel.setText(String.valueOf(StartRecordingThrustLevel));
        }

        public int getBatteryType() {
            return (int) this.dropdownBatteryType.getSelectedItemId();
        }

        public void setBatteryType(int BatteryType) {
            dropdownBatteryType.setSelection(BatteryType);
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
                            .text(getResources().getString(R.string.txtViewAltimeterResolution_tooltip))
                            .show();
                }
            });
            //Altimeter external eeprom size
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
            StopRecordingThrustLevel = (EditText) view.findViewById(R.id.editTxtStopRecordingThrustLevel);
            // Tool tip
            StopRecordingThrustLevel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ViewTooltip
                            .on(v)
                            .color(Color.BLACK)
                            .position(ViewTooltip.Position.TOP)
                            //At which altitude do you consider that we have landed
                            .text(getResources().getString(R.string.EndRecordAltitude_tooltip))
                            .show();
                }
            });
            // nbr of meters to consider that we have a lift off
            StartRecordingThrustLevel = (EditText) view.findViewById(R.id.editTxtStartRecordingThrustLevel);

            // Tool tip
            StartRecordingThrustLevel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ViewTooltip
                            .on(v)
                            .color(Color.BLACK)
                            .position(ViewTooltip.Position.TOP)
                            //At which altitude do you consider that we have lift off
                            .text(getResources().getString(R.string.LiftOffAltitude_tooltip))
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
            if (lTestStandCfg != null) {
                dropdownBaudRate.setSelection(lTestStandCfg.arrayIndex(itemsBaudRate, String.valueOf(lTestStandCfg.getConnectionSpeed())));

                dropdownTestStandResolution.setSelection(lTestStandCfg.getTestStandResolution());
                dropdownEEpromSize.setSelection(lTestStandCfg.arrayIndex(itemsEEpromSize, String.valueOf(lTestStandCfg.getEepromSize())));

                StopRecordingThrustLevel.setText(String.valueOf(lTestStandCfg.getStopRecordingThrustLevel()));
                StartRecordingThrustLevel.setText(String.valueOf(lTestStandCfg.getStartRecordingThrustLevel()));
                dropdownBatteryType.setSelection(lTestStandCfg.getBatteryType());
            }
            ViewCreated = true;
            return view;
        }
    }

    public static class Tab3Fragment extends Fragment {
        private static final String TAG = "Tab3Fragment";

        private TextView testStandName;
        private Spinner dropdownUnits;


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


        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.tabconfigpart3_fragment, container, false);

            //units
            dropdownUnits = (Spinner) view.findViewById(R.id.spinnerUnit);
            //"kg", "pounds"
            String[] items2 = new String[]{getResources().getString(R.string.unit_kg),
                    getResources().getString(R.string.unit_pound)};
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
                            .text(getResources().getString(R.string.txtAltiUnit_tooltip))
                            .show();
                }
            });
            //Test Stand name
            testStandName = (TextView) view.findViewById(R.id.txtAltiNameValue);


            if (ltestStandNameCfg != null) {
                testStandName.setText(ltestStandNameCfg.getTestStandName() + " ver: " +
                        ltestStandNameCfg.getTestStandMajorVersion() + "." + ltestStandNameCfg.getTestStandMinorVersion());


                dropdownUnits.setSelection(ltestStandNameCfg.getUnits());

            }
            ViewCreated = true;
            return view;
        }

    }


    private class RetrieveConfig extends AsyncTask<Void, Void, Void>  // UI thread
    {

        @Override
        protected void onPreExecute() {
            //"Retrieving Altimeter config...", "Please wait!!!"
            progress = ProgressDialog.show(TestStandTabConfigActivity.this,
                    getResources().getString(R.string.msg5),
                    getResources().getString(R.string.msg6));  //show a progress dialog
        }

        @Override
        protected Void doInBackground(Void... devices) //while the progress dialog is shown, the connection is done in background
        {
            boolean success = false;
            success = readConfig();
            //second attempt
            if (!success)
                success = readConfig();
            //third attempt
            if (!success)
                success = readConfig();
            //fourth and last
            if (!success)
                success = readConfig();
            return null;
        }

        @Override
        protected void onPostExecute(Void result) //after the doInBackground, it checks if everything went fine
        {
            super.onPostExecute(result);

            if (TestStandCfg != null && configPage2 != null && configPage3 != null) {

                //Config Tab 2
                if (configPage2.isViewCreated()) {
                    configPage2.setBaudRate(TestStandCfg.getConnectionSpeed());
                    configPage2.setTestStandResolution(TestStandCfg.getTestStandResolution());
                    configPage2.setEEpromSize(TestStandCfg.getEepromSize());
                    configPage2.setStopRecordingThrustLevel(TestStandCfg.getStopRecordingThrustLevel());
                }

                if (configPage3.isViewCreated()) {
                    configPage3.setTestStandName(TestStandCfg.getTestStandName() + " ver: " +
                            TestStandCfg.getTestStandMajorVersion() + "." + TestStandCfg.getTestStandMinorVersion());

                    configPage3.setDropdownUnits(TestStandCfg.getUnits());

                }

            }
            progress.dismiss();
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
            i.putExtra("help_file", "help_config_alti");
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
