package com.rocketmotorteststand.config.AppConfig;

import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;

import com.rocketmotorteststand.ConsoleApplication;
import com.rocketmotorteststand.R;

public class AppConfigTab2Fragment extends Fragment {

    private ConsoleApplication BT;

    public AppConfigTab2Fragment(ConsoleApplication lBT) {
        BT = lBT;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        View view = inflater.inflate(R.layout.fragment_app_config_part2, container, false);

        return view;
    }
    private void msg(String s) {
        Toast.makeText(getContext(), s, Toast.LENGTH_LONG).show();
    }
}
