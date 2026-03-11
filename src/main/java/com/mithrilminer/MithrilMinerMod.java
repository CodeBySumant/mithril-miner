package com.mithrilminer;

import com.mithrilminer.config.MinerConfig;
import com.mithrilminer.macro.MithrilMacro;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MithrilMinerMod implements ClientModInitializer {

    public static final String MOD_ID = "mithrilminer";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static MinerConfig config;
    public static MithrilMacro macro;
    public static KeyBinding toggleKey;

    // 1.21.9+ requires KeyBinding.Category instead of a plain String
    private static final KeyBinding.Category MINER_CATEGORY =
            KeyBinding.Category.create(Identifier.of(MOD_ID, "general"));

    @Override
    public void onInitializeClient() {
        LOGGER.info("Mithril Miner initializing...");

        config = MinerConfig.load();
        macro = new MithrilMacro();

        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.mithrilminer.toggle",
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

        HudRenderCallback.EVENT.register((drawContext, tickDeltaManager) ->
                macro.renderHud(drawContext)
        );

        LOGGER.info("Mithril Miner ready. Press INSERT to toggle.");
    }
}
