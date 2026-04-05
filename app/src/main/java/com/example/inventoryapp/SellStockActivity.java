package com.example.inventoryapp;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

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

        InventoryItem selected =
                filteredItems.get(
                        spinnerItems.getSelectedItemPosition()
                );

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

                        Toast.makeText(
                                this,
                                "Sale recorded",
                                Toast.LENGTH_SHORT
                        ).show();

                        finish();
                    });

        });
    }
}