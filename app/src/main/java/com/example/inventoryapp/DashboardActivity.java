package com.example.inventoryapp;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanIntentResult;
import com.journeyapps.barcodescanner.ScanOptions;

import java.util.List;

public class DashboardActivity extends AppCompatActivity
        implements InventoryAdapter.OnItemActionListener {

    private static final int CAMERA_PERMISSION_REQUEST = 102;

    private TextView tvWelcome, tvTotalItems, tvLowStock,
            tvOutOfStock, tvTotalValue;

    private RecyclerView recyclerView;
    private InventoryAdapter adapter;
    private EditText etSearch;
    private FloatingActionButton fabAdd;
    private ImageButton btnScanSearch;

    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;

    private List<InventoryItem> itemList;

    private boolean isAdmin = false;

    // ── Feature 1: Barcode scanner for search ──
    private final ActivityResultLauncher<ScanOptions> scanSearchLauncher =
            registerForActivityResult(new ScanContract(), this::onScanSearchResult);


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        dbHelper = new DatabaseHelper(this);
        sessionManager = new SessionManager(this);

        String email = sessionManager.getEmail();

        isAdmin = sessionManager.getRole().equals("admin");

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null)
            getSupportActionBar().setTitle(R.string.dashboard_title);


        tvWelcome    = findViewById(R.id.tvWelcome);
        tvTotalItems = findViewById(R.id.tvTotalItems);
        tvLowStock   = findViewById(R.id.tvLowStock);
        tvOutOfStock = findViewById(R.id.tvOutOfStock);
        tvTotalValue = findViewById(R.id.tvTotalValue);

        etSearch      = findViewById(R.id.etSearch);
        recyclerView  = findViewById(R.id.recyclerView);
        fabAdd        = findViewById(R.id.fabAdd);
        btnScanSearch = findViewById(R.id.btnScanSearch);

        tvWelcome.setText(getString(R.string.welcome_user, sessionManager.getUsername()));

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        fabAdd.setOnClickListener(v ->
                startActivity(new Intent(this, AddItemActivity.class))
        );

        etSearch.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterItems(s.toString());
            }
            public void afterTextChanged(Editable s) {}
        });

        // ── Feature 1: Scan-to-search button ──
        btnScanSearch.setOnClickListener(v -> launchScanSearch());

        loadData();
    }


    // ── Feature 1: Launch scanner for search ──
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

    // ── Feature 1: Use scanned text to filter list ──
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
                                           String[] permissions,
                                           int[] grantResults) {
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
            dbHelper.getAllItems(items -> adapter.updateList(items));
        } else {
            dbHelper.searchItems(query, items -> adapter.updateList(items));
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
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.dashboard_menu, menu);

        if (!isAdmin) {
            menu.findItem(R.id.action_audit_logs).setVisible(false);
        }

        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.action_sell) {
            startActivity(new Intent(this, SellStockActivity.class));
            return true;
        }

        if (id == R.id.action_reports) {
            startActivity(new Intent(this, Reports.class));
            return true;
        }

        if (id == R.id.action_audit_logs) {
            if (!isAdmin) return true;
            startActivity(new Intent(this, AuditActivity.class));
            return true;
        }

        // ── Feature 3: Settings / Dark Mode ──
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }

        if (id == R.id.action_logout) {
            sessionManager.logout();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onResume() {
        super.onResume();
        loadData();
    }
}