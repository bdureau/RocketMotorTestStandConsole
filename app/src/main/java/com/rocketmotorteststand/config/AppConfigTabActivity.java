package com.rocketmotorteststand.config;
/**
 * @description: In this activity you should be able to choose the application languages and looks and feel.
 * Still a lot to do but it is a good start
 * @author: boris.dureau@neuf.fr
 **/

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.rocketmotorteststand.ConsoleApplication;
import com.rocketmotorteststand.Help.AboutActivity;
import com.rocketmotorteststand.Help.HelpActivity;
import com.rocketmotorteststand.R;
import com.rocketmotorteststand.ShareHandler;
import com.rocketmotorteststand.config.AppConfig.AppConfigTab1Fragment;

import java.util.ArrayList;
import java.util.List;


public class AppConfigTabActivity extends AppCompatActivity {
    Button btnDismiss, btnSave, bdtDefault;
    private ViewPager mViewPager;
    SectionsPageAdapter adapter;

    private AppConfigTab1Fragment appConfigPage1 = null;
    //private Tab2Fragment appConfigPage2 = null;


    private AppConfigData appConfigData = null;

    ConsoleApplication myBT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //get the Connection Application pointer
        myBT = (ConsoleApplication) getApplication();

        myBT.getAppConf().ReadConfig();
        //Check the local and force it if needed
        //getApplicationContext().getResources().updateConfiguration(myBT.getAppLocal(), null);
        // get the data for all the drop down
        appConfigData = new AppConfigData(this);
        setContentView(R.layout.activity_app_config);

        mViewPager = (ViewPager) findViewById(R.id.container_config);
        setupViewPager(mViewPager);

        btnDismiss = (Button) findViewById(R.id.butDismiss);
        btnDismiss.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();      //exit the application configuration activity
            }
        });

        btnSave = (Button) findViewById(R.id.butSave);
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //save the application configuration
                SaveConfig();
            }
        });

        bdtDefault = (Button) findViewById(R.id.butDefault);
        bdtDefault.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //restore the application default configuration
                RestoreToDefault();
            }
        });
    }

    private void SaveConfig() {
        myBT.getAppConf().setApplicationLanguage("" + appConfigPage1.getAppLanguage() + "");
        myBT.getAppConf().setGraphColor("" + appConfigPage1.getGraphColor() + "");
        myBT.getAppConf().setUnits("" + appConfigPage1.getAppUnit() + "");
        myBT.getAppConf().setUnitsPressure("" + appConfigPage1.getAppUnitPressure() + "");

        myBT.getAppConf().setGraphBackColor("" + appConfigPage1.getGraphBackColor() + "");
        myBT.getAppConf().setFontSize("" + appConfigPage1.getFontSize() + "");
        myBT.getAppConf().setBaudRate("" + appConfigPage1.getBaudRate() + "");
        myBT.getAppConf().setConnectionType("" + appConfigPage1.getConnectionType() + "");
        myBT.getAppConf().setGraphicsLibType("" + appConfigPage1.getGraphicsLibType() + "");

        myBT.getAppConf().setFullUSBSupport(appConfigPage1.getFullUSBSupport());

        myBT.getAppConf().SaveConfig();
        finish();
    }

    private void RestoreToDefault() {
        myBT.getAppConf().ResetDefaultConfig();
        appConfigPage1.setAppLanguage(Integer.parseInt(myBT.getAppConf().getApplicationLanguage()));
        appConfigPage1.setAppUnit(Integer.parseInt(myBT.getAppConf().getUnits()));
        appConfigPage1.setAppUnitPressure(Integer.parseInt(myBT.getAppConf().getUnitsPressure()));
        appConfigPage1.setGraphColor(Integer.parseInt(myBT.getAppConf().getGraphColor()));
        appConfigPage1.setGraphBackColor(Integer.parseInt(myBT.getAppConf().getGraphBackColor()));
        appConfigPage1.setFontSize(Integer.parseInt(myBT.getAppConf().getFontSize()) - 8);
        appConfigPage1.setBaudRate(Integer.parseInt(myBT.getAppConf().getBaudRate()));
        appConfigPage1.setConnectionType(Integer.parseInt(myBT.getAppConf().getConnectionType()));
        appConfigPage1.setGraphicsLibType(Integer.parseInt(myBT.getAppConf().getGraphicsLibType()));


        if (myBT.getAppConf().getFullUSBSupport().equals("true")) {
            appConfigPage1.setFullUSBSupport(true);
        } else {
            appConfigPage1.setFullUSBSupport(false);
        }


    }


    private void setupViewPager(ViewPager viewPager) {
        adapter = new AppConfigTabActivity.SectionsPageAdapter(getSupportFragmentManager());
        appConfigPage1 = new AppConfigTab1Fragment(myBT, appConfigData); //AppConfigTabActivity.Tab1Fragment(myBT);


        adapter.addFragment(appConfigPage1, "TAB1");


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
/*
    public static class Tab1Fragment extends Fragment {
        private Spinner spAppLanguage, spGraphColor, spAppUnit, spAppUnitPressure, spGraphBackColor, spFontSize, spBaudRate;
        private Spinner spConnectionType, spGraphicsLibType;
        private CheckBox  cbFullUSBSupport;
        private ConsoleApplication BT;

        public Tab1Fragment(ConsoleApplication lBT) {
            BT = lBT;
        }

        public int getAppLanguage() {
            return (int) this.spAppLanguage.getSelectedItemId();
        }

        public void setAppLanguage(int value) {
            this.spAppLanguage.setSelection(value);
        }

        public int getGraphColor() {
            return (int) this.spGraphColor.getSelectedItemId();
        }

        public void setGraphColor(int value) {
            this.spGraphColor.setSelection(value);
        }

        public int getAppUnit() {
            return (int) this.spAppUnit.getSelectedItemId();
        }
        public int getAppUnitPressure() {
            return (int) this.spAppUnitPressure.getSelectedItemId();
        }

        public void setAppUnit(int value) {
            this.spAppUnit.setSelection(value);
        }
        public void setAppUnitPressure(int value) {
            this.spAppUnitPressure.setSelection(value);
        }

        public int getGraphBackColor() {
            return (int) this.spGraphBackColor.getSelectedItemId();
        }

        public void setGraphBackColor(int value) {
            this.spGraphBackColor.setSelection(value);
        }

        public int getFontSize() {
            return (int) this.spFontSize.getSelectedItemId();
        }

        public void setFontSize(int value) {
            this.spFontSize.setSelection(value);
        }

        public int getBaudRate() {
            return (int) this.spBaudRate.getSelectedItemId();
        }

        public void setBaudRate(int value) {
            this.spBaudRate.setSelection(value);
        }

        public int getConnectionType() {
            return (int) this.spConnectionType.getSelectedItemId();
        }

        public void setConnectionType(int value) {
            this.spConnectionType.setSelection(value);
        }

        public int getGraphicsLibType() {
            return (int) this.spGraphicsLibType.getSelectedItemId();
        }

        public void setGraphicsLibType(int value) {
            this.spGraphicsLibType.setSelection(value);
        }


        public String getFullUSBSupport() {
            if (cbFullUSBSupport.isChecked())
                return "true";
            else
                return "false";
        }

        public void setFullUSBSupport(boolean value) {
            cbFullUSBSupport.setChecked(value);
        }


        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            View view = inflater.inflate(R.layout.activity_app_config_part1, container, false);
            //Language
            spAppLanguage = (Spinner) view.findViewById(R.id.spinnerLanguage);

            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this.getActivity(), android.R.layout.simple_spinner_dropdown_item, appConfigData.getItemsLanguages());
            spAppLanguage.setAdapter(adapter);
            spAppLanguage.setEnabled(false); //disable it for the moment because it is causing troubles
            // graph color
            spGraphColor = (Spinner) view.findViewById(R.id.spinnerGraphColor);
            // String[] itemsColor = new String[]{"Black", "White", "Yellow", "Red", "Green", "Blue"};

            ArrayAdapter<String> adapterColor = new ArrayAdapter<String>(this.getActivity(), android.R.layout.simple_spinner_dropdown_item, appConfigData.getItemsColor());
            spGraphColor.setAdapter(adapterColor);
            // graph back color
            spGraphBackColor = (Spinner) view.findViewById(R.id.spinnerGraphBackColor);
            ArrayAdapter<String> adapterGraphColor = new ArrayAdapter<String>(this.getActivity(), android.R.layout.simple_spinner_dropdown_item, appConfigData.getItemsColor());

            spGraphBackColor.setAdapter(adapterGraphColor);
            //units
            spAppUnit = (Spinner) view.findViewById(R.id.spinnerUnits);

            ArrayAdapter<String> adapter2 = new ArrayAdapter<String>(this.getActivity(), android.R.layout.simple_spinner_dropdown_item, appConfigData.getItemsUnits());
            spAppUnit.setAdapter(adapter2);

            //units Pressure
            spAppUnitPressure = (Spinner) view.findViewById(R.id.spinnerUnitsPressure);

            ArrayAdapter<String> adapter3 = new ArrayAdapter<String>(this.getActivity(), android.R.layout.simple_spinner_dropdown_item, appConfigData.getItemsUnitsPressure());
            spAppUnitPressure.setAdapter(adapter3);

            //font size
            spFontSize = (Spinner) view.findViewById(R.id.spinnerFontSize);

            ArrayAdapter<String> adapterFontSize = new ArrayAdapter<String>(this.getActivity(), android.R.layout.simple_spinner_dropdown_item, appConfigData.getItemsFontSize());
            spFontSize.setAdapter(adapterFontSize);

            //Baud Rate
            spBaudRate = (Spinner) view.findViewById(R.id.spinnerBaudRate);

            ArrayAdapter<String> adapterBaudRate = new ArrayAdapter<String>(this.getActivity(), android.R.layout.simple_spinner_dropdown_item, appConfigData.getItemsBaudRate());
            spBaudRate.setAdapter(adapterBaudRate);

            //connection type
            spConnectionType = (Spinner) view.findViewById(R.id.spinnerConnectionType);

            ArrayAdapter<String> adapterConnectionType = new ArrayAdapter<String>(this.getActivity(), android.R.layout.simple_spinner_dropdown_item, appConfigData.getItemsConnectionType());
            spConnectionType.setAdapter(adapterConnectionType);

            //Graphics lib type
            spGraphicsLibType = (Spinner) view.findViewById(R.id.spinnerGraphicLibType);
            ArrayAdapter<String> adapterGraphicsLibType = new ArrayAdapter<String>(this.getActivity(), android.R.layout.simple_spinner_dropdown_item, appConfigData.getItemsGraphicsLib());
            spGraphicsLibType.setAdapter(adapterGraphicsLibType);


            //Allow only telemetry via USB
            cbFullUSBSupport = (CheckBox) view.findViewById(R.id.checkBoxFullUSBSupport);

            spAppLanguage.setSelection(Integer.parseInt(BT.getAppConf().getApplicationLanguage()));
            spAppUnit.setSelection(Integer.parseInt(BT.getAppConf().getUnits()));
            spAppUnitPressure.setSelection(Integer.parseInt(BT.getAppConf().getUnitsPressure()));
            spGraphColor.setSelection(Integer.parseInt(BT.getAppConf().getGraphColor()));
            spGraphBackColor.setSelection(Integer.parseInt(BT.getAppConf().getGraphBackColor()));
            spFontSize.setSelection((Integer.parseInt(BT.getAppConf().getFontSize()) - 8));
            spBaudRate.setSelection(Integer.parseInt(BT.getAppConf().getBaudRate()));
            spConnectionType.setSelection(Integer.parseInt(BT.getAppConf().getConnectionType()));
            spGraphicsLibType.setSelection(Integer.parseInt(BT.getAppConf().getGraphicsLibType()));

            if (BT.getAppConf().getFullUSBSupport().equals("true")) {
                cbFullUSBSupport.setChecked(true);
            } else {
                cbFullUSBSupport.setChecked(false);
            }

            return view;
        }

    }

    public static class Tab2Fragment extends Fragment {

        private ConsoleApplication BT;


        public Tab2Fragment(ConsoleApplication lBT) {
            BT = lBT;
        }



        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            View view = inflater.inflate(R.layout.activity_app_config_part2, container, false);

            return view;
        }
        private void msg(String s) {
            Toast.makeText(getContext(), s, Toast.LENGTH_LONG).show();
        }
    }
*/
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
            Intent i = new Intent(AppConfigTabActivity.this, HelpActivity.class);
            i.putExtra("help_file", "help_config_application");
            startActivity(i);
            return true;
        }

        if (id == R.id.action_about) {
            Intent i = new Intent(AppConfigTabActivity.this, AboutActivity.class);
            startActivity(i);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
