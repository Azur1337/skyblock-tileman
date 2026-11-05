package com.azur.skyblocktileman.client.tileman;

import com.google.gson.annotations.Expose;
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
}
