package com.azur.skyblocktileman.client.tileman;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

// Renders a movable Tileman stats HUD.
public final class TilemanHudOverlay {

    private static final int TITLE_COLOR = ARGB.color(255, 90, 220, 255);
    private static final int TEXT_COLOR = ARGB.color(255, 235, 235, 235);
    private static final int DRAG_HINT_COLOR = ARGB.color(255, 255, 220, 0);

    private static boolean moveMode = false;
    private static boolean dragging = false;
    private static boolean draggedThisSession = false;
    private static boolean previousLeftPressed = false;
    private static int dragOffsetX = 0;
    private static int dragOffsetY = 0;

    private TilemanHudOverlay() {}

    public static void register() {
        HudElementRegistry.addLast(
            Identifier.fromNamespaceAndPath("skyblocktileman", "tileman_stats_hud"),
            TilemanHudOverlay::renderOverlay
        );
    }

    public static void beginMoveMode() {
        moveMode = true;
        dragging = false;
        draggedThisSession = false;
        previousLeftPressed = false;
        Minecraft client = Minecraft.getInstance();
        client.mouseHandler.releaseMouse();
        TilemanChat.info("Drag the Tileman HUD with left click, then release to save.");
    }

    private static void renderOverlay(
        GuiGraphicsExtractor graphics,
        DeltaTracker tracker
    ) {
        TilemanConfig config = TilemanConfig.getInstance();
        if ((!config.isEnabled() || !config.isStatsHudEnabled()) && !moveMode) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }

        TilemanState state = TilemanState.getInstance();
        int tokens = state.getTokens();
        int unlockedBlocks = state.getUnlockedBlocks().size();
        long totalXp = state.getTotalSkillXp();
        long xpToNext = state.getXpToNextToken();
        int nextCost = state.getCurrentTokenCost();
        int ruleBreaks = state.getRuleBreaks();

        String[] lines = new String[] {
            "Tileman",
            "Tokens: " + tokens,
            "Unlocked: " + unlockedBlocks,
            "XP: " + String.format("%,d", totalXp),
            "Next Token: " + String.format("%,d XP", xpToNext),
            "Rule Breaks: " + ruleBreaks,
        };

        int maxWidth = 0;
        for (String line : lines) {
            maxWidth = Math.max(maxWidth, client.font.width(line));
        }
        int totalHeight = lines.length * 10;

        if (moveMode) {
            updateDragging(client, config, maxWidth, totalHeight, graphics.guiWidth(), graphics.guiHeight());
        }

        int anchorX = Mth.clamp(config.getHudX(), 0, Math.max(0, graphics.guiWidth() - 1));
        int anchorY = Mth.clamp(config.getHudY(), 0, Math.max(0, graphics.guiHeight() - totalHeight - 4));
        boolean rightAligned = anchorX > (graphics.guiWidth() / 2);
        int x = rightAligned ? Math.max(0, anchorX - maxWidth) : Math.min(anchorX, Math.max(0, graphics.guiWidth() - maxWidth - 4));
        int y = anchorY;

        if (moveMode) {
            String hint = "Drag HUD, release to save";
            int hintY = Math.max(2, y - 12);
            graphics.text(client.font, Component.literal(hint), x, hintY, DRAG_HINT_COLOR);
        }

        graphics.text(client.font, Component.literal(lines[0]), x, y, TITLE_COLOR);
        for (int i = 1; i < lines.length; i++) {
            graphics.text(
                client.font,
                Component.literal(lines[i]),
                x,
                y + (i * 10),
                TEXT_COLOR
            );
        }
    }

    private static void updateDragging(
        Minecraft client,
        TilemanConfig config,
        int hudWidth,
        int hudHeight,
        int guiWidth,
        int guiHeight
    ) {
        suppressPlayerInput(client);

        double mouseX = client.mouseHandler.xpos() * guiWidth / client.getWindow().getScreenWidth();
        double mouseY = client.mouseHandler.ypos() * guiHeight / client.getWindow().getScreenHeight();
        boolean leftPressed = client.mouseHandler.isLeftPressed();

        int anchorX = Mth.clamp(config.getHudX(), 0, Math.max(0, guiWidth - 1));
        int anchorY = Mth.clamp(config.getHudY(), 0, Math.max(0, guiHeight - hudHeight - 4));
        boolean rightAligned = anchorX > (guiWidth / 2);
        int left = rightAligned ? Math.max(0, anchorX - hudWidth) : Math.min(anchorX, Math.max(0, guiWidth - hudWidth - 4));
        int top = anchorY;
        int right = left + hudWidth;
        int bottom = top + hudHeight;

        if (leftPressed && !previousLeftPressed) {
            boolean inside = mouseX >= left && mouseX <= right && mouseY >= top && mouseY <= bottom;
            if (inside) {
                dragging = true;
                draggedThisSession = true;
                dragOffsetX = (int) Math.round(mouseX) - anchorX;
                dragOffsetY = (int) Math.round(mouseY) - anchorY;
            }
        }

        if (!leftPressed && previousLeftPressed && draggedThisSession) {
            dragging = false;
            moveMode = false;
            draggedThisSession = false;
            config.save();
            client.mouseHandler.grabMouse();
            TilemanChat.info("HUD position saved.");
        } else if (!leftPressed) {
            dragging = false;
        }

        if (dragging) {
            int nextX = (int) Math.round(mouseX) - dragOffsetX;
            int nextY = (int) Math.round(mouseY) - dragOffsetY;
            nextX = Mth.clamp(nextX, 0, Math.max(0, guiWidth - 1));
            nextY = Mth.clamp(nextY, 0, Math.max(0, guiHeight - hudHeight - 4));
            config.setHudPositionTransient(nextX, nextY);
        }

        previousLeftPressed = leftPressed;
    }

    private static void suppressPlayerInput(Minecraft client) {
        client.options.keyUp.setDown(false);
        client.options.keyLeft.setDown(false);
        client.options.keyDown.setDown(false);
        client.options.keyRight.setDown(false);
        client.options.keyJump.setDown(false);
        client.options.keyShift.setDown(false);
        client.options.keySprint.setDown(false);
        client.options.keyAttack.setDown(false);
        client.options.keyUse.setDown(false);
    }
}
