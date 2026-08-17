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
        double v = amount;
        if (v >= 1_000_000_000) {
            return trim(v / 1_000_000_000) + "B";
        }
        if (v >= 1_000_000) {
            return trim(v / 1_000_000) + "M";
        }
        if (v >= 10_000) {
            return trim(v / 1_000) + "K";
        }
        return Long.toString(amount);
    }

    private static String trim(double value) {
        String s = String.format(Locale.US, "%.1f", value);
        if (s.endsWith(".0")) {
            s = s.substring(0, s.length() - 2);
        }
        return s;
    }
}
