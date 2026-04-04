package com.example.inventoryapp;

import android.graphics.Color;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Reports extends AppCompatActivity {

    private PieChart importChart, exportChart;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState){

        super.onCreate(savedInstanceState);
        setContentView(R.layout.reports_analytics);

        Toolbar toolbar = findViewById(R.id.toolbarReports);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {

            getSupportActionBar().setTitle("Reports & Analytics");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        importChart = findViewById(R.id.importChart);
        exportChart = findViewById(R.id.exportChart);

        dbHelper = new DatabaseHelper(this);

        loadCharts();
    }


    private void loadCharts() {

        dbHelper.getAllItems(items -> {

            ArrayList<PieEntry> importEntries =
                    new ArrayList<>();

            ArrayList<PieEntry> exportEntries =
                    new ArrayList<>();

            for (InventoryItem item : items) {

                if (item.getQuantity() > 0) {

                    importEntries.add(
                            new PieEntry(
                                    item.getQuantity(),
                                    item.getName()
                            )
                    );
                }
            }

            FirebaseFirestore firestore =
                    FirebaseFirestore.getInstance();

            firestore.collection("sales")
                    .get()
                    .addOnSuccessListener(snapshot -> {

                        Map<String,Integer> totals =
                                new HashMap<>();

                        for (DocumentSnapshot doc : snapshot) {

                            String itemId =
                                    doc.getString("item_id");

                            Long qty =
                                    doc.getLong("quantity");

                            if (itemId != null && qty != null) {

                                totals.put(
                                        itemId,
                                        totals.getOrDefault(itemId, 0)
                                                + qty.intValue()
                                );
                            }
                        }

                        for (Map.Entry<String,Integer> entry :
                                totals.entrySet()) {

                            String itemName =
                                    getItemNameById(
                                            items,
                                            entry.getKey()
                                    );

                            exportEntries.add(
                                    new PieEntry(
                                            entry.getValue(),
                                            itemName
                                    )
                            );
                        }

                        setupChart(
                                importChart,
                                importEntries,
                                "Current Stock"
                        );

                        setupChart(
                                exportChart,
                                exportEntries,
                                "Items Sold"
                        );
                    });
        });
    }


    private void setupChart(
            PieChart chart,
            ArrayList<PieEntry> entries,
            String centerText
    ) {

        if (entries.isEmpty()) {

            chart.clear();
            chart.setNoDataText("No data available");
            chart.invalidate();

            return;
        }


        PieDataSet dataSet =
                new PieDataSet(entries, "");


        ArrayList<Integer> colors =
                new ArrayList<>();

        for (int c : ColorTemplate.MATERIAL_COLORS) colors.add(c);
        for (int c : ColorTemplate.COLORFUL_COLORS) colors.add(c);
        for (int c : ColorTemplate.JOYFUL_COLORS) colors.add(c);
        for (int c : ColorTemplate.LIBERTY_COLORS) colors.add(c);
        for (int c : ColorTemplate.PASTEL_COLORS) colors.add(c);


        dataSet.setColors(colors);
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(5f);

        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(13f);


        PieData data =
                new PieData(dataSet);


        chart.setData(data);

        chart.setUsePercentValues(true);
        chart.getDescription().setEnabled(false);

        chart.setCenterText(centerText);
        chart.setCenterTextSize(16f);

        chart.setEntryLabelColor(Color.BLACK);
        chart.setEntryLabelTextSize(12f);

        chart.animateY(1200);


        Legend legend =
                chart.getLegend();

        legend.setEnabled(true);
        legend.setTextColor(Color.BLACK);
        legend.setTextSize(12f);

        chart.invalidate();
    }


    private String getItemNameById(
            List<InventoryItem> items,
            String id
    ) {

        for (InventoryItem item : items) {

            if (item.getId().equals(id))
                return item.getName();
        }

        return "Unknown";
    }


    @Override
    public boolean onSupportNavigateUp() {

        finish();
        return true;
    }
}