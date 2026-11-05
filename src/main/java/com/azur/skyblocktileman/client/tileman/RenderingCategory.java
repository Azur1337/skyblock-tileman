package com.azur.skyblocktileman.client.tileman;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.Accordion;
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
        name = "Selection Preview",
        desc = "Outline the tile you are aiming at while in unlock mode."
    )
    @ConfigEditorBoolean
    public boolean selectionPreview = true;

    @Expose
    @ConfigOption(
        name = "Tile Compass",
        desc = "Show a beacon and outline on the nearest unlocked tile when standing on a locked one."
    )
    @ConfigEditorBoolean
    public boolean showTileCompass = true;

    @Expose
    @ConfigOption(
        name = "Island Banners",
        desc = "Center-screen prompts for first block and remote unlock."
    )
    @ConfigEditorBoolean
    public boolean islandBanners = true;

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
        name = "Tiles Row Scope",
        desc = "Count tiles across all islands instead of only the current one."
    )
    @ConfigEditorBoolean
    public boolean tilesCountAllIslands = false;

    @Expose
    @ConfigOption(
        name = "HUD Rows",
        desc = "Choose which rows the stats HUD shows."
    )
    @Accordion
    public HudRows hudRows = new HudRows();

    public static class HudRows {

        @Expose
        @ConfigOption(
            name = "Tokens Row",
            desc = "Show the available token count."
        )
        @ConfigEditorBoolean
        public boolean tokens = true;

        @Expose
        @ConfigOption(
            name = "Tiles Row",
            desc = "Show the unlocked tile count."
        )
        @ConfigEditorBoolean
        public boolean tiles = true;

        @Expose
        @ConfigOption(
            name = "Total XP Row",
            desc = "Show total skill XP earned."
        )
        @ConfigEditorBoolean
        public boolean totalXp = true;

        @Expose
        @ConfigOption(
            name = "Next Token Row",
            desc = "Show XP progress toward the next token."
        )
        @ConfigEditorBoolean
        public boolean nextToken = true;

        @Expose
        @ConfigOption(
            name = "Rule Breaks Row",
            desc = "Show the rule break count."
        )
        @ConfigEditorBoolean
        public boolean ruleBreaks = true;
    }

    @Expose
    @ConfigOption(
        name = "Move HUD",
        desc = "Close the config and drag the HUD into place, then click Save."
    )
    @ConfigEditorButton(runnableId = 1, buttonText = "Drag HUD")
    public int moveOverlayButton = 0;

    @Expose
    @ConfigOption(
        name = "Milestone Popups",
        desc = "Show a short animated banner when you complete a milestone."
    )
    @ConfigEditorBoolean
    public boolean showMilestonePopups = true;

    @Expose
    @ConfigOption(
        name = "Popup Duration",
        desc = "How long a milestone banner stays on screen, in milliseconds."
    )
    @ConfigEditorSlider(minValue = 1500, maxValue = 6000, minStep = 100)
    public int popupDurationMs = 3400;

    // saved HUD position, written only by move mode, not edited via config
    @Expose
    public int hudX = 8;

    @Expose
    public int hudY = 8;
}
