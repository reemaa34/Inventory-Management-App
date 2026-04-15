package com.example.inventoryapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.textfield.TextInputLayout;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanIntentResult;
import com.journeyapps.barcodescanner.ScanOptions;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class EditItemActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST = 101;

    private EditText etBarcode, etItemName, etQuantity, etPrice, etDescription, etMinStock;
    private Spinner spinnerCategory;
    private Button btnSave, btnCancel;
    private TextInputLayout tilBarcode;
    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;
    private InventoryItem itemToEdit;

    private String[] categories = {"Electronics", "Clothing", "Food & Beverage", "Furniture",
            "Tools", "Stationery", "Medicine", "Sports", "Toys", "Other"};

    private final ActivityResultLauncher<ScanOptions> barcodeLauncher =
            registerForActivityResult(new ScanContract(), this::onScanResult);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_item);

        dbHelper = new DatabaseHelper(this);
        sessionManager = new SessionManager(this);

        itemToEdit = (InventoryItem) getIntent().getSerializableExtra("item");

        etBarcode     = findViewById(R.id.etBarcode);
        etItemName    = findViewById(R.id.etItemName);
        etQuantity    = findViewById(R.id.etQuantity);
        etPrice       = findViewById(R.id.etPrice);
        etDescription = findViewById(R.id.etDescription);
        etMinStock    = findViewById(R.id.etMinStock);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        btnSave       = findViewById(R.id.btnSave);
        btnCancel     = findViewById(R.id.btnCancel);
        tilBarcode    = findViewById(R.id.tilBarcode);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);

        if (itemToEdit != null) {
            etBarcode.setText(itemToEdit.getBarcode());
            etItemName.setText(itemToEdit.getName());
            etQuantity.setText(String.valueOf(itemToEdit.getQuantity()));
            etPrice.setText(String.valueOf(itemToEdit.getPrice()));
            etDescription.setText(itemToEdit.getDescription());
            etMinStock.setText(String.valueOf(itemToEdit.getMinStock()));

            for (int i = 0; i < categories.length; i++) {
                if (categories[i].equals(itemToEdit.getCategory())) {
                    spinnerCategory.setSelection(i);
                    break;
                }
            }
        }

        btnSave.setOnClickListener(v -> updateItem());
        btnCancel.setOnClickListener(v -> finish());
        tilBarcode.setEndIconOnClickListener(v -> launchScanner());

        setupBottomNavigation();
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, DashboardActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_sell) {
                startActivity(new Intent(this, SellStockActivity.class));
                finish();
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

    private void launchScanner() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST);
        } else {
            startBarcodeScanner();
        }
    }

    private void startBarcodeScanner() {
        ScanOptions options = new ScanOptions();
        options.setPrompt("Scan a barcode");
        options.setBeepEnabled(true);
        options.setOrientationLocked(false);
        barcodeLauncher.launch(options);
    }

    private void onScanResult(ScanIntentResult result) {
        if (result.getContents() != null) {
            etBarcode.setText(result.getContents());
        }
    }

    private void updateItem() {
        String name = etItemName.getText().toString().trim();
        String qtyStr = etQuantity.getText().toString().trim();
        String priceStr = etPrice.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(qtyStr) || TextUtils.isEmpty(priceStr)) {
            Toast.makeText(this, "Please fill required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        int oldQty = itemToEdit.getQuantity();
        int newQty = Integer.parseInt(qtyStr);
        
        itemToEdit.setName(name);
        itemToEdit.setBarcode(etBarcode.getText().toString().trim());
        itemToEdit.setQuantity(newQty);
        itemToEdit.setPrice(Double.parseDouble(priceStr));
        itemToEdit.setCategory(spinnerCategory.getSelectedItem().toString());
        itemToEdit.setDescription(etDescription.getText().toString().trim());
        itemToEdit.setMinStock(TextUtils.isEmpty(etMinStock.getText()) ? 5 : Integer.parseInt(etMinStock.getText().toString()));

        dbHelper.updateItem(itemToEdit, success -> {
            if (success) {
                String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        .format(new Date());

                String details = "Updated item details. Quantity changed from " + oldQty + " to " + newQty;
                
                AuditLog auditLog = new AuditLog(
                        "UPDATED",
                        itemToEdit.getName(),
                        itemToEdit.getId(),
                        newQty,
                        sessionManager.getUsername(),
                        sessionManager.getEmail(),
                        timestamp,
                        details
                );
                
                dbHelper.insertAuditLog(auditLog, auditSuccess -> {
                    Toast.makeText(this, "Item updated", Toast.LENGTH_SHORT).show();
                    finish();
                });
            } else {
                Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
