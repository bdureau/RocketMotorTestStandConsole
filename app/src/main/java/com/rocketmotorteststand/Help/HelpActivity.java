package com.rocketmotorteststand.Help;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;

import com.rocketmotorteststand.ConsoleApplication;
import com.rocketmotorteststand.R;

import java.util.Locale;

/**
 * @description: This read and display the html help file
 * @author: boris.dureau@neuf.fr
 **/

public class HelpActivity extends AppCompatActivity {
    Button btnDismiss;
    WebView webView;
    ConsoleApplication myBT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //get the bluetooth Application pointer
        myBT = (ConsoleApplication) getApplication();
        //Check the local and force it if needed
        //getApplicationContext().getResources().updateConfiguration(myBT.getAppLocal(), null);

        setContentView(R.layout.activity_help);
        webView = (WebView) findViewById(R.id.webView);

        WebSettings webSetting = webView.getSettings();

        webSetting.setBuiltInZoomControls(true);
        webSetting.setJavaScriptEnabled(true);

        webView.setWebViewClient(new WebViewClient());
       /* if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            webView.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        webView.getSettings().setAllowFileAccess(true);*/

        Intent newint = getIntent();
        String FileName = newint.getStringExtra("help_file");
        if(myBT.getAppConf().getApplicationLanguage().equals("0")) {
            //use phone language
            try {
                if (Locale.getDefault().getLanguage() == "fr")
                    webView.loadUrl("file:///android_asset/help/" + FileName + "_fr.html");
                else
                    webView.loadUrl("file:///android_asset/help/" + FileName + ".html");
            } catch (Exception e) {
                e.printStackTrace();
                webView.loadUrl("file:///android_asset/help/" + FileName + ".html");
            }
        }
        else {
            //force it to English
            webView.loadUrl("file:///android_asset/help/" + FileName + ".html");
        }

        btnDismiss = (Button) findViewById(R.id.butClose);
        btnDismiss.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();      //exit the help activity
            }
        });
    }

    private class WebViewClient extends android.webkit.WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            return super.shouldOverrideUrlLoading(view, url);
        }
    }
}
