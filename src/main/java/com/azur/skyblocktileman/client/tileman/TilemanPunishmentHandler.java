package com.azur.skyblocktileman.client.tileman;

import com.azur.skyblocktileman.client.tileman.milestone.MilestoneTracker;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.core.Direction;

public final class TilemanPunishmentHandler {

	    private static final int RED = ARGB.color(255, 255, 30, 30);
	    private static final int GREEN = ARGB.color(255, 100, 255, 100);

	private static boolean violating = false;
	private static boolean wasOnSafeBlock = true;
	private static int soundCooldown = 0;

	private TilemanPunishmentHandler() {
	}

	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(TilemanPunishmentHandler::onEndTick);
		HudElementRegistry.addLast(
				Identifier.fromNamespaceAndPath("skyblocktileman", "punishment_overlay"),
				TilemanPunishmentHandler::renderOverlay
		);
	}

	    private static void onEndTick(Minecraft client) {
	        TilemanConfig config = TilemanConfig.getInstance();
	        if (client.player == null || client.level == null
	                || !config.isEnabled()
	                || !config.isPunishmentEnabled()) {
	            violating = false;
	            wasOnSafeBlock = true;
	            soundCooldown = 0;
	            return;
	        }

	        if (TilemanFirstBlockMode.isActive()) {
	            violating = false;
	            wasOnSafeBlock = true;
	            soundCooldown = 0;
	            return;
	        }

	        BlockPos playerPos = client.player.blockPosition();
	        double playerY = client.player.getY();
	        BlockPos standingOn = findStandingBlock(client.level, client.player.getX(), playerY, client.player.getZ());
	        violating = !TilemanState.getInstance().isUnlocked(standingOn);

		if (!violating) {
			wasOnSafeBlock = true;
			soundCooldown = 0;
			MilestoneTracker.getInstance().startFlawlessTracking();
			return;
		}

		if (wasOnSafeBlock) {
			TilemanState.getInstance().addRuleBreak();
			MilestoneTracker.getInstance().onRuleBreak();
			MilestoneTracker.getInstance().resetFlawlessTracking();
			wasOnSafeBlock = false;
		}

		if (soundCooldown <= 0) {
			client.level.playLocalSound(client.player, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 1.0F, 1.0F);
			soundCooldown = config.getSoundIntervalTicks();
		} else {
			soundCooldown--;
		}
	}

	    private static void renderOverlay(GuiGraphicsExtractor graphics, DeltaTracker tracker) {
	        Minecraft client = Minecraft.getInstance();
	        if (!TilemanConfig.getInstance().isEnabled()) {
	            return;
	        }

	        if (TilemanFirstBlockMode.isActive()) {
	            int centerX = graphics.guiWidth() / 2;
	            graphics.pose().pushMatrix();
	            graphics.pose().translate(centerX, graphics.guiHeight() / 4);
	            graphics.pose().scale(2.0F, 2.0F);
	            graphics.centeredText(client.font, Component.literal("SELECT YOUR FIRST BLOCK"), 0, 0, GREEN);
	            graphics.pose().popMatrix();

	            graphics.pose().pushMatrix();
	            graphics.pose().translate(centerX, graphics.guiHeight() / 4 + 30);
	            graphics.centeredText(client.font, Component.literal("Hold B and click a block to start"), 0, 0, GREEN);
	            graphics.pose().popMatrix();
	            return;
	        }

	        if (TilemanSelectionMode.isActive() && TilemanState.getInstance().getShop().isRemoteUnlockPending()) {
	            int centerX = graphics.guiWidth() / 2;
	            graphics.pose().pushMatrix();
	            graphics.pose().translate(centerX, graphics.guiHeight() / 4);
	            graphics.pose().scale(2.0F, 2.0F);
	            graphics.centeredText(client.font, Component.literal("REMOTE UNLOCK READY"), 0, 0, ARGB.color(255, 100, 200, 255));
	            graphics.pose().popMatrix();

	            graphics.pose().pushMatrix();
	            graphics.pose().translate(centerX, graphics.guiHeight() / 4 + 30);
	            graphics.centeredText(client.font, Component.literal("Click any block to unlock"), 0, 0, ARGB.color(255, 180, 180, 180));
	            graphics.pose().popMatrix();
	        }

	        if (!violating) {
	            return;
	        }

	        int centerX = graphics.guiWidth() / 2;
	        graphics.pose().pushMatrix();
	        graphics.pose().translate(centerX, graphics.guiHeight() / 4);
	        graphics.pose().scale(4.0F, 4.0F);
	        graphics.centeredText(client.font, Component.literal("RULE BROKEN"), 0, 0, RED);
	        graphics.pose().popMatrix();
	    }

	    private static BlockPos findStandingBlock(Level level, double x, double playerY, double z) {
	        int blockX = (int) Math.floor(x);
	        int blockZ = (int) Math.floor(z);
	        
	        for (int yOffset = 0; yOffset >= -2; yOffset--) {
	            int checkY = (int) Math.floor(playerY) + yOffset;
	            BlockPos pos = new BlockPos(blockX, checkY, blockZ);
	            BlockState state = level.getBlockState(pos);
	            
	            if (state.isAir()) {
	                continue;
	            }
	            
	            var shape = state.getCollisionShape(level, pos, CollisionContext.empty());
	            if (shape.isEmpty()) {
	                continue;
	            }
	            
	            double blockTop = checkY + shape.max(Direction.Axis.Y);
	            if (playerY >= blockTop - 0.01 && playerY <= blockTop + 0.5) {
	                return pos;
	            }
	        }
	        
	        return new BlockPos(blockX, (int) Math.floor(playerY) - 1, blockZ);
	    }
	}
