package com.azur.skyblocktileman.client.tileman;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class PunishmentCategory {

    @Expose
    @ConfigOption(
        name = "Punishment Enabled",
        desc = "Enable punishment when standing on a locked tile."
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
    @ConfigOption(name = "Thunder Interval", desc = "Ticks between thunder.")
    @ConfigEditorSlider(minValue = 5, maxValue = 200, minStep = 5)
    public int soundIntervalTicks = 100;
}
