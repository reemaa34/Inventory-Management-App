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

public class AddItemActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST = 101;

    private EditText etBarcode, etItemName, etQuantity, etPrice, etDescription, etMinStock;
    private Spinner spinnerCategory;
    private Button btnSave, btnCancel;
    private TextInputLayout tilBarcode;
    private DatabaseHelper dbHelper;
    private String[] categories = {"Electronics", "Clothing", "Food & Beverage", "Furniture",
            "Tools", "Stationery", "Medicine", "Sports", "Toys", "Other"};

    // ── Feature 1: ZXing barcode scanner launcher ──
    private final ActivityResultLauncher<ScanOptions> barcodeLauncher =
            registerForActivityResult(new ScanContract(), this::onScanResult);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_item);

        dbHelper = new DatabaseHelper(this);

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

        btnSave.setOnClickListener(v -> saveItem());
        btnCancel.setOnClickListener(v -> finish());

        // ── Feature 1: Launch scanner when scan button tapped ──
        tilBarcode.setEndIconOnClickListener(v -> launchScanner());

        setupBottomNavigation();
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);
        bottomNavigation.setSelectedItemId(R.id.nav_add);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_add) {
                return true;
            } else if (id == R.id.nav_home) {
                startActivity(new Intent(this, DashboardActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_sell) {
                startActivity(new Intent(this, SellStockActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_reports) {
                startActivity(new Intent(this, Reports.class));
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

    // ── Feature 1: Request camera permission then launch ZXing ──
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
        options.setPrompt("Scan a barcode or QR code");
        options.setBeepEnabled(true);
        options.setOrientationLocked(false);
        options.setBarcodeImageEnabled(false);
        barcodeLauncher.launch(options);
    }

    // ── Feature 1: Handle scan result ──
    private void onScanResult(ScanIntentResult result) {
        if (result.getContents() != null) {
            String scannedBarcode = result.getContents();
            etBarcode.setText(scannedBarcode);
            autoFillProductDetails(scannedBarcode);
            Toast.makeText(this, "Barcode scanned: " + scannedBarcode,
                    Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Scan cancelled", Toast.LENGTH_SHORT).show();
        }
    }

    private void autoFillProductDetails(String barcode) {
        dbHelper.getItemByBarcode(barcode, item -> {
            if (item != null) {
                etItemName.setText(item.getName());
                etPrice.setText(String.valueOf(item.getPrice()));
                
                // Set category spinner
                if (item.getCategory() != null) {
                    for (int i = 0; i < categories.length; i++) {
                        if (categories[i].equals(item.getCategory())) {
                            spinnerCategory.setSelection(i);
                            break;
                        }
                    }
                }
                
                if (item.getDescription() != null) {
                    etDescription.setText(item.getDescription());
                }
                
                Toast.makeText(this, "Product details auto-filled!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startBarcodeScanner();
            } else {
                Toast.makeText(this,
                        "Camera permission is required to scan barcodes",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private void saveItem() {
        String barcode     = etBarcode.getText().toString().trim();
        String name        = etItemName.getText().toString().trim();
        String quantityStr = etQuantity.getText().toString().trim();
        String priceStr    = etPrice.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String minStockStr = etMinStock.getText().toString().trim();
        String category    = spinnerCategory.getSelectedItem().toString();

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

        int quantity = Integer.parseInt(quantityStr);
        double price = Double.parseDouble(priceStr);
        int minStock = TextUtils.isEmpty(minStockStr) ? 5 : Integer.parseInt(minStockStr);

        String createdAt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(new Date());

        InventoryItem item = new InventoryItem(
                name, barcode, category, quantity, price,
                description, minStock, createdAt, null
        );

        dbHelper.addItem(item, success -> {
            if (success) {
                Toast.makeText(this, "Item added successfully!", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            } else {
                Toast.makeText(this, "Failed to add item", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
