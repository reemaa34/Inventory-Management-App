package com.example.inventoryapp;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanIntentResult;
import com.journeyapps.barcodescanner.ScanOptions;

import java.util.List;

public class DashboardActivity extends AppCompatActivity implements InventoryAdapter.OnItemActionListener, CategoryAdapter.OnCategoryClickListener {

    private static final int CAMERA_PERMISSION_REQUEST = 102;

    private TextView tvWelcome, tvTotalItems, tvLowStock,
            tvOutOfStock, tvTotalValue, tvSectionLabel;

    private RecyclerView recyclerView;
    private InventoryAdapter adapter;
    private CategoryAdapter categoryAdapter;
    private EditText etSearch;
    private ImageButton btnScanSearch;
    private MaterialButtonToggleGroup toggleGroupFilter;

    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;

    private List<InventoryItem> itemList;
    private List<InventoryItem> originalList;
private boolean isSortedByCategory = false;

    private boolean isAdmin = false;

    private final ActivityResultLauncher<ScanOptions> scanSearchLauncher =
            registerForActivityResult(new ScanContract(), this::onScanSearchResult);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        dbHelper = new DatabaseHelper(this);
        sessionManager = new SessionManager(this);

        isAdmin = sessionManager.getRole().equals("admin");

        tvWelcome    = findViewById(R.id.tvWelcome);
        tvTotalItems = findViewById(R.id.tvTotalItems);
        tvLowStock   = findViewById(R.id.tvLowStock);
        tvOutOfStock = findViewById(R.id.tvOutOfStock);
        tvTotalValue = findViewById(R.id.tvTotalValue);
        tvSectionLabel = findViewById(R.id.tvSectionLabel);

        etSearch      = findViewById(R.id.etSearch);
        recyclerView  = findViewById(R.id.recyclerView);
        btnScanSearch = findViewById(R.id.btnScanSearch);
        toggleGroupFilter = findViewById(R.id.toggleGroupFilter);
        findViewById(R.id.btnSortPrice).setOnClickListener(v -> {
            toggleSortByPrice();
        });

        tvWelcome.setText(getString(R.string.welcome_user, sessionManager.getUsername()));

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        etSearch.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0 && toggleGroupFilter.getCheckedButtonId() == R.id.btnCategories) {
                    toggleGroupFilter.check(R.id.btnAllItems);
                }
                filterItems(s.toString());
            }
            public void afterTextChanged(Editable s) {}
        });

        btnScanSearch.setOnClickListener(v -> launchScanSearch());

        toggleGroupFilter.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
    if (isChecked) {
        if (checkedId == R.id.btnAllItems) {
            showAllItems();
        } else if (checkedId == R.id.btnCategories) {
            showCategories();
        }
    }
});

        setupBottomNavigation();
        loadData();
    }

    private void showAllItems() {
        tvSectionLabel.setText("Inventory Items");
        loadData();
    }

    private void showCategories() {
        tvSectionLabel.setText("Categories");
        dbHelper.getAllCategories(categories -> {
            categoryAdapter = new CategoryAdapter(this, categories, this);
            recyclerView.setAdapter(categoryAdapter);
        });
    }

    @Override
    public void onCategoryClick(String category) {
        etSearch.setText(category);
        toggleGroupFilter.check(R.id.btnAllItems);
        // filterItems will be called by TextWatcher
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);
        bottomNavigation.setSelectedItemId(R.id.nav_home);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                return true;
            } else if (id == R.id.nav_sell) {
                startActivity(new Intent(this, SellStockActivity.class));
                return true;
            } else if (id == R.id.nav_reports) {
                startActivity(new Intent(this, Reports.class));
                return true;
            } else if (id == R.id.nav_add) {
                startActivity(new Intent(this, AddItemActivity.class));
                return true;
            } else if (id == R.id.nav_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            }
            return false;
        });
    }


    private void launchScanSearch() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST);
        } else {
            startScanSearch();
        }
    }

    private void startScanSearch() {
        ScanOptions options = new ScanOptions();
        options.setPrompt("Scan a barcode to search inventory");
        options.setBeepEnabled(true);
        options.setOrientationLocked(false);
        options.setBarcodeImageEnabled(false);
        scanSearchLauncher.launch(options);
    }

    private void onScanSearchResult(ScanIntentResult result) {
        if (result.getContents() != null) {
            String scanned = result.getContents();
            etSearch.setText(scanned);
            filterItems(scanned);
            Toast.makeText(this, "Searching: " + scanned, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Scan cancelled", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startScanSearch();
            } else {
                Toast.makeText(this,
                        "Camera permission required for scanning",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private void loadData() {
    dbHelper.getAllItems(items -> {
        itemList = items;

        // ⭐ Save original list
        originalList = new java.util.ArrayList<>(items);

        adapter = new InventoryAdapter(
                DashboardActivity.this,
                itemList,
                DashboardActivity.this
        );
        recyclerView.setAdapter(adapter);
        updateStats();
    });
}


    private void updateStats() {
        dbHelper.getTotalItems(value ->
                tvTotalItems.setText(String.valueOf(value)));
        dbHelper.getLowStockCount(value ->
                tvLowStock.setText(String.valueOf(value)));
        dbHelper.getOutOfStockCount(value ->
                tvOutOfStock.setText(String.valueOf(value)));
        dbHelper.getTotalInventoryValue(value ->
                tvTotalValue.setText(
                        getString(R.string.total_value_format, value)
                ));
    }

    private void filterItems(String query) {
        if (query.isEmpty()) {
            dbHelper.getAllItems(items -> {
                if (adapter != null) {
                    adapter.updateList(items);
                    originalList = new java.util.ArrayList<>(items);
                    if (recyclerView.getAdapter() != adapter) {
                        recyclerView.setAdapter(adapter);
                    }
                }
            });
        } else {
            dbHelper.searchItems(query, items -> {
                if (adapter != null) {
                    adapter.updateList(items);
                    originalList = new java.util.ArrayList<>(items);
                    if (recyclerView.getAdapter() != adapter) {
                        recyclerView.setAdapter(adapter);
                    }
                }
            });
        }
    }

    @Override
    public void onEdit(InventoryItem item) {
        Intent intent = new Intent(this, EditItemActivity.class);
        intent.putExtra("item", item);
        startActivity(intent);
    }

    @Override
    public void onDelete(InventoryItem item) {
        if (!isAdmin) {
            Toast.makeText(this, "Only admin can delete items", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_item_title)
                .setMessage(getString(R.string.delete_item_message, item.getName()))
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    dbHelper.deleteItem(item.getId(), success -> {
                        if (success) {
                            Toast.makeText(this, R.string.item_deleted, Toast.LENGTH_SHORT).show();
                            loadData();
                        }
                    });
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (toggleGroupFilter.getCheckedButtonId() == R.id.btnCategories) {
            showCategories();
        } else {
            loadData();
        }
    }
    private boolean isSorted = false;
private List<InventoryItem> originalList;



private void toggleSortByPrice() {

    if (itemList == null || adapter == null) return;

    if (!isSorted) {
        originalList = new java.util.ArrayList<>(itemList);

        java.util.Collections.sort(itemList, (a, b) ->
                Double.compare(a.getPrice(), b.getPrice())
        );

        Toast.makeText(this, "Sorted by Price", Toast.LENGTH_SHORT).show();
        isSorted = true;

    } else {
        itemList.clear();
        itemList.addAll(originalList);

        Toast.makeText(this, "Original Order Restored", Toast.LENGTH_SHORT).show();
        isSorted = false;
    }

    adapter.updateList(itemList);
}

}


