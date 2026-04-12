# PasteNBT Kotlin (Java 21)

A high-performance Minecraft Forge mod (1.20.1) built with **Kotlin** and **Java 21 Virtual Threads** for lag-free structure placement.

## 🚀 Features

- **Async Processing:** NBT parsing and preparation run on Java 21 Virtual Threads, ensuring 0ms impact on server tick during loading.
- **Anti-Lag Placement:** Blocks are placed in configurable batches per tick to prevent server crashes.
- **Ghost Preview:** See a transparent outline of the structure before confirming placement.
- **Chunk Snapping:** Automatically aligns structures to the NW corner or center of the current chunk.
- **Web Downloader:** Download `.nbt` files directly to the server via URL.
- **Grid Placement:** Mass-paste structures in a square grid (up to 11x11 chunks).
- **Granular Permissions:** Custom config system to allow specific players or UUIDs without requiring full OP.

## 🛠 Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/paste <name> [center/corner]` | Config/OP2 | Request a structure preview at player's location. |
| `/paste grid <name> <radius> [center/corner]` | Config/OP2 | Queue mass placement in a grid. |
| `/nbtpaster get <url> [name]` | Config/OP2 | Download a structure from a direct URL. |
| `/nbtpaster reload` | OP2 | Reload the configuration file. |

**Preview Keybinds:**
- **R:** Rotate structure 90° clockwise.
- **P:** Confirm and start placement.
- **C:** Cancel preview.

## 📦 Installation

1. Place the mod JAR in your `mods/` folder.
2. Ensure the server is running **Java 21**.
3. Place your structure files in `/config/structures/*.nbt`.
4. Configure permissions in `/config/nbtpaster.json`.

## 🔨 Building from Source

This project uses **ForgeGradle 6.0** and **Kotlin for Forge**.

```bash
# Ensure you have JDK 21 installed
./gradlew build
```

The compiled JAR will be in `build/libs/pastenbt-1.0.jar`.

## ⚙️ Configuration (`nbtpaster.json`)

```json
{
  "allowedPlayers": [],
  "allowedUUIDs": [],
  "allowOps": true,
  "blocksPerTick": 100,
  "maxActiveTasks": 2,
  "maxBlocksPerTask": 200000,
  "skipAir": true
}
```

## 📜 License
MIT
