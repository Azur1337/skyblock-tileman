package com.azur.skyblocktileman.client.tileman;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.Accordion;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class PunishmentCategory {

    @Expose
    @ConfigOption(
        name = "Enforce Tile Rules",
        desc = "Off stops rule breaks from counting and disables the Flawless milestones and streaks."
    )
    @ConfigEditorBoolean
    public boolean enabled = true;

    @Expose
    @ConfigOption(
        name = "Health Bar",
        desc = "Peak-style health bar that drains while you stand on a locked tile; an empty bar warps you to the hub."
    )
    @ConfigEditorBoolean
    public boolean healthEnabled = true;

    @Expose
    @ConfigOption(
        name = "Rule Broken Banner",
        desc = "Flash RULE BROKEN on screen when you stand on a locked tile."
    )
    @ConfigEditorBoolean
    public boolean ruleBrokenBanner = true;

    @Expose
    @ConfigOption(
        name = "Thunder Sound",
        desc = "Play thunder while standing on a locked tile. Detection and milestones still run."
    )
    @ConfigEditorBoolean
    public boolean thunderSound = true;

    @Expose
    @ConfigOption(name = "Thunder Interval", desc = "Ticks between thunder.")
    @ConfigEditorSlider(minValue = 5, maxValue = 200, minStep = 5)
    public int soundIntervalTicks = 100;

    @Expose
    @ConfigOption(
        name = "Fake Effects",
        desc = "Nausea and blindness as the health bar drops. The bar still drains without them."
    )
    @ConfigEditorBoolean
    public boolean fakeEffects = true;

    @Expose
    @ConfigOption(
        name = "Collapse Warp",
        desc = "Send /warp hub when the health bar empties. Off just refills the bar."
    )
    @ConfigEditorBoolean
    public boolean collapseWarp = true;

    @Expose
    @ConfigOption(
        name = "Transfer Grace",
        desc = "Seconds of forgiveness after a warp, teleport or respawn before rules apply."
    )
    @ConfigEditorSlider(minValue = 0, maxValue = 5, minStep = 1)
    public int transferGraceSeconds = 3;

    @Expose
    @ConfigOption(
        name = "Health Tuning",
        desc = "Advanced drain, refill and effect thresholds for the health bar."
    )
    @Accordion
    public HealthTuning healthTuning = new HealthTuning();

    public static class HealthTuning {

        @Expose
        @ConfigOption(
            name = "Off-Tile Grace",
            desc = "Seconds off a tile before the bar starts draining."
        )
        @ConfigEditorSlider(minValue = 1, maxValue = 10, minStep = 1)
        public int offTileGraceSeconds = 3;

        @Expose
        @ConfigOption(
            name = "Full Drain Time",
            desc = "Seconds off-tile to empty a full bar."
        )
        @ConfigEditorSlider(minValue = 15, maxValue = 120, minStep = 5)
        public int fullDrainSeconds = 45;

        @Expose
        @ConfigOption(
            name = "Full Refill Time",
            desc = "Seconds on-tile to refill an empty bar."
        )
        @ConfigEditorSlider(minValue = 10, maxValue = 90, minStep = 5)
        public int fullRefillSeconds = 30;

        @Expose
        @ConfigOption(
            name = "Nausea Below",
            desc = "Health percent under which fake nausea applies."
        )
        @ConfigEditorSlider(minValue = 25, maxValue = 90, minStep = 1)
        public int nauseaBelowPercent = 66;

        @Expose
        @ConfigOption(
            name = "Blindness Below",
            desc = "Health percent under which fake blindness applies."
        )
        @ConfigEditorSlider(minValue = 10, maxValue = 50, minStep = 1)
        public int blindnessBelowPercent = 33;
    }
}
