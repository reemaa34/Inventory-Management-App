package com.example.inventoryapp;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class ReportsActivity extends AppCompatActivity {

    private LineChart lineChart;
    private BarChart barChart;
    private HorizontalBarChart horizontalBarChart;
    private MaterialButtonToggleGroup toggleGroup;
    private MaterialButton btnSale, btnProduct;
    private TextView tvTotalRevenue, tvTotalUnits, tvBestCategory, tvChartTitle, tvSecondChartTitle;
    private DatabaseHelper dbHelper;
    private List<InventoryItem> itemList;
    private List<SaleLog> saleLogs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports);

        dbHelper = new DatabaseHelper(this);

        lineChart = findViewById(R.id.lineChart);
        barChart = findViewById(R.id.barChart);
        horizontalBarChart = findViewById(R.id.horizontalBarChart);
        toggleGroup = findViewById(R.id.toggleGroup);
        btnSale = findViewById(R.id.btnSale);
        btnProduct = findViewById(R.id.btnProduct);

        tvTotalRevenue = findViewById(R.id.tvTotalRevenue);
        tvTotalUnits = findViewById(R.id.tvTotalUnits);
        tvBestCategory = findViewById(R.id.tvBestCategory);
        tvChartTitle = findViewById(R.id.tvChartTitle);
        tvSecondChartTitle = findViewById(R.id.tvSecondChartTitle);

        findViewById(R.id.btnExportReport).setOnClickListener(v -> exportInventoryToCsv());

        setupToggleLogic();
        setupBottomNavigation();
        loadData();
    }

    private void loadData() {
        dbHelper.getAllItems(items -> {
            this.itemList = items;
            dbHelper.getSaleLogs(logs -> {
                this.saleLogs = logs;
                updateSummaryStats();
                showSalesGrowth();
            });
        });
    }

    private void updateSummaryStats() {
        double totalRevenue = 0;
        int totalUnits = 0;
        Map<String, Integer> categoryVolume = new HashMap<>();

        if (saleLogs != null) {
            for (SaleLog log : saleLogs) {
                totalRevenue += (log.getQuantity() * log.getPriceAtSale());
                totalUnits += log.getQuantity();
                
                String category = "Unknown";
                if (itemList != null) {
                    for (InventoryItem item : itemList) {
                        if (item.getId() != null && item.getId().equals(log.getItemId())) {
                            category = item.getCategory() != null ? item.getCategory() : "Unknown";
                            break;
                        }
                    }
                }
                categoryVolume.put(category, categoryVolume.getOrDefault(category, 0) + log.getQuantity());
            }
        }

        tvTotalRevenue.setText(String.format(Locale.getDefault(), "₹%.0f", totalRevenue));
        tvTotalUnits.setText(String.valueOf(totalUnits));

        String bestCat = "None";
        int maxVol = 0;
        for (Map.Entry<String, Integer> entry : categoryVolume.entrySet()) {
            if (entry.getValue() > maxVol) {
                maxVol = entry.getValue();
                bestCat = entry.getKey();
            }
        }
        tvBestCategory.setText(bestCat);
    }

    private void setupToggleLogic() {
        updateToggleStyles(R.id.btnSale);
        toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                updateToggleStyles(checkedId);
                if (checkedId == R.id.btnSale) {
                    showSalesGrowth();
                } else {
                    showInventoryHealth();
                }
            }
        });
    }

    private void showSalesGrowth() {
        tvChartTitle.setText("Sales Performance (Last 7 Days)");
        lineChart.setVisibility(View.VISIBLE);
        barChart.setVisibility(View.GONE);

        Map<String, Double> dailySales = new TreeMap<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar cal = Calendar.getInstance();
        for (int i = 0; i < 7; i++) {
            dailySales.put(sdf.format(cal.getTime()), 0.0);
            cal.add(Calendar.DATE, -1);
        }

        if (saleLogs != null) {
            for (SaleLog log : saleLogs) {
                try {
                    if (log.getTimestamp() != null) {
                        String date = log.getTimestamp().split(" ")[0];
                        if (dailySales.containsKey(date)) {
                            dailySales.put(date, dailySales.get(date) + (log.getQuantity() * log.getPriceAtSale()));
                        }
                    }
                } catch (Exception ignored) {}
            }
        }

        List<Entry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        int index = 0;
        for (Map.Entry<String, Double> entry : dailySales.entrySet()) {
            entries.add(new Entry(index, entry.getValue().floatValue()));
            String dateKey = entry.getKey();
            String label = (dateKey != null && dateKey.length() >= 10) ? dateKey.substring(5) : "";
            labels.add(label);
            index++;
        }

        LineDataSet dataSet = new LineDataSet(entries, "Revenue");
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setColor(ContextCompat.getColor(this, R.color.chart_orange));
        dataSet.setLineWidth(3f);
        dataSet.setDrawFilled(true);
        dataSet.setDrawCircles(true);
        dataSet.setCircleColor(ContextCompat.getColor(this, R.color.chart_orange));
        dataSet.setFillDrawable(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, 
                new int[]{Color.argb(100, 255, 152, 0), Color.TRANSPARENT}));

        lineChart.setData(new LineData(dataSet));
        lineChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        lineChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        lineChart.getXAxis().setGranularity(1f);
        lineChart.getAxisRight().setEnabled(false);
        lineChart.getDescription().setEnabled(false);
        lineChart.invalidate();

        showTopProducts();
    }

    private void showInventoryHealth() {
        tvChartTitle.setText("Stock Levels by Category");
        lineChart.setVisibility(View.GONE);
        barChart.setVisibility(View.VISIBLE);

        Map<String, Integer> catStock = new HashMap<>();
        if (itemList != null) {
            for (InventoryItem item : itemList) {
                String category = item.getCategory() != null ? item.getCategory().trim() : "Other";
                catStock.put(category, catStock.getOrDefault(category, 0) + item.getQuantity());
            }
        }

        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        int index = 0;
        List<String> keys = new ArrayList<>(catStock.keySet());
        Collections.sort(keys);
        for (String category : keys) {
            entries.add(new BarEntry(index, catStock.get(category)));
            labels.add(category);
            index++;
        }

        BarDataSet dataSet = new BarDataSet(entries, "Stock Count");
        dataSet.setColor(ContextCompat.getColor(this, R.color.chart_blue));
        
        barChart.setData(new BarData(dataSet));
        barChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        barChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        barChart.getXAxis().setGranularity(1f);
        barChart.getAxisRight().setEnabled(false);
        barChart.getDescription().setEnabled(false);
        barChart.invalidate();

        showLowStockInsights();
    }

    private void showTopProducts() {
        tvSecondChartTitle.setText("Top 5 Products by Revenue");
        
        Map<String, Double> revenueMap = new HashMap<>();
        Map<String, String> displayNames = new HashMap<>();

        if (saleLogs != null) {
            for (SaleLog log : saleLogs) {
                String name = log.getItemName();
                // Lookup name by ID if missing or generic
                if (name == null || name.isEmpty() || name.equalsIgnoreCase("Unknown")) {
                    if (itemList != null) {
                        for (InventoryItem item : itemList) {
                            if (item.getId() != null && item.getId().equals(log.getItemId())) {
                                name = item.getName();
                                break;
                            }
                        }
                    }
                }
                
                if (name == null || name.isEmpty()) name = "Unknown Product";
                
                String cleanName = name.trim();
                String key = cleanName.toLowerCase();
                
                double rev = log.getQuantity() * log.getPriceAtSale();
                revenueMap.put(key, revenueMap.getOrDefault(key, 0.0) + rev);
                
                // Keep the original casing for the first non-unknown name found
                if (!displayNames.containsKey(key) || displayNames.get(key).equalsIgnoreCase("Unknown Product")) {
                    displayNames.put(key, cleanName);
                }
            }
        }

        List<Map.Entry<String, Double>> sortedList = new ArrayList<>(revenueMap.entrySet());
        sortedList.sort((o1, o2) -> o2.getValue().compareTo(o1.getValue()));

        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        
        int count = Math.min(5, sortedList.size());
        // Labels list must match the x-indices 0 to count-1
        // We want Top 1 at the top of the chart (highest x-value)
        String[] labelArray = new String[count];
        
        for (int i = 0; i < count; i++) {
            Map.Entry<String, Double> entry = sortedList.get(i);
            float xPos = count - 1 - i;
            entries.add(new BarEntry(xPos, entry.getValue().floatValue()));
            labelArray[(int)xPos] = displayNames.get(entry.getKey());
        }
        
        for (String l : labelArray) labels.add(l != null ? l : "");

        BarDataSet dataSet = new BarDataSet(entries, "Revenue");
        dataSet.setColor(ContextCompat.getColor(this, R.color.chart_green));
        
        horizontalBarChart.setData(new BarData(dataSet));
        horizontalBarChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        horizontalBarChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        horizontalBarChart.getXAxis().setGranularity(1f);
        horizontalBarChart.getAxisRight().setEnabled(false);
        horizontalBarChart.getDescription().setEnabled(false);
        horizontalBarChart.invalidate();
    }

    private void showLowStockInsights() {
        tvSecondChartTitle.setText("Immediate Reorder Priority");
        List<InventoryItem> lowStock = new ArrayList<>();
        if (itemList != null) {
            for(InventoryItem item : itemList) {
                if(item.getQuantity() <= item.getMinStock()) lowStock.add(item);
            }
        }
        // Sort by quantity ascending (most critical first)
        lowStock.sort((o1, o2) -> Integer.compare(o1.getQuantity(), o2.getQuantity()));

        List<BarEntry> entries = new ArrayList<>();
        int count = Math.min(5, lowStock.size());
        String[] labelArray = new String[count];

        for (int i = 0; i < count; i++) {
            InventoryItem item = lowStock.get(i);
            float xPos = count - 1 - i;
            entries.add(new BarEntry(xPos, item.getQuantity()));
            labelArray[(int)xPos] = item.getName() != null ? item.getName() : "Unknown";
        }

        List<String> labels = new ArrayList<>();
        for (String l : labelArray) labels.add(l);

        BarDataSet dataSet = new BarDataSet(entries, "Current Qty");
        dataSet.setColor(Color.parseColor("#E53935")); // Material Red
        
        horizontalBarChart.setData(new BarData(dataSet));
        horizontalBarChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        horizontalBarChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        horizontalBarChart.getXAxis().setGranularity(1f);
        horizontalBarChart.getAxisRight().setEnabled(false);
        horizontalBarChart.getDescription().setEnabled(false);
        horizontalBarChart.invalidate();
    }

    private void updateToggleStyles(int checkedId) {
        int activeColor = ContextCompat.getColor(this, R.color.chart_orange);
        int inactiveColor = Color.WHITE;
        if (checkedId == R.id.btnSale) {
            btnSale.setBackgroundTintList(ColorStateList.valueOf(activeColor));
            btnSale.setTextColor(Color.WHITE);
            btnProduct.setBackgroundTintList(ColorStateList.valueOf(inactiveColor));
            btnProduct.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        } else {
            btnProduct.setBackgroundTintList(ColorStateList.valueOf(activeColor));
            btnProduct.setTextColor(Color.WHITE);
            btnSale.setBackgroundTintList(ColorStateList.valueOf(inactiveColor));
            btnSale.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        }
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);
        bottomNavigation.setSelectedItemId(R.id.nav_reports);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_reports) return true;
            Class<?> targetActivity = null;
            if (id == R.id.nav_home) targetActivity = DashboardActivity.class;
            else if (id == R.id.nav_sell) targetActivity = SellStockActivity.class;
            else if (id == R.id.nav_add) targetActivity = AddItemActivity.class;
            else if (id == R.id.nav_settings) targetActivity = SettingsActivity.class;
            if (targetActivity != null) {
                startActivity(new Intent(this, targetActivity));
                finish();
                return true;
            }
            return false;
        });
    }

    public void exportInventoryToCsv() {
        if (itemList == null || itemList.isEmpty()) {
            Toast.makeText(this, "No data to export", Toast.LENGTH_SHORT).show();
            return;
        }
        StringBuilder csvContent = new StringBuilder();
        csvContent.append("ID,Name,Category,Quantity,Price,MinStock,Status\n");
        for (InventoryItem item : itemList) {
            String status = item.getQuantity() == 0 ? "Out of Stock" : (item.getQuantity() <= item.getMinStock() ? "Low Stock" : "Normal");
            csvContent.append(item.getId()).append(",")
                    .append(item.getName()).append(",")
                    .append(item.getCategory()).append(",")
                    .append(item.getQuantity()).append(",")
                    .append(item.getPrice()).append(",")
                    .append(item.getMinStock()).append(",")
                    .append(status).append("\n");
        }
        try {
            File file = new File(getExternalFilesDir(null), "inventory_analysis.csv");
            FileOutputStream out = new FileOutputStream(file);
            out.write(csvContent.toString().getBytes());
            out.close();
            Uri path = FileProvider.getUriForFile(this, "com.example.inventoryapp.fileprovider", file);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/csv");
            intent.putExtra(Intent.EXTRA_STREAM, path);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Share Analysis"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
