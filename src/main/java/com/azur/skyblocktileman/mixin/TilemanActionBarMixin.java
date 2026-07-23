package com.azur.skyblocktileman.mixin;

import com.azur.skyblocktileman.client.tileman.SkillXpParser;
import com.azur.skyblocktileman.client.tileman.TilemanLog;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Handles the modern dedicated action bar packet, some servers use this, some use
// the older system-chat-with-overlay-flag route (see TilemanActionBarListener for that).
@Mixin(ClientPacketListener.class)
public class TilemanActionBarMixin {

    @Inject(method = "setActionBarText", at = @At("TAIL"))
    private void tileman$onActionBar(
        ClientboundSetActionBarTextPacket packet,
        CallbackInfo ci
    ) {
        String text = packet.text().getString();
        TilemanLog.debug("Raw action bar packet text: [{}]", text);
        SkillXpParser.parse(text);
    }
}
