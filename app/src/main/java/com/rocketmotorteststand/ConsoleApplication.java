package com.rocketmotorteststand;
/**
 * @description: This class instanciate pretty much everything including the connection
 * @author: boris.dureau@neuf.fr
 **/

import android.app.Application;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Handler;

import com.rocketmotorteststand.ThrustCurve.ThrustCurveData;
import com.rocketmotorteststand.config.TestStandConfigData;
import com.rocketmotorteststand.config.AppConfigData;
import com.rocketmotorteststand.connection.BluetoothConnection;
import com.rocketmotorteststand.connection.UsbConnection;

import java.io.IOException;
import java.io.InputStream;

import java.util.Locale;

/**
 * @description: This is quite a major class used everywhere because it can point to your connection,
 * appconfig
 * @author: boris.dureau@neuf.fr
 **/
public class ConsoleApplication extends Application {
    private boolean isConnected = false;
    // Store number of flight
    public int NbrOfFlight = 0;
    public int currentThrustCurveNbr = 0;
    private ThrustCurveData MyThrustCurve = null;
    private TestStandConfigData TestStandCfg = null;

    private static boolean DataReady = false;
    public long lastReceived = 0;
    public String commandRet = "";

    private double FEET_IN_METER = 1;
    private boolean exit = false;
    private GlobalConfig AppConf = null;
    private String address;
    private String myTypeOfConnection = "bluetooth";// "USB";//"bluetooth";

    private BluetoothConnection BTCon = null;
    private UsbConnection UsbCon = null;

    private Handler mHandler;

    public void setHandler(Handler mHandler) {
        this.mHandler = mHandler;
    }

    public String lastReadResult;
    public String lastData;

    @Override
    public void onCreate() {

        super.onCreate();
        TestStandCfg = new TestStandConfigData();
        MyThrustCurve = new ThrustCurveData(this, TestStandCfg.getTestStandName());
        AppConf = new GlobalConfig(this);
        AppConf.ReadConfig();
        BTCon = new BluetoothConnection();
        UsbCon = new UsbConnection();
        /*if (AppConf.getConnectionType().equals("0"))
            //bluetooth
            myTypeOfConnection= "bluetooth";
        else
            myTypeOfConnection ="usb";*/
        myTypeOfConnection = AppConf.getConnectionTypeValue();

    }

    public void setConnectionType(String TypeOfConnection) {
        myTypeOfConnection = TypeOfConnection;
    }

    public String getConnectionType() {
        return myTypeOfConnection;
    }

    public void setAddress(String bTAddress) {

        address = bTAddress;

    }

    public String getAddress() {
        return address;
    }

    public InputStream getInputStream() {
        InputStream tmpIn = null;
        if (myTypeOfConnection.equals("bluetooth")) {
            tmpIn = BTCon.getInputStream();
        } else {
            tmpIn = UsbCon.getInputStream();
        }
        return tmpIn;
    }

    public void setConnected(boolean Connected) {
        if (myTypeOfConnection.equals("bluetooth")) {
            BTCon.setBTConnected(Connected);
        } else {
            UsbCon.setUSBConnected(Connected);
        }
    }

    public UsbConnection getUsbCon() {
        return UsbCon;
    }

    public boolean getConnected() {
        boolean ret = false;
        if (myTypeOfConnection.equals("bluetooth")) {
            ret = BTCon.getBTConnected();
        } else {
            ret = UsbCon.getUSBConnected();
        }
        return ret;
    }

    public void setTestStandConfigData(TestStandConfigData configData) {
        TestStandCfg = configData;
    }

    public TestStandConfigData getTestStandConfigData() {
        return TestStandCfg;
    }

    public void setFlightData(ThrustCurveData fData) {
        MyThrustCurve = fData;
    }

    public ThrustCurveData getThrustCurveData() {
        return MyThrustCurve;
    }

    // connect to the bluetooth adapter
    public boolean connect() {
        boolean state = false;
       //appendLog("connect:");
        if (myTypeOfConnection.equals("bluetooth")) {
            state = BTCon.connect(address);
            setConnectionType("bluetooth");
            if (!isConnectionValid()) {
                Disconnect();
                state = false;
            }
        }
        return state;
    }

    // connect to the USB
    public boolean connect(UsbManager usbManager, UsbDevice device, int baudRate) {
        boolean state = false;
        if (myTypeOfConnection.equals("usb")) {
            state = UsbCon.connect(usbManager, device, baudRate);
            setConnectionType("usb");
            if (!isConnectionValid()) {
                Disconnect();
                state = false;
            }
        }
        return state;
    }

    public boolean connectFirmware(UsbManager usbManager, UsbDevice device, int baudRate) {
        boolean state = false;
        if (myTypeOfConnection.equals("usb")) {
            state = UsbCon.connect(usbManager, device, baudRate);
            setConnectionType("usb");
            /*if (!isConnectionValid()) {
                Disconnect();
                state = false;
            }*/
        }
        return state;
    }

    public boolean isConnectionValid() {
        boolean valid = false;
        //if(getConnected()) {

        setDataReady(false);
       /* try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }*/
        flush();
        clearInput();
        //  send 2 commands to get read of the module connection string on some modules
        write("h;".toString());

        flush();
        clearInput();
        write("h;".toString());

        String myMessage = "";
        long timeOut = 10000;
        long startTime = System.currentTimeMillis();
        long diffTime = 0;
        //get the results
        //wait for the result to come back
        try {
            /*while (getInputStream().available() <= 0 || diffTime < timeOut) {
                diffTime = System.currentTimeMillis() - startTime;}*/
            while (getInputStream().available() <= 0) ;
        } catch (IOException e) {

        }

        myMessage = ReadResult(3000);

        if (myMessage.equals("OK")) {
            lastReadResult = myMessage;
            valid = true;
        } else {
            lastReadResult = myMessage;
            valid = false;
        }
        //}

        //valid = true;
        return valid;
    }

    public void Disconnect() {
        if (myTypeOfConnection.equals("bluetooth")) {
            BTCon.Disconnect();
        } else {
            UsbCon.Disconnect();
        }
    }

    public void flush() {
        if (myTypeOfConnection.equals("bluetooth")) {
            BTCon.flush();
        }
    }

    public void write(String data) {
        if (myTypeOfConnection.equals("bluetooth")) {
            BTCon.write(data);
        } else {
            UsbCon.write(data);
        }
    }

    public void clearInput() {
        if (myTypeOfConnection.equals("bluetooth")) {
            BTCon.clearInput();
        } else {
            UsbCon.clearInput();
        }
    }

    public void initThrustCurveData() {
        MyThrustCurve = new ThrustCurveData(this, TestStandCfg.getTestStandName());

        if(AppConf.getUnits().equals("0"))  {//meters
        //if (AppConf.getUnitsValue().equals("Meters")) {
            FEET_IN_METER = 1;
        } else {
            FEET_IN_METER = 3.28084;
        }
    }



    public void setExit(boolean b) {
        this.exit = b;
    }


    public long calculateSentenceCHK(String currentSentence[]) {
        long chk = 0;
        String sentence = "";

        for (int i = 0; i < currentSentence.length - 1; i++) {
            sentence = sentence + currentSentence[i] + ",";
        }
        //Log.d("calculateSentenceCHK", sentence);
        chk = generateCheckSum(sentence);
        return chk;
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

    public String ReadResult(long timeout) {

        // Reads in data while data is available

        //setDataReady(false);
        this.exit = false;
        lastData = "";
        String fullBuff = "";
        String myMessage = "";
        lastReceived = System.currentTimeMillis();
        try {


            while (this.exit == false) {
                if ((System.currentTimeMillis() - lastReceived) > timeout)
                    this.exit = true;
                if (getInputStream().available() > 0) {
                    // Read in the available character
                    char ch = (char) getInputStream().read();
                    lastData = lastData + ch;
                    if (ch == '$') {

                        // read entire sentence until the end
                        String tempBuff = "";
                        while (ch != ';') {
                            // this is not the end of our command
                            ch = (char) getInputStream().read();
                            if (ch != '\r')
                                if (ch != '\n')
                                    if (ch != ';')
                                        tempBuff = tempBuff
                                                + Character.toString(ch);
                        }
                        if (ch == ';') {
                            ch = (char) getInputStream().read();
                        }

                        // Sentence currentSentence = null;
                        String currentSentence[] = new String[50];
                        if (!tempBuff.isEmpty()) {
                            //currentSentence = readSentence(tempBuff);
                            currentSentence = tempBuff.split(",");

                            fullBuff = fullBuff + tempBuff;
                        }

                        long chk = 0;
                        switch (currentSentence[0]) {
                            case "telemetry":
                                if (currentSentence[currentSentence.length - 1].matches("\\d+(?:\\.\\d+)?"))
                                    chk = Long.valueOf(currentSentence[currentSentence.length - 1]);
                                if (calculateSentenceCHK(currentSentence) == chk) {
                                    if (mHandler != null) {
                                        // Value 1 contains the current thrust
                                        if (currentSentence.length > 1)
                                            if (currentSentence[1].matches("\\d+(?:\\.\\d+)?"))
                                                mHandler.obtainMessage(1, String.valueOf(currentSentence[1])).sendToTarget();
                                            else
                                                mHandler.obtainMessage(1, String.valueOf(0)).sendToTarget();

                                        // value 2 time
                                        if (currentSentence.length > 2)
                                            if (currentSentence[2].matches("\\d+(?:\\.\\d+)?"))
                                                mHandler.obtainMessage(2, String.valueOf(currentSentence[2])).sendToTarget();
                                            else
                                                mHandler.obtainMessage(2, String.valueOf(0)).sendToTarget();

                                        // Value 3 contains the battery voltage
                                        if (currentSentence.length > 3)
                                            if (currentSentence[3].matches("\\d+(?:\\.\\d+)?"))
                                                mHandler.obtainMessage(3, String.valueOf(currentSentence[3])).sendToTarget();
                                            else
                                                mHandler.obtainMessage(3, String.valueOf(0)).sendToTarget();

                                        // Value 4 contains the eeprom
                                        if (currentSentence.length > 4)
                                            if (currentSentence[4].matches("\\d+(?:\\.\\d+)?"))
                                                mHandler.obtainMessage(4, String.valueOf(currentSentence[4])).sendToTarget();
                                            else
                                                mHandler.obtainMessage(4, String.valueOf(0)).sendToTarget();
                                        // Value 5 contains the number of thrust curves
                                        if (currentSentence.length > 5)
                                            if (currentSentence[5].matches("\\d+(?:\\.\\d+)?"))
                                                mHandler.obtainMessage(5, String.valueOf(currentSentence[5])).sendToTarget();
                                            else
                                                mHandler.obtainMessage(5, String.valueOf(0)).sendToTarget();

                                    }
                                }
                                break;

                            case "data":
                                if (currentSentence[currentSentence.length - 1].matches("\\d+(?:\\.\\d+)?"))
                                    chk = Long.valueOf(currentSentence[currentSentence.length - 1]);
                                String thrustCurveName = "Thrust CurveXX";
                                if (calculateSentenceCHK(currentSentence) == chk) {
                                    // Value 1 contain the thrust curve number
                                    if (currentSentence.length > 1)
                                        if (currentSentence[1].matches("\\d+(?:\\.\\d+)?")) {
                                            currentThrustCurveNbr = Integer.valueOf(currentSentence[1]) + 1;
                                            if (currentThrustCurveNbr < 10)
                                                //thrust curve
                                                thrustCurveName = getResources().getString(R.string.thrustcurve_name) + " "+ "0" + currentThrustCurveNbr;
                                            else
                                                //thrust curve
                                                thrustCurveName = getResources().getString(R.string.thrustcurve_name) + " " + currentThrustCurveNbr;
                                        }
                                    // Value 2 contain the time
                                    // Value 3 contain the thrust
                                    // To do
                                    int value2 = 0, value3 = 0;
                                    if (currentSentence.length > 2)
                                        if (currentSentence[2].matches("\\d+(?:\\.\\d+)?"))
                                            value2 = Integer.valueOf(currentSentence[2]);
                                        else
                                            value2 = 0;
                                    if (currentSentence.length > 3) {
                                        if (currentSentence[3].matches("\\d+(?:\\.\\d+)?"))
                                            value3 = Integer.valueOf(currentSentence[3]);
                                        else
                                            value3 = 0;
                                        //add the thrust
                                        MyThrustCurve.AddToFlight(value2,
                                                (long) (value3 * FEET_IN_METER), thrustCurveName, 0);

                                    }

                                }
                                break;
                            case "teststandconfig":
                                if (currentSentence[currentSentence.length - 1].matches("\\d+(?:\\.\\d+)?"))
                                    chk = Long.valueOf(currentSentence[currentSentence.length - 1]);
                                if (calculateSentenceCHK(currentSentence) == chk) {
                                    // Value 1 contains the units
                                    if (currentSentence.length > 1)
                                        if (currentSentence[1].matches("\\d+(?:\\.\\d+)?"))
                                            TestStandCfg.setUnits(Integer.valueOf(currentSentence[1]));
                                        else
                                            TestStandCfg.setUnits(0);

                                    // Value 2 contains The test stand name
                                    if (currentSentence.length > 2)
                                        TestStandCfg.setTestStandName(currentSentence[2]);

                                    // Value 3 contains the test stand major version
                                    if (currentSentence.length > 3)
                                        if (currentSentence[3].matches("\\d+(?:\\.\\d+)?"))
                                            TestStandCfg.setTestStandMajorVersion(Integer.valueOf(currentSentence[3]));
                                        else
                                            TestStandCfg.setTestStandMajorVersion(0);
                                    // Value 4 contain the test stand minor version
                                    if (currentSentence.length > 4)
                                        if (currentSentence[4].matches("\\d+(?:\\.\\d+)?"))
                                            TestStandCfg.setTestStandMinorVersion(Integer.valueOf(currentSentence[4]));
                                        else
                                            TestStandCfg.setTestStandMinorVersion(0);
                                    // Value 5 contains the connection speed
                                    if (currentSentence.length > 5)
                                        if (currentSentence[5].matches("\\d+(?:\\.\\d+)?"))
                                            TestStandCfg.setConnectionSpeed(Integer.valueOf(currentSentence[5]));
                                        else
                                            TestStandCfg.setConnectionSpeed(38400);

                                    // Value 6 contains start recording thrust level
                                    if (currentSentence.length > 6)
                                        if (currentSentence[6].matches("\\d+(?:\\.\\d+)?"))
                                            TestStandCfg.setStartRecordingThrustLevel(Integer.valueOf(currentSentence[6]));
                                        else
                                            //if you cannot read it, set it to 1 N
                                            TestStandCfg.setStartRecordingThrustLevel(1);

                                    // Value 7 contains the stop recording thrust level
                                    if (currentSentence.length > 7)
                                        if (currentSentence[7].matches("\\d+(?:\\.\\d+)?"))
                                            TestStandCfg.setStopRecordingThrustLevel(Integer.valueOf(currentSentence[7]));
                                        else
                                            TestStandCfg.setStopRecordingThrustLevel(0);

                                    // Value 8 contains the test stand resolution
                                    if (currentSentence.length > 8)
                                        if (currentSentence[8].matches("\\d+(?:\\.\\d+)?"))
                                            TestStandCfg.setTestStandResolution(Integer.valueOf(currentSentence[8]));
                                        else
                                            TestStandCfg.setTestStandResolution(0);
                                    // Value 9 contains the eeprom size
                                    if (currentSentence.length > 9)
                                        if (currentSentence[9].matches("\\d+(?:\\.\\d+)?"))
                                            TestStandCfg.setEepromSize(Integer.valueOf(currentSentence[9]));
                                        else
                                            TestStandCfg.setEepromSize(512);

                                    // value 10 contains battery type
                                    if (currentSentence.length > 10)
                                        if (currentSentence[10].matches("\\d+(?:\\.\\d+)?"))
                                            TestStandCfg.setBatteryType(Integer.valueOf(currentSentence[10]));
                                        else
                                            TestStandCfg.setBatteryType(0);

                                    // value 11 contains calibration_factor
                                    if (currentSentence.length > 11)
                                        if (currentSentence[11].matches("\\d+(?:\\.\\d+)?"))
                                            TestStandCfg.setCalibrationFactor(Integer.valueOf(currentSentence[11]));
                                        else
                                            TestStandCfg.setCalibrationFactor(0);
                                    // value 12 contains calibration_factor
                                    if (currentSentence.length > 12)
                                        if (currentSentence[12].matches("\\d+(?:\\.\\d+)?"))
                                            TestStandCfg.setCurrentOffset(Integer.valueOf(currentSentence[12]));
                                        else
                                            TestStandCfg.setCurrentOffset(0);
                                    myMessage = myMessage + " " + "teststandconfig";
                                } else {
                                    myMessage = myMessage + "KO" + "teststandconfig";
                                }

                                break;

                            case "nbrOfThrustCurve":
                                // Value 1 contains the number of Thrust curve
                                if (currentSentence.length > 1)
                                    if (currentSentence[1].matches("\\d+(?:\\.\\d+)?"))
                                        NbrOfFlight = (Integer.valueOf(currentSentence[1]));
                                myMessage = myMessage + " " + "nbrOfThrustCurve";
                                break;
                            case "start":
                                // We are starting reading data
                                setDataReady(false);
                                myMessage = "start";
                                break;
                            case "end":
                                // We have finished reading data
                                setDataReady(true);
                                myMessage = myMessage + " " + "end";
                                exit = true;
                                break;
                            case "OK":
                                setDataReady(true);
                                commandRet = currentSentence[0];
                                myMessage = "OK";
                                exit = true;
                                break;
                            case "KO":
                                setDataReady(true);
                                commandRet = currentSentence[0];
                                break;
                            case "UNKNOWN":
                                setDataReady(true);
                                commandRet = currentSentence[0];
                                break;
                            default:

                                break;
                        }

                    }
                }
            }
        } catch (IOException e) {
            myMessage = myMessage + " " + "error:" + e.getMessage();
        }
        return myMessage;
    }

    public void setDataReady(boolean value) {
        DataReady = value;
    }

    public boolean getDataReady() {
        return DataReady;
    }


    public Configuration getAppLocal() {

        Locale locale = null;
        if (AppConf.getApplicationLanguage().equals("1")) {
            locale = Locale.FRENCH;//new Locale("fr_FR");
        } else if (AppConf.getApplicationLanguage().equals("2")) {
            locale = Locale.ENGLISH;//new Locale("en_US");
        } else {
            locale = Locale.getDefault();
        }


        Configuration config = new Configuration();
        config.locale = locale;
        return config;

    }


    public GlobalConfig getAppConf() {
        return AppConf;
    }

    public void setAppConf(GlobalConfig value) {
        AppConf = value;
    }

    public class GlobalConfig {
        Context context;

        SharedPreferences appConfig = null;
        SharedPreferences.Editor edit = null;
        AppConfigData appCfgData = null;
        //application language
        private String applicationLanguage = "0";
        //Graph units
        private String units = "0";

        //flight retrieval timeout
       // private long flightRetrievalTimeout;
        //data retrieval timeout
        //private long configRetrievalTimeout;

        //graph background color
        private String graphBackColor = "1";
        //graph color
        private String graphColor = "0";
        //graph font size
        private String fontSize = "10";
        // connection type is bluetooth
        private String connectionType = "0";
        // default baud rate for USB is 38400
        private String baudRate = "8";
        private String graphicsLibType = "0";

        //private String allowMultipleDrogueMain = "false";
        private String fullUSBSupport= "false";
        /*private String say_main_event= "false";
        private String say_apogee_altitude= "false";
        private String say_main_altitude= "false";
        private String say_drogue_event= "false";
        private String say_altitude_event= "false";
        private String say_landing_event= "false";
        private String say_burnout_event= "false";
        private String say_warning_event= "false";
        private String say_liftoff_event= "false";

        private String telemetryVoice = "0";*/

        public GlobalConfig(Context current) {
            appConfig = getSharedPreferences("BearConsoleCfg", MODE_PRIVATE);
            edit = appConfig.edit();
            context = current;
            appCfgData = new AppConfigData(context);

        }

        public void ResetDefaultConfig() {

            applicationLanguage = "0"; // default to english
            graphBackColor = "1";
            graphColor = "0";
            fontSize = "10";
            units = "0"; //default to meters
            baudRate = "8"; // default to 38400 baud
            connectionType = "0";
            graphicsLibType = "1"; //Default to MP android chart lib
            //allowMultipleDrogueMain = "false";
            fullUSBSupport = "false";
            /*say_main_event= "false";
            say_apogee_altitude= "false";
            say_main_altitude= "false";
            say_drogue_event= "false";
            say_altitude_event= "false";
            say_landing_event= "false";
            say_burnout_event= "false";
            say_warning_event= "false";
            say_liftoff_event="false";
            telemetryVoice ="0";*/
            /*edit.clear();
            edit.putString("AppLanguage","0");
            edit.putString("Units", "0");
            edit.putString("GraphColor", "0");
            edit.putString("GraphBackColor", "1");
            edit.putString("FontSize", "10");*/

        }

        public void ReadConfig() {
            try {
                //Application language
                String applicationLanguage;
                applicationLanguage = appConfig.getString("AppLanguage", "");
                if (!applicationLanguage.equals(""))
                    setApplicationLanguage(applicationLanguage);

                //Application Units
                String appUnit;
                appUnit = appConfig.getString("Units", "");
                if (!appUnit.equals(""))
                    setUnits(appUnit);

                //Graph color
                String graphColor;
                graphColor = appConfig.getString("GraphColor", "");
                if (!graphColor.equals(""))
                    setGraphColor(graphColor);

                //Graph Background color
                String graphBackColor;
                graphBackColor = appConfig.getString("GraphBackColor", "");
                if (!graphBackColor.equals(""))
                    setGraphBackColor(graphBackColor);

                //Font size
                String fontSize;
                fontSize = appConfig.getString("FontSize", "");
                if (!fontSize.equals(""))
                    setFontSize(fontSize);

                //Baud rate
                String baudRate;
                baudRate = appConfig.getString("BaudRate", "");
                if (!baudRate.equals(""))
                    setBaudRate(baudRate);

                //Connection type
                String connectionType;
                connectionType = appConfig.getString("ConnectionType", "");
                if (!connectionType.equals(""))
                    setConnectionType(connectionType);

                //Graphics Lib Type
                String graphicsLibType;
                graphicsLibType = appConfig.getString("GraphicsLibType", "1");
                if (!graphicsLibType.equals(""))
                    setGraphicsLibType(graphicsLibType);

                //Allow multiple drogue and main
              /*  String allowMultipleDrogueMain;
                allowMultipleDrogueMain = appConfig.getString("AllowMultipleDrogueMain", "false");
                if (!allowMultipleDrogueMain.equals(""))
                    setAllowMultipleDrogueMain(allowMultipleDrogueMain);*/

                //enable full USB support
                String fullUSBSupport;
                fullUSBSupport = appConfig.getString("fullUSBSupport", "false");
                if (!fullUSBSupport.equals(""))
                    setFullUSBSupport(fullUSBSupport);

               /* String say_main_event;
                say_main_event = appConfig.getString("say_main_event", "false");
                if (!say_main_event.equals(""))
                    setMain_event(say_main_event);

                String say_apogee_altitude;
                say_apogee_altitude = appConfig.getString("say_apogee_altitude", "false");
                if (!say_apogee_altitude.equals(""))
                    setApogee_altitude(say_apogee_altitude);

                String say_main_altitude;
                say_main_altitude = appConfig.getString("say_main_altitude", "false");
                if (!say_main_altitude.equals(""))
                    setMain_altitude(say_main_altitude);

                String say_drogue_event;
                say_drogue_event = appConfig.getString("say_drogue_event", "false");
                if (!say_drogue_event.equals(""))
                    setDrogue_event(say_drogue_event);

                String say_altitude_event;
                say_altitude_event = appConfig.getString("say_altitude_event", "false");
                if (!say_altitude_event.equals(""))
                    setAltitude_event(say_altitude_event);

                String say_landing_event;
                say_landing_event = appConfig.getString("say_landing_event", "false");
                if (!say_landing_event.equals(""))
                    setLanding_event(say_landing_event);

                String say_burnout_event;
                say_burnout_event = appConfig.getString("say_burnout_event", "false");
                if (!say_burnout_event.equals(""))
                    setBurnout_event(say_burnout_event);

                String say_warning_event;
                say_warning_event = appConfig.getString("say_warning_event", "false");
                if (!say_warning_event.equals(""))
                    setWarning_event(say_warning_event);

                String say_liftoff_event;
                say_liftoff_event = appConfig.getString("say_liftoff_event", "false");
                if (!say_liftoff_event.equals(""))
                    setLiftOff_event(say_liftoff_event);

                String telemetryVoice;
                telemetryVoice = appConfig.getString("telemetryVoice", "0");
                if(!telemetryVoice.equals(""))
                    setTelemetryVoice(telemetryVoice);*/
            } catch (Exception e) {

            }
        }

        public void SaveConfig() {
            edit.putString("AppLanguage", getApplicationLanguage());
            edit.putString("Units", getUnits());
            edit.putString("GraphColor", getGraphColor());
            edit.putString("GraphBackColor", getGraphBackColor());
            edit.putString("FontSize", getFontSize());
            edit.putString("BaudRate", getBaudRate());
            edit.putString("ConnectionType", getConnectionType());
            edit.putString("GraphicsLibType", getGraphicsLibType());
           // edit.putString("AllowMultipleDrogueMain", getAllowMultipleDrogueMain());
            edit.putString("fullUSBSupport", getFullUSBSupport());

            /*edit.putString("say_main_event", getMain_event());
            edit.putString("say_apogee_altitude", getApogee_altitude());
            edit.putString("say_main_altitude", getMain_altitude());
            edit.putString("say_drogue_event", getDrogue_event());
            edit.putString("say_altitude_event", getAltitude_event());
            edit.putString("say_landing_event", getLanding_event());
            edit.putString("say_burnout_event", getBurnout_event());
            edit.putString("say_warning_event", getWarning_event());
            edit.putString("say_liftoff_event", getLiftOff_event());
            edit.putString("telemetryVoice", getTelemetryVoice());*/
            edit.commit();

        }

        public String getFontSize() {
            return fontSize;
        }

        public void setFontSize(String value) {
            fontSize = value;
        }

        public String getApplicationLanguage() {
            return applicationLanguage;
        }

        public void setApplicationLanguage(String value) {
            applicationLanguage = value;
        }

        //return the unit id
        public String getUnits() {
            return units;
        }

        public String getUnitsValue() {
            return appCfgData.getUnitsByNbr(Integer.parseInt(units));
        }

        //set the unit by id
        public void setUnits(String value) {
            units = value;
        }

        public String getGraphColor() {
            return graphColor;
        }

        public void setGraphColor(String value) {
            graphColor = value;
        }

        public String getGraphBackColor() {
            return graphBackColor;
        }

        public void setGraphBackColor(String value) {
            graphBackColor = value;
        }

        //get the id of the current connection type
        public String getConnectionType() {
            return connectionType;
        }

        //get the name of the current connection type
        public String getConnectionTypeValue() {
            return appCfgData.getConnectionTypeByNbr(Integer.parseInt(connectionType));
        }

        public void setConnectionType(String value) {
            connectionType = value;
        }

        public String getGraphicsLibType() {
            return graphicsLibType;
        }

        public String getGraphicsLibTypeValue() {
            return appCfgData.getGraphicsLibTypeByNbr(Integer.parseInt(graphicsLibType));
        }

        public void setGraphicsLibType(String value) {
            graphicsLibType = value;
        }

        public String getBaudRate() {
            return baudRate;
        }

        public String getBaudRateValue() {
            return appCfgData.getBaudRateByNbr(Integer.parseInt(baudRate));
        }

        public void setBaudRate(String value) {
            baudRate = value;
        }


       /* public void setAllowMultipleDrogueMain(String value) {
            allowMultipleDrogueMain = value;
        }
        public String getAllowMultipleDrogueMain() {
            return allowMultipleDrogueMain;//appCfgData.getMultipleDrogueMain();
        }*/
        public void setFullUSBSupport(String value) {
            fullUSBSupport = value;
        }
        public String getFullUSBSupport() {
            return fullUSBSupport;//appCfgData.getMultipleDrogueMain();
        }

        /*public void setMain_event(String value) { say_main_event = value;  }
        public String getMain_event() {
            return say_main_event;
        }

        public void setApogee_altitude(String value) { say_apogee_altitude = value;  }
        public String getApogee_altitude() {
            return say_apogee_altitude;
        }

        public void setMain_altitude(String value) { say_main_altitude = value;  }
        public String getMain_altitude() {
            return say_main_altitude;
        }

        public void setDrogue_event(String value) { say_drogue_event = value;  }
        public String getDrogue_event() {
            return say_drogue_event;
        }

        public void setAltitude_event(String value) { say_altitude_event = value;  }
        public String getAltitude_event() {
            return say_altitude_event;
        }

        public void setLanding_event(String value) { say_landing_event = value;  }
        public String getLanding_event() {
            return say_landing_event;
        }

        public void setBurnout_event(String value) { say_burnout_event = value;  }
        public String getBurnout_event() {
            return say_burnout_event;
        }

        public void setWarning_event(String value) { say_warning_event = value;  }
        public String getWarning_event() {
            return say_warning_event;
        }

        public void setLiftOff_event(String value) { say_liftoff_event = value;  }
        public String getLiftOff_event() {
            return say_liftoff_event;
        }

        public void setTelemetryVoice(String value) {telemetryVoice =value;}
        public String getTelemetryVoice() {
            return telemetryVoice;
        }*/
        public int ConvertFont(int font) {
            return font + 8;
        }

        public int ConvertColor(int col) {

            int myColor = 0;

            switch (col) {

                case 0:
                    myColor = Color.BLACK;
                    break;
                case 1:
                    myColor = Color.WHITE;
                    break;
                case 2:
                    myColor = Color.MAGENTA;
                    break;
                case 3:
                    myColor = Color.BLUE;
                    break;
                case 4:
                    myColor = Color.YELLOW;
                    break;
                case 5:
                    myColor = Color.GREEN;
                    break;
                case 6:
                    myColor = Color.GRAY;
                    break;
                case 7:
                    myColor = Color.CYAN;
                    break;
                case 8:
                    myColor = Color.DKGRAY;
                    break;
                case 9:
                    myColor = Color.LTGRAY;
                    break;
                case 10:
                    myColor = Color.RED;
                    break;
            }
            return myColor;
        }
    }
}
