package com.azur.skyblocktileman.client.tileman.dungeon;

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
 * Screen showing dungeon floor progress and unlock status.
 */
public class DungeonProgressScreen extends ContainerScreen {

    private static final int ROWS = 6;
    private static final int SIZE = ROWS * 9;
    
    private final SimpleContainer container;
    private boolean showingMasterMode = false;

    public DungeonProgressScreen(ChestMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.container = (SimpleContainer) menu.getContainer();
    }

    public static void open() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        client.schedule(() -> {
            SimpleContainer container = new SimpleContainer(SIZE);
            ChestMenu menu = new ChestMenu(MenuType.GENERIC_9x6, 0, client.player.getInventory(), container, ROWS);
            DungeonProgressScreen screen = new DungeonProgressScreen(
                menu, 
                client.player.getInventory(), 
                Component.literal("§6Dungeon Progress")
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
        
        DungeonData data = TilemanState.getInstance().getDungeonData();
        int tokens = TilemanState.getInstance().getTokens();
        
        // Title info
        setItem(4, createInfoItem(data, tokens));
        
        // Mode toggle
        setItem(49, createModeToggleItem());
        
        if (showingMasterMode) {
            // Master mode floors (M1-M7)
            int[] slots = {19, 20, 21, 22, 23, 24, 25};
            for (int i = 0; i < 7; i++) {
                DungeonFloor floor = DungeonFloor.fromNumber(i + 1, true);
                setItem(slots[i], createFloorItem(floor, data, tokens));
            }
        } else {
            // Normal floors (F1-F7)
            int[] slots = {19, 20, 21, 22, 23, 24, 25};
            for (int i = 0; i < 7; i++) {
                DungeonFloor floor = DungeonFloor.fromNumber(i + 1, false);
                setItem(slots[i], createFloorItem(floor, data, tokens));
            }
        }
        
        // Stats in bottom row
        setItem(37, createStatsItem(data));
        setItem(43, createRewardsInfoItem());
    }

    private ItemStack createInfoItem(DungeonData data, int tokens) {
        ItemStack stack = new ItemStack(Items.NETHER_STAR);
        setItemName(stack, "§6§lDungeon Progress");
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.literal("§7Your tokens: §e" + tokens));
        lore.add(Component.empty());
        
        int unlockedNormal = data.getUnlockedNormalFloorCount();
        int unlockedMaster = data.getUnlockedMasterModeFloorCount();
        lore.add(Component.literal("§7Normal floors: §a" + unlockedNormal + "§7/§a7"));
        lore.add(Component.literal("§7Master floors: §a" + unlockedMaster + "§7/§a7"));
        lore.add(Component.empty());
        lore.add(Component.literal("§7Total completions: §e" + data.getTotalCompletions()));
        
        setItemLore(stack, lore);
        return stack;
    }

    private ItemStack createFloorItem(DungeonFloor floor, DungeonData data, int tokens) {
        boolean unlocked = data.isUnlocked(floor);
        int cost = floor.getUnlockCost();
        boolean canAfford = tokens >= cost;
        
        ItemStack stack;
        if (unlocked) {
            stack = new ItemStack(Items.STAINED_GLASS_PANE.lime());
            setItemName(stack, "§a" + floor.getDisplayName() + " §7(Unlocked)");
        } else if (canAfford) {
            stack = new ItemStack(Items.STAINED_GLASS_PANE.yellow());
            setItemName(stack, "§e" + floor.getDisplayName() + " §7(Click to unlock)");
        } else {
            stack = new ItemStack(Items.STAINED_GLASS_PANE.red());
            setItemName(stack, "§c" + floor.getDisplayName() + " §7(Locked)");
        }
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        
        if (unlocked) {
            int completions = data.getCompletions(floor);
            int sRanks = data.getSRanks(floor);
            int sPlusRanks = data.getSPlusRanks(floor);
            
            lore.add(Component.literal("§7Completions: §e" + completions));
            lore.add(Component.literal("§7S Ranks: §6" + sRanks));
            lore.add(Component.literal("§7S+ Ranks: §d" + sPlusRanks));
            lore.add(Component.empty());
            
            // Show remaining rewards
            int rewardClears = Math.min(completions, 10);
            int bonusClears = Math.min(Math.max(0, completions - 10), 40);
            int tokensEarned = rewardClears * floor.getBaseReward() + bonusClears * Math.max(1, floor.getBaseReward() / 10);
            lore.add(Component.literal("§7Tokens earned: §6" + tokensEarned));
            
            if (completions < 50) {
                int remaining = 50 - completions;
                lore.add(Component.literal("§7Rewarded clears left: §e" + remaining));
            } else {
                lore.add(Component.literal("§8Max rewards reached"));
            }
        } else {
            lore.add(Component.literal("§7Unlock cost: §e" + cost + " tokens"));
            lore.add(Component.literal("§7You have: " + (canAfford ? "§a" : "§c") + tokens + " tokens"));
            lore.add(Component.empty());
            
            if (canAfford) {
                lore.add(Component.literal("§eClick to unlock!"));
            } else {
                lore.add(Component.literal("§cNeed " + (cost - tokens) + " more tokens"));
            }
            
            lore.add(Component.empty());
            lore.add(Component.literal("§7Rewards per clear:"));
            lore.add(Component.literal("§8• §7First 10: §e" + floor.getBaseReward() + " tokens"));
            lore.add(Component.literal("§8• §7Clears 11-50: §e" + Math.max(1, floor.getBaseReward() / 10) + " tokens"));
        }
        
        setItemLore(stack, lore);
        
        if (unlocked) {
            stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        }
        
        return stack;
    }

    private ItemStack createModeToggleItem() {
        ItemStack stack = new ItemStack(showingMasterMode ? Items.WITHER_SKELETON_SKULL : Items.ZOMBIE_HEAD);
        setItemName(stack, showingMasterMode ? "§c§lMaster Mode" : "§a§lNormal Mode");
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.literal("§7Click to switch to:"));
        lore.add(Component.literal(showingMasterMode ? "§a▶ Normal Mode" : "§c▶ Master Mode"));
        
        setItemLore(stack, lore);
        return stack;
    }

    private ItemStack createStatsItem(DungeonData data) {
        ItemStack stack = new ItemStack(Items.BOOK);
        setItemName(stack, "§6Statistics");
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.literal("§7Total completions: §e" + data.getTotalCompletions()));
        lore.add(Component.literal("§7Total S ranks: §6" + data.getTotalSRanks()));
        lore.add(Component.literal("§7Total S+ ranks: §d" + data.getTotalSPlusRanks()));
        
        setItemLore(stack, lore);
        return stack;
    }

    private ItemStack createRewardsInfoItem() {
        ItemStack stack = new ItemStack(Items.GOLD_INGOT);
        setItemName(stack, "§6Reward Info");
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.literal("§7Each floor gives tokens for clears:"));
        lore.add(Component.literal("§8• §7First 10 clears: Full reward"));
        lore.add(Component.literal("§8• §7Clears 11-50: 10% reward"));
        lore.add(Component.literal("§8• §7After 50: No reward"));
        lore.add(Component.empty());
        lore.add(Component.literal("§7The first 10 clears of each floor"));
        lore.add(Component.literal("§7give enough to unlock the next!"));
        
        setItemLore(stack, lore);
        return stack;
    }

    private void clearContainer() {
        for (int i = 0; i < SIZE; i++) {
            container.setItem(i, ItemStack.EMPTY);
        }
    }

    private void fillBorder() {
        ItemStack pane = new ItemStack(Items.STAINED_GLASS_PANE.gray());
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
        
        // Mode toggle
        if (name.contains("Mode")) {
            playClick();
            showingMasterMode = !showingMasterMode;
            loadScreen();
            return;
        }
        
        // Floor click - check if it's an unlock action
        if (name.contains("Click to unlock")) {
            // Find which floor was clicked
            int[] slots = {19, 20, 21, 22, 23, 24, 25};
            for (int i = 0; i < slots.length; i++) {
                if (slotId == slots[i]) {
                    DungeonFloor floor = DungeonFloor.fromNumber(i + 1, showingMasterMode);
                    if (floor != null) {
                        playClick();
                        DungeonUnlockScreen.open(floor);
                    }
                    return;
                }
            }
        }
    }

    private void playClick() {
        Minecraft.getInstance().player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 1.0f, 1.0f);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
