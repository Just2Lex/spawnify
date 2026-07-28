# ✨ Spawnify

**Spawnify** is a clean and production-ready spawn plugin for **Paper 1.21.11**. It gives you multi-world spawn handling, GUI selection, personal spawns, delayed teleports, cooldowns, and powerful hooks for other plugins.

## 🚀 Features

- 🌍 **Multi-world support** — separate spawn data for every world
- 🧍 **Personal spawns** — each player can have their own spawn per world
- 📍 **Multiple named spawns** — permission-based spawn points
- 🪟 **GUI selector** — paginated, tidy, and easy to extend
- ⏳ **Teleport countdown** — delay, subtitle countdown, optional blindness
- 🔔 **Effects** — titles, chat messages, particles, and sounds
- 🛡️ **Cooldowns** — configurable and bypassable via permissions
- 🧩 **PlaceholderAPI support** — ready for scoreboards, menus, and HUDs
- 🛠️ **Public API** — built for integration with other plugins

## 📦 Requirements

- **Paper 1.21.11**
- **Java 21**
- Optional: **PlaceholderAPI**

## ✅ Commands

### Player
`/spawn [help|list|gui|menu|personal|<spawn>]`

Examples:
- `/spawn` — open the selector or teleport directly
- `/spawn list` — open the spawn GUI
- `/spawn personal set` — save your personal spawn
- `/spawn first-join` — direct spawn selection when available

### Admin
`/spawnify <subcommand>`

Available subcommands:
- `reload`
- `list [world]`
- `create <id> [permission]`
- `delete <id> [world]`
- `world [world]`
- `firstjoin`
- `death`
- `personal set|clear <player> [world]`

## 🔑 Permissions

- `spawnify.use`
- `spawnify.personal.set`
- `spawnify.personal.clear`
- `spawnify.admin`
- `spawnify.bypass.cooldown`
- `spawnify.bypass.delay`

## 🎨 Configuration

Spawnify ships with **English defaults** in both `config.yml` and `messages.yml`.

You can customize:

- GUI title and pagination
- Spawn selection logic
- Teleport delay and cooldown
- Join / death / void routing
- Title, subtitle, sound, particle, and blindness presentation
- Message text and color formatting

## 🧠 PlaceholderAPI

Useful placeholders include cooldown, countdown, selected spawn, personal spawn state, and void settings. This makes Spawnify easy to connect to scoreboards, menus, and companion plugins.

## 🛠️ Build

```bash
./gradlew clean build
```

## 💡 Tip

For the best player experience, keep GUI labels short, use permissions per spawn, and tune the teleport countdown to match your server pacing.
