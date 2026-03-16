package com.mithrilminer.util;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.function.Predicate;

/**
 * All target block definitions — Mithril tiers AND SkyBlock ore blocks.
 *
 * IMPORTANT: Hypixel SkyBlock does NOT use ore blocks (coal_ore, redstone_ore etc.)
 * It uses the solid RESOURCE BLOCKS instead:
 *   QUARTZ_BLOCK, COAL_BLOCK, REDSTONE_BLOCK, LAPIS_BLOCK,
 *   DIAMOND_BLOCK, GOLD_BLOCK, IRON_BLOCK
 *
 * Tiers (higher = higher priority):
 *   0 = Gray Mithril   – Terracotta variants + Gray Wool
 *   1 = Green Mithril  – Prismarine
 *   2 = Blue Mithril   – Light Blue Wool
 *   3 = Titanium       – Polished Diorite
 *   4 = Coal / Quartz  – COAL_BLOCK, QUARTZ_BLOCK
 *   5 = Iron / Lapis   – IRON_BLOCK, LAPIS_BLOCK
 *   6 = Gold           – GOLD_BLOCK
 *   7 = Redstone       – REDSTONE_BLOCK
 *   8 = Diamond        – DIAMOND_BLOCK
 */
public final class MithrilBlocks {

    private MithrilBlocks() {}

    public static Predicate<BlockState> predicateForPriority(int priority) {
        return switch (priority) {

            // ── Mithril tiers ────────────────────────────────────────────────
            case 0 -> state ->
                    state.isOf(Blocks.TERRACOTTA)
                    || state.isOf(Blocks.WHITE_TERRACOTTA)
                    || state.isOf(Blocks.GRAY_TERRACOTTA)
                    || state.isOf(Blocks.GRAY_WOOL);

            case 1 -> state -> state.isOf(Blocks.PRISMARINE);

            case 2 -> state -> state.isOf(Blocks.LIGHT_BLUE_WOOL);

            case 3 -> state -> state.isOf(Blocks.POLISHED_DIORITE);

            // ── SkyBlock ore blocks (resource blocks, NOT ore blocks) ─────────
            case 4 -> state ->
                    state.isOf(Blocks.COAL_BLOCK)
                    || state.isOf(Blocks.QUARTZ_BLOCK);

            case 5 -> state ->
                    state.isOf(Blocks.IRON_BLOCK)
                    || state.isOf(Blocks.LAPIS_BLOCK);

            case 6 -> state -> state.isOf(Blocks.GOLD_BLOCK);

            case 7 -> state -> state.isOf(Blocks.REDSTONE_BLOCK);

            case 8 -> state -> state.isOf(Blocks.DIAMOND_BLOCK);

            default -> state -> false;
        };
    }

    public static String tierName(int priority) {
        return switch (priority) {
            case 0 -> "Gray Mithril";
            case 1 -> "Green Mithril";
            case 2 -> "Blue Mithril";
            case 3 -> "Titanium";
            case 4 -> "Coal/Quartz";
            case 5 -> "Iron/Lapis";
            case 6 -> "Gold";
            case 7 -> "Redstone";
            case 8 -> "Diamond";
            default -> "Unknown";
        };
    }

    /** Returns true if the block at pos is any target block. */
    public static boolean isMithril(World world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        for (int i = 0; i <= 8; i++) {
            if (predicateForPriority(i).test(state)) return true;
        }
        return false;
    }

    /**
     * Returns the priority tier of the block at pos, or -1 if not a target.
     * Checks highest tier first so diamond always beats gray mithril.
     */
    public static int getTier(World world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        for (int i = 8; i >= 0; i--) {
            if (predicateForPriority(i).test(state)) return i;
        }
        return -1;
    }
}
