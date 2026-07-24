package com.azur.skyblocktileman.client.tileman;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;

public final class TilemanFirstBlockMode {

    private static final int HIGHLIGHT_COLOR = ARGB.color(255, 0, 255, 100);
    private static final float LINE_WIDTH = 3.0F;

    private static boolean active = false;
    private static String islandName = "";
    private static boolean clickHandled = false;
    private static boolean wasKeyDown = false;

    private TilemanFirstBlockMode() {}

    public static void register() {
        AttackBlockCallback.EVENT.register(TilemanFirstBlockMode::onAttackBlock);
        LevelRenderEvents.BEFORE_GIZMOS.register(TilemanFirstBlockMode::onBeforeGizmos);
        ClientTickEvents.END_CLIENT_TICK.register(TilemanFirstBlockMode::onEndTick);
    }

    public static void activate(String displayName) {
        active = true;
        islandName = displayName;
        wasKeyDown = false;
        TilemanLog.debug(DebugCategory.BLOCKS, "First block selection mode activated for {}", displayName);
        showActivationMessage();
    }

    public static void deactivate() {
        active = false;
        islandName = "";
    }

    public static boolean isActive() {
        return active;
    }

    private static void showActivationMessage() {
        TilemanChat.info("No blocks unlocked on this island! Hold B and click to select your first free block.");
    }

    private static void showHoldingMessage() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }
        client.player.sendSystemMessage(
            Component.literal("§a§lClick any block to unlock it as your starting point!")
        );
    }

    private static void onEndTick(Minecraft client) {
        if (!active) {
            return;
        }

        if (client.options.keyAttack != null && !client.options.keyAttack.isDown()) {
            clickHandled = false;
        }

        TilemanState state = TilemanState.getInstance();
        if (!state.getUnlockedBlocks().isEmpty()) {
            deactivate();
            return;
        }

        boolean keyDown = TilemanSelectionMode.UNLOCK_MODE_KEY.isDown();
        if (keyDown && !wasKeyDown) {
            showHoldingMessage();
        }
        wasKeyDown = keyDown;
    }

    private static BlockPos getTargetedBlock() {
        Minecraft client = Minecraft.getInstance();
        if (client.hitResult instanceof BlockHitResult blockHit) {
            return blockHit.getBlockPos();
        }
        return null;
    }

    private static InteractionResult onAttackBlock(
        Player player,
        Level level,
        InteractionHand hand,
        BlockPos pos,
        Direction direction
    ) {
        if (!active || !TilemanSelectionMode.UNLOCK_MODE_KEY.isDown()) {
            return InteractionResult.PASS;
        }

        if (!clickHandled) {
            clickHandled = true;
            unlockFirstBlock(pos);
        }
        return InteractionResult.FAIL;
    }

    private static void unlockFirstBlock(BlockPos clicked) {
        Minecraft client = Minecraft.getInstance();
        TilemanState state = TilemanState.getInstance();
        
        BlockPos pos = BlockValidation.getStandableBlock(clicked, client.level);

        state.unlockBlock(pos);
        
        String message = "Unlocked your first block on " + islandName + " at " +
            pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
        if (!pos.equals(clicked)) {
            message += " (adjusted from non-standable block)";
        }
        message += "!";
        
        TilemanChat.info(message);
        TilemanLog.debug(DebugCategory.BLOCKS, "First block unlocked at {} on {}", pos, islandName);

        deactivate();
    }

    private static void onBeforeGizmos(LevelRenderContext context) {
        if (!active || !TilemanSelectionMode.UNLOCK_MODE_KEY.isDown()) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        BlockPos clicked = getTargetedBlock();
        if (clicked == null) {
            return;
        }

        BlockPos target = BlockValidation.getStandableBlock(clicked, client.level);

        Vec3 cameraPos = context.levelState().cameraRenderState.pos;
        PoseStack poseStack = context.poseStack();
        SubmitNodeCollector collector = context.submitNodeCollector();

        poseStack.pushPose();
        poseStack.translate(
            target.getX() - cameraPos.x,
            target.getY() - cameraPos.y,
            target.getZ() - cameraPos.z
        );
        collector.submitShapeOutline(
            poseStack,
            Shapes.block(),
            RenderTypes.lines(),
            HIGHLIGHT_COLOR,
            LINE_WIDTH,
            false
        );
        poseStack.popPose();
    }
}
