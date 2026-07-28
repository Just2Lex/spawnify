# Spawnify Wiki

This wiki documents the current architecture, commands, configuration model, runtime behaviour, placeholders, and integration points of Spawnify.

## Table of contents

1. Overview
2. Core concepts
3. Installation
4. First launch and generated files
5. Command reference
6. Permission reference
7. Configuration reference
8. Messages reference
9. PlaceholderAPI reference
10. API and events
11. Storage model
12. Teleport flow
13. GUI behaviour
14. Reload and lifecycle behaviour
15. Troubleshooting
16. Upgrade notes

---

## 1. Overview

Spawnify is a spawn management plugin for Paper 1.21.11. It is designed to be practical for live servers and flexible enough for more advanced spawn routing.

The plugin supports:

- one world spawn per world
- multiple named spawns per world
- personal spawns per player per world
- first-join spawn handling
- death spawn handling
- void routing with a configurable Y threshold
- delayed teleports with countdown presentation
- cooldown tracking and bypass permissions
- GUI-based spawn selection
- PlaceholderAPI integration
- a public event-driven API

Spawnify is split into three Gradle modules:

- `api` — public types for external plugins
- `core` — shared models, storage, config, and teleport logic
- `plugin` — Paper plugin entrypoint, commands, GUI, listeners, and PAPI expansion

---

## 2. Core concepts

### Spawn point
A spawn point is a stored location with at least:

- world name
- X, Y, Z coordinates
- yaw and pitch
- type
- identifier
- optional permission
- optional icon
- optional GUI slot
- enabled flag

### Spawn target
A target is a spawn option the player can actually use at runtime. Targets include:

- world default spawns
- named spawns
- personal spawns
- special spawns such as first-join and death spawns

### Personal spawn
A personal spawn is stored per:

- player UUID
- world name

That means the same player may have different personal spawns in different worlds.

### Special spawns
Spawnify reserves dedicated spawn flows for:

- `first-join`
- `death`
- `void`

These are handled separately from regular spawn selection.

---

## 3. Installation

1. Build the plugin with Gradle.
2. Copy the generated JAR into your server `plugins/` directory.
3. Start the server once.
4. Edit `config.yml` and `messages.yml`.
5. Reload with `/spawnify reload` or restart the server.

Recommended dependency:

- PlaceholderAPI if you want placeholders in scoreboards, menus, or HUD plugins.

---

## 4. First launch and generated files

On first start, the plugin creates:

- `config.yml`
- `messages.yml`

Both files ship with English defaults.

### Why two files?

- `config.yml` controls behaviour
- `messages.yml` controls player-facing text and GUI labels

This split keeps logic and content separate, which makes maintenance easier.

---

## 5. Command reference

## Player command: `/spawn`

### Syntax

```text
/spawn [help|list|gui|menu|personal|<spawn>]
```

Aliases:

- `/sp`

### Behaviour

Without arguments, `/spawn` will either:

- teleport directly to the only available target, or
- open the selector GUI, depending on configuration and available targets

### Subcommands

- `help` — show usage hints
- `list` / `gui` / `menu` — open the GUI selector
- `personal set` — save a personal spawn at your current location
- `personal clear` — remove a personal spawn for the current world
- `<spawn>` — direct target selection when identifier selection is enabled

### Examples

```text
/spawn
/spawn list
/spawn personal set
/spawn personal clear
/spawn world:spawn
```

---

## Admin command: `/spawnify`

### Syntax

```text
/spawnify <subcommand>
```

### Subcommands

#### `/spawnify reload`
Reload configuration, messages, and storage state.

#### `/spawnify list [world]`
Show all registered spawns. When a world is supplied, only spawns in that world are shown.

#### `/spawnify create <id> [permission]`
Create a named spawn at the admin’s current position.

#### `/spawnify delete <id> [world]`
Delete a named spawn. When the same id exists in multiple worlds, specifying a world removes ambiguity.

#### `/spawnify world [world]`
Set the default world spawn for the admin’s current position, or for a specific world if provided.

#### `/spawnify firstjoin`
Set the First Join Spawn at the admin’s current position.

#### `/spawnify death`
Set the Death Spawn at the admin’s current position.

#### `/spawnify personal set <player> [world]`
Set a personal spawn for another player.

#### `/spawnify personal clear <player> [world]`
Remove a personal spawn for another player.

---

## 6. Permission reference

| Permission | Default | Purpose |
|---|---:|---|
| `spawnify.use` | `true` | Allows use of `/spawn` |
| `spawnify.personal.set` | `true` | Allows saving personal spawns |
| `spawnify.personal.clear` | `true` | Allows clearing personal spawns |
| `spawnify.admin` | `op` | Allows access to `/spawnify` |
| `spawnify.bypass.cooldown` | `op` | Bypasses teleport cooldowns |
| `spawnify.bypass.delay` | `op` | Bypasses teleport delays |

The admin permission also grants the individual admin child permissions.

---

## 7. Configuration reference

This section explains every important config group in the shipped `config.yml`.

### 7.1 `settings.gui`

Controls the spawn selector GUI.

- `rows` — inventory rows, clamped between 2 and 6
- `title` — GUI title shown to players
- `filler-enabled` — fills unused slots with a background item
- `filler-material` — material used for filler slots

#### Practical notes

- Use 6 rows for large servers.
- Use a simple filler material to keep the menu readable.
- Keep the title short so it looks good on smaller clients.

---

### 7.2 `settings.personal`

- `enabled` — enables personal spawns

When disabled, personal spawn commands and personal placeholder values become irrelevant.

---

### 7.3 `settings.selection`

Controls what happens when a player opens `/spawn`.

- `open-gui-when-multiple` — open the GUI if more than one spawn is available
- `direct-teleport-when-single` — instantly teleport if only one target exists
- `multiple-behavior` — runtime selection strategy when many targets exist
- `single-behavior` — runtime selection strategy when one target exists
- `allow-identifier-argument` — allows `/spawn <id>` style selection
- `default-world-spawn-fallback` — allows fallback to world spawn when needed

#### Behavioural impact

These settings determine whether `/spawn` behaves like:

- a quick teleport command
- a menu command
- or a hybrid of both

---

### 7.4 `settings.teleport`

Controls the global teleport flow.

- `delay-seconds` — delay before teleport starts
- `cooldown-seconds` — cooldown applied after teleport
- `use-blindness` — applies blindness during waiting if enabled by the flow
- `preserve-saved-orientation` — keeps the stored yaw and pitch
- `force-saved-orientation` — forces saved orientation on teleport
- `cancel-on-move` — cancels the countdown if the player moves

#### Presentation group

`settings.teleport.presentation` controls the global teleport presentation:

- `message` — chat message before teleport
- `completion` — chat message after teleport
- `title` — title/subtitle shown during the flow
- `effects.sound` — optional sound effect
- `effects.particles` — optional particle effect
- `blindness` — optional blindness effect

#### Countdown group

`settings.teleport.countdown` controls the waiting-time UI.

- `title.enabled` — whether the countdown title appears
- `title.text` — title text shown during the countdown
- `subtitle.enabled` — whether the countdown subtitle appears
- `subtitle.text` — subtitle text, usually containing `%seconds%`
- `subtitle.message-key` — message key for a custom subtitle source

#### Practical notes

- Use the countdown title for the waiting state.
- Use the completion title for the final teleport feedback.
- Keep the countdown subtitle simple and readable.
- Use blindness sparingly; it can be useful for immersive servers, but not every server needs it.

---

### 7.5 `settings.connection`

Controls welcome titles on join.

- `title.enabled` — global connection title toggle
- `title.first-join-enabled` — apply connection title on first join
- `title.repeat-join-enabled` — apply connection title on repeat join
- `title.text` — title text
- `title.subtitle` — subtitle text
- `fade-in`, `stay`, `fade-out` — title timing values

#### Behavioural notes

Connection titles are separate from teleport titles. That keeps the join experience from clashing with the teleport countdown.

---

### 7.6 `settings.join.first-join`

Controls the first-time player flow.

- `enabled` — enables first-join logic
- `spawn-id` — target spawn identifier, usually `first-join`
- `target-mode` — target resolution mode
- `teleport-delay-seconds` — delay before teleport on first join
- `apply-cooldown` — whether to apply cooldown after first-join teleport
- `cooldown-seconds` — cooldown duration for this flow
- `fallback-to-world-spawn` — fallback when the configured spawn is unavailable
- `presentation` — presentation block specific to first join

#### Important behaviour

The first-join flow should only happen once per player. After that, the repeat-join flow or normal spawn behaviour applies.

---

### 7.7 `settings.join.repeat-join`

Controls repeat join behaviour.

- `enabled` — enables repeat join logic
- `teleport-to-world-spawn` — legacy-style fallback route
- `target-mode` — target resolution mode
- `spawn-id` — specific spawn identifier when used
- `teleport-delay-seconds` — delay before teleport on repeat join
- `apply-cooldown` — whether to apply cooldown after repeat join teleport
- `cooldown-seconds` — cooldown duration for this flow
- `fallback-to-world-spawn` — fallback when the configured spawn is unavailable
- `presentation` — presentation block specific to repeat joins

#### Practical notes

This flow is useful for servers that always want players to return to a specific world spawn when they rejoin.

---

### 7.8 `settings.death`

Controls respawn routing.

- `enabled` — enables death handling
- `respawn-delay-seconds` — delay used before respawn flow continues
- `teleport-delay-seconds` — delay before teleport after respawn
- `spawn-id` — preferred death spawn identifier
- `apply-cooldown` — whether to apply cooldown
- `cooldown-seconds` — cooldown duration for death flow
- `fallback-to-world-spawn` — fallback to world spawn if needed
- `presentation` — presentation block for death routing

#### Practical notes

The death flow is separate from first join and normal `/spawn` logic.

---

### 7.9 `settings.void`

Controls void protection.

- `enabled` — enables void detection
- `y-threshold` — Y level at or below which the void flow triggers
- `teleport-delay-seconds` — delay before void teleport
- `teleport-cooldown-seconds` — cooldown used by the void flow
- `apply-cooldown` — whether the void flow should apply cooldown
- `presentation` — presentation block for void routing

#### Practical notes

A void threshold is useful on custom maps, skyblock servers, and minigame worlds where the world bottom is not always the correct trigger point.

---

## 8. Messages reference

The shipped `messages.yml` contains all visible text and GUI labels.

### General keys

- `prefix`
- `no-permission`
- `player-only`
- `player-not-found`
- `spawn-not-found`
- `spawn-available-none`
- `spawn-available-one`
- `spawn-available-multiple`
- `spawn-unknown`
- `spawn-ambiguous`
- `spawn-ambiguous-delete`
- `spawn-set`
- `spawn-removed`
- `world-spawn-set`
- `first-join-set`
- `death-spawn-set`
- `personal-set`
- `personal-cleared`
- `reloaded`
- `admin-help-title`
- `spawn-help-title`
- `spawn-item-selected`
- `admin-help-lines`
- `spawn-help-lines`
- `teleport.*`
- `join.*`
- `death.*`
- `void.*`
- `gui-title`
- `gui-next`
- `gui-prev`
- `gui-close`
- `spawn-item-lore`

### Formatting

The messages use legacy color codes. You can safely use:

- `&a`, `&c`, `&e`, `&7`, etc.
- placeholders such as `%spawn%`, `%player%`, `%world%`, `%type%`, `%permission%`, and `%seconds%`

---

## 9. PlaceholderAPI reference

Spawnify registers the placeholder identifier `spawnify`.

### Cooldown and countdown

- `%spawnify_cooldown%`
- `%spawnify_cooldown_remaining%`
- `%spawnify_cooldown_active%`
- `%spawnify_cooldown_formatted%`
- `%spawnify_countdown_remaining%`
- `%spawnify_teleport_remaining%`
- `%spawnify_countdown_formatted%`
- `%spawnify_teleport_formatted%`
- `%spawnify_teleport_active%`

### Selection and state

- `%spawnify_available%`
- `%spawnify_available_count%`
- `%spawnify_world%`
- `%spawnify_has_personal%`
- `%spawnify_personal_world%`
- `%spawnify_spawn%`
- `%spawnify_selected_spawn%`
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

### Formatting notes

- Numeric placeholders return plain numbers.
- `*_formatted` placeholders return `MM:SS` values.
- `selected_spawn` falls back to a useful value when no explicit selection exists.

---

## 10. API and events

Spawnify exposes public API classes in the `api` module and event classes for integration.

### Main use cases

Other plugins can:

- observe spawn resolution
- cancel a teleport
- replace a target before teleport
- react to countdown start/tick/end
- react to joins, deaths, commands, and void routing

### Event flow

A typical teleport flow looks like this:

1. A spawn target is selected or resolved.
2. A command or lifecycle event triggers the request.
3. `SpawnSelectionEvent` or `SpawnResolveEvent` may be fired.
4. `SpawnCountdownStartEvent` is fired if a delay exists.
5. `SpawnCountdownTickEvent` fires during the countdown.
6. `SpawnTeleportEvent` fires before the final teleport.
7. `SpawnTeleportedEvent` fires after the teleport completes.

### Cancellable hooks

Where it makes sense, events are cancellable so external plugins can stop or redirect the flow.

---

## 11. Storage model

Spawnify stores data by world and by player.

### Stored data types

- world default spawn per world
- named spawns per world
- personal spawn per player per world
- first-join spawn
- death spawn

### Why this model works

This layout keeps the project extensible:

- new worlds do not break existing data
- new named spawns can be added without changing the storage schema
- personal spawns remain isolated from global spawns

---

## 12. Teleport flow

### Normal `/spawn`

1. Player runs `/spawn`.
2. Available targets are collected.
3. GUI or direct teleport is selected based on configuration.
4. A countdown can start.
5. Cooldown is applied if configured.
6. The final teleport happens.
7. Effects and messages are shown.

### First join

1. Player joins for the first time.
2. First-join flow resolves a target.
3. Optional connection title may appear.
4. A teleport request is queued or executed.
5. Completion effects are shown.

### Repeat join

1. Player joins again.
2. Repeat-join flow resolves a target if enabled.
3. Teleport happens according to the configured strategy.

### Death

1. Player dies.
2. The death event is captured.
3. Respawn routing is applied.
4. Optional teleport delay is used.
5. The player is moved to the configured death target or fallback target.

### Void

1. Player crosses the configured Y threshold.
2. A void event is triggered.
3. Teleport logic resolves a safe spawn.
4. The player is moved away from the void.

---

## 13. GUI behaviour

The spawn GUI is designed to be:

- readable
- paginated
- permission-aware
- easy to extend

### What players see

- spawn items with world, type, and permission info
- selected spawn highlighting
- page navigation buttons
- close button

### Why it matters

This gives players a clean overview of what they can actually use, instead of forcing them to guess spawn names.

---

## 14. Reload and lifecycle behaviour

### On reload

Spawnify reloads:

- `config.yml`
- `messages.yml`
- storage repositories
- active runtime references

### On disable

Spawnify clears active countdown sessions and shuts down cleanly.

### Practical recommendation

A full server restart is still the safest option for large operational changes, but the plugin is designed to be reload-safe for normal configuration edits.

---

## 15. Troubleshooting

### Players do not see English messages

Check whether an old `messages.yml` is still present in the plugin folder. If it was generated by an older version, delete it or reload after updating the plugin so the migration can update legacy stock values.

### `/spawn` opens the GUI instead of teleporting

Check the settings under `settings.selection` and verify how many spawn targets are currently available to the player.

### Countdown title is not shown

Check:

- `settings.teleport.countdown.title.enabled`
- `settings.teleport.delay-seconds`
- whether the teleport flow is being bypassed by permissions

### Void routing does not trigger

Check:

- `settings.void.enabled`
- `settings.void.y-threshold`
- the world’s spawn height and custom map layout

### PlaceholderAPI placeholders are empty

Check that PlaceholderAPI is installed and that the expansion is registered. Also confirm that the player actually has a selected or available spawn target.

---

## 16. Upgrade notes

When upgrading from older builds:

- review `config.yml`
- review `messages.yml`
- keep an eye on spawn identifiers
- verify per-world named spawns
- test first-join and death flows on a staging server before production rollout

If you already had an older Russian configuration, this release ships with English defaults and migration helpers for the most common legacy stock values.
