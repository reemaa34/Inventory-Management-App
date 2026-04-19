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
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.File;

public class EditItemActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_FOR_IMAGE = 102;

    private EditText etItemName, etQuantity, etPrice, etDescription, etMinStock;
    private Spinner spinnerCategory;
    private Button btnUpdate, btnCancel, btnSelectImage;
    private ImageView ivProductPreview;
    private DatabaseHelper dbHelper;
    private InventoryItem currentItem;

    // Selected image URI (from gallery or camera)
    private Uri selectedImageUri = null;
    // URI for the camera-captured photo
    private Uri cameraImageUri = null;

    private final String[] categories = {"Electronics", "Clothing", "Food & Beverage", "Furniture",
            "Tools", "Stationery", "Medicine", "Sports", "Toys", "Other"};

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
        ivProductPreview = findViewById(R.id.ivProductPreview);
        btnSelectImage = findViewById(R.id.btnSelectImage);

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

        // Load existing product image if available
        if (currentItem.getImageUrl() != null && !currentItem.getImageUrl().isEmpty()) {
            Glide.with(this)
                    .load(currentItem.getImageUrl())
                    .centerCrop()
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .into(ivProductPreview);
        }

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
        btnSelectImage.setOnClickListener(v -> showImageSourceDialog());

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
                        openCamera();
                    } else {
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

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_FOR_IMAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchCamera();
            } else {
                Toast.makeText(this,
                        "Camera permission is required to take photos",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_reports) {
                startActivity(new Intent(this, Reports.class));
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

        // Disable button to prevent double-tap
        btnUpdate.setEnabled(false);
        btnUpdate.setText("Updating...");

        if (selectedImageUri != null) {
            // New image selected — upload to Cloudinary first
            Toast.makeText(this, "Uploading image...", Toast.LENGTH_SHORT).show();

            CloudinaryHelper.uploadImage(this, selectedImageUri, new CloudinaryHelper.UploadCallback() {
                @Override
                public void onSuccess(String imageUrl) {
                    currentItem.setImageUrl(imageUrl);
                    saveUpdatedItem();
                }

                @Override
                public void onFailure(String errorMessage) {
                    btnUpdate.setEnabled(true);
                    btnUpdate.setText("Update Item");
                    Toast.makeText(EditItemActivity.this,
                            "Image upload failed: " + errorMessage,
                            Toast.LENGTH_LONG).show();
                }
            });
        } else {
            // No new image — keep existing imageUrl and just update
            saveUpdatedItem();
        }
    }

    /**
     * Saves the updated item to Firestore.
     */
    private void saveUpdatedItem() {
        dbHelper.updateItem(currentItem, success -> {
            if (success) {
                Toast.makeText(this,
                        "Item updated successfully!",
                        Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            } else {
                btnUpdate.setEnabled(true);
                btnUpdate.setText("Update Item");
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
