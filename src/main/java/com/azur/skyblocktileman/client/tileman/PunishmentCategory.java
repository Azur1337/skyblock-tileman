package com.azur.skyblocktileman.client.tileman;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class PunishmentCategory {

    @Expose
    @ConfigOption(name = "Punishment Enabled", desc = "Enable the punishment system when standing on unlocked blocks.")
    @ConfigEditorBoolean
    public boolean enabled = true;

    @Expose
    @ConfigOption(name = "Sound Interval (ticks)", desc = "How often the thunder sound plays while violating. 20 ticks = 1 second.")
    @ConfigEditorSlider(minValue = 5, maxValue = 200, minStep = 5)
    public int soundIntervalTicks = 100;
}
