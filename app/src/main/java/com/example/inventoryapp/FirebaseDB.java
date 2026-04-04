package com.example.inventoryapp;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirebaseDB {

    private final FirebaseFirestore db;

    public FirebaseDB() {
        db = FirebaseFirestore.getInstance();
    }


    // ================= USERS =================

    public void registerUser(String username,
                             String email,
                             String password,
                             BooleanCallback callback) {

        Map<String, Object> user = new HashMap<>();

        user.put("username", username);
        user.put("email", email);
        user.put("password", password);

        db.collection("users")
                .add(user)
                .addOnSuccessListener(doc -> callback.onCallback(true))
                .addOnFailureListener(e -> callback.onCallback(false));
    }


    public void loginUser(String email,
                          String password,
                          BooleanCallback callback) {

        db.collection("users")
                .whereEqualTo("email", email)
                .whereEqualTo("password", password)
                .get()
                .addOnSuccessListener(snapshot ->
                        callback.onCallback(!snapshot.isEmpty()))
                .addOnFailureListener(e ->
                        callback.onCallback(false));
    }


    public void isEmailExists(String email,
                              BooleanCallback callback) {

        db.collection("users")
                .whereEqualTo("email", email)
                .get()
                .addOnSuccessListener(snapshot ->
                        callback.onCallback(!snapshot.isEmpty()))
                .addOnFailureListener(e ->
                        callback.onCallback(false));
    }


    public void getUserName(String email,
                            StringCallback callback) {

        db.collection("users")
                .whereEqualTo("email", email)
                .get()
                .addOnSuccessListener(snapshot -> {

                    String username = "";

                    if (!snapshot.isEmpty()) {
                        username = snapshot.getDocuments()
                                .get(0)
                                .getString("username");
                    }

                    callback.onCallback(username);
                });
    }



    // ================= INVENTORY =================

    public void addItem(InventoryItem item,
                        BooleanCallback callback) {

        db.collection("inventory")
                .add(item)
                .addOnSuccessListener(doc ->
                        callback.onCallback(true))
                .addOnFailureListener(e ->
                        callback.onCallback(false));
    }


    public void getAllItems(ItemListCallback callback) {

        db.collection("inventory")
                .orderBy("name")
                .get()
                .addOnSuccessListener(snapshot -> {

                    List<InventoryItem> list =
                            snapshot.toObjects(InventoryItem.class);

                    callback.onCallback(list);
                });
    }


    public void searchItems(String query,
                            ItemListCallback callback) {

        db.collection("inventory")
                .get()
                .addOnSuccessListener(snapshot -> {

                    List<InventoryItem> list =
                            snapshot.toObjects(InventoryItem.class);

                    List<InventoryItem> filtered =
                            new ArrayList<>();

                    for (InventoryItem item : list) {

                        if (item.getName().toLowerCase().contains(query.toLowerCase())
                                || item.getCategory().toLowerCase().contains(query.toLowerCase())) {

                            filtered.add(item);
                        }
                    }

                    callback.onCallback(filtered);
                });
    }


    public void updateItem(String documentId,
                           InventoryItem item,
                           BooleanCallback callback) {

        db.collection("inventory")
                .document(documentId)
                .set(item)
                .addOnSuccessListener(a ->
                        callback.onCallback(true))
                .addOnFailureListener(e ->
                        callback.onCallback(false));
    }


    public void deleteItem(String documentId,
                           BooleanCallback callback) {

        db.collection("inventory")
                .document(documentId)
                .delete()
                .addOnSuccessListener(a ->
                        callback.onCallback(true))
                .addOnFailureListener(e ->
                        callback.onCallback(false));
    }


    public void getItemsByCategory(String category,
                                   ItemListCallback callback) {

        db.collection("inventory")
                .whereEqualTo("category", category)
                .get()
                .addOnSuccessListener(snapshot -> {

                    List<InventoryItem> list =
                            snapshot.toObjects(InventoryItem.class);

                    callback.onCallback(list);
                });
    }



    // ================= DASHBOARD STATS =================

    public void getTotalItems(IntCallback callback) {

        db.collection("inventory")
                .get()
                .addOnSuccessListener(snapshot ->
                        callback.onCallback(snapshot.size()));
    }


    public void getLowStockCount(IntCallback callback) {

        db.collection("inventory")
                .get()
                .addOnSuccessListener(snapshot -> {

                    int count = 0;

                    for (QueryDocumentSnapshot doc : snapshot) {

                        Long quantity = doc.getLong("quantity");
                        Long minStock = doc.getLong("min_stock");

                        if (quantity != null &&
                                minStock != null &&
                                quantity <= minStock) {

                            count++;
                        }
                    }

                    callback.onCallback(count);
                });
    }


    public void getOutOfStockCount(IntCallback callback) {

        db.collection("inventory")
                .whereEqualTo("quantity", 0)
                .get()
                .addOnSuccessListener(snapshot ->
                        callback.onCallback(snapshot.size()));
    }


    public void getTotalInventoryValue(DoubleCallback callback) {

        db.collection("inventory")
                .get()
                .addOnSuccessListener(snapshot -> {

                    double total = 0;

                    for (DocumentSnapshot doc : snapshot) {

                        Long quantity = doc.getLong("quantity");
                        Double price = doc.getDouble("price");

                        if (quantity != null && price != null) {
                            total += quantity * price;
                        }
                    }

                    callback.onCallback(total);
                });
    }



    // ================= SALES =================

    public void insertSale(int itemId,
                           int quantity,
                           String customer,
                           String soldBy,
                           String timestamp,
                           BooleanCallback callback) {

        Map<String, Object> sale = new HashMap<>();

        sale.put("item_id", itemId);
        sale.put("quantity", quantity);
        sale.put("customer", customer);
        sale.put("sold_by", soldBy);
        sale.put("timestamp", timestamp);

        db.collection("sales")
                .add(sale)
                .addOnSuccessListener(doc ->
                        callback.onCallback(true))
                .addOnFailureListener(e ->
                        callback.onCallback(false));
    }



    // ================= CALLBACK INTERFACES =================

    public interface BooleanCallback {
        void onCallback(boolean result);
    }

    public interface StringCallback {
        void onCallback(String result);
    }

    public interface ItemListCallback {
        void onCallback(List<InventoryItem> items);
    }

    public interface IntCallback {
        void onCallback(int value);
    }

    public interface DoubleCallback {
        void onCallback(double value);
    }
}