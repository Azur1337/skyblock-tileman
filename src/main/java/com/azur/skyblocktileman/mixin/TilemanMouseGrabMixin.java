package com.azur.skyblocktileman.mixin;

import com.azur.skyblocktileman.client.tileman.TilemanHudOverlay;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// During HUD move mode a click must not re-grab the cursor (vanilla treats the first
// click on an ungrabbed window as "return to game"), which hides the cursor mid-drag.
@Mixin(MouseHandler.class)
public abstract class TilemanMouseGrabMixin {

    @Redirect(
        method = "onButton",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/MouseHandler;grabMouse()V"
        )
    )
    private void tileman$skipGrab(MouseHandler handler) {
        if (!TilemanHudOverlay.isMoveActive()) {
            handler.grabMouse();
        }
    }
}
