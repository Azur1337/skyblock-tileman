package com.azur.skyblocktileman.client.tileman.dungeon;

import com.azur.skyblocktileman.client.tileman.TilemanState;
import com.azur.skyblocktileman.client.tileman.ui.Theme;
import com.azur.skyblocktileman.client.tileman.ui.TilemanChestScreen;
import com.azur.skyblocktileman.client.tileman.ui.TilemanItem;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;

// Confirmation screen for unlocking dungeon floors.
// Cancel, ESC and the barrier all return to the screen that opened it (or close).
public class DungeonUnlockScreen extends TilemanChestScreen {

    private final DungeonFloor floor;
    private final Runnable onBack;
    private boolean returning;

    private DungeonUnlockScreen(DungeonFloor floor, Runnable onBack) {
        super(3, Component.literal("Unlock " + floor.getDisplayName()));
        this.floor = floor;
        this.onBack = onBack;
    }

    public static void open(DungeonFloor floor) {
        open(floor, () -> Minecraft.getInstance().gui.setScreen(null));
    }

    public static void open(DungeonFloor floor, Runnable onBack) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }
        client.schedule(() -> {
            DungeonUnlockScreen screen = new DungeonUnlockScreen(floor, onBack);
            screen.loadView();
            client.gui.setScreen(screen);
        });
    }

    private void loadView() {
        buildFrame();

        int cost = floor.getUnlockCost();
        int tokens = TilemanState.getInstance().getTokens();
        boolean canAfford = tokens >= cost;

        setItem(
            slot(1, 4),
            createFloorInfoItem(cost, tokens, canAfford).build()
        );

        int confirmSlot = slot(1, 2);
        TilemanItem confirm = TilemanItem.of(
            canAfford
                ? Items.DYED_TERRACOTTA.lime()
                : Items.DYED_TERRACOTTA.red()
        )
            .name(
                canAfford
                    ? Theme.GOOD +
                          "Unlock " +
                          Theme.DESC +
                          "(" +
                          cost +
                          " tokens)"
                    : Theme.BAD + "Unlock " + Theme.BAD + "[CANNOT AFFORD]"
            )
            .blank();
        if (canAfford) {
            confirm.action("Click to unlock " + floor.getDisplayName());
        } else {
            confirm.bad(
                "Not enough tokens! Need " + (cost - tokens) + " more."
            );
        }
        setItem(confirmSlot, confirm.build());
        onClick(confirmSlot, this::tryUnlock);

        int cancelSlot = slot(1, 6);
        setItem(cancelSlot, Theme.no());
        onClick(cancelSlot, this::back);
    }

    private void tryUnlock() {
        // tryUnlockFloor owns the deny messages and the success sound
        if (DungeonTracker.getInstance().tryUnlockFloor(floor)) {
            back();
        } else {
            loadView();
        }
    }

    private void back() {
        if (returning) {
            return;
        }
        returning = true;
        playNav();
        onBack.run();
    }

    @Override
    public void onClose() {
        back();
    }

    private TilemanItem createFloorInfoItem(
        int cost,
        int tokens,
        boolean canAfford
    ) {
        TilemanItem item = TilemanItem.of(
            floor.isMasterMode()
                ? Items.WITHER_SKELETON_SKULL
                : Items.ZOMBIE_HEAD
        )
            .name(Theme.NAME + floor.getDisplayName())
            .blank()
            .value("Unlock cost: " + cost + " tokens")
            .desc("You have: " + tokens + " tokens");
        if (canAfford) {
            item.good("You can afford this!");
        } else {
            item.bad("Not enough tokens!");
        }
        item.blank();
        item.desc("Unlocking gives you:");
        item.desc("Free tiles inside the dungeon");
        item.desc("Token rewards from clears");
        item.blank();
        item.desc("Clear rewards:");
        item.desc("First 10 clears: " + floor.getBaseReward() + " tokens each");
        item.desc(
            "Clears 11-50: " +
                Math.max(1, floor.getBaseReward() / 10) +
                " tokens each"
        );
        return item;
    }
}
