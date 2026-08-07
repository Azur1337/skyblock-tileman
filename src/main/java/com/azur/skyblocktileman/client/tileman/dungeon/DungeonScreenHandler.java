package com.azur.skyblocktileman.client.tileman.dungeon;

import com.azur.skyblocktileman.client.tileman.DebugCategory;
import com.azur.skyblocktileman.client.tileman.TilemanLog;
import com.azur.skyblocktileman.client.tileman.TilemanState;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles detection and blocking of dungeon menu interactions.
 * Called from the DungeonMenuMixin.
 */
public final class DungeonScreenHandler {
    
    private DungeonScreenHandler() {}
    
    // The dungeon floor selection menu title
    private static final String CATACOMBS_GATE_TITLE = "Catacombs Gate";
    
    // Party finder menu titles
    private static final String PARTY_FINDER_TITLE = "Party Finder";
    private static final String DUNGEON_FINDER_TITLE = "Dungeon Finder"; // floor selection in party finder
    private static final String SELECT_FLOOR_TITLE = "Select Floor"; // another variant
    
    // Pattern for normal floors: "The Catacombs - Floor I", "The Catacombs - Floor II", etc.
    // Also matches entrance: "The Catacombs - Entrance"
    private static final Pattern NORMAL_FLOOR_PATTERN = 
        Pattern.compile("The Catacombs - Floor ([IVX]+)", Pattern.CASE_INSENSITIVE);
    
    // Pattern for master mode floors: "MM The Catacombs - Floor I", etc.
    private static final Pattern MASTER_FLOOR_PATTERN = 
        Pattern.compile("MM The Catacombs - Floor ([IVX]+)", Pattern.CASE_INSENSITIVE);
    
    // Entrance pattern (always accessible, no unlock needed)
    private static final Pattern ENTRANCE_PATTERN = 
        Pattern.compile("The Catacombs - Entrance", Pattern.CASE_INSENSITIVE);
    
    // Party finder floor patterns (e.g., "Floor I", "Floor VII", "Master Mode Floor I")
    private static final Pattern PF_NORMAL_FLOOR_PATTERN = 
        Pattern.compile("^Floor ([IVX]+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern PF_MASTER_FLOOR_PATTERN = 
        Pattern.compile("^Master Mode Floor ([IVX]+)$", Pattern.CASE_INSENSITIVE);
    // Short forms: "F1", "F7", "M1", "M7"
    private static final Pattern PF_SHORT_FLOOR_PATTERN = 
        Pattern.compile("^([FM])(\\d)$", Pattern.CASE_INSENSITIVE);
    
    /**
     * Handle a click on a dungeon menu slot.
     * Returns true if we handled it (and should cancel the normal click).
     */
    public static boolean handleClick(AbstractContainerScreen<?> screen, Slot slot) {
        if (slot == null) return false;
        
        String title = screen.getTitle().getString();
        
        // Log all container interactions for debugging
        TilemanLog.debug(DebugCategory.ALL, "Container click - Title: '{}'", title);
        
        // Only handle clicks in the Catacombs Gate menu
        if (!title.contains(CATACOMBS_GATE_TITLE)) {
            return false;
        }
        
        TilemanLog.debug(DebugCategory.ALL, "Catacombs Gate menu click detected");
        
        ItemStack itemStack = slot.getItem();
        if (itemStack.isEmpty()) {
            TilemanLog.debug(DebugCategory.ALL, "Empty slot clicked");
            return false;
        }
        
        String itemName = itemStack.getHoverName().getString();
        TilemanLog.debug(DebugCategory.ALL, "Clicked item name: '{}'", itemName);
        
        // Check if this is the entrance (always allowed)
        if (ENTRANCE_PATTERN.matcher(itemName).find()) {
            TilemanLog.debug(DebugCategory.ALL, "Entrance clicked, allowing");
            return false;
        }
        
        // Check for master mode floor
        Matcher masterMatcher = MASTER_FLOOR_PATTERN.matcher(itemName);
        if (masterMatcher.find()) {
            String romanNumeral = masterMatcher.group(1);
            int floorNum = romanToInt(romanNumeral);
            DungeonFloor floor = DungeonFloor.fromNumber(floorNum, true);
            
            if (floor != null) {
                return handleFloorClick(floor);
            }
        }
        
        // Check for normal floor
        Matcher normalMatcher = NORMAL_FLOOR_PATTERN.matcher(itemName);
        if (normalMatcher.find()) {
            String romanNumeral = normalMatcher.group(1);
            int floorNum = romanToInt(romanNumeral);
            DungeonFloor floor = DungeonFloor.fromNumber(floorNum, false);
            
            if (floor != null) {
                return handleFloorClick(floor);
            }
        }
        
        TilemanLog.debug(DebugCategory.ALL, "No floor pattern matched for: {}", itemName);
        return false;
    }
    
    /**
     * Handle clicks in party finder menus.
     * Returns true if we handled it (and should cancel the normal click).
     */
    public static boolean handlePartyFinderClick(AbstractContainerScreen<?> screen, Slot slot) {
        if (slot == null) return false;
        
        String title = screen.getTitle().getString();
        
        // Only handle party finder related menus
        if (!title.contains(PARTY_FINDER_TITLE) && 
            !title.contains(DUNGEON_FINDER_TITLE) && 
            !title.contains(SELECT_FLOOR_TITLE)) {
            return false;
        }
        
        ItemStack itemStack = slot.getItem();
        if (itemStack.isEmpty()) {
            return false;
        }
        
        String itemName = itemStack.getHoverName().getString();
        // Strip color codes
        itemName = ChatFormatting.stripFormatting(itemName);
        if (itemName == null) return false;
        
        TilemanLog.debug(DebugCategory.ALL, "Party finder click - Title: '{}', Item: '{}'", title, itemName);
        
        DungeonFloor floor = parseFloorFromItemName(itemName);
        if (floor == null) {
            return false;
        }
        
        return handleFloorClick(floor);
    }
    
    /**
     * Try to parse a DungeonFloor from various item name formats.
     */
    private static DungeonFloor parseFloorFromItemName(String itemName) {
        // Check entrance (always allowed, return null to not block)
        if (ENTRANCE_PATTERN.matcher(itemName).find()) {
            return null;
        }
        
        // Check full floor names (Catacombs Gate format)
        Matcher masterMatcher = MASTER_FLOOR_PATTERN.matcher(itemName);
        if (masterMatcher.find()) {
            int floorNum = romanToInt(masterMatcher.group(1));
            return DungeonFloor.fromNumber(floorNum, true);
        }
        
        Matcher normalMatcher = NORMAL_FLOOR_PATTERN.matcher(itemName);
        if (normalMatcher.find()) {
            int floorNum = romanToInt(normalMatcher.group(1));
            return DungeonFloor.fromNumber(floorNum, false);
        }
        
        // Check party finder formats
        Matcher pfMasterMatcher = PF_MASTER_FLOOR_PATTERN.matcher(itemName);
        if (pfMasterMatcher.find()) {
            int floorNum = romanToInt(pfMasterMatcher.group(1));
            return DungeonFloor.fromNumber(floorNum, true);
        }
        
        Matcher pfNormalMatcher = PF_NORMAL_FLOOR_PATTERN.matcher(itemName);
        if (pfNormalMatcher.find()) {
            int floorNum = romanToInt(pfNormalMatcher.group(1));
            return DungeonFloor.fromNumber(floorNum, false);
        }
        
        // Check short forms (F1, M7, etc.)
        Matcher shortMatcher = PF_SHORT_FLOOR_PATTERN.matcher(itemName);
        if (shortMatcher.find()) {
            boolean isMaster = shortMatcher.group(1).equalsIgnoreCase("M");
            int floorNum = Integer.parseInt(shortMatcher.group(2));
            return DungeonFloor.fromNumber(floorNum, isMaster);
        }
        
        return null;
    }
    
    /**
     * Check if an item represents a dungeon floor and return its lock status.
     * Used for tooltip modification.
     * 
     * @return null if not a floor item, Boolean.TRUE if locked, Boolean.FALSE if unlocked
     */
    public static Boolean getFloorLockStatus(String itemName) {
        if (itemName == null) return null;
        
        // Strip color codes
        itemName = ChatFormatting.stripFormatting(itemName);
        if (itemName == null) return null;
        
        DungeonFloor floor = parseFloorFromItemName(itemName);
        if (floor == null) {
            return null; // Not a floor item
        }
        
        DungeonData data = TilemanState.getInstance().getDungeonData();
        return !data.isUnlocked(floor); // true if locked
    }
    
    /**
     * Get the floor from an item name (for tooltip display).
     */
    public static DungeonFloor getFloorFromItemName(String itemName) {
        if (itemName == null) return null;
        itemName = ChatFormatting.stripFormatting(itemName);
        if (itemName == null) return null;
        return parseFloorFromItemName(itemName);
    }
    
    /**
     * Add tileman lock status to tooltip if applicable.
     */
    public static void modifyTooltip(ItemStack stack, List<Component> tooltip, String screenTitle) {
        if (screenTitle == null) return;
        
        // Only modify tooltips in dungeon-related menus
        if (!screenTitle.contains(CATACOMBS_GATE_TITLE) && 
            !screenTitle.contains(PARTY_FINDER_TITLE) &&
            !screenTitle.contains(DUNGEON_FINDER_TITLE) &&
            !screenTitle.contains(SELECT_FLOOR_TITLE)) {
            return;
        }
        
        String itemName = stack.getHoverName().getString();
        DungeonFloor floor = getFloorFromItemName(itemName);
        
        if (floor == null) {
            return; // Not a floor item
        }
        
        DungeonData data = TilemanState.getInstance().getDungeonData();
        boolean isLocked = !data.isUnlocked(floor);
        
        // Add a blank line and tileman status
        tooltip.add(Component.empty());
        if (isLocked) {
            tooltip.add(Component.literal("§c§lTILEMAN: §cLocked"));
            tooltip.add(Component.literal("§7Unlock cost: §e" + floor.getUnlockCost() + " tokens"));
        } else {
            tooltip.add(Component.literal("§a§lTILEMAN: §aUnlocked"));
        }
    }
    
    /**
     * Handle click on a specific floor.
     * Returns true if click should be blocked (floor locked).
     */
    private static boolean handleFloorClick(DungeonFloor floor) {
        TilemanLog.debug(DebugCategory.ALL, "Detected floor: {}", floor.getId());
        
        DungeonData data = TilemanState.getInstance().getDungeonData();
        
        if (data.isUnlocked(floor)) {
            TilemanLog.debug(DebugCategory.ALL, "Floor {} is unlocked, allowing click", floor.getId());
            return false;
        }
        
        // Floor is locked - open unlock confirmation screen
        TilemanLog.debug(DebugCategory.ALL, "Floor {} is locked, opening unlock screen", floor.getId());
        
        final DungeonFloor floorToUnlock = floor;
        Minecraft client = Minecraft.getInstance();
        client.schedule(() -> {
            DungeonUnlockScreen.open(floorToUnlock);
        });
        
        return true; // Cancel the original click
    }
    
    /**
     * Convert Roman numeral to integer.
     */
    private static int romanToInt(String roman) {
        if (roman == null || roman.isEmpty()) return 0;
        roman = roman.toUpperCase();
        
        return switch (roman) {
            case "I" -> 1;
            case "II" -> 2;
            case "III" -> 3;
            case "IV" -> 4;
            case "V" -> 5;
            case "VI" -> 6;
            case "VII" -> 7;
            default -> 0;
        };
    }
}
