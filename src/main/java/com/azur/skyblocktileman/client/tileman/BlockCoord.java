package com.azur.skyblocktileman.client.tileman;

import net.minecraft.core.BlockPos;

// gson serializable blockpos
public record BlockCoord(int x, int y, int z) {
    public static BlockCoord of(BlockPos pos) {
        return new BlockCoord(pos.getX(), pos.getY(), pos.getZ());
    }

    public BlockPos toBlockPos() {
        return new BlockPos(x, y, z);
    }
}
