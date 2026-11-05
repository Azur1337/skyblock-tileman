package com.azur.skyblocktileman.client.tileman;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.ARGB;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

// Peak-style health bar: drains while off-tile, refills on-tile, warps to hub once it empties.
public final class TilemanHealthHandler {

    private static final long ON_TILE_GRACE_MS = 1000;
    // the white damage trail slides down over this duration
    private static final float TRAIL_PER_MS = 1f / 2500f;

    private static final int BAR_WIDTH = 72;
    public static final int BAR_HEIGHT = 6;
    private static final int OUTLINE = ARGB.color(255, 245, 235, 210);
    private static final int BACKGROUND = ARGB.color(255, 40, 38, 48);
    private static final int FILL_TOP = ARGB.color(255, 235, 90, 85);
    private static final int FILL_BOTTOM = ARGB.color(255, 170, 45, 45);
    private static final int TRAIL = ARGB.color(180, 255, 255, 255);
    private static final int COLLAPSE_TEXT = ARGB.color(255, 255, 60, 60);

    private static float health = 1f;
    private static float trail = 1f;
    private static long offMs = 0;
    private static long onMs = 0;
    private static long lastTickMs = 0;
    private static long flashUntilMs = 0;

    private TilemanHealthHandler() {}

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(
            TilemanHealthHandler::onEndTick
        );
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
            reset()
        );
        // the bar itself is drawn by TilemanHudOverlay so it lives in the HUD layout
    }

    private static void onEndTick(Minecraft client) {
        long now = System.currentTimeMillis();
        long dt = lastTickMs == 0 ? 0 : Math.min(now - lastTickMs, 1000L);
        lastTickMs = now;

        TilemanConfig config = TilemanConfig.getInstance();
        boolean live =
            client.player != null &&
            client.level != null &&
            TilemanState.getInstance().isTilemanActive() &&
            config.isPunishmentEnabled() &&
            config.isHealthPunishmentEnabled() &&
            !TilemanFirstBlockMode.isActive();

        if (!live) {
            reset();
            return;
        }

        if (TilemanPunishmentHandler.isInTransferGrace()) {
            // the landing is not the player's fault: hold the bar and restart its timers
            offMs = 0;
            onMs = 0;
            return;
        }

        BlockPos standingOn = TilemanPunishmentHandler.findStandingBlock(
            client.level,
            client.player.getX(),
            client.player.getY(),
            client.player.getZ()
        );
        boolean onTile = TilemanState.getInstance().isUnlockedColumn(
            standingOn,
            client.level
        );

        if (onTile) {
            offMs = 0;
            onMs += dt;
            if (onMs >= ON_TILE_GRACE_MS) {
                health = Math.min(
                    1f,
                    health + config.getHealthRegenPerMs() * dt
                );
            }
        } else {
            onMs = 0;
            offMs += dt;
            if (offMs >= config.getOffTileGraceMs()) {
                health -= config.getHealthDrainPerMs() * dt;
            }
        }

        if (trail > health) {
            trail = Math.max(health, trail - TRAIL_PER_MS * dt);
        } else {
            trail = health;
        }

        if (config.isFakeEffectsEnabled()) {
            updateEffect(
                client,
                MobEffects.NAUSEA,
                health < config.getNauseaThreshold(),
                true
            );
            updateEffect(
                client,
                MobEffects.BLINDNESS,
                health < config.getBlindnessThreshold(),
                false
            );
        } else {
            clearEffects(client);
        }

        if (health <= 0f) {
            collapse(client);
        }
    }

    // nausea uses an infinite instance: re-adding it ramps the blend factor and resets the camera wobble phase (the stutter)
    private static void updateEffect(
        Minecraft client,
        Holder<MobEffect> effect,
        boolean shouldHave,
        boolean infinite
    ) {
        if (client.player == null) {
            return;
        }
        MobEffectInstance active = client.player.getEffect(effect);
        if (shouldHave) {
            if (infinite) {
                if (active == null) {
                    client.player.addEffect(
                        new MobEffectInstance(
                            effect,
                            MobEffectInstance.INFINITE_DURATION,
                            0,
                            false,
                            false
                        )
                    );
                }
            } else if (active == null || active.endsWithin(40)) {
                if (active != null) {
                    client.player.removeEffect(effect);
                }
                client.player.addEffect(
                    new MobEffectInstance(effect, 100, 0, false, false)
                );
            }
        } else if (active != null) {
            client.player.removeEffect(effect);
        }
    }

    private static void collapse(Minecraft client) {
        health = 1f;
        trail = 1f;
        offMs = 0;
        onMs = 0;
        flashUntilMs = System.currentTimeMillis() + 600;
        clearEffects(client);

        if (
            TilemanConfig.getInstance().isCollapseWarpEnabled() &&
            client.player != null &&
            client.getConnection() != null &&
            HypixelUtil.isConnectedToHypixel(client)
        ) {
            client
                .getConnection()
                .send(new ServerboundChatCommandPacket("warp hub"));
            TilemanChat.warn(
                "You collapsed! Warping to the hub. Walk your way back."
            );
        } else {
            TilemanChat.warn("You collapsed!");
        }

        if (
            client.player != null &&
            TilemanConfig.getInstance().isCollapseSoundEnabled()
        ) {
            client.player.playSound(SoundEvents.CHAIN_BREAK, 1.0F, 0.6F);
        }
    }

    private static void reset() {
        health = 1f;
        trail = 1f;
        offMs = 0;
        onMs = 0;
        lastTickMs = 0;
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            clearEffects(client);
        }
    }

    private static void clearEffects(Minecraft client) {
        if (client.player == null) {
            return;
        }
        client.player.removeEffect(MobEffects.NAUSEA);
        client.player.removeEffect(MobEffects.BLINDNESS);
    }

    // true when the health bar should be drawn; used by the stats HUD layout
    public static boolean isHealthBarVisible() {
        Minecraft client = Minecraft.getInstance();
        TilemanConfig config = TilemanConfig.getInstance();
        return (
            client.player != null &&
            client.level != null &&
            TilemanState.getInstance().isTilemanActive() &&
            config.isPunishmentEnabled() &&
            config.isHealthPunishmentEnabled() &&
            !TilemanFirstBlockMode.isActive()
        );
    }

    // draws the bar at the given position; called by TilemanHudOverlay under the title
    public static void renderBar(GuiGraphicsExtractor graphics, int x, int y) {
        if (!isHealthBarVisible()) {
            return;
        }
        Minecraft client = Minecraft.getInstance();

        graphics.fill(
            x - 2,
            y - 2,
            x + BAR_WIDTH + 2,
            y + BAR_HEIGHT + 2,
            OUTLINE
        );
        graphics.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, BACKGROUND);

        int trailW = Math.round(trail * BAR_WIDTH);
        int healthW = Math.round(health * BAR_WIDTH);
        if (trailW > healthW) {
            graphics.fill(x + healthW, y, x + trailW, y + BAR_HEIGHT, TRAIL);
        }

        // fill blinks bright red past the blindness threshold, since a text label would hit the stats rows
        boolean blinkWarn =
            health < TilemanConfig.getInstance().getBlindnessThreshold() &&
            (client.level.getGameTime() / 10) % 2 == 0;
        int fillTop = blinkWarn ? COLLAPSE_TEXT : FILL_TOP;
        int fillBottom = blinkWarn ? COLLAPSE_TEXT : FILL_BOTTOM;
        graphics.fill(x, y, x + healthW, y + BAR_HEIGHT - 2, fillTop);
        graphics.fill(
            x,
            y + BAR_HEIGHT - 2,
            x + healthW,
            y + BAR_HEIGHT,
            fillBottom
        );

        // brief white flash over the bar right after a collapse
        long now = System.currentTimeMillis();
        if (now < flashUntilMs && (now / 100) % 2 == 0) {
            graphics.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, TRAIL);
        }
    }
}
