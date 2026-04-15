package com.example.inventoryapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.text.SimpleDateFormat;
import java.util.*;

public class SellStockActivity extends AppCompatActivity {

    private Spinner spinnerCategory, spinnerItems;
    private EditText etQuantity, etCustomer;
    private Button btnSell;

    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;

    private List<InventoryItem> filteredItems;

    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sell_stock);

        Toolbar toolbar = findViewById(R.id.toolbarSell);
        setSupportActionBar(toolbar);

        dbHelper = new DatabaseHelper(this);
        sessionManager = new SessionManager(this);

        spinnerCategory = findViewById(R.id.spinnerCategory);
        spinnerItems = findViewById(R.id.spinnerItems);
        etQuantity = findViewById(R.id.etSellQuantity);
        etCustomer = findViewById(R.id.etCustomer);
        btnSell = findViewById(R.id.btnSellStock);

        loadCategories();

        spinnerCategory.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {

                    public void onItemSelected(
                            AdapterView<?> parent,
                            android.view.View view,
                            int position,
                            long id) {

                        loadItemsByCategory(
                                spinnerCategory
                                        .getSelectedItem()
                                        .toString()
                        );
                    }

                    public void onNothingSelected(
                            AdapterView<?> parent) {}
                });

        btnSell.setOnClickListener(v -> sellItem());
        
        setupBottomNavigation();
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);
        bottomNavigation.setSelectedItemId(R.id.nav_sell);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, DashboardActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_sell) {
                return true;
            } else if (id == R.id.nav_reports) {
                startActivity(new Intent(this, ReportsActivity.class));
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
            }
            return false;
        });
    }

    private void loadCategories() {

        dbHelper.getAllCategories(categories -> {

            spinnerCategory.setAdapter(
                    new ArrayAdapter<>(
                            this,
                            android.R.layout.simple_spinner_dropdown_item,
                            categories
                    )
            );
        });
    }

    private void loadItemsByCategory(String category) {

        dbHelper.getItemsByCategory(category, items -> {

            filteredItems = items;

            String[] names =
                    new String[items.size()];

            for (int i = 0; i < items.size(); i++)
                names[i] = items.get(i).getName();

            spinnerItems.setAdapter(
                    new ArrayAdapter<>(
                            this,
                            android.R.layout.simple_spinner_dropdown_item,
                            names
                    )
            );
        });
    }

    private void sellItem() {

        String qtyStr =
                etQuantity.getText().toString();

        if (TextUtils.isEmpty(qtyStr))
            return;

        int qty =
                Integer.parseInt(qtyStr);

        int selectedPos = spinnerItems.getSelectedItemPosition();
        if (selectedPos < 0 || filteredItems == null || filteredItems.isEmpty()) {
            return;
        }

        InventoryItem selected = filteredItems.get(selectedPos);

        if (qty > selected.getQuantity()) {

            Toast.makeText(
                    this,
                    "Not enough stock",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        selected.setQuantity(
                selected.getQuantity() - qty
        );

        String timestamp =
                new SimpleDateFormat(
                        "yyyy-MM-dd HH:mm:ss",
                        Locale.getDefault()
                ).format(new Date());

        dbHelper.updateItem(selected, success -> {

            if (!success) {

                Toast.makeText(
                        this,
                        "Failed to update stock",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            dbHelper.insertSale(
                    selected.getId(),
                    selected.getName(),
                    qty,
                    selected.getPrice(),          // Bug 2 fix: capture price at time of sale
                    etCustomer.getText().toString(),
                    sessionManager.getUsername(),
                    timestamp,
                    saleSuccess -> {

                        if (!saleSuccess) {

                            Toast.makeText(
                                    this,
                                    "Sale failed",
                                    Toast.LENGTH_SHORT
                            ).show();

                            return;
                        }

                        // Audit Log: Item Sold
                        AuditLog auditLog = new AuditLog(
                                "SOLD",
                                selected.getName(),
                                selected.getId(),
                                qty,
                                sessionManager.getUsername(),
                                sessionManager.getEmail(),
                                timestamp,
                                "Sold to: " + etCustomer.getText().toString()
                        );
                        dbHelper.insertAuditLog(auditLog, auditSuccess -> {
                            Toast.makeText(
                                    this,
                                    "Sale recorded",
                                    Toast.LENGTH_SHORT
                            ).show();
                            finish();
                        });
                    });

        });
    }
}
