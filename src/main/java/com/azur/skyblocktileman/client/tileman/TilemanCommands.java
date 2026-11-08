package com.azur.skyblocktileman.client.tileman;

import com.azur.skyblocktileman.client.tileman.dungeon.DungeonData;
import com.azur.skyblocktileman.client.tileman.dungeon.DungeonFloor;
import com.azur.skyblocktileman.client.tileman.dungeon.DungeonProgressScreen;
import com.azur.skyblocktileman.client.tileman.dungeon.DungeonTracker;
import com.azur.skyblocktileman.client.tileman.milestone.MilestoneScreen;
import com.azur.skyblocktileman.client.tileman.shop.TilemanShopScreen;
import com.azur.skyblocktileman.client.tileman.ui.TilemanHubScreen;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class TilemanCommands {

    private TilemanCommands() {}

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register(
            (dispatcher, registryAccess) ->
                dispatcher.register(
                    ClientCommands.literal("tileman")
                        .executes(context -> openHub())
                        .then(
                            ClientCommands.literal("config").executes(context ->
                                openConfig()
                            )
                        )
                        .then(
                            ClientCommands.literal("debug")
                                .executes(context ->
                                    toggleDebug(context.getSource())
                                )
                                .then(
                                    ClientCommands.literal("on").executes(
                                        context ->
                                            setDebug(context.getSource(), true)
                                    )
                                )
                                .then(
                                    ClientCommands.literal("off").executes(
                                        context ->
                                            setDebug(context.getSource(), false)
                                    )
                                )
                        )
                        .then(
                            ClientCommands.literal("enable")
                                .executes(context ->
                                    toggleEnabled(context.getSource())
                                )
                                .then(
                                    ClientCommands.literal("on").executes(
                                        context ->
                                            setEnabled(
                                                context.getSource(),
                                                true
                                            )
                                    )
                                )
                                .then(
                                    ClientCommands.literal("off").executes(
                                        context ->
                                            setEnabled(
                                                context.getSource(),
                                                false
                                            )
                                    )
                                )
                        )
                        .then(
                            ClientCommands.literal("optin")
                                .then(
                                    ClientCommands.literal("on").executes(
                                        context ->
                                            setOptIn(context.getSource(), true)
                                    )
                                )
                                .then(
                                    ClientCommands.literal("off").executes(
                                        context ->
                                            setOptIn(context.getSource(), false)
                                    )
                                )
                        )
                        .then(
                            ClientCommands.literal("overlay")
                                .executes(context ->
                                    toggleOverlay(context.getSource())
                                )
                                .then(
                                    ClientCommands.literal("on").executes(
                                        context ->
                                            setOverlay(
                                                context.getSource(),
                                                true
                                            )
                                    )
                                )
                                .then(
                                    ClientCommands.literal("off").executes(
                                        context ->
                                            setOverlay(
                                                context.getSource(),
                                                false
                                            )
                                    )
                                )
                        )
                        .then(
                            ClientCommands.literal("shop").executes(context ->
                                openShop()
                            )
                        )
                        .then(
                            ClientCommands.literal("milestones").executes(
                                context -> openMilestones()
                            )
                        )
                        .then(
                            ClientCommands.literal("dungeons").executes(
                                context -> showDungeonProgress()
                            )
                        )
                        .then(
                            ClientCommands.literal("unlock").then(
                                ClientCommands.argument(
                                    "floor",
                                    StringArgumentType.word()
                                ).executes(context ->
                                    unlockFloor(
                                        context.getSource(),
                                        StringArgumentType.getString(
                                            context,
                                            "floor"
                                        )
                                    )
                                )
                            )
                        )
                        .then(
                            ClientCommands.literal("balance")
                                .then(
                                    ClientCommands.literal("reload").executes(
                                        context ->
                                            balanceReload(context.getSource())
                                    )
                                )
                                .then(
                                    ClientCommands.literal("dump").executes(
                                        context ->
                                            balanceDump(context.getSource())
                                    )
                                )
                                .then(
                                    ClientCommands.literal("status").executes(
                                        context ->
                                            balanceStatus(context.getSource())
                                    )
                                )
                        )
                        .then(
                            ClientCommands.literal("backups").executes(
                                context -> listBackups(context.getSource())
                            )
                        )
                        .then(
                            ClientCommands.literal("restore").then(
                                ClientCommands.argument(
                                    "backup",
                                    StringArgumentType.word()
                                ).executes(context ->
                                    restoreBackup(
                                        context.getSource(),
                                        StringArgumentType.getString(
                                            context,
                                            "backup"
                                        )
                                    )
                                )
                            )
                        )
                )
        );
    }

    private static int toggleEnabled(FabricClientCommandSource source) {
        return setEnabled(source, !TilemanConfig.getInstance().isEnabled());
    }

    private static int balanceReload(FabricClientCommandSource source) {
        source.sendFeedback(
            Component.literal("[Tileman] " + Balance.reload()).withStyle(
                ChatFormatting.GOLD
            )
        );
        return 1;
    }

    private static int balanceDump(FabricClientCommandSource source) {
        source.sendFeedback(
            Component.literal("[Tileman] " + Balance.dump()).withStyle(
                ChatFormatting.GOLD
            )
        );
        return 1;
    }

    private static int balanceStatus(FabricClientCommandSource source) {
        StringBuilder bands = new StringBuilder();
        for (int i = 0; i < TilemanState.bandCount(); i++) {
            if (i > 0) {
                bands.append("/");
            }
            bands.append(TilemanState.tokenBandCost(i));
        }
        String error = Balance.lastError();
        source.sendFeedback(
            Component.literal(
                "[Tileman] Balance: " +
                    Balance.overrideCount() +
                    " overrides active. Bands: " +
                    bands +
                    ". XP guard: " +
                    SkillXpParser.maxReasonableXpGain() +
                    (error != null ? ". Last reload error: " + error : "")
            ).withStyle(ChatFormatting.GOLD)
        );
        return 1;
    }

    private static int listBackups(FabricClientCommandSource source) {
        List<Path> files = SaveBackups.list();
        if (files.isEmpty()) {
            source.sendFeedback(
                Component.literal(
                    "[Tileman] No save backups yet. One is taken on every load and every 10 minutes."
                ).withStyle(ChatFormatting.GRAY)
            );
            return 0;
        }
        source.sendFeedback(
            Component.literal(
                "[Tileman] Save backups (newest first): " + files.size()
            ).withStyle(ChatFormatting.GOLD)
        );
        for (int i = files.size() - 1; i >= 0; i--) {
            Path file = files.get(i);
            String size = "";
            try {
                size = " (" + Files.size(file) / 1024 + " KB)";
            } catch (IOException ignored) {}
            source.sendFeedback(
                Component.literal(
                    "  #" +
                        (files.size() - i) +
                        "  " +
                        file.getFileName() +
                        size
                ).withStyle(ChatFormatting.GRAY)
            );
        }
        source.sendFeedback(
            Component.literal(
                "[Tileman] Restore with /tileman restore <number or name>."
            ).withStyle(ChatFormatting.GRAY)
        );
        return 1;
    }

    private static int restoreBackup(
        FabricClientCommandSource source,
        String backup
    ) {
        Path file = SaveBackups.find(backup);
        if (file == null) {
            source.sendError(
                Component.literal(
                    "[Tileman] No backup matches \"" +
                        backup +
                        "\". See /tileman backups."
                )
            );
            return 0;
        }
        String error = TilemanState.getInstance().restoreFrom(file);
        if (error != null) {
            source.sendError(Component.literal("[Tileman] " + error));
            return 0;
        }
        source.sendFeedback(
            Component.literal(
                "[Tileman] Restored " +
                    file.getFileName() +
                    ". The replaced save was backed up first."
            ).withStyle(ChatFormatting.GOLD)
        );
        return 1;
    }

    private static int setOptIn(
        FabricClientCommandSource source,
        boolean optIn
    ) {
        TilemanState state = TilemanState.getInstance();
        if (
            optIn &&
            !state.hasProgress() &&
            !TilemanLoginHandler.isCurrentProfileNew()
        ) {
            source.sendError(
                Component.literal(
                    "[Tileman] Tileman mode can only be enabled on profiles created after the mod was installed."
                )
            );
            return 0;
        }
        state.setTilemanMode(optIn);
        source.sendFeedback(
            Component.literal(
                "[Tileman] Tileman mode for this profile: " +
                    (optIn ? "ON" : "OFF")
            ).withStyle(ChatFormatting.GOLD)
        );
        return 1;
    }

    private static int setEnabled(
        FabricClientCommandSource source,
        boolean enabled
    ) {
        TilemanConfig.getInstance().setEnabled(enabled);
        source.sendFeedback(
            Component.literal(
                "[Tileman] Mod enabled: " + (enabled ? "ON" : "OFF")
            ).withStyle(ChatFormatting.GOLD)
        );
        return 1;
    }

    private static int openHub() {
        TilemanHubScreen.open();
        return 1;
    }

    private static int openConfig() {
        Minecraft.getInstance().schedule(() ->
            TilemanConfig.getInstance().openConfigScreen()
        );
        return 1;
    }

    private static int toggleDebug(FabricClientCommandSource source) {
        return setDebug(source, !TilemanConfig.getInstance().isDebugMode());
    }

    private static int setDebug(
        FabricClientCommandSource source,
        boolean enabled
    ) {
        TilemanConfig.getInstance().setDebugMode(enabled);
        source.sendFeedback(
            Component.literal(
                "[Tileman] Debug mode: " + (enabled ? "ON" : "OFF")
            ).withStyle(ChatFormatting.GOLD)
        );
        return 1;
    }

    private static int toggleOverlay(FabricClientCommandSource source) {
        return setOverlay(
            source,
            !TilemanConfig.getInstance().isShowUnlockedOverlay()
        );
    }

    private static int setOverlay(
        FabricClientCommandSource source,
        boolean enabled
    ) {
        TilemanConfig.getInstance().setShowUnlockedOverlay(enabled);
        source.sendFeedback(
            Component.literal(
                "[Tileman] Unlocked block overlay: " + (enabled ? "ON" : "OFF")
            ).withStyle(ChatFormatting.GOLD)
        );
        return 1;
    }

    private static int openShop() {
        TilemanShopScreen.open();
        return 1;
    }

    private static int openMilestones() {
        MilestoneScreen.open();
        return 1;
    }

    private static int showDungeonProgress() {
        DungeonProgressScreen.open();
        return 1;
    }

    private static int unlockFloor(
        FabricClientCommandSource source,
        String floorId
    ) {
        DungeonFloor floor = DungeonFloor.fromId(
            floorId.toUpperCase(Locale.ROOT)
        );

        if (floor == null) {
            source.sendError(
                Component.literal(
                    "[Tileman] Unknown floor: " +
                        floorId +
                        ". Valid floors: F1-F7, M1-M7"
                )
            );
            return 0;
        }

        DungeonData data = TilemanState.getInstance().getDungeonData();

        if (data.isUnlocked(floor)) {
            source.sendFeedback(
                Component.literal(
                    "[Tileman] " +
                        floor.getDisplayName() +
                        " is already unlocked!"
                ).withStyle(ChatFormatting.YELLOW)
            );
            return 1;
        }

        int cost = floor.getUnlockCost();
        int tokens = TilemanState.getInstance().getTokens();

        if (tokens < cost) {
            source.sendError(
                Component.literal(
                    "[Tileman] Not enough tokens! Need " +
                        cost +
                        ", have " +
                        tokens
                )
            );
            return 0;
        }

        boolean success = DungeonTracker.getInstance().tryUnlockFloor(floor);

        if (success) {
            source.sendFeedback(
                Component.literal(
                    "[Tileman] Unlocked " + floor.getDisplayName() + "!"
                ).withStyle(ChatFormatting.GREEN)
            );
        }

        return success ? 1 : 0;
    }
}
