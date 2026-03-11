package com.mithrilminer.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.List;

public final class BlockScanner {

    private final MinecraftClient mc = MinecraftClient.getInstance();

    /**
     * Finds the closest valid Mithril block to the player's crosshair
     * within a strict maximum distance, ignoring tier priorities.
     */
    public BlockPos findClosestToCrosshair(double maxDistance, List<BlockPos> blacklist) {
        ClientWorld world = mc.world;
        if (world == null || mc.player == null) return null;

        BlockPos origin = mc.player.getBlockPos();
        int r = (int) Math.ceil(maxDistance);
        BlockPos bestPos = null;
        double bestCost = Double.MAX_VALUE;
        Vec3d eyePos = mc.player.getEyePos();

        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    BlockPos pos = origin.add(dx, dy, dz);
                    if (blacklist.contains(pos)) continue;

                    // 1. Enforce strict distance from player's eyes
                    if (eyePos.distanceTo(Vec3d.ofCenter(pos)) > maxDistance) continue;

                    // 2. Must be ANY tier of Mithril
                    if (!MithrilBlocks.isMithril(world, pos)) continue;

                    // 3. Must be mineable and visible
                    if (!canMineBlock(pos)) continue;
                    if (!hasLineOfSight(pos)) continue;

                    // 4. Find the one closest to the center of your screen
                    double cost = rotationCost(pos);
                    if (cost < bestCost) {
                        bestCost = cost;
                        bestPos = pos;
                    }
                }
            }
        }
        return bestPos;
    }

    /**
     * A block is mineable if at least one adjacent face is air or non-opaque.
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