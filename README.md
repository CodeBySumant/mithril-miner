# Mithril Miner ⛏️

A highly efficient, automated Mithril mining macro for Minecraft 1.21.11 (Fabric). Designed specifically for Hypixel Skyblock, this mod utilizes a robust state machine, smooth camera rotations, and crosshair-based block targeting to create a reliable and human-like mining experience.

## ✨ Features

* **Crosshair-Based Targeting:** Prioritizes mining the Mithril block closest to the center of your screen within a strict 4-block radius, preventing unnatural "snapping" to distant blocks.
* **Smooth Rotations:** Uses linear interpolation (lerping) to smoothly transition yaw and pitch over a configurable amount of ticks.
* **Smart Block Detection:** Checks for line-of-sight and block mineability (exposed faces) before attempting to mine.
* **Stuck Detection & Blacklisting:** Tracks server-side block break progress. If a block fails to mine after a set time, the macro blacklists the block and moves to the next one.
* **Player Safety:** Optional feature to halt the macro if another player enters a defined radius.
* **On-Screen HUD:** Displays the current state of the macro, the target block tier, and mining progress directly on your screen.

## ⚙️ How It Works (The State Machine)

The core logic is driven by a strict state machine `MithrilMacro.java`:

1. `IDLE`: The macro is disabled.
2. `FIND_BLOCK`: Scans a 4-block spherical radius for mapped Mithril blocks (Terracotta, Prismarine, Wool, Diorite). It calculates the rotation cost and selects the block closest to your crosshair.
3. `ROTATE_TO_BLOCK`: Smoothly interpolates the player's camera to look exactly at the center of the target block.
4. `MINE_BLOCK`: Programmatically holds the attack key (Left-Click). It listens for `BlockBreakingProgressS2CPacket` from the server to track if the block is actually breaking.
5. `STUCK_CHECK`: If mining takes too long without progress, the block is blacklisted for the current session.
6. `COOLDOWN`: Triggered if the macro gets stuck multiple times in a row, pausing the bot to prevent suspicious behavior.

## 🚀 Usage

1. Launch Minecraft 1.21.11 with the Fabric Loader.
2. Join a Hypixel Skyblock lobby and head to the Dwarven Mines.
3. Press **`INSERT`** (default) to toggle the macro on or off. 
4. The macro will automatically find the closest Mithril, rotate, and mine it. You do not need to hold down your mouse.

## 🛠️ Configuration

Upon first launch, a configuration file is generated at `config/mithrilminer.json`. You can modify the following parameters:

* `rotationTicks` (Default: 8): How many in-game ticks it takes to complete a rotation. Higher = slower/smoother.
* `stuckThresholdSeconds` (Default: 4): How long the macro will hold left-click without server break progress before considering itself stuck.
* `maxStuckCount` (Default: 3): How many consecutive stuck blocks trigger a cooldown.
* `restartCooldownMs` (Default: 6000): Cooldown duration in milliseconds after hitting the max stuck count.
* `playerSafetyRadius` (Default: 0): If set above 0, the macro will not start if another player is within this radius.

## 📦 Building from Source

This project uses the Gradle build system and the Fabric Loom plugin.

1. Clone the repository.
2. Open a terminal in the project directory.
3. Run the following command:
   ```bash
   ./gradlew build
