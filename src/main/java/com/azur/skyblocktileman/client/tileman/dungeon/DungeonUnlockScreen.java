package com.azur.skyblocktileman.client.tileman.dungeon;

import com.azur.skyblocktileman.client.tileman.TilemanChat;
import com.azur.skyblocktileman.client.tileman.TilemanState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.List;

/**
 * Confirmation screen for unlocking dungeon floors.
 */
public class DungeonUnlockScreen extends ContainerScreen {

    private static final int ROWS = 3;
    private static final int SIZE = ROWS * 9;
    
    private final DungeonFloor floor;
    private final SimpleContainer container;

    public DungeonUnlockScreen(ChestMenu menu, Inventory playerInventory, Component title, DungeonFloor floor) {
        super(menu, playerInventory, title);
        this.floor = floor;
        this.container = (SimpleContainer) menu.getContainer();
    }

    public static void open(DungeonFloor floor) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        client.schedule(() -> {
            SimpleContainer container = new SimpleContainer(SIZE);
            ChestMenu menu = new ChestMenu(MenuType.GENERIC_9x3, 0, client.player.getInventory(), container, ROWS);
            DungeonUnlockScreen screen = new DungeonUnlockScreen(
                menu, 
                client.player.getInventory(), 
                Component.literal("§6Unlock " + floor.getDisplayName()),
                floor
            );
            screen.loadScreen();
            client.gui.setScreen(screen);
        });
    }

    @Override
    protected void init() {
        super.init();
        loadScreen();
    }

    private void loadScreen() {
        clearContainer();
        fillBorder();
        
        int cost = floor.getUnlockCost();
        int tokens = TilemanState.getInstance().getTokens();
        boolean canAfford = tokens >= cost;
        
        // Floor info item (center)
        setItem(13, createFloorInfoItem(cost, tokens, canAfford));
        
        // Confirm button (left)
        setItem(11, createConfirmItem(canAfford));
        
        // Cancel button (right)
        setItem(15, createCancelItem());
    }

    private ItemStack createFloorInfoItem(int cost, int tokens, boolean canAfford) {
        ItemStack stack = new ItemStack(floor.isMasterMode() ? Items.WITHER_SKELETON_SKULL : Items.ZOMBIE_HEAD);
        setItemName(stack, "§6" + floor.getDisplayName());
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.literal("§7Unlock Cost: §e" + cost + " tokens"));
        lore.add(Component.literal("§7You have: " + (canAfford ? "§a" : "§c") + tokens + " tokens"));
        lore.add(Component.empty());
        
        if (canAfford) {
            lore.add(Component.literal("§aYou can afford this!"));
        } else {
            lore.add(Component.literal("§cNot enough tokens!"));
            lore.add(Component.literal("§7Need §e" + (cost - tokens) + " §7more tokens"));
        }
        
        lore.add(Component.empty());
        lore.add(Component.literal("§7Unlocking gives you:"));
        lore.add(Component.literal("§8• §fFree tiles inside dungeon"));
        lore.add(Component.literal("§8• §fToken rewards from clears"));
        
        // Show reward info
        lore.add(Component.empty());
        lore.add(Component.literal("§7Clear rewards:"));
        lore.add(Component.literal("§8• §fFirst 10 clears: §e" + floor.getBaseReward() + " tokens §7each"));
        lore.add(Component.literal("§8• §fClears 11-50: §e" + Math.max(1, floor.getBaseReward() / 10) + " tokens §7each"));
        
        setItemLore(stack, lore);
        return stack;
    }

    private ItemStack createConfirmItem(boolean canAfford) {
        ItemStack stack = new ItemStack(canAfford ? Items.STAINED_GLASS_PANE.lime() : Items.STAINED_GLASS_PANE.gray());
        
        if (canAfford) {
            setItemName(stack, "§a§lCONFIRM UNLOCK");
            List<Component> lore = new ArrayList<>();
            lore.add(Component.literal("§7Click to unlock " + floor.getDisplayName()));
            setItemLore(stack, lore);
        } else {
            setItemName(stack, "§c§lCANNOT AFFORD");
            List<Component> lore = new ArrayList<>();
            lore.add(Component.literal("§7You don't have enough tokens"));
            setItemLore(stack, lore);
        }
        
        return stack;
    }

    private ItemStack createCancelItem() {
        ItemStack stack = new ItemStack(Items.STAINED_GLASS_PANE.red());
        setItemName(stack, "§c§lCANCEL");
        List<Component> lore = new ArrayList<>();
        lore.add(Component.literal("§7Return to dungeon menu"));
        setItemLore(stack, lore);
        return stack;
    }

    private void clearContainer() {
        for (int i = 0; i < SIZE; i++) {
            container.setItem(i, ItemStack.EMPTY);
        }
    }

    private void fillBorder() {
        ItemStack pane = new ItemStack(Items.STAINED_GLASS_PANE.black());
        setItemName(pane, " ");
        for (int i = 0; i < SIZE; i++) {
            container.setItem(i, pane.copy());
        }
    }

    private void setItem(int slot, ItemStack stack) {
        container.setItem(slot, stack);
    }

    private void setItemName(ItemStack stack, String name) {
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
    }

    private void setItemLore(ItemStack stack, List<Component> lore) {
        stack.set(DataComponents.LORE, new ItemLore(lore));
    }

    @Override
    protected void slotClicked(Slot slot, int slotId, int button, ContainerInput type) {
        if (slot == null || slotId < 0 || slotId >= SIZE) return;

        ItemStack clicked = slot.getItem();
        if (clicked.isEmpty()) return;

        String name = clicked.getHoverName().getString();
        
        Minecraft client = Minecraft.getInstance();
        
        if (name.contains("CONFIRM UNLOCK")) {
            // Try to unlock
            boolean success = DungeonTracker.getInstance().tryUnlockFloor(floor);
            
            if (success) {
                playSuccess();
                // Close screen
                client.schedule(() -> client.gui.setScreen(null));
            } else {
                playError();
                // Refresh screen to show updated token count
                loadScreen();
            }
        } else if (name.contains("CANCEL")) {
            playClick();
            // Just close, go back to dungeon menu (which is still there underneath)
            client.schedule(() -> client.gui.setScreen(null));
        }
    }

    private void playClick() {
        Minecraft.getInstance().player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 1.0f, 1.0f);
    }

    private void playSuccess() {
        Minecraft.getInstance().player.playSound(SoundEvents.PLAYER_LEVELUP, 1.0f, 1.0f);
    }

    private void playError() {
        Minecraft.getInstance().player.playSound(SoundEvents.VILLAGER_NO, 1.0f, 1.0f);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
