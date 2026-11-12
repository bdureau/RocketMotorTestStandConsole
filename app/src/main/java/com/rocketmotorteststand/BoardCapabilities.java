package com.rocketmotorteststand;

/**
 * @description: Single source of truth describing what each test stand board model can measure
 * and configure. Every screen used to repeat long chains of
 * name.equals("TestStandESP32V3") || ... checks, and the TestStandESP32V4 board had been
 * forgotten in many of them. All those checks now go through this class, so adding a new
 * board only requires updating the lists below.
 * @author: boris.dureau@neuf.fr
 **/
public final class BoardCapabilities {

    /** Index of the thrust series inside a thrust curve XYSeriesCollection. */
    public static final int SERIES_THRUST = 0;
    /** Index of the first casing pressure series. */
    public static final int SERIES_PRESSURE = 1;
    /** Index of the second casing pressure series. */
    public static final int SERIES_PRESSURE2 = 2;
    /** Index of the filtered thrust series computed by the board firmware. */
    public static final int SERIES_FILTERED_THRUST = 3;

    private BoardCapabilities() {
        // Utility class: no instances
    }

    /**
     * Returns true when the board is one of the models supported by this application.
     */
    public static boolean isSupported(String name) {
        return "TestStand".equals(name)
                || "TestStandSTM32".equals(name)
                || "TestStandSTM32V2".equals(name)
                || "TestStandSTM32V3".equals(name)
                || "TestStandESP32".equals(name)
                || "TestStandESP32V3".equals(name)
                || "TestStandESP32V4".equals(name);
    }

    /**
     * Returns true when the board has (at least) one casing pressure sensor.
     */
    public static boolean hasPressure(String name) {
        return "TestStandSTM32V2".equals(name)
                || "TestStandESP32".equals(name)
                || hasPressure2(name);
    }

    /**
     * Returns true when the board has a second casing pressure sensor. These boards also send
     * a filtered thrust value in each recorded sample.
     */
    public static boolean hasPressure2(String name) {
        return "TestStandSTM32V3".equals(name)
                || "TestStandESP32V3".equals(name)
                || "TestStandESP32V4".equals(name);
    }

    /**
     * Returns true when the board sends a filtered thrust series with each recorded sample.
     */
    public static boolean hasFilteredThrust(String name) {
        return hasPressure2(name);
    }

    /**
     * Returns true when the board firmware stores a configurable Bluetooth module name.
     */
    public static boolean hasBluetoothName(String name) {
        return "TestStandESP32".equals(name)
                || "TestStandESP32V3".equals(name)
                || "TestStandESP32V4".equals(name);
    }

    /**
     * Returns the position of the Bluetooth name inside the "teststandconfig" sentence,
     * or -1 when the board does not send it.
     */
    public static int bluetoothNameFieldIndex(String name) {
        if ("TestStandESP32".equals(name))
            return 15;
        if ("TestStandESP32V3".equals(name) || "TestStandESP32V4".equals(name))
            return 16;
        return -1;
    }

    /**
     * Returns the number of data series recorded for one thrust curve:
     * 1 = thrust, 2 = thrust + pressure, 4 = thrust + pressure + pressure 2 + filtered thrust.
     */
    public static int seriesCount(String name) {
        if (hasPressure2(name))
            return 4;
        if (hasPressure(name))
            return 2;
        return 1;
    }
}
