package com.azur.skyblocktileman.client.tileman.ui;

import com.azur.skyblocktileman.client.tileman.TilemanConfig;
import com.azur.skyblocktileman.client.tileman.TilemanState;
import com.azur.skyblocktileman.client.tileman.dungeon.DungeonProgressScreen;
import com.azur.skyblocktileman.client.tileman.milestone.MilestoneScreen;
import com.azur.skyblocktileman.client.tileman.shop.TilemanShopScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

// Root chest menu opened by bare /tileman: entry to shop, milestones, dungeons and config.
public class TilemanHubScreen extends TilemanChestScreen {

    private TilemanHubScreen() {
        super(4, Component.literal("Tileman"));
    }

    public static void open() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }
        client.schedule(() -> {
            TilemanHubScreen screen = new TilemanHubScreen();
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

        setItem(
            slot(1, 4),
            TilemanItem.of(Items.GOLD_INGOT)
                .name(Theme.NAME + "Your Tokens")
                .blank()
                .value(Fmt.tokens(TilemanState.getInstance().getTokens()))
                .build()
        );

        addButton(
            slot(2, 2),
            Items.CHEST,
            "Shop",
            "Spend tokens on upgrades",
            () -> TilemanShopScreen.open(TilemanHubScreen::open)
        );
        addButton(
            slot(2, 4),
            Items.BOOK,
            "Milestones",
            "Earn tokens by hitting milestones",
            () -> MilestoneScreen.open(TilemanHubScreen::open)
        );
        addButton(
            slot(2, 6),
            Items.WITHER_SKELETON_SKULL,
            "Dungeons",
            "Unlock floors and view progress",
            () -> DungeonProgressScreen.open(TilemanHubScreen::open)
        );

        int settings = slot(footerRow(), 8);
        setItem(settings, Theme.settings());
        onClick(settings, () -> {
            playNav();
            Minecraft client = Minecraft.getInstance();
            client.execute(() -> {
                client.gui.setScreen(null);
                TilemanConfig.getInstance().openConfigScreen();
            });
        });
    }

    private void addButton(
        int slot,
        Item icon,
        String label,
        String desc,
        Runnable action
    ) {
        setItem(
            slot,
            TilemanItem.of(icon)
                .name(Theme.NAME + label)
                .blank()
                .desc(desc)
                .action("Click to open")
                .build()
        );
        onClick(slot, () -> {
            playNav();
            action.run();
        });
    }
}
