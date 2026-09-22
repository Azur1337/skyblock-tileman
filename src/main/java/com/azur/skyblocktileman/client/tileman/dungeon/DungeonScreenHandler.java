package com.azur.skyblocktileman.client.tileman.dungeon;

import com.azur.skyblocktileman.client.tileman.DebugCategory;
import com.azur.skyblocktileman.client.tileman.TilemanLog;
import com.azur.skyblocktileman.client.tileman.TilemanState;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class DungeonScreenHandler {

    private DungeonScreenHandler() {}

    private static final String CATACOMBS_GATE_TITLE = "Catacombs Gate";

    private static final String PARTY_FINDER_TITLE = "Party Finder";
    private static final String DUNGEON_FINDER_TITLE = "Dungeon Finder";
    private static final String SELECT_FLOOR_TITLE = "Select Floor";

    // Catacombs Gate menu: "The Catacombs - Floor II"
    private static final Pattern NORMAL_FLOOR_PATTERN = Pattern.compile(
        "The Catacombs - Floor ([IVX]+)",
        Pattern.CASE_INSENSITIVE
    );

    // Catacombs Gate menu, master mode: "MM The Catacombs - Floor I"
    private static final Pattern MASTER_FLOOR_PATTERN = Pattern.compile(
        "MM The Catacombs - Floor ([IVX]+)",
        Pattern.CASE_INSENSITIVE
    );

    // The entrance is always accessible, no unlock needed
    private static final Pattern ENTRANCE_PATTERN = Pattern.compile(
        "The Catacombs - Entrance",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern PF_NORMAL_FLOOR_PATTERN = Pattern.compile(
        "^Floor ([IVX]+)$",
        Pattern.CASE_INSENSITIVE
    );
    private static final Pattern PF_MASTER_FLOOR_PATTERN = Pattern.compile(
        "^Master Mode Floor ([IVX]+)$",
        Pattern.CASE_INSENSITIVE
    );
    private static final Pattern PF_SHORT_FLOOR_PATTERN = Pattern.compile(
        "^([FM])(\\d)$",
        Pattern.CASE_INSENSITIVE
    );

    public static boolean handleClick(
        AbstractContainerScreen<?> screen,
        Slot slot
    ) {
        if (slot == null) return false;

        String title = screen.getTitle().getString();

        TilemanLog.debug(
            DebugCategory.ALL,
            "Container click - Title: '{}'",
            title
        );

        if (!title.contains(CATACOMBS_GATE_TITLE)) {
            return false;
        }

        TilemanLog.debug(
            DebugCategory.ALL,
            "Catacombs Gate menu click detected"
        );

        ItemStack itemStack = slot.getItem();
        if (itemStack.isEmpty()) {
            TilemanLog.debug(DebugCategory.ALL, "Empty slot clicked");
            return false;
        }

        String itemName = itemStack.getHoverName().getString();
        TilemanLog.debug(
            DebugCategory.ALL,
            "Clicked item name: '{}'",
            itemName
        );

        if (ENTRANCE_PATTERN.matcher(itemName).find()) {
            TilemanLog.debug(DebugCategory.ALL, "Entrance clicked, allowing");
            return false;
        }

        Matcher masterMatcher = MASTER_FLOOR_PATTERN.matcher(itemName);
        if (masterMatcher.find()) {
            String romanNumeral = masterMatcher.group(1);
            int floorNum = romanToInt(romanNumeral);
            DungeonFloor floor = DungeonFloor.fromNumber(floorNum, true);

            if (floor != null) {
                return handleFloorClick(floor);
            }
        }

        Matcher normalMatcher = NORMAL_FLOOR_PATTERN.matcher(itemName);
        if (normalMatcher.find()) {
            String romanNumeral = normalMatcher.group(1);
            int floorNum = romanToInt(romanNumeral);
            DungeonFloor floor = DungeonFloor.fromNumber(floorNum, false);

            if (floor != null) {
                return handleFloorClick(floor);
            }
        }

        TilemanLog.debug(
            DebugCategory.ALL,
            "No floor pattern matched for: {}",
            itemName
        );
        return false;
    }

    public static boolean handlePartyFinderClick(
        AbstractContainerScreen<?> screen,
        Slot slot
    ) {
        if (slot == null) return false;

        String title = screen.getTitle().getString();

        if (
            !title.contains(PARTY_FINDER_TITLE) &&
            !title.contains(DUNGEON_FINDER_TITLE) &&
            !title.contains(SELECT_FLOOR_TITLE)
        ) {
            return false;
        }

        ItemStack itemStack = slot.getItem();
        if (itemStack.isEmpty()) {
            return false;
        }

        String itemName = itemStack.getHoverName().getString();
        itemName = ChatFormatting.stripFormatting(itemName);
        if (itemName == null) return false;

        TilemanLog.debug(
            DebugCategory.ALL,
            "Party finder click - Title: '{}', Item: '{}'",
            title,
            itemName
        );

        DungeonFloor floor = parseFloorFromItemName(itemName);
        if (floor == null) {
            return false;
        }

        return handleFloorClick(floor);
    }

    private static DungeonFloor parseFloorFromItemName(String itemName) {
        if (ENTRANCE_PATTERN.matcher(itemName).find()) {
            return null;
        }

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

        Matcher shortMatcher = PF_SHORT_FLOOR_PATTERN.matcher(itemName);
        if (shortMatcher.find()) {
            boolean isMaster = shortMatcher.group(1).equalsIgnoreCase("M");
            int floorNum = Integer.parseInt(shortMatcher.group(2));
            return DungeonFloor.fromNumber(floorNum, isMaster);
        }

        return null;
    }

    public static Boolean getFloorLockStatus(String itemName) {
        if (itemName == null) return null;

        itemName = ChatFormatting.stripFormatting(itemName);
        if (itemName == null) return null;

        DungeonFloor floor = parseFloorFromItemName(itemName);
        if (floor == null) {
            return null;
        }

        DungeonData data = TilemanState.getInstance().getDungeonData();
        return !data.isUnlocked(floor);
    }

    public static DungeonFloor getFloorFromItemName(String itemName) {
        if (itemName == null) return null;
        itemName = ChatFormatting.stripFormatting(itemName);
        if (itemName == null) return null;
        return parseFloorFromItemName(itemName);
    }

    public static void modifyTooltip(
        ItemStack stack,
        List<Component> tooltip,
        String screenTitle
    ) {
        if (screenTitle == null) return;

        if (
            !screenTitle.contains(CATACOMBS_GATE_TITLE) &&
            !screenTitle.contains(PARTY_FINDER_TITLE) &&
            !screenTitle.contains(DUNGEON_FINDER_TITLE) &&
            !screenTitle.contains(SELECT_FLOOR_TITLE)
        ) {
            return;
        }

        String itemName = stack.getHoverName().getString();
        DungeonFloor floor = getFloorFromItemName(itemName);

        if (floor == null) {
            return;
        }

        DungeonData data = TilemanState.getInstance().getDungeonData();
        boolean isLocked = !data.isUnlocked(floor);

        tooltip.add(Component.empty());
        if (isLocked) {
            tooltip.add(Component.literal("§c§lTILEMAN: §cLocked"));
            tooltip.add(
                Component.literal(
                    "§7Unlock cost: §e" + floor.getUnlockCost() + " tokens"
                )
            );
        } else {
            tooltip.add(Component.literal("§a§lTILEMAN: §aUnlocked"));
        }
    }

    private static boolean handleFloorClick(DungeonFloor floor) {
        TilemanLog.debug(
            DebugCategory.ALL,
            "Detected floor: {}",
            floor.getId()
        );

        DungeonData data = TilemanState.getInstance().getDungeonData();

        if (data.isUnlocked(floor)) {
            TilemanLog.debug(
                DebugCategory.ALL,
                "Floor {} is unlocked, allowing click",
                floor.getId()
            );
            return false;
        }

        TilemanLog.debug(
            DebugCategory.ALL,
            "Floor {} is locked, opening unlock screen",
            floor.getId()
        );

        final DungeonFloor floorToUnlock = floor;
        Minecraft client = Minecraft.getInstance();
        client.schedule(() -> {
            DungeonUnlockScreen.open(floorToUnlock);
        });

        return true;
    }

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
