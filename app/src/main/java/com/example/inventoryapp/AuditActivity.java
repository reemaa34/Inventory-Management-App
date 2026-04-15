package com.example.inventoryapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.File;
import java.io.FileOutputStream;

public class AuditActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private TableLayout auditTable;

    private double totalStockValue = 0;
    private double totalRevenue = 0;
    private double profitLoss = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audit);

        dbHelper = new DatabaseHelper(this);

        auditTable = findViewById(R.id.auditTable);

        Button exportBtn =
                findViewById(R.id.btnExportExcel);

        loadAuditData();

        exportBtn.setOnClickListener(
                v -> exportExcel()
        );

        setupBottomNavigation();
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_reports) {
                startActivity(new Intent(this, ReportsActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_sell) {
                startActivity(new Intent(this, SellStockActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_add) {
                startActivity(new Intent(this, AddItemActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_home) {
                startActivity(new Intent(this, DashboardActivity.class));
                finish();
                return true;
            }
            return false;
        });
    }

    private void loadAuditData() {

        dbHelper.getTotalInventoryValue(stockValue -> {

            totalStockValue = stockValue;

            dbHelper.getTotalRevenue(revenue -> {

                totalRevenue = revenue;

                profitLoss = totalRevenue - totalStockValue;

                addRow("Current Inventory Value",  totalStockValue);
                addRow("Total Sales Revenue",       totalRevenue);
                addRow("Revenue vs Stock Difference", profitLoss);
            });
        });
    }


    private void addRow(String label,
                        double value) {

        TableRow row =
                new TableRow(this);

        TextView col1 =
                new TextView(this);

        TextView col2 =
                new TextView(this);

        col1.setText(label);
        col2.setText(String.valueOf(value));

        col1.setPadding(20,20,20,20);
        col2.setPadding(20,20,20,20);

        row.addView(col1);
        row.addView(col2);

        auditTable.addView(row);
    }


    private void exportExcel() {

        try {

            File file =
                    new File(
                            getExternalFilesDir(null),
                            "audit_report.xls"
                    );

            FileOutputStream fos =
                    new FileOutputStream(file);

            String data =
                    "Metric\tValue\n"
                            + "Inventory Value\t"
                            + totalStockValue + "\n"
                            + "Revenue\t"
                            + totalRevenue + "\n"
                            + "Profit/Loss\t"
                            + profitLoss;

            fos.write(data.getBytes());

            fos.close();

            Toast.makeText(
                    this,
                    "Excel exported to Downloads",
                    Toast.LENGTH_LONG
            ).show();

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Export failed",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }
}
