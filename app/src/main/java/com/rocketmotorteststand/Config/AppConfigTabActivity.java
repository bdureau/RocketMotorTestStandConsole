package com.rocketmotorteststand.Config;
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
import com.rocketmotorteststand.Config.AppConfig.AppConfigTab1Fragment;

import java.util.ArrayList;
import java.util.List;


public class AppConfigTabActivity extends AppCompatActivity {
    Button btnDismiss, btnSave, bdtDefault;
    private ViewPager mViewPager;
    SectionsPageAdapter adapter;

    private AppConfigTab1Fragment appConfigPage1 = null;

    private AppConfigData appConfigData = null;

    ConsoleApplication myBT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //get the Connection Application pointer
        myBT = (ConsoleApplication) getApplication();

        myBT.getAppConf().ReadConfig();

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
        myBT.getAppConf().setApplicationLanguage(appConfigPage1.getAppLanguage());
        myBT.getAppConf().setGraphColor(appConfigPage1.getGraphColor());
        myBT.getAppConf().setUnits(appConfigPage1.getAppUnit());
        myBT.getAppConf().setUnitsPressure(appConfigPage1.getAppUnitPressure());

        myBT.getAppConf().setGraphBackColor( appConfigPage1.getGraphBackColor());
        myBT.getAppConf().setFontSize(appConfigPage1.getFontSize());
        myBT.getAppConf().setBaudRate(appConfigPage1.getBaudRate());
        myBT.getAppConf().setConnectionType(appConfigPage1.getConnectionType());
        myBT.getAppConf().setGraphicsLibType(appConfigPage1.getGraphicsLibType());

        myBT.getAppConf().setFullUSBSupport(appConfigPage1.getFullUSBSupport());
        myBT.getAppConf().setManualRecording(appConfigPage1.getAllowManualRecording());
        myBT.getAppConf().SaveConfig();
        finish();
    }

    private void RestoreToDefault() {
        myBT.getAppConf().ResetDefaultConfig();
        appConfigPage1.setAppLanguage(myBT.getAppConf().getApplicationLanguage());
        appConfigPage1.setAppUnit(myBT.getAppConf().getUnits());
        appConfigPage1.setAppUnitPressure(myBT.getAppConf().getUnitsPressure());
        appConfigPage1.setGraphColor(myBT.getAppConf().getGraphColor());
        appConfigPage1.setGraphBackColor(myBT.getAppConf().getGraphBackColor());
        appConfigPage1.setFontSize(myBT.getAppConf().getFontSize());
        appConfigPage1.setBaudRate(myBT.getAppConf().getBaudRate());
        appConfigPage1.setConnectionType(myBT.getAppConf().getConnectionType());
        appConfigPage1.setGraphicsLibType(myBT.getAppConf().getGraphicsLibType());

        appConfigPage1.setFullUSBSupport(myBT.getAppConf().getFullUSBSupport());
        appConfigPage1.setAllowManualRecording(myBT.getAppConf().getManualRecording());
    }


    private void setupViewPager(ViewPager viewPager) {
        adapter = new AppConfigTabActivity.SectionsPageAdapter(getSupportFragmentManager());
        appConfigPage1 = new AppConfigTab1Fragment(myBT, appConfigData);

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
