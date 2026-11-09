package com.weatherradar;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

public class SettingsActivity extends AppCompatActivity {

    private Spinner spinnerUnits, spinnerTheme;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = getSharedPreferences("WeatherPrefs", MODE_PRIVATE);

        spinnerUnits = findViewById(R.id.spinnerUnits);
        spinnerTheme = findViewById(R.id.spinnerTheme);
        Button btnRefresh = findViewById(R.id.btnSettingsRefresh);
        Button btnRadar = findViewById(R.id.btnSettingsRadar);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // إعداد قائمة وحدات الحرارة
        String[] units = {"مئوية (°C)", "فهرنهايت (°F)"};
        ArrayAdapter<String> unitsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, units);
        spinnerUnits.setAdapter(unitsAdapter);
        // استعادة الاختيار المحفوظ
        int savedUnit = prefs.getInt("unit", 0);
        spinnerUnits.setSelection(savedUnit);

        // إعداد قائمة السمات (الألوان)
        String[] themes = {"صافي (أزرق)", "ليلي (بنفسجي)", "ممطر (رمادي)"};
        ArrayAdapter<String> themesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, themes);
        spinnerTheme.setAdapter(themesAdapter);
        int savedTheme = prefs.getInt("theme", 0);
        spinnerTheme.setSelection(savedTheme);

        // حفظ التفضيلات عند التغيير
        spinnerUnits.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                prefs.edit().putInt("unit", position).apply();
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        spinnerTheme.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                prefs.edit().putInt("theme", position).apply();
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        // أزرار التحديث والرادار
        btnRefresh.setOnClickListener(v -> {
            // هنا يمكن إضافة كود لتحديث البيانات (مثلاً إرسال Intent أو استدعاء دالة)
            finish(); // العودة للشاشة الرئيسية لتحديثها
        });

        btnRadar.setOnClickListener(v -> {
            startActivity(new Intent(SettingsActivity.this, RadarActivity.class));
        });
    }
}
