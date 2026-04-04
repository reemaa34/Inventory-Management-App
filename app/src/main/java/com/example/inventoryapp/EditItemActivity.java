package com.example.inventoryapp;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class EditItemActivity extends AppCompatActivity {

    private EditText etItemName, etQuantity, etPrice, etDescription, etMinStock;
    private Spinner spinnerCategory;
    private Button btnUpdate, btnCancel;
    private DatabaseHelper dbHelper;
    private InventoryItem currentItem;

    private final String[] categories = {"Electronics", "Clothing", "Food & Beverage", "Furniture",
            "Tools", "Stationery", "Medicine", "Sports", "Toys", "Other"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_item);

        dbHelper = new DatabaseHelper(this);
        currentItem = (InventoryItem) getIntent().getSerializableExtra("item");

        if (currentItem == null) {
            finish();
            return;
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Edit Item");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        etItemName = findViewById(R.id.etItemName);
        etQuantity = findViewById(R.id.etQuantity);
        etPrice = findViewById(R.id.etPrice);
        etDescription = findViewById(R.id.etDescription);
        etMinStock = findViewById(R.id.etMinStock);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        btnUpdate = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);

        btnUpdate.setText("Update Item");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);

        // Pre-fill existing data
        etItemName.setText(currentItem.getName());
        etQuantity.setText(String.valueOf(currentItem.getQuantity()));
        etPrice.setText(String.valueOf(currentItem.getPrice()));
        etDescription.setText(currentItem.getDescription());
        etMinStock.setText(String.valueOf(currentItem.getMinStock()));

        // Set spinner to current category
        if (currentItem.getCategory() != null) {
            for (int i = 0; i < categories.length; i++) {
                if (categories[i].equals(currentItem.getCategory())) {
                    spinnerCategory.setSelection(i);
                    break;
                }
            }
        }

        btnUpdate.setOnClickListener(v -> updateItem());
        btnCancel.setOnClickListener(v -> finish());
    }

    private void updateItem() {
        String name = etItemName.getText().toString().trim();
        String quantityStr = etQuantity.getText().toString().trim();
        String priceStr = etPrice.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String minStockStr = etMinStock.getText().toString().trim();
        String category = spinnerCategory.getSelectedItem().toString();

        if (TextUtils.isEmpty(name)) {
            etItemName.setError("Item name is required");
            etItemName.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(quantityStr)) {
            etQuantity.setError("Quantity is required");
            etQuantity.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(priceStr)) {
            etPrice.setError("Price is required");
            etPrice.requestFocus();
            return;
        }

        currentItem.setName(name);
        currentItem.setCategory(category);
        currentItem.setQuantity(Integer.parseInt(quantityStr));
        currentItem.setPrice(Double.parseDouble(priceStr));
        currentItem.setDescription(description);
        currentItem.setMinStock(TextUtils.isEmpty(minStockStr) ? 5 : Integer.parseInt(minStockStr));

        dbHelper.updateItem(currentItem, success -> {

            if (success) {

                Toast.makeText(this,
                        "Item updated successfully!",
                        Toast.LENGTH_SHORT).show();

                setResult(RESULT_OK);
                finish();

            } else {

                Toast.makeText(this,
                        "Failed to update item",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
