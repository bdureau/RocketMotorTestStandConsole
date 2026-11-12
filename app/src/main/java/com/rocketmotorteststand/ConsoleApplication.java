package com.rocketmotorteststand;
/**
 * @description: Application singleton shared by every screen. It owns the active connection
 * (Bluetooth or USB serial), the test stand configuration read from the board, the
 * downloaded thrust curves and the application settings. It also implements the serial
 * protocol parser (ReadResult) that decodes the "$sentence,...,checksum;" frames sent by
 * the test stand firmware.
 * @author: boris.dureau@neuf.fr
 **/

import android.app.Application;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import com.rocketmotorteststand.ThrustCurve.ThrustCurveData;
import com.rocketmotorteststand.config.TestStandConfigData;
import com.rocketmotorteststand.connection.BluetoothConnection;
import com.rocketmotorteststand.connection.UsbConnection;

import java.io.IOException;
import java.io.InputStream;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

public class ConsoleApplication extends Application {
    private static final String TAG = "TestStand console";

    /** Name of the shared preferences file holding the application settings. */
    public static final String PREFS_NAME = "TestStandConsoleCfg";

    /** Maximum length of one protocol sentence; longer input is treated as line noise. */
    private static final int MAX_SENTENCE_LENGTH = 1024;

    /** Pause between two polls of the input stream when no byte is available (ms). */
    private static final long POLL_INTERVAL_MS = 2;

    // Pre-compiled number patterns. A single digit is a valid number: the former pattern
    // "^-?[0-9]\\d+" required at least two digits and silently rejected every frame whose
    // checksum was between 1 and 9 (about 3.5% of all frames).
    private static final Pattern SIGNED_NUMBER = Pattern.compile("-?\\d+(?:\\.\\d+)?");
    private static final Pattern UNSIGNED_NUMBER = Pattern.compile("\\d+(?:\\.\\d+)?");
    private static final Pattern SIGNED_INTEGER = Pattern.compile("-?\\d+");

    // Number of thrust curves stored on the board
    public int NbrOfThrustCurves = 0;
    public int currentThrustCurveNbr = 0;
    private ThrustCurveData MyThrustCurve = null;
    private TestStandConfigData TestStandCfg = null;
    private TestTrame testTrame = null;

    // Shared between the UI thread and the reading threads: must be volatile so that a
    // change made by one thread is immediately visible to the others.
    private static volatile boolean DataReady = false;
    private volatile boolean exit = false;
    private volatile Handler mHandler;

    public volatile long lastReceived = 0;
    public volatile String commandRet = "";
    public volatile String lastReadResult;
    public volatile String lastData;

    private GlobalConfig AppConf = null;
    private String address, moduleName;
    private volatile String myTypeOfConnection = "bluetooth";

    private BluetoothConnection BTCon = null;
    private UsbConnection UsbCon = null;

    // Single background thread used to send commands to the board without blocking the UI
    // thread (for instance when a screen is closed and must stop the telemetry stream).
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

    /**
     * Optional stop condition checked while waiting for serial data. It lets a reading
     * thread be stopped reliably by its owner (e.g. when the telemetry screen is closed)
     * even if the global exit flag has just been reset by a new ReadResult call.
     */
    public interface StopCondition {
        boolean shouldStop();
    }

    /**
     * Returns a context whose resources use the language selected in the application
     * settings: 0 = phone language, 1 = English.
     * Only the Application context uses it; activities get their language from
     * AppCompatDelegate (see applyAppLanguage).
     */
    public static Context updateBaseContextLocale(Context context, int appLanguage) {
        Locale locale;
        if (appLanguage == 1) {
            locale = Locale.ENGLISH;
        } else {
            // Read the real system locale. Locale.getDefault() is not used because it may
            // have been overridden earlier in the same process, which prevented switching
            // back to the phone language without restarting the application.
            Configuration systemConfig = Resources.getSystem().getConfiguration();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                locale = systemConfig.getLocales().get(0);
            } else {
                locale = systemConfig.locale;
            }
        }
        Configuration config = new Configuration(context.getResources().getConfiguration());
        config.setLocale(locale);
        return context.createConfigurationContext(config);
    }

    /**
     * Applies the language selected in the settings to every AppCompat activity using the
     * per-app language API. AppCompat recreates the visible activities when it changes.
     * 0 = phone language, 1 = English.
     */
    public static void applyAppLanguage(int appLanguage) {
        LocaleListCompat locales = (appLanguage == 1)
                ? LocaleListCompat.forLanguageTags("en")
                : LocaleListCompat.getEmptyLocaleList();
        if (!locales.equals(AppCompatDelegate.getApplicationLocales())) {
            AppCompatDelegate.setApplicationLocales(locales);
        }
    }

    @Override
    protected void attachBaseContext(Context base) {
        SharedPreferences prefs = base.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int lang = prefs.getInt("AppLanguage", 0);
        super.attachBaseContext(updateBaseContextLocale(base, lang));
    }

    public void setHandler(Handler mHandler) {
        this.mHandler = mHandler;
    }

    /**
     * Removes the message handler, but only if it is still the one registered by the
     * caller. Screens call it when they are destroyed so that the application does not
     * keep a reference to a dead activity.
     */
    public void clearHandler(Handler handler) {
        if (this.mHandler == handler) {
            this.mHandler = null;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        TestStandCfg = new TestStandConfigData();
        MyThrustCurve = new ThrustCurveData(this, TestStandCfg.getTestStandName());
        testTrame = new TestTrame();
        AppConf = new GlobalConfig(this);
        AppConf.ReadConfig();
        BTCon = new BluetoothConnection();
        UsbCon = new UsbConnection();
        myTypeOfConnection = AppConf.getConnectionTypeValue();
        // Apply the saved language to every activity
        applyAppLanguage(AppConf.getApplicationLanguage());
    }

    /**
     * Returns the single background thread used to talk to the board outside of the UI
     * thread. Commands submitted here are executed in order.
     */
    public ExecutorService getIoExecutor() {
        return ioExecutor;
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

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String bTmoduleName) {
        moduleName = bTmoduleName;
    }

    public InputStream getInputStream() {
        if ("bluetooth".equals(myTypeOfConnection)) {
            return BTCon.getInputStream();
        } else {
            return UsbCon.getInputStream();
        }
    }

    public void setConnected(boolean Connected) {
        if ("bluetooth".equals(myTypeOfConnection)) {
            BTCon.setBTConnected(Connected);
        } else {
            UsbCon.setUSBConnected(Connected);
        }
    }

    public UsbConnection getUsbCon() {
        return UsbCon;
    }

    public boolean getConnected() {
        if ("bluetooth".equals(myTypeOfConnection)) {
            return BTCon.getBTConnected();
        } else {
            return UsbCon.getUSBConnected();
        }
    }

    public void setTestStandConfigData(TestStandConfigData configData) {
        TestStandCfg = configData;
    }

    public TestStandConfigData getTestStandConfigData() {
        return TestStandCfg;
    }

    /**
     * Convenience accessor for the name of the connected board model.
     */
    public String getBoardName() {
        return TestStandCfg != null ? TestStandCfg.getTestStandName() : null;
    }

    public TestTrame getTestTrame() {
        return testTrame;
    }

    public void setFlightData(ThrustCurveData fData) {
        MyThrustCurve = fData;
    }

    public ThrustCurveData getThrustCurveData() {
        return MyThrustCurve;
    }

    public int getNbrOfThrustCurves() {
        return NbrOfThrustCurves;
    }

    public void setNbrOfThrustCurves(int value) {
        NbrOfThrustCurves = value;
    }

    /**
     * Connects to the Bluetooth module whose address was selected. Must be called from a
     * background thread because it blocks until the board answers or the timeout expires.
     */
    public boolean connect() {
        boolean state = false;
        if ("bluetooth".equals(myTypeOfConnection)) {
            state = BTCon.connect(address);
            setConnectionType("bluetooth");
            if (state && !isConnectionValid()) {
                Disconnect();
                state = false;
            }
        }
        return state;
    }

    /**
     * Opens the USB serial port and checks that a test stand answers. Must be called from a
     * background thread because it blocks until the board answers or the timeout expires.
     */
    public boolean connect(UsbManager usbManager, UsbDevice device, int baudRate) {
        boolean state = false;
        if ("usb".equals(myTypeOfConnection)) {
            state = UsbCon.connect(usbManager, device, baudRate);
            setConnectionType("usb");
            if (state && !isConnectionValid()) {
                Disconnect();
                state = false;
            }
        }
        return state;
    }

    /**
     * Opens the USB serial port without checking for a test stand answer. Used by the
     * firmware flashing screen, where the board runs its bootloader.
     */
    public boolean connectFirmware(UsbManager usbManager, UsbDevice device, int baudRate) {
        boolean state = false;
        if ("usb".equals(myTypeOfConnection)) {
            state = UsbCon.connect(usbManager, device, baudRate);
            setConnectionType("usb");
        }
        return state;
    }

    /**
     * Sends the "h" command and returns true if the board answers "$OK;".
     * Waits at most 10 seconds for the first byte, then 3 seconds for the answer.
     */
    public boolean isConnectionValid() {
        setDataReady(false);
        flush();
        clearInput();
        // Send 2 commands to get rid of the module connection string on some modules
        write("h;");

        flush();
        clearInput();
        write("h;");

        if (!waitForData(10000)) {
            lastReadResult = "";
            return false;
        }

        String myMessage = ReadResult(3000);
        lastReadResult = myMessage;
        return "OK".equals(myMessage);
    }

    public void Disconnect() {
        if ("bluetooth".equals(myTypeOfConnection)) {
            BTCon.Disconnect();
        } else {
            UsbCon.Disconnect();
        }
    }

    public void flush() {
        if ("bluetooth".equals(myTypeOfConnection)) {
            BTCon.flush();
        }
    }

    public void write(String data) {
        if ("bluetooth".equals(myTypeOfConnection)) {
            BTCon.write(data);
        } else {
            UsbCon.write(data);
        }
    }

    public void clearInput() {
        if ("bluetooth".equals(myTypeOfConnection)) {
            BTCon.clearInput();
        } else {
            UsbCon.clearInput();
        }
    }

    public void initThrustCurveData() {
        MyThrustCurve = new ThrustCurveData(this, TestStandCfg.getTestStandName());
    }

    /**
     * Requests the current ReadResult / waitForData call to stop as soon as possible.
     */
    public void setExit(boolean b) {
        this.exit = b;
    }

    /**
     * Waits until at least one byte is available on the connection.
     * Replaces the former busy loops "while (available() <= 0);" which had no timeout,
     * used 100% of a CPU core and froze the application when the board did not answer.
     *
     * @param timeoutMs maximum waiting time in milliseconds
     * @return true if data is available, false on timeout, cancel or closed connection
     */
    public boolean waitForData(long timeoutMs) {
        return waitForData(timeoutMs, null);
    }

    /**
     * Same as waitForData(long) but also stops when the given condition becomes true.
     */
    public boolean waitForData(long timeoutMs, StopCondition stop) {
        this.exit = false;
        long deadline = System.currentTimeMillis() + timeoutMs;
        try {
            while (true) {
                if (exit || (stop != null && stop.shouldStop()))
                    return false;
                InputStream in = getInputStream();
                if (in == null)
                    return false;
                if (in.available() > 0)
                    return true;
                if (System.currentTimeMillis() > deadline)
                    return false;
                Thread.sleep(POLL_INTERVAL_MS);
            }
        } catch (IOException e) {
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Reads one byte from the connection, waiting until the deadline.
     *
     * @return the byte (0-255), or -1 on timeout, cancel, interruption or end of stream
     */
    private int readByte(long deadline, StopCondition stop) throws IOException {
        while (true) {
            if (exit || (stop != null && stop.shouldStop()))
                return -1;
            InputStream in = getInputStream();
            if (in == null)
                return -1;
            if (in.available() > 0) {
                // read() returns -1 when the stream has been closed (e.g. USB unplugged).
                // The former code cast it to a char ('￿') and looped forever.
                return in.read();
            }
            if (System.currentTimeMillis() > deadline)
                return -1;
            try {
                Thread.sleep(POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return -1;
            }
        }
    }

    public long calculateSentenceCHK(String currentSentence[]) {
        StringBuilder sentence = new StringBuilder();
        for (int i = 0; i < currentSentence.length - 1; i++) {
            sentence.append(currentSentence[i]).append(',');
        }
        return generateCheckSum(sentence.toString());
    }

    /**
     * Checksum used by the firmware: sum of the bytes of the sentence modulo 256.
     */
    public static Integer generateCheckSum(String value) {
        byte[] data = value.getBytes();
        long checksum = 0L;
        for (byte b : data) {
            checksum += b;
        }
        checksum = checksum % 256;
        return (int) checksum;
    }

    /**
     * Returns true if the last field of the sentence is a valid checksum of the other fields.
     */
    private boolean isChecksumValid(String[] sentence) {
        String last = sentence[sentence.length - 1].trim();
        if (!SIGNED_INTEGER.matcher(last).matches())
            return false;
        try {
            return calculateSentenceCHK(sentence) == Long.parseLong(last);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /** Returns true if the value is a (possibly negative, possibly decimal) number. */
    public static boolean isSignedNumber(String value) {
        return value != null && SIGNED_NUMBER.matcher(value.trim()).matches();
    }

    /** Returns true if the value is a positive (possibly decimal) number. */
    public static boolean isUnsignedNumber(String value) {
        return value != null && UNSIGNED_NUMBER.matcher(value.trim()).matches();
    }

    /**
     * Parses a number that may be negative or decimal, rounding it to the nearest integer.
     * Never throws: returns defaultValue when the text is not a number. The former code
     * called Long.valueOf() on values accepted by a decimal regex, which crashed with a
     * NumberFormatException on values like "12.5".
     */
    public static long parseLongSafe(String value, long defaultValue) {
        if (!isSignedNumber(value))
            return defaultValue;
        try {
            return Math.round(Double.parseDouble(value.trim()));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /** Same as parseLongSafe but for int values. */
    public static int parseIntSafe(String value, int defaultValue) {
        return (int) parseLongSafe(value, defaultValue);
    }

    /** Parses a double value; returns defaultValue when the text is not a number. */
    public static double parseDoubleSafe(String value, double defaultValue) {
        if (!isSignedNumber(value))
            return defaultValue;
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Sends a value to the registered screen handler, or "0" when it is not a valid number.
     */
    private void sendNumber(Handler handler, int what, String value, boolean allowNegative) {
        boolean valid = allowNegative ? isSignedNumber(value) : isUnsignedNumber(value);
        handler.obtainMessage(what, valid ? value.trim() : "0").sendToTarget();
    }

    public String ReadResult(long timeout) {
        return ReadResult(timeout, null);
    }

    /**
     * Reads and decodes protocol sentences ("$name,field1,...,checksum;") until a
     * terminating sentence (end / OK) is received, the timeout expires, setExit(true) is
     * called or the optional stop condition becomes true.
     *
     * @param timeout maximum reading time in milliseconds
     * @param stop    optional extra stop condition (may be null)
     * @return a summary of the sentences received, e.g. "start teststandconfig end" or "OK"
     */
    public String ReadResult(long timeout, StopCondition stop) {
        this.exit = false;
        StringBuilder rawData = new StringBuilder();
        String myMessage = "";
        lastReceived = System.currentTimeMillis();
        long deadline = lastReceived + timeout;
        try {
            while (!this.exit) {
                int c = readByte(deadline, stop);
                if (c < 0)
                    break; // timeout, cancel, or the connection was closed
                char ch = (char) c;
                if (rawData.length() < MAX_SENTENCE_LENGTH)
                    rawData.append(ch);
                if (ch != '$')
                    continue;

                // Read the entire sentence until the ';' terminator
                StringBuilder tempBuff = new StringBuilder();
                boolean complete = false;
                while (true) {
                    c = readByte(deadline, stop);
                    if (c < 0)
                        break;
                    ch = (char) c;
                    if (ch == ';') {
                        complete = true;
                        break;
                    }
                    if (ch == '$') {
                        // A new sentence starts: the previous one was truncated (lost bytes)
                        tempBuff.setLength(0);
                        continue;
                    }
                    if (ch != '\r' && ch != '\n')
                        tempBuff.append(ch);
                    if (tempBuff.length() > MAX_SENTENCE_LENGTH)
                        break; // line noise, give up on this sentence
                }
                if (c < 0)
                    break;
                if (!complete || tempBuff.length() == 0)
                    continue; // empty "$;" or garbage: ignore it instead of crashing

                String[] currentSentence = tempBuff.toString().split(",");
                if (currentSentence.length == 0)
                    continue;
                myMessage = processSentence(currentSentence, myMessage);
            }
        } catch (IOException e) {
            myMessage = myMessage + " " + "error:" + e.getMessage();
        } finally {
            lastData = rawData.toString();
        }
        return myMessage;
    }

    /**
     * Decodes one sentence and updates the application state.
     *
     * @return the updated summary message
     */
    private String processSentence(String[] currentSentence, String myMessage) {
        Handler handler = mHandler;
        String boardName = getBoardName();

        switch (currentSentence[0]) {
            case "calibration":
                if (isChecksumValid(currentSentence) && handler != null) {
                    // Value 1 contains the offset
                    if (currentSentence.length > 1)
                        sendNumber(handler, 1, currentSentence[1], true);
                    // Value 2 contains the calibration factor
                    if (currentSentence.length > 2)
                        sendNumber(handler, 2, currentSentence[2], true);
                    // Value 3 contains the flag ("Done" when the calibration is finished)
                    if (currentSentence.length > 3)
                        handler.obtainMessage(3, currentSentence[3]).sendToTarget();
                }
                // This break was missing: the code fell through into "telemetry" and sent
                // the calibration values a second time with another meaning.
                break;

            case "telemetry":
                if (isChecksumValid(currentSentence) && handler != null) {
                    // Value 1 contains the current thrust (can be negative around zero)
                    if (currentSentence.length > 1)
                        sendNumber(handler, 1, currentSentence[1], true);
                    // Value 2 contains the time
                    if (currentSentence.length > 2)
                        sendNumber(handler, 2, currentSentence[2], false);
                    // Value 3 contains the battery voltage
                    if (currentSentence.length > 3)
                        sendNumber(handler, 3, currentSentence[3], false);
                    // Value 4 contains the eeprom usage
                    if (currentSentence.length > 4)
                        sendNumber(handler, 4, currentSentence[4], false);
                    // Value 5 contains the number of thrust curves
                    if (currentSentence.length > 5)
                        sendNumber(handler, 5, currentSentence[5], false);
                    else
                        Log.d(TAG, "Length: " + currentSentence.length);
                    // Value 6 contains the current pressure
                    if (currentSentence.length > 6)
                        sendNumber(handler, 6, currentSentence[6], true);
                    // Value 7 contains the current pressure from the second sensor
                    if (BoardCapabilities.hasPressure2(boardName) && currentSentence.length > 7)
                        sendNumber(handler, 7, currentSentence[7], true);
                }
                break;

            case "data":
                if (isChecksumValid(currentSentence)) {
                    addThrustCurveSample(currentSentence, boardName);
                }
                break;

            case "teststandconfig":
                if (isChecksumValid(currentSentence)) {
                    parseTestStandConfig(currentSentence);
                    myMessage = myMessage + " " + "teststandconfig";
                } else {
                    myMessage = myMessage + "KO" + "teststandconfig";
                }
                break;

            case "testTrame":
                testTrame.setTrameStatus(isChecksumValid(currentSentence));
                if (currentSentence.length > 1)
                    testTrame.setCurrentTrame(currentSentence[1]);
                else
                    testTrame.setCurrentTrame("Error reading packet");
                myMessage = myMessage + " " + "testTrame";
                break;

            case "nbrOfThrustCurve":
                // Value 1 contains the number of thrust curves
                NbrOfThrustCurves = 0;
                if (currentSentence.length > 1 && isUnsignedNumber(currentSentence[1]))
                    NbrOfThrustCurves = parseIntSafe(currentSentence[1], 0);
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
            case "UNKNOWN":
                setDataReady(true);
                commandRet = currentSentence[0];
                break;

            default:
                break;
        }
        return myMessage;
    }

    /**
     * Adds one recorded sample ("data" sentence) to the downloaded thrust curves.
     * Fields: 1 = curve number, 2 = time (ms), 3 = thrust, 4 = pressure,
     * 5 = pressure 2, 6 = filtered thrust. Negative values are kept: the former code
     * replaced them by 0, which hid the load cell zero drift and biased the impulse.
     */
    private void addThrustCurveSample(String[] currentSentence, String boardName) {
        if (currentSentence.length < 4 || !isUnsignedNumber(currentSentence[1]))
            return; // no curve number: the sample cannot be stored

        currentThrustCurveNbr = parseIntSafe(currentSentence[1], 0) + 1;
        String prefix = getResources().getString(R.string.thrustcurve_name) + " ";
        String thrustCurveName = (currentThrustCurveNbr < 10)
                ? prefix + "0" + currentThrustCurveNbr
                : prefix + currentThrustCurveNbr;

        long time = parseLongSafe(currentSentence[2], 0);

        // Thrust
        MyThrustCurve.AddToThrustCurve(time, parseLongSafe(currentSentence[3], 0),
                thrustCurveName, BoardCapabilities.SERIES_THRUST);

        // Casing pressure
        if (BoardCapabilities.hasPressure(boardName) && currentSentence.length > 4) {
            MyThrustCurve.AddToThrustCurve(time, parseLongSafe(currentSentence[4], 0),
                    thrustCurveName, BoardCapabilities.SERIES_PRESSURE);
        }

        if (BoardCapabilities.hasPressure2(boardName)) {
            // Second casing pressure
            if (currentSentence.length > 5) {
                MyThrustCurve.AddToThrustCurve(time, parseLongSafe(currentSentence[5], 0),
                        thrustCurveName, BoardCapabilities.SERIES_PRESSURE2);
            }
            // Filtered thrust
            if (currentSentence.length > 6) {
                MyThrustCurve.AddToThrustCurve(time, parseLongSafe(currentSentence[6], 0),
                        thrustCurveName, BoardCapabilities.SERIES_FILTERED_THRUST);
            }
        }
    }

    /**
     * Decodes the "teststandconfig" sentence into the TestStandConfigData object.
     */
    private void parseTestStandConfig(String[] s) {
        // Value 1 contains the units
        if (s.length > 1)
            TestStandCfg.setUnits(parseIntSafe(s[1], 0));
        // Value 2 contains the test stand name
        if (s.length > 2)
            TestStandCfg.setTestStandName(s[2]);
        String name = (s.length > 2) ? s[2] : TestStandCfg.getTestStandName();
        // Value 3 contains the test stand major version
        if (s.length > 3)
            TestStandCfg.setTestStandMajorVersion(parseIntSafe(s[3], 0));
        // Value 4 contains the test stand minor version
        if (s.length > 4)
            TestStandCfg.setTestStandMinorVersion(parseIntSafe(s[4], 0));
        // Value 5 contains the connection speed
        if (s.length > 5)
            TestStandCfg.setConnectionSpeed(parseLongSafe(s[5], 38400));
        // Value 6 contains the start recording thrust level (not used by the application)
        // Value 7 contains the stop recording time
        if (s.length > 7)
            TestStandCfg.setStopRecordingTime(parseIntSafe(s[7], 0));
        // Value 8 contains the test stand resolution
        if (s.length > 8)
            TestStandCfg.setTestStandResolution(parseIntSafe(s[8], 0));
        // Value 9 contains the eeprom size
        if (s.length > 9)
            TestStandCfg.setEepromSize(parseIntSafe(s[9], 512));
        // Value 10 contains the battery type
        if (s.length > 10)
            TestStandCfg.setBatteryType(parseIntSafe(s[10], 0));
        // Value 11 contains the calibration factor (can be negative)
        if (s.length > 11)
            TestStandCfg.setCalibrationFactor(parseLongSafe(s[11], 0));
        // Value 12 contains the current offset (can be negative)
        if (s.length > 12)
            TestStandCfg.setCurrentOffset(parseLongSafe(s[12], 0));
        // Value 13 contains the pressure sensor type
        if (s.length > 13)
            TestStandCfg.setPressureSensorType(parseIntSafe(s[13], 0));
        // Value 14 contains the telemetry module type
        if (s.length > 14)
            TestStandCfg.setTelemetryType(parseIntSafe(s[14], 0));
        // Value 15 contains the second pressure sensor type
        if (BoardCapabilities.hasPressure2(name) && s.length > 15)
            TestStandCfg.setPressureSensorType2(parseIntSafe(s[15], 0));
        // Bluetooth module name (position depends on the board)
        int btIndex = BoardCapabilities.bluetoothNameFieldIndex(name);
        if (btIndex > 0 && s.length > btIndex)
            TestStandCfg.setBluetoothName(s[btIndex]);
    }

    public void setDataReady(boolean value) {
        DataReady = value;
    }

    public boolean getDataReady() {
        return DataReady;
    }

    public GlobalConfig getAppConf() {
        return AppConf;
    }

    public void setAppConf(GlobalConfig value) {
        AppConf = value;
    }

    /**
     * Last test frame received from the board by the connection test screen.
     */
    public class TestTrame {

        private volatile String currentTrame = "";
        private volatile boolean trameStatus = false;

        public void setCurrentTrame(String trame) {
            currentTrame = trame;
        }

        public String getCurrentTrame() {
            return currentTrame;
        }

        public void setTrameStatus(boolean val) {
            trameStatus = val;
        }

        public boolean getTrameStatus() {
            return trameStatus;
        }
    }
}
