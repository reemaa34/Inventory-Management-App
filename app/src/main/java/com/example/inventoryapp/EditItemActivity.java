package com.example.inventoryapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import android.net.Uri;
import android.widget.ImageView;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.activity.result.ActivityResultLauncher;
import com.bumptech.glide.Glide;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.util.UUID;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class EditItemActivity extends AppCompatActivity {

    private EditText etItemName, etQuantity, etPrice, etDescription, etMinStock;
    private Spinner spinnerCategory;
    private Button btnUpdate, btnCancel;
    private DatabaseHelper dbHelper;
    private InventoryItem currentItem;

    private ImageView ivProductPreview;
    private Button btnSelectImage;
    private Uri imageUri;

    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    imageUri = result.getData().getData();
                    if (ivProductPreview != null) ivProductPreview.setImageURI(imageUri);
                }
            });

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

        ivProductPreview = findViewById(R.id.ivProductPreview);
        btnSelectImage = findViewById(R.id.btnSelectImage);

        btnSelectImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            imagePickerLauncher.launch(intent);
        });

        // Load existing image if any
        if (currentItem.getImageUrl() != null && !currentItem.getImageUrl().isEmpty()) {
            Glide.with(this)
                 .load(currentItem.getImageUrl())
                 .placeholder(android.R.drawable.ic_menu_gallery)
                 .into(ivProductPreview);
        }

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

        setupBottomNavigation();
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

        if (imageUri != null) {
            Toast.makeText(this, "Uploading image...", Toast.LENGTH_SHORT).show();
            StorageReference storageRef = FirebaseStorage.getInstance().getReference()
                    .child("productImages/" + UUID.randomUUID().toString());
            
            storageRef.putFile(imageUri).addOnSuccessListener(taskSnapshot -> {
                storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    currentItem.setImageUrl(uri.toString());
                    updateItemInDB();
                });
            }).addOnFailureListener(e -> {
                Toast.makeText(this, "Image upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                updateItemInDB();
            });
        } else {
            updateItemInDB();
        }
    }

    private void updateItemInDB() {
        dbHelper.updateItem(currentItem, success -> {
            if (success) {
                Toast.makeText(this, "Item updated successfully!", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            } else {
                Toast.makeText(this, "Failed to update item", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
