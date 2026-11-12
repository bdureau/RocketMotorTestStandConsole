package com.rocketmotorteststand.connection;
/**
 *
 *   @description: USB serial connection to the test stand. The input stream of the USB serial
 *   library is replaced by SerialInputStream because the library's available() method always
 *   returned 0.
 *
 *   @author: boris.dureau@neuf.fr
 *
 **/
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.util.Log;


import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.io.IOException;
import java.io.InputStream;

import java.util.concurrent.ArrayBlockingQueue;


public class UsbConnection {

    private UsbSerialDevice serialPort;
    private UsbDeviceConnection connection;
    private SerialInputStream usbInputStream;
    // Read by the UI thread and written by the connection threads
    private volatile boolean isUSBConnected = false;

    public boolean connect(UsbManager usbManager, UsbDevice device, int baudRate) {
        boolean connected = false;
        connection = usbManager.openDevice(device);
        if (connection == null) {
            // No permission or device already gone
            Log.d("SERIAL", "CANNOT OPEN DEVICE");
            setUSBConnected(false);
            return false;
        }
        serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection);
        if (serialPort != null) {
            if (serialPort.open()) { //Set Serial Connection Parameters.
                serialPort.setBaudRate(baudRate);
                serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
                serialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
                serialPort.setParity(UsbSerialInterface.PARITY_NONE);
                serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);

                usbInputStream = new SerialInputStream(serialPort);

                connected = true;
            } else {
                Log.d("SERIAL", "PORT NOT OPEN");
            }
        } else {
            Log.d("SERIAL", "PORT IS NULL");
        }
        setUSBConnected(connected);
        return connected;
    }

    /**
     * Closes the serial port and the input stream. Closing the input stream wakes up any
     * thread waiting for data: read() then returns -1 (end of stream).
     */
    public void Disconnect() {
        if (usbInputStream != null) {
            usbInputStream.close();
        }
        if (serialPort != null) {
            serialPort.close();
        }
        setUSBConnected(false);
    }

    public void write(String data) {
        if (serialPort != null) {
            serialPort.write(data.getBytes());
        }
    }

    public void flush() {
        //not implemented
    }

    public void clearInput() {
        if (usbInputStream == null)
            return;
        try {
            usbInputStream.skip(usbInputStream.available());
        } catch (IOException e) {
            // nothing to clear
        }
    }

    public InputStream getInputStream() {
        return usbInputStream;
    }

    public void setUSBConnected(boolean UsbConnected) {
        isUSBConnected = UsbConnected;
    }

    public boolean getUSBConnected() {
        return isUSBConnected;
    }

    /**
     * Created by BDUREAU on 29/07/2017.
     * Input stream fed by the USB serial library callback. Do not use the class from the USB
     * serial library because its available() method always returns 0.
     */
    public class SerialInputStream extends InputStream implements UsbSerialInterface.UsbReadCallback {
        /** Marker queued by close() to wake up a reader blocked in read(). */
        private static final int END_OF_STREAM = -1;

        protected final UsbSerialInterface device;
        protected final ArrayBlockingQueue<Integer> data = new ArrayBlockingQueue<>(256);

        protected volatile boolean is_open;

        public SerialInputStream(UsbSerialInterface mydevice) {
            this.device = mydevice;
            is_open = true;
            device.read(this);
        }

        @Override
        public int available() throws IOException {
            // Once closed, report one byte so that pollers call read() and get -1
            if (!is_open)
                return 1;
            return data.size();
        }

        @Override
        public int read() {
            while (is_open) {
                try {
                    int value = data.take();
                    if (value == END_OF_STREAM)
                        is_open = false;
                    return value;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return END_OF_STREAM;
                }
            }
            return END_OF_STREAM;
        }

        @Override
        public long skip(long n) {
            long skipped = 0;
            while (skipped < n && is_open) {
                Integer value = data.poll();
                if (value == null)
                    break;
                if (value == END_OF_STREAM) {
                    is_open = false;
                    break;
                }
                skipped++;
            }
            return skipped;
        }

        @Override
        public void close() {
            is_open = false;
            data.clear();
            // Wake up a thread blocked in take(); offer() never blocks
            data.offer(END_OF_STREAM);
        }

        public void onReceivedData(byte[] new_data) {
            for (byte b : new_data) {
                if (!is_open)
                    return;
                try {
                    data.put(((int) b) & 0xff);
                } catch (InterruptedException e) {
                    // the library thread is stopping: drop the remaining bytes
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }
}
