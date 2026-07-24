package com.azur.skyblocktileman.client.tileman;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class RenderingCategory {

    @Expose
    @ConfigOption(name = "Show Unlocked Overlay", desc = "Render a green highlight on the top face of unlocked blocks near you.")
    @ConfigEditorBoolean
    public boolean showUnlockedOverlay = true;

    @Expose
    @ConfigOption(name = "Highlight Radius", desc = "How far away (in blocks) unlocked block highlights are rendered.")
    @ConfigEditorSlider(minValue = 5, maxValue = 30, minStep = 1)
    public int highlightRadius = 15;

    @Expose
    @ConfigOption(name = "Show Stats HUD", desc = "Show Tileman stats HUD with tokens, unlocked blocks, XP and rule breaks.")
    @ConfigEditorBoolean
    public boolean showStatsHud = true;

    @Expose
    @ConfigOption(name = "HUD Icon Mode", desc = "Show icons instead of text labels in the stats HUD.")
    @ConfigEditorBoolean
    public boolean hudIconMode = true;

    @Expose
    @ConfigOption(name = "HUD X", desc = "Horizontal HUD position in screen pixels.")
    @ConfigEditorSlider(minValue = 0, maxValue = 600, minStep = 1)
    public int hudX = 8;

    @Expose
    @ConfigOption(name = "HUD Y", desc = "Vertical HUD position in screen pixels.")
    @ConfigEditorSlider(minValue = 0, maxValue = 400, minStep = 1)
    public int hudY = 8;

    @Expose
    @ConfigOption(name = "Move Overlay", desc = "Close config and drag the HUD in-game.")
    @ConfigEditorButton(runnableId = 1, buttonText = "Drag HUD")
    public int moveOverlayButton = 0;
}
