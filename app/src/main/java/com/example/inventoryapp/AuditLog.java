package com.example.inventoryapp;

import com.google.firebase.firestore.PropertyName;

public class AuditLog {
    private String id;
    private String action; // e.g., "ADDED", "SOLD", "UPDATED", "DELETED"
    private String itemName;
    private String itemId;
    private int quantity;
    private String userName;
    private String userEmail;
    private String timestamp;
    private String details;

    public AuditLog() {}

    public AuditLog(String action, String itemName, String itemId, int quantity, 
                    String userName, String userEmail, String timestamp, String details) {
        this.action = action;
        this.itemName = itemName;
        this.itemId = itemId;
        this.quantity = quantity;
        this.userName = userName;
        this.userEmail = userEmail;
        this.timestamp = timestamp;
        this.details = details;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    @PropertyName("action")
    public String getAction() { return action; }
    @PropertyName("action")
    public void setAction(String action) { this.action = action; }

    @PropertyName("item_name")
    public String getItemName() { return itemName; }
    @PropertyName("item_name")
    public void setItemName(String itemName) { this.itemName = itemName; }

    @PropertyName("item_id")
    public String getItemId() { return itemId; }
    @PropertyName("item_id")
    public void setItemId(String itemId) { this.itemId = itemId; }

    @PropertyName("quantity")
    public int getQuantity() { return quantity; }
    @PropertyName("quantity")
    public void setQuantity(int quantity) { this.quantity = quantity; }

    @PropertyName("user_name")
    public String getUserName() { return userName; }
    @PropertyName("user_name")
    public void setUserName(String userName) { this.userName = userName; }

    @PropertyName("user_email")
    public String getUserEmail() { return userEmail; }
    @PropertyName("user_email")
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    @PropertyName("timestamp")
    public String getTimestamp() { return timestamp; }
    @PropertyName("timestamp")
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    @PropertyName("details")
    public String getDetails() { return details; }
    @PropertyName("details")
    public void setDetails(String details) { this.details = details; }
}
