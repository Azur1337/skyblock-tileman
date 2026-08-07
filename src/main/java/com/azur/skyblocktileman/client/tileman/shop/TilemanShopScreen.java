package com.azur.skyblocktileman.client.tileman.shop;

import com.azur.skyblocktileman.client.tileman.ShopData;
import com.azur.skyblocktileman.client.tileman.TilemanChat;
import com.azur.skyblocktileman.client.tileman.TilemanState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import java.util.ArrayList;
import java.util.List;

public class TilemanShopScreen extends ContainerScreen {

    private static final int ROWS = 6;
    private static final int COLS = 9;
    private static final int SIZE = ROWS * COLS;

    private ShopCategory currentCategory = null;
    private final SimpleContainer container;

    public TilemanShopScreen(ChestMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.container = (SimpleContainer) menu.getContainer();
    }

    public static void open() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        
        client.schedule(() -> {
            SimpleContainer container = new SimpleContainer(SIZE);
            ChestMenu menu = new ChestMenu(MenuType.GENERIC_9x6, 0, client.player.getInventory(), container, ROWS);
            TilemanShopScreen screen = new TilemanShopScreen(menu, client.player.getInventory(), Component.literal("Tileman Shop"));
            screen.loadMainMenu();
            client.gui.setScreen(screen);
        });
    }

    @Override
    protected void init() {
        super.init();
        if (currentCategory == null) {
            loadMainMenu();
        }
    }

    private void loadMainMenu() {
        currentCategory = null;
        clearContainer();
        fillBorder();
        
        // Center the 2 categories symmetrically (slots 21 and 23)
        setItem(21, createCategoryItem(ShopCategory.PERMANENT));
        setItem(23, createCategoryItem(ShopCategory.CONSUMABLES));
    }

    private void loadCategory(ShopCategory category) {
        currentCategory = category;
        clearContainer();
        fillBorder();
        
        setItem(45, createBackItem());
        
        switch (category) {
            case PERMANENT -> loadPermanentItems();
            case CONSUMABLES -> loadConsumableItems();
        }
    }

    private void loadPermanentItems() {
        ShopData shop = TilemanState.getInstance().getShop();
        int tokens = TilemanState.getInstance().getTokens();
        
        setItem(11, createShopItem("Multi Unlock", "multi_unlock", Items.DIAMOND_PICKAXE, 
            shop.getMultiUnlockLevel(), ShopData.getMultiUnlockPrice(shop.getMultiUnlockLevel()), tokens,
            "Unlock multiple tiles at once",
            "Current: " + shop.getBaseTilesPerUnlock() + " tiles",
            "Next: " + (shop.getBaseTilesPerUnlock() + 1) + " tiles"));
        
        setItem(12, createShopItem("Lucky Unlock", "lucky_unlock", Items.RABBIT_FOOT,
            shop.getLuckyUnlockLevel(), ShopData.getLuckyUnlockPrice(shop.getLuckyUnlockLevel()), tokens,
            "Chance to unlock double tiles",
            "Current: " + shop.getLuckyPercent() + "% chance",
            "Next: " + (shop.getLuckyPercent() + 5) + "% chance"));
        
        setItem(13, createShopItem("Efficient Scaling", "efficient_scaling", Items.GOLD_INGOT,
            shop.getEfficientScalingLevel(), ShopData.getEfficientScalingPrice(shop.getEfficientScalingLevel()), tokens,
            "Reduce token cost scaling",
            "Current: " + (shop.getEfficientScalingLevel() * 2) + "% reduction",
            "Next: " + ((shop.getEfficientScalingLevel() + 1) * 2) + "% reduction"));
        
        setItem(20, createSkillAffinityItem("Combat", tokens));
        setItem(21, createSkillAffinityItem("Mining", tokens));
        setItem(22, createSkillAffinityItem("Foraging", tokens));
        setItem(23, createSkillAffinityItem("Farming", tokens));
        setItem(24, createSkillAffinityItem("Fishing", tokens));
        
        setItem(29, createShopItem("Frenzy Duration", "frenzy_duration", Items.CLOCK,
            shop.getFrenzyDurationLevel(), ShopData.getFrenzyDurationPrice(shop.getFrenzyDurationLevel()), tokens,
            "Increase XP Frenzy duration",
            "Current: " + (15 + shop.getFrenzyDurationLevel() * 5) + " minutes",
            "Next: " + (15 + (shop.getFrenzyDurationLevel() + 1) * 5) + " minutes"));
        
        setItem(30, createShopItem("Frenzy Power", "frenzy_power", Items.BLAZE_POWDER,
            shop.getFrenzyPowerLevel(), ShopData.getFrenzyPowerPrice(shop.getFrenzyPowerLevel()), tokens,
            "Increase XP Frenzy bonus",
            "Current: +" + (50 + shop.getFrenzyPowerLevel() * 10) + "% XP",
            "Next: +" + (50 + (shop.getFrenzyPowerLevel() + 1) * 10) + "% XP"));
    }

    private void loadConsumableItems() {
        ShopData shop = TilemanState.getInstance().getShop();
        int tokens = TilemanState.getInstance().getTokens();
        
        ItemStack remoteItem = new ItemStack(Items.ENDER_PEARL);
        String remoteReq = ShopRequirements.getConsumableRequirementText("remote_unlock");
        boolean remoteLocked = remoteReq != null;
        
        if (remoteLocked) {
            setItemName(remoteItem, "§cRemote Unlock §4[LOCKED]");
        } else {
            setItemName(remoteItem, "§aRemote Unlock");
        }
        
        List<Component> remoteLore = new ArrayList<>();
        remoteLore.add(Component.literal("§7Next unlock ignores adjacency"));
        remoteLore.add(Component.empty());
        
        if (remoteLocked) {
            remoteLore.add(Component.literal("§c" + remoteReq));
        } else if (shop.isRemoteUnlockPending()) {
            remoteLore.add(Component.literal("§6ACTIVE - Ready to use!"));
            remoteItem.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        } else {
            remoteLore.add(Component.literal("§7Cost: §6" + 5 + " tokens"));
            if (tokens >= 5) {
                remoteLore.add(Component.literal("§eClick to purchase!"));
            } else {
                remoteLore.add(Component.literal("§cNot enough tokens!"));
            }
        }
        setItemLore(remoteItem, remoteLore);
        setItem(21, remoteItem);
        
        ItemStack frenzyItem = new ItemStack(Items.EXPERIENCE_BOTTLE);
        String frenzyReq = ShopRequirements.getConsumableRequirementText("xp_frenzy");
        boolean frenzyLocked = frenzyReq != null;
        
        if (frenzyLocked) {
            setItemName(frenzyItem, "§cXP Frenzy §4[LOCKED]");
        } else {
            setItemName(frenzyItem, "§aXP Frenzy");
        }
        
        List<Component> frenzyLore = new ArrayList<>();
        int frenzyPower = 50 + shop.getFrenzyPowerLevel() * 10;
        int frenzyDuration = 15 + shop.getFrenzyDurationLevel() * 5;
        frenzyLore.add(Component.literal("§7Gain +" + frenzyPower + "% XP for " + frenzyDuration + " min"));
        frenzyLore.add(Component.empty());
        
        if (frenzyLocked) {
            frenzyLore.add(Component.literal("§c" + frenzyReq));
        } else if (shop.isXpFrenzyActive()) {
            int remaining = (int) (shop.getXpFrenzyRemainingMs() / 1000 / 60);
            frenzyLore.add(Component.literal("§6ACTIVE - " + remaining + " min remaining"));
            frenzyItem.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        } else {
            frenzyLore.add(Component.literal("§7Cost: §6" + 15 + " tokens"));
            if (tokens >= 15) {
                frenzyLore.add(Component.literal("§eClick to purchase!"));
            } else {
                frenzyLore.add(Component.literal("§cNot enough tokens!"));
            }
        }
        setItemLore(frenzyItem, frenzyLore);
        setItem(23, frenzyItem);
    }

    private ItemStack createCategoryItem(ShopCategory category) {
        ItemStack stack = switch (category) {
            case PERMANENT -> new ItemStack(Items.NETHER_STAR);
            case CONSUMABLES -> new ItemStack(Items.BREWING_STAND);
        };
        setItemName(stack, "§a" + category.getDisplayName());
        List<Component> lore = new ArrayList<>();
        lore.add(Component.literal("§7Click to browse"));
        setItemLore(stack, lore);
        return stack;
    }

    private ItemStack createBackItem() {
        ItemStack stack = new ItemStack(Items.ARROW);
        setItemName(stack, "§cBack");
        List<Component> lore = new ArrayList<>();
        lore.add(Component.literal("§7Return to main menu"));
        setItemLore(stack, lore);
        return stack;
    }

    private ItemStack createShopItem(String name, String itemKey, net.minecraft.world.item.Item item, int level, int price, int tokens, String... desc) {
        ItemStack stack = new ItemStack(item);
        int nextLevel = level + 1;
        String requirementText = ShopRequirements.getRequirementText(itemKey, nextLevel);
        boolean locked = requirementText != null;
        
        if (locked) {
            setItemName(stack, "§c" + name + " §7(Level " + level + ") §4[LOCKED]");
        } else {
            setItemName(stack, "§a" + name + " §7(Level " + level + ")");
        }
        
        List<Component> lore = new ArrayList<>();
        for (String line : desc) {
            lore.add(Component.literal("§7" + line));
        }
        lore.add(Component.empty());
        
        if (locked) {
            lore.add(Component.literal("§c" + requirementText));
        } else {
            lore.add(Component.literal("§7Cost: §6" + price + " tokens"));
            if (tokens >= price) {
                lore.add(Component.literal("§eClick to purchase!"));
            } else {
                lore.add(Component.literal("§cNot enough tokens!"));
            }
        }
        setItemLore(stack, lore);
        if (level > 0) {
            stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        }
        return stack;
    }

    private ItemStack createSkillAffinityItem(String skill, int tokens) {
        ShopData shop = TilemanState.getInstance().getShop();
        int level = shop.getSkillAffinityLevel(skill);
        int price = ShopData.getSkillAffinityPrice(level);
        String itemKey = skill.toLowerCase() + "_affinity";
        int nextLevel = level + 1;
        String requirementText = ShopRequirements.getRequirementText(itemKey, nextLevel);
        boolean locked = requirementText != null;
        
        net.minecraft.world.item.Item item = switch (skill) {
            case "Combat" -> Items.DIAMOND_SWORD;
            case "Mining" -> Items.DIAMOND_PICKAXE;
            case "Foraging" -> Items.DIAMOND_AXE;
            case "Farming" -> Items.DIAMOND_HOE;
            case "Fishing" -> Items.FISHING_ROD;
            default -> Items.PAPER;
        };
        
        ItemStack stack = new ItemStack(item);
        if (locked) {
            setItemName(stack, "§c" + skill + " Affinity §7(Level " + level + ") §4[LOCKED]");
        } else {
            setItemName(stack, "§a" + skill + " Affinity §7(Level " + level + ")");
        }
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.literal("§7Bonus XP from " + skill));
        int currentBonus = (int) ((shop.getSkillXpMultiplier(skill) - 1.0) * 100);
        int nextBonus = (int) ((1.0 + (1.0 - Math.pow(0.9, level + 1)) - 1.0) * 100);
        lore.add(Component.literal("§7Current: +" + currentBonus + "%"));
        lore.add(Component.literal("§7Next: +" + nextBonus + "%"));
        lore.add(Component.empty());
        
        if (locked) {
            lore.add(Component.literal("§c" + requirementText));
        } else {
            lore.add(Component.literal("§7Cost: §6" + price + " tokens"));
            if (tokens >= price) {
                lore.add(Component.literal("§eClick to purchase!"));
            } else {
                lore.add(Component.literal("§cNot enough tokens!"));
            }
        }
        setItemLore(stack, lore);
        if (level > 0) {
            stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        }
        return stack;
    }

    private void clearContainer() {
        for (int i = 0; i < SIZE; i++) {
            container.setItem(i, ItemStack.EMPTY);
        }
    }

    private void fillBorder() {
        ItemStack pane = new ItemStack(Items.STAINED_GLASS_PANE.black());
        setItemName(pane, " ");
        for (int i = 0; i < SIZE; i++) {
            container.setItem(i, pane.copy());
        }
    }

    private void setItem(int slot, ItemStack stack) {
        container.setItem(slot, stack);
    }

    private void setItemName(ItemStack stack, String name) {
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
    }

    private void setItemLore(ItemStack stack, List<Component> lore) {
        stack.set(DataComponents.LORE, new ItemLore(lore));
    }

    @Override
    protected void slotClicked(Slot slot, int slotId, int button, ContainerInput type) {
        if (slot == null || slotId < 0 || slotId >= SIZE) {
            return;
        }
        
        ItemStack clicked = slot.getItem();
        if (clicked.isEmpty()) return;
        
        String name = clicked.getHoverName().getString();
        
        if (currentCategory == null) {
            if (name.contains("Permanent")) {
                playClick();
                loadCategory(ShopCategory.PERMANENT);
            } else if (name.contains("Consumables")) {
                playClick();
                loadCategory(ShopCategory.CONSUMABLES);
            }
            return;
        }
        
        if (name.contains("Back")) {
            playClick();
            loadMainMenu();
            return;
        }
        
        handlePurchase(slotId, name);
    }

    private void handlePurchase(int slot, String name) {
        ShopData shop = TilemanState.getInstance().getShop();
        TilemanState state = TilemanState.getInstance();
        
        boolean purchased = false;
        String purchaseName = "";
        
        if (currentCategory == ShopCategory.PERMANENT) {
            if (name.contains("Multi Unlock")) {
                int nextLevel = shop.getMultiUnlockLevel() + 1;
                if (!ShopRequirements.canPurchase("multi_unlock", nextLevel)) {
                    playFail();
                    TilemanChat.warn("Unlock required milestone first!");
                    return;
                }
                int price = ShopData.getMultiUnlockPrice(shop.getMultiUnlockLevel());
                if (state.spendTokens(price)) {
                    shop.setMultiUnlockLevel(nextLevel);
                    purchased = true;
                    purchaseName = "Multi Unlock level " + shop.getMultiUnlockLevel();
                }
            } else if (name.contains("Lucky Unlock")) {
                int nextLevel = shop.getLuckyUnlockLevel() + 1;
                if (!ShopRequirements.canPurchase("lucky_unlock", nextLevel)) {
                    playFail();
                    TilemanChat.warn("Unlock required milestone first!");
                    return;
                }
                int price = ShopData.getLuckyUnlockPrice(shop.getLuckyUnlockLevel());
                if (state.spendTokens(price)) {
                    shop.setLuckyUnlockLevel(nextLevel);
                    purchased = true;
                    purchaseName = "Lucky Unlock level " + shop.getLuckyUnlockLevel();
                }
            } else if (name.contains("Efficient Scaling")) {
                int nextLevel = shop.getEfficientScalingLevel() + 1;
                if (!ShopRequirements.canPurchase("efficient_scaling", nextLevel)) {
                    playFail();
                    TilemanChat.warn("Unlock required milestone first!");
                    return;
                }
                int price = ShopData.getEfficientScalingPrice(shop.getEfficientScalingLevel());
                if (state.spendTokens(price)) {
                    shop.setEfficientScalingLevel(nextLevel);
                    purchased = true;
                    purchaseName = "Efficient Scaling level " + shop.getEfficientScalingLevel();
                }
            } else if (name.contains("Frenzy Duration")) {
                int nextLevel = shop.getFrenzyDurationLevel() + 1;
                if (!ShopRequirements.canPurchase("frenzy_duration", nextLevel)) {
                    playFail();
                    TilemanChat.warn("Unlock required milestone first!");
                    return;
                }
                int price = ShopData.getFrenzyDurationPrice(shop.getFrenzyDurationLevel());
                if (state.spendTokens(price)) {
                    shop.setFrenzyDurationLevel(nextLevel);
                    purchased = true;
                    purchaseName = "Frenzy Duration level " + shop.getFrenzyDurationLevel();
                }
            } else if (name.contains("Frenzy Power")) {
                int nextLevel = shop.getFrenzyPowerLevel() + 1;
                if (!ShopRequirements.canPurchase("frenzy_power", nextLevel)) {
                    playFail();
                    TilemanChat.warn("Unlock required milestone first!");
                    return;
                }
                int price = ShopData.getFrenzyPowerPrice(shop.getFrenzyPowerLevel());
                if (state.spendTokens(price)) {
                    shop.setFrenzyPowerLevel(nextLevel);
                    purchased = true;
                    purchaseName = "Frenzy Power level " + shop.getFrenzyPowerLevel();
                }
            } else if (name.contains("Affinity")) {
                String skill = extractSkill(name);
                if (skill != null) {
                    String itemKey = skill.toLowerCase() + "_affinity";
                    int nextLevel = shop.getSkillAffinityLevel(skill) + 1;
                    if (!ShopRequirements.canPurchase(itemKey, nextLevel)) {
                        playFail();
                        TilemanChat.warn("Unlock required milestone first!");
                        return;
                    }
                    int price = ShopData.getSkillAffinityPrice(shop.getSkillAffinityLevel(skill));
                    if (state.spendTokens(price)) {
                        shop.setSkillAffinityLevel(skill, nextLevel);
                        purchased = true;
                        purchaseName = skill + " Affinity level " + shop.getSkillAffinityLevel(skill);
                    }
                }
            }
        } else if (currentCategory == ShopCategory.CONSUMABLES) {
            if (name.contains("Remote Unlock") && !shop.isRemoteUnlockPending()) {
                if (!ShopRequirements.canPurchaseConsumable("remote_unlock")) {
                    playFail();
                    TilemanChat.warn("Unlock required milestone first!");
                    return;
                }
                if (state.spendTokens(5)) {
                    shop.setRemoteUnlockPending(true);
                    purchased = true;
                    purchaseName = "Remote Unlock";
                }
            } else if (name.contains("XP Frenzy") && !shop.isXpFrenzyActive()) {
                if (!ShopRequirements.canPurchaseConsumable("xp_frenzy")) {
                    playFail();
                    TilemanChat.warn("Unlock required milestone first!");
                    return;
                }
                if (state.spendTokens(15)) {
                    shop.activateXpFrenzy();
                    purchased = true;
                    purchaseName = "XP Frenzy";
                }
            }
        }
        
        if (purchased) {
            playPurchase();
            state.save();
            TilemanChat.info("Purchased " + purchaseName + "!");
            loadCategory(currentCategory);
        } else {
            playFail();
        }
    }

    private String extractSkill(String name) {
        String[] skills = {"Combat", "Mining", "Foraging", "Farming", "Fishing"};
        for (String skill : skills) {
            if (name.contains(skill)) return skill;
        }
        return null;
    }

    private void playClick() {
        Minecraft.getInstance().player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 1.0f, 1.0f);
    }

    private void playPurchase() {
        Minecraft.getInstance().player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
    }

    private void playFail() {
        Minecraft.getInstance().player.playSound(SoundEvents.VILLAGER_NO, 1.0f, 1.0f);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractBackground(graphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
