package com.azur.skyblocktileman.mixin;

import com.azur.skyblocktileman.client.tileman.TilemanConfig;
import com.azur.skyblocktileman.client.tileman.dungeon.DungeonScreenHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Mixin to add Tileman lock status to dungeon floor item tooltips.
 */
@Mixin(ItemStack.class)
public class DungeonTooltipMixin {

    @Inject(method = "getTooltipLines", at = @At("RETURN"))
    private void tileman$modifyTooltip(Item.TooltipContext context, TooltipFlag flag, CallbackInfoReturnable<List<Component>> cir) {
        if (!TilemanConfig.getInstance().isEnabled()) {
            return;
        }
        
        Minecraft client = Minecraft.getInstance();
        Screen currentScreen = client.gui.screen();
        
        // Only modify tooltips when in a container screen
        if (!(currentScreen instanceof AbstractContainerScreen<?> containerScreen)) {
            return;
        }
        
        String screenTitle = containerScreen.getTitle().getString();
        ItemStack stack = (ItemStack) (Object) this;
        List<Component> tooltip = cir.getReturnValue();
        
        DungeonScreenHandler.modifyTooltip(stack, tooltip, screenTitle);
    }
}
