package com.azur.skyblocktileman.client.tileman;

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
}
