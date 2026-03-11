package com.mithrilminer.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.BlockBreakingProgressS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Intercepts BlockBreakingProgressS2CPacket to track our own block-break progress.
 */
@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {

    @Inject(method = "onBlockBreakingProgress", at = @At("TAIL"))
    private void onBlockBreakingProgress(BlockBreakingProgressS2CPacket packet, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        if (packet.getEntityId() != mc.player.getId()) return;
        BreakProgressTracker.update(packet.getPos(), packet.getProgress());
    }
}
