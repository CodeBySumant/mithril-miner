package com.mithrilminer.macro;

import com.mithrilminer.MithrilMinerMod;
import com.mithrilminer.mixin.BreakProgressTracker;
import com.mithrilminer.util.BlockScanner;
import com.mithrilminer.util.MithrilBlocks;
import com.mithrilminer.util.RotationHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/**
 * Core mithril mining state machine.
 *
 * States:
 *   IDLE           – macro is off
 *   FIND_BLOCK     – scanning for the best nearby mithril block
 *   ROTATE_TO_BLOCK – smoothly turning to face the target block
 *   MINE_BLOCK     – holding left-click to mine
 *   STUCK_CHECK    – block not breaking, blacklist and retry
 *   COOLDOWN       – pause after too many stuck events
 */
public final class MithrilMacro {

    public enum State {
        IDLE, FIND_BLOCK, ROTATE_TO_BLOCK, MINE_BLOCK, STUCK_CHECK, COOLDOWN
    }

    private State state = State.IDLE;

    private final MinecraftClient mc = MinecraftClient.getInstance();
    private final BlockScanner    scanner  = new BlockScanner();
    private final RotationHandler rotation = new RotationHandler();

    private BlockPos targetBlock = null;
    private final List<BlockPos> blacklist = new ArrayList<>();

    private int  lastBreakProgress = -1;
    private int  ticksNoProgress   = 0;
    private int  stuckCount        = 0;
    private long cooldownStartMs   = 0;
    private boolean miningKeyHeld  = false;

    private String statusMessage = "Off";

    // ── Public API ─────────────────────────────────────────────────────────────

    public boolean isEnabled() { return state != State.IDLE; }
    public State   getState()  { return state; }

    public void toggle() {
        if (state == State.IDLE) onEnable(); else onDisable();
    }

    // ── Enable / Disable ───────────────────────────────────────────────────────

    private void onEnable() {
        MithrilMinerMod.LOGGER.info("MithrilMacro enabled.");
        if (MithrilMinerMod.config.playerSafetyRadius > 0
                && isPlayerNearby(MithrilMinerMod.config.playerSafetyRadius)) {
            log("Another player nearby — not starting!");
            return;
        }
        blacklist.clear();
        targetBlock   = null;
        stuckCount    = 0;
        ticksNoProgress = 0;
        rotation.reset();
        setState(State.FIND_BLOCK);
    }

    private void onDisable() {
        MithrilMinerMod.LOGGER.info("MithrilMacro disabled.");
        releaseMiningKey();
        rotation.reset();
        targetBlock = null;
        setState(State.IDLE);
    }

    // ── Main tick ─────────────────────────────────────────────────────────────

    public void onTick(MinecraftClient client) {
        if (state == State.IDLE) return;
        if (client.player == null || client.world == null) return;

        rotation.onTick();

        switch (state) {
            case FIND_BLOCK       -> tickFindBlock();
            case ROTATE_TO_BLOCK  -> tickRotate();
            case MINE_BLOCK       -> tickMine();
            case STUCK_CHECK      -> tickStuckCheck();
            case COOLDOWN         -> tickCooldown();
            default               -> {}
        }
    }

    // ── FIND_BLOCK ─────────────────────────────────────────────────────────────

    private void tickFindBlock() {
        releaseMiningKey();
        rotation.reset();
        ticksNoProgress = 0;

        List<BlockPos> candidates = scanner.findByPriorityOrder(
                getPriorityOrder(),
                MithrilMinerMod.config.searchRadius,
                blacklist
        );

        if (candidates.isEmpty()) {
            statusMessage = "No mithril nearby (r=" + MithrilMinerMod.config.searchRadius + ")";
            return;
        }

        targetBlock = candidates.get(0);
        int tier = MithrilBlocks.getTier(mc.world, targetBlock);
        statusMessage = "Found " + MithrilBlocks.tierName(tier) + " @ " + targetBlock.toShortString();
        log(statusMessage);

        rotation.startRotatingTo(targetBlock, MithrilMinerMod.config.rotationTicks);
        setState(State.ROTATE_TO_BLOCK);
    }

    // ── ROTATE_TO_BLOCK ────────────────────────────────────────────────────────

    private void tickRotate() {
        if (targetBlock == null) {
            setState(State.FIND_BLOCK);
            return;
        }
        if (!MithrilBlocks.isMithril(mc.world, targetBlock)) {
            log("Target gone before rotation finished, refinding...");
            targetBlock = null;
            setState(State.FIND_BLOCK);
            return;
        }
        if (rotation.isComplete()) {
            stuckCount        = 0;
            ticksNoProgress   = 0;
            lastBreakProgress = -1;
            setState(State.MINE_BLOCK);
        } else {
            statusMessage = "Rotating to "
                    + MithrilBlocks.tierName(MithrilBlocks.getTier(mc.world, targetBlock));
        }
    }

    // ── MINE_BLOCK ─────────────────────────────────────────────────────────────

    private void tickMine() {
        if (targetBlock == null) {
            setState(State.FIND_BLOCK);
            return;
        }

        ClientPlayerEntity player = mc.player;

        // Block was mined
        if (!MithrilBlocks.isMithril(mc.world, targetBlock)) {
            log("Block mined! Finding next...");
            releaseMiningKey();
            blacklist.clear();
            targetBlock = null;
            setState(State.FIND_BLOCK);
            return;
        }

        // Re-rotate if we're no longer looking at it
        HitResult hit = player.raycast(5.0, 0f, false);
        boolean lookingAtTarget = (hit instanceof BlockHitResult bhr)
                && bhr.getBlockPos().equals(targetBlock);

        if (!lookingAtTarget) {
            rotation.startRotatingTo(targetBlock, MithrilMinerMod.config.rotationTicks);
            setState(State.ROTATE_TO_BLOCK);
            return;
        }

        // Stuck detection via break progress
        int breakProgress = getBreakProgress(targetBlock);
        if (breakProgress > lastBreakProgress && breakProgress >= 0) {
            ticksNoProgress   = 0;
            lastBreakProgress = breakProgress;
        } else {
            ticksNoProgress++;
        }

        if (ticksNoProgress > MithrilMinerMod.config.stuckThresholdSeconds * 20) {
            stuckCount++;
            log("Stuck detected! (count=" + stuckCount + ")");
            releaseMiningKey();
            setState(State.STUCK_CHECK);
            return;
        }

        holdMiningKey();
        statusMessage = "Mining "
                + MithrilBlocks.tierName(MithrilBlocks.getTier(mc.world, targetBlock))
                + " (progress=" + breakProgress + "/9)";
    }

    // ── STUCK_CHECK ────────────────────────────────────────────────────────────

    private void tickStuckCheck() {
        if (stuckCount >= MithrilMinerMod.config.maxStuckCount) {
            log("Too many stuck events — cooldown.");
            stuckCount = 0;
            blacklist.clear();
            cooldownStartMs = System.currentTimeMillis();
            setState(State.COOLDOWN);
            return;
        }
        if (targetBlock != null) {
            blacklist.add(targetBlock);
            log("Blacklisted " + targetBlock.toShortString());
            targetBlock = null;
        }
        ticksNoProgress   = 0;
        lastBreakProgress = -1;
        setState(State.FIND_BLOCK);
    }

    // ── COOLDOWN ───────────────────────────────────────────────────────────────

    private void tickCooldown() {
        long elapsed = System.currentTimeMillis() - cooldownStartMs;
        long remaining = MithrilMinerMod.config.restartCooldownMs - elapsed;
        statusMessage = "Cooldown... " + remaining / 1000 + "s";
        if (elapsed >= MithrilMinerMod.config.restartCooldownMs) {
            log("Cooldown done, resuming.");
            setState(State.FIND_BLOCK);
        }
    }

    // ── HUD ────────────────────────────────────────────────────────────────────

    public void renderHud(DrawContext context) {
        if (mc.options.hudHidden) return;
        int x = 4;
        int y = mc.getWindow().getScaledHeight() - 36;
        String header = isEnabled()
                ? "§aMithril Miner §f[" + state + "]"
                : "§cMithril Miner §f[OFF]";
        context.drawTextWithShadow(mc.textRenderer, Text.literal(header),           x, y,      0xFFFFFF);
        context.drawTextWithShadow(mc.textRenderer, Text.literal("§7" + statusMessage), x, y + 10, 0xAAAAAA);
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private int[] getPriorityOrder() {
        return new int[]{
            MithrilMinerMod.config.priority1,
            MithrilMinerMod.config.priority2,
            MithrilMinerMod.config.priority3,
            MithrilMinerMod.config.priority4
        };
    }

    private int getBreakProgress(BlockPos pos) {
        if (mc.world == null) return -1;
        if (mc.world.getBlockState(pos).isAir()) return 9;
        return BreakProgressTracker.getProgressFor(pos);
    }

    private void holdMiningKey() {
        if (!miningKeyHeld && mc.player != null) {
            mc.options.attackKey.setPressed(true);
            miningKeyHeld = true;
        }
    }

    private void releaseMiningKey() {
        if (miningKeyHeld && mc.player != null) {
            mc.options.attackKey.setPressed(false);
            miningKeyHeld = false;
        }
    }

    private boolean isPlayerNearby(int radius) {
        if (mc.player == null || mc.world == null) return false;
        // getPos() does not exist in 1.21.11 Yarn — use getX/Y/Z directly
        Vec3d pos = new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ());
        Box box = new Box(
                pos.subtract(radius, radius, radius),
                pos.add(radius, radius, radius)
        );
        return !mc.world.getEntitiesByClass(PlayerEntity.class, box, e -> e != mc.player).isEmpty();
    }

    private void setState(State newState) {
        MithrilMinerMod.LOGGER.debug("MithrilMacro: {} -> {}", state, newState);
        state = newState;
    }

    private void log(String msg) {
        MithrilMinerMod.LOGGER.info("[MithrilMacro] {}", msg);
        if (mc.player != null) {
            mc.player.sendMessage(Text.literal("§6[MithrilMiner] §f" + msg), false);
        }
    }
}
