package com.azur.skyblocktileman.client.tileman;

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

// Punishes the player for standing on a block that has not been unlocked.
public final class TilemanPunishmentHandler {

	private static final int RED = ARGB.color(255, 255, 30, 30);

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

		BlockPos standingOn = client.player.blockPosition().below();
		violating = !TilemanState.getInstance().isUnlocked(standingOn);

		if (!violating) {
			wasOnSafeBlock = true;
			soundCooldown = 0;
			return;
		}

		// Only count a new rule break on the transition from safe -> violating
		if (wasOnSafeBlock) {
			TilemanState.getInstance().addRuleBreak();
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
}
