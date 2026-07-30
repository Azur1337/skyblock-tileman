package com.azur.skyblocktileman.client.tileman.milestone;

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

public class MilestoneScreen extends ContainerScreen {

    private static final int ROWS = 6;
    private static final int SIZE = ROWS * 9;

    private MilestoneCategory currentCategory = null;
    private final SimpleContainer container;

    public MilestoneScreen(ChestMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.container = (SimpleContainer) menu.getContainer();
    }

    public static void open() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        client.schedule(() -> {
            SimpleContainer container = new SimpleContainer(SIZE);
            ChestMenu menu = new ChestMenu(MenuType.GENERIC_9x6, 0, client.player.getInventory(), container, ROWS);
            MilestoneScreen screen = new MilestoneScreen(menu, client.player.getInventory(), Component.literal("Tileman Milestones"));
            screen.loadMainMenu();
            client.gui.setScreen(screen);
        });
    }

    @Override
    protected void init() {
        super.init();
        if (currentCategory == null) {
            loadMainMenu();
        }
    }

    private void loadMainMenu() {
        currentCategory = null;
        clearContainer();
        fillBorder();

        int slot = 10;
        for (MilestoneCategory category : MilestoneCategory.values()) {
            if (slot == 17) slot = 19;
            if (slot > 25) break;
            
            setItem(slot, createCategoryItem(category));
            slot++;
        }

        setItem(40, createStatsItem());
    }

    private void loadCategory(MilestoneCategory category) {
        currentCategory = category;
        clearContainer();
        fillBorder();

        setItem(45, createBackItem());

        List<MilestoneType> milestones = getMilestonesForCategory(category);
        int slot = 10;
        for (MilestoneType type : milestones) {
            if (slot == 17) slot = 19;
            if (slot == 26) slot = 28;
            if (slot == 35) slot = 37;
            if (slot > 43) break;

            setItem(slot, createMilestoneItem(type));
            slot++;
        }
    }

    private List<MilestoneType> getMilestonesForCategory(MilestoneCategory category) {
        List<MilestoneType> list = new ArrayList<>();
        for (MilestoneType type : MilestoneType.values()) {
            if (type.getCategory() == category) {
                list.add(type);
            }
        }
        return list;
    }

    private ItemStack createCategoryItem(MilestoneCategory category) {
        ItemStack stack = new ItemStack(getCategoryIcon(category));
        setItemName(stack, "§a" + category.getDisplayName());

        List<Component> lore = new ArrayList<>();
        int completed = 0;
        int total = 0;
        for (MilestoneType type : MilestoneType.values()) {
            if (type.getCategory() == category) {
                total += type.getMaxTier();
                completed += TilemanState.getInstance().getMilestones().getCompletedTier(type);
            }
        }
        lore.add(Component.literal("§7Progress: §e" + completed + "§7/§e" + total + " §7tiers"));
        lore.add(Component.empty());
        lore.add(Component.literal("§eClick to view!"));
        setItemLore(stack, lore);

        if (completed == total && total > 0) {
            stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        }

        return stack;
    }

    private net.minecraft.world.item.Item getCategoryIcon(MilestoneCategory category) {
        return switch (category) {
            case PROGRESSION -> Items.GOLDEN_PICKAXE;
            case SKILL -> Items.ENCHANTED_BOOK;
            case CHALLENGE -> Items.GOLDEN_APPLE;
            case FLAWLESS -> Items.DIAMOND;
            case SLAYER -> Items.WITHER_SKELETON_SKULL;
            case LOCATION -> Items.COMPASS;
            case SECRET -> Items.ENDER_EYE;
        };
    }

    private ItemStack createMilestoneItem(MilestoneType type) {
        MilestoneData data = TilemanState.getInstance().getMilestones();
        int completedTier = data.getCompletedTier(type);
        int maxTier = type.getMaxTier();
        long progress = MilestoneTracker.getInstance().getProgressForType(type);

        ItemStack stack = new ItemStack(type.getIcon());

        StringBuilder tierStars = new StringBuilder();
        for (int i = 1; i <= maxTier; i++) {
            tierStars.append(i <= completedTier ? "§6★" : "§8☆");
        }

        setItemName(stack, "§a" + type.getDisplayName() + " " + tierStars);

        List<Component> lore = new ArrayList<>();
        
        if (type.hasDescription()) {
            lore.add(Component.literal("§7" + type.getDescription()));
            lore.add(Component.empty());
        }

        if (completedTier < maxTier) {
            int nextTier = completedTier + 1;
            long target = type.getTargetForTier(nextTier);
            int reward = type.getRewardForTier(nextTier);

            lore.add(Component.literal("§7Progress: §e" + formatNumber(progress) + "§7/§e" + formatNumber(target)));
            
            int percent = (int) Math.min(100, (progress * 100) / target);
            String bar = createProgressBar(percent);
            lore.add(Component.literal(bar + " §7" + percent + "%"));
            
            lore.add(Component.empty());
            lore.add(Component.literal("§7Next: §6" + type.getTierNumeral(nextTier) + " §7(+" + reward + " tokens)"));
        } else {
            lore.add(Component.literal("§a§lCOMPLETED!"));
        }

        if (completedTier > 0) {
            lore.add(Component.empty());
            int totalReward = 0;
            for (int i = 1; i <= completedTier; i++) {
                totalReward += type.getRewardForTier(i);
            }
            lore.add(Component.literal("§7Total earned: §6" + totalReward + " tokens"));
        }

        if (completedTier == maxTier) {
            stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        }

        setItemLore(stack, lore);
        return stack;
    }

    private ItemStack createStatsItem() {
        MilestoneData data = TilemanState.getInstance().getMilestones();
        ItemStack stack = new ItemStack(Items.BOOK);
        setItemName(stack, "§6Milestone Stats");

        int totalCompleted = 0;
        int totalTiers = 0;
        for (MilestoneType type : MilestoneType.values()) {
            totalTiers += type.getMaxTier();
            totalCompleted += data.getCompletedTier(type);
        }

        List<Component> lore = new ArrayList<>();
        lore.add(Component.literal("§7Total progress: §e" + totalCompleted + "§7/§e" + totalTiers + " §7tiers"));
        lore.add(Component.literal("§7Tokens from milestones: §6" + data.getTotalTokensFromMilestones()));
        lore.add(Component.empty());
        lore.add(Component.literal("§7Shop spending: §6" + data.getTotalShopSpent() + " tokens"));
        lore.add(Component.literal("§7Current streak: §e" + data.getNoMistakeStreak() + " tiles"));

        setItemLore(stack, lore);
        return stack;
    }

    private ItemStack createBackItem() {
        ItemStack stack = new ItemStack(Items.ARROW);
        setItemName(stack, "§cBack");
        List<Component> lore = new ArrayList<>();
        lore.add(Component.literal("§7Return to categories"));
        setItemLore(stack, lore);
        return stack;
    }

    private String createProgressBar(int percent) {
        int filled = percent / 5;
        int empty = 20 - filled;
        return "§a" + "▌".repeat(filled) + "§7" + "▌".repeat(empty);
    }

    private String formatNumber(long num) {
        if (num >= 1_000_000_000) {
            return String.format("%.1fB", num / 1_000_000_000.0);
        } else if (num >= 1_000_000) {
            return String.format("%.1fM", num / 1_000_000.0);
        } else if (num >= 1_000) {
            return String.format("%.1fK", num / 1_000.0);
        }
        return String.valueOf(num);
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

        if (currentCategory == null) {
            for (MilestoneCategory category : MilestoneCategory.values()) {
                if (name.contains(category.getDisplayName())) {
                    playClick();
                    loadCategory(category);
                    return;
                }
            }
        } else {
            if (name.contains("Back")) {
                playClick();
                loadMainMenu();
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
