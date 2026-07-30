package com.azur.skyblocktileman.client.tileman.shop;

public enum ShopCategory {
    PERMANENT("Permanent Upgrades"),
    CONSUMABLES("Consumables");

    private final String displayName;

    ShopCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
