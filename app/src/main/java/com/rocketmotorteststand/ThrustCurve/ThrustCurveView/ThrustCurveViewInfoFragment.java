package com.rocketmotorteststand.ThrustCurve.ThrustCurveView;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.rocketmotorteststand.BoardCapabilities;
import com.rocketmotorteststand.ConsoleApplication;
import com.rocketmotorteststand.FileExportHelper;
import com.rocketmotorteststand.R;
import com.rocketmotorteststand.ThrustCurve.ThrustCurveData;
import com.rocketmotorteststand.ThrustCurve.ThrustCurveViewTabActivity;
import com.rocketmotorteststand.UnitsHelper;

import org.afree.data.xy.XYSeries;
import org.afree.data.xy.XYSeriesCollection;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * @description: Information tab of a thrust curve. Analyses the curve (burn time, maximum and
 * average thrust, total impulse, motor class, maximum pressures) and exports it as CSV files,
 * as a RASP .eng motor file (OpenRocket, RockSim...) or as a zip of all the files.
 *
 * Physics: the burn is delimited by the points where the thrust crosses 5% of its maximum
 * (rising, then falling after the peak). The total impulse is the integral of the thrust over
 * that interval (trapezoidal rule) and the average thrust is impulse / burn time.
 * Raw data: time in ms, thrust in grams (force), pressure in PSI.
 * @author: boris.dureau@neuf.fr
 **/
public class ThrustCurveViewInfoFragment extends Fragment {

    /** Preferences remembering the last motor data typed in the .eng export dialog. */
    private static final String ENG_PREFS = "EngExportCfg";
    /** Threshold delimiting the burn, as a fraction of the maximum value. */
    private static final double BURN_THRESHOLD = 0.05;

    private ThrustCurveData mythrustCurve;
    private String ThrustCurveName = null;
    private ConsoleApplication myBT;

    private TextView nbrOfSamplesValue, thrustCurveNbrValue;
    private TextView recordingDurationValue, thrustTimeValue, maxThrustValue, averageThrustValue;
    private TextView motorClassValue, totalImpulseValue, maxPressureValue, maxPressureValue2;
    private TextView maxPressure, maxPressure2;
    private Button buttonExportToCsv, buttonExportToCsvFull, buttonExportToEng, buttonShareEng,
            butShareZip;

    private XYSeriesCollection lThrustCurveData;
    private BurnAnalysis analysis = new BurnAnalysis();
    private String thrustUnit = "", pressureUnit = "";
    private double CONVERT = 1.0, CONVERT_PRESSURE = 1.0;

    /**
     * Result of the analysis of the thrust series.
     */
    private static class BurnAnalysis {
        boolean valid = false;
        int start = -1, max = -1, stop = -1;  // sample indexes
        double maxThrustGrams = 0;
        double burnTimeS = 0;
        double impulseGramS = 0;               // integral of the thrust, gram.second
        double totalImpulseNs = 0;
        double averageThrustGrams = 0;
        double averageThrustN = 0;
        String motorClass = "";                 // e.g. "H120"
    }

    /**
     * Motor data typed by the user for the .eng file header.
     */
    private static class EngSpec {
        double diameterMm, lengthMm, propellantG, totalG;
        String delays, manufacturer;
    }

    /** Called once the user has validated the .eng export dialog. */
    private interface EngSpecCallback {
        void onSpec(EngSpec spec);
    }

    /**
     * Empty constructor required by Android to recreate the fragment. A recreated instance
     * is discarded by the host activity (see FragmentRecreationHelper).
     */
    public ThrustCurveViewInfoFragment() {
    }

    public ThrustCurveViewInfoFragment(ThrustCurveData data,
                                       ConsoleApplication pBT,
                                       String pUnits[],
                                       String pThrustCurveName) {
        mythrustCurve = data;
        myBT = pBT;
        ThrustCurveName = pThrustCurveName;
        // pUnits is no longer modified here: the units are computed from the settings
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_thrustcurve_info, container, false);

        buttonExportToCsv = (Button) view.findViewById(R.id.butExportToCsv);
        buttonShareEng = (Button) view.findViewById(R.id.butShareEng);
        buttonExportToCsvFull = (Button) view.findViewById(R.id.butExportToCsvFull);
        buttonExportToEng = (Button) view.findViewById(R.id.butExportToEng);
        butShareZip = (Button) view.findViewById(R.id.butShareZip);
        recordingDurationValue = view.findViewById(R.id.thrustCurveDurationValue);
        thrustTimeValue = view.findViewById(R.id.thrustTimeValue);
        maxThrustValue = view.findViewById(R.id.maxThrustValue);
        averageThrustValue = view.findViewById(R.id.averageThrustValue);
        nbrOfSamplesValue = view.findViewById(R.id.nbrOfSamplesValue);
        thrustCurveNbrValue = view.findViewById(R.id.thrustCurveNbrValue);
        motorClassValue = view.findViewById(R.id.motorClassValue);
        totalImpulseValue = view.findViewById(R.id.totalImpulseValue);
        maxPressureValue = view.findViewById(R.id.maxPressureValue);
        maxPressure = view.findViewById(R.id.maxPressure);
        maxPressureValue2 = view.findViewById(R.id.maxPressureValue2);
        maxPressure2 = view.findViewById(R.id.maxPressure2);

        lThrustCurveData = (mythrustCurve != null) ? mythrustCurve.GetThrustCurveData(ThrustCurveName) : null;
        if (lThrustCurveData == null || lThrustCurveData.getSeriesCount() == 0)
            return view;

        // Units chosen in the settings
        myBT.getAppConf().ReadConfig();
        thrustUnit = UnitsHelper.thrustLabel(getContext(), myBT.getAppConf());
        pressureUnit = UnitsHelper.pressureLabel(getContext(), myBT.getAppConf());
        CONVERT = UnitsHelper.thrustFactor(myBT.getAppConf());
        // The pressure unit used to be compared with the thrust unit setting by mistake
        CONVERT_PRESSURE = UnitsHelper.pressureFactor(myBT.getAppConf());

        // The pressure rows depend on the series actually recorded (the TestStandESP32V4
        // was missing from the former name lists, so its pressures were never shown)
        int visibility1 = hasPressure() ? View.VISIBLE : View.INVISIBLE;
        maxPressureValue.setVisibility(visibility1);
        maxPressure.setVisibility(visibility1);
        int visibility2 = hasPressure2() ? View.VISIBLE : View.INVISIBLE;
        maxPressureValue2.setVisibility(visibility2);
        maxPressure2.setVisibility(visibility2);

        XYSeries thrust = lThrustCurveData.getSeries(BoardCapabilities.SERIES_THRUST);
        thrustCurveNbrValue.setText(ThrustCurveName);
        nbrOfSamplesValue.setText(String.valueOf(thrust.getItemCount()));
        if (thrust.getItemCount() > 0) {
            double recordingDuration = thrust.getMaxX() / 1000;
            recordingDurationValue.setText(String.format(Locale.getDefault(), "%.3f secs", recordingDuration));
        }

        analysis = analyse(thrust);
        if (analysis.valid) {
            thrustTimeValue.setText(String.format(Locale.getDefault(), "%.2f secs", analysis.burnTimeS));
            maxThrustValue.setText(String.format(Locale.getDefault(), "%.1f %s",
                    analysis.maxThrustGrams * CONVERT, thrustUnit));
            averageThrustValue.setText(String.format(Locale.getDefault(), "%.1f %s",
                    analysis.averageThrustGrams * CONVERT, thrustUnit));
            // An impulse is in newton.seconds (it was labelled "Newtons")
            totalImpulseValue.setText(String.format(Locale.getDefault(), "%.1f N.s", analysis.totalImpulseNs));
            motorClassValue.setText(analysis.motorClass);
        } else {
            thrustTimeValue.setText("-");
            totalImpulseValue.setText("-");
            motorClassValue.setText("-");
            if (thrust.getItemCount() > 0)
                maxThrustValue.setText(String.format(Locale.getDefault(), "%.1f %s",
                        thrust.getMaxY() * CONVERT, thrustUnit));
        }

        // Maximum pressures, in the unit chosen in the settings (it was always shown in PSI)
        if (hasPressure()) {
            XYSeries p1 = lThrustCurveData.getSeries(BoardCapabilities.SERIES_PRESSURE);
            if (p1.getItemCount() > 0)
                maxPressureValue.setText(String.format(Locale.getDefault(), "%.1f %s",
                        p1.getMaxY() * CONVERT_PRESSURE, pressureUnit));
        }
        if (hasPressure2()) {
            XYSeries p2 = lThrustCurveData.getSeries(BoardCapabilities.SERIES_PRESSURE2);
            if (p2.getItemCount() > 0)
                maxPressureValue2.setText(String.format(Locale.getDefault(), "%.1f %s",
                        p2.getMaxY() * CONVERT_PRESSURE, pressureUnit));
        }

        buttonExportToCsv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveFiles(buildBurnCsvFiles(newFileBaseName()));
            }
        });

        buttonExportToCsvFull.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveFiles(buildFullCsvFiles(newFileBaseName()));
            }
        });

        buttonExportToEng.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exportEng();
            }
        });

        buttonShareEng.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareEng();
            }
        });

        butShareZip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareZip();
            }
        });
        return view;
    }

    private boolean hasPressure() {
        return lThrustCurveData != null
                && lThrustCurveData.getSeriesCount() > BoardCapabilities.SERIES_PRESSURE;
    }

    private boolean hasPressure2() {
        return lThrustCurveData != null
                && lThrustCurveData.getSeriesCount() > BoardCapabilities.SERIES_PRESSURE2;
    }

    // ---------------------------------------------------------------------------------------
    // Analysis
    // ---------------------------------------------------------------------------------------

    /**
     * Finds the burn (5% of the maximum thrust rising, then falling after the peak) and
     * computes burn time, total impulse (trapezoidal integration), average thrust and class.
     */
    private BurnAnalysis analyse(XYSeries thrust) {
        BurnAnalysis a = new BurnAnalysis();
        if (thrust.getItemCount() < 2)
            return a;
        ThrustCurveViewTabActivity.ThrustUtil tu = new ThrustCurveViewTabActivity.ThrustUtil();
        a.maxThrustGrams = thrust.getMaxY();
        double trigger = a.maxThrustGrams * BURN_THRESHOLD;
        a.start = tu.searchX(thrust, trigger);
        a.max = tu.searchX(thrust, a.maxThrustGrams);
        a.stop = tu.searchXFrom(thrust, a.max, trigger);
        if (a.start == -1 || a.max == -1 || a.stop == -1 || a.stop <= a.start || a.maxThrustGrams <= 0)
            return a;

        a.burnTimeS = (thrust.getX(a.stop).doubleValue() - thrust.getX(a.start).doubleValue()) / 1000.0;
        if (a.burnTimeS <= 0)
            return a;
        a.impulseGramS = tu.integrate(thrust, a.start, a.stop);
        a.totalImpulseNs = a.impulseGramS / 1000.0 * UnitsHelper.STANDARD_GRAVITY;
        a.averageThrustGrams = a.impulseGramS / a.burnTimeS;
        a.averageThrustN = a.totalImpulseNs / a.burnTimeS;
        // Motor designation: class letter + average thrust in newtons, e.g. "H120"
        a.motorClass = tu.motorClass(a.totalImpulseNs) + Math.round(a.averageThrustN);
        a.valid = true;
        return a;
    }

    /**
     * Returns the [start, stop] sample indexes of the pressure rise of a pressure series,
     * using min + 5% of (max - min) as threshold, or null if not found.
     */
    private int[] pressureWindow(XYSeries pressure) {
        if (pressure.getItemCount() < 2)
            return null;
        ThrustCurveViewTabActivity.ThrustUtil tu = new ThrustCurveViewTabActivity.ThrustUtil();
        double max = pressure.getMaxY();
        double min = pressure.getMinY();
        // The former formula (max x 5% + min) did not scale with the pressure range
        double trigger = min + (max - min) * BURN_THRESHOLD;
        int start = tu.searchX(pressure, trigger);
        int peak = tu.searchX(pressure, max);
        int stop = tu.searchXFrom(pressure, peak, trigger);
        if (start == -1 || peak == -1 || stop == -1 || stop <= start)
            return null;
        return new int[]{start, stop};
    }

    // ---------------------------------------------------------------------------------------
    // File contents
    // ---------------------------------------------------------------------------------------

    /**
     * Base of the exported file names: curve name, motor class and date, without spaces or
     * characters that are not allowed in a file name. HH: 24 hour clock (hh was ambiguous).
     */
    private String newFileBaseName() {
        String date = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss", Locale.US).format(new Date());
        String name = ThrustCurveName;
        if (analysis.valid)
            name = name + "_" + analysis.motorClass;
        return FileExportHelper.sanitizeFileName(name + "_" + date);
    }

    /**
     * Builds a CSV file for one series. Numbers always use a dot as decimal separator
     * (Locale.US): in French the former code produced "0,123 ;12,5" which broke the columns.
     *
     * @param from         first sample index (inclusive)
     * @param to           last sample index (exclusive)
     * @param timeOffsetMs time subtracted from each sample
     */
    private String buildCsv(XYSeries serie, int serieIndex, int from, int to, double timeOffsetMs) {
        double factor = UnitsHelper.seriesFactor(myBT.getAppConf(), serieIndex);
        boolean isThrust = UnitsHelper.isThrustSeries(serieIndex);
        String header = "time (s)," + (isThrust ? "thrust (" + thrustUnit + ")" : "pressure (" + pressureUnit + ")");
        StringBuilder sb = new StringBuilder(header).append('\n');
        int end = Math.min(to, serie.getItemCount());
        for (int k = Math.max(0, from); k < end; k++) {
            sb.append(String.format(Locale.US, "%.3f,%.3f%n",
                    (serie.getX(k).doubleValue() - timeOffsetMs) / 1000.0,
                    serie.getY(k).doubleValue() * factor));
        }
        return sb.toString();
    }

    /**
     * CSV files limited to the burn: thrust, then the pressure rises (time relative to the
     * thrust start so that all the files share the same time base).
     */
    private Map<String, String> buildBurnCsvFiles(String base) {
        Map<String, String> files = new LinkedHashMap<>();
        XYSeries thrust = lThrustCurveData.getSeries(BoardCapabilities.SERIES_THRUST);
        double t0 = analysis.valid ? thrust.getX(analysis.start).doubleValue() : 0;
        if (analysis.valid) {
            files.put(base + ".csv", buildCsv(thrust, BoardCapabilities.SERIES_THRUST,
                    analysis.start, analysis.stop + 1, t0));
        }
        if (hasPressure())
            addPressureCsv(files, base + "_pressure.csv", BoardCapabilities.SERIES_PRESSURE, t0);
        if (hasPressure2())
            addPressureCsv(files, base + "_pressure2.csv", BoardCapabilities.SERIES_PRESSURE2, t0);
        return files;
    }

    private void addPressureCsv(Map<String, String> files, String name, int serieIndex, double thrustStartMs) {
        XYSeries pressure = lThrustCurveData.getSeries(serieIndex);
        int[] window = pressureWindow(pressure);
        if (window == null)
            return;
        // Without a valid thrust burn, the pressure rise itself is the time reference (the
        // former code read the sample at index -1 and crashed)
        double t0 = analysis.valid ? thrustStartMs : pressure.getX(window[0]).doubleValue();
        files.put(name, buildCsv(pressure, serieIndex, window[0], window[1] + 1, t0));
    }

    /**
     * CSV files with the whole recording of every series.
     */
    private Map<String, String> buildFullCsvFiles(String base) {
        Map<String, String> files = new LinkedHashMap<>();
        XYSeries thrust = lThrustCurveData.getSeries(BoardCapabilities.SERIES_THRUST);
        files.put(base + "_full.csv", buildCsv(thrust, BoardCapabilities.SERIES_THRUST,
                0, thrust.getItemCount(), 0));
        if (hasPressure()) {
            XYSeries p1 = lThrustCurveData.getSeries(BoardCapabilities.SERIES_PRESSURE);
            files.put(base + "_pressure_full.csv", buildCsv(p1, BoardCapabilities.SERIES_PRESSURE,
                    0, p1.getItemCount(), 0));
        }
        if (hasPressure2()) {
            XYSeries p2 = lThrustCurveData.getSeries(BoardCapabilities.SERIES_PRESSURE2);
            files.put(base + "_pressure2_full.csv", buildCsv(p2, BoardCapabilities.SERIES_PRESSURE2,
                    0, p2.getItemCount(), 0));
        }
        return files;
    }

    /**
     * Builds a RASP .eng motor file readable by OpenRocket and RockSim.
     * Format: comment lines starting with ';', a header line
     * "name diameter(mm) length(mm) delays propellant(kg) total(kg) manufacturer",
     * then "time(s) thrust(N)" points with strictly increasing times, ending with thrust 0.
     * The former export wrote the words "motorDiam casinglength..." in the header, which
     * the simulators rejected, and had no final zero thrust point.
     */
    private String buildEng(EngSpec spec) {
        XYSeries thrust = lThrustCurveData.getSeries(BoardCapabilities.SERIES_THRUST);
        StringBuilder sb = new StringBuilder();
        sb.append(";File generated by RocketMotorTestStand\n");
        sb.append(String.format(Locale.US, ";%s - total impulse %.2f N.s - burn time %.3f s - average thrust %.1f N%n",
                ThrustCurveName, analysis.totalImpulseNs, analysis.burnTimeS, analysis.averageThrustN));
        sb.append(String.format(Locale.US, "%s %.0f %.0f %s %.4f %.4f %s%n",
                FileExportHelper.sanitizeFileName(analysis.motorClass),
                spec.diameterMm, spec.lengthMm,
                FileExportHelper.sanitizeFileName(spec.delays),
                spec.propellantG / 1000.0, spec.totalG / 1000.0,
                FileExportHelper.sanitizeFileName(spec.manufacturer)));

        // Time origin: the sample just before the 5% crossing, so that the first point has
        // a time > 0 (the simulators add the implicit (0, 0) point themselves)
        double t0 = thrust.getX(analysis.start > 0 ? analysis.start - 1 : analysis.start).doubleValue();
        double lastTime = 0;
        double factorN = UnitsHelper.STANDARD_GRAVITY / 1000.0; // grams -> newtons
        for (int k = analysis.start; k <= analysis.stop; k++) {
            double t = (thrust.getX(k).doubleValue() - t0) / 1000.0;
            if (t <= lastTime)
                continue; // times must be strictly increasing
            // thrust cannot be negative in a motor file (load cell noise)
            double f = Math.max(0, thrust.getY(k).doubleValue() * factorN);
            sb.append(String.format(Locale.US, "%.3f %.3f%n", t, f));
            lastTime = t;
        }
        // Final point: thrust back to zero at the next sample time
        double endTime;
        if (analysis.stop + 1 < thrust.getItemCount())
            endTime = (thrust.getX(analysis.stop + 1).doubleValue() - t0) / 1000.0;
        else
            endTime = lastTime + 0.001;
        if (endTime <= lastTime)
            endTime = lastTime + 0.001;
        sb.append(String.format(Locale.US, "%.3f %.3f%n", endTime, 0.0));
        sb.append(";\n");
        return sb.toString();
    }

    // ---------------------------------------------------------------------------------------
    // Actions
    // ---------------------------------------------------------------------------------------

    /**
     * Saves the files in Download/RocketMotorTestStand and tells the user where they are.
     */
    private void saveFiles(Map<String, String> files) {
        if (files.isEmpty()) {
            Toast.makeText(getContext(), R.string.no_burn_detected, Toast.LENGTH_LONG).show();
            return;
        }
        List<String> saved = new ArrayList<>();
        for (Map.Entry<String, String> file : files.entrySet()) {
            String path = FileExportHelper.saveToDownloads(requireContext(), file.getKey(),
                    file.getValue().getBytes(StandardCharsets.UTF_8));
            if (path != null)
                saved.add(file.getKey());
        }
        if (saved.isEmpty()) {
            Toast.makeText(getContext(), R.string.export_failed, Toast.LENGTH_LONG).show();
        } else {
            StringBuilder msg = new StringBuilder(getString(R.string.files_saved));
            for (String name : saved)
                msg.append('\n').append(name);
            Toast.makeText(getContext(), msg.toString(), Toast.LENGTH_LONG).show();
        }
    }

    private void exportEng() {
        if (!analysis.valid) {
            Toast.makeText(getContext(), R.string.no_burn_detected, Toast.LENGTH_LONG).show();
            return;
        }
        askEngSpec(new EngSpecCallback() {
            @Override
            public void onSpec(EngSpec spec) {
                String fileName = newFileBaseName() + ".eng";
                String path = FileExportHelper.saveToDownloads(requireContext(), fileName,
                        buildEng(spec).getBytes(StandardCharsets.UTF_8));
                if (path == null) {
                    Toast.makeText(getContext(), R.string.export_failed, Toast.LENGTH_LONG).show();
                    return;
                }
                new AlertDialog.Builder(getContext())
                        .setMessage(getString(R.string.not_saved_msg) + "\n" + path)
                        .setTitle(R.string.not_saved_title)
                        .setCancelable(false)
                        .setPositiveButton(R.string.not_saved_ok, new DialogInterface.OnClickListener() {
                            public void onClick(final DialogInterface dialog, final int id) {
                                dialog.cancel();
                            }
                        })
                        .show();
            }
        });
    }

    private void shareEng() {
        if (!analysis.valid) {
            Toast.makeText(getContext(), R.string.no_burn_detected, Toast.LENGTH_LONG).show();
            return;
        }
        askEngSpec(new EngSpecCallback() {
            @Override
            public void onSpec(EngSpec spec) {
                String fileName = newFileBaseName() + ".eng";
                Uri uri = FileExportHelper.saveForSharing(requireContext(), fileName,
                        buildEng(spec).getBytes(StandardCharsets.UTF_8));
                if (uri == null) {
                    Toast.makeText(getContext(), R.string.export_failed, Toast.LENGTH_LONG).show();
                    return;
                }
                FileExportHelper.share(requireActivity(), uri, "text/plain",
                        "MotorTestStand has shared with you some info");
            }
        });
    }

    /**
     * Shares a zip with all the CSV files and, when a burn was detected, the .eng file.
     */
    private void shareZip() {
        if (!analysis.valid) {
            shareZipWith(null);
            return;
        }
        askEngSpec(new EngSpecCallback() {
            @Override
            public void onSpec(EngSpec spec) {
                shareZipWith(spec);
            }
        });
    }

    private void shareZipWith(@Nullable EngSpec spec) {
        String base = newFileBaseName();
        Map<String, byte[]> entries = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : buildBurnCsvFiles(base).entrySet())
            entries.put(e.getKey(), e.getValue().getBytes(StandardCharsets.UTF_8));
        for (Map.Entry<String, String> e : buildFullCsvFiles(base).entrySet())
            entries.put(e.getKey(), e.getValue().getBytes(StandardCharsets.UTF_8));
        if (spec != null)
            entries.put(base + ".eng", buildEng(spec).getBytes(StandardCharsets.UTF_8));
        try {
            Uri uri = FileExportHelper.saveForSharing(requireContext(), "motorData.zip",
                    FileExportHelper.zip(entries));
            if (uri == null) {
                Toast.makeText(getContext(), R.string.export_failed, Toast.LENGTH_LONG).show();
                return;
            }
            FileExportHelper.share(requireActivity(), uri, "application/zip",
                    "MotorTestStand has shared with you some info");
        } catch (IOException e) {
            Toast.makeText(getContext(), R.string.export_failed, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Asks the motor data needed in the .eng header (diameter, length, delays, masses,
     * manufacturer). The last values typed are remembered.
     */
    private void askEngSpec(final EngSpecCallback callback) {
        final Context ctx = requireContext();
        final SharedPreferences prefs = ctx.getSharedPreferences(ENG_PREFS, Context.MODE_PRIVATE);

        LinearLayout layout = new LinearLayout(ctx);
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (16 * ctx.getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding / 2, padding, 0);

        final EditText diameter = addField(layout, R.string.eng_diameter, prefs.getString("diameter", ""), true);
        final EditText length = addField(layout, R.string.eng_length, prefs.getString("length", ""), true);
        final EditText delays = addField(layout, R.string.eng_delays, prefs.getString("delays", "P"), false);
        final EditText propellant = addField(layout, R.string.eng_propellant_mass, prefs.getString("propellant", ""), true);
        final EditText total = addField(layout, R.string.eng_total_mass, prefs.getString("total", ""), true);
        final EditText manufacturer = addField(layout, R.string.eng_manufacturer, prefs.getString("manufacturer", "Homemade"), false);

        android.widget.ScrollView scroll = new android.widget.ScrollView(ctx);
        scroll.addView(layout);

        new AlertDialog.Builder(ctx)
                .setTitle(R.string.eng_dialog_title)
                .setView(scroll)
                .setNegativeButton(R.string.fv_cancel, null)
                .setPositiveButton(R.string.fv_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        EngSpec spec = new EngSpec();
                        spec.diameterMm = parseUserNumber(diameter);
                        spec.lengthMm = parseUserNumber(length);
                        spec.propellantG = parseUserNumber(propellant);
                        spec.totalG = parseUserNumber(total);
                        spec.delays = delays.getText().toString().trim();
                        spec.manufacturer = manufacturer.getText().toString().trim();
                        if (spec.delays.isEmpty())
                            spec.delays = "P";
                        if (spec.manufacturer.isEmpty())
                            spec.manufacturer = "Homemade";
                        if (spec.diameterMm <= 0 || spec.lengthMm <= 0 || spec.propellantG <= 0
                                || spec.totalG < spec.propellantG) {
                            Toast.makeText(ctx, R.string.eng_invalid_values, Toast.LENGTH_LONG).show();
                            return;
                        }
                        prefs.edit()
                                .putString("diameter", diameter.getText().toString().trim())
                                .putString("length", length.getText().toString().trim())
                                .putString("delays", spec.delays)
                                .putString("propellant", propellant.getText().toString().trim())
                                .putString("total", total.getText().toString().trim())
                                .putString("manufacturer", spec.manufacturer)
                                .apply();
                        callback.onSpec(spec);
                    }
                })
                .show();
    }

    /**
     * Reads a number typed by the user, accepting a comma as decimal separator.
     */
    private static double parseUserNumber(EditText field) {
        return ConsoleApplication.parseDoubleSafe(field.getText().toString().replace(',', '.'), -1);
    }

    /**
     * Adds a label and a text field to the .eng export dialog.
     */
    private EditText addField(LinearLayout layout, int labelRes, String value, boolean numeric) {
        Context ctx = layout.getContext();
        TextView label = new TextView(ctx);
        label.setText(labelRes);
        layout.addView(label);
        EditText field = new EditText(ctx);
        field.setText(value);
        field.setSingleLine(true);
        if (numeric)
            field.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        layout.addView(field);
        return field;
    }
}
