package com.azur.skyblocktileman.client.tileman;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class XpTokensCategory {

    @Expose
    @ConfigOption(name = "Base Token Cost", desc = "Base amount of Skill XP needed for one Block Unlock Token.")
    @ConfigEditorSlider(minValue = 100, maxValue = 5000, minStep = 100)
    public int baseTokenCost = 1000;

    @Expose
    @ConfigOption(name = "Cost Scale Interval", desc = "For every this many total XP earned, the token cost increases by the base amount.")
    @ConfigEditorSlider(minValue = 100000, maxValue = 5000000, minStep = 100000)
    public int costScaleInterval = 1000000;
}
