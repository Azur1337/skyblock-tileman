package com.azur.skyblocktileman.client.tileman;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.Accordion;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class GeneralCategory {

    @Expose
    @ConfigOption(name = "Tileman Enabled", desc = "Master toggle for all Tileman features.")
    @ConfigEditorBoolean
    public boolean enabled = true;

    @Expose
    @ConfigOption(name = "Debug Mode", desc = "Show debug messages in chat.")
    @ConfigEditorBoolean
    public boolean debugMode = false;

    @Expose
    @ConfigOption(name = "Debug Categories", desc = "Choose which debug messages to show.")
    @Accordion
    public DebugCategories debugCategories = new DebugCategories();

    public static class DebugCategories {
        @Expose
        @ConfigOption(name = "Action Bar", desc = "Log raw action bar text and XP parsing.")
        @ConfigEditorBoolean
        public boolean actionBar = false;

        @Expose
        @ConfigOption(name = "Island Detection", desc = "Log /locraw responses and island changes.")
        @ConfigEditorBoolean
        public boolean island = true;

        @Expose
        @ConfigOption(name = "Profile", desc = "Log profile switches and API responses.")
        @ConfigEditorBoolean
        public boolean profile = true;

        @Expose
        @ConfigOption(name = "Tokens", desc = "Log XP updates and token earning.")
        @ConfigEditorBoolean
        public boolean tokens = true;

        @Expose
        @ConfigOption(name = "Blocks", desc = "Log block unlocking and first block mode.")
        @ConfigEditorBoolean
        public boolean blocks = true;

        @Expose
        @ConfigOption(name = "Rendering", desc = "Log overlay and HUD rendering.")
        @ConfigEditorBoolean
        public boolean rendering = true;

        @Expose
        @ConfigOption(name = "Slayer", desc = "Log slayer quest detection and milestone tracking.")
        @ConfigEditorBoolean
        public boolean slayer = true;
    }
}
