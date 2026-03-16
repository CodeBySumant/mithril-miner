package com.mithrilminer.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mithrilminer.MithrilMinerMod;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Path;

public final class MinerConfig {

    // Use MOD_ID from root class so it stays in sync automatically
    private static final Logger LOGGER      = LoggerFactory.getLogger(MithrilMinerMod.MOD_ID);
    private static final Gson   GSON        = new GsonBuilder().setPrettyPrinting().create();
    private static final Path   CONFIG_PATH =
            FabricLoader.getInstance().getConfigDir().resolve(MithrilMinerMod.MOD_ID + ".json");

    // Mining priorities: 0=Gray, 1=Green, 2=Blue, 3=Titanium
    public int priority1 = 3;
    public int priority2 = 2;
    public int priority3 = 1;
    public int priority4 = 0;

    public int rotationTicks          = 8;
    public int stuckThresholdSeconds  = 4;
    public int maxStuckCount          = 3;
    public int searchRadius           = 10;
    public int playerSafetyRadius     = 0;
    public int restartCooldownMs      = 6000;

    public static MinerConfig load() {
        if (CONFIG_PATH.toFile().exists()) {
            try (Reader r = new FileReader(CONFIG_PATH.toFile())) {
                MinerConfig cfg = GSON.fromJson(r, MinerConfig.class);
                if (cfg != null) return cfg;
            } catch (Exception e) {
                LOGGER.warn("Failed to load config, using defaults.", e);
            }
        }
        MinerConfig def = new MinerConfig();
        def.save();
        return def;
    }

    public void save() {
        try (Writer w = new FileWriter(CONFIG_PATH.toFile())) {
            GSON.toJson(this, w);
        } catch (Exception e) {
            LOGGER.warn("Failed to save config.", e);
        }
    }
}
