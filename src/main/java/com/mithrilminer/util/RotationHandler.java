package com.mithrilminer.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public final class RotationHandler {

    private final MinecraftClient mc = MinecraftClient.getInstance();

    private float targetYaw;
    private float targetPitch;
    private float startYaw;
    private float startPitch;
    private int   totalTicks;
    private int   elapsedTicks;
    private boolean active = false;

    /**
     * Start rotating toward the center of a BlockPos.
     * Kept for backward compatibility — MithrilMacro now prefers
     * startRotatingToPoint() with BlockUtil.getClosestVisibleSidePos().
     */
    public void startRotatingTo(BlockPos target, int ticks) {
        startRotatingToPoint(Vec3d.ofCenter(target), ticks);
    }

    /**
     * Start rotating toward an exact Vec3d world-space point.
     * Called with BlockUtil.getClosestVisibleSidePos() so the player aims
     * at the closest exposed face surface rather than the block center.
     */
    public void startRotatingToPoint(Vec3d point, int ticks) {
        if (mc.player == null) return;
        Vec3d eye   = mc.player.getEyePos();
        Vec3d delta = point.subtract(eye);

        targetYaw   = (float) Math.toDegrees(Math.atan2(-delta.x, delta.z));
        targetPitch = (float) Math.toDegrees(-Math.atan2(delta.y,
                Math.sqrt(delta.x * delta.x + delta.z * delta.z)));

        startYaw     = mc.player.getYaw();
        startPitch   = mc.player.getPitch();
        totalTicks   = Math.max(1, ticks);
        elapsedTicks = 0;
        active       = true;
    }

    public void onTick() {
        if (!active || mc.player == null) return;
        elapsedTicks++;
        float t = Math.min(1.0f, (float) elapsedTicks / totalTicks);
        mc.player.setYaw(lerpYaw(startYaw, targetYaw, t));
        mc.player.setPitch(lerp(startPitch, targetPitch, t));
        if (elapsedTicks >= totalTicks) active = false;
    }

    public boolean isComplete() {
        if (mc.player == null) return true;
        return !active
            || (Math.abs(wrapDegrees(mc.player.getYaw() - targetYaw)) < 1.0f
                && Math.abs(mc.player.getPitch() - targetPitch) < 1.0f);
    }

    public void reset() {
        active       = false;
        elapsedTicks = 0;
    }

    private float lerp(float a, float b, float t) { return a + (b - a) * t; }

    private float lerpYaw(float from, float to, float t) {
        return from + wrapDegrees(to - from) * t;
    }

    private float wrapDegrees(float d) {
        d = d % 360f;
        if (d >= 180f)  d -= 360f;
        if (d < -180f)  d += 360f;
        return d;
    }
}
