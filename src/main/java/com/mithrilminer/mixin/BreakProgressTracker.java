package com.mithrilminer.mixin;

import net.minecraft.util.math.BlockPos;

public final class BreakProgressTracker {

    private BreakProgressTracker() {}

    public static BlockPos currentPos      = null;
    public static int      currentProgress = -1;
    public static long     lastUpdateMs    = 0;

    public static void update(BlockPos pos, int progress) {
        currentPos      = pos;
        currentProgress = progress;
        lastUpdateMs    = System.currentTimeMillis();
    }

    public static void reset() {
        currentPos      = null;
        currentProgress = -1;
        lastUpdateMs    = 0;
    }

    /** Returns break progress for the given pos, or -1 if stale / different block. */
    public static int getProgressFor(BlockPos pos) {
        if (pos == null || currentPos == null) return -1;
        if (!pos.equals(currentPos))           return -1;
        if (System.currentTimeMillis() - lastUpdateMs > 2000) return -1;
        return currentProgress;
    }
}
