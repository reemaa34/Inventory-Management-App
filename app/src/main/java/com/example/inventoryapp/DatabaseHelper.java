package com.example.inventoryapp;

import android.content.Context;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatabaseHelper {

    private final FirebaseFirestore db;

    public DatabaseHelper(Context context) {
        db = FirebaseFirestore.getInstance();
    }

    // ================= CALLBACK INTERFACES =================

    public interface BooleanCallback {
        void onCallback(boolean result);
    }

    public interface StringCallback {
        void onCallback(String result);
    }

    public interface IntCallback {
        void onCallback(int result);
    }

    public interface DoubleCallback {
        void onCallback(double result);
    }

    public interface ItemListCallback {
        void onCallback(List<InventoryItem> items);
    }

    public interface CategoryListCallback {
        void onCallback(List<String> categories);
    }


    // ================= USER METHODS =================

    public void registerUser(String username,
                             String email,
                             String password,
                             String role,
                             BooleanCallback callback) {

        Map<String,Object> user = new HashMap<>();

        user.put("username", username);
        user.put("email", email);
        user.put("password", password);
        user.put("role", role);

        db.collection("users")
                .document(email)
                .set(user)
                .addOnSuccessListener(a -> callback.onCallback(true))
                .addOnFailureListener(e -> callback.onCallback(false));
    }


    public void loginUser(String email,
                          String password,
                          BooleanCallback callback) {

        db.collection("users")
                .document(email)
                .get()
                .addOnSuccessListener(doc -> {

                    if (!doc.exists()) {
                        callback.onCallback(false);
                        return;
                    }

                    String storedPassword = doc.getString("password");

                    callback.onCallback(
                            storedPassword != null &&
                                    storedPassword.equals(password)
                    );
                })
                .addOnFailureListener(e -> callback.onCallback(false));
    }


    public void isEmailExists(String email,
                              BooleanCallback callback) {

        db.collection("users")
                .document(email)
                .get()
                .addOnSuccessListener(doc ->
                        callback.onCallback(doc.exists()))
                .addOnFailureListener(e ->
                        callback.onCallback(false));
    }


    public void getUserRole(String email,
                            StringCallback callback) {

        db.collection("users")
                .document(email)
                .get()
                .addOnSuccessListener(doc -> {

                    if (doc.exists()) {
                        String role = doc.getString("role");
                        callback.onCallback(
                                role != null ? role : "employee"
                        );
                    } else {
                        callback.onCallback("employee");
                    }
                })
                .addOnFailureListener(e ->
                        callback.onCallback("employee"));
    }


    public void getUserName(String email,
                            StringCallback callback) {

        db.collection("users")
                .document(email)
                .get()
                .addOnSuccessListener(doc -> {

                    if (doc.exists()) {
                        String username =
                                doc.getString("username");

                        callback.onCallback(
                                username != null ? username : ""
                        );
                    } else {
                        callback.onCallback("");
                    }
                })
                .addOnFailureListener(e ->
                        callback.onCallback(""));
    }


    // ================= INVENTORY METHODS =================

    public void addItem(InventoryItem item,
                        BooleanCallback callback) {

        String docId =
                db.collection("inventory")
                        .document()
                        .getId();

        item.setId(docId);

        db.collection("inventory")
                .document(docId)
                .set(item)
                .addOnSuccessListener(a ->
                        callback.onCallback(true))
                .addOnFailureListener(e ->
                        callback.onCallback(false));
    }


    public void getAllItems(ItemListCallback callback) {

        db.collection("inventory")
                .orderBy("name")
                .get()
                .addOnSuccessListener(snapshot -> {

                    List<InventoryItem> items =
                            new ArrayList<>();

                    for (DocumentSnapshot doc : snapshot) {

                        InventoryItem item =
                                doc.toObject(InventoryItem.class);

                        if (item != null) {

                            item.setId(doc.getId());
                            items.add(item);
                        }
                    }

                    callback.onCallback(items);
                })
                .addOnFailureListener(e ->
                        callback.onCallback(new ArrayList<>()));
    }


    public void searchItems(String query,
                            ItemListCallback callback) {

        db.collection("inventory")
                .get()
                .addOnSuccessListener(snapshot -> {

                    List<InventoryItem> filtered =
                            new ArrayList<>();

                    for (DocumentSnapshot doc : snapshot) {

                        InventoryItem item =
                                doc.toObject(InventoryItem.class);

                        if (item != null &&
                                (item.getName().toLowerCase().contains(query.toLowerCase())
                                        || item.getCategory().toLowerCase().contains(query.toLowerCase()))) {

                            item.setId(doc.getId());
                            filtered.add(item);
                        }
                    }

                    callback.onCallback(filtered);
                })
                .addOnFailureListener(e ->
                        callback.onCallback(new ArrayList<>()));
    }


    public void updateItem(InventoryItem item,
                           BooleanCallback callback) {

        db.collection("inventory")
                .document(item.getId())
                .set(item)
                .addOnSuccessListener(a ->
                        callback.onCallback(true))
                .addOnFailureListener(e ->
                        callback.onCallback(false));
    }


    public void deleteItem(String id,
                           BooleanCallback callback) {

        db.collection("inventory")
                .document(id)
                .delete()
                .addOnSuccessListener(a ->
                        callback.onCallback(true))
                .addOnFailureListener(e ->
                        callback.onCallback(false));
    }


    public void getAllCategories(CategoryListCallback callback) {

        db.collection("inventory")
                .get()
                .addOnSuccessListener(snapshot -> {

                    List<String> categories =
                            new ArrayList<>();

                    for (DocumentSnapshot doc : snapshot) {

                        String category =
                                doc.getString("category");

                        if (category != null &&
                                !categories.contains(category)) {

                            categories.add(category);
                        }
                    }

                    callback.onCallback(categories);
                })
                .addOnFailureListener(e ->
                        callback.onCallback(new ArrayList<>()));
    }


    public void getItemsByCategory(String category,
                                   ItemListCallback callback) {

        db.collection("inventory")
                .whereEqualTo("category", category)
                .get()
                .addOnSuccessListener(snapshot -> {

                    List<InventoryItem> items =
                            new ArrayList<>();

                    for (DocumentSnapshot doc : snapshot) {

                        InventoryItem item =
                                doc.toObject(InventoryItem.class);

                        if (item != null) {

                            item.setId(doc.getId());
                            items.add(item);
                        }
                    }

                    callback.onCallback(items);
                })
                .addOnFailureListener(e ->
                        callback.onCallback(new ArrayList<>()));
    }


    // ================= DASHBOARD STATS =================

    public void getTotalItems(IntCallback callback) {

        db.collection("inventory")
                .get()
                .addOnSuccessListener(snapshot ->
                        callback.onCallback(snapshot.size()))
                .addOnFailureListener(e ->
                        callback.onCallback(0));
    }


    public void getLowStockCount(IntCallback callback) {

        db.collection("inventory")
                .get()
                .addOnSuccessListener(snapshot -> {

                    int count = 0;

                    for (DocumentSnapshot doc : snapshot) {

                        Long qty =
                                doc.getLong("quantity");

                        Long min =
                                doc.getLong("minStock");

                        if (qty != null &&
                                min != null &&
                                qty <= min &&
                                qty > 0) {

                            count++;
                        }
                    }

                    callback.onCallback(count);
                })
                .addOnFailureListener(e ->
                        callback.onCallback(0));
    }


    public void getOutOfStockCount(IntCallback callback) {

        db.collection("inventory")
                .whereEqualTo("quantity", 0)
                .get()
                .addOnSuccessListener(snapshot ->
                        callback.onCallback(snapshot.size()))
                .addOnFailureListener(e ->
                        callback.onCallback(0));
    }


    public void getTotalInventoryValue(DoubleCallback callback) {

        db.collection("inventory")
                .get()
                .addOnSuccessListener(snapshot -> {

                    double total = 0;

                    for (DocumentSnapshot doc : snapshot) {

                        Long qty =
                                doc.getLong("quantity");

                        Double price =
                                doc.getDouble("price");

                        if (qty != null && price != null)
                            total += qty * price;
                    }

                    callback.onCallback(total);
                })
                .addOnFailureListener(e ->
                        callback.onCallback(0.0));
    }


    public void getTotalRevenue(DoubleCallback callback) {

        db.collection("sales")
                .get()
                .addOnSuccessListener(snapshot -> {

                    if (snapshot.isEmpty()) {
                        callback.onCallback(0.0);
                        return;
                    }

                    double total = 0;

                    for (DocumentSnapshot doc : snapshot) {

                        Long qty   = doc.getLong("quantity");
                        // price_at_sale was added in the fixed insertSale().
                        // For older records that lack it, fall back to 0.
                        Double price = doc.getDouble("price_at_sale");

                        if (qty != null && price != null) {
                            total += qty * price;   // correct: revenue = qty × price
                        }
                    }

                    callback.onCallback(total);
                })
                .addOnFailureListener(e ->
                        callback.onCallback(0.0));
    }


    // ================= SALES METHODS =================

    public void insertSale(String itemId,
                           int quantity,
                           double priceAtSale,   // Bug 2 fix: persist price so revenue = qty × price
                           String customer,
                           String soldBy,
                           String timestamp,
                           BooleanCallback callback) {

        Map<String,Object> sale = new HashMap<>();

        sale.put("item_id",       itemId);
        sale.put("quantity",      quantity);
        sale.put("price_at_sale", priceAtSale);  // stored so getTotalRevenue can multiply
        sale.put("customer",      customer);
        sale.put("sold_by",       soldBy);
        sale.put("timestamp",     timestamp);

        db.collection("sales")
                .add(sale)
                .addOnSuccessListener(doc ->
                        callback.onCallback(true))
                .addOnFailureListener(e ->
                        callback.onCallback(false));
    }
}