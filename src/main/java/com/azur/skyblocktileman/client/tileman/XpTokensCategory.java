package com.azur.skyblocktileman.client.tileman;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;

public class XpTokensCategory {

    @Expose
    @ConfigOption(name = "XP per Token", desc = "Amount of Skill XP needed for one Block Unlock Token.")
    @ConfigEditorSlider(minValue = 100, maxValue = 10000, minStep = 100)
    public int xpPerToken = 1000;

    @Expose
    @ConfigOption(name = "Enable Scaling", desc = "If enabled, token cost increases as you earn more tokens.")
    @ConfigEditorBoolean
    public boolean enableScaling = false;

    @Expose
    @ConfigOption(name = "Scale Interval", desc = "For every this many tokens earned, the cost increases by the base XP amount.")
    @ConfigEditorSlider(minValue = 10, maxValue = 500, minStep = 10)
    public int scaleInterval = 100;
}
