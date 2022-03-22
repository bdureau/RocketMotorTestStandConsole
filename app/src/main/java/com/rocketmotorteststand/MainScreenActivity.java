package com.rocketmotorteststand;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.rocketmotorteststand.Flash.FlashFirmware;
import com.rocketmotorteststand.ThrustCurve.ThrustCurveListActivity;
import com.rocketmotorteststand.ThrustCurve.ThrustCurveViewTabActivity;
import com.rocketmotorteststand.config.Config3DR;
import com.rocketmotorteststand.config.ConfigBT;
import com.rocketmotorteststand.config.TestStandConfigData;
import com.rocketmotorteststand.config.TestStandTabConfigActivity;
import com.rocketmotorteststand.config.AppConfigActivity;
import com.rocketmotorteststand.Help.AboutActivity;
import com.rocketmotorteststand.Help.HelpActivity;
import com.rocketmotorteststand.connection.SearchBluetooth;
import com.rocketmotorteststand.connection.TestConnection;
import com.rocketmotorteststand.telemetry.TestStandStatus;
import com.rocketmotorteststand.telemetry.Telemetry;
import com.rocketmotorteststand.telemetry.TelemetryMp;
//import com.google.android.gms.appindexing.AppIndex;
//import com.google.android.gms.common.api.GoogleApiClient;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class MainScreenActivity extends AppCompatActivity {
    String address = null;
    Button btnTestStandSettings, btnReadThrustCurves, btnConnectDisconnect, btnReset;
    Button btnTelemetry, btnStatus, btnFlashFirmware;

    //private ProgressDialog progress;
    UsbManager usbManager;
    UsbDevice device;
    public final String ACTION_USB_PERMISSION = "com.rocketmotorteststand.USB_PERMISSION";

    ConsoleApplication myBT;
    private TestStandConfigData AltiCfg = null;
    private FirmwareCompatibility firmCompat =null;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    //private GoogleApiClient client;

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() { //Broadcast Receiver to automatically start and stop the Serial connection.
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_USB_PERMISSION)) {
                boolean granted = intent.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                if (granted) {

                    if (myBT.connect(usbManager, device, Integer.parseInt(myBT.getAppConf().getBaudRateValue()))) {
                        myBT.setConnected(true);
                        EnableUI();
                        btnFlashFirmware.setEnabled(false);
                        myBT.setConnectionType("usb");
                        btnConnectDisconnect.setText(getResources().getString(R.string.disconnect));
                    }

                } else {
                    msg("PERM NOT GRANTED");
                }
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
                msg("I can connect via usb");
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
                if (myBT.getConnectionType().equals("usb"))
                    if (myBT.getConnected()) {
                        myBT.Disconnect();
                        btnConnectDisconnect.setText(getResources().getString(R.string.connect));
                    }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        usbManager = (UsbManager) getSystemService(this.USB_SERVICE);

        //get the bluetooth and USB Application pointer
        myBT = (ConsoleApplication) getApplication();

        //This will check if the firmware is compatible with the app and advice on flashing the firmware
        firmCompat = new FirmwareCompatibility();

        setContentView(R.layout.activity_main_screen);
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(broadcastReceiver, filter);

        btnTestStandSettings = (Button) findViewById(R.id.butAltiSettings);
        btnReadThrustCurves = (Button) findViewById(R.id.butReadFlight);
        btnTelemetry = (Button) findViewById(R.id.butTelemetry);
        btnStatus = (Button) findViewById(R.id.butStatus);
        btnFlashFirmware = (Button) findViewById(R.id.butFlashFirmware);
        btnConnectDisconnect = (Button) findViewById(R.id.butDisconnect);

        btnReset = (Button) findViewById(R.id.butReset);

        if (myBT.getConnected()) {
            EnableUI();
            btnFlashFirmware.setEnabled(false);
        } else {
            DisableUI();
            btnConnectDisconnect.setText(R.string.connect);
            btnFlashFirmware.setEnabled(true);
        }

        //commands to be sent to bluetooth
        btnTestStandSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent i = new Intent(MainScreenActivity.this, TestStandTabConfigActivity.class);
                //Change the activity.
                startActivity(i);
            }
        });

        //commands to be sent to flash the firmware
        btnFlashFirmware.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                myBT.getAppConf().ReadConfig();
                Intent i = new Intent(MainScreenActivity.this, FlashFirmware.class);
                //Change the activity.
                startActivity(i);
            }
        });

        btnReadThrustCurves.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MainScreenActivity.this, ThrustCurveListActivity.class);
                startActivity(i);
            }
        });

        btnTelemetry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //turn on telemetry
                if (myBT.getConnected()) {
                    myBT.flush();
                    myBT.clearInput();

                    myBT.write("y1;".toString());
                }
                Intent i;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O){
                    //if android ver = 8 or greater use the MPlib
                    i = new Intent(MainScreenActivity.this, TelemetryMp.class);
                } else {
                    if (myBT.getAppConf().getGraphicsLibType().equals("0"))
                        i = new Intent(MainScreenActivity.this, Telemetry.class);
                    else
                        i = new Intent(MainScreenActivity.this, TelemetryMp.class);
                }
                startActivity(i);
            }
        });

        btnStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //turn on telemetry
                if (myBT.getConnected()) {
                    myBT.flush();
                    myBT.clearInput();
                    myBT.write("y1;".toString());
                }
                Intent i = new Intent(MainScreenActivity.this, TestStandStatus.class);
                startActivity(i);
            }
        });

        btnConnectDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myBT.getAppConf().ReadConfig();
                if (myBT.getAppConf().getConnectionType().equals("0"))
                    myBT.setConnectionType("bluetooth");
                else
                    myBT.setConnectionType("usb");

                if (myBT.getConnected()) {
                    Disconnect(); //close connection
                    DisableUI();
                    // we are disconnected enable flash firmware
                    btnFlashFirmware.setEnabled(true);
                    btnConnectDisconnect.setText(R.string.connect);
                } else {
                    if (myBT.getConnectionType().equals("bluetooth")) {
                        address = myBT.getAddress();

                        if (address != null) {
                            new ConnectBT().execute(); //Call the class to connect
                            if (myBT.getConnected()) {
                                EnableUI();
                                // cannot flash firmware if connected
                                btnFlashFirmware.setEnabled(false);
                                btnConnectDisconnect.setText(getResources().getString(R.string.disconnect));
                            }
                        } else {
                            // choose the bluetooth device
                            Intent i = new Intent(MainScreenActivity.this, SearchBluetooth.class);
                            startActivity(i);
                        }
                    } else {
                        //this is a USB connection
                        HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
                        if (!usbDevices.isEmpty()) {
                            boolean keep = true;
                            for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                                device = entry.getValue();
                                int deviceVID = device.getVendorId();

                                PendingIntent pi = PendingIntent.getBroadcast(MainScreenActivity.this, 0, new Intent(ACTION_USB_PERMISSION), 0);
                                usbManager.requestPermission(device, pi);
                                keep = false;

                                if (!keep)
                                    break;
                            }
                        }
                    }
                }
            }
        });

        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MainScreenActivity.this, ResetSettingsActivity.class);
                startActivity(i);
            }
        });


        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        //client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    private void DisableUI() {
        btnTestStandSettings.setEnabled(false);
        btnReadThrustCurves.setEnabled(false);
        btnTelemetry.setEnabled(false);

        btnReset.setEnabled(false);
        btnStatus.setEnabled(false);
        // now enable or disable the menu entries by invalidating it
        invalidateOptionsMenu();
    }

    private void EnableUI() {

        boolean success;
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

        /*
        supported firmware
        TestStand
        TestStandSTM32

         */
        if (myBT.getTestStandConfigData().getTestStandName().equals("TestStand") ||
                myBT.getTestStandConfigData().getTestStandName().equals("TestStandSTM32") ||
                myBT.getTestStandConfigData().getTestStandName().equals("TestStandSTM32V2")
        ) {
            Log.d("MainScreen", "test stand name: " + myBT.getTestStandConfigData().getTestStandName());


            //enable it for bT or USB only if full support
            if (myBT.getAppConf().getConnectionType().equals("0") || (myBT.getAppConf().getConnectionType().equals("1") && myBT.getAppConf().getFullUSBSupport().equals("true")))
                btnReadThrustCurves.setEnabled(true);
            else
                btnReadThrustCurves.setEnabled(false);

            btnTelemetry.setEnabled(true);
            //enable it for bT or USB only if full support
            if (myBT.getAppConf().getConnectionType().equals("0") || (myBT.getAppConf().getConnectionType().equals("1") && myBT.getAppConf().getFullUSBSupport().equals("true"))) {
                btnTestStandSettings.setEnabled(true);
                btnReset.setEnabled(true);
                btnStatus.setEnabled(true);
            } else {
                btnTestStandSettings.setEnabled(false);
                btnReset.setEnabled(false);
                btnStatus.setEnabled(false);
            }
            msg(getResources().getString(R.string.MS_msg4));
            btnConnectDisconnect.setText(getResources().getString(R.string.disconnect));
            btnFlashFirmware.setEnabled(false);
            if(!firmCompat.IsCompatible(myBT.getTestStandConfigData().getTestStandName(),
                    myBT.getTestStandConfigData().getTestStandMajorVersion()+ "."+ myBT.getTestStandConfigData().getTestStandMinorVersion())) {
                msg(getString(R.string.flash_advice_msg));
            }
            else {
                msg(getResources().getString(R.string.MS_msg4));
            }
        } else {
            msg(getString(R.string.unsuported_firmware_msg));
            myBT.Disconnect();
        }
        // now enable or disable the menu entries by invalidating it
        invalidateOptionsMenu();
    }

    // fast way to call Toast
    private void msg(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
    }

    private void Disconnect() {
        myBT.Disconnect();
    }


    private boolean readConfig() {
        // ask for config
        boolean success = false;
        if (myBT.getConnected()) {

            Log.d("MainScreen", "Retreiving test stand config...");
            myBT.setDataReady(false);
            myBT.flush();
            myBT.clearInput();
            //switch off the main loop before sending the config
            myBT.write("m0;".toString());

            //wait for the result to come back
            try {
                while (myBT.getInputStream().available() <= 0) ;
            } catch (IOException e) {

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

                }
                myMessage = myBT.ReadResult(3000);
                //reading the config
                if (myMessage.equals("start teststandconfig end")) {
                    try {
                        AltiCfg = myBT.getTestStandConfigData();
                        success = true;
                    } catch (Exception e) {

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

                    }
                    myMessage = myBT.ReadResult(3000);
                    //reading the config
                    if (myMessage.equals("start teststandconfig end")) {
                        try {
                            AltiCfg = myBT.getTestStandConfigData();
                            success = true;
                        } catch (Exception e) {

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

                }
                myMessage = myBT.ReadResult(3000);
                if (myMessage.equals("OK")) {
                    myBT.flush();
                }
            }
        } else {
            Log.d("MainScreen", "Not connected");
        }
        return success;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        myBT.getAppConf().ReadConfig();
        //only enable bluetooth module search if connection type is bluetooth
        if(myBT.getAppConf().getConnectionType().equals("1")) {
            menu.findItem(R.id.action_bluetooth).setEnabled(false);
        }
        else {
            menu.findItem(R.id.action_bluetooth).setEnabled(true);
        }

        //if we are connected then enable some menu options and if not disable them
        if(myBT.getConnected()){
            // We are connected so no need to choose the bluetooth
            menu.findItem(R.id.action_bluetooth).setEnabled(false);
            // We are connected so we do not want to configure the 3DR module
            menu.findItem(R.id.action_mod3dr_settings).setEnabled(false);
            // same goes for the BT module
            menu.findItem(R.id.action_modbt_settings).setEnabled(false);
            // Allow connection testing
            menu.findItem(R.id.action_test_connection).setEnabled(true);
        } else {
            // not connected so allow those
            menu.findItem(R.id.action_mod3dr_settings).setEnabled(true);
            menu.findItem(R.id.action_modbt_settings).setEnabled(true);
            //cannot do connection testing until we are connected
            menu.findItem(R.id.action_test_connection).setEnabled(false);
        }
        return true;

    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent i = new Intent(MainScreenActivity.this, AppConfigActivity.class);
            startActivity(i);
            return true;
        }
        //open help screen
        if (id == R.id.action_help) {
            Intent i = new Intent(MainScreenActivity.this, HelpActivity.class);
            i.putExtra("help_file", "help");
            startActivity(i);
            return true;
        }
        if (id == R.id.action_bluetooth) {
            // choose the bluetooth device
            Intent i = new Intent(MainScreenActivity.this, SearchBluetooth.class);
            startActivity(i);
            return true;
        }
        if (id == R.id.action_about) {
            Intent i = new Intent(MainScreenActivity.this, AboutActivity.class);
            startActivity(i);
            return true;
        }
        if (id == R.id.action_mod3dr_settings) {
            Intent i = new Intent(MainScreenActivity.this, Config3DR.class);
            startActivity(i);
            return true;
        }
        //Open the bluetooth module config
        if (id == R.id.action_modbt_settings) {
            Intent i = new Intent(MainScreenActivity.this, ConfigBT.class);
            startActivity(i);
            return true;
        }
        //Test current connection
        if (id == R.id.action_test_connection) {
            Intent i = new Intent(MainScreenActivity.this, TestConnection.class);
            startActivity(i);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
       /* client.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "MainScreen Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://com.altimeter.bdureau.bearconsole/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);*/
    }

    @Override
    public void onStop() {
        super.onStop();

    }

    /* This is the Bluetooth connection sub class */
    private class ConnectBT extends AsyncTask<Void, Void, Void>  // UI thread
    {
        private AlertDialog.Builder builder = null;
        private AlertDialog alert;
        private boolean ConnectSuccess = true; //if it's here, it's almost connected

        @Override
        protected void onPreExecute() {
            //"Connecting...", "Please wait!!!"
            builder = new AlertDialog.Builder(MainScreenActivity.this);
            //Connecting...
            builder.setMessage(getResources().getString(R.string.MS_msg1))
                    .setTitle(getResources().getString(R.string.MS_msg2))
                    .setCancelable(false)
                    .setNegativeButton(getResources().getString(R.string.main_screen_actity), new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface dialog, final int id) {
                            dialog.cancel();
                            myBT.setExit(true);
                            myBT.Disconnect();
                        }
                    });
            alert = builder.create();
            alert.show();
        }

        @Override
        protected Void doInBackground(Void... devices) //while the progress dialog is shown, the connection is done in background
        {
            if (!myBT.getConnected()) {

                if (myBT.connect())
                    ConnectSuccess = true;
                else
                    ConnectSuccess = false;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) //after the doInBackground, it checks if everything went fine
        {
            super.onPostExecute(result);

            if (!ConnectSuccess) {
                //msg(myBT.lastReadResult);
                //msg(myBT.lastData);
                //Connection Failed. Is it a SPP Bluetooth? Try again.
                //msg(getResources().getString(R.string.MS_msg3));
                //finish();
            } else {
                //Connected.
                myBT.setConnected(true);
                EnableUI();
            }
            alert.dismiss();
        }
    }

    public class FirmwareCompatibility {
        // Create a hash map
        public HashMap<String, String> hm;
        FirmwareCompatibility() {
            hm =null;
            hm = new HashMap();
            //init compatible versions
            Add("TestStandSTM32V2", "1.2");
            Add("TestStandSTM32", "1.2");
            Add("TestStand", "1.1");
        }
        public void Add ( String altiName, String verList) {
            hm.put(altiName,verList);
        }

        public boolean IsCompatible(String altiName,String ver) {
            boolean compatible = false;
            String compatFirmwareList="";
            Set set = hm.entrySet();

            // Get an iterator
            Iterator i = set.iterator();
            while(i.hasNext()) {
                Map.Entry me = (Map.Entry)i.next();

                if (me.getKey().equals(altiName)){
                    compatFirmwareList = me.getValue().toString();
                    break;
                }
            }
            String firmwareVersion[] = compatFirmwareList.split(",");
            for (int j = 0; j < firmwareVersion.length; j++ ){
                if(firmwareVersion[j].equals(ver))
                    compatible = true;
            }
            return compatible;
        }

    }
}
