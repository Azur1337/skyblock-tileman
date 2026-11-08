package com.azur.skyblocktileman.mixin;

import com.azur.skyblocktileman.client.tileman.TilemanState;
import com.azur.skyblocktileman.client.tileman.dungeon.DungeonScreenHandler;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Intercepts clicks on Hypixel dungeon menus to block locked floors. */
@Mixin(AbstractContainerScreen.class)
public abstract class DungeonMenuMixin {

    @Shadow
    protected Slot hoveredSlot;

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void tileman$onMouseClicked(
        MouseButtonEvent event,
        boolean isDoubleClick,
        CallbackInfoReturnable<Boolean> cir
    ) {
        if (!TilemanState.getInstance().isTilemanActive()) {
            return;
        }

        AbstractContainerScreen<?> screen =
            (AbstractContainerScreen<?>) (Object) this;

        if (hoveredSlot != null) {
            if (DungeonScreenHandler.handleClick(screen, hoveredSlot)) {
                cir.setReturnValue(true);
                return;
            }

            if (
                DungeonScreenHandler.handlePartyFinderClick(screen, hoveredSlot)
            ) {
                cir.setReturnValue(true);
            }
        }
    }
}
