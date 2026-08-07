package com.azur.skyblocktileman.client.tileman;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import com.azur.skyblocktileman.client.tileman.dungeon.DungeonData;
import com.azur.skyblocktileman.client.tileman.dungeon.DungeonFloor;
import com.azur.skyblocktileman.client.tileman.dungeon.DungeonProgressScreen;
import com.azur.skyblocktileman.client.tileman.dungeon.DungeonTracker;
import com.azur.skyblocktileman.client.tileman.shop.TilemanShopScreen;
import com.azur.skyblocktileman.client.tileman.milestone.MilestoneScreen;

public final class TilemanCommands {

    private TilemanCommands() {}

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register(
            (dispatcher, registryAccess) ->
                dispatcher.register(
                    ClientCommands.literal("tileman")
                        .then(
                            ClientCommands.literal("config")
                                .executes(context -> openConfig(context.getSource()))
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
                            ClientCommands.literal("shop")
                                .executes(context -> openShop(context.getSource()))
                        )
                        .then(
                            ClientCommands.literal("milestones")
                                .executes(context -> openMilestones(context.getSource()))
                        )
                        .then(
                            ClientCommands.literal("dungeons")
                                .executes(context -> showDungeonProgress(context.getSource()))
                        )
                        .then(
                            ClientCommands.literal("unlock")
                                .then(
                                    ClientCommands.argument("floor", StringArgumentType.word())
                                        .executes(context -> unlockFloor(
                                            context.getSource(),
                                            StringArgumentType.getString(context, "floor")
                                        ))
                                )
                        )
                )
        );
    }

    private static int toggleEnabled(FabricClientCommandSource source) {
        return setEnabled(source, !TilemanConfig.getInstance().isEnabled());
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

    private static int openConfig(FabricClientCommandSource source) {
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

    private static int openShop(FabricClientCommandSource source) {
        TilemanShopScreen.open();
        return 1;
    }

    private static int openMilestones(FabricClientCommandSource source) {
        MilestoneScreen.open();
        return 1;
    }

    private static int showDungeonProgress(FabricClientCommandSource source) {
        DungeonProgressScreen.open();
        return 1;
    }

    private static int unlockFloor(FabricClientCommandSource source, String floorId) {
        DungeonFloor floor = DungeonFloor.fromId(floorId.toUpperCase());
        
        if (floor == null) {
            source.sendError(Component.literal(
                "[Tileman] Unknown floor: " + floorId + ". Valid floors: F1-F7, M1-M7"
            ));
            return 0;
        }
        
        DungeonData data = TilemanState.getInstance().getDungeonData();
        
        if (data.isUnlocked(floor)) {
            source.sendFeedback(Component.literal(
                "[Tileman] " + floor.getDisplayName() + " is already unlocked!"
            ).withStyle(ChatFormatting.YELLOW));
            return 1;
        }
        
        int cost = floor.getUnlockCost();
        int tokens = TilemanState.getInstance().getTokens();
        
        if (tokens < cost) {
            source.sendError(Component.literal(
                "[Tileman] Not enough tokens! Need " + cost + ", have " + tokens
            ));
            return 0;
        }
        
        boolean success = DungeonTracker.getInstance().tryUnlockFloor(floor);
        
        if (success) {
            source.sendFeedback(Component.literal(
                "[Tileman] Unlocked " + floor.getDisplayName() + "!"
            ).withStyle(ChatFormatting.GREEN));
        }
        
        return success ? 1 : 0;
    }
}
