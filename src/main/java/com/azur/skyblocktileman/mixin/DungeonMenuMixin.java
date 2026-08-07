package com.azur.skyblocktileman.mixin;

import com.azur.skyblocktileman.client.tileman.TilemanConfig;
import com.azur.skyblocktileman.client.tileman.dungeon.DungeonScreenHandler;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin to intercept clicks on Hypixel's dungeon menus.
 * Blocks interaction with locked floors.
 */
@Mixin(AbstractContainerScreen.class)
public abstract class DungeonMenuMixin {

    @Shadow
    protected Slot hoveredSlot;

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void tileman$onMouseClicked(MouseButtonEvent event, boolean flag, CallbackInfoReturnable<Boolean> cir) {
        if (!TilemanConfig.getInstance().isEnabled()) {
            return;
        }
        
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
        
        // Check if clicking on a slot
        if (hoveredSlot != null) {
            // Check if this is a dungeon-related menu and handle it
            if (DungeonScreenHandler.handleClick(screen, hoveredSlot)) {
                cir.setReturnValue(true);
                return;
            }
            
            // Also check party finder menus
            if (DungeonScreenHandler.handlePartyFinderClick(screen, hoveredSlot)) {
                cir.setReturnValue(true);
            }
        }
    }
}
