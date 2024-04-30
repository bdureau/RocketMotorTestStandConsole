package com.rocketmotorteststand.Config.AppConfig;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.rocketmotorteststand.ConsoleApplication;
import com.rocketmotorteststand.R;
import com.rocketmotorteststand.Config.AppConfigData;

public class AppConfigTab1Fragment extends Fragment {
    private Spinner spAppLanguage, spGraphColor, spAppUnit, spAppUnitPressure, spGraphBackColor,
            spFontSize, spBaudRate;
    private Spinner spConnectionType, spGraphicsLibType;
    private CheckBox cbFullUSBSupport;
    private CheckBox cbAllowManualRecording;
    private ConsoleApplication BT;
    private boolean ViewCreated = false;
    private AppConfigData appConfigData = null;


    public AppConfigTab1Fragment(ConsoleApplication lBT, AppConfigData pAppConfigData) {
        BT = lBT;
        appConfigData = pAppConfigData;
    }

    public int getAppLanguage() {
        return (int) this.spAppLanguage.getSelectedItemId();
    }

    public void setAppLanguage(int value) {
        this.spAppLanguage.setSelection(value);
    }

    public int getGraphColor() {
        return (int) this.spGraphColor.getSelectedItemId();
    }

    public void setGraphColor(int value) {
        this.spGraphColor.setSelection(value);
    }

    public int getAppUnit() {
        return (int) this.spAppUnit.getSelectedItemId();
    }
    public int getAppUnitPressure() {
        return (int) this.spAppUnitPressure.getSelectedItemId();
    }

    public void setAppUnit(int value) {
        this.spAppUnit.setSelection(value);
    }
    public void setAppUnitPressure(int value) {
        this.spAppUnitPressure.setSelection(value);
    }

    public int getGraphBackColor() {
        return (int) this.spGraphBackColor.getSelectedItemId();
    }

    public void setGraphBackColor(int value) {
        this.spGraphBackColor.setSelection(value);
    }

    public int getFontSize() {
        return (int) this.spFontSize.getSelectedItemId();
    }

    public void setFontSize(int value) {
        this.spFontSize.setSelection(value);
    }

    public int getBaudRate() {
        return (int) this.spBaudRate.getSelectedItemId();
    }

    public void setBaudRate(int value) {
        this.spBaudRate.setSelection(value);
    }

    public int getConnectionType() {
        return (int) this.spConnectionType.getSelectedItemId();
    }

    public void setConnectionType(int value) {
        this.spConnectionType.setSelection(value);
    }

    public int getGraphicsLibType() {
        return (int) this.spGraphicsLibType.getSelectedItemId();
    }

    public void setGraphicsLibType(int value) {
        this.spGraphicsLibType.setSelection(value);
    }


    public boolean getFullUSBSupport() {
        return (cbFullUSBSupport.isChecked());
    }

    public void setFullUSBSupport(boolean value) {
        cbFullUSBSupport.setChecked(value);
    }
    public boolean getAllowManualRecording() {
        return cbAllowManualRecording.isChecked();
    }

    public void setAllowManualRecording(boolean value) {
        cbAllowManualRecording.setChecked(value);
    }

    public boolean isViewCreated() {
        return ViewCreated;
    }
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        View view = inflater.inflate(R.layout.fragment_app_config_part1, container, false);

        //Language
        spAppLanguage = (Spinner) view.findViewById(R.id.spinnerLanguage);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this.getActivity(),
                android.R.layout.simple_spinner_dropdown_item, appConfigData.getItemsLanguages());
        spAppLanguage.setAdapter(adapter);

        // graph color
        spGraphColor = (Spinner) view.findViewById(R.id.spinnerGraphColor);
        // String[] itemsColor = new String[]{"Black", "White", "Yellow", "Red", "Green", "Blue"};
        ArrayAdapter<String> adapterColor = new ArrayAdapter<String>(this.getActivity(),
                android.R.layout.simple_spinner_dropdown_item, appConfigData.getItemsColor());
        spGraphColor.setAdapter(adapterColor);
        // graph back color
        spGraphBackColor = (Spinner) view.findViewById(R.id.spinnerGraphBackColor);
        ArrayAdapter<String> adapterGraphColor = new ArrayAdapter<String>(this.getActivity(),
                android.R.layout.simple_spinner_dropdown_item, appConfigData.getItemsColor());

        spGraphBackColor.setAdapter(adapterGraphColor);
        //units
        spAppUnit = (Spinner) view.findViewById(R.id.spinnerUnits);

        ArrayAdapter<String> adapter2 = new ArrayAdapter<String>(this.getActivity(),
                android.R.layout.simple_spinner_dropdown_item, appConfigData.getItemsUnits());
        spAppUnit.setAdapter(adapter2);

        //units Pressure
        spAppUnitPressure = (Spinner) view.findViewById(R.id.spinnerUnitsPressure);

        ArrayAdapter<String> adapter3 = new ArrayAdapter<String>(this.getActivity(),
                android.R.layout.simple_spinner_dropdown_item, appConfigData.getItemsUnitsPressure());
        spAppUnitPressure.setAdapter(adapter3);

        //font size
        spFontSize = (Spinner) view.findViewById(R.id.spinnerFontSize);

        ArrayAdapter<String> adapterFontSize = new ArrayAdapter<String>(this.getActivity(),
                android.R.layout.simple_spinner_dropdown_item, appConfigData.getItemsFontSize());
        spFontSize.setAdapter(adapterFontSize);

        //Baud Rate
        spBaudRate = (Spinner) view.findViewById(R.id.spinnerBaudRate);

        ArrayAdapter<String> adapterBaudRate = new ArrayAdapter<String>(this.getActivity(),
                android.R.layout.simple_spinner_dropdown_item, appConfigData.getItemsBaudRate());
        spBaudRate.setAdapter(adapterBaudRate);

        //connection type
        spConnectionType = (Spinner) view.findViewById(R.id.spinnerConnectionType);

        ArrayAdapter<String> adapterConnectionType = new ArrayAdapter<String>(this.getActivity(),
                android.R.layout.simple_spinner_dropdown_item, appConfigData.getItemsConnectionType());
        spConnectionType.setAdapter(adapterConnectionType);

        //Graphics lib type
        spGraphicsLibType = (Spinner) view.findViewById(R.id.spinnerGraphicLibType);
        ArrayAdapter<String> adapterGraphicsLibType = new ArrayAdapter<String>(this.getActivity(),
                android.R.layout.simple_spinner_dropdown_item, appConfigData.getItemsGraphicsLib());
        spGraphicsLibType.setAdapter(adapterGraphicsLibType);


        //Allow only telemetry via USB
        cbFullUSBSupport = (CheckBox) view.findViewById(R.id.checkBoxFullUSBSupport);

        spAppLanguage.setSelection(BT.getAppConf().getApplicationLanguage());
        spAppUnit.setSelection(BT.getAppConf().getUnits());
        spAppUnitPressure.setSelection(BT.getAppConf().getUnitsPressure());
        spGraphColor.setSelection(BT.getAppConf().getGraphColor());
        spGraphBackColor.setSelection(BT.getAppConf().getGraphBackColor());
        spFontSize.setSelection(BT.getAppConf().getFontSize());
        spBaudRate.setSelection(BT.getAppConf().getBaudRate());
        spConnectionType.setSelection(BT.getAppConf().getConnectionType());
        spGraphicsLibType.setSelection(BT.getAppConf().getGraphicsLibType());
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            //if android ver = 8 or greater use the MPlib so disable the choice and for it to use MP
            spGraphicsLibType.setSelection(1);
            spGraphicsLibType.setEnabled(false);
        }

        cbFullUSBSupport.setChecked(BT.getAppConf().getFullUSBSupport());
        // allow manual recording
        cbAllowManualRecording = (CheckBox) view.findViewById(R.id.checkBoxAllowManualRecording);
        cbAllowManualRecording.setChecked(BT.getAppConf().getManualRecording());

        ViewCreated = true;
        return view;
    }

}