package com.azur.skyblocktileman.mixin;

import com.azur.skyblocktileman.client.tileman.DebugCategory;
import com.azur.skyblocktileman.client.tileman.SkillXpParser;
import com.azur.skyblocktileman.client.tileman.TilemanLog;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// disabled, hypixel uses system chat overlay not this packet
@Mixin(ClientPacketListener.class)
public class TilemanActionBarMixin {

    // @Inject(method = "setActionBarText", at = @At("TAIL"))
    private void tileman$onActionBar(
        ClientboundSetActionBarTextPacket packet,
        CallbackInfo ci
    ) {
        String text = packet.text().getString();
        TilemanLog.debug(DebugCategory.ACTION_BAR, "Raw action bar packet text: [{}]", text);
        SkillXpParser.parse(text);
    }
}
