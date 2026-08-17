package com.azur.skyblocktileman.client.tileman.ui;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

// Color roles and standard parts from UI-DESIGN.md; screens reference roles, never raw codes
public final class Theme {

    public static final String DESC = "§7";
    public static final String ACTION = "§e";
    public static final String GOOD = "§a";
    public static final String BAD = "§c";
    public static final String VALUE = "§6";
    public static final String NAME = "§f";

    private Theme() {}

    public static ItemStack pane() {
        // HIDE_TOOLTIP stops the empty name from showing as an (empty) hover box
        return TilemanItem.of(Items.STAINED_GLASS_PANE.black())
            .name(" ")
            .hideTooltip()
            .build();
    }

    public static ItemStack close() {
        return TilemanItem.of(Items.BARRIER)
            .name(BAD + "Close")
            .build();
    }

    public static ItemStack back() {
        return TilemanItem.of(Items.SPECTRAL_ARROW)
            .name(NAME + "Back")
            .build();
    }

    public static ItemStack info() {
        return TilemanItem.of(Items.OAK_SIGN)
            .name(NAME + "Info")
            .build();
    }

    public static ItemStack next() {
        return TilemanItem.of(Items.ARROW)
            .name(NAME + "Next")
            .build();
    }

    public static ItemStack settings() {
        return TilemanItem.of(Items.COMPARATOR)
            .name(NAME + "Settings")
            .blank()
            .desc("Open the Tileman config")
            .build();
    }

    public static ItemStack yes() {
        return TilemanItem.of(Items.DYED_TERRACOTTA.lime())
            .name(GOOD + "Confirm")
            .blank()
            .desc("Click to confirm")
            .build();
    }

    public static ItemStack no() {
        return TilemanItem.of(Items.DYED_TERRACOTTA.red())
            .name(BAD + "Cancel")
            .blank()
            .desc("Return to previous menu")
            .build();
    }
}
