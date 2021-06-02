package com.rocketmotorteststand.config;

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
    private int stopRecordingThrustLevel =0;

    private int startRecordingThrustLevel =1;
    private int batteryType =0;
    private long calibrationFactor = 0;
    private long currentOffset=0;

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



    //Minimum recording thrust
    public void setStopRecordingThrustLevel(int value) {stopRecordingThrustLevel =value;}
    public int getStopRecordingThrustLevel(){return stopRecordingThrustLevel;}


    //test stand baud rate
    public void setConnectionSpeed(long value) {connectionSpeed = value;}
    public long getConnectionSpeed() {return connectionSpeed;}

    //Sensor resolution
    public void setTestStandResolution(int value) {testStandResolution = value;}
    public int getTestStandResolution() {return testStandResolution;}

    //eeprom size
    public void setEepromSize(int value) {eepromSize = value;}
    public int getEepromSize() {return eepromSize;}



    //index in an array
    public int arrayIndex (String stringArray[], String pattern) {

        for (int i =0; i < stringArray.length ; i++) {
            if(stringArray[i].equals(pattern))
                return i;
        }
        return -1;
    }




    //start recoding thrust
    public void setStartRecordingThrustLevel(int value) {startRecordingThrustLevel =value;}
    public int getStartRecordingThrustLevel(){return startRecordingThrustLevel;}

    public void setBatteryType(int value) {batteryType =value;}
    public int getBatteryType(){return batteryType;}

    public void setCalibrationFactor(long value) {calibrationFactor =value;}
    public long getCalibrationFactor(){return calibrationFactor;}

    //currentOffset
    public void setCurrentOffset(long value) {currentOffset =value;}
    public long getCurrentOffset(){return currentOffset;}
}
