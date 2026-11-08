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
    private static final int REFRESH_INTERVAL_TICKS = 10;

    private final SimpleContainer container;
    private final int rows;
    private final Map<Integer, Runnable> clickHandlers = new HashMap<>();
    private final Map<Integer, Runnable> rightClickHandlers = new HashMap<>();
    private int ticksSinceRefresh;
    // when set, closing this screen re-opens the caller (e.g. the Hub) instead of the game
    private Runnable returnTo;

    protected TilemanChestScreen(int rows, Component title) {
        this(rows, title, new SimpleContainer(rows * COLS));
    }

    // the container is built before super so the menu and the field share one reference;
    // no cast from menu.getContainer()
    private TilemanChestScreen(
        int rows,
        Component title,
        SimpleContainer container
    ) {
        super(
            buildMenu(rows, container),
            Minecraft.getInstance().player.getInventory(),
            title
        );
        this.rows = rows;
        this.container = container;
    }

    private static ChestMenu buildMenu(int rows, SimpleContainer container) {
        MenuType<ChestMenu> type = switch (rows) {
            case 1 -> MenuType.GENERIC_9x1;
            case 2 -> MenuType.GENERIC_9x2;
            case 3 -> MenuType.GENERIC_9x3;
            case 4 -> MenuType.GENERIC_9x4;
            case 5 -> MenuType.GENERIC_9x5;
            default -> MenuType.GENERIC_9x6;
        };
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

    // UI-DESIGN section 5: right click is the context jump, e.g. requirement -> milestone
    protected void onRightClick(int slotId, Runnable handler) {
        rightClickHandlers.put(slotId, handler);
    }

    // Hypixel-style frame: every slot gets a dark pane, content is placed on top.
    protected void buildFrame() {
        container.clearContent();
        clickHandlers.clear();
        rightClickHandlers.clear();
        for (int i = 0; i < rows * COLS; i++) {
            container.setItem(i, Theme.pane());
        }
        addClose();
    }

    protected void addClose() {
        int slotId = slot(footerRow(), 4);
        setItem(slotId, Theme.close());
        onClick(slotId, this::onClose);
    }

    protected void addBack(Runnable action) {
        int slotId = slot(footerRow(), 3);
        setItem(slotId, Theme.back());
        onClick(slotId, () -> {
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
        // UI-DESIGN section 5: left click acts, right click jumps; number keys arrive as
        // SWAP with button 0-8 (so key "1" is button 0!) and double-click as PICKUP_ALL with
        // button 0, so the mode must be checked, not just the button.
        if (type != ContainerInput.PICKUP) {
            return;
        }
        Runnable handler =
            button == 0
                ? clickHandlers.get(slotId)
                : button == 1
                  ? rightClickHandlers.get(slotId)
                  : null;
        if (handler != null) {
            handler.run();
        }
    }

    @Override
    protected void containerTick() {
        if (++ticksSinceRefresh >= REFRESH_INTERVAL_TICKS) {
            ticksSinceRefresh = 0;
            refresh();
        }
    }

    // Every REFRESH_INTERVAL_TICKS while the screen is open.
    protected void refresh() {}

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    protected void setReturnTo(Runnable target) {
        this.returnTo = target;
    }

    protected Runnable getReturnTo() {
        return returnTo;
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
