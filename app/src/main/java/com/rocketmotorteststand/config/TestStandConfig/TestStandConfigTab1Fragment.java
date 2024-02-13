package com.rocketmotorteststand.config.TestStandConfig;

import android.graphics.Color;
import android.os.Bundle;
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
import com.rocketmotorteststand.ConsoleApplication;
import com.rocketmotorteststand.R;
import com.rocketmotorteststand.config.TestStandConfigData;

public class TestStandConfigTab1Fragment extends Fragment {
    private static final String TAG = "Tab2Fragment";
    private TestStandConfigData lTestStandCfg = null;
    private String[] itemsBaudRate;
    private String[] itemsTestStandResolution;
    private String[] itemsEEpromSize;
    private String[] itemsBatteryType;
    private String[] itemsSensorType;
    private String[] itemsTelemetryType;
    private Spinner dropdownBaudRate;
    private Spinner dropdownTestStandResolution, dropdownEEpromSize;
    private Spinner dropdownSensorType, dropdownSensorType2;
    private Spinner dropdownTelemetryType,dropdownBatteryType;
    private EditText StopRecordingTime;
    private ConsoleApplication myBT;

    private boolean ViewCreated = false;
    private TextView txtViewEEpromSize, txtViewSensorType, txtViewTelemetryType, txtViewSensorType2;

    public TestStandConfigTab1Fragment(ConsoleApplication lBT, TestStandConfigData cfg) {
        lTestStandCfg = cfg;
        myBT = lBT;
    }

    public boolean isViewCreated() {
        return ViewCreated;
    }


    public int getTestStandResolution() {
        return (int) this.dropdownTestStandResolution.getSelectedItemId();
    }

    public void setTestStandResolution(int TestStandResolution) {
        this.dropdownTestStandResolution.setSelection(TestStandResolution);
    }

    public int getEEpromSize() {
        int ret;
        try {
            ret = Integer.parseInt(itemsEEpromSize[(int) dropdownEEpromSize.getSelectedItemId()]);
        } catch (Exception e) {
            ret = 0;
        }
        return ret;
    }

    public void setEEpromSize(int EEpromSize) {
        this.dropdownEEpromSize.setSelection(lTestStandCfg.arrayIndex(itemsEEpromSize, String.valueOf(EEpromSize)));
    }

    public long getBaudRate() {
        int ret;
        try {
            ret = Integer.parseInt(itemsBaudRate[(int) this.dropdownBaudRate.getSelectedItemId()]);
        } catch (Exception e) {
            ret = 0;
        }
        return ret;
    }

    public void setBaudRate(long BaudRate) {
        this.dropdownBaudRate.setSelection(lTestStandCfg.arrayIndex(itemsBaudRate, String.valueOf(BaudRate)));
    }


    public int getStopRecordingTime() {
        int ret;
        try {
            ret = Integer.parseInt(this.StopRecordingTime.getText().toString());
        } catch (Exception e) {
            ret = 0;
        }
        return ret;
    }

    public void setStopRecordingTime(int StopRecordingTime) {
        this.StopRecordingTime.setText(String.valueOf(StopRecordingTime));
    }


    public int getBatteryType() {
        return (int) this.dropdownBatteryType.getSelectedItemId();
    }

    public void setBatteryType(int BatteryType) {
        dropdownBatteryType.setSelection(BatteryType);
    }

    public int getSensorType() {
        return (int) this.dropdownSensorType.getSelectedItemId();
    }

    public void setSensorType(int SensorType) {
        dropdownSensorType.setSelection(SensorType);
    }
    public int getSensorType2() {
        return (int) this.dropdownSensorType2.getSelectedItemId();
    }
    public void setSensorType2(int SensorType) {
        dropdownSensorType2.setSelection(SensorType);
    }
    public int getTelemetryType() {
        return (int) this.dropdownTelemetryType.getSelectedItemId();
    }

    public void setTelemetryType(int TelemetryType) {
        dropdownTelemetryType.setSelection(TelemetryType);
    }
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_teststand_config_part2, container, false);

        //baud rate
        dropdownBaudRate = (Spinner) view.findViewById(R.id.spinnerBaudRate);
        itemsBaudRate = new String[]{"300",
                "1200",
                "2400",
                "4800",
                "9600",
                "14400",
                "19200",
                "28800",
                "38400",
                "57600",
                "115200",
                "230400"};
        ArrayAdapter<String> adapterBaudRate = new ArrayAdapter<String>(this.getActivity(),
                android.R.layout.simple_spinner_dropdown_item, itemsBaudRate);
        dropdownBaudRate.setAdapter(adapterBaudRate);
        // Tool tip
        view.findViewById(R.id.txtViewBaudRate).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ViewTooltip
                        .on(v)
                        .color(Color.BLACK)
                        .position(ViewTooltip.Position.TOP)
                        //Choose the test stand baud rate. Be carefull you might not be able to communicate
                        .text(getResources().getString(R.string.txtViewBaudRate_tooltip))
                        .show();
            }
        });

        // test stand resolution
        dropdownTestStandResolution = (Spinner) view.findViewById(R.id.spinnerTestStandResolution);
        itemsTestStandResolution = new String[]{"ULTRALOWPOWER", "STANDARD", "HIGHRES", "ULTRAHIGHRES"};
        ArrayAdapter<String> adapterTestStandResolution = new ArrayAdapter<String>(this.getActivity(),
                android.R.layout.simple_spinner_dropdown_item, itemsTestStandResolution);
        dropdownTestStandResolution.setAdapter(adapterTestStandResolution);

        // Tool tip
        view.findViewById(R.id.txtViewTestStandResolution).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ViewTooltip
                        .on(v)
                        .color(Color.BLACK)
                        .position(ViewTooltip.Position.TOP)
                        //Choose the altimeter resolution. The faster you rocket goes the lower it has to be
                        .text(getResources().getString(R.string.txtViewTestStandResolution_tooltip))
                        .show();
            }
        });
        //Test Stand external eeprom size
        txtViewEEpromSize = (TextView) view.findViewById(R.id.txtViewEEpromSize);
        dropdownEEpromSize = (Spinner) view.findViewById(R.id.spinnerEEpromSize);
        itemsEEpromSize = new String[]{"32", "64", "128", "256", "512", "1024"};
        ArrayAdapter<String> adapterEEpromSize = new ArrayAdapter<String>(this.getActivity(),
                android.R.layout.simple_spinner_dropdown_item, itemsEEpromSize);
        dropdownEEpromSize.setAdapter(adapterEEpromSize);


        // Tool tip
        view.findViewById(R.id.txtViewEEpromSize).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ViewTooltip
                        .on(v)
                        .color(Color.BLACK)
                        .position(ViewTooltip.Position.TOP)
                        //Choose the test stand eeprom size depending on which eeprom is used
                        .text(getResources().getString(R.string.txtViewEEpromSize_tooltip))
                        .show();
            }
        });


        // nbr of newtons to stop recording thrust
        StopRecordingTime = (EditText) view.findViewById(R.id.editTxtStopRecordingTime);
        // Tool tip
        StopRecordingTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ViewTooltip
                        .on(v)
                        .color(Color.BLACK)
                        .position(ViewTooltip.Position.TOP)
                        //recording timeout
                        .text(getResources().getString(R.string.EndRecordTime_tooltip))
                        .show();
            }
        });

        dropdownBatteryType = (Spinner) view.findViewById(R.id.spinnerBatteryType);
        //"Unknown",
        itemsBatteryType = new String[]{getResources().getString(R.string.config_unknown),
                "2S (7.4 Volts)", "9 Volts", "3S (11.1 Volts)"};
        ArrayAdapter<String> adapterBatteryType = new ArrayAdapter<String>(this.getActivity(),
                android.R.layout.simple_spinner_dropdown_item, itemsBatteryType);
        dropdownBatteryType.setAdapter(adapterBatteryType);

        // Tool tip
        view.findViewById(R.id.txtViewBatteryType).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ViewTooltip
                        .on(v)
                        .color(Color.BLACK)
                        .position(ViewTooltip.Position.TOP)
                        //Enter battery type used to make sure that we do not discharge it too much
                        .text(getResources().getString(R.string.txtViewBatteryType_tooltip))
                        .show();
            }
        });

        txtViewSensorType = (TextView)  view.findViewById(R.id.txtViewSensorType);
        dropdownSensorType = (Spinner) view.findViewById(R.id.spinnerSensorType);
        //"Unknown",
        itemsSensorType = new String[]{getResources().getString(R.string.config_unknown),
                "100 PSI", "150 PSI", "200 PSI", "300 PSI", "500 PSI", "1000 PSI", "1600 PSI"};
        ArrayAdapter<String> adapterSensorType = new ArrayAdapter<String>(this.getActivity(),
                android.R.layout.simple_spinner_dropdown_item, itemsSensorType);
        dropdownSensorType.setAdapter(adapterSensorType);

        // Tool tip
        view.findViewById(R.id.txtViewSensorType).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ViewTooltip
                        .on(v)
                        .color(Color.BLACK)
                        .position(ViewTooltip.Position.TOP)
                        //Enter battery type used to make sure that we do not discharge it too much
                        .text(getResources().getString(R.string.txtViewSensorType_tooltip))
                        .show();
            }
        });

        //second pressure sensor if available
        txtViewSensorType2 = (TextView)  view.findViewById(R.id.txtViewSensorType2);
        dropdownSensorType2 = (Spinner) view.findViewById(R.id.spinnerSensorType2);
        ArrayAdapter<String> adapterSensorType2 = new ArrayAdapter<String>(this.getActivity(),
                android.R.layout.simple_spinner_dropdown_item, itemsSensorType);
        dropdownSensorType2.setAdapter(adapterSensorType2);
        // Tool tip
        view.findViewById(R.id.txtViewSensorType2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ViewTooltip
                        .on(v)
                        .color(Color.BLACK)
                        .position(ViewTooltip.Position.TOP)
                        //Enter battery type used to make sure that we do not discharge it too much
                        .text(getResources().getString(R.string.txtViewSensorType_tooltip))
                        .show();
            }
        });

        txtViewTelemetryType = (TextView)  view.findViewById(R.id.txtViewTelemetryType);
        dropdownTelemetryType = (Spinner) view.findViewById(R.id.spinnerTelemetryType);
        //"Unknown",
        itemsTelemetryType = new String[]{"Fast", "Medium", "Slow", "Very slow"};
        ArrayAdapter<String> adapterTelemetryType = new ArrayAdapter<String>(this.getActivity(),
                android.R.layout.simple_spinner_dropdown_item, itemsTelemetryType);
        dropdownTelemetryType.setAdapter(adapterTelemetryType);

        if (myBT.getTestStandConfigData().getTestStandName().equals("TestStandSTM32V2") ||
                myBT.getTestStandConfigData().getTestStandName().equals("TestStandSTM32V3") ||
                myBT.getTestStandConfigData().getTestStandName().equals("TestStandESP32") ||
                myBT.getTestStandConfigData().getTestStandName().equals("TestStandESP32V3")) {
            dropdownSensorType.setVisibility(View.VISIBLE);
            txtViewSensorType.setVisibility(View.VISIBLE);
        } else {
            dropdownSensorType.setVisibility(View.INVISIBLE);
            txtViewSensorType.setVisibility(View.INVISIBLE);
        }
        if(myBT.getTestStandConfigData().getTestStandName().equals("TestStandSTM32V3") ||
                myBT.getTestStandConfigData().getTestStandName().equals("TestStandESP32V3")){
            dropdownSensorType2.setVisibility(View.VISIBLE);
            txtViewSensorType2.setVisibility(View.VISIBLE);
        } else {
            dropdownSensorType2.setVisibility(View.INVISIBLE);
            txtViewSensorType2.setVisibility(View.INVISIBLE);
        }
        if (lTestStandCfg != null) {
            dropdownBaudRate.setSelection(lTestStandCfg.arrayIndex(itemsBaudRate,
                    String.valueOf(lTestStandCfg.getConnectionSpeed())));

            dropdownTestStandResolution.setSelection(lTestStandCfg.getTestStandResolution());
            dropdownEEpromSize.setSelection(lTestStandCfg.arrayIndex(itemsEEpromSize,
                    String.valueOf(lTestStandCfg.getEepromSize())));

            StopRecordingTime.setText(String.valueOf(lTestStandCfg.getStopRecordingTime()));

            dropdownBatteryType.setSelection(lTestStandCfg.getBatteryType());
            dropdownSensorType.setSelection(lTestStandCfg.getPressureSensorType());
            dropdownSensorType2.setSelection(lTestStandCfg.getPressureSensorType2());
            dropdownTelemetryType.setSelection(lTestStandCfg.getTelemetryType());
        }
        ViewCreated = true;
        return view;
    }
}
