package com.azur.skyblocktileman.client.tileman;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

public final class TilemanHudOverlay {

    private static final int TITLE_COLOR = ARGB.color(255, 90, 220, 255);
    private static final int TEXT_COLOR = ARGB.color(255, 235, 235, 235);
    private static final int DRAG_HINT_COLOR = ARGB.color(255, 255, 220, 0);
    private static final int TOOLTIP_BG = ARGB.color(220, 20, 20, 20);
    private static final int TOOLTIP_BORDER = ARGB.color(255, 80, 80, 80);

    private static final Identifier ICON_TOKENS = Identifier.fromNamespaceAndPath("skyblocktileman", "textures/hud/tokens.png");
    private static final Identifier ICON_UNLOCKED = Identifier.fromNamespaceAndPath("skyblocktileman", "textures/hud/unlocked.png");
    private static final Identifier ICON_TOTAL_XP = Identifier.fromNamespaceAndPath("skyblocktileman", "textures/hud/total_xp.png");
    private static final Identifier ICON_NEXT_TOKEN = Identifier.fromNamespaceAndPath("skyblocktileman", "textures/hud/next_token.png");
    private static final Identifier ICON_RULE_BROKEN = Identifier.fromNamespaceAndPath("skyblocktileman", "textures/hud/rule_broken.png");

    private static final Identifier[] ICONS = {
        ICON_TOKENS,
        ICON_UNLOCKED,
        ICON_TOTAL_XP,
        ICON_NEXT_TOKEN,
        ICON_RULE_BROKEN,
    };

    private static final String[] LABELS = {
        "Tokens",
        "Blocks",
        "Total XP",
        "Next",
        "Breaks",
    };

    private static final String[] TOOLTIPS = {
        "Available tokens to unlock blocks",
        "Total blocks you have unlocked",
        "Total skill XP earned across all skills",
        "XP progress toward next token",
        "Times you stood on a locked block",
    };

    private static final int ICON_SIZE = 12;
    private static final int LINE_HEIGHT = 14;

    private static boolean moveMode = false;
    private static boolean dragging = false;
    private static boolean draggedThisSession = false;
    private static boolean previousLeftPressed = false;
    private static int dragOffsetX = 0;
    private static int dragOffsetY = 0;

    private static int lastHudLeft = 0;
    private static int lastHudTop = 0;
    private static int lastHudWidth = 0;
    private static int lastLineHeight = LINE_HEIGHT;
    private static int lastLineCount = 0;

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
        long currentXp = state.getXpSinceLastToken();
        long xpNeeded = state.getXpForNextToken();
        int ruleBreaks = state.getRuleBreaks();

        String[] values = new String[] {
            String.valueOf(tokens),
            String.valueOf(unlockedBlocks),
            formatNumber(totalXp),
            formatNumber(currentXp) + "/" + formatNumber(xpNeeded),
            String.valueOf(ruleBreaks),
        };

        boolean iconMode = config.isHudIconMode();
        int labelWidth = iconMode ? ICON_SIZE : getMaxLabelWidth(client);
        
        int maxValueWidth = 0;
        for (String value : values) {
            maxValueWidth = Math.max(maxValueWidth, client.font.width(value));
        }
        int maxWidth = Math.max(client.font.width("Tileman"), labelWidth + 4 + maxValueWidth);
        
        int totalHeight = LINE_HEIGHT + (values.length * LINE_HEIGHT);

        if (moveMode) {
            updateDragging(client, config, maxWidth, totalHeight, graphics.guiWidth(), graphics.guiHeight());
        }

        int anchorX = Mth.clamp(config.getHudX(), 0, Math.max(0, graphics.guiWidth() - 1));
        int anchorY = Mth.clamp(config.getHudY(), 0, Math.max(0, graphics.guiHeight() - totalHeight - 4));
        boolean rightAligned = anchorX > (graphics.guiWidth() / 2);
        int x = rightAligned ? Math.max(0, anchorX - maxWidth) : Math.min(anchorX, Math.max(0, graphics.guiWidth() - maxWidth - 4));
        int y = anchorY;

        lastHudLeft = x;
        lastHudTop = y;
        lastHudWidth = maxWidth;
        lastLineHeight = LINE_HEIGHT;
        lastLineCount = values.length;

        if (moveMode) {
            String hint = "Drag HUD, release to save";
            int hintY = Math.max(2, y - 12);
            graphics.text(client.font, Component.literal(hint), x, hintY, DRAG_HINT_COLOR);
        }

        graphics.text(client.font, Component.literal("Tileman"), x, y, TITLE_COLOR);
        
        int lineY = y + LINE_HEIGHT;
        
        for (int i = 0; i < values.length; i++) {
            if (iconMode) {
                renderIcon(graphics, ICONS[i], x, lineY);
            } else {
                graphics.text(client.font, Component.literal(LABELS[i]), x, lineY + 2, TEXT_COLOR);
            }
            graphics.text(client.font, Component.literal(values[i]), x + labelWidth + 4, lineY + 2, TEXT_COLOR);
            lineY += LINE_HEIGHT;
        }

        renderTooltipIfHovered(graphics, client);
    }

    private static int getMaxLabelWidth(Minecraft client) {
        int max = 0;
        for (String label : LABELS) {
            max = Math.max(max, client.font.width(label));
        }
        return max;
    }

    private static void renderIcon(GuiGraphicsExtractor graphics, Identifier texture, int x, int y) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
    }

    private static void renderTooltipIfHovered(GuiGraphicsExtractor graphics, Minecraft client) {
        if (!canShowTooltip(client)) {
            return;
        }

        double mouseX = getScaledMouseX(client, graphics.guiWidth());
        double mouseY = getScaledMouseY(client, graphics.guiHeight());

        if (mouseX < lastHudLeft || mouseX > lastHudLeft + lastHudWidth) {
            return;
        }

        int firstLineY = lastHudTop + lastLineHeight;
        int hoveredIndex = -1;
        
        for (int i = 0; i < lastLineCount; i++) {
            int lineTop = firstLineY + (i * lastLineHeight);
            int lineBottom = lineTop + lastLineHeight;
            if (mouseY >= lineTop && mouseY < lineBottom) {
                hoveredIndex = i;
                break;
            }
        }

        if (hoveredIndex < 0 || hoveredIndex >= TOOLTIPS.length) {
            return;
        }

        String tooltip = TOOLTIPS[hoveredIndex];
        int tooltipWidth = client.font.width(tooltip) + 8;
        int tooltipHeight = 14;
        
        int tooltipX = (int) mouseX + 12;
        int tooltipY = (int) mouseY - 4;
        
        if (tooltipX + tooltipWidth > graphics.guiWidth()) {
            tooltipX = (int) mouseX - tooltipWidth - 4;
        }
        if (tooltipY + tooltipHeight > graphics.guiHeight()) {
            tooltipY = graphics.guiHeight() - tooltipHeight;
        }
        if (tooltipY < 0) {
            tooltipY = 0;
        }

        graphics.fill(tooltipX - 1, tooltipY - 1, tooltipX + tooltipWidth + 1, tooltipY + tooltipHeight + 1, TOOLTIP_BORDER);
        graphics.fill(tooltipX, tooltipY, tooltipX + tooltipWidth, tooltipY + tooltipHeight, TOOLTIP_BG);
        graphics.text(client.font, Component.literal(tooltip), tooltipX + 4, tooltipY + 3, TEXT_COLOR);
    }

    private static boolean canShowTooltip(Minecraft client) {
        Screen screen = client.gui.screen();
        if (screen == null) {
            return false;
        }
        if (screen instanceof AbstractContainerScreen) {
            return true;
        }
        String className = screen.getClass().getName().toLowerCase();
        return className.contains("chat") || className.contains("inventory") || className.contains("container");
    }

    private static double getScaledMouseX(Minecraft client, int guiWidth) {
        return client.mouseHandler.xpos() * guiWidth / client.getWindow().getScreenWidth();
    }

    private static double getScaledMouseY(Minecraft client, int guiHeight) {
        return client.mouseHandler.ypos() * guiHeight / client.getWindow().getScreenHeight();
    }

    private static String formatNumber(long num) {
        if (num >= 1_000_000) {
            return String.format("%.1fM", num / 1_000_000.0);
        } else if (num >= 1_000) {
            return String.format("%.1fK", num / 1_000.0);
        }
        return String.valueOf(num);
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
