package com.azur.skyblocktileman.client.tileman.ui;

import com.azur.skyblocktileman.client.tileman.TilemanConfig;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

// Base for Tileman chest screens: slotClicked never calls super, so no packets reach the server.
public abstract class TilemanChestScreen extends ContainerScreen {

    protected static final int COLS = 9;

    private final SimpleContainer container;
    private final int rows;
    private final Map<Integer, Runnable> clickHandlers = new HashMap<>();
    private int ticksSinceRefresh;
    // when set, closing this screen re-opens the caller (e.g. the Hub) instead of the game
    private Runnable returnTo;

    protected TilemanChestScreen(int rows, Component title) {
        super(
            buildMenu(rows),
            Minecraft.getInstance().player.getInventory(),
            title
        );
        this.rows = rows;
        this.container = (SimpleContainer) this.menu.getContainer();
    }

    private static ChestMenu buildMenu(int rows) {
        MenuType<ChestMenu> type = switch (rows) {
            case 1 -> MenuType.GENERIC_9x1;
            case 2 -> MenuType.GENERIC_9x2;
            case 3 -> MenuType.GENERIC_9x3;
            case 4 -> MenuType.GENERIC_9x4;
            case 5 -> MenuType.GENERIC_9x5;
            default -> MenuType.GENERIC_9x6;
        };
        SimpleContainer container = new SimpleContainer(rows * COLS);
        return new ChestMenu(
            type,
            0,
            Minecraft.getInstance().player.getInventory(),
            container,
            rows
        );
    }

    protected static int slot(int row, int col) {
        return row * COLS + col;
    }

    protected int footerRow() {
        return rows - 1;
    }

    protected void setItem(int slotId, ItemStack stack) {
        container.setItem(slotId, stack);
    }

    protected void onClick(int slotId, Runnable handler) {
        clickHandlers.put(slotId, handler);
    }

    // Hypixel-style frame: every slot gets a dark pane, content is placed on top.
    protected void buildFrame() {
        container.clearContent();
        clickHandlers.clear();
        for (int i = 0; i < rows * COLS; i++) {
            container.setItem(i, Theme.pane());
        }
        addClose();
    }

    protected void addClose() {
        int s = slot(footerRow(), 4);
        setItem(s, Theme.close());
        onClick(s, this::onClose);
    }

    protected void addBack(Runnable action) {
        int s = slot(footerRow(), 3);
        setItem(s, Theme.back());
        onClick(s, () -> {
            playNav();
            action.run();
        });
    }

    @Override
    protected void slotClicked(
        Slot slot,
        int slotId,
        int button,
        ContainerInput type
    ) {
        Runnable handler = clickHandlers.get(slotId);
        if (handler != null) {
            handler.run();
        }
    }

    @Override
    protected void containerTick() {
        if (++ticksSinceRefresh >= 10) {
            ticksSinceRefresh = 0;
            refresh();
        }
    }

    // Every 10 ticks while the screen is open.
    protected void refresh() {}

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    protected void setReturnTo(Runnable target) {
        this.returnTo = target;
    }

    @Override
    public void onClose() {
        if (returnTo != null) {
            Runnable target = returnTo;
            returnTo = null;
            target.run();
            return;
        }
        super.onClose();
    }

    protected void playNav() {
        if (minecraft.player != null) {
            minecraft.player.playSound(
                SoundEvents.UI_BUTTON_CLICK.value(),
                1.0f,
                1.0f
            );
        }
    }

    protected void playConfirm() {
        if (
            minecraft.player != null &&
            TilemanConfig.getInstance().isPurchaseSoundEnabled()
        ) {
            minecraft.player.playSound(
                SoundEvents.EXPERIENCE_ORB_PICKUP,
                1.0f,
                1.0f
            );
        }
    }
}
