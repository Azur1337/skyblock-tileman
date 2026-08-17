package com.azur.skyblocktileman.client.tileman.ui;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.TooltipDisplay;

// Builder for the standard item anatomy (UI-DESIGN.md section 4)
public final class TilemanItem {

    private final ItemStack stack;
    private final List<Component> lore = new ArrayList<>();

    private TilemanItem(Item item) {
        this.stack = new ItemStack(item);
    }

    public static TilemanItem of(Item item) {
        return new TilemanItem(item);
    }

    public TilemanItem name(String text) {
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(text));
        return this;
    }

    public TilemanItem line(String text) {
        lore.add(Component.literal(text));
        return this;
    }

    public TilemanItem blank() {
        lore.add(Component.empty());
        return this;
    }

    public TilemanItem desc(String text) {
        return line(Theme.DESC + text);
    }

    public TilemanItem action(String text) {
        return line(Theme.ACTION + text);
    }

    public TilemanItem good(String text) {
        return line(Theme.GOOD + text);
    }

    public TilemanItem bad(String text) {
        return line(Theme.BAD + text);
    }

    public TilemanItem value(String text) {
        return line(Theme.VALUE + text);
    }

    public TilemanItem hideTooltip() {
        stack.set(
            DataComponents.TOOLTIP_DISPLAY,
            new TooltipDisplay(true, new LinkedHashSet<>())
        );
        return this;
    }

    public TilemanItem glint() {
        stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        return this;
    }

    public ItemStack build() {
        if (!lore.isEmpty()) {
            stack.set(DataComponents.LORE, new ItemLore(lore));
        }
        return stack;
    }
}
