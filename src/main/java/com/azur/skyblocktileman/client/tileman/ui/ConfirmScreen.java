package com.azur.skyblocktileman.client.tileman.ui;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;

// Generic tiny yes/no chest, used for confirmations like the profile opt-in.
// ESC and the barrier both count as "No".
public final class ConfirmScreen extends TilemanChestScreen {

    private static final int LORE_LINE_WIDTH = 38;

    private final Runnable onYes;
    private final Runnable onNo;
    private boolean answered;

    private ConfirmScreen(
        String question,
        String detail,
        Runnable onYes,
        Runnable onNo
    ) {
        super(3, Component.literal(question));
        this.onYes = onYes;
        this.onNo = onNo;

        buildFrame();

        TilemanItem sign = TilemanItem.of(Items.OAK_SIGN)
            .name(Theme.NAME + question)
            .blank();
        for (String line : wrap(detail, LORE_LINE_WIDTH)) {
            sign.desc(line);
        }
        setItem(slot(1, 4), sign.build());

        int yesSlot = slot(1, 2);
        setItem(yesSlot, Theme.yes());
        onClick(yesSlot, () -> answer(onYes, true));

        int noSlot = slot(1, 6);
        setItem(noSlot, Theme.no());
        onClick(noSlot, () -> answer(onNo, false));
    }

    private static List<String> wrap(String text, int width) {
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : text.split(" ")) {
            if (
                current.length() > 0 &&
                current.length() + 1 + word.length() > width
            ) {
                lines.add(current.toString());
                current.setLength(0);
            }
            if (current.length() > 0) {
                current.append(' ');
            }
            current.append(word);
        }
        if (current.length() > 0) {
            lines.add(current.toString());
        }
        return lines;
    }

    public static void open(
        String question,
        String detail,
        Runnable onYes,
        Runnable onNo
    ) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }
        client.schedule(() ->
            client.gui.setScreen(
                new ConfirmScreen(question, detail, onYes, onNo)
            )
        );
    }

    private void answer(Runnable action, boolean confirmSound) {
        answered = true;
        if (confirmSound) {
            playConfirm();
        } else {
            playNav();
        }
        Minecraft.getInstance().gui.setScreen(null);
        action.run();
    }

    @Override
    public void onClose() {
        if (!answered) {
            answered = true;
            onNo.run();
        }
        super.onClose();
    }
}
