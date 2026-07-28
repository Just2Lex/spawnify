# Spawnify

Spawnify is a production-oriented spawn system for **Paper 1.21.11**. It supports per-world spawns, personal spawns, multiple named spawn points, GUI selection, delayed teleportation, cooldowns, first-join / death / void routing, PlaceholderAPI, and a public API for other plugins.

## Highlights

- Multi-world spawn storage
- Personal spawn per player per world
- Multiple named spawns with permissions
- GUI-based spawn selector with pagination
- `/spawn` command with direct teleport or selector flow
- First Join Spawn, Death Spawn, and Void Spawn flows
- Delayed teleport with countdown, optional blindness, sounds, particles, and titles
- Cooldown support with bypass permissions
- PlaceholderAPI expansion
- Public API and cancellable events for integration
- UTF-8 safe configuration with English defaults

## Requirements

- Minecraft Paper **1.21.11**
- **Java 21**
- Optional: **PlaceholderAPI**

## Project structure

- `api/` — public API consumed by other plugins
- `core/` — shared configuration, storage, services, and teleport logic
- `plugin/` — Paper entrypoint, commands, GUI, listeners, and PlaceholderAPI expansion

## Installation

1. Build the project with Gradle.
2. Copy the generated plugin JAR into your server `plugins/` folder.
3. Start the server once to generate `config.yml` and `messages.yml`.
4. Edit the configuration to fit your server.
5. Restart or reload the plugin with `/spawnify reload`.

## Commands

### Player command

`/spawn [help|list|gui|menu|personal|<spawn>]`

Aliases: `/sp`

Examples:
- `/spawn` — open the selector or teleport directly depending on configuration
- `/spawn list` — open the GUI selector
- `/spawn first-join` — teleport directly if allowed
- `/spawn world:spawn` — direct target selection
- `/spawn personal set` — save a personal spawn at your current position
- `/spawn personal clear` — remove your personal spawn for the current world

### Admin command

`/spawnify <subcommand>`

Common subcommands:
- `reload`
- `list [world]`
- `create <id> [permission]`
- `delete <id> [world]`
- `world [world]`
- `firstjoin`
- `death`
- `personal set|clear <player> [world]`

## Permissions

| Permission | Default | Description |
|---|---:|---|
| `spawnify.use` | `true` | Allows using `/spawn` |
| `spawnify.personal.set` | `true` | Allows saving a personal spawn |
| `spawnify.personal.clear` | `true` | Allows removing a personal spawn |
| `spawnify.admin` | `op` | Grants access to admin commands |
| `spawnify.bypass.cooldown` | `op` | Bypasses teleport cooldowns |
| `spawnify.bypass.delay` | `op` | Bypasses delayed teleports |

## Configuration files

### `config.yml`

Controls spawn selection behaviour, GUI layout, teleport delay, cooldowns, title/subtitle presentation, sound, particles, blindness, join flows, death flows, and void handling.

### `messages.yml`

Contains all player-facing messages and GUI labels. The shipped defaults are **English**.

## PlaceholderAPI placeholders

Install PlaceholderAPI to use the `spawnify` expansion.

Useful placeholders include:

- `%spawnify_cooldown%`
- `%spawnify_cooldown_remaining%`
- `%spawnify_cooldown_active%`
- `%spawnify_cooldown_formatted%`
- `%spawnify_countdown_remaining%`
- `%spawnify_teleport_remaining%`
- `%spawnify_countdown_formatted%`
- `%spawnify_teleport_formatted%`
- `%spawnify_teleport_active%`
- `%spawnify_available%`
- `%spawnify_available_count%`
- `%spawnify_world%`
- `%spawnify_has_personal%`
- `%spawnify_personal_world%`
- `%spawnify_spawn%`
- `%spawnify_selected_spawn_id%`
- `%spawnify_selected_spawn_type%`
- `%spawnify_selected_spawn_world%`
- `%spawnify_selected_spawn_permission%`
- `%spawnify_selected_spawn_coords%`
- `%spawnify_selected_spawn_x%`
- `%spawnify_selected_spawn_y%`
- `%spawnify_selected_spawn_z%`
- `%spawnify_selected_spawn_yaw%`
- `%spawnify_selected_spawn_pitch%`
- `%spawnify_selected_spawn_icon%`
- `%spawnify_selected_spawn_slot%`
- `%spawnify_selected_spawn_enabled%`
- `%spawnify_spawn_names%`
- `%spawnify_first_join_spawn%`
- `%spawnify_death_spawn%`
- `%spawnify_void_enabled%`
- `%spawnify_void_threshold%`

## API and events

Spawnify exposes a public API and cancellable events for other plugins. The event flow covers spawn resolving, selection, countdown, teleport start, teleport completion, command handling, joins, deaths, and void routing.

## Build from source

```bash
./gradlew clean build
```

Build outputs are written to the module-specific build directories configured in Gradle.

## Notes

- The plugin is designed for Paper, not Spigot.
- The project uses Java 21.
- Reloads are supported, but a full server restart is still the safest option after major configuration changes.
