package com.azur.skyblocktileman.client.tileman;

import com.azur.skyblocktileman.SkyblockTileman;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
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
import net.minecraft.world.InteractionResult;

public final class TilemanHudOverlay {

    private static final int TITLE_COLOR = ARGB.color(255, 90, 220, 255);
    private static final int TEXT_COLOR = ARGB.color(255, 235, 235, 235);
    private static final int DRAG_HINT_COLOR = ARGB.color(255, 255, 220, 0);
    private static final int SAVE_COLOR = ARGB.color(255, 90, 220, 90);
    private static final int CANCEL_COLOR = ARGB.color(255, 220, 90, 90);
    private static final int BTN_BG = ARGB.color(255, 20, 20, 20);
    private static final String SAVE_LABEL = "Save";
    private static final String CANCEL_LABEL = "Cancel";
    private static final int TOOLTIP_BG = ARGB.color(220, 20, 20, 20);
    private static final int TOOLTIP_BORDER = ARGB.color(255, 80, 80, 80);

    private static final Identifier ICON_TOKENS = SkyblockTileman.id(
        "textures/hud/tokens.png"
    );
    private static final Identifier ICON_UNLOCKED = SkyblockTileman.id(
        "textures/hud/unlocked.png"
    );
    private static final Identifier ICON_TOTAL_XP = SkyblockTileman.id(
        "textures/hud/total_xp.png"
    );
    private static final Identifier ICON_NEXT_TOKEN = SkyblockTileman.id(
        "textures/hud/next_token.png"
    );
    private static final Identifier ICON_RULE_BROKEN = SkyblockTileman.id(
        "textures/hud/rule_broken.png"
    );

    private record HudRow(
        Identifier icon,
        String label,
        String value,
        String tooltip
    ) {}

    private static final int ICON_SIZE = 12;
    private static final int LINE_HEIGHT = 14;
    private static final int BTN_PAD_X = 8;
    private static final int BTN_GAP = 10;
    private static final int BTN_H = 12;

    private static boolean moveMode = false;
    private static boolean dragging = false;
    private static boolean draggedThisSession = false;
    private static boolean previousLeftPressed = false;
    private static boolean pendingMouseRelease = false;
    private static int dragOffsetX = 0;
    private static int dragOffsetY = 0;
    private static int savedX = 0;
    private static int savedY = 0;
    private static int saveX = 0;
    private static int saveY = 0;
    private static int saveW = 0;
    private static int saveH = 10;
    private static int cancelX = 0;
    private static int cancelY = 0;
    private static int cancelW = 0;
    private static int cancelH = 10;

    private static int lastHudLeft = 0;
    private static int lastHudTop = 0;
    private static int lastHudWidth = 0;
    private static int lastFirstLineY = 0;
    private static int lastLineHeight = LINE_HEIGHT;
    private static List<HudRow> lastRows = List.of();

    private TilemanHudOverlay() {}

    public static void register() {
        HudElementRegistry.addLast(
            SkyblockTileman.id("tileman_stats_hud"),
            TilemanHudOverlay::renderOverlay
        );
        // clear movement keys before the tick reads them, else we still walk in move mode
        ClientTickEvents.START_CLIENT_TICK.register(
            TilemanHudOverlay::onStartTick
        );

        // clicks are for dragging only: cancel all player interaction while in move mode
        AttackBlockCallback.EVENT.register(
            (player, level, hand, pos, direction) ->
                moveMode ? InteractionResult.FAIL : InteractionResult.PASS
        );
        AttackEntityCallback.EVENT.register(
            (player, level, hand, entity, hitResult) ->
                moveMode ? InteractionResult.FAIL : InteractionResult.PASS
        );
        UseBlockCallback.EVENT.register((player, level, hand, hitResult) ->
            moveMode ? InteractionResult.FAIL : InteractionResult.PASS
        );
        UseItemCallback.EVENT.register((player, level, hand) ->
            moveMode ? InteractionResult.FAIL : InteractionResult.PASS
        );
    }

    private static void onStartTick(Minecraft client) {
        if (moveMode) {
            suppressPlayerInput(client);
        }
    }

    public static boolean isMoveActive() {
        return moveMode;
    }

    public static void beginMoveMode() {
        TilemanConfig config = TilemanConfig.getInstance();
        savedX = config.getHudX();
        savedY = config.getHudY();
        moveMode = true;
        dragging = false;
        draggedThisSession = false;
        previousLeftPressed = false;
        // release happens in the render loop: the screen-close path grabs the mouse
        // again after this method returns, so releasing here would be undone
        pendingMouseRelease = true;
        TilemanChat.info(
            "Drag the Tileman HUD with left click, then click Save."
        );
    }

    private static void renderOverlay(
        GuiGraphicsExtractor graphics,
        DeltaTracker tracker
    ) {
        TilemanConfig config = TilemanConfig.getInstance();
        boolean statsVisible =
            TilemanState.getInstance().isTilemanActive() &&
            config.isStatsHudEnabled();
        boolean healthVisible = TilemanHealthHandler.isHealthBarVisible();
        if (!statsVisible && !healthVisible && !moveMode) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }

        TilemanState state = TilemanState.getInstance();
        int tokens = state.getTokens();
        boolean allIslands = config.isTilesCountAllIslands();
        int unlockedBlocks = allIslands
            ? state.getTotalUnlockedBlocks()
            : state.getUnlockedBlocks().size();
        long totalXp = state.getTotalSkillXp();
        long currentXp = state.getXpSinceLastToken();
        long xpNeeded = state.getXpForNextToken();
        int ruleBreaks = state.getRuleBreaks();

        List<HudRow> rows = new ArrayList<>();
        if (config.isHudRowTokens()) {
            rows.add(
                new HudRow(
                    ICON_TOKENS,
                    "Tokens",
                    String.valueOf(tokens),
                    "Available tokens to unlock blocks"
                )
            );
        }
        if (config.isHudRowTiles()) {
            rows.add(
                new HudRow(
                    ICON_UNLOCKED,
                    "Blocks",
                    String.valueOf(unlockedBlocks),
                    allIslands
                        ? "Tiles unlocked across all islands"
                        : "Tiles unlocked on this island"
                )
            );
        }
        if (config.isHudRowTotalXp()) {
            rows.add(
                new HudRow(
                    ICON_TOTAL_XP,
                    "Total XP",
                    formatNumber(totalXp),
                    "Total skill XP earned across all skills"
                )
            );
        }
        if (config.isHudRowNextToken()) {
            rows.add(
                new HudRow(
                    ICON_NEXT_TOKEN,
                    "Next",
                    formatNumber(currentXp) + "/" + formatNumber(xpNeeded),
                    "XP progress toward next token"
                )
            );
        }
        if (config.isHudRowRuleBreaks()) {
            rows.add(
                new HudRow(
                    ICON_RULE_BROKEN,
                    "Breaks",
                    String.valueOf(ruleBreaks),
                    "Times you stood on a locked block"
                )
            );
        }

        boolean iconMode = config.isHudIconMode();
        int labelWidth = iconMode ? ICON_SIZE : getMaxLabelWidth(client, rows);

        int maxValueWidth = 0;
        for (HudRow row : rows) {
            maxValueWidth = Math.max(
                maxValueWidth,
                client.font.width(row.value())
            );
        }
        int maxWidth = Math.max(
            client.font.width("Tileman"),
            labelWidth + 4 + maxValueWidth
        );

        // in move mode the full layout is previewed even if stats are toggled off
        boolean statsShown = statsVisible || moveMode;
        int barSlot = healthVisible ? LINE_HEIGHT : 0;
        int totalHeight =
            (statsShown ? LINE_HEIGHT + rows.size() * LINE_HEIGHT : 0) +
            barSlot;

        if (moveMode) {
            updateDragging(
                client,
                config,
                maxWidth,
                totalHeight,
                graphics.guiWidth(),
                graphics.guiHeight()
            );
        }

        int anchorX = Mth.clamp(
            config.getHudX(),
            0,
            Math.max(0, graphics.guiWidth() - 1)
        );
        int anchorY = Mth.clamp(
            config.getHudY(),
            0,
            Math.max(0, graphics.guiHeight() - totalHeight - 4)
        );
        boolean rightAligned = anchorX > graphics.guiWidth() / 2;
        int x = rightAligned
            ? Math.max(0, anchorX - maxWidth)
            : Math.min(
                  anchorX,
                  Math.max(0, graphics.guiWidth() - maxWidth - 4)
              );
        int y = anchorY;

        lastHudLeft = x;
        lastHudTop = y;
        lastHudWidth = maxWidth;
        lastLineHeight = LINE_HEIGHT;
        lastRows = statsShown ? rows : List.of();

        if (moveMode) {
            String hint = "Drag the HUD, then click Save";
            int hintY = Math.max(2, y - 12);
            graphics.text(
                client.font,
                Component.literal(hint),
                x,
                hintY,
                DRAG_HINT_COLOR
            );
            drawMoveButton(
                graphics,
                client,
                cancelX,
                cancelY,
                cancelW,
                cancelH,
                CANCEL_LABEL,
                CANCEL_COLOR
            );
            drawMoveButton(
                graphics,
                client,
                saveX,
                saveY,
                saveW,
                saveH,
                SAVE_LABEL,
                SAVE_COLOR
            );
        }

        int cursorY = y;
        if (statsShown) {
            graphics.text(
                client.font,
                Component.literal("Tileman"),
                x,
                cursorY,
                TITLE_COLOR
            );
            cursorY += LINE_HEIGHT;
        }

        if (barSlot > 0) {
            if (healthVisible) {
                TilemanHealthHandler.renderBar(graphics, x, cursorY + 2);
            }
            cursorY += barSlot;
        }

        lastFirstLineY = cursorY;

        int lineY = cursorY;

        if (statsShown) {
            for (HudRow row : rows) {
                if (iconMode) {
                    renderIcon(graphics, row.icon(), x, lineY);
                } else {
                    graphics.text(
                        client.font,
                        Component.literal(row.label()),
                        x,
                        lineY + 2,
                        TEXT_COLOR
                    );
                }
                graphics.text(
                    client.font,
                    Component.literal(row.value()),
                    x + labelWidth + 4,
                    lineY + 2,
                    TEXT_COLOR
                );
                lineY += LINE_HEIGHT;
            }
        }

        renderTooltipIfHovered(graphics, client);
    }

    private static int getMaxLabelWidth(Minecraft client, List<HudRow> rows) {
        int max = 0;
        for (HudRow row : rows) {
            max = Math.max(max, client.font.width(row.label()));
        }
        return max;
    }

    private static void renderIcon(
        GuiGraphicsExtractor graphics,
        Identifier texture,
        int x,
        int y
    ) {
        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            texture,
            x,
            y,
            0,
            0,
            ICON_SIZE,
            ICON_SIZE,
            ICON_SIZE,
            ICON_SIZE
        );
    }

    private static void renderTooltipIfHovered(
        GuiGraphicsExtractor graphics,
        Minecraft client
    ) {
        if (!canShowTooltip(client)) {
            return;
        }

        double mouseX = getScaledMouseX(client, graphics.guiWidth());
        double mouseY = getScaledMouseY(client, graphics.guiHeight());

        if (mouseX < lastHudLeft || mouseX > lastHudLeft + lastHudWidth) {
            return;
        }

        int firstLineY = lastFirstLineY;
        int hoveredIndex = -1;

        for (int i = 0; i < lastRows.size(); i++) {
            int lineTop = firstLineY + i * lastLineHeight;
            int lineBottom = lineTop + lastLineHeight;
            if (mouseY >= lineTop && mouseY < lineBottom) {
                hoveredIndex = i;
                break;
            }
        }

        if (hoveredIndex < 0 || hoveredIndex >= lastRows.size()) {
            return;
        }

        String tooltip = lastRows.get(hoveredIndex).tooltip();
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

        graphics.fill(
            tooltipX - 1,
            tooltipY - 1,
            tooltipX + tooltipWidth + 1,
            tooltipY + tooltipHeight + 1,
            TOOLTIP_BORDER
        );
        graphics.fill(
            tooltipX,
            tooltipY,
            tooltipX + tooltipWidth,
            tooltipY + tooltipHeight,
            TOOLTIP_BG
        );
        graphics.text(
            client.font,
            Component.literal(tooltip),
            tooltipX + 4,
            tooltipY + 3,
            TEXT_COLOR
        );
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
        return (
            className.contains("chat") ||
            className.contains("inventory") ||
            className.contains("container")
        );
    }

    private static double getScaledMouseX(Minecraft client, int guiWidth) {
        return (
            (client.mouseHandler.xpos() * guiWidth) /
            client.getWindow().getScreenWidth()
        );
    }

    private static double getScaledMouseY(Minecraft client, int guiHeight) {
        return (
            (client.mouseHandler.ypos() * guiHeight) /
            client.getWindow().getScreenHeight()
        );
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

        double mouseX =
            (client.mouseHandler.xpos() * guiWidth) /
            client.getWindow().getScreenWidth();
        double mouseY =
            (client.mouseHandler.ypos() * guiHeight) /
            client.getWindow().getScreenHeight();
        boolean leftPressed = client.mouseHandler.isLeftPressed();

        if (pendingMouseRelease && client.gui.screen() == null) {
            // wait out any grab from the screen-close path, then free the cursor
            client.mouseHandler.releaseMouse();
            pendingMouseRelease = false;
        }

        layoutMoveButtons(client, guiWidth, guiHeight);

        int anchorX = Mth.clamp(config.getHudX(), 0, Math.max(0, guiWidth - 1));
        int anchorY = Mth.clamp(
            config.getHudY(),
            0,
            Math.max(0, guiHeight - hudHeight - 4)
        );
        boolean rightAligned = anchorX > guiWidth / 2;
        int left = rightAligned
            ? Math.max(0, anchorX - hudWidth)
            : Math.min(anchorX, Math.max(0, guiWidth - hudWidth - 4));
        int top = anchorY;
        int right = left + hudWidth;
        int bottom = top + hudHeight;

        if (leftPressed && !previousLeftPressed) {
            if (inRect(mouseX, mouseY, saveX, saveY, saveW, saveH)) {
                config.save();
                endMoveMode(client);
                TilemanChat.info("HUD position saved.");
            } else if (
                inRect(mouseX, mouseY, cancelX, cancelY, cancelW, cancelH)
            ) {
                config.setHudPositionTransient(savedX, savedY);
                endMoveMode(client);
                TilemanChat.info("HUD move cancelled.");
            } else if (
                mouseX >= left &&
                mouseX <= right &&
                mouseY >= top &&
                mouseY <= bottom
            ) {
                dragging = true;
                draggedThisSession = true;
                dragOffsetX = (int) Math.round(mouseX) - anchorX;
                dragOffsetY = (int) Math.round(mouseY) - anchorY;
            }
        }

        if (!leftPressed) {
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

    private static void layoutMoveButtons(
        Minecraft client,
        int guiWidth,
        int guiHeight
    ) {
        int sw = client.font.width(SAVE_LABEL);
        int cw = client.font.width(CANCEL_LABEL);
        int saveCellW = sw + BTN_PAD_X * 2;
        int cancelCellW = cw + BTN_PAD_X * 2;
        int totalW = cancelCellW + BTN_GAP + saveCellW;
        int startX = (guiWidth - totalW) / 2;
        int boxY = guiHeight - 46;
        saveX = startX;
        saveY = boxY;
        saveW = saveCellW;
        saveH = BTN_H;
        cancelX = startX + saveCellW + BTN_GAP;
        cancelY = boxY;
        cancelW = cancelCellW;
        cancelH = BTN_H;
    }

    private static void drawMoveButton(
        GuiGraphicsExtractor graphics,
        Minecraft client,
        int x,
        int y,
        int w,
        int h,
        String label,
        int color
    ) {
        graphics.fill(x, y, x + w, y + h, BTN_BG);
        graphics.outline(x, y, w, h, color);
        graphics.centeredText(
            client.font,
            Component.literal(label),
            x + w / 2,
            y + (h - 9) / 2,
            color
        );
    }

    private static boolean inRect(
        double mx,
        double my,
        int x,
        int y,
        int w,
        int h
    ) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private static void endMoveMode(Minecraft client) {
        moveMode = false;
        dragging = false;
        draggedThisSession = false;
        pendingMouseRelease = false;
        client.mouseHandler.grabMouse();
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
