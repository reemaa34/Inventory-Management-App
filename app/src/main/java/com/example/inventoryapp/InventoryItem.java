package com.example.inventoryapp;

import java.io.Serializable;

public class InventoryItem implements Serializable {

    private String id;
    private String name;
    private String barcode;
    private String category;
    private int quantity;
    private double price;
    private String description;
    private int minStock;
    private String createdAt;
    private String imageUrl; // New field for product image

    public InventoryItem() {}

    public InventoryItem(String name,
                         String barcode,
                         String category,
                         int quantity,
                         double price,
                         String description,
                         int minStock,
                         String createdAt,
                         String imageUrl) { // Update constructor

        this.name = name;
        this.barcode = barcode;
        this.category = category;
        this.quantity = quantity;
        this.price = price;
        this.description = description;
        this.minStock = minStock;
        this.createdAt = createdAt;
        this.imageUrl = imageUrl;
    }


    public String getId() { return id; }
    public void setId(String id) { this.id = id; }


    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }


    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }


    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }


    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }


    public int getMinStock() { return minStock; }
    public void setMinStock(int minStock) { this.minStock = minStock; }


    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }


    public boolean isLowStock() {
        return quantity <= minStock && quantity > 0;
    }


    public boolean isOutOfStock() {
        return quantity == 0;
    }
}