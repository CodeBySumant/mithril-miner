package com.mithrilminer.util;

/**
 * Maps chat commands to allowed tier sets.
 *
 * Commands:
 *   !all        – mine everything (default)
 *   !mithril    – all mithril tiers (0-3)
 *   !gray       – gray mithril only
 *   !green      – green mithril only
 *   !blue       – blue mithril only
 *   !titanium   – titanium only
 *   !coal       – coal + quartz blocks (tier 4)
 *   !iron       – iron + lapis blocks (tier 5)
 *   !gold       – gold blocks (tier 6)
 *   !redstone   – redstone blocks (tier 7)
 *   !diamond    – diamond blocks (tier 8)
 *   !ores       – all SkyBlock ore blocks (tiers 4-8)
 */
public enum MiningMode {

    ALL        ("!all",       new int[]{0,1,2,3,4,5,6,7,8}),
    MITHRIL    ("!mithril",   new int[]{0,1,2,3}),
    GRAY       ("!gray",      new int[]{0}),
    GREEN      ("!green",     new int[]{1}),
    BLUE       ("!blue",      new int[]{2}),
    TITANIUM   ("!titanium",  new int[]{3}),
    COAL       ("!coal",      new int[]{4}),
    IRON       ("!iron",      new int[]{5}),
    GOLD       ("!gold",      new int[]{6}),
    REDSTONE   ("!redstone",  new int[]{7}),
    DIAMOND    ("!diamond",   new int[]{8}),
    ORES       ("!ores",      new int[]{4,5,6,7,8});

    public final String command;
    private final java.util.Set<Integer> allowedTiers;

    MiningMode(String command, int[] tiers) {
        this.command = command;
        this.allowedTiers = new java.util.HashSet<>();
        for (int t : tiers) allowedTiers.add(t);
    }

    public boolean allows(int tier) {
        return allowedTiers.contains(tier);
    }

    public String displayName() {
        return command.substring(1);
    }

    public static MiningMode fromCommand(String msg) {
        String lower = msg.trim().toLowerCase();
        for (MiningMode mode : values()) {
            if (mode.command.equals(lower)) return mode;
        }
        return null;
    }

    public static String helpList() {
        StringBuilder sb = new StringBuilder();
        for (MiningMode m : values()) sb.append(m.command).append(" ");
        return sb.toString().trim();
    }
}
