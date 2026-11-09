package com.weatherradar;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.appcompat.app.AppCompatActivity;

public class RadarActivity extends AppCompatActivity {

    private WebView webView;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_radar); // تأكد أن هذا هو اسم ملف التصميم الخاص بالرادار

        webView = findViewById(R.id.webView);

        // إعدادات لتحسين أداء الخريطة
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.setWebViewClient(new WebViewClient());

        // 🌟 مسح الكاش لضمان ظهور التصميم الجديد فوراً
        webView.clearCache(true);
        webView.clearHistory();
        webView.clearFormData();

        // تحميل الخريطة من مجلد assets
        webView.loadUrl("file:///android_asset/radar_map.html");
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
