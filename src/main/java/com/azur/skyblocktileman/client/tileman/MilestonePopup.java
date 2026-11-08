package com.azur.skyblocktileman.client.tileman;

import com.azur.skyblocktileman.SkyblockTileman;
import java.util.ArrayDeque;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

// Animated top-center banner shown when a milestone tier completes
public final class MilestonePopup {

    private static final int IN_MS = 340;
    private static final int OUT_MS = 640;
    private static final int MAX_QUEUE = 6;
    private static final int MIN_VISIBLE_ALPHA = 4;
    private static final int MIN_SPARKLE_ALPHA = 12;
    // ease-in fraction at which the text starts fading up
    private static final float TEXT_REVEAL_AT = 0.55f;

    private static final int CARD_H = 46;
    private static final int TOP_MARGIN = 14;

    private static final int ACCENT = ARGB.color(255, 236, 188, 74);
    private static final int ACCENT_DIM = ARGB.color(255, 150, 110, 30);
    private static final int BG = ARGB.color(255, 14, 12, 10);
    private static final int SHINE = ARGB.color(255, 255, 255, 255);
    private static final int EYEBROW = ARGB.color(255, 236, 188, 74);
    private static final int TITLE = ARGB.color(255, 245, 245, 245);
    private static final int SUBTITLE = ARGB.color(255, 105, 230, 120);

    private MilestonePopup() {}

    private static final ArrayDeque<Entry> QUEUE = new ArrayDeque<>();

    public static void register() {
        HudElementRegistry.addLast(
            SkyblockTileman.id("milestone_popup"),
            MilestonePopup::render
        );
    }

    public static void show(String title, String subtitle) {
        if (!TilemanConfig.getInstance().isMilestonePopupsEnabled()) {
            return;
        }
        if (QUEUE.size() >= MAX_QUEUE) {
            return;
        }
        QUEUE.addLast(new Entry(title, subtitle));
    }

    private static void render(
        GuiGraphicsExtractor graphics,
        DeltaTracker tracker
    ) {
        if (QUEUE.isEmpty()) {
            return;
        }
        if (!TilemanState.getInstance().isTilemanActive()) {
            QUEUE.clear();
            return;
        }
        long now = System.currentTimeMillis();
        Entry head = QUEUE.peekFirst();
        // an entry animates only once it reaches the head, so a burst of milestones
        // plays one after another instead of collapsing into the first
        if (head.startMs == 0L) {
            head.startMs = now;
        }
        while (
            now - head.startMs >=
            TilemanConfig.getInstance().getPopupDurationMs()
        ) {
            QUEUE.pollFirst();
            head = QUEUE.peekFirst();
            if (head == null) {
                return;
            }
            head.startMs = now;
        }
        draw(head, graphics, now);
    }

    private static void draw(
        Entry entry,
        GuiGraphicsExtractor graphics,
        long now
    ) {
        Minecraft client = Minecraft.getInstance();
        int total = TilemanConfig.getInstance().getPopupDurationMs();
        int elapsedMs = (int) (now - entry.startMs);

        float inP = Mth.clamp(elapsedMs / (float) IN_MS, 0f, 1f);
        float easeIn = 1f - (1f - inP) * (1f - inP) * (1f - inP);
        float outP = Mth.clamp(
            (elapsedMs - (total - OUT_MS)) / (float) OUT_MS,
            0f,
            1f
        );
        float life = easeIn * (1f - outP);
        int alpha = (int) (255 * life);
        if (alpha <= MIN_VISIBLE_ALPHA) {
            return;
        }

        int centerX = graphics.guiWidth() / 2;
        int fullW =
            Math.max(
                client.font.width("MILESTONE"),
                Math.max(
                    client.font.width(entry.title),
                    client.font.width(entry.subtitle)
                )
            ) + 28;

        int w = (int) (fullW * (0.35f + 0.65f * easeIn));
        int bob = (int) (Math.sin(elapsedMs / 260f) * 1.2f);
        int x = centerX - w / 2;
        int y = TOP_MARGIN + bob - (int) ((1f - easeIn) * 18f);

        graphics.fill(
            x - 2,
            y - 2,
            x + w + 2,
            y + CARD_H + 2,
            withAlpha(ACCENT_DIM, alpha / 3)
        );
        graphics.fill(
            x,
            y,
            x + w,
            y + CARD_H,
            withAlpha(BG, (int) (210 * life))
        );
        graphics.fill(x, y, x + w, y + 2, withAlpha(ACCENT, alpha));
        graphics.fill(
            x,
            y + CARD_H - 2,
            x + w,
            y + CARD_H,
            withAlpha(ACCENT, alpha)
        );

        drawShine(graphics, entry, x, y, w, elapsedMs, life);
        drawSparkles(graphics, x, y, w, elapsedMs, alpha);

        if (easeIn > TEXT_REVEAL_AT) {
            int textAlpha = (int) (255 *
                Mth.clamp((easeIn - TEXT_REVEAL_AT) / 0.45f, 0f, 1f) *
                (1f - outP));
            int cy = y + 7;
            drawCentered(
                graphics,
                client,
                "MILESTONE",
                centerX,
                cy,
                withAlpha(EYEBROW, textAlpha)
            );
            drawCentered(
                graphics,
                client,
                entry.title,
                centerX,
                cy + 13,
                withAlpha(TITLE, textAlpha)
            );
            drawCentered(
                graphics,
                client,
                entry.subtitle,
                centerX,
                cy + 26,
                withAlpha(SUBTITLE, textAlpha)
            );
        }
    }

    private static void drawShine(
        GuiGraphicsExtractor graphics,
        Entry entry,
        int x,
        int y,
        int w,
        int elapsedMs,
        float life
    ) {
        int span =
            TilemanConfig.getInstance().getPopupDurationMs() - IN_MS - OUT_MS;
        if (elapsedMs <= IN_MS || span <= 0) {
            return;
        }
        float sweep = (elapsedMs - IN_MS) / (float) span;
        int shineW = 16;
        int sx = x + (int) (sweep * (w + shineW * 2)) - shineW;
        int x0 = Math.max(x, sx);
        int x1 = Math.min(x + w, sx + shineW);
        if (x1 > x0) {
            graphics.fill(
                x0,
                y + 2,
                x1,
                y + CARD_H - 2,
                withAlpha(SHINE, (int) (36 * life))
            );
        }
    }

    private static void drawSparkles(
        GuiGraphicsExtractor graphics,
        int x,
        int y,
        int w,
        int elapsedMs,
        int alpha
    ) {
        int cols = 7;
        for (int i = 0; i < cols; i++) {
            float tw = (float) (0.5 +
                0.5 * Math.sin(elapsedMs / 220f + i * 1.7f));
            int sa = (int) (alpha * tw);
            if (sa < MIN_SPARKLE_ALPHA) {
                continue;
            }
            int sx = x + 2 + (int) ((w - 5) * (i / (float) (cols - 1)));
            int c = withAlpha(ACCENT, sa);
            graphics.fill(sx, y - 3, sx + 1, y - 2, c);
            graphics.fill(sx, y + CARD_H + 2, sx + 1, y + CARD_H + 3, c);
        }
    }

    private static void drawCentered(
        GuiGraphicsExtractor graphics,
        Minecraft client,
        String text,
        int centerX,
        int y,
        int color
    ) {
        int x = centerX - client.font.width(text) / 2;
        graphics.text(client.font, Component.literal(text), x, y, color);
    }

    private static int withAlpha(int color, int alpha) {
        return (Mth.clamp(alpha, 0, 255) << 24) | (color & 0x00FFFFFF);
    }

    private static final class Entry {

        final String title;
        final String subtitle;
        long startMs;

        Entry(String title, String subtitle) {
            this.title = title;
            this.subtitle = subtitle;
        }
    }
}
