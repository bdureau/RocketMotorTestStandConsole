package com.rocketmotorteststand.Config;

/**
 *
 *   @description: Test stand configuration class that manages all the data
 *
 *   @author: boris.dureau@neuf.fr
 *
 **/
public class TestStandConfigData {

    //Test Stand config variable
    private int units = 0;

    private String TestStandName = "TestStandToto"; //default stupid name

    private int minorVersion =0;
    private int majorVersion =0;

    private long connectionSpeed= 38400;
    private int testStandResolution = 0;
    private int eepromSize= 512;
    private int stopRecordingTime =10; //10s

    private int batteryType =0;
    private long calibrationFactor = 0;
    private long currentOffset=0;
    private int pressureSensorType = 0;
    private int pressureSensorType2 = 0;
    private int TelemetryType = 0;

    public TestStandConfigData()
    {

    }

    public void setUnits(int value)
    {
        units = value;
    }

    public int getUnits()
    {
        return units;
    }

    public int getTestStandMinorVersion()
    {
        return minorVersion;
    }
    public int getTestStandMajorVersion()
    {
        return majorVersion;
    }
    public void setTestStandMinorVersion(int value)
    {
        minorVersion=value;
    }
    public void setTestStandMajorVersion(int value)
    {
        majorVersion=value;
    }

    public void setTestStandName(String value)
    {
        TestStandName = value;
    }

    public String getTestStandName()
    {
        return TestStandName;
    }

    //Number of second during which we are recording
    public void setStopRecordingTime(int value) {stopRecordingTime =value;}
    public int getStopRecordingTime(){return stopRecordingTime;}

    //test stand baud rate
    public void setConnectionSpeed(long value) {connectionSpeed = value;}
    public long getConnectionSpeed() {return connectionSpeed;}

    //Sensor resolution
    public void setTestStandResolution(int value) {testStandResolution = value;}
    public int getTestStandResolution() {return testStandResolution;}

    //eeprom size
    public void setEepromSize(int value) {eepromSize = value;}
    public int getEepromSize() {return eepromSize;}

    // TelemetryType
    public void setTelemetryType(int value){TelemetryType=value;}
    public int getTelemetryType(){return TelemetryType;}

    //index in an array
    public int arrayIndex (String stringArray[], String pattern) {

        for (int i =0; i < stringArray.length ; i++) {
            if(stringArray[i].equals(pattern))
                return i;
        }
        return -1;
    }

    public void setBatteryType(int value) {batteryType =value;}
    public int getBatteryType(){return batteryType;}

    public void setCalibrationFactor(long value) {calibrationFactor =value;}
    public long getCalibrationFactor(){return calibrationFactor;}

    //currentOffset
    public void setCurrentOffset(long value) {currentOffset =value;}
    public long getCurrentOffset(){return currentOffset;}

    //pressureSensorType
    public void setPressureSensorType(int value) {pressureSensorType =value;}
    public int getPressureSensorType(){return pressureSensorType;}

    public void setPressureSensorType2(int value) {pressureSensorType2 =value;}
    public int getPressureSensorType2(){return pressureSensorType2;}
}
