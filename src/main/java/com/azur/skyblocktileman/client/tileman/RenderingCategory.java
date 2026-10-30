package com.azur.skyblocktileman.client.tileman;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class RenderingCategory {

    @Expose
    @ConfigOption(
        name = "Unlocked Block Overlay",
        desc = "Render a highlight on the top face of nearby unlocked tiles."
    )
    @ConfigEditorBoolean
    public boolean showUnlockedOverlay = true;

    @Expose
    @ConfigOption(name = "Highlight Radius", desc = "Tile highlight range.")
    @ConfigEditorSlider(minValue = 5, maxValue = 30, minStep = 1)
    public int highlightRadius = 15;

    @Expose
    @ConfigOption(
        name = "Stats HUD",
        desc = "Show the stats HUD with tokens, unlocked tiles, XP and rule breaks."
    )
    @ConfigEditorBoolean
    public boolean showStatsHud = true;

    @Expose
    @ConfigOption(
        name = "HUD Icon Mode",
        desc = "Show icons instead of text labels in the stats HUD."
    )
    @ConfigEditorBoolean
    public boolean hudIconMode = true;

    @Expose
    @ConfigOption(
        name = "Move HUD",
        desc = "Close the config and drag the HUD into place, then click Save."
    )
    @ConfigEditorButton(runnableId = 1, buttonText = "Drag HUD")
    public int moveOverlayButton = 0;

    @Expose
    @ConfigOption(
        name = "Tile Compass",
        desc = "Show a beacon and outline on the nearest unlocked tile when standing on a locked one."
    )
    @ConfigEditorBoolean
    public boolean showTileCompass = true;

    @Expose
    @ConfigOption(
        name = "Milestone Popups",
        desc = "Show a short animated banner when you complete a milestone."
    )
    @ConfigEditorBoolean
    public boolean showMilestonePopups = true;

    // saved HUD position, written only by move mode, not edited via config
    @Expose
    public int hudX = 8;

    @Expose
    public int hudY = 8;
}
