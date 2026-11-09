package com.weatherradar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONArray;
import org.json.JSONObject;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class SearchCityActivity extends AppCompatActivity {

    private EditText etSearch;
    private RecyclerView rvCities;
    private TextView tvNoResults;
    private CityAdapter adapter;
    private List<CityAdapter.City> cityList;
    private RequestQueue queue;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_city);

        etSearch = findViewById(R.id.etSearch);
        rvCities = findViewById(R.id.rvCities);
        tvNoResults = findViewById(R.id.tvNoResults);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        queue = Volley.newRequestQueue(this);
        cityList = new ArrayList<>();

        adapter = new CityAdapter(cityList, city -> {
            Intent returnIntent = new Intent();
            returnIntent.putExtra("city_name", city.name);
            returnIntent.putExtra("lat", city.lat);
            returnIntent.putExtra("lon", city.lon);
            setResult(RESULT_OK, returnIntent);
            finish();
        });

        rvCities.setLayoutManager(new LinearLayoutManager(this));
        rvCities.setAdapter(adapter);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (searchRunnable != null) handler.removeCallbacks(searchRunnable);
                searchRunnable = () -> searchCity(s.toString());
                handler.postDelayed(searchRunnable, 500);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void searchCity(String query) {
        if (query.trim().length() < 2) {
            cityList.clear();
            adapter.notifyDataSetChanged();
            tvNoResults.setVisibility(View.GONE);
            return;
        }

        try {
            String encodedQuery = URLEncoder.encode(query.trim(), "UTF-8");
            String url = "https://geocoding-api.open-meteo.com/v1/search?name=" + encodedQuery + "&count=10&language=ar&format=json";

            JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                    response -> {
                        cityList.clear();
                        try {
                            if (response.has("results")) {
                                JSONArray results = response.getJSONArray("results");
                                for (int i = 0; i < results.length(); i++) {
                                    JSONObject cityObj = results.getJSONObject(i);
                                    cityList.add(new CityAdapter.City(
                                        cityObj.getString("name"),
                                        cityObj.optString("country", ""),
                                        cityObj.getDouble("latitude"),
                                        cityObj.getDouble("longitude")
                                    ));
                                }
                                tvNoResults.setVisibility(View.GONE);
                            } else {
                                tvNoResults.setVisibility(View.VISIBLE);
                            }
                        } catch (Exception e) {
                            Log.e("SearchError", "Error parsing JSON", e);
                            tvNoResults.setVisibility(View.VISIBLE);
                        }
                        adapter.notifyDataSetChanged();
                    }, 
                    error -> {
                        Log.e("SearchError", "Volley error", error);
                        tvNoResults.setVisibility(View.VISIBLE);
                    });
            queue.add(request);
        } catch (Exception e) {
            Log.e("SearchError", "Encoding error", e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null && searchRunnable != null) handler.removeCallbacks(searchRunnable);
    }
}
