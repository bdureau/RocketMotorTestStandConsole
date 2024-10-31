package com.rocketmotorteststand.config.TestStandConfig;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.florent37.viewtooltip.ViewTooltip;
import com.rocketmotorteststand.R;
import com.rocketmotorteststand.config.TestStandConfigData;

public class TestStandConfigTab2Fragment extends Fragment {
    private static final String TAG = "Tab3Fragment";

    private TextView testStandName,  CalibrationFactor, CurrentOffset, txtBluetoothName;
    private Spinner dropdownUnits;
    private EditText calibrationWeight, bluetoothName;


    private TestStandConfigData ltestStandNameCfg = null;

    public TestStandConfigTab2Fragment(TestStandConfigData cfg) {
        ltestStandNameCfg = cfg;
    }

    private boolean ViewCreated = false;

    public boolean isViewCreated() {
        return ViewCreated;
    }


    public void setTestStandName(String altiName) {
        this.testStandName.setText(altiName);
    }

    public String getTestStandName() {
        return (String) this.testStandName.getText();
    }

    public int getDropdownUnits() {
        return (int) this.dropdownUnits.getSelectedItemId();
    }

    public void setDropdownUnits(int Units) {
        this.dropdownUnits.setSelection(Units);
    }

    public int getCalibrationFactor() {
        int ret;
        try {
            ret = Integer.parseInt(this.CalibrationFactor.getText().toString());
        } catch (Exception e) {
            ret = 0;
        }
        return ret;
    }

    public void setCalibrationFactor(String value){
        this.CalibrationFactor.setText(value);
    }

    //getCurrentOffset
    public int getCurrentOffset() {
        int ret;
        try {
            ret = Integer.parseInt(this.CurrentOffset.getText().toString());
        } catch (Exception e) {
            ret = 0;
        }
        return ret;
    }
    public void setCurrentOffset(String value){
        this.CurrentOffset.setText(value);
    }
    public String getCalibrationWeight() {
        return this.calibrationWeight.getText().toString();
    }

    public String getBluetoothName() { return this.bluetoothName.getText().toString();}

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_teststand_config_part3, container, false);
        CalibrationFactor = (TextView) view.findViewById(R.id.txtCalibrationFactorValue);
        CurrentOffset = (TextView) view.findViewById(R.id.txtCalibrationOffsetValue);
        calibrationWeight = (EditText) view.findViewById(R.id.txtCalibrationWeightValue);

        //units
        dropdownUnits = (Spinner) view.findViewById(R.id.spinnerUnit);
        //"kg", "pounds"
        String[] items2 = new String[]{getResources().getString(R.string.unit_kg),
                getResources().getString(R.string.unit_pound), getResources().getString(R.string.unit_newton)};
        ArrayAdapter<String> adapter2 = new ArrayAdapter<String>(this.getActivity(),
                android.R.layout.simple_spinner_dropdown_item, items2);
        dropdownUnits.setAdapter(adapter2);

        // Tool tip
        view.findViewById(R.id.txtTestStandUnit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ViewTooltip
                        .on(v)
                        .color(Color.BLACK)
                        .position(ViewTooltip.Position.TOP)
                        //Choose the altitude units you are familiar with
                        .text(getResources().getString(R.string.txtTestStandUnit_tooltip))
                        .show();
            }
        });
        //Test Stand name
        testStandName = (TextView) view.findViewById(R.id.txtAltiNameValue);

        if (ltestStandNameCfg != null) {
            testStandName.setText(ltestStandNameCfg.getTestStandName() + " ver: " +
                    ltestStandNameCfg.getTestStandMajorVersion() + "." +
                    ltestStandNameCfg.getTestStandMinorVersion());

            dropdownUnits.setSelection(ltestStandNameCfg.getUnits());
            CalibrationFactor.setText(String.valueOf(ltestStandNameCfg.getCalibrationFactor()));
            CurrentOffset.setText(String.valueOf(ltestStandNameCfg.getCurrentOffset()));
        }

        //bluetoothName
        //testand name
        txtBluetoothName = (TextView) view.findViewById(R.id.txtBluetoothName);
        bluetoothName = (EditText) view.findViewById(R.id.editBluetoothName);
        bluetoothName.setText(ltestStandNameCfg.getBluetoothName());
        if ((ltestStandNameCfg.getTestStandName().equals("TestStandESP32") ||
                ltestStandNameCfg.getTestStandName().equals("TestStandESP32V3")  &&
                (ltestStandNameCfg.getTestStandMajorVersion() >= 1 && ltestStandNameCfg.getTestStandMinorVersion() > 7))) {
            Log.d( TAG, "Version greater  than 1.7");
            txtBluetoothName.setVisibility(View.VISIBLE);
            bluetoothName.setVisibility(View.VISIBLE);
        } else {
            txtBluetoothName.setVisibility(View.INVISIBLE);
            bluetoothName.setVisibility(View.INVISIBLE);
        }
        ViewCreated = true;
        return view;
    }

}
