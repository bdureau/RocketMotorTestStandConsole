package com.rocketmotorteststand.connection;
/**
 *   @description:  Lists the paired Bluetooth devices so that the user can select the test
 *   stand Bluetooth module. Handles the Android 12+ BLUETOOTH_CONNECT runtime permission.
 *
 *   @author: boris.dureau@neuf.fr
 *
 **/
import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.rocketmotorteststand.ConsoleApplication;
import com.rocketmotorteststand.R;
import com.rocketmotorteststand.ShareHandler;
import com.rocketmotorteststand.Help.AboutActivity;
import com.rocketmotorteststand.Help.HelpActivity;

import java.util.ArrayList;
import java.util.Set;

public class SearchBluetooth extends AppCompatActivity {
    //widgets
    Button btnPaired, btnDismiss;
    ListView devicelist;
    //Bluetooth
    private BluetoothAdapter myBluetooth = null;
    private Set<BluetoothDevice> pairedDevices;
    public static String EXTRA_ADDRESS = "device_address";
    ConsoleApplication myBT ;
    /** Request code used for the BLUETOOTH_CONNECT runtime permission. */
    private static final int REQUEST_BT_CONNECT = 2;
    /** Request code used to ask the user to switch Bluetooth on. */
    private static final int REQUEST_ENABLE_BT = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        myBT = (ConsoleApplication) getApplication();

        setContentView(R.layout.activity_search_bluetooth);

        //Calling widgets
        btnPaired = (Button)findViewById(R.id.button);
        btnDismiss = (Button) findViewById(R.id.butSearchBTDismiss);
        devicelist = (ListView)findViewById(R.id.listView);

        // The buttons are wired first: the former code returned from onCreate while asking
        // for the permission, so on Android 12+ the buttons did nothing on first launch.
        btnPaired.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                pairedDevicesList();
            }
        });

        btnDismiss.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        //if the device has bluetooth
        myBluetooth = BluetoothAdapter.getDefaultAdapter();

        if(myBluetooth == null)
        {
            //Bluetooth Device Not Available
            Toast.makeText(getApplicationContext(), getResources().getString(R.string.BT_msg1), Toast.LENGTH_LONG).show();
            // finish() does not stop onCreate: return, the code below would crash
            finish();
            return;
        }

        if (!hasConnectPermission()) {
            // The list is filled in onRequestPermissionsResult once the user has answered
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_BT_CONNECT);
            return;
        }
        enableBluetoothOrListDevices();
    }

    /**
     * BLUETOOTH_CONNECT is a runtime permission on Android 12+ only; it is implicitly granted
     * on older versions. (BLUETOOTH_SCAN is not needed: the application only lists the
     * already paired devices and never runs a discovery.)
     */
    private boolean hasConnectPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S)
            return true;
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Asks the user to switch Bluetooth on if needed, otherwise lists the paired devices.
     */
    @SuppressWarnings("MissingPermission")
    private void enableBluetoothOrListDevices() {
        if (!myBluetooth.isEnabled()) {
            Intent turnBTon = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(turnBTon, REQUEST_ENABLE_BT);
            return;
        }
        pairedDevicesList();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BT_CONNECT) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableBluetoothOrListDevices();
            } else {
                msgPermissionDenied();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Bluetooth has been switched on (or not): refresh the list
        if (requestCode == REQUEST_ENABLE_BT && myBluetooth != null && myBluetooth.isEnabled()) {
            pairedDevicesList();
        }
    }

    private void msgPermissionDenied() {
        Toast.makeText(getApplicationContext(), getResources().getString(R.string.perm_not_granted),
                Toast.LENGTH_LONG).show();
    }

    @SuppressWarnings("MissingPermission")
    private void pairedDevicesList()
    {
        if (myBluetooth == null)
            return;
        if (!hasConnectPermission()) {
            ActivityCompat.requestPermissions(SearchBluetooth.this,
                    new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_BT_CONNECT);
            return;
        }
        pairedDevices = myBluetooth.getBondedDevices();
        ArrayList list = new ArrayList();

        if (pairedDevices != null && pairedDevices.size()>0)
        {
            for(BluetoothDevice bt : pairedDevices)
            {
                list.add(bt.getName() + "\n" + bt.getAddress()); //Get the device's name and the address
            }
        }
        else
        {
            //"No Paired Bluetooth Devices Found."
            Toast.makeText(getApplicationContext(), getResources().getString(R.string.BT_msg2), Toast.LENGTH_LONG).show();
        }

        final ArrayAdapter adapter = new ArrayAdapter(this,android.R.layout.simple_list_item_1, list);
        devicelist.setAdapter(adapter);
        devicelist.setOnItemClickListener(myListClickListener); //Method called when the device from the list is clicked

    }

    private AdapterView.OnItemClickListener myListClickListener = new AdapterView.OnItemClickListener()
    {
        public void onItemClick (AdapterView<?> av, View v, int arg2, long arg3)
        {
            // Get the device MAC address, the last 17 chars in the View
            String info = ((TextView) v).getText().toString();
            String address = info.substring(info.length() - 17);
            myBT.setAddress(address);
            String moduleName = info.substring(0, info.length() - 17);
            myBT.setModuleName(moduleName);
            finish();
        }
    };


    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
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

        //open application settings screen
       /* if (id == R.id.action_settings) {
            Intent i= new Intent(SearchBluetooth.this, AppConfigActivity.class);
            startActivity(i);
            return true;
        }*/
        //share screen
        if (id == R.id.action_share) {
            ShareHandler.takeScreenShot(findViewById(android.R.id.content).getRootView(), this);
            return true;
        }
        //open help screen
        if (id == R.id.action_help) {
            Intent i= new Intent(SearchBluetooth.this, HelpActivity.class);
            i.putExtra("help_file", "help_bluetooth");
            startActivity(i);
            return true;
        }
        //open about screen
        if (id == R.id.action_about) {
            Intent i= new Intent(SearchBluetooth.this, AboutActivity.class);
            startActivity(i);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
