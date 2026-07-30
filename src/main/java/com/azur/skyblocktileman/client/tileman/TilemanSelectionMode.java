package com.azur.skyblocktileman.client.tileman;

import com.azur.skyblocktileman.client.tileman.milestone.MilestoneTracker;
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
        
        if (state.getShop().isRemoteUnlockPending()) {
            return true;
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
        ShopData shop = state.getShop();
        BlockPos pos = BlockValidation.getStandableBlock(clicked, level);

        if (state.isUnlocked(pos)) {
            TilemanChat.warn("That block is already unlocked.");
            return;
        }

        boolean isRemote = shop.isRemoteUnlockPending();
        
        if (!isRemote && !BlockValidation.isHorizontallyAdjacentToUnlocked(pos, state)) {
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

        if (isRemote) {
            shop.consumeRemoteUnlock();
            state.unlockBlock(pos);
            state.save();
            MilestoneTracker.getInstance().onTileUnlocked();
            TilemanChat.info("Remote unlock used! Unlocked block at " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + "! (" + state.getTokens() + " token(s) left)");
            return;
        }

        int baseTiles = shop.getBaseTilesPerUnlock();
        int tilesToUnlock = shop.calculateTilesToUnlock();
        boolean luckyProc = shop.didLuckyProc(baseTiles, tilesToUnlock);

        int unlocked = unlockMultipleTiles(pos, tilesToUnlock, state, level);
        state.save();

        for (int i = 0; i < unlocked; i++) {
            MilestoneTracker.getInstance().onTileUnlocked();
        }
        if (luckyProc) {
            int multiplier = tilesToUnlock / baseTiles;
            MilestoneTracker.getInstance().onLuckyProc(multiplier);
        }

        StringBuilder message = new StringBuilder();
        message.append("Unlocked ").append(unlocked).append(" tile(s)");
        if (luckyProc) {
            message.append(" (LUCKY!)");
        }
        message.append("! (").append(state.getTokens()).append(" token(s) left)");
        
        TilemanChat.info(message.toString());
    }

    private static final Direction[] HORIZONTAL_DIRECTIONS = {
        Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST
    };

    private static int unlockMultipleTiles(BlockPos start, int count, TilemanState state, Level level) {
        state.unlockBlock(start);
        int unlocked = 1;

        if (count <= 1) {
            return unlocked;
        }

        java.util.List<BlockPos> candidates = new java.util.ArrayList<>();
        java.util.Set<BlockPos> checked = new java.util.HashSet<>();
        checked.add(start);

        for (Direction dir : HORIZONTAL_DIRECTIONS) {
            BlockPos neighbor = start.relative(dir);
            BlockPos standable = BlockValidation.getStandableBlock(neighbor, level);
            if (!state.isUnlocked(standable) && !checked.contains(standable)) {
                candidates.add(standable);
                checked.add(standable);
            }
        }

        java.util.Random random = new java.util.Random();
        while (unlocked < count && !candidates.isEmpty()) {
            int idx = random.nextInt(candidates.size());
            BlockPos next = candidates.remove(idx);
            
            if (state.isUnlocked(next)) {
                continue;
            }

            state.unlockBlock(next);
            unlocked++;

            for (Direction dir : HORIZONTAL_DIRECTIONS) {
                BlockPos neighbor = next.relative(dir);
                BlockPos standable = BlockValidation.getStandableBlock(neighbor, level);
                if (!state.isUnlocked(standable) && !checked.contains(standable)) {
                    candidates.add(standable);
                    checked.add(standable);
                }
            }
        }

        return unlocked;
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
