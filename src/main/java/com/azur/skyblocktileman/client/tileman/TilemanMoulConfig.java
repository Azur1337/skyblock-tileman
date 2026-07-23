package com.azur.skyblocktileman.client.tileman;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.Config;
import io.github.notenoughupdates.moulconfig.annotations.Category;
import io.github.notenoughupdates.moulconfig.common.text.StructuredText;
import net.minecraft.client.Minecraft;

// Root MoulConfig class. Defines categories shown in the sidebar of the config GUI.
public class TilemanMoulConfig extends Config {

    private static final int MOVE_OVERLAY_RUNNABLE = 1;

    @Override
    public StructuredText getTitle() {
        return StructuredText.of("§b§lTileman §7Config");
    }

    @Expose
    @Category(name = "General", desc = "General mod settings")
    public GeneralCategory general = new GeneralCategory();

    @Expose
    @Category(name = "XP & Tokens", desc = "Skill XP tracking and token economy settings")
    public XpTokensCategory xpTokens = new XpTokensCategory();

    @Expose
    @Category(name = "Rendering", desc = "Visual settings for block overlays and highlights")
    public RenderingCategory rendering = new RenderingCategory();

    @Expose
    @Category(name = "Punishment", desc = "Settings for the rule-break punishment system")
    public PunishmentCategory punishment = new PunishmentCategory();

    @Override
    public void executeRunnable(int runnableId) {
        if (runnableId == MOVE_OVERLAY_RUNNABLE) {
            Minecraft.getInstance().gui.setScreen(null);
            TilemanHudOverlay.beginMoveMode();
            return;
        }
        super.executeRunnable(runnableId);
    }

    @Override
    public boolean isValidRunnable(int runnableId) {
        return runnableId == MOVE_OVERLAY_RUNNABLE || super.isValidRunnable(runnableId);
    }
}
