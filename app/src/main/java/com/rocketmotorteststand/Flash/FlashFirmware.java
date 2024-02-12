package com.rocketmotorteststand.Flash;

/**
 * @description: This is used to flash the altimeter firmware from the Android device using an OTG cable
 * so that the store Android application is compatible with altimeter. This works with the
 * ATMega328 based altimeters as well as the STM32 based altimeters
 * @author: boris.dureau@neuf.fr
 **/

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

//import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;

import android.widget.ImageView;
//import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;


import com.rocketmotorteststand.Help.AboutActivity;
import com.rocketmotorteststand.Help.HelpActivity;
import com.rocketmotorteststand.R;

import com.physicaloid.lib.Boards;
import com.physicaloid.lib.Physicaloid;

import com.physicaloid.lib.programmer.avr.UploadErrors;
import com.physicaloid.lib.usb.driver.uart.UartConfig;
import com.rocketmotorteststand.ShareHandler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import java.io.InputStream;
//import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import static com.physicaloid.misc.Misc.toHexStr;


public class FlashFirmware extends AppCompatActivity {
    Physicaloid mPhysicaloid;

    boolean recorverFirmware = false;
    public Spinner spinnerFirmware;
    public ImageView imageTestStand;

    Boards mSelectedBoard;
    Button btFlash;

    TextView tvRead;
    private AlertDialog.Builder builder = null;
    private AlertDialog alert;
    private ArrayList<Boards> mBoardList;
    private UartConfig uartConfig;

    private static final String ASSET_FILE_NAME_TESTSTAND = "firmwares/MotorTestStand.ino.hex";
    private static final String ASSET_FILE_NAME_TESTSTANDSTM32 = "firmwares/MotorTestStand1.5.ino.bin";
    private static final String ASSET_FILE_NAME_TESTSTANDSTM32V2 = "firmwares/MotorTestStand1.5V2.ino.bin";
    private static final String ASSET_FILE_NAME_TESTSTANDSTM32V3 = "firmwares/MotorTestStand1.6V3.ino.bin";


    private static final String ASSET_FILE_RESET_TESTSTAND = "recover_firmwares/ResetMotorTestStand.ino.hex";
    private static final String ASSET_FILE_RESET_TESTSTANDSTM32 = "recover_firmwares/ResetMotorTestStand.ino.bin";
    private static final String ASSET_FILE_RESET_TESTSTANDSTM32V2 = "recover_firmwares/ResetMotorTestStand.ino.bin";
    private static final String ASSET_FILE_RESET_TESTSTANDSTM32V3 = "recover_firmwares/ResetMotorTestStand.ino.bin";

    // ESP32
    private static final String ASSET_FILE_NAME_TESTSTANDESP32_FILE1 = "firmwares/ESP32/boot_app0.bin";
    private static final String ASSET_FILE_NAME_TESTSTANDESP32_FILE2 = "firmwares/ESP32/MotorTestStand1.5.ino.bootloader.bin";
    private static final String ASSET_FILE_NAME_TESTSTANDESP32_FILE3 = "firmwares/ESP32/MotorTestStand1.5.ino.bin";
    private static final String ASSET_FILE_NAME_TESTSTANDESP32_FILE4 = "firmwares/ESP32/MotorTestStand1.5.ino.partitions.bin";

    private static final String ASSET_FILE_NAME_TESTSTANDESP32V3_FILE1 = "firmwares/ESP32/boot_app0.bin";
    private static final String ASSET_FILE_NAME_TESTSTANDESP32V3_FILE2 = "firmwares/ESP32/MotorTestStand1.6V3.ino.bootloader.bin";
    private static final String ASSET_FILE_NAME_TESTSTANDESP32V3_FILE3 = "firmwares/ESP32/MotorTestStand1.6V3.ino.bin";
    private static final String ASSET_FILE_NAME_TESTSTANDESP32V3_FILE4 = "firmwares/ESP32/MotorTestStand1.6V3.ino.partitions.bin";

    private static final String ASSET_FILE_RESET_TESTSTANDESP32_FILE1 = "recover_firmwares/ESP32/boot_app0.bin";
    private static final String ASSET_FILE_RESET_TESTSTANDESP32_FILE2 = "recover_firmwares/ESP32/MotorTestStandReset.ino.bootloader.bin";
    private static final String ASSET_FILE_RESET_TESTSTANDESP32_FILE3 = "recover_firmwares/ESP32/MotorTestStandReset.ino.bin";
    private static final String ASSET_FILE_RESET_TESTSTANDESP32_FILE4 = "recover_firmwares/ESP32/MotorTestStandReset.ino.partitions.bin";

    private String[] itemsBaudRate;
    private String[] itemsFirmwares;
    private Spinner dropdownBaudRate;

    // fast way to call Toast
    private void msg(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flash_firmware);

        spinnerFirmware = (Spinner) findViewById(R.id.spinnerFirmware);
        itemsFirmwares = new String[]{
                "TestStand",
                "TestStandSTM32",
                "TestStandSTM32V2",
                "TestStandSTM32V3",
                "TestStandESP32",
                "TestStandESP32V3"
        };

        ArrayAdapter<String> adapterFirmware = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_dropdown_item, itemsFirmwares);
        spinnerFirmware.setAdapter(adapterFirmware);
        spinnerFirmware.setSelection(1);

        btFlash = (Button) findViewById(R.id.btFlash);
        tvRead = (TextView) findViewById(R.id.tvRead);
        imageTestStand = (ImageView) findViewById(R.id.imageAlti);

        mPhysicaloid = new Physicaloid(this);
        mBoardList = new ArrayList<Boards>();
        for (Boards board : Boards.values()) {
            if (board.support > 0) {
                mBoardList.add(board);
            }
        }

        mSelectedBoard = mBoardList.get(1);
        uartConfig = new UartConfig(115200, UartConfig.DATA_BITS8, UartConfig.STOP_BITS1, UartConfig.PARITY_NONE, false, false);

        btFlash.setEnabled(true);
        if (mPhysicaloid.open()) {
            mPhysicaloid.setConfig(uartConfig);
        } else {
            //cannot open
            Toast.makeText(this, getResources().getString(R.string.msg13), Toast.LENGTH_LONG).show();
        }


        //baud rate
        dropdownBaudRate = (Spinner) findViewById(R.id.spinnerBaud);
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

        ArrayAdapter<String> adapterBaudRate = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_dropdown_item, itemsBaudRate);
        dropdownBaudRate.setAdapter(adapterBaudRate);
        dropdownBaudRate.setSelection(10);

        spinnerFirmware.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                if (itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("TestStand"))
                    imageTestStand.setImageDrawable(getResources().getDrawable(R.drawable.teststand, getApplicationContext().getTheme()));

                if (itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("TestStandSTM32"))
                    imageTestStand.setImageDrawable(getResources().getDrawable(R.drawable.teststandstm32, getApplicationContext().getTheme()));

                if (itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("TestStandSTM32V2"))
                    imageTestStand.setImageDrawable(getResources().getDrawable(R.drawable.teststandstm32v2, getApplicationContext().getTheme()));
                if (itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("TestStandSTM32V3"))
                    imageTestStand.setImageDrawable(getResources().getDrawable(R.drawable.teststandstm32v2, getApplicationContext().getTheme()));

                if (itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("TestStandESP32"))
                    imageTestStand.setImageDrawable(getResources().getDrawable(R.drawable.teststandesp32, getApplicationContext().getTheme()));
                if (itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("TestStandESP32V3"))
                    imageTestStand.setImageDrawable(getResources().getDrawable(R.drawable.teststandesp32, getApplicationContext().getTheme()));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // your code here
            }

        });

        builder = new AlertDialog.Builder(this);
        //Running Saving commands
        builder.setMessage(R.string.flash_firmware_long_msg)
                .setTitle(R.string.flash_firmware_msg)
                .setCancelable(false)
                .setPositiveButton(R.string.flash_firmware_ok, new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        dialog.cancel();
                    }
                });
        alert = builder.create();
        alert.show();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        close();
    }

    public void onClickDismiss(View v) {
        close();
        finish();
    }

    public void onClickRecover(View v) {
        String recoverFileName;
        recoverFileName = ASSET_FILE_RESET_TESTSTAND;

        if (itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("TestStand"))
            recoverFileName = ASSET_FILE_RESET_TESTSTAND;

        if (itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("TestStandSTM32"))
            recoverFileName = ASSET_FILE_RESET_TESTSTANDSTM32;

        if (itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("TestStandSTM32V2"))
            recoverFileName = ASSET_FILE_RESET_TESTSTANDSTM32V2;
        if (itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("TestStandSTM32V3"))
            recoverFileName = ASSET_FILE_RESET_TESTSTANDSTM32V2;



        tvRead.setText("");
        tvRead.setText(getResources().getString(R.string.after_complete_upload));
        //rbTestStand
        if (!itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("TestStandSTM32") &&
                !itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("TestStandSTM32V2") &&
                !itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("TestStandSTM32V3")) {
            try {
                builder = new AlertDialog.Builder(FlashFirmware.this);
                //Recover firmware...
                builder.setMessage(getResources().getString(R.string.msg18))
                        .setTitle(getResources().getString(R.string.msg11))
                        .setCancelable(false)
                        .setNegativeButton(getResources().getString(R.string.firmware_cancel), new DialogInterface.OnClickListener() {
                            public void onClick(final DialogInterface dialog, final int id) {
                                dialog.cancel();
                                mPhysicaloid.cancelUpload();
                            }
                        });
                alert = builder.create();
                alert.show();
                mPhysicaloid.setBaudrate(Integer.parseInt(itemsBaudRate[(int) this.dropdownBaudRate.getSelectedItemId()]));
                mPhysicaloid.upload(mSelectedBoard, getResources().getAssets().open(recoverFileName), mUploadCallback);
            } catch (RuntimeException e) {
                //Log.e(TAG, e.toString());
            } catch (IOException e) {
                //Log.e(TAG, e.toString());
            }
        } else if (itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("TestStandESP32") ||
                itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("TestStandESP32V3")) {
            recorverFirmware = true;
            new UploadESP32Asyc().execute();
        }
        else {
            recorverFirmware = true;
            new UploadSTM32Asyc().execute();
        }
    }

    public void onClickDetect(View v) {
        new DetectAsyc().execute();
    }

    public void onClickFirmwareInfo(View v) {
        tvRead.setText(R.string.the_following_firmwares_are_available);
        tvRead.append("\n");
        tvRead.append(ASSET_FILE_NAME_TESTSTAND);
        tvRead.append("\n");
        tvRead.append(ASSET_FILE_NAME_TESTSTANDSTM32);
        tvRead.append("\n");
        tvRead.append(ASSET_FILE_NAME_TESTSTANDSTM32V2);
        tvRead.append("\n");
        tvRead.append(ASSET_FILE_NAME_TESTSTANDESP32_FILE3);
    }

    public void onClickFlash(View v) {
        String firmwareFileName;

        firmwareFileName = ASSET_FILE_NAME_TESTSTAND;

        if (itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("TestStand"))
            firmwareFileName = ASSET_FILE_NAME_TESTSTAND;

        if (itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("TestStandSTM32"))
            firmwareFileName = ASSET_FILE_NAME_TESTSTANDSTM32;

        if (itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("TestStandSTM32V2"))
            firmwareFileName = ASSET_FILE_NAME_TESTSTANDSTM32V2;

        if (itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("TestStandSTM32V3"))
            firmwareFileName = ASSET_FILE_NAME_TESTSTANDSTM32V3;

        tvRead.setText("");
        if (itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("TestStand")) {
            try {
                builder = new AlertDialog.Builder(FlashFirmware.this);
                //Flashing firmware...
                builder.setMessage(getResources().getString(R.string.msg10))
                        .setTitle(getResources().getString(R.string.msg11))
                        .setCancelable(false)
                        .setNegativeButton(getResources().getString(R.string.firmware_cancel), new DialogInterface.OnClickListener() {
                            public void onClick(final DialogInterface dialog, final int id) {
                                dialog.cancel();
                                mPhysicaloid.cancelUpload();
                            }
                        });
                alert = builder.create();
                alert.show();

                mPhysicaloid.setBaudrate(Integer.parseInt(itemsBaudRate[(int) this.dropdownBaudRate.getSelectedItemId()]));
                mPhysicaloid.upload(mSelectedBoard, getResources().getAssets().open(firmwareFileName), mUploadCallback);
            } catch (RuntimeException e) {
                //Log.e(TAG, e.toString());
            } catch (IOException e) {
                //Log.e(TAG, e.toString());
            }
        } else if (itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("TestStandESP32") ||
                itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("TestStandESP32V3")) {
            tvRead.setText(R.string.loading_esp32_firmware);
            recorverFirmware = false;
            new UploadESP32Asyc().execute();
        }
        else if(itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("TestStandSTM32") ||
                itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("TestStandSTM32V2") ||
                itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("TestStandSTM32V3")){
            tvRead.setText(R.string.loading_stm32_firmware);
            recorverFirmware = false;
            new UploadSTM32Asyc().execute();
        }
    }

    private class DetectAsyc extends AsyncTask<Void, Void, Void>  // UI thread
    {

        @Override
        protected void onPreExecute() {
            builder = new AlertDialog.Builder(FlashFirmware.this);
            //Attempting to detect firmware...
            builder.setMessage(getResources().getString(R.string.detect_firmware))
                    .setTitle(getResources().getString(R.string.msg_detect_firmware))
                    .setCancelable(false)
                    .setNegativeButton(getResources().getString(R.string.firmware_cancel), new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface dialog, final int id) {
                            dialog.cancel();
                        }
                    });
            alert = builder.create();
            alert.show();
        }

        @Override
        protected Void doInBackground(Void... voids) {

            String version = "";

            FirmwareInfo firm = new FirmwareInfo(mPhysicaloid);
            firm.open(38400);
            version = firm.getFirmwarVersion();

            tvAppend(tvRead, getString(R.string.firmware_ver_detected) + version + "\n");

            if (version.equals("TestStand")) {
                spinnerFirmware.setSelection(0);
            }
            if (version.equals("TestStandSTM32")) {
                spinnerFirmware.setSelection(1);
            }
            if (version.equals("TestStandSTM32V2")) {
                spinnerFirmware.setSelection(2);
            }
            if (version.equals("TestStandSTM32V3")) {
                spinnerFirmware.setSelection(3);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) //after the doInBackground, it checks if everything went fine
        {
            alert.dismiss();
        }
    }


    private class UploadSTM32Asyc extends AsyncTask<Void, Void, Void>  // UI thread
    {

        @Override
        protected void onPreExecute() {
            builder = new AlertDialog.Builder(FlashFirmware.this);
            //Flashing firmware...
            builder.setMessage(getResources().getString(R.string.msg10))
                    .setTitle(getResources().getString(R.string.msg11))
                    .setCancelable(false)
                    .setNegativeButton(getResources().getString(R.string.firmware_cancel), new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface dialog, final int id) {
                            dialog.cancel();
                        }
                    });
            alert = builder.create();
            alert.show();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            if (!recorverFirmware) {
                if (itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("TestStandSTM32"))
                    uploadSTM32(ASSET_FILE_NAME_TESTSTANDSTM32, mUploadSTM32Callback);
                else if (itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("TestStandSTM32V2"))
                    uploadSTM32(ASSET_FILE_NAME_TESTSTANDSTM32V2, mUploadSTM32Callback);
                else if (itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("TestStandSTM32V3"))
                    uploadSTM32(ASSET_FILE_NAME_TESTSTANDSTM32V3, mUploadSTM32Callback);
            } else {
                uploadSTM32(ASSET_FILE_RESET_TESTSTANDSTM32, mUploadSTM32Callback);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) //after the doInBackground, it checks if everything went fine
        {
            alert.dismiss();
        }
    }

    public void uploadSTM32(String fileName, UploadSTM32CallBack UpCallback) {
        boolean failed = false;
        InputStream is = null;

        try {
            is = getAssets().open(fileName);
        } catch (IOException e) {
            //e.printStackTrace();
            tvAppend(tvRead, getString(R.string.firmware_file_not_found) + ASSET_FILE_NAME_TESTSTANDSTM32 + "\n");
        } catch (Exception e) {
            e.printStackTrace();
            tvAppend(tvRead, "gethexfile : " + ASSET_FILE_NAME_TESTSTANDSTM32 + "\n");
        }

        dialogAppend(getString(R.string.firmware_starting));
        CommandInterface cmd;

        cmd = new CommandInterface(UpCallback, mPhysicaloid);

        cmd.open(Integer.parseInt(itemsBaudRate[(int) this.dropdownBaudRate.getSelectedItemId()]));
        int ret = cmd.initChip();
        if (ret == 1)
            dialogAppend("Chip has been initiated:" + ret);
        else {
            dialogAppend("Chip has not been initiated:" + ret);
            failed = true;
        }
        int bootversion = 0;
        if (!failed) {
            bootversion = cmd.cmdGet();
            //dialogAppend("bootversion:"+ bootversion);
            tvAppend(tvRead, " bootversion:" + bootversion + "\n");
            if (bootversion < 20 || bootversion >= 100) {
                tvAppend(tvRead, " bootversion not good:" + bootversion + "\n");
                failed = true;
            }
        }

        if (!failed) {
            byte chip_id[]; // = new byte [4];
            chip_id = cmd.cmdGetID();
            tvAppend(tvRead, " chip id:" + toHexStr(chip_id, 2) + "\n");
        }

        if (!failed) {
            if (bootversion < 0x30) {
                tvAppend(tvRead, "Erase 1\n");
                cmd.cmdEraseMemory();
            } else {
                tvAppend(tvRead, "Erase 2\n");
                cmd.cmdExtendedEraseMemory();
            }
        }
        if (!failed) {
            cmd.drain();
            tvAppend(tvRead, "writeMemory" + "\n");
            ret = cmd.writeMemory(0x8000000, is);
            tvAppend(tvRead, "writeMemory finish" + "\n\n\n\n");
            if (ret == 1) {
                tvAppend(tvRead, "writeMemory success" + "\n\n\n\n");
            }
        }
        if (!failed) {
            cmd.cmdGo(0x8000000);
        }
        cmd.releaseChip();
    }

    private byte[] readFile(InputStream inputStream) {
        ByteArrayOutputStream byteArrayOutputStream = null;

        int i;
        try {
            byteArrayOutputStream = new ByteArrayOutputStream();
            i = inputStream.read();
            while (i != -1) {
                byteArrayOutputStream.write(i);
                i = inputStream.read();
            }
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return byteArrayOutputStream.toByteArray();
    }
    private class UploadESP32Asyc extends AsyncTask<Void, Void, Void>  // UI thread
    {

        @Override
        protected void onPreExecute() {
            builder = new AlertDialog.Builder(FlashFirmware.this);
            //Flashing firmware...
            builder.setMessage(getResources().getString(R.string.msg10))
                    .setTitle(getResources().getString(R.string.msg11))
                    .setCancelable(false)
                    .setNegativeButton(getResources().getString(R.string.firmware_cancel), new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface dialog, final int id) {
                            dialog.cancel();
                        }
                    });
            alert = builder.create();
            alert.show();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            String firmwareFileName[] = new String[4];
            if (!recorverFirmware) {
                if (itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("TestStandESP32")) {
                    firmwareFileName[0] = ASSET_FILE_NAME_TESTSTANDESP32_FILE1;
                    firmwareFileName[1] = ASSET_FILE_NAME_TESTSTANDESP32_FILE2;
                    firmwareFileName[2] = ASSET_FILE_NAME_TESTSTANDESP32_FILE3;
                    firmwareFileName[3] = ASSET_FILE_NAME_TESTSTANDESP32_FILE4;
                } else if (itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("TestStandESP32V3")) {
                    firmwareFileName[0] = ASSET_FILE_NAME_TESTSTANDESP32V3_FILE1;
                    firmwareFileName[1] = ASSET_FILE_NAME_TESTSTANDESP32V3_FILE2;
                    firmwareFileName[2] = ASSET_FILE_NAME_TESTSTANDESP32V3_FILE3;
                    firmwareFileName[3] = ASSET_FILE_NAME_TESTSTANDESP32V3_FILE4;
                }

                uploadESP32(firmwareFileName, mUploadSTM32Callback);
            } else {
                firmwareFileName[0] = ASSET_FILE_RESET_TESTSTANDESP32_FILE1;
                firmwareFileName[1] = ASSET_FILE_RESET_TESTSTANDESP32_FILE2;
                firmwareFileName[2] = ASSET_FILE_RESET_TESTSTANDESP32_FILE3;
                firmwareFileName[3] = ASSET_FILE_RESET_TESTSTANDESP32_FILE4;

                uploadESP32(firmwareFileName, mUploadSTM32Callback);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) //after the doInBackground, it checks if everything went fine
        {
            alert.dismiss();
        }
    }

    public void uploadESP32(String fileName[], UploadSTM32CallBack UpCallback) {
        boolean failed = false;
        InputStream file1 = null;
        InputStream file2 = null;
        InputStream file3 = null;
        InputStream file4 = null;
        CommandInterfaceESP32 cmd;


        cmd = new CommandInterfaceESP32(UpCallback, mPhysicaloid);

        try {
            file1 = getAssets().open(fileName[0]);

        } catch (IOException e) {
            //e.printStackTrace();
            tvAppend(tvRead, getString(R.string.file_not_found) + ASSET_FILE_NAME_TESTSTANDESP32_FILE1 + "\n");
        } catch (Exception e) {
            e.printStackTrace();
            tvAppend(tvRead, "gethexfile : " + ASSET_FILE_NAME_TESTSTANDESP32_FILE1 + "\n");
        }

        try {
            file2 = getAssets().open(fileName[1]);

        } catch (IOException e) {
            //e.printStackTrace();
            tvAppend(tvRead, getString(R.string.file_not_found) + ASSET_FILE_NAME_TESTSTANDESP32_FILE2 + "\n");
        } catch (Exception e) {
            e.printStackTrace();
            tvAppend(tvRead, "gethexfile : " + ASSET_FILE_NAME_TESTSTANDESP32_FILE2 + "\n");
        }
        try {
            file3 = getAssets().open(fileName[2]);

        } catch (IOException e) {
            //e.printStackTrace();
            tvAppend(tvRead, getString(R.string.file_not_found) + ASSET_FILE_NAME_TESTSTANDESP32_FILE3 + "\n");
        } catch (Exception e) {
            e.printStackTrace();
            tvAppend(tvRead, "gethexfile : " + ASSET_FILE_NAME_TESTSTANDESP32_FILE3 + "\n");
        }

        try {
            file4 = getAssets().open(fileName[3]);

        } catch (IOException e) {
            //e.printStackTrace();
            tvAppend(tvRead, getString(R.string.file_not_found) + ASSET_FILE_NAME_TESTSTANDESP32_FILE4 + "\n");
        } catch (Exception e) {
            e.printStackTrace();
            tvAppend(tvRead, "gethexfile : " + ASSET_FILE_NAME_TESTSTANDESP32_FILE4 + "\n");
        }

        dialogAppend(getString(R.string.starting));


        boolean ret = cmd.initChip();
        if (ret)
            dialogAppend(getString(R.string.chip_has_not_been_init) + ret);
        else {
            dialogAppend(getString(R.string.chip_has_not_been_initiated) + ret);
            failed = true;
        }
        int bootversion = 0;
        if (!failed) {
            // let's detect the chip, not really required but I just want to make sure that
            // it is
            // an ESP32 because this is what the program is for
            int chip = cmd.detectChip();
            if (chip == cmd.ESP32)
                tvAppend(tvRead, getString(R.string.chip_is_esp32));

            // now that we have initialized the chip we can change the baud rate to 921600
            // first we tell the chip the new baud rate
            dialogAppend(getString(R.string.changing_baudrate_to_921600));
            cmd.changeBaudeRate();
            cmd.init();

            // Those are the files you want to flush
            dialogAppend(getString(R.string.flashing_file_1_0xe000));
            cmd.flashData(readFile(file1), 0xe000, 0);
            dialogAppend(getString(R.string.flashing_file_2_0x1000));
            cmd.flashData(readFile(file2), 0x1000, 0);

            dialogAppend(getString(R.string.flashing_file_3_0x10000));
            cmd.flashData(readFile(file3), 0x10000, 0);
            dialogAppend(getString(R.string.flashing_file_4_0x8000));
            cmd.flashData(readFile(file4), 0x8000, 0);

            // we have finish flashing lets reset the board so that the program can start
            cmd.reset();

            dialogAppend(getString(R.string.done));
            tvAppend(tvRead, getString(R.string.done));
        }
    }
    Physicaloid.UploadCallBack mUploadCallback = new Physicaloid.UploadCallBack() {

        @Override
        public void onUploading(int value) {
            dialogAppend(getResources().getString(R.string.msg12) + value + " %");
        }

        @Override
        public void onPreUpload() {
            //Upload : Start
            tvAppend(tvRead, getResources().getString(R.string.msg14));
        }

        public void info(String value) {
            tvAppend(tvRead, value);
        }

        @Override
        public void onPostUpload(boolean success) {
            if (success) {
                //Upload : Successful
                tvAppend(tvRead, getResources().getString(R.string.msg16));
            } else {
                //Upload fail
                tvAppend(tvRead, getResources().getString(R.string.msg15));
            }

            alert.dismiss();
        }

        @Override
        //Cancel uploading
        public void onCancel() {
            tvAppend(tvRead, getResources().getString(R.string.msg17));
        }

        @Override
        //Error  :
        public void onError(UploadErrors err) {
            tvAppend(tvRead, getResources().getString(R.string.msg18) + err.toString() + "\n");
        }

    };

    UploadSTM32CallBack mUploadSTM32Callback = new UploadSTM32CallBack() {

        @Override
        public void onUploading(int value) {
            dialogAppend(getResources().getString(R.string.msg12) + value + " %");
        }

        @Override
        public void onInfo(String value) {
            tvAppend(tvRead, value);
        }

        @Override
        public void onPreUpload() {
            //Upload : Start
            tvAppend(tvRead, getResources().getString(R.string.msg14));
        }

        public void info(String value) {
            tvAppend(tvRead, value);
        }

        @Override
        public void onPostUpload(boolean success) {
            if (success) {
                //Upload : Successful
                tvAppend(tvRead, getResources().getString(R.string.msg16));
            } else {
                //Upload fail
                tvAppend(tvRead, getResources().getString(R.string.msg15));
            }

            alert.dismiss();
        }

        @Override
        //Cancel uploading
        public void onCancel() {
            tvAppend(tvRead, getResources().getString(R.string.msg17));
        }

        @Override
        //Error  :
        public void onError(UploadSTM32Errors err) {
            tvAppend(tvRead, getResources().getString(R.string.msg18) + err.toString() + "\n");
        }

    };
    Handler mHandler = new Handler();

    private void tvAppend(TextView tv, CharSequence text) {
        final TextView ftv = tv;
        final CharSequence ftext = text;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                ftv.append(ftext);

            }
        });
    }

    private void dialogAppend(CharSequence text) {
        final CharSequence ftext = text;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                alert.setMessage(ftext);
            }
        });
    }

    private void close() {
        if (mPhysicaloid.close()) {

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

        //share current screen with other app
        if (id == R.id.action_share) {
            //takeScreenShot(getWindow().getDecorView());
            ShareHandler.takeScreenShot(findViewById(android.R.id.content).getRootView(), this);
        }

        //open help screen
        if (id == R.id.action_help) {
            Intent i = new Intent(FlashFirmware.this, HelpActivity.class);
            i.putExtra("help_file", "help_flash_firmware");
            startActivity(i);
            return true;
        }

        if (id == R.id.action_about) {
            Intent i = new Intent(FlashFirmware.this, AboutActivity.class);
            startActivity(i);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}