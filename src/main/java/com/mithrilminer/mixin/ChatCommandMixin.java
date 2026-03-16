package com.mithrilminer.mixin;

import com.mithrilminer.MithrilMinerMod;
import com.mithrilminer.util.MiningMode;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Intercepts outgoing chat messages before they are sent to the server.
 * If the message starts with '!' and matches a known MiningMode command,
 * the message is consumed locally (never sent to Hypixel) and the mode
 * is applied to the macro.
 */
@Mixin(ClientPlayNetworkHandler.class)
public class ChatCommandMixin {

    @Inject(method = "sendChatMessage", at = @At("HEAD"), cancellable = true)
    private void onSendChat(String message, CallbackInfo ci) {
        if (message == null || !message.startsWith("!")) return;

        MiningMode mode = MiningMode.fromCommand(message.trim());

        if (mode != null) {
            // Cancel — don't send to server
            ci.cancel();

            // Apply the mode
            MithrilMinerMod.macro.setMode(mode);

            // Confirm to player in local chat (false = action bar off, appears in chat)
            net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
            if (mc.player != null) {
                mc.player.sendMessage(
                    Text.literal("§6[SkyBlock Helper] §fMining mode → §a" + mode.displayName()),
                    false
                );
            }
        } else if (message.startsWith("!")) {
            // Unknown ! command — show help, still cancel so it's not sent to server
            ci.cancel();
            net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
            if (mc.player != null) {
                mc.player.sendMessage(
                    Text.literal("§6[SkyBlock Helper] §fCommands: §7" + MiningMode.helpList()),
                    false
                );
            }
        }
    }
}
