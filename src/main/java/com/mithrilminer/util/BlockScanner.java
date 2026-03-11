package com.mithrilminer.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class BlockScanner {

    private final MinecraftClient mc = MinecraftClient.getInstance();

    /**
     * Find all mithril blocks matching the given priority tier
     * within [radius] blocks, excluding the blacklist.
     */
    public List<BlockPos> findMithril(int priority, int radius, List<BlockPos> blacklist) {
        ClientWorld world = mc.world;
        if (world == null || mc.player == null) return List.of();

        BlockPos origin = mc.player.getBlockPos();
        List<BlockPos> results = new ArrayList<>();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos pos = origin.add(dx, dy, dz);
                    if (blacklist.contains(pos)) continue;
                    if (!MithrilBlocks.predicateForPriority(priority).test(world.getBlockState(pos))) continue;
                    if (!canMineBlock(pos)) continue;
                    results.add(pos);
                }
            }
        }

        results.sort(Comparator.comparingDouble(this::rotationCost));
        return results;
    }

    /**
     * Scan tiers in priority order, returning the first non-empty tier's results.
     */
    public List<BlockPos> findByPriorityOrder(int[] priorityOrder, int radius, List<BlockPos> blacklist) {
        for (int tier : priorityOrder) {
            List<BlockPos> found = findMithril(tier, radius, blacklist);
            if (!found.isEmpty()) return found;
        }
        return List.of();
    }

    /**
     * A block is mineable if at least one adjacent face is air or non-opaque.
     * Uses isOpaque() — compatible with 1.21.x (isTransparent was removed).
     */
    public boolean canMineBlock(BlockPos pos) {
        ClientWorld world = mc.world;
        if (world == null) return false;
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = pos.offset(dir);
            if (world.getBlockState(neighbor).isAir() || !world.getBlockState(neighbor).isOpaque()) {
                return true;
            }
        }
        return false;
    }

    public boolean hasLineOfSight(BlockPos pos) {
        if (mc.player == null || mc.world == null) return false;
        Vec3d eyePos = mc.player.getEyePos();
        Vec3d blockCenter = Vec3d.ofCenter(pos);
        BlockHitResult hit = mc.world.raycast(new RaycastContext(
                eyePos, blockCenter,
                RaycastContext.ShapeType.OUTLINE,
                RaycastContext.FluidHandling.NONE,
                mc.player
        ));
        return hit.getType() == HitResult.Type.MISS || hit.getBlockPos().equals(pos);
    }

    private double rotationCost(BlockPos pos) {
        if (mc.player == null) return Double.MAX_VALUE;
        Vec3d eye = mc.player.getEyePos();
        Vec3d target = Vec3d.ofCenter(pos);
        Vec3d delta = target.subtract(eye);

        double yaw   = Math.toDegrees(Math.atan2(-delta.x, delta.z));
        double pitch = Math.toDegrees(-Math.atan2(delta.y, Math.sqrt(delta.x * delta.x + delta.z * delta.z)));

        double yawDiff   = Math.abs(wrapDegrees(yaw - mc.player.getYaw()));
        double pitchDiff = Math.abs(pitch - mc.player.getPitch());
        return yawDiff + pitchDiff;
    }

    private double wrapDegrees(double d) {
        d = d % 360.0;
        if (d >= 180.0)  d -= 360.0;
        if (d < -180.0)  d += 360.0;
        return d;
    }
}
