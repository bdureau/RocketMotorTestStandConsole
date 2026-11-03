/*
 * =================================================================================================
 * Project:        Rocket Motor Test Stand Console
 * Package:        com.rocketmotorteststand
 * Class:          MainScreenActivity.java
 *
 * Description:    Main dashboard activity managing device connections (Bluetooth / USB-Serial),
 *                 retrieving test stand configuration asynchronously, verifying firmware
 *                 compatibility, and routing user actions to telemetry, thrust curve plotting,
 *                 firmware flashing, and hardware configuration screens.
 *
 * Author:         Bear Instruments / Rocket Motor Test Stand Team
 * Standards:      Android API 21+ compatible, robust asynchronous I/O handling, memory leak-safe.
 * =================================================================================================
 */

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
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.rocketmotorteststand.Flash.FlashFirmware;
import com.rocketmotorteststand.ThrustCurve.ThrustCurveListActivity;
import com.rocketmotorteststand.config.ConfigModules.Config3DR;
import com.rocketmotorteststand.config.ConfigModules.ConfigBT;
import com.rocketmotorteststand.config.ConfigModules.ConfigLoraE220;
import com.rocketmotorteststand.config.ConfigModules.ConfigLoraE32;
import com.rocketmotorteststand.config.TestStandConfigData;
import com.rocketmotorteststand.config.TestStandTabConfigActivity;
import com.rocketmotorteststand.config.AppConfigTabActivity;
import com.rocketmotorteststand.Help.AboutActivity;
import com.rocketmotorteststand.Help.HelpActivity;
import com.rocketmotorteststand.connection.SearchBluetooth;
import com.rocketmotorteststand.connection.TestConnection;
import com.rocketmotorteststand.Telemetry.TestStandStatusTabActivity;
import com.rocketmotorteststand.Telemetry.TestStandTelemetryTabActivity;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainScreenActivity extends AppCompatActivity {

    public final String TAG = "MainScreenActivity";
    public final String ACTION_USB_PERMISSION = "com.rocketmotorteststand.USB_PERMISSION";

    // Bluetooth / USB target address
    private String address = null;

    // Dashboard navigation cards
    private CardView btnTestStandSettings, btnReadThrustCurves, btnConnectDisconnect, btnReset;
    private CardView btnTelemetry, btnStatus, btnFlashFirmware, btnAbout;

    // Dashboard card icons
    private ImageView image_settings, image_curve, image_telemetry, image_reset, image_status,
            image_firmware, image_info, image_connect;

    // Dashboard card labels
    private TextView text_settings, text_curve, text_telemetry, text_reset, text_status,
            text_firmware, text_info, text_connect;

    // USB subsystem managers
    private UsbManager usbManager;
    private UsbDevice device;

    // Application state and configuration handles
    private ConsoleApplication myBT;
    private TestStandConfigData AltiCfg = null;
    private FirmwareCompatibility firmCompat = null;

    // Background workers to prevent UI thread freezing (ANR)
    private final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean isReceiverRegistered = false;

    /**
     * BroadcastReceiver monitoring USB hardware events (Permissions, Attach, Detach).
     */
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) return;

            if (action.equals(ACTION_USB_PERMISSION)) {
                // Check if user granted permission to access USB device
                boolean granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false);

                if (granted) {
                    if (myBT.connect(usbManager, device, 38400)) {
                        myBT.setConnected(true);
                        Log.d(TAG, "USB connection successfully established");
                        myBT.setConnectionType("usb");
                        loadConfigAndEnableUI();
                    }
                } else {
                    msg(getString(R.string.perm_not_granted));
                }
            } else if (action.equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
                msg(getString(R.string.connection_via_usb_ok));
            } else if (action.equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
                // Handle hot-unplugging of USB device
                if ("usb".equals(myBT.getConnectionType()) && myBT.getConnected()) {
                    myBT.Disconnect();
                    DisableUI();
                    setEnabledCard(true, btnFlashFirmware, image_firmware, text_firmware);
                    text_connect.setText(getString(R.string.connect));
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

        // Obtain application context instance holding global connection states
        myBT = (ConsoleApplication) getApplication();

        // Initialize firmware compatibility matrix
        firmCompat = new FirmwareCompatibility();
        setContentView(R.layout.activity_main_screen);

        // Register USB lifecycle receiver safely using AndroidX ContextCompat
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);

        ContextCompat.registerReceiver(this, broadcastReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
        isReceiverRegistered = true;

        // Initialize CardView widgets
        btnTestStandSettings = findViewById(R.id.settings_card);
        btnReadThrustCurves = findViewById(R.id.thrust_curve_card);
        btnTelemetry = findViewById(R.id.telemetry_card);
        btnStatus = findViewById(R.id.status_card);
        btnFlashFirmware = findViewById(R.id.flash_card);
        btnConnectDisconnect = findViewById(R.id.connect_card);
        btnReset = findViewById(R.id.reset_card);
        btnAbout = findViewById(R.id.info_card);

        // Initialize CardView icons
        image_settings = findViewById(R.id.image_settings);
        image_curve = findViewById(R.id.image_curve);
        image_telemetry = findViewById(R.id.image_telemetry);
        image_reset = findViewById(R.id.image_reset);
        image_status = findViewById(R.id.image_status);
        image_firmware = findViewById(R.id.image_firmware);
        image_info = findViewById(R.id.image_info);
        image_connect = findViewById(R.id.image_connect);

        // Initialize CardView text labels
        text_settings = findViewById(R.id.text_settings);
        text_curve = findViewById(R.id.text_curve);
        text_telemetry = findViewById(R.id.text_telemetry);
        text_reset = findViewById(R.id.text_reset);
        text_status = findViewById(R.id.text_status);
        text_firmware = findViewById(R.id.text_firmware);
        text_info = findViewById(R.id.text_info);
        text_connect = findViewById(R.id.text_connect);

        // Adjust UI according to initial connection state
        if (myBT.getConnected()) {
            loadConfigAndEnableUI();
            setEnabledCard(false, btnFlashFirmware, image_firmware, text_firmware);
        } else {
            DisableUI();
            text_connect.setText(R.string.connect);
            setEnabledCard(true, btnFlashFirmware, image_firmware, text_firmware);
        }

        // --- Card Click Listeners ---

        btnTestStandSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainScreenActivity.this, TestStandTabConfigActivity.class));
            }
        });

        btnFlashFirmware.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myBT.getAppConf().ReadConfig();
                startActivity(new Intent(MainScreenActivity.this, FlashFirmware.class));
            }
        });

        btnAbout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainScreenActivity.this, AboutActivity.class));
            }
        });

        btnReadThrustCurves.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainScreenActivity.this, ThrustCurveListActivity.class));
            }
        });

        btnTelemetry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (myBT.getConnected()) {
                    myBT.flush();
                    myBT.clearInput();
                    myBT.write("y1;"); // Enable live telemetry downlink
                }
                startActivity(new Intent(MainScreenActivity.this, TestStandTelemetryTabActivity.class));
            }
        });

        btnStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (myBT.getConnected()) {
                    myBT.flush();
                    myBT.clearInput();
                    myBT.write("y1;");
                }
                startActivity(new Intent(MainScreenActivity.this, TestStandStatusTabActivity.class));
            }
        });

        btnConnectDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myBT.getAppConf().ReadConfig();
                if (myBT.getAppConf().getConnectionType() == GlobalConfig.ConnectionType.BT) {
                    myBT.setConnectionType("bluetooth");
                } else {
                    myBT.setConnectionType("usb");
                }

                if (myBT.getConnected()) {
                    // Disconnect active link
                    Disconnect();
                    DisableUI();
                    setEnabledCard(true, btnFlashFirmware, image_firmware, text_firmware);
                    text_connect.setText(R.string.connect);
                } else {
                    // Handle Bluetooth connection request
                    if ("bluetooth".equals(myBT.getConnectionType())) {
                        address = myBT.getAddress();
                        if (address != null) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(MainScreenActivity.this);
                            builder.setMessage(getString(R.string.do_you_want_to_connect_to_module) + myBT.getModuleName() + "?")
                                    .setTitle("")
                                    .setCancelable(false)
                                    .setPositiveButton(getString(R.string.Yes), new DialogInterface.OnClickListener() {
                                        public void onClick(final DialogInterface dialog, final int id) {
                                            dialog.cancel();
                                            new ConnectBT().execute();
                                        }
                                    })
                                    .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                                        public void onClick(final DialogInterface dialog, final int id) {
                                            dialog.cancel();
                                            startActivity(new Intent(MainScreenActivity.this, SearchBluetooth.class));
                                        }
                                    });
                            builder.create().show();
                        } else {
                            startActivity(new Intent(MainScreenActivity.this, SearchBluetooth.class));
                        }
                    } else {
                        // Handle USB Serial connection request
                        myBT.setModuleName("USB");
                        HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
                        if (!usbDevices.isEmpty()) {
                            for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                                device = entry.getValue();

                                // Configure PendingIntent flags safely according to Android OS version
                                int flags = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) ? PendingIntent.FLAG_IMMUTABLE : 0;
                                PendingIntent pi = PendingIntent.getBroadcast(
                                        MainScreenActivity.this, 0, new Intent(ACTION_USB_PERMISSION), flags);

                                usbManager.requestPermission(device, pi);
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
                startActivity(new Intent(MainScreenActivity.this, ResetSettingsActivity.class));
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unregister receiver to prevent memory leak crashes
        if (isReceiverRegistered) {
            unregisterReceiver(broadcastReceiver);
            isReceiverRegistered = false;
        }
        // Shutdown background thread pool
        backgroundExecutor.shutdownNow();
    }

    /**
     * Disables interactive cards when disconnected.
     */
    private void DisableUI() {
        setEnabledCard(false, btnTestStandSettings, image_settings, text_settings);
        setEnabledCard(false, btnReadThrustCurves, image_curve, text_curve);
        setEnabledCard(false, btnTelemetry, image_telemetry, text_telemetry);
        setEnabledCard(false, btnReset, image_reset, text_reset);
        setEnabledCard(false, btnStatus, image_status, text_status);
        invalidateOptionsMenu();
    }

    /**
     * Controls the enabled state and visual alpha of a card view.
     */
    private void setEnabledCard(boolean enable, CardView card, ImageView image, TextView text) {
        card.setEnabled(enable);
        image.setImageAlpha(enable ? 0xFF : 0x3F);
        if (text != null) {
            text.setAlpha(enable ? 1.0f : 0.4f);
        }
    }

    /**
     * Executes configuration retrieval asynchronously off the main UI thread.
     */
    private void loadConfigAndEnableUI() {
        backgroundExecutor.execute(new Runnable() {
            @Override
            public void run() {
                boolean success = false;
                // Retry up to 4 times to ensure reliable serial handshake
                for (int attempt = 0; attempt < 4 && !success; attempt++) {
                    success = readConfig();
                }

                final boolean finalSuccess = success;
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        applyUIConfiguration(finalSuccess);
                    }
                });
            }
        });
    }

    /**
     * Evaluates board compatibility and configures UI elements accordingly on the main thread.
     */
    private void applyUIConfiguration(boolean readSuccess) {
        TestStandConfigData config = myBT.getTestStandConfigData();

        if (readSuccess && config != null && config.getTestStandName() != null) {
            String standName = config.getTestStandName();
            Log.d(TAG, "Test stand identified: " + standName);

            if (isSupportedStand(standName)) {
                boolean fullSupportOrBT = (myBT.getAppConf().getConnectionType() == GlobalConfig.ConnectionType.BT) ||
                        (myBT.getAppConf().getConnectionType() == GlobalConfig.ConnectionType.USB && myBT.getAppConf().getFullUSBSupport());

                setEnabledCard(fullSupportOrBT, btnReadThrustCurves, image_curve, text_curve);
                setEnabledCard(true, btnTelemetry, image_telemetry, text_telemetry);
                setEnabledCard(fullSupportOrBT, btnTestStandSettings, image_settings, text_settings);
                setEnabledCard(fullSupportOrBT, btnReset, image_reset, text_reset);
                setEnabledCard(fullSupportOrBT, btnStatus, image_status, text_status);

                text_connect.setText(getString(R.string.disconnect));
                setEnabledCard(false, btnFlashFirmware, image_firmware, text_firmware);

                String version = config.getTestStandMajorVersion() + "." + config.getTestStandMinorVersion();
                if (!firmCompat.IsCompatible(standName, version)) {
                    msg(getString(R.string.flash_advice_msg));
                } else {
                    msg(getString(R.string.MS_msg4));
                }
            } else {
                msg(getString(R.string.unsuported_firmware_msg));
                myBT.Disconnect();
                DisableUI();
                setEnabledCard(true, btnFlashFirmware, image_firmware, text_firmware);
                text_connect.setText(getString(R.string.connect));
            }
        } else {
            msg(getString(R.string.unsuported_firmware_msg));
            myBT.Disconnect();
            DisableUI();
            setEnabledCard(true, btnFlashFirmware, image_firmware, text_firmware);
            text_connect.setText(getString(R.string.connect));
        }
        invalidateOptionsMenu();
    }

    /**
     * Checks if the connected board name is supported by the application.
     */
    private boolean isSupportedStand(String name) {
        return "TestStand".equals(name) ||
                "TestStandSTM32".equals(name) ||
                "TestStandSTM32V2".equals(name) ||
                "TestStandSTM32V3".equals(name) ||
                "TestStandESP32".equals(name) ||
                "TestStandESP32V4".equals(name) ||
                "TestStandESP32V3".equals(name);
    }

    private void msg(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
    }

    private void Disconnect() {
        myBT.Disconnect();
    }

    /**
     * Background protocol routine to pause board acquisition, read configuration, and resume loop.
     */
    private boolean readConfig() {
        if (!myBT.getConnected()) {
            Log.d(TAG, "Not connected");
            return false;
        }

        try {
            Thread.sleep(500); // Allow UART buffer to stabilize
            myBT.setDataReady(false);
            myBT.flush();
            myBT.clearInput();

            // Command m0: Pause test stand measurement loop
            myBT.write("m0;");

            String myMessage = myBT.ReadResult(3000);
            if (!"OK".equals(myMessage)) {
                return false;
            }

            // Command b: Request configuration block
            myBT.setDataReady(false);
            myBT.flush();
            myBT.clearInput();
            myBT.write("b;");
            myBT.flush();

            myMessage = myBT.ReadResult(3000);
            boolean configOk = false;

            if ("start teststandconfig end".equals(myMessage)) {
                AltiCfg = myBT.getTestStandConfigData();
                configOk = true;
            } else {
                // Second attempt if not acknowledged
                myBT.setDataReady(false);
                myBT.flush();
                myBT.clearInput();
                myBT.write("b;");
                myBT.flush();

                myMessage = myBT.ReadResult(3000);
                if ("start teststandconfig end".equals(myMessage)) {
                    AltiCfg = myBT.getTestStandConfigData();
                    configOk = true;
                }
            }

            // Command m1: Resume test stand measurement loop
            myBT.setDataReady(false);
            myBT.flush();
            myBT.clearInput();
            myBT.write("m1;");

            myBT.ReadResult(3000);
            myBT.flush();

            return configOk;
        } catch (Exception e) {
            Log.e(TAG, "Error in readConfig: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        myBT.getAppConf().ReadConfig();

        // Restrict Bluetooth pairing option if connection type is USB
        if (myBT.getAppConf().getConnectionType() == GlobalConfig.ConnectionType.USB) {
            menu.findItem(R.id.action_bluetooth).setEnabled(false);
        } else {
            menu.findItem(R.id.action_bluetooth).setEnabled(true);
        }

        // Toggle configuration menu items depending on current active connection
        if (myBT.getConnected()) {
            menu.findItem(R.id.action_bluetooth).setEnabled(false);
            menu.findItem(R.id.action_mod3dr_settings).setEnabled(false);
            menu.findItem(R.id.action_modbt_settings).setEnabled(false);
            menu.findItem(R.id.action_modlora_settings).setEnabled(false);
            menu.findItem(R.id.action_modlorae32_settings).setEnabled(false);
            menu.findItem(R.id.action_test_connection).setEnabled(true);
        } else {
            menu.findItem(R.id.action_mod3dr_settings).setEnabled(true);
            menu.findItem(R.id.action_modbt_settings).setEnabled(true);
            menu.findItem(R.id.action_modlora_settings).setEnabled(true);
            menu.findItem(R.id.action_modlorae32_settings).setEnabled(true);
            menu.findItem(R.id.action_test_connection).setEnabled(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            startActivity(new Intent(this, AppConfigTabActivity.class));
            return true;
        }
        if (id == R.id.action_help) {
            Intent i = new Intent(this, HelpActivity.class);
            i.putExtra("help_file", "help");
            startActivity(i);
            return true;
        }
        if (id == R.id.action_bluetooth) {
            startActivity(new Intent(this, SearchBluetooth.class));
            return true;
        }
        if (id == R.id.action_about) {
            startActivity(new Intent(this, AboutActivity.class));
            return true;
        }
        if (id == R.id.action_mod3dr_settings) {
            startActivity(new Intent(this, Config3DR.class));
            return true;
        }
        if (id == R.id.action_modbt_settings) {
            startActivity(new Intent(this, ConfigBT.class));
            return true;
        }
        if (id == R.id.action_modlora_settings) {
            startActivity(new Intent(this, ConfigLoraE220.class));
            return true;
        }
        if (id == R.id.action_modlorae32_settings) {
            startActivity(new Intent(this, ConfigLoraE32.class));
            return true;
        }
        if (id == R.id.action_test_connection) {
            startActivity(new Intent(this, TestConnection.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Asynchronous task handling Bluetooth socket connection.
     */
    private class ConnectBT extends AsyncTask<Void, Void, Boolean> {
        private AlertDialog alert;

        @Override
        protected void onPreExecute() {
            String dialogMsg = getString(R.string.MS_msg1) + "\n" + myBT.getModuleName();
            AlertDialog.Builder builder = new AlertDialog.Builder(MainScreenActivity.this);
            builder.setMessage(dialogMsg)
                    .setTitle(getString(R.string.MS_msg2))
                    .setCancelable(false)
                    .setNegativeButton(getString(R.string.main_screen_actity), new DialogInterface.OnClickListener() {
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
        protected Boolean doInBackground(Void... devices) {
            if (!myBT.getConnected()) {
                return myBT.connect();
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            super.onPostExecute(success);
            if (alert != null && alert.isShowing()) {
                alert.dismiss();
            }

            if (success) {
                myBT.setConnected(true);
                loadConfigAndEnableUI();
            } else {
                msg(getString(R.string.MS_msg3));
            }
        }
    }

    /**
     * Helper class mapping hardware platforms to supported firmware revisions.
     */
    public class FirmwareCompatibility {
        private final HashMap<String, String> hm = new HashMap<>();

        public FirmwareCompatibility() {
            Add("TestStandSTM32", "1.7,1.8,1.9");
            Add("TestStandSTM32V2", "1.5,1.6,1.7,1.8,1.9");
            Add("TestStandSTM32V3", "1.7,1.8,1.9");
            Add("TestStandESP32", "1.7,1.8,1.9");
            Add("TestStandESP32V3", "1.7,1.8,1.9");
            Add("TestStandESP32V4", "1.9");
            Add("TestStand", "1.1");
        }

        public void Add(String altiName, String verList) {
            hm.put(altiName, verList);
        }

        /**
         * Validates whether a given board version is registered as compatible.
         */
        public boolean IsCompatible(String altiName, String ver) {
            if (altiName == null || ver == null) return false;

            String compatFirmwareList = hm.get(altiName);
            if (compatFirmwareList == null) return false;

            String[] firmwareVersions = compatFirmwareList.split(",");
            for (String fv : firmwareVersions) {
                if (fv.trim().equals(ver.trim())) {
                    return true;
                }
            }
            return false;
        }
    }
}