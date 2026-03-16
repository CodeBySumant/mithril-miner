package com.mithrilminer.util;

import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import java.util.*;

/**
 * Ported from MightyMiner (Forge 1.8.9) BlockUtil.java → Fabric 1.21.11.
 *
 * Key API changes:
 *  - EnumFacing        → Direction
 *  - Vec3              → Vec3d
 *  - BlockPos.getX()   → same (unchanged)
 *  - mc.theWorld       → mc.world
 *  - mc.thePlayer      → mc.player
 *  - getPositionEyes() → getEyePos()
 *  - Block.getStateId  → removed; we use Registry-based lookup instead
 *  - shouldSideBeRendered → isSideSolid / isOpaque on BlockState
 */
public final class BlockUtil {

    // Credit: GTC (original), ported to Direction/Vec3d
    // Offsets for each face — used to aim at the inner surface of a block face.
    public static final Map<Direction, float[]> BLOCK_SIDES;

    static {
        BLOCK_SIDES = new EnumMap<>(Direction.class);
        BLOCK_SIDES.put(Direction.DOWN,  new float[]{0.5f, 0.01f, 0.5f});
        BLOCK_SIDES.put(Direction.UP,    new float[]{0.5f, 0.99f, 0.5f});
        BLOCK_SIDES.put(Direction.WEST,  new float[]{0.01f, 0.5f, 0.5f});
        BLOCK_SIDES.put(Direction.EAST,  new float[]{0.99f, 0.5f, 0.5f});
        BLOCK_SIDES.put(Direction.NORTH, new float[]{0.5f, 0.5f, 0.01f});
        BLOCK_SIDES.put(Direction.SOUTH, new float[]{0.5f, 0.5f, 0.99f});
    }

    // Center fallback (null face equivalent from original)
    private static final float[] CENTER_OFFSET = {0.5f, 0.5f, 0.5f};

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private BlockUtil() {}

    // ─── Side position helpers ────────────────────────────────────────────────

    /** Returns the world position of a specific face's inner surface point. */
    public static Vec3d getSidePos(BlockPos block, Direction face) {
        final float[] offset = face != null ? BLOCK_SIDES.get(face) : CENTER_OFFSET;
        return new Vec3d(block.getX() + offset[0], block.getY() + offset[1], block.getZ() + offset[2]);
    }

    /** Raycasts from the player's eye to a specific face. Returns true if unobstructed. */
    public static boolean canSeeSide(BlockPos block, Direction side) {
        if (mc.player == null || mc.world == null) return false;
        return canSeePoint(mc.player.getEyePos(), getSidePos(block, side), block);
    }

    /** Raycasts from a custom origin to a specific face. */
    public static boolean canSeeSide(Vec3d from, BlockPos block, Direction side) {
        if (mc.world == null) return false;
        return canSeePoint(from, getSidePos(block, side), block);
    }

    // ─── Visibility checks ────────────────────────────────────────────────────

    /**
     * Returns all faces of a block visible from the player's eye.
     * Replaces original getAllVisibleSides(BlockPos).
     */
    public static List<Direction> getAllVisibleSides(BlockPos block) {
        if (mc.player == null) return Collections.emptyList();
        return getAllVisibleSides(mc.player.getEyePos(), block);
    }

    /**
     * Returns all faces of a block visible from a custom origin.
     * Replaces original getAllVisibleSides(Vec3, BlockPos).
     */
    public static List<Direction> getAllVisibleSides(Vec3d from, BlockPos block) {
        if (mc.world == null) return Collections.emptyList();
        final List<Direction> sides = new ArrayList<>();
        for (Direction face : Direction.values()) {
            // Skip faces that are covered by an adjacent solid block
            if (isFaceCovered(block, face)) continue;
            if (canSeeSide(from, block, face)) {
                sides.add(face);
            }
        }
        return sides;
    }

    /**
     * Returns true if ANY face of the block has line-of-sight from the player.
     * Replaces original hasVisibleSide(BlockPos).
     */
    public static boolean hasVisibleSide(BlockPos block) {
        if (mc.player == null) return false;
        return hasVisibleSide(mc.player.getEyePos(), block);
    }

    public static boolean hasVisibleSide(Vec3d from, BlockPos block) {
        if (mc.world == null) return false;
        for (Direction face : Direction.values()) {
            if (isFaceCovered(block, face)) continue;
            if (canSeeSide(from, block, face)) return true;
        }
        return false;
    }

    // ─── Closest visible side ─────────────────────────────────────────────────

    /**
     * Returns the Vec3d aim point of the closest visible face to the player.
     * Replaces original getClosestVisibleSidePos(BlockPos).
     */
    public static Vec3d getClosestVisibleSidePos(BlockPos block) {
        if (mc.player == null) return Vec3d.ofCenter(block);
        return getClosestVisibleSidePos(mc.player.getEyePos(), block);
    }

    /**
     * Returns the Vec3d aim point of the closest visible face from a custom origin.
     * Replaces original getClosestVisibleSidePos(Vec3, BlockPos).
     */
    public static Vec3d getClosestVisibleSidePos(Vec3d from, BlockPos block) {
        if (mc.world == null) return Vec3d.ofCenter(block);

        Direction bestFace = null;
        double bestDist = Double.MAX_VALUE;

        for (Direction face : Direction.values()) {
            if (isFaceCovered(block, face)) continue;
            Vec3d sidePos = getSidePos(block, face);
            double dist = from.distanceTo(sidePos);
            if (canSeeSide(from, block, face) && dist < bestDist) {
                bestDist = dist;
                bestFace = face;
            }
        }

        return bestFace != null ? getSidePos(block, bestFace) : Vec3d.ofCenter(block);
    }

    /**
     * Returns the closest visible Direction from the player's eye.
     * Replaces original getClosestVisibleSide(BlockPos).
     */
    public static Direction getClosestVisibleSide(BlockPos block) {
        if (mc.player == null) return null;
        return getClosestVisibleSide(mc.player.getEyePos(), block);
    }

    public static Direction getClosestVisibleSide(Vec3d from, BlockPos block) {
        if (mc.world == null) return null;
        Direction bestFace = null;
        double bestDist = Double.MAX_VALUE;
        for (Direction face : Direction.values()) {
            if (isFaceCovered(block, face)) continue;
            double dist = from.distanceTo(getSidePos(block, face));
            if (canSeeSide(from, block, face) && dist < bestDist) {
                bestDist = dist;
                bestFace = face;
            }
        }
        return bestFace;
    }

    // ─── Block strength (ported from original getBlockStrength) ───────────────

    /**
     * Returns the effective "strength" value used to estimate mining time.
     * Original used Block.getStateId() — we accept the block's Registry name instead
     * and match against known SkyBlock block identifiers.
     *
     * For Hypixel SkyBlock Crystal Hollows blocks, strength maps to ~ticks to break
     * at base speed. Higher = harder.
     */
    public static int getBlockStrength(Block block) {
        Identifier id = Registries.BLOCK.getId(block);
        if (id == null) return 5000;

        return switch (id.getPath()) {
            // ── Vanilla ore/resource blocks ───────────────────────────────────
            case "diamond_block"  -> 600;
            case "gold_block"     -> 600;
            case "redstone_block" -> 600;
            case "lapis_block"    -> 600;
            case "emerald_block"  -> 600;
            case "iron_block"     -> 600;
            case "coal_block"     -> 600;

            // ── Vanilla ores ──────────────────────────────────────────────────
            case "coal_ore", "deepslate_coal_ore"         -> 500;
            case "iron_ore", "deepslate_iron_ore"         -> 500;
            case "copper_ore", "deepslate_copper_ore"     -> 500;
            case "gold_ore", "deepslate_gold_ore"         -> 500;
            case "lapis_ore", "deepslate_lapis_ore"       -> 500;
            case "redstone_ore", "deepslate_redstone_ore" -> 500;
            case "diamond_ore", "deepslate_diamond_ore"   -> 500;
            case "emerald_ore", "deepslate_emerald_ore"   -> 500;
            case "nether_gold_ore"                        -> 400;
            case "nether_quartz_ore"                      -> 400;
            case "ancient_debris"                         -> 3000;

            // ── SkyBlock Mithril tiers ────────────────────────────────────────
            case "polished_diorite"  -> 2000;  // Titanium
            case "light_blue_wool"   -> 1500;  // Blue Mithril
            case "prismarine"        -> 800;   // Green Mithril
            case "dark_prismarine"   -> 800;
            case "prismarine_bricks" -> 800;
            case "gray_wool"         -> 500;   // Gray Mithril
            case "gray_terracotta"   -> 500;
            case "terracotta"        -> 500;   // Gray Mithril (tier 0)
            case "white_terracotta"  -> 500;   // Gray Mithril (tier 0)

            // ── Gemstone blocks (SkyBlock) ────────────────────────────────────
            // Opal / Topaz (hardest common gems)
            case "white_stained_glass",
                 "yellow_stained_glass"      -> 3800;

            // Amber / Sapphire / Jade / Amethyst
            case "orange_stained_glass",
                 "blue_stained_glass",
                 "green_stained_glass",
                 "purple_stained_glass"      -> 3000;

            // Jasper (hardest single gem)
            case "red_stained_glass"         -> 4800;

            // Aquamarine / Peridot / Onyx / Citrine
            case "cyan_stained_glass",
                 "lime_stained_glass",
                 "black_stained_glass",
                 "yellow_terracotta"         -> 5200;

            // Ruby
            case "red_terracotta"            -> 2300;

            // Stone (hardstone in SkyBlock)
            case "stone"                     -> 50;

            default -> 5000;
        };
    }

    /**
     * Estimates mining time in ticks.
     * Formula preserved from original: ceil((strength * 30) / miningSpeed).
     */
    public static int getMiningTime(Block block, int miningSpeed) {
        return (int) Math.ceil((getBlockStrength(block) * 30.0) / miningSpeed);
    }

    // ─── Random aim point helpers ─────────────────────────────────────────────

    /**
     * Returns up to 20 random points on a specific face.
     * Ported from original pointsOnBlockSide (credited to GTC).
     */
    public static List<Vec3d> pointsOnBlockSide(BlockPos block, Direction side) {
        final Set<Vec3d> points = new LinkedHashSet<>();
        final Random rng = new Random();

        float[] it = side != null ? BLOCK_SIDES.get(side) : CENTER_OFFSET;
        for (int i = 0; i < 20; i++) {
            float x = it[0] == 0.5f ? randomVal(rng) : it[0];
            float y = it[1] == 0.5f ? randomVal(rng) : it[1];
            float z = it[2] == 0.5f ? randomVal(rng) : it[2];
            points.add(new Vec3d(block.getX() + x, block.getY() + y, block.getZ() + z));
        }
        return new ArrayList<>(points);
    }

    /** Returns random aim points on all visible sides, sorted by distance. */
    public static List<Vec3d> bestPointsOnVisibleSides(BlockPos block) {
        if (mc.player == null) return Collections.emptyList();
        Vec3d eye = mc.player.getEyePos();
        List<Vec3d> points = new ArrayList<>();
        for (Direction side : getAllVisibleSides(block)) {
            pointsOnBlockSide(block, side).stream()
                    .filter(p -> canSeePoint(eye, p, block))
                    .forEach(points::add);
        }
        points.sort(Comparator.comparingDouble(eye::distanceTo));
        return points;
    }

    // ─── Target finder (integrates MithrilBlocks tiers + BlockUtil visibility) ─

    /**
     * Finds the best target block within maxDistance.
     *
     * Priority order (highest first):
     *   3 = Titanium, 2 = Blue Mithril, 1 = Green Mithril, 0 = Gray Mithril
     *
     * Among equal-tier blocks, picks the one closest to the player's crosshair
     * (smallest yaw+pitch delta), identical to BlockScanner.rotationCost logic.
     *
     * Uses BlockUtil visibility (getClosestVisibleSidePos raycast) instead of
     * BlockScanner's simple center-of-block raycast, giving more accurate results
     * for partially exposed faces.
     *
     * @param world     current ClientWorld (use mc.world)
     * @param blacklist positions to skip (e.g. recently failed blocks)
     * @param maxDist   maximum reach in blocks (use config value, typically 4.5)
     * @return best BlockPos, or null if nothing found
     */
    public static BlockPos findBestTarget(World world, List<BlockPos> blacklist, double maxDist) {
        if (mc.player == null || world == null) return null;

        BlockPos origin  = mc.player.getBlockPos();
        Vec3d    eyePos  = mc.player.getEyePos();
        int      r       = (int) Math.ceil(maxDist);

        BlockPos bestPos  = null;
        int      bestTier = -1;
        double   bestCost = Double.MAX_VALUE;

        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    BlockPos pos = origin.add(dx, dy, dz);

                    if (blacklist != null && blacklist.contains(pos)) continue;
                    if (eyePos.distanceTo(Vec3d.ofCenter(pos)) > maxDist)  continue;

                    int tier = MithrilBlocks.getTier(world, pos);
                    if (tier < 0) continue;                        // not a target block

                    // Must have at least one exposed, visible face
                    if (!hasVisibleSide(eyePos, pos)) continue;

                    // Prefer higher tier; break ties by rotation cost
                    double cost = rotationCostFromEye(eyePos, pos);
                    if (tier > bestTier || (tier == bestTier && cost < bestCost)) {
                        bestTier = tier;
                        bestCost = cost;
                        bestPos  = pos;
                    }
                }
            }
        }
        return bestPos;
    }

    /** Yaw+pitch delta from eye to block center — lower = closer to crosshair. */
    private static double rotationCostFromEye(Vec3d eye, BlockPos pos) {
        Vec3d delta = Vec3d.ofCenter(pos).subtract(eye);
        double yaw   = Math.toDegrees(Math.atan2(-delta.x, delta.z));
        double pitch = Math.toDegrees(-Math.atan2(delta.y, Math.sqrt(delta.x*delta.x + delta.z*delta.z)));
        if (mc.player == null) return Double.MAX_VALUE;
        double dy = Math.abs(wrapDegreesD(yaw   - mc.player.getYaw()));
        double dp = Math.abs(pitch - mc.player.getPitch());
        return dy + dp;
    }

    private static double wrapDegreesD(double d) {
        d = d % 360.0;
        if (d >= 180.0) d -= 360.0;
        if (d < -180.0) d += 360.0;
        return d;
    }

    // ─── Long hash (from original, unchanged) ────────────────────────────────

    public static long longHash(int x, int y, int z) {
        long hash = 3241;
        hash = 3457689L * hash + x;
        hash = 8734625L * hash + y;
        hash = 2873465L * hash + z;
        return hash;
    }

    // ─── Internal helpers ─────────────────────────────────────────────────────

    /**
     * Returns true if the face of a block is fully covered by an adjacent opaque block.
     * Replaces shouldSideBeRendered() check from original.
     */
    private static boolean isFaceCovered(BlockPos block, Direction face) {
        if (mc.world == null) return true;
        BlockPos neighbor = block.offset(face);
        return mc.world.getBlockState(neighbor).isOpaque();
    }

    /**
     * Raycasts from `from` toward `target`, returns true if the first block hit
     * is the expected block (or the path is clear).
     */
    private static boolean canSeePoint(Vec3d from, Vec3d target, BlockPos expectedBlock) {
        if (mc.world == null || mc.player == null) return false;
        BlockHitResult hit = mc.world.raycast(new RaycastContext(
                from, target,
                RaycastContext.ShapeType.OUTLINE,
                RaycastContext.FluidHandling.NONE,
                mc.player
        ));
        return hit.getType() == HitResult.Type.MISS || hit.getBlockPos().equals(expectedBlock);
    }

    private static float randomVal(Random rng) {
        return (rng.nextInt(6) + 2) / 10.0f;
    }
}
