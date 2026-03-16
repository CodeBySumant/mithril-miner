package com.mithrilminer.macro;

import com.mithrilminer.MithrilMinerMod;
import com.mithrilminer.util.BlockScanner;
import com.mithrilminer.util.BlockUtil;
import com.mithrilminer.util.BreakProgressTracker;
import com.mithrilminer.util.MiningMode;
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

public final class MithrilMacro {

    public enum State {
        IDLE, FIND_BLOCK, ROTATE_TO_BLOCK, MINE_BLOCK, STUCK_CHECK, COOLDOWN
    }

    private State      state      = State.IDLE;
    private MiningMode miningMode = MiningMode.ALL;

    private final MinecraftClient mc       = MinecraftClient.getInstance();
    private final BlockScanner    scanner  = new BlockScanner();
    private final RotationHandler rotation = new RotationHandler();

    private BlockPos             targetBlock = null;
    private final List<BlockPos> blacklist   = new ArrayList<>();

    private int     lastBreakProgress = -1;
    private int     ticksNoProgress   = 0;
    private int     stuckCount        = 0;
    private long    cooldownStartMs   = 0;
    private boolean miningKeyHeld     = false;

    private String statusMessage = "Off";

    // ── Public API ─────────────────────────────────────────────────────────────

    public boolean    isEnabled() { return state != State.IDLE; }
    public State      getState()  { return state; }
    public MiningMode getMode()   { return miningMode; }

    public void toggle() {
        if (state == State.IDLE) onEnable(); else onDisable();
    }

    public void setMode(MiningMode mode) {
        this.miningMode = mode;
        MithrilMinerMod.LOGGER.info("Mining mode set to: {}", mode.displayName());
        if (state != State.IDLE) {
            releaseMiningKey();
            targetBlock = null;
            blacklist.clear();
            setState(State.FIND_BLOCK);
        }
    }

    // ── Enable / Disable ───────────────────────────────────────────────────────

    private void onEnable() {
        MithrilMinerMod.LOGGER.info("Macro enabled. Mode: {}", miningMode.displayName());
        if (MithrilMinerMod.config.playerSafetyRadius > 0
                && isPlayerNearby(MithrilMinerMod.config.playerSafetyRadius)) {
            log("Another player nearby — not starting!");
            return;
        }
        blacklist.clear();
        targetBlock     = null;
        stuckCount      = 0;
        ticksNoProgress = 0;
        rotation.reset();
        setState(State.FIND_BLOCK);
    }

    private void onDisable() {
        MithrilMinerMod.LOGGER.info("Macro disabled.");
        releaseMiningKey();
        rotation.reset();
        targetBlock = null;
        setState(State.IDLE);
    }

    // ── Main tick ──────────────────────────────────────────────────────────────

    public void onTick(MinecraftClient client) {
        if (state == State.IDLE) return;
        if (client.player == null || client.world == null) return;
        rotation.onTick();
        switch (state) {
            case FIND_BLOCK      -> tickFindBlock();
            case ROTATE_TO_BLOCK -> tickRotate();
            case MINE_BLOCK      -> tickMine();
            case STUCK_CHECK     -> tickStuckCheck();
            case COOLDOWN        -> tickCooldown();
            default              -> {}
        }
    }

    // ── FIND_BLOCK ─────────────────────────────────────────────────────────────

    private void tickFindBlock() {
        releaseMiningKey();
        rotation.reset();
        ticksNoProgress = 0;

        targetBlock = scanner.findBestBlock(4.5, blacklist, miningMode);

        if (targetBlock == null) {
            statusMessage = "No [" + miningMode.displayName() + "] in range";
            return;
        }

        int tier = MithrilBlocks.getTier(mc.world, targetBlock);
        statusMessage = "Found " + MithrilBlocks.tierName(tier) + " @ " + targetBlock.toShortString();
        log(statusMessage);

        Vec3d aimPoint = BlockUtil.getClosestVisibleSidePos(targetBlock);
        rotation.startRotatingToPoint(aimPoint, MithrilMinerMod.config.rotationTicks);
        setState(State.ROTATE_TO_BLOCK);
    }

    // ── ROTATE_TO_BLOCK ────────────────────────────────────────────────────────

    private void tickRotate() {
        if (targetBlock == null) { setState(State.FIND_BLOCK); return; }
        if (!MithrilBlocks.isMithril(mc.world, targetBlock)) {
            log("Target gone, refinding...");
            targetBlock = null;
            setState(State.FIND_BLOCK);
            return;
        }
        if (rotation.isComplete()) {
            stuckCount = 0; ticksNoProgress = 0; lastBreakProgress = -1;
            setState(State.MINE_BLOCK);
        } else {
            statusMessage = "Rotating to "
                    + MithrilBlocks.tierName(MithrilBlocks.getTier(mc.world, targetBlock));
        }
    }

    // ── MINE_BLOCK ─────────────────────────────────────────────────────────────

    private void tickMine() {
        if (targetBlock == null) { setState(State.FIND_BLOCK); return; }

        if (!MithrilBlocks.isMithril(mc.world, targetBlock)) {
            log("Block mined! Finding next...");
            releaseMiningKey();
            blacklist.clear();
            targetBlock = null;
            setState(State.FIND_BLOCK);
            return;
        }

        HitResult hit = mc.player.raycast(5.0, 0f, false);
        boolean lookingAtTarget = (hit instanceof BlockHitResult bhr)
                && bhr.getBlockPos().equals(targetBlock);

        if (!lookingAtTarget) {
            Vec3d aimPoint = BlockUtil.getClosestVisibleSidePos(targetBlock);
            rotation.startRotatingToPoint(aimPoint, MithrilMinerMod.config.rotationTicks);
            setState(State.ROTATE_TO_BLOCK);
            return;
        }

        int breakProgress = getBreakProgress(targetBlock);
        if (breakProgress > lastBreakProgress && breakProgress >= 0) {
            ticksNoProgress = 0;
            lastBreakProgress = breakProgress;
        } else {
            ticksNoProgress++;
        }

        if (ticksNoProgress > MithrilMinerMod.config.stuckThresholdSeconds * 20) {
            stuckCount++;
            log("Stuck! (count=" + stuckCount + ")");
            releaseMiningKey();
            setState(State.STUCK_CHECK);
            return;
        }

        holdMiningKey();
        statusMessage = "Mining "
                + MithrilBlocks.tierName(MithrilBlocks.getTier(mc.world, targetBlock))
                + " (" + breakProgress + "/9)";
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
            targetBlock = null;
        }
        ticksNoProgress = 0;
        lastBreakProgress = -1;
        setState(State.FIND_BLOCK);
    }

    // ── COOLDOWN ───────────────────────────────────────────────────────────────

    private void tickCooldown() {
        long elapsed   = System.currentTimeMillis() - cooldownStartMs;
        long remaining = MithrilMinerMod.config.restartCooldownMs - elapsed;
        statusMessage  = "Cooldown... " + remaining / 1000 + "s";
        if (elapsed >= MithrilMinerMod.config.restartCooldownMs) {
            log("Resuming.");
            setState(State.FIND_BLOCK);
        }
    }

    // ── HUD ────────────────────────────────────────────────────────────────────

    public void renderHud(DrawContext context) {
        // Only render when actually in-game (not in menus, loading screens etc.)
        if (mc.world == null || mc.player == null) return;
        if (mc.options.hudHidden) return;

        int screenHeight = mc.getWindow().getScaledHeight();
        int x = 4;
        int y = screenHeight - 48;

        // Line 1: on/off + state
        String line1 = isEnabled()
                ? "§aSkyBlock Helper §7[§f" + state + "§7]"
                : "§cSkyBlock Helper §7[§fOFF§7]";

        // Line 2: active mining mode
        String line2 = "§eMode: §f" + miningMode.displayName();

        // Line 3: current status
        String line3 = "§7" + statusMessage;

        context.drawTextWithShadow(mc.textRenderer, Text.literal(line1), x, y,      0xFFFFFF);
        context.drawTextWithShadow(mc.textRenderer, Text.literal(line2), x, y + 10, 0xFFFFFF);
        context.drawTextWithShadow(mc.textRenderer, Text.literal(line3), x, y + 20, 0xAAAAAA);
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

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
        Vec3d pos = new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ());
        Box box = new Box(pos.subtract(radius, radius, radius), pos.add(radius, radius, radius));
        return !mc.world.getEntitiesByClass(PlayerEntity.class, box, e -> e != mc.player).isEmpty();
    }

    private void setState(State newState) {
        MithrilMinerMod.LOGGER.debug("State: {} -> {}", state, newState);
        state = newState;
    }

    private void log(String msg) {
        MithrilMinerMod.LOGGER.info("[Macro] {}", msg);
        if (mc.player != null) {
            mc.player.sendMessage(Text.literal("§6[SkyBlock Helper] §f" + msg), false);
        }
    }
}
