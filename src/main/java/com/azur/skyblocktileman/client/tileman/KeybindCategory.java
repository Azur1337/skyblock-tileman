package com.azur.skyblocktileman.client.tileman;

import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;
import org.lwjgl.glfw.GLFW;

// These ints mirror the KeyMappings, and options.txt is the real store, so transient keeps them out of moulconfig.json.
public class KeybindCategory {

    @ConfigOption(
        name = "Unlock Mode",
        desc = "Hold to preview and place tile unlocks."
    )
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_B)
    public transient int unlock = GLFW.GLFW_KEY_B;

    @ConfigOption(name = "Open Hub", desc = "Opens the Tileman hub menu.")
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_K)
    public transient int hub = GLFW.GLFW_KEY_K;

    @ConfigOption(
        name = "Move HUD",
        desc = "Starts HUD drag mode. Unbound by default to keep the key free."
    )
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_UNKNOWN)
    public transient int moveHud = GLFW.GLFW_KEY_UNKNOWN;
}
