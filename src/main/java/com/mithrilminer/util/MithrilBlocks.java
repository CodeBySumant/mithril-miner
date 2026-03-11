package com.mithrilminer.util;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.function.Predicate;

/**
 * Mithril block tier definitions.
 *
 * Tiers:
 *   0 = Gray     - Terracotta variants + Gray Wool
 *   1 = Green    - Prismarine
 *   2 = Blue     - Light Blue Wool
 *   3 = Titanium - Polished Diorite
 */
public final class MithrilBlocks {

    private MithrilBlocks() {}

    public static Predicate<BlockState> predicateForPriority(int priority) {
        return switch (priority) {
            case 0 -> state ->
                    state.isOf(Blocks.TERRACOTTA)
                    || state.isOf(Blocks.WHITE_TERRACOTTA)
                    || state.isOf(Blocks.GRAY_TERRACOTTA)
                    || state.isOf(Blocks.GRAY_WOOL);
            case 1 -> state -> state.isOf(Blocks.PRISMARINE);
            case 2 -> state -> state.isOf(Blocks.LIGHT_BLUE_WOOL);
            case 3 -> state -> state.isOf(Blocks.POLISHED_DIORITE);
            default -> state -> false;
        };
    }

    public static String tierName(int priority) {
        return switch (priority) {
            case 0 -> "Gray Mithril";
            case 1 -> "Green Mithril";
            case 2 -> "Blue Mithril";
            case 3 -> "Titanium";
            default -> "Unknown";
        };
    }

    public static boolean isMithril(World world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        for (int i = 0; i <= 3; i++) {
            if (predicateForPriority(i).test(state)) return true;
        }
        return false;
    }

    public static int getTier(World world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        for (int i = 3; i >= 0; i--) {
            if (predicateForPriority(i).test(state)) return i;
        }
        return -1;
    }
}
