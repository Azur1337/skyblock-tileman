package com.azur.skyblocktileman.client.tileman.dungeon;

import com.azur.skyblocktileman.client.tileman.TilemanState;
import com.azur.skyblocktileman.client.tileman.ui.Theme;
import com.azur.skyblocktileman.client.tileman.ui.TilemanChestScreen;
import com.azur.skyblocktileman.client.tileman.ui.TilemanItem;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;

public class DungeonProgressScreen extends TilemanChestScreen {

    // static so reopening after the unlock screen keeps the viewed mode
    private static boolean showingMasterMode = false;

    private DungeonProgressScreen() {
        super(6, Component.literal("Dungeon Progress"));
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
            DungeonProgressScreen screen = new DungeonProgressScreen();
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
        buildFrame();

        DungeonData data = TilemanState.getInstance().getDungeonData();
        int tokens = TilemanState.getInstance().getTokens();

        setItem(slot(1, 4), createInfoItem(data, tokens).build());

        for (int i = 0; i < 7; i++) {
            addFloor(
                slot(2, 1 + i),
                DungeonFloor.fromNumber(i + 1, showingMasterMode),
                data,
                tokens
            );
        }

        setItem(slot(4, 2), createStatsItem(data).build());
        addSync(slot(4, 4));
        setItem(slot(4, 6), createRewardsInfoItem().build());
        addModeToggle(slot(footerRow(), 3));
    }

    private void addFloor(
        int slot,
        DungeonFloor floor,
        DungeonData data,
        int tokens
    ) {
        if (floor == null) {
            return;
        }
        boolean unlocked = data.isUnlocked(floor);
        int cost = floor.getUnlockCost();

        TilemanItem item;
        if (unlocked) {
            item = TilemanItem.of(Items.STAINED_GLASS_PANE.lime())
                .name(
                    Theme.NAME +
                        floor.getDisplayName() +
                        " " +
                        Theme.GOOD +
                        "[UNLOCKED]"
                )
                .blank()
                .desc("Completions: " + data.getCompletions(floor));
            int rewardClears = Math.min(data.getCompletions(floor), 10);
            int bonusClears = Math.min(
                Math.max(0, data.getCompletions(floor) - 10),
                40
            );
            int earned =
                rewardClears * floor.getBaseReward() +
                bonusClears * Math.max(1, floor.getBaseReward() / 10);
            item.value("Tokens earned: " + earned);
            if (data.getCompletions(floor) < 50) {
                item.desc(
                    "Rewarded clears left: " + (50 - data.getCompletions(floor))
                );
            } else {
                item.desc("Max rewards reached");
            }
            item.glint();
        } else {
            boolean canAfford = tokens >= cost;
            item = TilemanItem.of(
                canAfford
                    ? Items.STAINED_GLASS_PANE.yellow()
                    : Items.STAINED_GLASS_PANE.red()
            )
                .name(
                    Theme.NAME +
                        floor.getDisplayName() +
                        " " +
                        (canAfford
                            ? Theme.ACTION + "[READY]"
                            : Theme.BAD + "[LOCKED]")
                )
                .blank()
                .value("Unlock cost: " + cost + " tokens");
            if (canAfford) {
                item.action("Click to unlock");
            } else {
                item.bad(
                    "Not enough tokens! Need " + (cost - tokens) + " more."
                );
            }
            item.blank();
            item.desc("Clear rewards:");
            item.desc("First 10: " + floor.getBaseReward() + " tokens each");
            item.desc(
                "Clears 11-50: " +
                    Math.max(1, floor.getBaseReward() / 10) +
                    " tokens each"
            );

            onClick(slot, () -> {
                playNav();
                DungeonUnlockScreen.open(floor, DungeonProgressScreen::open);
            });
        }
        setItem(slot, item.build());
    }

    private void addModeToggle(int slot) {
        setItem(
            slot,
            TilemanItem.of(
                showingMasterMode
                    ? Items.WITHER_SKELETON_SKULL
                    : Items.ZOMBIE_HEAD
            )
                .name(
                    (showingMasterMode ? Theme.BAD : Theme.GOOD) +
                        (showingMasterMode ? "Master Mode" : "Normal Mode")
                )
                .blank()
                .action(
                    "Switch to " +
                        (showingMasterMode ? "Normal" : "Master") +
                        " Mode"
                )
                .build()
        );
        onClick(slot, () -> {
            playNav();
            showingMasterMode = !showingMasterMode;
            loadView();
        });
    }

    private void addSync(int slot) {
        long cooldown =
            DungeonTracker.getInstance().getManualSyncCooldownSeconds();
        TilemanItem item = TilemanItem.of(Items.CLOCK)
            .name(Theme.NAME + "Sync Completions")
            .blank();
        if (cooldown > 0) {
            item.desc("Cooldown: " + cooldown + "s");
        } else {
            item.action("Click to check for new clears");
        }
        item.desc("Auto-syncs every 2 minutes");
        setItem(slot, item.build());
        onClick(slot, () -> {
            if (DungeonTracker.getInstance().requestManualSync()) {
                playNav();
            }
            loadView();
        });
    }

    private TilemanItem createInfoItem(DungeonData data, int tokens) {
        int unlockedNormal = data.getUnlockedNormalFloorCount();
        int unlockedMaster = data.getUnlockedMasterModeFloorCount();
        return TilemanItem.of(Items.NETHER_STAR)
            .name(Theme.NAME + "Dungeon Progress")
            .blank()
            .desc("Your tokens: " + tokens)
            .desc("Normal floors: " + unlockedNormal + "/7")
            .desc("Master floors: " + unlockedMaster + "/7")
            .desc("Total completions: " + data.getTotalCompletions());
    }

    private TilemanItem createStatsItem(DungeonData data) {
        return TilemanItem.of(Items.BOOK)
            .name(Theme.NAME + "Statistics")
            .blank()
            .desc("Total completions: " + data.getTotalCompletions());
    }

    private TilemanItem createRewardsInfoItem() {
        return TilemanItem.of(Items.GOLD_INGOT)
            .name(Theme.NAME + "Reward Info")
            .blank()
            .desc("Each floor gives tokens for clears:")
            .desc("First 10 clears: full reward")
            .desc("Clears 11-50: 10% reward")
            .desc("After 50: no reward");
    }
}
