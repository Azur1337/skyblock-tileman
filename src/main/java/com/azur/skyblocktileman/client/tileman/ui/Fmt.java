package com.azur.skyblocktileman.client.tileman.ui;

import java.util.Locale;

// Locale-stable number formatting so displayed values never drift by region
public final class Fmt {

    private Fmt() {}

    public static String xp(long amount) {
        return String.format(Locale.US, "%,d", amount);
    }

    public static String tokens(int count) {
        return count + (count == 1 ? " token" : " tokens");
    }

    // fixed-dot abbreviations, never comma decimals: 12.5K / 3.4M / 1.1B
    public static String compact(long amount) {
        double value = amount;
        if (value >= 1_000_000_000) {
            return trim(value / 1_000_000_000) + "B";
        }
        if (value >= 1_000_000) {
            return trim(value / 1_000_000) + "M";
        }
        if (value >= 10_000) {
            return trim(value / 1_000) + "K";
        }
        return Long.toString(amount);
    }

    private static String trim(double value) {
        String text = String.format(Locale.US, "%.1f", value);
        if (text.endsWith(".0")) {
            text = text.substring(0, text.length() - 2);
        }
        return text;
    }
}
