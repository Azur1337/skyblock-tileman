package com.azur.skyblocktileman.client.tileman.shop;

import com.azur.skyblocktileman.client.tileman.ShopData;
import com.azur.skyblocktileman.client.tileman.TilemanChat;
import com.azur.skyblocktileman.client.tileman.TilemanState;
import com.azur.skyblocktileman.client.tileman.ui.Theme;
import com.azur.skyblocktileman.client.tileman.ui.TilemanChestScreen;
import com.azur.skyblocktileman.client.tileman.ui.TilemanItem;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.IntUnaryOperator;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public class TilemanShopScreen extends TilemanChestScreen {

    private static final String[] SKILLS = {
        "Combat",
        "Mining",
        "Foraging",
        "Farming",
        "Fishing",
    };

    private ShopCategory currentCategory = null;

    private TilemanShopScreen() {
        super(6, Component.literal("Tileman Shop"));
    }

    public static void open() {
        open(null);
    }

    public static void open(Runnable back) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }
        client.schedule(() -> {
            TilemanShopScreen screen = new TilemanShopScreen();
            screen.setReturnTo(back);
            screen.loadView();
            client.gui.setScreen(screen);
        });
    }

    // rebuilds dynamic content (balances, frenzy countdown) while open
    @Override
    protected void refresh() {
        loadView();
    }

    private void loadView() {
        if (currentCategory == null) {
            loadMainMenu();
        } else {
            loadCategory(currentCategory);
        }
    }

    private void loadMainMenu() {
        buildFrame();
        addCategory(slot(2, 3), ShopCategory.PERMANENT);
        addCategory(slot(2, 5), ShopCategory.CONSUMABLES);
    }

    private void addCategory(int slot, ShopCategory category) {
        Item icon =
            category == ShopCategory.PERMANENT
                ? Items.NETHER_STAR
                : Items.BREWING_STAND;
        setItem(
            slot,
            TilemanItem.of(icon)
                .name(Theme.GOOD + category.getDisplayName())
                .blank()
                .action("Click to browse")
                .build()
        );
        onClick(slot, () -> {
            playNav();
            currentCategory = category;
            loadView();
        });
    }

    private void loadCategory(ShopCategory category) {
        buildFrame();
        addBack(this::goMainMenu);
        if (category == ShopCategory.PERMANENT) {
            loadPermanentItems();
        } else {
            loadConsumableItems();
        }
    }

    private void goMainMenu() {
        currentCategory = null;
        loadView();
    }

    private void loadPermanentItems() {
        ShopData shop = TilemanState.getInstance().getShop();

        addUpgrade(
            slot(1, 2),
            "Multi Unlock",
            "multi_unlock",
            Items.DIAMOND_PICKAXE,
            Integer.MAX_VALUE,
            shop::getMultiUnlockLevel,
            ShopData::getMultiUnlockPrice,
            shop::setMultiUnlockLevel,
            "Unlock multiple tiles at once",
            "Current: " + shop.getBaseTilesPerUnlock() + " tiles",
            "Next: " + (shop.getBaseTilesPerUnlock() + 1) + " tiles"
        );

        addUpgrade(
            slot(1, 3),
            "Lucky Unlock",
            "lucky_unlock",
            Items.RABBIT_FOOT,
            Integer.MAX_VALUE,
            shop::getLuckyUnlockLevel,
            ShopData::getLuckyUnlockPrice,
            shop::setLuckyUnlockLevel,
            "Chance to unlock extra tiles per token",
            "Current: " + shop.getLuckyPercent() + "%",
            "Next: " + (shop.getLuckyPercent() + 5) + "%"
        );

        addUpgrade(
            slot(1, 4),
            "Efficient Scaling",
            "efficient_scaling",
            Items.SUNFLOWER,
            ShopData.EFFICIENT_SCALING_MAX,
            shop::getEfficientScalingLevel,
            ShopData::getEfficientScalingPrice,
            shop::setEfficientScalingLevel,
            "Tokens cost less Skill XP to earn",
            "Current: -" + shop.getEfficientScalingBonus() + "% token cost",
            "Next: -" +
                (shop.getEfficientScalingLevel() + 1) * 2 +
                "% token cost"
        );

        addUpgrade(
            slot(1, 5),
            "Frenzy Duration",
            "frenzy_duration",
            Items.CLOCK,
            Integer.MAX_VALUE,
            shop::getFrenzyDurationLevel,
            ShopData::getFrenzyDurationPrice,
            shop::setFrenzyDurationLevel,
            "Longer XP Frenzy",
            "Current: " + (15 + shop.getFrenzyDurationLevel() * 5) + " min",
            "Next: " + (15 + (shop.getFrenzyDurationLevel() + 1) * 5) + " min"
        );

        addUpgrade(
            slot(1, 6),
            "Frenzy Power",
            "frenzy_power",
            Items.BLAZE_POWDER,
            Integer.MAX_VALUE,
            shop::getFrenzyPowerLevel,
            ShopData::getFrenzyPowerPrice,
            shop::setFrenzyPowerLevel,
            "Stronger XP Frenzy",
            "Current: +" + (50 + shop.getFrenzyPowerLevel() * 10) + "% XP",
            "Next: +" + (50 + (shop.getFrenzyPowerLevel() + 1) * 10) + "% XP"
        );

        for (int i = 0; i < SKILLS.length; i++) {
            addAffinity(slot(3, 2 + i), SKILLS[i]);
        }
    }

    private void addUpgrade(
        int slot,
        String label,
        String reqKey,
        Item icon,
        int maxLevel,
        IntSupplier levelGet,
        IntUnaryOperator priceFn,
        IntConsumer levelSet,
        String... desc
    ) {
        int tokens = TilemanState.getInstance().getTokens();
        int level = levelGet.getAsInt();
        int price = priceFn.applyAsInt(level);

        TilemanItem item = TilemanItem.of(icon);
        String status = null;
        String denyText = null;

        if (level >= maxLevel) {
            status = Theme.GOOD + "[MAX]";
            denyText = "Already at max level!";
        } else {
            denyText = ShopRequirements.getRequirementText(reqKey, level + 1);
            if (denyText != null) {
                status = Theme.BAD + "[LOCKED]";
            } else if (tokens < price) {
                status = Theme.BAD + "[LOCKED]";
                denyText =
                    "Not enough tokens! Need " + (price - tokens) + " more.";
            } else {
                status = Theme.GOOD + "[READY]";
                denyText = null;
            }
        }

        item.name(
            Theme.NAME +
                label +
                " " +
                Theme.DESC +
                "(Lvl " +
                level +
                ") " +
                status
        );
        item.blank();
        for (String line : desc) {
            item.desc(line);
        }
        item.blank();
        if (denyText != null) {
            item.bad(denyText);
        } else {
            item.action("Click to purchase");
        }
        item.value("Cost: " + price + " tokens");
        if (level > 0) {
            item.glint();
        }
        setItem(slot, item.build());

        onClick(slot, () ->
            buyUpgrade(label, reqKey, maxLevel, levelGet, priceFn, levelSet)
        );
    }

    private void addAffinity(int slot, String skill) {
        ShopData shop = TilemanState.getInstance().getShop();
        Item icon = switch (skill) {
            case "Combat" -> Items.DIAMOND_SWORD;
            case "Mining" -> Items.DIAMOND_PICKAXE;
            case "Foraging" -> Items.DIAMOND_AXE;
            case "Farming" -> Items.DIAMOND_HOE;
            default -> Items.FISHING_ROD;
        };
        addUpgrade(
            slot,
            skill + " Affinity",
            skill.toLowerCase() + "_affinity",
            icon,
            Integer.MAX_VALUE,
            () -> shop.getSkillAffinityLevel(skill),
            ShopData::getSkillAffinityPrice,
            level -> shop.setSkillAffinityLevel(skill, level),
            "Earn more token XP from " + skill,
            "Current: +" +
                ShopData.getSkillAffinityBonusPercent(
                    shop.getSkillAffinityLevel(skill)
                ) +
                "%",
            "Next: +" +
                ShopData.getSkillAffinityBonusPercent(
                    shop.getSkillAffinityLevel(skill) + 1
                ) +
                "%"
        );
    }

    private void loadConsumableItems() {
        ShopData shop = TilemanState.getInstance().getShop();
        int tokens = TilemanState.getInstance().getTokens();

        String remoteReq = ShopRequirements.getConsumableRequirementText(
            "remote_unlock"
        );
        boolean remoteReady = shop.isRemoteUnlockPending();
        int remotePrice = ShopData.getRemoteUnlockPrice();

        TilemanItem remote = TilemanItem.of(Items.ENDER_PEARL)
            .name(
                Theme.NAME +
                    "Remote Unlock " +
                    (remoteReq != null
                        ? Theme.BAD + "[LOCKED]"
                        : remoteReady
                          ? Theme.GOOD + "[ACTIVE]"
                          : Theme.GOOD + "[READY]")
            )
            .blank()
            .desc("Next unlock ignores adjacency");
        if (remoteReq != null) {
            remote.bad(remoteReq);
        } else if (remoteReady) {
            remote.good("Ready to use!");
            remote.glint();
        } else {
            remote.action(
                tokens >= remotePrice
                    ? "Click to purchase"
                    : "Not enough tokens! Need " +
                          (remotePrice - tokens) +
                          " more."
            );
            remote.value("Cost: " + remotePrice + " tokens");
        }
        int remoteSlot = slot(2, 3);
        setItem(remoteSlot, remote.build());
        onClick(remoteSlot, () -> {
            if (shop.isRemoteUnlockPending()) {
                TilemanChat.warn("Remote Unlock is already pending!");
                return;
            }
            buyConsumable("remote_unlock", remotePrice, "Remote Unlock", () ->
                shop.setRemoteUnlockPending(true)
            );
        });

        String frenzyReq = ShopRequirements.getConsumableRequirementText(
            "xp_frenzy"
        );
        boolean frenzyActive = shop.isXpFrenzyActive();
        int frenzyPrice = ShopData.getXpFrenzyPrice();
        int power = 50 + shop.getFrenzyPowerLevel() * 10;
        int duration = 15 + shop.getFrenzyDurationLevel() * 5;

        TilemanItem frenzy = TilemanItem.of(Items.EXPERIENCE_BOTTLE)
            .name(
                Theme.NAME +
                    "XP Frenzy " +
                    (frenzyReq != null
                        ? Theme.BAD + "[LOCKED]"
                        : frenzyActive
                          ? Theme.GOOD + "[ACTIVE]"
                          : Theme.GOOD + "[READY]")
            )
            .blank()
            .desc("+" + power + "% XP for " + duration + " minutes");
        if (frenzyReq != null) {
            frenzy.bad(frenzyReq);
        } else if (frenzyActive) {
            frenzy.good(
                Math.max(1, shop.getXpFrenzyRemainingMs() / 60000) +
                    " min remaining"
            );
            frenzy.glint();
        } else {
            frenzy.action(
                tokens >= frenzyPrice
                    ? "Click to purchase"
                    : "Not enough tokens! Need " +
                          (frenzyPrice - tokens) +
                          " more."
            );
            frenzy.value("Cost: " + frenzyPrice + " tokens");
        }
        int frenzySlot = slot(2, 5);
        setItem(frenzySlot, frenzy.build());
        onClick(frenzySlot, () -> {
            if (shop.isXpFrenzyActive()) {
                TilemanChat.warn("XP Frenzy is already active!");
                return;
            }
            buyConsumable(
                "xp_frenzy",
                frenzyPrice,
                "XP Frenzy",
                shop::activateXpFrenzy
            );
        });
    }

    private void buyUpgrade(
        String label,
        String reqKey,
        int maxLevel,
        IntSupplier levelGet,
        IntUnaryOperator priceFn,
        IntConsumer levelSet
    ) {
        TilemanState state = TilemanState.getInstance();
        int level = levelGet.getAsInt();

        if (level >= maxLevel) {
            TilemanChat.warn(label + " is already at max level!");
            return;
        }
        String reqText = ShopRequirements.getRequirementText(reqKey, level + 1);
        if (reqText != null) {
            TilemanChat.warn(reqText);
            return;
        }
        int price = priceFn.applyAsInt(level);
        if (!state.spendTokens(price)) {
            TilemanChat.warn(
                "Not enough tokens! Need " +
                    (price - state.getTokens()) +
                    " more."
            );
            return;
        }
        levelSet.accept(level + 1);
        playConfirm();
        TilemanChat.bought(label + " level " + (level + 1));
    }

    private void buyConsumable(
        String reqKey,
        int price,
        String label,
        Runnable apply
    ) {
        TilemanState state = TilemanState.getInstance();
        String reqText = ShopRequirements.getConsumableRequirementText(reqKey);
        if (reqText != null) {
            TilemanChat.warn(reqText);
            return;
        }
        if (!state.spendTokens(price)) {
            TilemanChat.warn(
                "Not enough tokens! Need " +
                    (price - state.getTokens()) +
                    " more."
            );
            return;
        }
        apply.run();
        playConfirm();
        TilemanChat.bought(label);
    }
}
