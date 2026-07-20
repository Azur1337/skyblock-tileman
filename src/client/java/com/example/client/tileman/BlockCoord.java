package com.example.client.tileman;

import net.minecraft.core.BlockPos;

/**
 * A simple, immutable, Gson-serializable block coordinate.
 * We use a dedicated record instead of BlockPos directly so that:
 *  - Gson serializes it as a plain {"x":.., "y":.., "z":..} JSON object, independent
 *    of Minecraft's own BlockPos implementation (which may not serialize cleanly,
 *    and may change between versions).
 *  - It gets free, correct equals()/hashCode() implementations, which is required
 *    for storing these safely in a Set.
 */
public record BlockCoord(int x, int y, int z) {
    public static BlockCoord of(BlockPos pos) {
        return new BlockCoord(pos.getX(), pos.getY(), pos.getZ());
    }

    public BlockPos toBlockPos() {
        return new BlockPos(x, y, z);
    }
}
