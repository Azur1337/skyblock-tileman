package com.azur.skyblocktileman.client.tileman;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.TallGrassBlock;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;

public final class BlockValidation {

    private static final Direction[] HORIZONTAL_DIRECTIONS = {
        Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST
    };

    private BlockValidation() {}

    public static boolean isHorizontallyAdjacentToUnlocked(BlockPos pos, TilemanState state) {
        for (Direction direction : HORIZONTAL_DIRECTIONS) {
            BlockPos neighbor = pos.relative(direction);
            for (int yOffset = -1; yOffset <= 1; yOffset++) {
                if (state.isUnlocked(neighbor.offset(0, yOffset, 0))) {
                    return true;
                }
            }
        }
        return false;
    }

    public static BlockPos getStandableBlock(BlockPos clicked, Level level) {
        if (level == null) {
            return clicked;
        }

        BlockPos current = clicked;
        for (int i = 0; i < 10; i++) {
            BlockState state = level.getBlockState(current);
            if (isStandableBlock(state, level, current)) {
                return current;
            }
            current = current.below();
        }

        return clicked;
    }

    public static boolean isStandableBlock(BlockState state, Level level, BlockPos pos) {
        Block block = state.getBlock();

        if (state.isAir()) {
            return false;
        }

        if (block instanceof LiquidBlock) {
            return false;
        }

        if (block instanceof CropBlock ||
            block instanceof BushBlock ||
            block instanceof TallGrassBlock ||
            block instanceof FlowerBlock ||
            block instanceof DoublePlantBlock ||
            block instanceof SugarCaneBlock ||
            block instanceof VineBlock) {
            return false;
        }

        if (block instanceof TorchBlock ||
            block instanceof SignBlock) {
            return false;
        }

        if (block instanceof CarpetBlock ||
            block instanceof SnowLayerBlock) {
            return false;
        }

        if (block instanceof FenceBlock ||
            block instanceof FenceGateBlock ||
            block instanceof WallBlock) {
            return false;
        }

        var shape = state.getCollisionShape(level, pos, CollisionContext.empty());
        if (shape.isEmpty()) {
            return false;
        }

        double maxY = shape.max(Direction.Axis.Y);
        return maxY >= 0.5;
    }

    public static boolean isNonStandableBlock(BlockState state, Level level, BlockPos pos) {
        return !isStandableBlock(state, level, pos);
    }

    public static boolean isPlayerOnUnlockedBlock() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) {
            return false;
        }
        BlockPos standingOn = findStandingBlock(client.level, client.player.getX(), client.player.getY(), client.player.getZ());
        return TilemanState.getInstance().isUnlocked(standingOn);
    }
    
    private static BlockPos findStandingBlock(Level level, double x, double playerY, double z) {
        int blockX = (int) Math.floor(x);
        int blockZ = (int) Math.floor(z);
        
        for (int yOffset = 0; yOffset >= -2; yOffset--) {
            int checkY = (int) Math.floor(playerY) + yOffset;
            BlockPos pos = new BlockPos(blockX, checkY, blockZ);
            BlockState state = level.getBlockState(pos);
            
            if (state.isAir()) {
                continue;
            }
            
            var shape = state.getCollisionShape(level, pos, CollisionContext.empty());
            if (shape.isEmpty()) {
                continue;
            }
            
            double blockTop = checkY + shape.max(Direction.Axis.Y);
            if (playerY >= blockTop - 0.01 && playerY <= blockTop + 0.5) {
                return pos;
            }
        }
        
        return new BlockPos(blockX, (int) Math.floor(playerY) - 1, blockZ);
    }
}
