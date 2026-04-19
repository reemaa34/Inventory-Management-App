package com.example.inventoryapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.textfield.TextInputLayout;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanIntentResult;
import com.journeyapps.barcodescanner.ScanOptions;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AddItemActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST = 101;
    private static final int CAMERA_PERMISSION_FOR_IMAGE = 102;

    private EditText etBarcode, etItemName, etQuantity, etPrice, etDescription, etMinStock;
    private Spinner spinnerCategory;
    private Button btnSave, btnCancel, btnSelectImage;
    private ImageView ivProductPreview;
    private TextInputLayout tilBarcode;
    private DatabaseHelper dbHelper;
    private String[] categories = {"Electronics", "Clothing", "Food & Beverage", "Furniture",
            "Tools", "Stationery", "Medicine", "Sports", "Toys", "Other"};

    // Selected image URI (from gallery or camera)
    private Uri selectedImageUri = null;
    // URI for the camera-captured photo
    private Uri cameraImageUri = null;

    // ── Feature 1: ZXing barcode scanner launcher ──
    private final ActivityResultLauncher<ScanOptions> barcodeLauncher =
            registerForActivityResult(new ScanContract(), this::onScanResult);

    // ── Gallery image picker launcher ──
    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            selectedImageUri = result.getData().getData();
                            if (selectedImageUri != null) {
                                Glide.with(this)
                                        .load(selectedImageUri)
                                        .centerCrop()
                                        .into(ivProductPreview);
                            }
                        }
                    }
            );

    // ── Camera capture launcher ──
    private final ActivityResultLauncher<Uri> cameraLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.TakePicture(),
                    success -> {
                        if (success && cameraImageUri != null) {
                            selectedImageUri = cameraImageUri;
                            Glide.with(this)
                                    .load(selectedImageUri)
                                    .centerCrop()
                                    .into(ivProductPreview);
                        }
                    }
            );

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
        ivProductPreview = findViewById(R.id.ivProductPreview);
        btnSelectImage   = findViewById(R.id.btnSelectImage);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);

        btnSave.setOnClickListener(v -> saveItem());
        btnCancel.setOnClickListener(v -> finish());
        btnSelectImage.setOnClickListener(v -> showImageSourceDialog());

        // ── Feature 1: Launch scanner when scan button tapped ──
        tilBarcode.setEndIconOnClickListener(v -> launchScanner());

        setupBottomNavigation();
    }

    /**
     * Shows a dialog letting the user choose between Camera and Gallery.
     */
    private void showImageSourceDialog() {
        String[] options = {"📷  Take Photo", "🖼️  Choose from Gallery"};

        new AlertDialog.Builder(this)
                .setTitle("Select Image")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        // Camera
                        openCamera();
                    } else {
                        // Gallery
                        openGallery();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Opens the camera to capture a product image.
     */
    private void openCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_FOR_IMAGE);
        } else {
            launchCamera();
        }
    }

    private void launchCamera() {
        // Create a temp file in cache for the camera to write to
        File photoFile = new File(getCacheDir(), "product_photo_" + System.currentTimeMillis() + ".jpg");
        cameraImageUri = FileProvider.getUriForFile(this,
                getApplicationContext().getPackageName() + ".fileprovider",
                photoFile);
        cameraLauncher.launch(cameraImageUri);
    }

    /**
     * Opens the system image picker to choose a product image.
     */
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
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

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (requestCode == CAMERA_PERMISSION_REQUEST) {
                startBarcodeScanner();
            } else if (requestCode == CAMERA_PERMISSION_FOR_IMAGE) {
                launchCamera();
            }
        } else {
            Toast.makeText(this,
                    "Camera permission is required",
                    Toast.LENGTH_LONG).show();
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

        // Disable save button to prevent double-tap
        btnSave.setEnabled(false);
        btnSave.setText("Saving...");

        if (selectedImageUri != null) {
            // Upload image to Cloudinary first, then save item with URL
            Toast.makeText(this, "Uploading image...", Toast.LENGTH_SHORT).show();

            CloudinaryHelper.uploadImage(this, selectedImageUri, new CloudinaryHelper.UploadCallback() {
                @Override
                public void onSuccess(String imageUrl) {
                    InventoryItem item = new InventoryItem(
                            name, barcode, category, quantity, price,
                            description, minStock, createdAt, imageUrl
                    );
                    saveItemToFirestore(item);
                }

                @Override
                public void onFailure(String errorMessage) {
                    btnSave.setEnabled(true);
                    btnSave.setText("Save Item");
                    Toast.makeText(AddItemActivity.this,
                            "Image upload failed: " + errorMessage,
                            Toast.LENGTH_LONG).show();
                }
            });
        } else {
            // No image selected — save item without image (image is optional)
            InventoryItem item = new InventoryItem(
                    name, barcode, category, quantity, price,
                    description, minStock, createdAt, null
            );
            saveItemToFirestore(item);
        }
    }

    /**
     * Saves the InventoryItem to Firestore via DatabaseHelper.
     */
    private void saveItemToFirestore(InventoryItem item) {
        dbHelper.addItem(item, success -> {
            if (success) {
                Toast.makeText(this, "Item added successfully!", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            } else {
                btnSave.setEnabled(true);
                btnSave.setText("Save Item");
                Toast.makeText(this, "Failed to add item", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
