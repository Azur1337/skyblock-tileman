package com.azur.skyblocktileman.client.tileman;

import com.azur.skyblocktileman.SkyblockTileman;
import com.azur.skyblocktileman.client.tileman.milestone.MilestoneTracker;
import com.azur.skyblocktileman.client.tileman.slayer.SlayerTracker;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;

public final class TilemanPunishmentHandler {

    private static final int RED = ARGB.color(255, 255, 30, 30);
    private static final int GREEN = ARGB.color(255, 100, 255, 100);

    private static boolean violating = false;
    private static boolean wasOnSafeBlock = true;
    private static int soundCooldown = 0;

    // server-driven position changes must not book an instant rule break; see onEndTick
    private static long graceUntilMs = 0;
    private static Level lastLevel = null;
    private static BlockPos lastPos = null;
    // 15 blocks in one tick is beyond any movement, so a larger jump means a teleport
    private static final long TELEPORT_DIST_SQR = 225;

    public static boolean isInTransferGrace() {
        return System.currentTimeMillis() < graceUntilMs;
    }

    private TilemanPunishmentHandler() {}

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(
            TilemanPunishmentHandler::onEndTick
        );
        HudElementRegistry.addLast(
            SkyblockTileman.id("punishment_overlay"),
            TilemanPunishmentHandler::renderOverlay
        );
    }

    private static void onEndTick(Minecraft client) {
        TilemanConfig config = TilemanConfig.getInstance();
        if (
            client.player == null ||
            client.level == null ||
            !TilemanState.getInstance().isTilemanActive() ||
            !config.isPunishmentEnabled()
        ) {
            violating = false;
            wasOnSafeBlock = true;
            soundCooldown = 0;
            lastLevel = null;
            lastPos = null;
            return;
        }

        // a new level instance or a jump no movement could make: warps, respawns, soul teleports
        BlockPos playerPos = client.player.blockPosition();
        boolean transferred = client.level != lastLevel;
        boolean jumped =
            lastPos != null &&
            distanceSq(lastPos, playerPos) > TELEPORT_DIST_SQR;
        lastLevel = client.level;
        lastPos = playerPos;
        if (transferred || jumped) {
            long graceMs = config.getTransferGraceMs();
            graceUntilMs = System.currentTimeMillis() + graceMs;
            TilemanLog.debug(
                DebugCategory.PUNISHMENT,
                "Transfer grace armed for {} ms ({})",
                graceMs,
                transferred ? "server transfer" : "position jump"
            );
        }

        if (TilemanFirstBlockMode.isActive()) {
            violating = false;
            wasOnSafeBlock = true;
            soundCooldown = 0;
            return;
        }

        if (System.currentTimeMillis() < graceUntilMs) {
            // the landing has not settled yet: no break, no taint, no thunder
            violating = false;
            wasOnSafeBlock = true;
            soundCooldown = 0;
            return;
        }

        double playerY = client.player.getY();
        BlockPos standingOn = findStandingBlock(
            client.level,
            client.player.getX(),
            playerY,
            client.player.getZ()
        );

        // Use column-based unlock check to handle jumping and caves properly
        violating = !TilemanState.getInstance().isUnlockedColumn(
            standingOn,
            client.level
        );

        if (!violating) {
            wasOnSafeBlock = true;
            soundCooldown = 0;
            MilestoneTracker.getInstance().startFlawlessTracking();
            return;
        }

        if (wasOnSafeBlock) {
            TilemanState.getInstance().addRuleBreak();
            TilemanLog.debug(
                DebugCategory.PUNISHMENT,
                "Rule break booked at {} on island {}",
                playerPos,
                TilemanState.getInstance().getActiveIsland()
            );
            MilestoneTracker.getInstance().onRuleBreak();
            MilestoneTracker.getInstance().resetFlawlessTracking();
            SlayerTracker.getInstance().onSteppedOnLockedTile();
            wasOnSafeBlock = false;
        }

        if (config.isThunderSoundEnabled()) {
            if (soundCooldown <= 0) {
                client.level.playLocalSound(
                    client.player,
                    SoundEvents.LIGHTNING_BOLT_THUNDER,
                    SoundSource.HOSTILE,
                    1.0F,
                    1.0F
                );
                soundCooldown = config.getSoundIntervalTicks();
            } else {
                soundCooldown--;
            }
        }
    }

    private static void renderOverlay(
        GuiGraphicsExtractor graphics,
        DeltaTracker tracker
    ) {
        Minecraft client = Minecraft.getInstance();
        TilemanConfig config = TilemanConfig.getInstance();
        if (!TilemanState.getInstance().isTilemanActive()) {
            return;
        }

        if (TilemanFirstBlockMode.isActive()) {
            if (config.isIslandBannersEnabled()) {
                int centerX = graphics.guiWidth() / 2;
                graphics.pose().pushMatrix();
                graphics.pose().translate(centerX, graphics.guiHeight() / 4);
                graphics.pose().scale(2.0F, 2.0F);
                graphics.centeredText(
                    client.font,
                    Component.literal("SELECT YOUR FIRST BLOCK"),
                    0,
                    0,
                    GREEN
                );
                graphics.pose().popMatrix();

                graphics.pose().pushMatrix();
                graphics
                    .pose()
                    .translate(centerX, graphics.guiHeight() / 4 + 30);
                graphics.centeredText(
                    client.font,
                    Component.literal("Hold ")
                        .append(
                            TilemanSelectionMode.UNLOCK_MODE_KEY.getTranslatedKeyMessage()
                        )
                        .append(
                            Component.literal(" and click a block to start")
                        ),
                    0,
                    0,
                    GREEN
                );
                graphics.pose().popMatrix();
            }
            return;
        }

        if (
            config.isIslandBannersEnabled() &&
            TilemanSelectionMode.isActive() &&
            TilemanState.getInstance().getShop().isRemoteUnlockPending()
        ) {
            int centerX = graphics.guiWidth() / 2;
            graphics.pose().pushMatrix();
            graphics.pose().translate(centerX, graphics.guiHeight() / 4);
            graphics.pose().scale(2.0F, 2.0F);
            graphics.centeredText(
                client.font,
                Component.literal("REMOTE UNLOCK READY"),
                0,
                0,
                ARGB.color(255, 100, 200, 255)
            );
            graphics.pose().popMatrix();

            graphics.pose().pushMatrix();
            graphics.pose().translate(centerX, graphics.guiHeight() / 4 + 30);
            graphics.centeredText(
                client.font,
                Component.literal("Click any block to unlock"),
                0,
                0,
                ARGB.color(255, 180, 180, 180)
            );
            graphics.pose().popMatrix();
        }

        if (!violating || !config.isRuleBrokenBannerEnabled()) {
            return;
        }

        int centerX = graphics.guiWidth() / 2;
        graphics.pose().pushMatrix();
        graphics.pose().translate(centerX, graphics.guiHeight() / 4);
        graphics.pose().scale(4.0F, 4.0F);
        graphics.centeredText(
            client.font,
            Component.literal("RULE BROKEN"),
            0,
            0,
            RED
        );
        graphics.pose().popMatrix();
    }

    private static long distanceSq(BlockPos a, BlockPos b) {
        long dx = a.getX() - b.getX();
        long dy = a.getY() - b.getY();
        long dz = a.getZ() - b.getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    // fresh standing check for the token economy; deliberately independent of the
    // punishment toggle, and first-block mode counts as safe (nothing to withhold yet)
    public static boolean isPlayerOnUnlockedTile() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) {
            return false;
        }
        if (TilemanFirstBlockMode.isActive()) {
            return true;
        }
        BlockPos standingOn = findStandingBlock(
            client.level,
            client.player.getX(),
            client.player.getY(),
            client.player.getZ()
        );
        return TilemanState.getInstance().isUnlockedColumn(
            standingOn,
            client.level
        );
    }

    // package-visible so the health handler can reuse the exact same standing check
    static BlockPos findStandingBlock(
        Level level,
        double x,
        double playerY,
        double z
    ) {
        int blockX = (int) Math.floor(x);
        int blockZ = (int) Math.floor(z);

        for (int yOffset = 0; yOffset >= -2; yOffset--) {
            int checkY = (int) Math.floor(playerY) + yOffset;
            BlockPos pos = new BlockPos(blockX, checkY, blockZ);
            BlockState state = level.getBlockState(pos);

            if (state.isAir()) {
                continue;
            }

            var shape = state.getCollisionShape(
                level,
                pos,
                CollisionContext.empty()
            );
            if (shape.isEmpty()) {
                continue;
            }

            double blockTop = checkY + shape.max(Direction.Axis.Y);
            // Wider range (up to 1.0 above the top) so stairs, slabs and jumps still count
            if (playerY >= blockTop - 0.1 && playerY <= blockTop + 1.0) {
                return pos;
            }
        }

        return new BlockPos(blockX, (int) Math.floor(playerY) - 1, blockZ);
    }
}
