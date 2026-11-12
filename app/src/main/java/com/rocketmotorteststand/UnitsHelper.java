package com.rocketmotorteststand;

import android.content.Context;

/**
 * @description: Unit conversions shared by every screen (telemetry, status, thrust curve charts,
 * curve information and file exports).
 * The board always sends thrust in grams (force) and casing pressure in PSI. Each screen used
 * to have its own copy of the conversion code and several copies compared the pressure unit
 * with the thrust unit setting by mistake; everything now goes through this class.
 * @author: boris.dureau@neuf.fr
 **/
public final class UnitsHelper {

    /** Standard gravity: converts kilogram-force to newtons. */
    public static final double STANDARD_GRAVITY = 9.80665;
    /** Converts kilograms to pounds. */
    public static final double KG_TO_POUNDS = 2.20462;
    /** Converts PSI to bar. */
    public static final double PSI_TO_BAR = 0.0689476;
    /** Converts PSI to kilopascal. */
    public static final double PSI_TO_KPA = 6.89476;

    private UnitsHelper() {
        // Utility class: no instances
    }

    /**
     * Factor converting a raw thrust value (grams) into the thrust unit chosen in the settings.
     */
    public static double thrustFactor(GlobalConfig conf) {
        int units = conf.getUnits();
        if (units == GlobalConfig.ThrustUnits.POUNDS)
            return KG_TO_POUNDS / 1000.0;
        if (units == GlobalConfig.ThrustUnits.NEWTONS)
            return STANDARD_GRAVITY / 1000.0;
        return 1.0 / 1000.0; // kg
    }

    /**
     * Name of the thrust unit chosen in the settings (without parentheses).
     */
    public static String thrustLabel(Context ctx, GlobalConfig conf) {
        int units = conf.getUnits();
        if (units == GlobalConfig.ThrustUnits.POUNDS)
            return ctx.getString(R.string.Pounds_fview);
        if (units == GlobalConfig.ThrustUnits.NEWTONS)
            return ctx.getString(R.string.Newtons_fview);
        return ctx.getString(R.string.Kg_fview);
    }

    /**
     * Factor converting a raw pressure value (PSI) into the pressure unit chosen in the settings.
     */
    public static double pressureFactor(GlobalConfig conf) {
        int units = conf.getUnitsPressure();
        if (units == GlobalConfig.PressureUnits.BAR)
            return PSI_TO_BAR;
        if (units == GlobalConfig.PressureUnits.KPascal)
            return PSI_TO_KPA;
        return 1.0; // PSI
    }

    /**
     * Name of the pressure unit chosen in the settings (without parentheses).
     */
    public static String pressureLabel(Context ctx, GlobalConfig conf) {
        int units = conf.getUnitsPressure();
        if (units == GlobalConfig.PressureUnits.BAR)
            return ctx.getString(R.string.pressure_unit_bar);
        if (units == GlobalConfig.PressureUnits.KPascal)
            return ctx.getString(R.string.pressure_unit_kpascal);
        return ctx.getString(R.string.pressure_unit_psi);
    }

    /**
     * Builds the array of unit labels matching the series of a thrust curve:
     * thrust, pressure, pressure 2, filtered thrust.
     */
    public static String[] seriesUnits(Context ctx, GlobalConfig conf) {
        String thrust = thrustLabel(ctx, conf);
        String pressure = pressureLabel(ctx, conf);
        return new String[]{thrust, pressure, pressure, thrust};
    }

    /**
     * Returns true if the series index holds a thrust value (raw grams), false if it holds a
     * pressure value (raw PSI).
     */
    public static boolean isThrustSeries(int seriesIndex) {
        return seriesIndex == BoardCapabilities.SERIES_THRUST
                || seriesIndex == BoardCapabilities.SERIES_FILTERED_THRUST;
    }

    /**
     * Factor converting the raw values of the given series into the units chosen in the settings.
     */
    public static double seriesFactor(GlobalConfig conf, int seriesIndex) {
        return isThrustSeries(seriesIndex) ? thrustFactor(conf) : pressureFactor(conf);
    }
}
