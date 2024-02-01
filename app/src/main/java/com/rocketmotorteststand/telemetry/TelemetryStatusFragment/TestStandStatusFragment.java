package com.rocketmotorteststand.telemetry.TelemetryStatusFragment;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.rocketmotorteststand.ConsoleApplication;
import com.rocketmotorteststand.R;

public class TestStandStatusFragment extends Fragment {
    private ConsoleApplication myBT;
    private String[] units;
    private Button btnTare;
    private boolean ViewCreated = false;
    private TextView txtViewBatteryVoltage, txtViewCurrentPressure;
    private TextView txtViewThrust, txtViewVoltage, txtViewLink, txtEEpromUsage, txtNbrOfThrustCurve;
    private TextView txtViewEEprom, txtViewThrustCurve, txtViewCurrentPressureValue;

    public TestStandStatusFragment(ConsoleApplication pBT, String[] pUnits) {
        myBT = pBT;
        units = pUnits;
    }

    public void setBatteryVoltage(String value) {
        if (ViewCreated) {
            txtViewVoltage.setText(value + " Volts");
            if (value.matches("\\d+(?:\\.\\d+)?"))
                if (Double.parseDouble(value) < 7.0) {
                    txtViewVoltage.setTextColor(Color.RED);
                } else {
                    txtViewVoltage.setTextColor(txtViewThrust.getTextColors());
                }
        }
    }

    public void setThrust(String value) {
        if (ViewCreated)
            txtViewThrust.setText(value);
    }

    public void setPressure(String value) {
        if (ViewCreated)
            txtViewCurrentPressureValue.setText(value);
    }

    public void setEEpromUsage(String value) {
        if (ViewCreated) {
            txtEEpromUsage.setText(value + " %");
            if (value.matches("\\d+(?:\\.\\d+)?"))
                if (Integer.parseInt(value) >= 90) {
                    txtEEpromUsage.setTextColor(Color.RED);
                } else {
                    txtEEpromUsage.setTextColor(txtViewThrust.getTextColors());
                }
        }
    }

    public void setNbrOfThrustCurve(String nbrOfThrustCurve) {
        if (ViewCreated) {
            txtNbrOfThrustCurve.setText(nbrOfThrustCurve);
            // If we have the maximum of thrust curve put it in red
            if (nbrOfThrustCurve.matches("\\d+(?:\\.\\d+)?"))
                if (Integer.parseInt(nbrOfThrustCurve) >= 23) {
                    txtNbrOfThrustCurve.setTextColor(Color.RED);
                } else {
                    txtNbrOfThrustCurve.setTextColor(txtViewThrust.getTextColors());
                }
        }
    }

    public boolean isViewCreated() {
        return ViewCreated;
    }

    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        View view = inflater.inflate(R.layout.fragment_teststand_status, container, false);

        txtViewThrust = (TextView) view.findViewById(R.id.txtViewThrust);
        txtViewVoltage = (TextView) view.findViewById(R.id.txtViewVoltage);
        txtViewLink = (TextView) view.findViewById(R.id.txtViewLink);
        txtViewBatteryVoltage = (TextView) view.findViewById(R.id.txtViewBatteryVoltage);
        txtEEpromUsage = (TextView) view.findViewById(R.id.txtViewEEpromUsage);
        txtNbrOfThrustCurve = (TextView) view.findViewById(R.id.txtViewNbrOfThrustCurve);
        txtViewEEprom = (TextView) view.findViewById(R.id.txtViewEEprom);
        txtViewThrustCurve = (TextView) view.findViewById(R.id.txtViewThrustCurve);
        txtViewCurrentPressureValue = (TextView) view.findViewById(R.id.txtViewCurrentPressureValue);
        txtViewCurrentPressure = (TextView) view.findViewById(R.id.txtViewCurrentPressure);
        btnTare = (Button) view.findViewById(R.id.butTare);

        txtViewVoltage.setVisibility(View.VISIBLE);
        txtViewBatteryVoltage.setVisibility(View.VISIBLE);

        if (myBT.getTestStandConfigData().getTestStandName().equals("TestStandSTM32V2") ||
                myBT.getTestStandConfigData().getTestStandName().equals("TestStandESP32")) {
            txtViewCurrentPressureValue.setVisibility(View.VISIBLE);
            txtViewCurrentPressure.setVisibility(View.VISIBLE);
        } else {
            txtViewCurrentPressureValue.setVisibility(View.INVISIBLE);
            txtViewCurrentPressure.setVisibility(View.INVISIBLE);
        }
        txtViewEEprom.setVisibility(View.VISIBLE);
        txtViewThrustCurve.setVisibility(View.VISIBLE);
        txtEEpromUsage.setVisibility(View.VISIBLE);
        txtNbrOfThrustCurve.setVisibility(View.VISIBLE);


        txtViewLink.setText(myBT.getConnectionType());


        btnTare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Send tare command
                myBT.flush();
                myBT.clearInput();
                myBT.write("j;\n".toString());
            }
        });
        ViewCreated = true;
        return view;
    }
}
