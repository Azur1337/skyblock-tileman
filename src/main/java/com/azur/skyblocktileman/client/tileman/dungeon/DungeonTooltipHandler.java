package com.azur.skyblocktileman.client.tileman.dungeon;

import com.azur.skyblocktileman.client.tileman.TilemanConfig;
import com.azur.skyblocktileman.client.tileman.TilemanState;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Adds Tileman lock status to dungeon floor item tooltips.
 */
public final class DungeonTooltipHandler {
    
    private static final String CATACOMBS_GATE_TITLE = "Catacombs Gate";
    private static final String PARTY_FINDER_TITLE = "Party Finder";
    private static final String DUNGEON_FINDER_TITLE = "Dungeon Finder";
    private static final String SELECT_FLOOR_TITLE = "Select Floor";
    
    private DungeonTooltipHandler() {}
    
    public static void register() {
        ItemTooltipCallback.EVENT.register(DungeonTooltipHandler::onTooltip);
    }
    
    private static void onTooltip(ItemStack stack, net.minecraft.world.item.Item.TooltipContext context, 
                                   net.minecraft.world.item.TooltipFlag flag, List<Component> lines) {
        if (!TilemanConfig.getInstance().isEnabled()) {
            return;
        }
        
        Minecraft client = Minecraft.getInstance();
        Screen currentScreen = client.gui.screen();
        
        if (!(currentScreen instanceof AbstractContainerScreen<?> containerScreen)) {
            return;
        }
        
        String screenTitle = containerScreen.getTitle().getString();
        
        // Only modify tooltips in dungeon-related menus
        if (!screenTitle.contains(CATACOMBS_GATE_TITLE) && 
            !screenTitle.contains(PARTY_FINDER_TITLE) &&
            !screenTitle.contains(DUNGEON_FINDER_TITLE) &&
            !screenTitle.contains(SELECT_FLOOR_TITLE)) {
            return;
        }
        
        String itemName = stack.getHoverName().getString();
        itemName = ChatFormatting.stripFormatting(itemName);
        if (itemName == null) return;
        
        DungeonFloor floor = DungeonScreenHandler.getFloorFromItemName(itemName);
        if (floor == null) {
            return;
        }
        
        DungeonData data = TilemanState.getInstance().getDungeonData();
        boolean isLocked = !data.isUnlocked(floor);
        
        lines.add(Component.empty());
        if (isLocked) {
            lines.add(Component.literal("§c§lTILEMAN: §cLocked"));
            lines.add(Component.literal("§7Unlock cost: §e" + floor.getUnlockCost() + " tokens"));
        } else {
            lines.add(Component.literal("§a§lTILEMAN: §aUnlocked"));
        }
    }
}
