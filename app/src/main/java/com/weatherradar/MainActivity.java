package com.weatherradar;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    TextView tvCityName, tvTemperature, tvCondition, tvWindDirection, tvAqi, tvHighLow;
    TextView tvHumidity, tvWindSpeed, tvFeelsLike, tvPressure, tvUvIndex, tvRainChance, tvSunrise, tvSunset;
    LinearLayout layoutForecast;
    HourlyForecastView hourlyForecastView;
    WindCompassView windCompassView;
    SunArcView sunArcView;
    View mainLayout;
    TextView btnAddCity, btnSettings, btnLocation, btnFavorite;

    double lat = 14.5425;
    double lon = 49.1242;
    String currentCityName = "المكلا";
    int tempUnit = 0;
    boolean isFavorite = false;

    ActivityResultLauncher<Intent> searchActivityLauncher;

    private LocationManager locationManager;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    private Handler locationTimeoutHandler = new Handler(Looper.getMainLooper());
    private Runnable locationTimeoutRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(Color.TRANSPARENT);

        setContentView(R.layout.activity_main);

        mainLayout = findViewById(R.id.mainLayout);
        mainLayout.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_in));

        SharedPreferences prefs = getSharedPreferences("WeatherPrefs", MODE_PRIVATE);
        tempUnit = prefs.getInt("unit", 0);

        tvCityName = findViewById(R.id.tvCityName);
        tvTemperature = findViewById(R.id.tvTemperature);
        tvCondition = findViewById(R.id.tvCondition);
        tvHighLow = findViewById(R.id.tvHighLow);
        tvHumidity = findViewById(R.id.tvHumidity);
        tvWindSpeed = findViewById(R.id.tvWindSpeed);
        tvWindDirection = findViewById(R.id.tvWindDirection);
        tvFeelsLike = findViewById(R.id.tvFeelsLike);
        tvPressure = findViewById(R.id.tvPressure);
        tvUvIndex = findViewById(R.id.tvUvIndex);
        tvRainChance = findViewById(R.id.tvRainChance);
        tvSunrise = findViewById(R.id.tvSunrise);
        tvSunset = findViewById(R.id.tvSunset);
        tvAqi = findViewById(R.id.tvAqi);
        layoutForecast = findViewById(R.id.layoutForecast);
        hourlyForecastView = findViewById(R.id.hourlyForecastView);
        windCompassView = findViewById(R.id.windCompassView);
        sunArcView = findViewById(R.id.sunArcView);
        btnAddCity = findViewById(R.id.btnAddCity);
        btnSettings = findViewById(R.id.btnSettings);
        btnLocation = findViewById(R.id.btnLocation);
        btnFavorite = findViewById(R.id.btnFavorite);

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        searchActivityLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();
                        lat = data.getDoubleExtra("lat", lat);
                        lon = data.getDoubleExtra("lon", lon);
                        currentCityName = data.getStringExtra("city_name");
                        tvCityName.setText(currentCityName);
                        checkFavoriteStatus();
                        fetchWeather();
                        fetchAirQuality();
                    }
                }
        );

        if (btnAddCity != null) {
            btnAddCity.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, SearchCityActivity.class);
                searchActivityLauncher.launch(intent);
            });
        }

        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, SettingsActivity.class)));
        }

        if (btnLocation != null) {
            btnLocation.setOnClickListener(v -> checkLocationPermission());
        }

        if (btnFavorite != null) {
            btnFavorite.setOnClickListener(v -> toggleFavorite());
        }

        mainLayout.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_sunny));
        tvCityName.setText(currentCityName);
        checkFavoriteStatus();
        fetchWeather();
        fetchAirQuality();
    }

    private void toggleFavorite() {
        SharedPreferences prefs = getSharedPreferences("Favorites", MODE_PRIVATE);
        String key = "fav_" + currentCityName;
        
        if (isFavorite) {
            prefs.edit().remove(key).apply();
            isFavorite = false;
            btnFavorite.setText("☆");
            Toast.makeText(this, "تمت إزالة المدينة من المفضلة", Toast.LENGTH_SHORT).show();
        } else {
            prefs.edit().putString(key, lat + "," + lon).apply();
            isFavorite = true;
            btnFavorite.setText("★");
            Toast.makeText(this, "تمت إضافة المدينة إلى المفضلة", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkFavoriteStatus() {
        SharedPreferences prefs = getSharedPreferences("Favorites", MODE_PRIVATE);
        String key = "fav_" + currentCityName;
        isFavorite = prefs.contains(key);
        btnFavorite.setText(isFavorite ? "★" : "☆");
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            getUserLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getUserLocation();
            } else {
                Toast.makeText(this, "تم رفض إذن الموقع", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void getUserLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        Toast.makeText(this, "جاري تحديد موقعك...", Toast.LENGTH_LONG).show();

        locationTimeoutRunnable = () -> Toast.makeText(MainActivity.this, "تعذر تحديد الموقع، تأكد من تشغيل GPS", Toast.LENGTH_LONG).show();
        locationTimeoutHandler.postDelayed(locationTimeoutRunnable, 15000);

        LocationListener listener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                locationTimeoutHandler.removeCallbacks(locationTimeoutRunnable);
                lat = location.getLatitude();
                lon = location.getLongitude();
                getCityNameFromLocation(lat, lon);
                fetchWeather();
                fetchAirQuality();
                locationManager.removeUpdates(this);
            }

            @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
            @Override public void onProviderEnabled(@NonNull String provider) {}
            @Override public void onProviderDisabled(@NonNull String provider) {}
        };

        try {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, listener, Looper.getMainLooper());
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, listener, Looper.getMainLooper());
        } catch (Exception e) {
            Log.e("GPS", "Error requesting location", e);
        }
    }

    private void getCityNameFromLocation(double lat, double lon) {
        try {
            Geocoder geocoder = new Geocoder(this, new Locale("ar"));
            List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                String city = address.getLocality();
                if (city == null || city.isEmpty()) {
                    city = address.getSubAdminArea();
                }
                if (city != null && !city.isEmpty()) {
                    currentCityName = city;
                    tvCityName.setText(currentCityName);
                } else {
                    currentCityName = "موقعي الحالي";
                    tvCityName.setText(currentCityName);
                }
            } else {
                currentCityName = "موقعي الحالي";
                tvCityName.setText(currentCityName);
            }
            checkFavoriteStatus();
        } catch (IOException e) {
            e.printStackTrace();
            currentCityName = "موقعي الحالي";
            tvCityName.setText(currentCityName);
            checkFavoriteStatus();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences prefs = getSharedPreferences("WeatherPrefs", MODE_PRIVATE);
        int newUnit = prefs.getInt("unit", 0);
        if (newUnit != tempUnit) {
            tempUnit = newUnit;
            fetchWeather();
        }
    }

    // 🌟 تحديث دالة جودة الهواء لتعمل بشكل أفضل
    private void fetchAirQuality() {
        String url = "https://air-quality-api.open-meteo.com/v1/air-quality?latitude=" + lat + "&longitude=" + lon + "&current=european_aqi";
        RequestQueue queue = Volley.newRequestQueue(this);
        
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        if (response.has("current")) {
                            JSONObject current = response.getJSONObject("current");
                            int aqi = current.getInt("european_aqi");
                            String status;
                            if (aqi <= 20) status = "ممتازة";
                            else if (aqi <= 40) status = "جيدة";
                            else if (aqi <= 60) status = "متوسطة";
                            else if (aqi <= 80) status = "سيئة";
                            else status = "سيئة جداً";
                            
                            tvAqi.setText("جودة الهواء: " + aqi + " (" + status + ")");
                        } else {
                            tvAqi.setText("جودة الهواء: غير متوفرة");
                        }
                    } catch (Exception e) {
                        Log.e("AQI", "Parse Error", e);
                        tvAqi.setText("جودة الهواء: خطأ في القراءة");
                    }
                },
                error -> {
                    Log.e("AQI", "Volley Error", error);
                    tvAqi.setText("جودة الهواء: فشل الاتصال");
                });

        queue.add(request);
    }

    private void fetchWeather() {
        String url = "https://api.open-meteo.com/v1/forecast?latitude=" + lat +
                "&longitude=" + lon +
                "&current=temperature_2m,relative_humidity_2m,apparent_temperature,surface_pressure,windspeed_10m,winddirection_10m,weathercode,is_day" +
                "&hourly=temperature_2m,weathercode,windspeed_10m" +
                "&daily=weathercode,temperature_2m_max,temperature_2m_min,sunrise,sunset,uv_index_max,precipitation_probability_max" +
                "&temperature_unit=" + (tempUnit == 0 ? "celsius" : "fahrenheit") +
                "&timezone=auto";

        RequestQueue queue = Volley.newRequestQueue(this);
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONObject current = response.getJSONObject("current");
                        tvTemperature.setText(String.format(Locale.US, "%.0f", current.getDouble("temperature_2m")));
                        tvCondition.setText(getWeatherDescription(current.getInt("weathercode")));
                        tvHumidity.setText(String.format(Locale.US, "%.0f%%", current.getDouble("relative_humidity_2m")));
                        tvWindSpeed.setText(String.format(Locale.US, "%.1f كم/س", current.getDouble("windspeed_10m")));
                        tvFeelsLike.setText(String.format(Locale.US, "%.0f°", current.getDouble("apparent_temperature")));
                        tvPressure.setText(String.format(Locale.US, "%.0f hPa", current.getDouble("surface_pressure")));

                        windCompassView.setDirection((float) current.getDouble("winddirection_10m"));
                        tvWindDirection.setText(getWindDirectionName(current.getDouble("winddirection_10m")));

                        JSONObject daily = response.getJSONObject("daily");
                        tvHighLow.setText(String.format(Locale.US, "%.0f° / %.0f°", daily.getJSONArray("temperature_2m_min").getDouble(0), daily.getJSONArray("temperature_2m_max").getDouble(0)));
                        tvSunrise.setText(daily.getJSONArray("sunrise").getString(0).split("T")[1]);
                        tvSunset.setText(daily.getJSONArray("sunset").getString(0).split("T")[1]);
                        tvUvIndex.setText(String.format(Locale.US, "%.1f", daily.getJSONArray("uv_index_max").getDouble(0)));
                        tvRainChance.setText(String.format(Locale.US, "%.0f%%", daily.getJSONArray("precipitation_probability_max").getDouble(0)));

                        layoutForecast.removeAllViews();
                        SimpleDateFormat apiFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                        SimpleDateFormat dispFmt = new SimpleDateFormat("EEEE", new Locale("ar"));
                        JSONArray timeArr = daily.getJSONArray("time");
                        JSONArray maxArr = daily.getJSONArray("temperature_2m_max");
                        JSONArray minArr = daily.getJSONArray("temperature_2m_min");
                        JSONArray codeArr = daily.getJSONArray("weathercode");

                        for (int i = 0; i < Math.min(5, timeArr.length()); i++) {
                            View item = getLayoutInflater().inflate(R.layout.item_forecast, layoutForecast, false);
                            TextView tvDay = item.findViewById(R.id.tvForecastDay);
                            TextView tvIcon = item.findViewById(R.id.tvForecastIcon);
                            TextView tvMax = item.findViewById(R.id.tvForecastTempMax);
                            TextView tvMin = item.findViewById(R.id.tvForecastTempMin);
                            if (i == 0) tvDay.setText("اليوم");
                            else if (i == 1) tvDay.setText("غداً");
                            else {
                                try { tvDay.setText(dispFmt.format(apiFmt.parse(timeArr.getString(i)))); } catch (Exception e) { tvDay.setText(timeArr.getString(i)); }
                            }
                            tvIcon.setText(getWeatherIcon(codeArr.getInt(i)));
                            tvMax.setText(String.format(Locale.US, "%.0f°", maxArr.getDouble(i)));
                            tvMin.setText(String.format(Locale.US, "%.0f°", minArr.getDouble(i)));
                            layoutForecast.addView(item);
                        }

                        JSONObject hourly = response.getJSONObject("hourly");
                        JSONArray hTime = hourly.getJSONArray("time");
                        JSONArray hTemp = hourly.getJSONArray("temperature_2m");
                        JSONArray hCode = hourly.getJSONArray("weathercode");
                        JSONArray hWind = hourly.getJSONArray("windspeed_10m");
                        SimpleDateFormat nowFmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:00", Locale.US);
                        String nowStr = nowFmt.format(new Date());
                        int startIndex = 0;
                        for (int i = 0; i < hTime.length(); i++) { if (hTime.getString(i).compareTo(nowStr) >= 0) { startIndex = i; break; } }

                        List<HourlyForecastView.HourlyItem> hourlyList = new ArrayList<>();
                        for (int i = startIndex; i < startIndex + 24 && i < hTime.length(); i++) {
                            String t = hTime.getString(i).split("T")[1];
                            String formattedTime = t;
                            try { formattedTime = new SimpleDateFormat("hh a", new Locale("ar")).format(new SimpleDateFormat("HH:mm", Locale.US).parse(t)); } catch (Exception e) {}
                            hourlyList.add(new HourlyForecastView.HourlyItem(formattedTime, hTemp.getDouble(i), hCode.getInt(i), hWind.getDouble(i)));
                        }
                        hourlyForecastView.setData(hourlyList);
                        updateBackground(current.getInt("weathercode"), current.getInt("is_day"));
                    } catch (Exception e) { Log.e("Weather", "Error", e); }
                }, error -> Toast.makeText(this, "فشل الاتصال", Toast.LENGTH_SHORT).show());
        queue.add(request);
    }

    private void updateBackground(int code, int isDay) {
        int bgResId = isDay == 0 ? R.drawable.bg_night : (code == 0 || code == 1 ? R.drawable.bg_sunny : R.drawable.bg_cloudy);
        mainLayout.setBackground(ContextCompat.getDrawable(this, bgResId));
    }

    private String getWindDirectionName(double degrees) {
        String[] dirs = {"شمال", "شمال شرق", "شرق", "جنوب شرق", "جنوب", "جنوب غرب", "غرب", "شمال غرب"};
        return dirs[(int) Math.round((degrees % 360) / 45.0) % 8];
    }

    private String getWeatherDescription(int code) {
        if (code == 0) return "صافٍ";
        if (code <= 3) return "غائم";
        if (code <= 48) return "ضباب";
        if (code <= 67) return "مطر";
        if (code <= 77) return "ثلج";
        if (code <= 82) return "زخات مطر";
        if (code >= 95) return "عواصف رعدية";
        return "غير معروف";
    }

    private String getWeatherIcon(int code) {
        if (code == 0) return "☀️";
        if (code <= 3) return "⛅";
        if (code <= 48) return "🌫️";
        if (code <= 67) return "🌧️";
        if (code <= 77) return "❄️";
        if (code <= 82) return "🌦️";
        return "⛈️";
    }
}
