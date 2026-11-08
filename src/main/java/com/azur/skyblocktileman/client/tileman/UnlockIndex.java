package com.azur.skyblocktileman.client.tileman;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.Collection;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;

// packed x,z column to the y's unlocked in it, rebuilt on profile/island change
final class UnlockIndex {

    private static final IntSet NO_Y = new IntOpenHashSet(0);

    private final Long2ObjectMap<IntOpenHashSet> columns =
        new Long2ObjectOpenHashMap<>();

    private int minX = 1;
    private int maxX = 0;
    private int minZ = 1;
    private int maxZ = 0;

    // reused across nearestTo calls so the ring walk allocates nothing
    private final Best best = new Best();

    boolean isEmpty() {
        return columns.isEmpty();
    }

    void rebuild(Collection<BlockCoord> blocks) {
        columns.clear();
        minX = 1;
        maxX = 0;
        minZ = 1;
        maxZ = 0;
        for (BlockCoord block : blocks) {
            add(block);
        }
    }

    void add(BlockCoord block) {
        long key = key(block.x(), block.z());
        IntOpenHashSet ys = columns.get(key);
        if (ys == null) {
            ys = new IntOpenHashSet(2);
            columns.put(key, ys);
            expandBounds(block.x(), block.z());
        }
        ys.add(block.y());
    }

    private void expandBounds(int x, int z) {
        minX = Math.min(minX, x);
        maxX = Math.max(maxX, x);
        minZ = Math.min(minZ, z);
        maxZ = Math.max(maxZ, z);
    }

    IntSet columnYs(int x, int z) {
        IntOpenHashSet ys = columns.get(key(x, z));
        return ys == null ? NO_Y : ys;
    }

    // Cost tracks how much of the box overlaps unlocked territory, not how many tiles the player owns.
    void forEachWithin(
        BlockPos center,
        int radius,
        Consumer<BlockPos> visitor
    ) {
        if (isEmpty()) {
            return;
        }

        double radiusSq = (double) radius * radius;
        int centerX = center.getX();
        int centerZ = center.getZ();
        int fromX = Math.max(centerX - radius, minX);
        int toX = Math.min(centerX + radius, maxX);
        int fromZ = Math.max(centerZ - radius, minZ);
        int toZ = Math.min(centerZ + radius, maxZ);

        // Whichever is smaller: the clipped box's cells or the columns that hold tiles, so neither territory shape is a slow path.
        long cells = (long) (toX - fromX + 1) * (toZ - fromZ + 1);
        if (cells <= columns.size()) {
            for (int x = fromX; x <= toX; x++) {
                for (int z = fromZ; z <= toZ; z++) {
                    visitColumn(x, z, center, radiusSq, visitor);
                }
            }
        } else {
            for (Long2ObjectMap.Entry<IntOpenHashSet> entry : columns.long2ObjectEntrySet()) {
                int x = unpackX(entry.getLongKey());
                int z = unpackZ(entry.getLongKey());
                if (x < fromX || x > toX || z < fromZ || z > toZ) {
                    continue;
                }
                visitColumn(entry.getValue(), x, z, center, radiusSq, visitor);
            }
        }
    }

    private void visitColumn(
        int x,
        int z,
        BlockPos center,
        double radiusSq,
        Consumer<BlockPos> visitor
    ) {
        IntOpenHashSet ys = columns.get(key(x, z));
        if (ys != null) {
            visitColumn(ys, x, z, center, radiusSq, visitor);
        }
    }

    private void visitColumn(
        IntOpenHashSet ys,
        int x,
        int z,
        BlockPos center,
        double radiusSq,
        Consumer<BlockPos> visitor
    ) {
        for (int y : ys) {
            BlockPos pos = new BlockPos(x, y, z);
            if (center.distSqr(pos) <= radiusSq) {
                visitor.accept(pos);
            }
        }
    }

    // Walks one square ring at a time and stops once a ring cannot beat the best find; unlocks are adjacency-gated, so it exits early.
    BlockPos nearestTo(BlockPos center) {
        if (isEmpty()) {
            return null;
        }

        int centerX = center.getX();
        int centerZ = center.getZ();
        int maxRing = Math.max(
            Math.max(Math.abs(centerX - minX), Math.abs(centerX - maxX)),
            Math.max(Math.abs(centerZ - minZ), Math.abs(centerZ - maxZ))
        );

        best.reset(center, centerX, centerZ);

        for (int distance = 0; distance <= maxRing; distance++) {
            if (best.found() && (double) distance * distance > best.distSq()) {
                break;
            }
            walkRing(centerX, centerZ, distance);
        }

        return best.pos();
    }

    private void walkRing(int centerX, int centerZ, int distance) {
        if (distance == 0) {
            considerColumn(centerX, centerZ);
            return;
        }

        int fromX = centerX - distance;
        int toX = centerX + distance;
        int fromZ = centerZ - distance;
        int toZ = centerZ + distance;

        for (int x = fromX; x <= toX; x++) {
            considerColumn(x, fromZ);
            considerColumn(x, toZ);
        }
        for (int z = fromZ + 1; z < toZ; z++) {
            considerColumn(fromX, z);
            considerColumn(toX, z);
        }
    }

    private void considerColumn(int x, int z) {
        IntOpenHashSet ys = columns.get(key(x, z));
        if (ys == null) {
            return;
        }
        for (int y : ys) {
            best.consider(x, y, z);
        }
    }

    private static long key(int x, int z) {
        return ((long) x << 32) ^ (z & 0xFFFFFFFFL);
    }

    private static int unpackX(long key) {
        return (int) (key >> 32);
    }

    private static int unpackZ(long key) {
        return (int) key;
    }

    private static final class Best {

        private BlockPos center;
        private int centerX;
        private int centerZ;
        private BlockPos pos;
        private double distSq = Double.MAX_VALUE;

        private void reset(BlockPos center, int centerX, int centerZ) {
            this.center = center;
            this.centerX = centerX;
            this.centerZ = centerZ;
            this.pos = null;
            this.distSq = Double.MAX_VALUE;
        }

        private void consider(int x, int y, int z) {
            // cheap reject before building a BlockPos to compare
            long dx = (long) x - centerX;
            long dz = (long) z - centerZ;
            if (pos != null && dx * dx + dz * dz > distSq) {
                return;
            }
            BlockPos candidate = new BlockPos(x, y, z);
            double candidateDist = center.distSqr(candidate);
            if (candidateDist < distSq) {
                distSq = candidateDist;
                pos = candidate;
            }
        }

        private boolean found() {
            return pos != null;
        }

        private double distSq() {
            return distSq;
        }

        private BlockPos pos() {
            return pos;
        }
    }
}
