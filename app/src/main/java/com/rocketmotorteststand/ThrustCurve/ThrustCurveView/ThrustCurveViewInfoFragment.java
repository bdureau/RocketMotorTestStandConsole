package com.rocketmotorteststand.ThrustCurve.ThrustCurveView;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.rocketmotorteststand.ConsoleApplication;
import com.rocketmotorteststand.R;
import com.rocketmotorteststand.ThrustCurve.ThrustCurveData;
import com.rocketmotorteststand.ThrustCurve.ThrustCurveViewTabActivity;

import org.afree.data.xy.XYSeriesCollection;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ThrustCurveViewInfoFragment extends Fragment {

    private ThrustCurveData mythrustCurve;
    private XYSeriesCollection thrustCurveData = null;
    private String ThrustCurveName = null;
    private TextView nbrOfSamplesValue, thrustCurveNbrValue;
    private TextView recordingDurationValue, thrustTimeValue, maxThrustValue, averageThrustValue;
    private TextView motorClassValue, totalImpulseValue, maxPressureValue;
    private TextView maxPressure;
    private Button buttonExportToCsv, buttonExportToCsvFull, buttonExportToEng, buttonShareEng,
            butShareZip;
    double totalImpulse = 0;
    int numberOfCurves = 0;
    private String motorClass = "";
    double thrustDuration = 0;
    double averageThrust = 0;
    private AlertDialog.Builder builder = null;
    private AlertDialog alert;
    boolean SavedCurvesOK = false;
    private ConsoleApplication myBT;
    private static double CONVERT = 1.0, CONVERT_PRESSURE = 1.0;
    private String[] units = null;

    public ThrustCurveViewInfoFragment(ThrustCurveData data,
                                       ConsoleApplication pBT,
                                       String pUnits[],
                                       String pThrustCurveName) {
        mythrustCurve = data;
        myBT = pBT;
        ThrustCurveName = pThrustCurveName;
        units = pUnits;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        numberOfCurves = 1;
        if (myBT.getTestStandConfigData().getTestStandName().equals("TestStandSTM32V2")) {
            numberOfCurves = 2;
        }
        // Read the application config
        myBT.getAppConf().ReadConfig();
        if (myBT.getAppConf().getUnits().equals("0")) {
            //kg
            units[0] = "(" + getResources().getString(R.string.Kg_fview) + ")";
            CONVERT = 1.0 / 1000;

        } else if (myBT.getAppConf().getUnits().equals("1")) {
            //pounds
            units[0] = getResources().getString(R.string.Pounds_fview);
            CONVERT = 2.20462 / 1000;

        } else if (myBT.getAppConf().getUnits().equals("2")) {
            //newtons
            units[0] = getResources().getString(R.string.Newtons_fview);
            CONVERT = 9.80665 / 1000;
        }

        if (numberOfCurves > 1) {
            if (myBT.getAppConf().getUnitsPressure().equals("0")) {
                //PSI
                units[1] = "(" + "PSI" + ")";
                CONVERT_PRESSURE = 1;
            } else if (myBT.getAppConf().getUnits().equals("1")) {
                //BAR
                units[1] = "BAR";
                CONVERT_PRESSURE = 0.0689476;
            } else if (myBT.getAppConf().getUnits().equals("2")) {
                //Kpascal
                units[1] = "Kpascal";
                CONVERT_PRESSURE = 6.89476;
            }
        }
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

        if (myBT.getTestStandConfigData().getTestStandName().equals("TestStandSTM32V2")) {
            maxPressureValue.setVisibility(View.VISIBLE);
            maxPressure.setVisibility(View.VISIBLE);
        } else {
            maxPressureValue.setVisibility(View.INVISIBLE);
            maxPressure.setVisibility(View.INVISIBLE);
        }

        XYSeriesCollection lThrustCurveData;

        lThrustCurveData = mythrustCurve.GetThrustCurveData(ThrustCurveName);
        int nbrData = lThrustCurveData.getSeries(0).getItemCount();

        // ThrustCurve nbr
        thrustCurveNbrValue.setText(ThrustCurveName + "");

        //nbr of samples
        nbrOfSamplesValue.setText(nbrData + "");

        //Recording duration
        double recordingDuration = lThrustCurveData.getSeries(0).getMaxX() / 1000;
        recordingDurationValue.setText(String.format("%.3f ", recordingDuration) + " secs");

        ThrustCurveViewTabActivity.ThrustUtil tu = new ThrustCurveViewTabActivity.ThrustUtil();
        double maxThrust = lThrustCurveData.getSeries(0).getMaxY();
        double triggerThrust = maxThrust * (5.0 / 100.0);

        int curveStart = tu.searchX(lThrustCurveData.getSeries(0), triggerThrust);
        int curveMaxThrust = tu.searchX(lThrustCurveData.getSeries(0), maxThrust);
        int curveStop = tu.searchXFrom(lThrustCurveData.getSeries(0), curveMaxThrust, triggerThrust);
        if (curveStart != -1 && curveMaxThrust != -1 && curveStop != -1) {
            // thrust duration
            thrustDuration = ((double) lThrustCurveData.getSeries(0).getX(curveStop) - (double) lThrustCurveData.getSeries(0).getX(curveStart)) / 1000.0;
            thrustTimeValue.setText(String.format("%.2f ", thrustDuration) + " secs");
            double totThurst = 0;
            for (int i = curveStart; i < curveStop; i++) {
                totThurst = totThurst + (double) lThrustCurveData.getSeries(0).getY(i);
            }
            averageThrust = totThurst / ((double) (curveStop - curveStart));

            //max thrust
            maxThrustValue.setText(String.format("%.1f ", maxThrust * CONVERT) + units[0]);
            //average thrust
            averageThrustValue.setText(String.format("%.1f ", averageThrust * CONVERT) + units[0]);

            //tot impulse
            //double totalImpulse = 0;
            totalImpulse = averageThrust * (9.80665 / 1000) * thrustDuration;
            totalImpulseValue.setText(String.format("%.0f ", totalImpulse) + " Newtons");
            //motor class
            motorClass = tu.motorClass(totalImpulse) + String.format("%.0f ", averageThrust * (9.80665 / 1000));
            motorClassValue.setText(tu.motorClass(totalImpulse) + String.format("%.0f ", averageThrust * (9.80665 / 1000)));
        }

        if (myBT.getTestStandConfigData().getTestStandName().equals("TestStandSTM32V2")) {
            double maxPres = lThrustCurveData.getSeries(1).getMaxY();
            maxPressureValue.setText(maxPres + " PSI");
        }
        buttonExportToCsv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exportToCSV(motorClass, lThrustCurveData);      //export the data to a csv file
            }
        });

        buttonExportToCsvFull.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exportToCSVFull(motorClass, lThrustCurveData);      //export the data to a csv file
            }
        });

        buttonExportToEng.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exportToEng(motorClass, lThrustCurveData, getString(R.string.file_saved_msg2));      //export the data to an eng file
            }
        });

        buttonShareEng.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String currentEng = exportToEng(motorClass, lThrustCurveData, "");
                Log.d("File:", currentEng);

                File engFile = new File(Environment.getExternalStoragePublicDirectory
                        (Environment.DIRECTORY_DOWNLOADS), currentEng);
                Toast.makeText(getContext(), currentEng, Toast.LENGTH_SHORT).show();
                shareFile(engFile);
            }
        });
        butShareZip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // Create a file for the zip file
                File zipFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "motorData.zip");
                ArrayList<String> fileNames = new ArrayList<>();
                fileNames.add(exportThrustToCSV(lThrustCurveData));
                if (myBT.getTestStandConfigData().getTestStandName().equals("TestStandSTM32V2")) {
                    fileNames.add(exportPressureToCSV(lThrustCurveData));
                }
                fileNames.add(exportToEng(motorClass, lThrustCurveData, ""));
                try {
                    // Create a zip output stream to write to the zip file
                    FileOutputStream fos = new FileOutputStream(zipFile);
                    ZipOutputStream zos = new ZipOutputStream(fos);

                    for (String fileName : fileNames) {
                        ZipEntry ze = new ZipEntry(fileName);
                        // Add the zip entry to the zip output stream
                        zos.putNextEntry(ze);
                        // Read the file and write it to the zip output stream
                        File filetoZip = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),  fileName);
                        FileInputStream fis = new FileInputStream(filetoZip);
                        byte[] buffer = new byte[1024];
                        int len;
                        while ((len = fis.read(buffer)) > 0) {
                            zos.write(buffer, 0, len);
                        }
                        // Close the zip entry and the file input stream
                        zos.closeEntry();
                        fis.close();
                    }
                    // Close the zip output stream
                    zos.close();
                    fos.close();
                }catch (Exception e) {
                    e.printStackTrace();
                    Log.d("error", "we have an issue");
                }
                //Toast.makeText(getContext(), currentEng, Toast.LENGTH_SHORT).show();
                shareFile(zipFile);
            }
        });
        return view;
    }


    private void exportToCSV(String motorClass, XYSeriesCollection lThrustCurveData) {
        String fileNames="";
        String thrustFileName = exportThrustToCSV(lThrustCurveData);
        fileNames = thrustFileName;
        if (myBT.getTestStandConfigData().getTestStandName().equals("TestStandSTM32V2")) {
            String pressureFileName = exportPressureToCSV(lThrustCurveData);
            fileNames = fileNames + "\n" + pressureFileName;
        }
        Toast.makeText(getContext(), fileNames, Toast.LENGTH_SHORT).show();
    }

    private String exportThrustToCSV(XYSeriesCollection lThrustCurveData) {
        SimpleDateFormat sdf = new SimpleDateFormat("_dd-MM-yyyy_hh-mm-ss");
        String date = sdf.format(System.currentTimeMillis());
        String csv_data = "time,thrust" + units[0] + "\n";/// your csv data as string;

        ThrustCurveViewTabActivity.ThrustUtil tu = new ThrustCurveViewTabActivity.ThrustUtil();
        double maxThrust = lThrustCurveData.getSeries(0).getMaxY();
        double triggerThrust = maxThrust * (5.0 / 100.0);

        int curveStart = tu.searchX(lThrustCurveData.getSeries(0), triggerThrust);
        int curveMaxThrust = tu.searchX(lThrustCurveData.getSeries(0), maxThrust);
        int curveStop = tu.searchXFrom(lThrustCurveData.getSeries(0), curveMaxThrust, triggerThrust);
        // Export Thrust
        if (curveStart != -1 && curveMaxThrust != -1 && curveStop != -1) {
            for (int k = curveStart; k < curveStop; k++) {

                String curTime = String.format("%.3f ", (lThrustCurveData.getSeries(0).getX(k).floatValue() - lThrustCurveData.getSeries(0).getX(curveStart).floatValue()) / 1000);
                if (curTime.contains(","))
                    curTime = curTime + ";";
                else
                    curTime = curTime + ",";
                String currData = "";
                if (myBT.getAppConf().getUnits().equals("0")) {
                    //kg
                    currData = String.format("%.1f ", (lThrustCurveData.getSeries(0).getY(k).floatValue() / 1000));
                } else if (myBT.getAppConf().getUnits().equals("1")) {
                    //pound
                    currData = String.format("%.1f ", (lThrustCurveData.getSeries(0).getY(k).floatValue() / 1000) * (float) 2.20462);
                } else if (myBT.getAppConf().getUnits().equals("2")) {
                    //newton
                    currData = String.format("%.1f ", (lThrustCurveData.getSeries(0).getY(k).floatValue() / 1000) * (float) 9.80665);
                }
                csv_data = csv_data + curTime + currData + "\n";
            }
        }

        //SimpleDateFormat sdf = new SimpleDateFormat("_dd-MM-yyyy_hh-mm-ss");
        //String date = sdf.format(System.currentTimeMillis());
        String fileName = ThrustCurveName + "_" + motorClass + "_" + date + ".csv";
        createFile(fileName, csv_data, "");
        return "RocketMotorTestStand/" +fileName;
    }

    private String exportPressureToCSV(XYSeriesCollection lThrustCurveData) {
        SimpleDateFormat sdf = new SimpleDateFormat("_dd-MM-yyyy_hh-mm-ss");
        String date = sdf.format(System.currentTimeMillis());
        String fileName = "";
        ThrustCurveViewTabActivity.ThrustUtil tu = new ThrustCurveViewTabActivity.ThrustUtil();
        double maxThrust = lThrustCurveData.getSeries(0).getMaxY();
        double triggerThrust = maxThrust * (5.0 / 100.0);

        int curveStart = tu.searchX(lThrustCurveData.getSeries(0), triggerThrust);
        String csv_data_pressure = "time,pressure" + units[1] + "\n";/// your csv data as string;

        double maxPressure = lThrustCurveData.getSeries(1).getMaxY();
        double minPressure = lThrustCurveData.getSeries(1).getMinY();
        double triggerPressure = (maxPressure * (5.0 / 100.0)) + minPressure;

        int curvePressureStart = tu.searchX(lThrustCurveData.getSeries(1), triggerPressure);
        int curveMaxPressure = tu.searchX(lThrustCurveData.getSeries(1), maxPressure);
        int curvePressureStop = tu.searchXFrom(lThrustCurveData.getSeries(1), curveMaxPressure, triggerPressure);

        if (curvePressureStart != -1 && curveMaxPressure != -1 && curvePressureStop != -1) {
            for (int k = curvePressureStart; k < curvePressureStop; k++) {

                String curTime = String.format("%.3f ", (lThrustCurveData.getSeries(1).getX(k).floatValue() - lThrustCurveData.getSeries(1).getX(curveStart).floatValue()) / 1000);
                if (curTime.contains(","))
                    curTime = curTime + ";";
                else
                    curTime = curTime + ",";
                String currData = "";
                if (myBT.getAppConf().getUnitsPressure().equals("0")) {
                    //PSI
                    currData = String.format("%.1f ", (lThrustCurveData.getSeries(1).getY(k).floatValue()));
                } else if (myBT.getAppConf().getUnitsPressure().equals("1")) {
                    //bar
                    currData = String.format("%.1f ", lThrustCurveData.getSeries(1).getY(k).floatValue() / (float) 14.504);
                } else if (myBT.getAppConf().getUnitsPressure().equals("2")) {
                    //Kpascal
                    currData = String.format("%.1f ", lThrustCurveData.getSeries(1).getY(k).floatValue() * (float) 6.895);
                }
                csv_data_pressure = csv_data_pressure + curTime + currData + "\n";
            }
            fileName = ThrustCurveName + "_pressure_" + motorClass + "_" + date + ".csv";
            createFile(fileName, csv_data_pressure, "");
        }
        return "RocketMotorTestStand/" +fileName;
    }

    private void exportToCSVFull(String motorClass, XYSeriesCollection lThrustCurveData) {

        String csv_data = "time,thrust" + units[0] + "\n";/// your csv data as string;

        for (int k = 0; k < lThrustCurveData.getSeries(0).getItemCount(); k++) {
            String curTime = String.format("%.3f ", (lThrustCurveData.getSeries(0).getX(k).floatValue()) / 1000);
            if (curTime.contains(","))
                curTime = curTime + ";";
            else
                curTime = curTime + ",";
            String currData = "";
            if (myBT.getAppConf().getUnits().equals("0")) {
                //kg
                currData = String.format("%.1f ", (lThrustCurveData.getSeries(0).getY(k).floatValue() / 1000));
            } else if (myBT.getAppConf().getUnits().equals("1")) {
                //pound
                currData = String.format("%.1f ", (lThrustCurveData.getSeries(0).getY(k).floatValue() / 1000) * (float) 2.20462);
            } else if (myBT.getAppConf().getUnits().equals("2")) {
                //newton
                currData = String.format("%.1f ", (lThrustCurveData.getSeries(0).getY(k).floatValue() / 1000) * (float) 9.80665);
            }
            csv_data = csv_data + curTime + currData + "\n";
        }


        SimpleDateFormat sdf = new SimpleDateFormat("_dd-MM-yyyy_hh-mm-ss");
        String date = sdf.format(System.currentTimeMillis());
        createFile(ThrustCurveName + "_full_" + motorClass + "_" + date + ".csv", csv_data, "");

        //export pressure
        if (myBT.getTestStandConfigData().getTestStandName().equals("TestStandSTM32V2")) {
            String csv_data_pressure = "time,pressure" + units[1] + "\n";

            for (int k = 0; k < lThrustCurveData.getSeries(1).getItemCount(); k++) {
                String curTime = String.format("%.3f ", (lThrustCurveData.getSeries(1).getX(k).floatValue()) / 1000);
                if (curTime.contains(","))
                    curTime = curTime + ";";
                else
                    curTime = curTime + ",";
                String currData = "";
                if (myBT.getAppConf().getUnitsPressure().equals("0")) {
                    //PSI
                    currData = String.format("%.1f ", (lThrustCurveData.getSeries(1).getY(k).floatValue()));
                } else if (myBT.getAppConf().getUnitsPressure().equals("1")) {
                    //bar
                    currData = String.format("%.1f ", lThrustCurveData.getSeries(1).getY(k).floatValue() / (float) 14.504);
                } else if (myBT.getAppConf().getUnitsPressure().equals("2")) {
                    //Kpascal
                    currData = String.format("%.1f ", lThrustCurveData.getSeries(1).getY(k).floatValue() * (float) 6.895);
                }
                csv_data_pressure = csv_data_pressure + curTime + currData + "\n";
            }
            createFile(ThrustCurveName + "_pressure_full_" + motorClass + "_" + date + ".csv", csv_data_pressure, "");
        }
    }

    private void createFile(String fileName, String csv_data, String msg) {
        File root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

        //if you want to create a sub-dir
        root = new File(root, "RocketMotorTestStand");
        root.mkdir();

        //SimpleDateFormat sdf = new SimpleDateFormat("_dd-MM-yyyy_hh-mm-ss");
        //String date = sdf.format(System.currentTimeMillis());

        // select the name for your file
        root = new File(root, fileName);

        try {
            FileOutputStream fout = new FileOutputStream(root);
            fout.write(csv_data.getBytes());

            fout.close();
            if (!msg.equals("")) {
                //Confirmation message
                builder = new AlertDialog.Builder(this.getContext());
                //Running Saving commands
                builder.setMessage(getString(R.string.not_saved_msg) + Environment.DIRECTORY_DOWNLOADS + "\\RocketMotorTestStand\\" + fileName + msg)
                        .setTitle(R.string.not_saved_title)
                        .setCancelable(false)
                        .setPositiveButton(R.string.not_saved_ok, new DialogInterface.OnClickListener() {
                            public void onClick(final DialogInterface dialog, final int id) {
                                dialog.cancel();
                            }
                        });

                alert = builder.create();
                alert.show();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();

            boolean bool = false;
            try {
                // try to create the file
                bool = root.createNewFile();
            } catch (IOException ex) {
                ex.printStackTrace();
            }

            if (bool) {
                // call the method again
                createFile(fileName, csv_data, msg);
            } else {
                //throw new IllegalStateException(getString(R.string.failed_to_create_csv));
                SavedCurvesOK = false;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String exportToEng(String motorClass, XYSeriesCollection lThrustCurveData, String msg) {

        String motorfile_data = ";File generated by RocketMotorTestStand \n;\n";
        //K1530 54 714 0 1.287998 1.499998 TruCore

        motorfile_data = motorfile_data + "\n";
        motorfile_data = motorfile_data + motorClass + " " + "motorDiam" + " " + "casinglength"
                + " " + "delay" + " " + "propellantWeight" + " " + "totalWeight" + " " + "manufacturer" + "\n";

        ThrustCurveViewTabActivity.ThrustUtil tu = new ThrustCurveViewTabActivity.ThrustUtil();
        double maxThrust = thrustCurveData.getSeries(0).getMaxY();
        double triggerThrust = maxThrust * (5.0 / 100.0);

        int curveStart = tu.searchX(thrustCurveData.getSeries(0), triggerThrust);
        int curveMaxThrust = tu.searchX(thrustCurveData.getSeries(0), maxThrust);
        int curveStop = tu.searchXFrom(thrustCurveData.getSeries(0), curveMaxThrust, triggerThrust);

        if (curveStart != -1 && curveMaxThrust != -1 && curveStop != -1) {
            for (int k = curveStart; k < curveStop; k++) {

                //newton
                //yValues.add(new Entry(thrustCurveData.getSeries(0).getX(k).floatValue() - thrustCurveData.getSeries(0).getX(curveStart).floatValue(),
                // (thrustCurveData.getSeries(0).getY(k).floatValue() / 1000) * (float) 9.80665));
                String currData = String.format("%.3f ", (thrustCurveData.getSeries(0).getX(k).floatValue() - thrustCurveData.getSeries(0).getX(curveStart).floatValue()) / 1000) + " " +
                        String.format("%.1f ", (thrustCurveData.getSeries(0).getY(k).floatValue() / 1000) * (float) 9.80665);
                currData = currData.replace(",", ".");
                motorfile_data = motorfile_data + currData + "\n";
            }
        }

        motorfile_data = motorfile_data + ";\n";
        SimpleDateFormat sdf = new SimpleDateFormat("_dd-MM-yyyy_hh-mm-ss");
        String date = sdf.format(System.currentTimeMillis());
        createFile(ThrustCurveName + "_" + motorClass + "_" + date + ".eng", motorfile_data, msg);
        return "RocketMotorTestStand/" + ThrustCurveName + "_" + motorClass + "_" + date + ".eng";
    }

    //Share file
    private void shareFile(File file) {

        Uri uri = FileProvider.getUriForFile(
                getContext(),
                getContext().getPackageName() +  ".provider",
                file);

        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.setType("file/*");
        intent.putExtra(android.content.Intent.EXTRA_TEXT, "MotorTestStand has shared with you some info");
        intent.putExtra(Intent.EXTRA_STREAM, uri);


        Intent chooser = Intent.createChooser(intent, "Share File");

        List<ResolveInfo> resInfoList = getContext().getPackageManager().queryIntentActivities(chooser, PackageManager.MATCH_DEFAULT_ONLY);

        for (ResolveInfo resolveInfo : resInfoList) {
            String packageName = resolveInfo.activityInfo.packageName;
            getContext().grantUriPermission(packageName, uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }

        try {
            this.startActivity(chooser);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(getContext(), "No App Available", Toast.LENGTH_SHORT).show();
        }
    }
}
