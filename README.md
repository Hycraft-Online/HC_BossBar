# HC_BossBar

Dark Souls-style boss health bar HUD for tracked enemy entities. When a boss is marked via the API or command, a health bar with trailing damage indicator appears at the top of the player's screen. The bar updates in real-time by polling the boss entity's health stat and displays a "Boss Defeated" overlay when the boss dies.

## Features

- Dark Souls-inspired boss health bar HUD with boss name display
- Trailing damage indicator that smoothly follows actual health loss
- Real-time health polling at 250ms intervals via the entity stat system
- "Boss Defeated" overlay with timed fade-out sequence
- Per-player boss tracking (each player can track a different boss)
- Automatic cleanup when the boss entity dies or becomes invalid
- `/boss` command for marking entities as bosses
- Programmatic API for other plugins to trigger boss bars (`markBoss`, `clearBoss`, `showBossDefeated`)

## Dependencies

- **EntityModule** (required) -- Hytale entity system
- **HC_MultiHud** (optional) -- allows displaying alongside other custom HUDs

## Building

```bash
./gradlew build
```
