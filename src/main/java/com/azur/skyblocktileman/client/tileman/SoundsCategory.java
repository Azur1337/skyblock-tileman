package com.azur.skyblocktileman.client.tileman;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class SoundsCategory {

    @Expose
    @ConfigOption(
        name = "Tileman Sounds",
        desc = "Master toggle for Tileman's positive and neutral feedback sounds."
    )
    @ConfigEditorBoolean
    public boolean enabled = true;

    @Expose
    @ConfigOption(
        name = "Purchase Chime",
        desc = "Experience orb sound when a shop purchase or opt-in succeeds."
    )
    @ConfigEditorBoolean
    public boolean purchaseChime = true;

    @Expose
    @ConfigOption(
        name = "Milestone Toast",
        desc = "Challenge-complete sound for milestones and floor unlocks."
    )
    @ConfigEditorBoolean
    public boolean milestoneToast = true;

    @Expose
    @ConfigOption(
        name = "Dungeon Reward",
        desc = "Level-up sound when dungeon completions pay out tokens."
    )
    @ConfigEditorBoolean
    public boolean dungeonReward = true;

    @Expose
    @ConfigOption(
        name = "Frenzy Expiry",
        desc = "Sound when an XP Frenzy runs out."
    )
    @ConfigEditorBoolean
    public boolean frenzyExpiry = true;

    @Expose
    @ConfigOption(
        name = "Collapse Snap",
        desc = "Sound when the health bar collapses."
    )
    @ConfigEditorBoolean
    public boolean collapseSnap = true;
}
