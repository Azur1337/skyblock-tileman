package com.azur.skyblocktileman.client.tileman;

import com.azur.skyblocktileman.client.tileman.ui.TilemanHubScreen;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import org.lwjgl.glfw.GLFW;

// Keeps the MoulConfig keybind mirrors and the vanilla KeyMappings in sync; the KeyMapping and options.txt are the truth.
public final class KeybindSync {

    public static final KeyMapping HUB_KEY =
        KeyMappingHelper.registerKeyMapping(
            new KeyMapping(
                "key.skyblocktileman.hub",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                TilemanSelectionMode.CATEGORY
            )
        );

    // Unbound by default: Tileman claims as few keys as it can, since other mods bind keys too.
    public static final KeyMapping MOVE_HUD_KEY =
        KeyMappingHelper.registerKeyMapping(
            new KeyMapping(
                "key.skyblocktileman.move_hud",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                TilemanSelectionMode.CATEGORY
            )
        );

    private static final List<Binding> BINDINGS = new ArrayList<>();

    private KeybindSync() {}

    public static void register() {
        KeybindCategory keybinds = TilemanConfig.getInstance().getKeybinds();

        add(
            TilemanSelectionMode.UNLOCK_MODE_KEY,
            () -> keybinds.unlock,
            v -> keybinds.unlock = v
        );
        add(HUB_KEY, () -> keybinds.hub, v -> keybinds.hub = v);
        add(MOVE_HUD_KEY, () -> keybinds.moveHud, v -> keybinds.moveHud = v);

        // Seed the mirrors from the KeyMappings before the tick handler runs, so those defaults are the only ones.
        for (Binding b : BINDINGS) {
            b.lastCode = currentCode(b.key);
            b.setConfig.accept(b.lastCode);
        }

        ClientTickEvents.END_CLIENT_TICK.register(KeybindSync::onEndTick);
    }

    private static void add(
        KeyMapping key,
        IntSupplier getConfig,
        IntConsumer setConfig
    ) {
        BINDINGS.add(new Binding(key, getConfig, setConfig));
    }

    private static void onEndTick(Minecraft client) {
        boolean rebound = false;
        for (Binding b : BINDINGS) {
            rebound |= b.reconcile();
        }

        if (rebound) {
            // setKey only assigns the field; dispatch runs off a static key -> KeyMapping table it does not touch, so resetMapping is required.
            KeyMapping.resetMapping();
            client.options.save();
        }

        boolean enabled = TilemanConfig.getInstance().isEnabled();

        // consumeClick runs even when disabled so a stale click cannot fire once the master toggle comes back on.
        if (HUB_KEY.consumeClick() && enabled) {
            TilemanHubScreen.open();
        }

        // Move mode defers its mouse release to the render loop, which assumes no screen is opening, so only start it from the live game.
        if (
            MOVE_HUD_KEY.consumeClick() &&
            enabled &&
            client.gui.screen() == null
        ) {
            TilemanHudOverlay.beginMoveMode();
        }
    }

    private static int currentCode(KeyMapping key) {
        InputConstants.Key bound = KeyMappingHelper.getBoundKeyOf(key);
        return bound == null
            ? InputConstants.UNKNOWN.getValue()
            : bound.getValue();
    }

    // The widget's int space: 0-9 are mouse buttons, anything else a keyboard key.
    private static InputConstants.Key toKey(int code) {
        return code >= 0 && code <= 9
            ? InputConstants.Type.MOUSE.getOrCreate(code)
            : InputConstants.Type.KEYSYM.getOrCreate(code);
    }

    // options.keyMappings is vanilla plus every Fabric-registered binding, so this catches other mods; mods reaching into KeyMapping.ALL are invisible.
    private static KeyMapping findConflict(KeyMapping self, int code) {
        // Unbinding is never a conflict: lots of bindings sit unbound at once.
        if (code == InputConstants.UNKNOWN.getValue()) {
            return null;
        }
        Minecraft client = Minecraft.getInstance();
        if (client.options == null) {
            return null;
        }
        for (KeyMapping other : client.options.keyMappings) {
            if (other != null && other != self && currentCode(other) == code) {
                return other;
            }
        }
        return null;
    }

    private static String actionName(KeyMapping key) {
        return I18n.get(key.getName());
    }

    private static String keyLabel(int code) {
        return toKey(code).getDisplayName().getString();
    }

    private static final class Binding {

        private final KeyMapping key;
        private final IntSupplier getConfig;
        private final IntConsumer setConfig;
        private int lastCode;

        private Binding(
            KeyMapping key,
            IntSupplier getConfig,
            IntConsumer setConfig
        ) {
            this.key = key;
            this.getConfig = getConfig;
            this.setConfig = setConfig;
        }

        // True when this reconcile pushed a new binding, meaning the global dispatch table is now stale.
        private boolean reconcile() {
            int vanilla = currentCode(key);
            if (vanilla != lastCode) {
                // 26.2 dropped the Controls screen's conflict warning, so warn on a shared key but keep the player's deliberate choice.
                lastCode = vanilla;
                setConfig.accept(vanilla);
                warnIfShared(vanilla);
                return false;
            }

            int wanted = getConfig.getAsInt();
            if (wanted == lastCode) {
                return false;
            }

            KeyMapping clash = findConflict(key, wanted);
            if (clash == null) {
                lastCode = wanted;
                key.setKey(toKey(wanted));
                return true;
            }

            // Denied: snap the mirror back so the row shows the binding it kept, then say who owns the key.
            setConfig.accept(lastCode);
            TilemanChat.warn(
                actionName(key) +
                    ": " +
                    keyLabel(wanted) +
                    " is already used by " +
                    actionName(clash) +
                    "."
            );
            return false;
        }

        private void warnIfShared(int code) {
            KeyMapping clash = findConflict(key, code);
            if (clash != null) {
                TilemanChat.warn(
                    actionName(clash) +
                        " and " +
                        actionName(key) +
                        " are both on " +
                        keyLabel(code) +
                        "."
                );
            }
        }
    }
}
