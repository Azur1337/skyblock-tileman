package com.azur.skyblocktileman.client.tileman.milestone;

import com.azur.skyblocktileman.client.tileman.TilemanState;
import com.azur.skyblocktileman.client.tileman.ui.Fmt;
import com.azur.skyblocktileman.client.tileman.ui.Theme;
import com.azur.skyblocktileman.client.tileman.ui.TilemanChestScreen;
import com.azur.skyblocktileman.client.tileman.ui.TilemanItem;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class MilestoneScreen extends TilemanChestScreen {

    private MilestoneCategory currentCategory = null;

    private MilestoneScreen() {
        super(6, Component.literal("Tileman Milestones"));
    }

    public static void open() {
        open(null);
    }

    public static void open(Runnable back) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }
        client.schedule(() -> {
            MilestoneScreen screen = new MilestoneScreen();
            screen.setReturnTo(back);
            screen.loadView();
            client.gui.setScreen(screen);
        });
    }

    @Override
    protected void refresh() {
        loadView();
    }

    private void loadView() {
        if (currentCategory == null) {
            loadMainMenu();
        } else {
            loadCategory(currentCategory);
        }
    }

    private void loadMainMenu() {
        buildFrame();

        MilestoneCategory[] categories = MilestoneCategory.values();
        int[] slots = {
            slot(1, 2),
            slot(1, 4),
            slot(1, 6),
            slot(2, 2),
            slot(2, 4),
            slot(2, 6),
        };
        for (int i = 0; i < categories.length && i < slots.length; i++) {
            addCategory(slots[i], categories[i]);
        }

        setItem(slot(4, 4), createStatsItem());
    }

    private void addCategory(int slot, MilestoneCategory category) {
        int completed = 0;
        int total = 0;
        for (MilestoneType type : MilestoneType.values()) {
            if (type.getCategory() == category) {
                total += type.getMaxTier();
                completed += TilemanState.getInstance()
                    .getMilestones()
                    .getCompletedTier(type);
            }
        }

        TilemanItem item = TilemanItem.of(getCategoryIcon(category))
            .name(
                Theme.NAME +
                    category.getDisplayName() +
                    " " +
                    Theme.DESC +
                    "(" +
                    completed +
                    "/" +
                    total +
                    ")"
            )
            .blank()
            .desc("Progress: " + completed + "/" + total + " tiers")
            .action("Click to view");
        if (total > 0 && completed == total) {
            item.glint();
        }

        setItem(slot, item.build());
        onClick(slot, () -> {
            playNav();
            currentCategory = category;
            loadView();
        });
    }

    private Item getCategoryIcon(MilestoneCategory category) {
        return switch (category) {
            case PROGRESSION -> Items.GOLDEN_PICKAXE;
            case SKILL -> Items.ENCHANTED_BOOK;
            case SLAYER -> Items.WITHER_SKELETON_SKULL;
            case CHALLENGE -> Items.GOLDEN_APPLE;
            case FLAWLESS -> Items.DIAMOND;
            case SECRET -> Items.ENDER_EYE;
        };
    }

    private void loadCategory(MilestoneCategory category) {
        buildFrame();
        addBack(this::goMainMenu);

        int index = 0;
        for (MilestoneType type : MilestoneType.values()) {
            if (type.getCategory() != category) {
                continue;
            }
            int row = 1 + index / 7;
            if (row > footerRow() - 1) {
                break;
            }
            setItem(
                slot(row, 1 + (index % 7)),
                createMilestoneItem(type).build()
            );
            index++;
        }
    }

    private void goMainMenu() {
        currentCategory = null;
        loadView();
    }

    private TilemanItem createMilestoneItem(MilestoneType type) {
        MilestoneData data = TilemanState.getInstance().getMilestones();
        int completedTier = data.getCompletedTier(type);
        int maxTier = type.getMaxTier();
        long progress = MilestoneTracker.getInstance().getProgressForType(type);

        TilemanItem item = TilemanItem.of(type.getIcon())
            .name(
                Theme.NAME +
                    type.getDisplayName() +
                    " " +
                    Theme.DESC +
                    "(Tier " +
                    completedTier +
                    "/" +
                    maxTier +
                    ")"
            )
            .blank();

        if (type.hasDescription()) {
            item.desc(type.getDescription()).blank();
        }

        if (completedTier < maxTier) {
            int nextTier = completedTier + 1;
            long target = type.getTargetForTier(nextTier);
            int reward = type.getRewardForTier(nextTier);
            long shown = Math.min(progress, target);

            item.desc(
                "Progress: " + Fmt.compact(shown) + "/" + Fmt.compact(target)
            );
            item.line(
                barColor(progress, target) +
                    " " +
                    percentOf(progress, target) +
                    "%"
            );
            item.blank();
            item.desc(
                "Next: " +
                    type.getTierNumeral(nextTier) +
                    " (+" +
                    reward +
                    " tokens)"
            );
        } else {
            item.good("COMPLETED!");
        }

        if (completedTier > 0) {
            int totalReward = 0;
            for (int i = 1; i <= completedTier; i++) {
                totalReward += type.getRewardForTier(i);
            }
            item.blank();
            item.value("Total earned: " + totalReward + " tokens");
        }

        if (completedTier == maxTier) {
            item.glint();
        }

        return item;
    }

    private static final int BAR_WIDTH = 20;

    private String barColor(long progress, long target) {
        int filled = (int) Math.min(
            BAR_WIDTH,
            target <= 0 ? 0 : (progress * BAR_WIDTH) / target
        );
        return (
            "§a" + "#".repeat(filled) + "§8" + "-".repeat(BAR_WIDTH - filled)
        );
    }

    private int percentOf(long progress, long target) {
        if (target <= 0) return 100;
        return (int) Math.min(100, (progress * 100) / target);
    }

    private ItemStack createStatsItem() {
        MilestoneData data = TilemanState.getInstance().getMilestones();

        int totalCompleted = 0;
        int totalTiers = 0;
        for (MilestoneType type : MilestoneType.values()) {
            totalTiers += type.getMaxTier();
            totalCompleted += data.getCompletedTier(type);
        }

        return TilemanItem.of(Items.BOOK)
            .name(Theme.NAME + "Milestone Stats")
            .blank()
            .desc(
                "Total progress: " +
                    totalCompleted +
                    "/" +
                    totalTiers +
                    " tiers"
            )
            .value(
                "Tokens from milestones: " + data.getTotalTokensFromMilestones()
            )
            .value("Shop spending: " + data.getTotalShopSpent() + " tokens")
            .desc("Current streak: " + data.getNoMistakeStreak() + " tiles")
            .build();
    }
}
