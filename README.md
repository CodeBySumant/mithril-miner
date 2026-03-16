# SkyBlock Helper

A client-side Fabric mod for **Hypixel SkyBlock** that automates mining in the Crystal Hollows and Dwarven Mines. Targets Mithril, Titanium, and all SkyBlock ore blocks with a smart tier-priority scanner and smooth rotation.

---

## Features

- **Tier-priority scanning** — always targets the most valuable block in range first (Diamond > Titanium > Redstone > Mithril, etc.)
- **Smooth rotation** — gradually turns to face the closest exposed face of the target block, not the center
- **Stuck detection** — automatically blacklists blocks that aren't breaking and moves on
- **Cooldown system** — pauses after repeated stuck events to avoid suspicious behavior
- **Chat commands** — type `!diamond` or `!mithril` in chat to instantly switch what the macro mines
- **HUD overlay** — always-visible status display showing current state, active mode, and target info
- **Player safety** — configurable radius that stops the macro if another player is nearby

---

## Requirements

| Dependency | Version |
|---|---|
| Minecraft | 1.21.11 |
| Fabric Loader | ≥ 0.18.4 |
| Fabric API | 0.141.3+1.21.11 |
| Java | 21 |

---

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/) for Minecraft 1.21.11
2. Download [Fabric API](https://modrinth.com/mod/fabric-api) and place it in your `mods` folder
3. Place `skyblock-helper-1.0.0.jar` in your `.minecraft/mods` folder
4. Launch Minecraft with the Fabric 1.21.11 profile

---

## Building from Source

```bash
git clone https://github.com/yourusername/skyblock-helper.git
cd skyblock-helper
./gradlew build
```

Output JAR will be at `build/libs/skyblock-helper-1.0.0.jar`.

---

## Usage

### Toggle the macro
Press **INSERT** to turn the macro on or off.

### Chat commands
Open chat, type a command and press Enter. The command is intercepted locally — **nothing is sent to the Hypixel server**.

| Command | What it mines |
|---|---|
| `!all` | Everything (default) |
| `!mithril` | All Mithril tiers |
| `!gray` | Gray Mithril only |
| `!green` | Green Mithril only |
| `!blue` | Blue Mithril only |
| `!titanium` | Titanium only |
| `!coal` | Coal Block + Quartz Block |
| `!iron` | Iron Block + Lapis Block |
| `!gold` | Gold Block |
| `!redstone` | Redstone Block |
| `!diamond` | Diamond Block |
| `!ores` | All SkyBlock ore blocks |

Switching modes while the macro is running instantly clears the current target and re-scans with the new filter.

### HUD
Three lines are displayed in the bottom-left corner of the screen:

```
SkyBlock Helper [MINE_BLOCK]
Mode: diamond
Mining Diamond @ 142, 58, -301 (7/9)
```

---

## Block Tiers

The scanner always picks the highest-tier block in range. Equal-tier blocks are broken by crosshair proximity.

| Tier | Blocks | SkyBlock location |
|---|---|---|
| 8 | `DIAMOND_BLOCK` | Crystal Hollows |
| 7 | `REDSTONE_BLOCK` | Crystal Hollows |
| 6 | `GOLD_BLOCK` | Crystal Hollows / Dwarven Mines |
| 5 | `IRON_BLOCK`, `LAPIS_BLOCK` | Dwarven Mines |
| 4 | `COAL_BLOCK`, `QUARTZ_BLOCK` | Dwarven Mines |
| 3 | `POLISHED_DIORITE` (Titanium) | Crystal Hollows |
| 2 | `LIGHT_BLUE_WOOL` (Blue Mithril) | Crystal Hollows |
| 1 | `PRISMARINE` (Green Mithril) | Crystal Hollows |
| 0 | `TERRACOTTA`, `GRAY_WOOL` (Gray Mithril) | Crystal Hollows |

---

## Configuration

The config file is created automatically at:
```
.minecraft/config/skyblock_helper.json
```

| Option | Default | Description |
|---|---|---|
| `rotationTicks` | `8` | Ticks to smoothly rotate to target (~0.4s) |
| `stuckThresholdSeconds` | `4` | Seconds with no break progress before considering stuck |
| `maxStuckCount` | `3` | Stuck events before entering cooldown |
| `searchRadius` | `10` | Block scan radius |
| `playerSafetyRadius` | `0` | Stop if a player is within this radius (0 = disabled) |
| `restartCooldownMs` | `6000` | Cooldown duration in milliseconds |

---

## Project Structure

```
src/main/java/com/mithrilminer/
├── MithrilMinerMod.java               # Mod entry point, key binding, HUD registration
├── config/
│   └── MinerConfig.java               # JSON config loader/saver
├── macro/
│   └── MithrilMacro.java              # Core state machine (FIND → ROTATE → MINE)
├── mixin/
│   ├── ClientPlayNetworkHandlerMixin  # Intercepts break progress packets
│   └── ChatCommandMixin               # Intercepts ! chat commands
└── util/
    ├── BlockScanner.java              # Tier-aware block finder
    ├── BlockUtil.java                 # Face visibility, closest side, mining time
    ├── BreakProgressTracker.java      # Static holder for current break progress
    ├── MiningMode.java                # Chat command → tier filter mapping
    ├── MithrilBlocks.java             # All target block definitions + tier system
    └── RotationHandler.java           # Smooth yaw/pitch interpolation
```

---

## How It Works

1. **INSERT pressed** → macro enables, enters `FIND_BLOCK`
2. **FIND_BLOCK** → scans 4.5-block sphere, picks highest-tier visible block matching current mode
3. **ROTATE_TO_BLOCK** → smoothly lerps yaw/pitch toward the closest exposed face of the target
4. **MINE_BLOCK** → holds left-click, monitors break progress via server packets
5. **STUCK_CHECK** → if no progress for N seconds, blacklists block and goes back to step 2
6. **COOLDOWN** → after 3 stuck events, pauses for 6 seconds before resuming

---

## Disclaimer

This mod automates gameplay actions. Use it at your own risk. The authors are not responsible for any consequences of using this mod on any server.

---

## License

MIT
