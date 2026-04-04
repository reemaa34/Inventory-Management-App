package com.example.inventoryapp;

import android.os.Bundle;
import android.os.Environment;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

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
    }


    private void loadAuditData() {

        dbHelper.getTotalInventoryValue(stockValue -> {

            totalStockValue = stockValue;

            dbHelper.getTotalRevenue(revenue -> {

                totalRevenue = revenue;
                profitLoss = totalRevenue - totalStockValue;

                addRow("Total Inventory Value", totalStockValue);
                addRow("Total Revenue", totalRevenue);
                addRow("Net Profit / Loss", profitLoss);
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