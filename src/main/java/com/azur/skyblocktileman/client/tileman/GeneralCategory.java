package com.azur.skyblocktileman.client.tileman;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class GeneralCategory {

    @Expose
    @ConfigOption(name = "Tileman Enabled", desc = "Master toggle for all Tileman features.")
    @ConfigEditorBoolean
    public boolean enabled = true;

    @Expose
    @ConfigOption(name = "Debug Mode", desc = "Show debug messages in chat. Useful for troubleshooting.")
    @ConfigEditorBoolean
    public boolean debugMode = false;
}
