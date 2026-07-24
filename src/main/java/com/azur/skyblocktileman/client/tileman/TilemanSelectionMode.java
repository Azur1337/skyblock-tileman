package com.azur.skyblocktileman.client.tileman;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import org.lwjgl.glfw.GLFW;

public final class TilemanSelectionMode {

    private static final KeyMapping.Category CATEGORY =
        KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath("skyblocktileman", "tileman")
        );

    public static final KeyMapping UNLOCK_MODE_KEY =
        KeyMappingHelper.registerKeyMapping(
            new KeyMapping(
                "key.skyblocktileman.unlock_mode",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_B,
                CATEGORY
            )
        );

    private static final int VALID_TARGET_COLOR = ARGB.color(255, 255, 220, 0);
    private static final float LINE_WIDTH = 3.0F;

    private static boolean clickHandled = false;

    private TilemanSelectionMode() {}

    public static void register() {
        AttackBlockCallback.EVENT.register(TilemanSelectionMode::onAttackBlock);
        LevelRenderEvents.BEFORE_GIZMOS.register(TilemanSelectionMode::onBeforeGizmos);
        ClientTickEvents.END_CLIENT_TICK.register(TilemanSelectionMode::onEndTick);
    }

    private static void onEndTick(Minecraft client) {
        if (client.options.keyAttack != null && !client.options.keyAttack.isDown()) {
            clickHandled = false;
        }
    }

    public static boolean isActive() {
        return TilemanConfig.getInstance().isEnabled() && UNLOCK_MODE_KEY.isDown();
    }

    private static BlockPos getTargetedBlock() {
        Minecraft client = Minecraft.getInstance();
        if (client.hitResult instanceof BlockHitResult blockHit) {
            return blockHit.getBlockPos();
        }
        return null;
    }

    private static BlockPos resolveTargetBlock(BlockPos clicked) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            return clicked;
        }
        return BlockValidation.getStandableBlock(clicked, client.level);
    }

    private static boolean isValidTarget(BlockPos pos) {
        TilemanState state = TilemanState.getInstance();
        BlockPos resolved = resolveTargetBlock(pos);
        
        if (state.isUnlocked(resolved)) {
            return false;
        }
        
        return BlockValidation.isHorizontallyAdjacentToUnlocked(resolved, state);
    }

    private static InteractionResult onAttackBlock(
        Player player,
        Level level,
        InteractionHand hand,
        BlockPos pos,
        Direction direction
    ) {
        if (!TilemanConfig.getInstance().isEnabled() || !isActive()) {
            return InteractionResult.PASS;
        }

        if (TilemanFirstBlockMode.isActive()) {
            return InteractionResult.PASS;
        }

        if (!clickHandled) {
            clickHandled = true;
            tryUnlock(pos, level);
        }
        return InteractionResult.FAIL;
    }

    private static void tryUnlock(BlockPos clicked, Level level) {
        TilemanState state = TilemanState.getInstance();
        BlockPos pos = BlockValidation.getStandableBlock(clicked, level);

        if (state.isUnlocked(pos)) {
            TilemanChat.warn("That block is already unlocked.");
            return;
        }

        if (!BlockValidation.isHorizontallyAdjacentToUnlocked(pos, state)) {
            TilemanChat.warn(
                "You can only unlock blocks adjacent to an already-unlocked block."
            );
            return;
        }

        if (state.getTokens() <= 0) {
            long xpNeeded = state.getXpToNextToken();
            TilemanChat.warn(
                "You don't have any Block Unlock Tokens! Need " +
                    String.format("%,d", xpNeeded) +
                    " more Skill XP for your next one."
            );
            return;
        }

        if (!state.spendToken()) {
            return;
        }

        state.unlockBlock(pos);
        
        String message = "Unlocked block at " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
        if (!pos.equals(clicked)) {
            message += " (adjusted from non-standable block)";
        }
        message += "! (" + state.getTokens() + " token(s) left)";
        
        TilemanChat.info(message);
    }

    private static void onBeforeGizmos(LevelRenderContext context) {
        if (!TilemanConfig.getInstance().isEnabled() || !isActive()) {
            return;
        }

        if (TilemanFirstBlockMode.isActive()) {
            return;
        }

        BlockPos clicked = getTargetedBlock();
        if (clicked == null) {
            return;
        }

        BlockPos target = resolveTargetBlock(clicked);
        if (!isValidTarget(clicked)) {
            return;
        }

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
            VALID_TARGET_COLOR,
            LINE_WIDTH,
            false
        );
        poseStack.popPose();
    }
}
