package com.rocketmotorteststand.Telemetry.TelemetryStatusFragment;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.rocketmotorteststand.BoardCapabilities;
import com.rocketmotorteststand.ConsoleApplication;
import com.rocketmotorteststand.R;

/**
 * @description: Status tab of the test stand: current thrust, casing pressure(s), battery
 * voltage, eeprom usage and number of recorded thrust curves. Also offers the tare button.
 * @author: boris.dureau@neuf.fr
 **/
public class TestStandStatusFragment extends Fragment {
    private ConsoleApplication myBT;
    private String[] units;
    private Button btnTare;
    private boolean ViewCreated = false;
    private TextView txtViewBatteryVoltage, txtViewCurrentPressure, txtViewCurrentPressure2;
    private TextView txtViewThrust, txtViewVoltage, txtViewLink, txtEEpromUsage, txtNbrOfThrustCurve;
    private TextView txtViewEEprom, txtViewThrustCurve, txtViewCurrentPressureValue, txtViewCurrentPressureValue2;

    /**
     * Empty constructor required by Android to recreate the fragment. A recreated instance
     * is discarded by the host activity (see FragmentRecreationHelper).
     */
    public TestStandStatusFragment() {
    }

    public TestStandStatusFragment(ConsoleApplication pBT, String[] pUnits) {
        myBT = pBT;
        units = pUnits;
    }

    public void setBatteryVoltage(String value) {
        if (ViewCreated) {
            txtViewVoltage.setText(value + " Volts");
            if (ConsoleApplication.isUnsignedNumber(value))
                if (ConsoleApplication.parseDoubleSafe(value, 0) < 7.0) {
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

    public void setPressure2(String value) {
        if (ViewCreated)
            txtViewCurrentPressureValue2.setText(value);
    }

    public void setEEpromUsage(String value) {
        if (ViewCreated) {
            txtEEpromUsage.setText(value + " %");
            // parseDoubleSafe: Integer.parseInt crashed on decimal values such as "12.5"
            if (ConsoleApplication.isUnsignedNumber(value))
                if (ConsoleApplication.parseDoubleSafe(value, 0) >= 90) {
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
            if (ConsoleApplication.isUnsignedNumber(nbrOfThrustCurve))
                if (ConsoleApplication.parseDoubleSafe(nbrOfThrustCurve, 0) >= 23) {
                    txtNbrOfThrustCurve.setTextColor(Color.RED);
                } else {
                    txtNbrOfThrustCurve.setTextColor(txtViewThrust.getTextColors());
                }
        }
    }

    public boolean isViewCreated() {
        return ViewCreated;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ViewCreated = false;
    }

    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
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
        txtViewCurrentPressureValue2 = (TextView) view.findViewById(R.id.txtViewCurrentPressureValue2);
        txtViewCurrentPressure2 = (TextView) view.findViewById(R.id.txtViewCurrentPressure2);
        btnTare = (Button) view.findViewById(R.id.butTare);

        txtViewVoltage.setVisibility(View.VISIBLE);
        txtViewBatteryVoltage.setVisibility(View.VISIBLE);

        if (BoardCapabilities.hasPressure(myBT.getBoardName())) {
            txtViewCurrentPressureValue.setVisibility(View.VISIBLE);
            txtViewCurrentPressure.setVisibility(View.VISIBLE);
        } else {
            txtViewCurrentPressureValue.setVisibility(View.INVISIBLE);
            txtViewCurrentPressure.setVisibility(View.INVISIBLE);
        }

        if (BoardCapabilities.hasPressure2(myBT.getBoardName())) {
            txtViewCurrentPressureValue2.setVisibility(View.VISIBLE);
            txtViewCurrentPressure2.setVisibility(View.VISIBLE);
        } else {
            txtViewCurrentPressureValue2.setVisibility(View.INVISIBLE);
            txtViewCurrentPressure2.setVisibility(View.INVISIBLE);
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
