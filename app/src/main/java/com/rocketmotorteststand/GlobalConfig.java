package com.rocketmotorteststand;


import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

import com.rocketmotorteststand.config.AppConfigData;

/**
 * @description: Application settings (language, units, graph colours and font, connection type,
 * USB baud rate, chart library...) stored in the shared preferences.
 * The default values are defined once (DEFAULT_* constants) and used by the field
 * initialisation, by ResetDefaultConfig() and by ReadConfig(): they used to differ, so a
 * fresh installation started with a font size of 0, a 300 baud USB speed and a black graph
 * background instead of the documented defaults.
 * @author: boris.dureau@neuf.fr
 **/
public class GlobalConfig {
    // Default values of the settings
    private static final int DEFAULT_LANGUAGE = 0;         // phone language
    private static final int DEFAULT_UNITS = 0;            // kg
    private static final int DEFAULT_UNITS_PRESSURE = 0;   // PSI
    private static final int DEFAULT_GRAPH_BACK_COLOR = 1; // white
    private static final int DEFAULT_GRAPH_COLOR = 0;      // black
    private static final int DEFAULT_FONT_SIZE = 10;
    private static final int DEFAULT_BAUD_RATE = 8;        // index of 38400 baud
    private static final int DEFAULT_CONNECTION_TYPE = 0;  // bluetooth
    private static final int DEFAULT_GRAPHICS_LIB = 1;     // MPAndroidChart
    private static final boolean DEFAULT_FULL_USB_SUPPORT = false;
    private static final boolean DEFAULT_MANUAL_RECORDING = true;

    Context context;

    SharedPreferences appConfig = null;
    SharedPreferences.Editor edit = null;
    AppConfigData appCfgData = null;
    //application language
    private int applicationLanguage = DEFAULT_LANGUAGE;
    //Graph units
    private int units = DEFAULT_UNITS;
    private int unitsPressure = DEFAULT_UNITS_PRESSURE;

    //graph background color
    private int graphBackColor = DEFAULT_GRAPH_BACK_COLOR;
    //graph color
    private int graphColor = DEFAULT_GRAPH_COLOR;
    //graph font size
    private int fontSize = DEFAULT_FONT_SIZE;
    // connection type is bluetooth
    private int connectionType = DEFAULT_CONNECTION_TYPE;
    // default baud rate for USB is 38400
    private int baudRate = DEFAULT_BAUD_RATE;
    private int graphicsLibType = DEFAULT_GRAPHICS_LIB;

    private boolean fullUSBSupport = DEFAULT_FULL_USB_SUPPORT;
    private boolean allowManualRecording = DEFAULT_MANUAL_RECORDING;


    public GlobalConfig(Context current) {
        context = current;
        appConfig = context.getSharedPreferences(ConsoleApplication.PREFS_NAME, MODE_PRIVATE);
        edit = appConfig.edit();
        context = current;
        appCfgData = new AppConfigData(context);

    }

    public void ResetDefaultConfig() {
        applicationLanguage = DEFAULT_LANGUAGE;
        graphBackColor = DEFAULT_GRAPH_BACK_COLOR;
        graphColor = DEFAULT_GRAPH_COLOR;
        fontSize = DEFAULT_FONT_SIZE;
        units = DEFAULT_UNITS;
        unitsPressure = DEFAULT_UNITS_PRESSURE;
        baudRate = DEFAULT_BAUD_RATE;
        connectionType = DEFAULT_CONNECTION_TYPE;
        graphicsLibType = DEFAULT_GRAPHICS_LIB;
        fullUSBSupport = DEFAULT_FULL_USB_SUPPORT;
        allowManualRecording = DEFAULT_MANUAL_RECORDING;
    }

    /**
     * Reads the settings from the shared preferences. A setting that was never saved gets
     * its default value (the same as ResetDefaultConfig()).
     */
    public void ReadConfig() {
        try {
            setApplicationLanguage(appConfig.getInt("AppLanguage", DEFAULT_LANGUAGE));
            setUnits(appConfig.getInt("Units", DEFAULT_UNITS));
            setUnitsPressure(appConfig.getInt("UnitsPressure", DEFAULT_UNITS_PRESSURE));
            setGraphColor(appConfig.getInt("GraphColor", DEFAULT_GRAPH_COLOR));
            setGraphBackColor(appConfig.getInt("GraphBackColor", DEFAULT_GRAPH_BACK_COLOR));
            setFontSize(appConfig.getInt("FontSize", DEFAULT_FONT_SIZE));
            setBaudRate(appConfig.getInt("BaudRate", DEFAULT_BAUD_RATE));
            setConnectionType(appConfig.getInt("ConnectionType", DEFAULT_CONNECTION_TYPE));
            setGraphicsLibType(appConfig.getInt("GraphicsLibType", DEFAULT_GRAPHICS_LIB));
            setFullUSBSupport(appConfig.getBoolean("fullUSBSupport", DEFAULT_FULL_USB_SUPPORT));
            setManualRecording(appConfig.getBoolean("allowManualRecording", DEFAULT_MANUAL_RECORDING));
        } catch (ClassCastException e) {
            // A preference was stored with another type by an old version: keep the defaults
        }
    }

    public void SaveConfig() {
        edit.putInt("AppLanguage", getApplicationLanguage());
        edit.putInt("Units", getUnits());
        edit.putInt("UnitsPressure", getUnitsPressure());
        edit.putInt("GraphColor", getGraphColor());
        edit.putInt("GraphBackColor", getGraphBackColor());
        edit.putInt("FontSize", getFontSize());
        edit.putInt("BaudRate", getBaudRate());
        edit.putInt("ConnectionType", getConnectionType());
        edit.putInt("GraphicsLibType", getGraphicsLibType());
        edit.putBoolean("fullUSBSupport", getFullUSBSupport());
        edit.putBoolean("allowManualRecording", getManualRecording());
        edit.commit();

    }

    public int getFontSize() {
        return fontSize;
    }

    public void setFontSize(int value) {
        fontSize = value;
    }

    public int getApplicationLanguage() {
        return applicationLanguage;
    }

    public void setApplicationLanguage(int value) {
        applicationLanguage = value;
    }

    //return the unit id
    public int getUnits() {
        return units;
    }

    public String getUnitsValue() {
        return appCfgData.getUnitsByNbr(units);
    }

    //set the unit by id
    public void setUnits(int value) {
        units = value;
    }

    // get pressure units
    public int getUnitsPressure() { return unitsPressure; }

    public String getUnitsPressureValue() { return appCfgData.getUnitsPressureByNbr(unitsPressure); }
    public void setUnitsPressure(int value) {
        unitsPressure = value;
    }

    public int getGraphColor() {
        return graphColor;
    }

    public void setGraphColor(int value) {
        graphColor = value;
    }

    public int getGraphBackColor() {
        return graphBackColor;
    }

    public void setGraphBackColor(int value) {
        graphBackColor = value;
    }

    //get the id of the current connection type
    public int getConnectionType() {
        return connectionType;
    }

    //get the name of the current connection type
    public String getConnectionTypeValue() {
        return appCfgData.getConnectionTypeByNbr(connectionType);
    }

    public void setConnectionType(int value) {
        connectionType = value;
    }

    public int getGraphicsLibType() {
        return graphicsLibType;
    }

    public String getGraphicsLibTypeValue() {
        return appCfgData.getGraphicsLibTypeByNbr(graphicsLibType);
    }

    public void setGraphicsLibType(int value) {
        graphicsLibType = value;
    }

    public int getBaudRate() {
        return baudRate;
    }

    public String getBaudRateValue() {
        return appCfgData.getBaudRateByNbr(baudRate);
    }

    public void setBaudRate(int value) {
        baudRate = value;
    }


    public void setFullUSBSupport(boolean value) {
        fullUSBSupport = value;
    }
    public boolean getFullUSBSupport() {
        return fullUSBSupport;
    }

    public void setManualRecording(boolean value) {
        allowManualRecording = value;
    }

    public boolean getManualRecording() {
        return allowManualRecording;
    }

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
    // Setting values. "final": they are constants and must not be modified at run time.
    public static class ThrustUnits
    {
        public static final int KG = 0;
        public static final int POUNDS = 1;
        public static final int NEWTONS = 2;
    }
    public static class PressureUnits
    {
        public static final int PSI = 0;
        public static final int BAR = 1;
        public static final int KPascal = 2;
    }

    public static class ConnectionType
    {
        public static final int BT = 0;
        public static final int USB = 1;
    }
}