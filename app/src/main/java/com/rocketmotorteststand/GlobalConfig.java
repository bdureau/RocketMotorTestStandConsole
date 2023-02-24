package com.rocketmotorteststand;


import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

import com.rocketmotorteststand.config.AppConfigData;

public class GlobalConfig {
    Context context;
    /*public enum ThrustUnits {KG, POUNDS, NEWTONS}
    public enum PressureUnits {PSI,BAR,KPascal}*/

    SharedPreferences appConfig = null;
    SharedPreferences.Editor edit = null;
    AppConfigData appCfgData = null;
    //application language
    private int applicationLanguage = 0;
    //Graph units
    private int units = 0;
    private int unitsPressure = 0;

    //graph background color
    private int graphBackColor = 1;
    //graph color
    private int graphColor = 0;
    //graph font size
    private int fontSize = 10;
    // connection type is bluetooth
    private int connectionType = 0;
    // default baud rate for USB is 38400
    private int baudRate = 8;
    private int graphicsLibType = 0;

    private boolean fullUSBSupport= false;


    public GlobalConfig(Context current) {
        context = current;
        appConfig = context.getSharedPreferences("TestStandConsoleCfg", MODE_PRIVATE);
        edit = appConfig.edit();
        context = current;
        appCfgData = new AppConfigData(context);

    }

    public void ResetDefaultConfig() {
        applicationLanguage = 0; // default to phone language
        graphBackColor = 1;
        graphColor = 0;
        fontSize = 10;
        units = 0; //default to kg
        unitsPressure = 0; //default to PSI
        baudRate = 8; // default to 38400 baud
        connectionType = 0;
        graphicsLibType = 1; //Default to MP android chart lib
        fullUSBSupport = false;
    }

    public void ReadConfig() {
        try {
            //Application language
            //String applicationLanguage;
            int applicationLanguage = appConfig.getInt("AppLanguage", 0);
            //if (!applicationLanguage.equals(""))
            setApplicationLanguage(applicationLanguage);

            //Application Units
            //String appUnit;
            int appUnit = appConfig.getInt("Units", 0);
            //if (!appUnit.equals(""))
            setUnits(appUnit);

            //String appUnitPressure;
            int appUnitPressure = appConfig.getInt("UnitsPressure", 0);
            //if (!appUnitPressure.equals(""))
            setUnitsPressure(appUnitPressure);

            //Graph color
            //String graphColor;
            int graphColor = appConfig.getInt("GraphColor", 0);
            //if (!graphColor.equals(""))
            setGraphColor(graphColor);

            //Graph Background color
            //String graphBackColor;
            graphBackColor = appConfig.getInt("GraphBackColor", 0);
            //if (!graphBackColor.equals(""))
            setGraphBackColor(graphBackColor);

            //Font size
            //String fontSize;
            int fontSize = appConfig.getInt("FontSize", 0);
            //if (!fontSize.equals(""))
            setFontSize(fontSize);

            //Baud rate
            //String baudRate;
            int baudRate = appConfig.getInt("BaudRate", 0);
            //if (!baudRate.equals(""))
            setBaudRate(baudRate);

            //Connection type
            //String connectionType;
            int connectionType = appConfig.getInt("ConnectionType", 0);
            //if (!connectionType.equals(""))
            setConnectionType(connectionType);

            //Graphics Lib Type
            //String graphicsLibType;
            graphicsLibType = appConfig.getInt("GraphicsLibType", 1);
            //if (!graphicsLibType.equals(""))
            setGraphicsLibType(graphicsLibType);


            //enable full USB support
            //String fullUSBSupport;
            boolean fullUSBSupport = appConfig.getBoolean("fullUSBSupport", false);
            //if (!fullUSBSupport.equals(""))
            setFullUSBSupport(fullUSBSupport);

        } catch (Exception e) {

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
    public static class ThrustUnits
    {
        public static int KG = 0;
        public static int POUNDS= 1;
        public static int NEWTONS = 2;
    }
    public static class PressureUnits
    {
        public static int PSI = 0;
        public static int BAR = 1;
        public static int KPascal = 2;
    }

    public static class ConnectionType
    {
        public static int BT =0;
        public static int USB =1;
    }
}