package com.example.client.mixin;

import com.example.client.tileman.TilemanState;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Intercepts action bar text packets (which Hypixel Skyblock uses to show
 * live Skill XP gains) and feeds any Skill XP drops straight into
 * {@link TilemanState}, so the token count updates instantly, without
 * waiting on the (slower) Hypixel API baseline.
 * <p>
 * Example matched text: "+13.2 Combat (45.6%)" -> group(1) = "13.2" XP gained.
 */
@Mixin(ClientPacketListener.class)
public class TilemanActionBarMixin {

	// Matches "+<amount> <SkillName> (" as it appears in the Hypixel Skyblock action bar,
	// e.g. "+50 Farming (12.3%)". We deliberately don't need the percentage group.
	private static final Pattern SKILL_XP_PATTERN = Pattern.compile(
			"\\+([0-9]+(?:\\.[0-9]+)?) (Farming|Mining|Combat|Foraging|Fishing|Enchanting|Alchemy|Carpentry|Runecrafting|Social|Taming|Hunting) \\("
	);

	@Inject(method = "setActionBarText", at = @At("HEAD"))
	private void tileman$onActionBar(ClientboundSetActionBarTextPacket packet, CallbackInfo ci) {
		String text = packet.text().getString();

		Matcher matcher = SKILL_XP_PATTERN.matcher(text);
		while (matcher.find()) {
			try {
				double xpGained = Double.parseDouble(matcher.group(1));
				TilemanState.getInstance().onSkillXpGained(xpGained);
			} catch (NumberFormatException ignored) {
				// Malformed/unexpected number format - skip this match rather than crash.
			}
		}
	}
}
