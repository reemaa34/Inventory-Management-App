package com.example.inventoryapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Reports extends AppCompatActivity {

    private static final int STORAGE_PERMISSION_REQUEST = 200;
    private static final int PENDING_CSV = 1;
    private static final int PENDING_PDF = 2;
    private int pendingExport = 0;

    private PieChart importChart, exportChart;
    private DatabaseHelper dbHelper;

    // cached item list for export
    private List<InventoryItem> cachedItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.reports_analytics);

        Toolbar toolbar = findViewById(R.id.toolbarReports);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Reports & Analytics");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        importChart = findViewById(R.id.importChart);
        exportChart = findViewById(R.id.exportChart);

        dbHelper = new DatabaseHelper(this);

        // ── Feature 2: Export buttons ──
        Button btnExportCsv = findViewById(R.id.btnExportCsv);
        Button btnExportPdf = findViewById(R.id.btnExportPdf);

        btnExportCsv.setOnClickListener(v -> requestExport(PENDING_CSV));
        btnExportPdf.setOnClickListener(v -> requestExport(PENDING_PDF));

        loadCharts();
    }


    private void loadCharts() {

        dbHelper.getAllItems(items -> {

            cachedItems = items;   // cache for export

            ArrayList<PieEntry> importEntries = new ArrayList<>();
            ArrayList<PieEntry> exportEntries = new ArrayList<>();

            for (InventoryItem item : items) {
                if (item.getQuantity() > 0) {
                    importEntries.add(new PieEntry(item.getQuantity(), item.getName()));
                }
            }

            FirebaseFirestore firestore = FirebaseFirestore.getInstance();
            firestore.collection("sales").get().addOnSuccessListener(snapshot -> {

                Map<String, Integer> totals = new HashMap<>();
                for (DocumentSnapshot doc : snapshot) {
                    String itemId = doc.getString("item_id");
                    Long qty = doc.getLong("quantity");
                    if (itemId != null && qty != null) {
                        totals.put(itemId, totals.getOrDefault(itemId, 0) + qty.intValue());
                    }
                }

                for (Map.Entry<String, Integer> entry : totals.entrySet()) {
                    String itemName = getItemNameById(items, entry.getKey());
                    exportEntries.add(new PieEntry(entry.getValue(), itemName));
                }

                setupChart(importChart, importEntries, "Current Stock");
                setupChart(exportChart, exportEntries, "Items Sold");
            });
        });
    }


    private void setupChart(PieChart chart, ArrayList<PieEntry> entries, String centerText) {

        if (entries.isEmpty()) {
            chart.clear();
            chart.setNoDataText("No data available");
            chart.invalidate();
            return;
        }

        PieDataSet dataSet = new PieDataSet(entries, "");

        ArrayList<Integer> colors = new ArrayList<>();
        for (int c : ColorTemplate.MATERIAL_COLORS) colors.add(c);
        for (int c : ColorTemplate.COLORFUL_COLORS) colors.add(c);
        for (int c : ColorTemplate.JOYFUL_COLORS)   colors.add(c);
        for (int c : ColorTemplate.LIBERTY_COLORS)  colors.add(c);
        for (int c : ColorTemplate.PASTEL_COLORS)   colors.add(c);

        dataSet.setColors(colors);
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(5f);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(13f);

        chart.setData(new PieData(dataSet));
        chart.setUsePercentValues(true);
        chart.getDescription().setEnabled(false);
        chart.setCenterText(centerText);
        chart.setCenterTextSize(16f);
        chart.setEntryLabelColor(Color.BLACK);
        chart.setEntryLabelTextSize(12f);
        chart.animateY(1200);

        Legend legend = chart.getLegend();
        legend.setEnabled(true);
        legend.setTextColor(Color.BLACK);
        legend.setTextSize(12f);

        chart.invalidate();
    }


    private String getItemNameById(List<InventoryItem> items, String id) {
        for (InventoryItem item : items) {
            if (item.getId().equals(id)) return item.getName();
        }
        return "Unknown";
    }


    // ── Feature 2: Permission → export dispatch ──
    private void requestExport(int type) {
        pendingExport = type;
        if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.P) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        STORAGE_PERMISSION_REQUEST);
                return;
            }
        }
        doExport(type);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_REQUEST) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                doExport(pendingExport);
            } else {
                Toast.makeText(this,
                        "Storage permission required to export",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private void doExport(int type) {
        if (cachedItems == null || cachedItems.isEmpty()) {
            Toast.makeText(this, "No inventory data to export", Toast.LENGTH_SHORT).show();
            return;
        }
        if (type == PENDING_CSV) exportCsv();
        else if (type == PENDING_PDF) exportPdf();
    }


    // ── Feature 2: CSV Export ──
    private void exportCsv() {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(new Date());
        String fileName = "inventory_" + timestamp + ".csv";

        File outFile = new File(getFilesDir(), fileName);

        try (FileOutputStream fos = new FileOutputStream(outFile)) {

            // Header
            String header = "ID,Name,Category,Quantity,Price,MinStock,Description,CreatedAt\n";
            fos.write(header.getBytes());

            // Rows
            for (InventoryItem item : cachedItems) {
                String row = String.format(Locale.getDefault(),
                        "\"%s\",\"%s\",\"%s\",%d,%.2f,%d,\"%s\",\"%s\"\n",
                        item.getId() != null ? item.getId() : "",
                        item.getName(),
                        item.getCategory(),
                        item.getQuantity(),
                        item.getPrice(),
                        item.getMinStock(),
                        item.getDescription() != null ? item.getDescription() : "",
                        item.getCreatedAt() != null ? item.getCreatedAt() : ""
                );
                fos.write(row.getBytes());
            }

            fos.flush();
            shareFile(outFile, "text/csv");

        } catch (IOException e) {
            Toast.makeText(this, "CSV export failed: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }


    // ── Feature 2: PDF Export ──
    private void exportPdf() {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(new Date());
        String fileName = "inventory_" + timestamp + ".pdf";

        File outFile = new File(getFilesDir(), fileName);

        PdfDocument document = new PdfDocument();

        int pageWidth  = 595;   // A4 width in points
        int pageHeight = 842;   // A4 height in points
        int marginLeft = 30;
        int rowHeight  = 22;
        int headerH    = 50;
        int startY     = headerH + 20;
        int maxRowsPerPage = (pageHeight - startY - 40) / rowHeight;

        Paint paintTitle  = new Paint();
        paintTitle.setColor(Color.WHITE);
        paintTitle.setTextSize(14f);
        paintTitle.setFakeBoldText(true);

        Paint paintBg = new Paint();
        paintBg.setColor(Color.parseColor("#1976D2"));

        Paint paintCell = new Paint();
        paintCell.setColor(Color.BLACK);
        paintCell.setTextSize(10f);

        Paint paintAltBg = new Paint();
        paintAltBg.setColor(Color.parseColor("#E3F2FD"));

        Paint paintLine = new Paint();
        paintLine.setColor(Color.LTGRAY);
        paintLine.setStrokeWidth(0.5f);

        int pageNumber  = 1;
        int rowIndex    = 0;
        int globalRow   = 0;
        PdfDocument.Page page = null;
        Canvas canvas = null;

        List<String[]> allRows = new ArrayList<>();
        // Build all rows first
        for (InventoryItem item : cachedItems) {
            allRows.add(new String[]{
                    item.getName(),
                    item.getCategory(),
                    String.valueOf(item.getQuantity()),
                    String.format(Locale.getDefault(), "\u20B9%.2f", item.getPrice()),
                    String.valueOf(item.getMinStock())
            });
        }

        String[] colHeaders = {"Name", "Category", "Qty", "Price", "Min Stock"};
        int[] colX = {marginLeft, 170, 290, 345, 430};

        while (globalRow <= allRows.size()) {

            if (page == null || rowIndex >= maxRowsPerPage) {
                if (page != null) document.finishPage(page);
                PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(
                        pageWidth, pageHeight, pageNumber++).create();
                page = document.startPage(pageInfo);
                canvas = page.getCanvas();
                rowIndex = 0;

                // Draw header bar
                canvas.drawRect(0, 0, pageWidth, headerH, paintBg);
                canvas.drawText("Inventory Report — " +
                        new SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                                .format(new Date()),
                        marginLeft, 32f, paintTitle);

                // Draw column headers
                float colHeaderY = startY - 4;
                Paint paintColHdr = new Paint();
                paintColHdr.setColor(Color.parseColor("#0D47A1"));
                paintColHdr.setTextSize(10f);
                paintColHdr.setFakeBoldText(true);
                for (int i = 0; i < colHeaders.length; i++) {
                    canvas.drawText(colHeaders[i], colX[i], colHeaderY, paintColHdr);
                }
                canvas.drawLine(marginLeft, startY, pageWidth - marginLeft, startY, paintLine);
            }

            if (globalRow == allRows.size()) break; // last iteration just to close page

            String[] row = allRows.get(globalRow);
            float y = startY + (rowIndex + 1) * rowHeight;

            // Alternate row background
            if (rowIndex % 2 == 0) {
                canvas.drawRect(marginLeft, y - rowHeight + 4,
                        pageWidth - marginLeft, y + 4, paintAltBg);
            }

            for (int c = 0; c < row.length && c < colX.length; c++) {
                canvas.drawText(row[c] != null ? row[c] : "", colX[c], y, paintCell);
            }
            canvas.drawLine(marginLeft, y + 4, pageWidth - marginLeft, y + 4, paintLine);

            rowIndex++;
            globalRow++;
        }

        if (page != null) document.finishPage(page);

        try (FileOutputStream fos = new FileOutputStream(outFile)) {
            document.writeTo(fos);
            document.close();
            shareFile(outFile, "application/pdf");
        } catch (IOException e) {
            document.close();
            Toast.makeText(this, "PDF export failed: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }


    // ── Feature 2: Share file via intent ──
    private void shareFile(File file, String mimeType) {
        Uri uri = FileProvider.getUriForFile(
                this,
                getPackageName() + ".fileprovider",
                file);

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType(mimeType);
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Inventory Export");
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivity(Intent.createChooser(shareIntent, "Share inventory via"));
    }


    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}