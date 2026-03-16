package com.mithrilminer;

import com.mithrilminer.config.MinerConfig;
import com.mithrilminer.macro.MithrilMacro;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MithrilMinerMod implements ClientModInitializer {

    public static final String MOD_ID = "skyblock_helper";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static MinerConfig  config;
    public static MithrilMacro macro;
    public static KeyBinding   toggleKey;

    private static final KeyBinding.Category MINER_CATEGORY =
            KeyBinding.Category.create(Identifier.of(MOD_ID, "general"));

    // Identifier for our HUD element
    private static final Identifier HUD_ID = Identifier.of(MOD_ID, "overlay");

    @Override
    public void onInitializeClient() {
        LOGGER.info("SkyBlock Helper initializing...");

        config = MinerConfig.load();
        macro  = new MithrilMacro();

        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key." + MOD_ID + ".toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_INSERT,
                MINER_CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleKey.wasPressed()) {
                macro.toggle();
            }
            macro.onTick(client);
        });

        // HudElementRegistry is the correct API for Fabric API 0.116+ (replaces
        // the deprecated HudRenderCallback which stopped working in 0.116+)
        HudElementRegistry.addLast(HUD_ID, (context, tickCounter) ->
                macro.renderHud(context)
        );

        LOGGER.info("SkyBlock Helper ready. Press INSERT to toggle.");
    }
}
