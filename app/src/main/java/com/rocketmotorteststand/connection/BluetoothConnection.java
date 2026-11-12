package com.rocketmotorteststand.connection;
/**
 *
 *   @description: Bluetooth classic (SPP / RFCOMM) connection to the test stand Bluetooth module:
 *   connect, disconnect, read and write.
 *   @author: boris.dureau@neuf.fr
 *
 **/
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;


public class BluetoothConnection {
    private BluetoothAdapter myBluetooth = null;
    private volatile BluetoothSocket btSocket = null;
    // Read by the UI thread and written by the connection threads
    private volatile boolean isBtConnected = false;
    //SPP UUID
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    /**
     * Opens an RFCOMM connection to the module with the given MAC address.
     * Blocking call: must not run on the UI thread.
     */
    public boolean connect(String address) {
        boolean state;
        try {
            myBluetooth = BluetoothAdapter.getDefaultAdapter();
            if (myBluetooth == null || address == null) {
                setBTConnected(false);
                return false;
            }
            BluetoothDevice btDevice = myBluetooth.getRemoteDevice(address);
            setBtSocket(btDevice.createRfcommSocketToServiceRecord(myUUID));
            try {
                // The application never starts a discovery. cancelDiscovery() requires the
                // BLUETOOTH_SCAN permission on Android 12+, which the application no longer
                // asks for, so a SecurityException is simply ignored.
                myBluetooth.cancelDiscovery();
            } catch (SecurityException e) {
                // ignored
            }
            getBtSocket().connect();
            state = true;
            setBTConnected(true);

        } catch (IOException | SecurityException | IllegalArgumentException e) {
            state = false;
            setBTConnected(false);
        }
        return state;
    }

    public void setBTConnected(boolean BtConnected) {
        isBtConnected = BtConnected;
    }

    public boolean getBTConnected() {
        return isBtConnected;
    }

    public void setBtSocket(BluetoothSocket mybtSocket) {
        btSocket = mybtSocket;
    }

    public BluetoothSocket getBtSocket() {
        return btSocket;
    }

    public InputStream getInputStream() {
        BluetoothSocket socket = btSocket;
        if (socket == null)
            return null;
        try {
            return socket.getInputStream();
        } catch (IOException e) {
            return null;
        }
    }

    public void Disconnect() {
        BluetoothSocket socket = btSocket;
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                // already closed
            }
        }
        setBTConnected(false);
    }

    public void write(String data) {
        BluetoothSocket socket = btSocket;
        if (socket == null)
            return;
        try {
            socket.getOutputStream().write(data.getBytes());
        } catch (IOException e) {
            // connection lost: the readers will time out
        }
    }

    public void flush() {
        BluetoothSocket socket = btSocket;
        if (socket == null)
            return;
        try {
            socket.getOutputStream().flush();
        } catch (IOException e) {
            // connection lost
        }
    }

    public void clearInput() {
        BluetoothSocket socket = btSocket;
        if (socket == null)
            return;
        try {
            InputStream in = socket.getInputStream();
            in.skip(in.available());
        } catch (IOException e) {
            // connection lost
        }
    }
}
