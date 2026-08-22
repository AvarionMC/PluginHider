# PluginHider

Hide your Minecraft plugins from prying eyes. The plugin commands can still be executed, but they won't show up in tab
completion, `/version <plugin name>` command results, or when players use the `/plugins` command.

## Requirements

- **Minecraft server:** **Paper 1.21.11** (or a compatible Paper build). PluginHider reproduces
  Paper's own `/plugins` and `/version` output so hidden plugins are indistinguishable from ones
  that aren't installed; it is Paper-only and no longer supports Spigot.
- **Java:** 21 or later.
- **PacketEvents:** [a build recent enough to support your server version](https://www.spigotmc.org/resources/packetevents-api.80279/) (PluginHider is built against ***v2.13.0***; older builds may not know your protocol version).

## Configuration

Configuration is managed through the `config.yml` file:

```yaml
# List of plugins to hide from players. Use '*' to hide all plugins.
# IMPORTANT: Default Minecraft/Bukkit commands remain visible even with '*' unless explicitly hidden.
hide_plugins:
   - PluginHider
   - ProtocolLib
   - packetevents

# List of plugins to show, even if they would otherwise be hidden.
# Takes priority over hide_plugins.
show_plugins:
   - 'MySuperCoolPlugin'

# Controls whether plugin commands can be tab-completed with the plugin name prefix.
# Example: When true, commands can be completed as "pluginname:command"
# When false, only the command name without the plugin prefix will be shown.
should_allow_colon_tabcompletion: false

# Per-player visibility, on top of the global rules above. Maps a player UUID to what
# that player may additionally see (tab-completion, /plugins, /version, /help).
#   - "*"           -> that player sees everything.
#   - a plugin list -> that player also sees just those plugins (and their commands).
# Operators get no special treatment; only the server console and the UUIDs listed here
# ever see more than a normal player.
player_plugins:
   00000000-0000-0000-0000-000000000000: "*"
   11111111-1111-1111-1111-111111111111:
      - Essentials
      - WorldEdit
```

### Bukkit and Minecraft Commands Visibility

**Important Note:** When using `'*'` in `hide_plugins`, all plugin commands will be hidden **EXCEPT** for default
Minecraft and Bukkit commands.

To explicitly hide Minecraft and Bukkit commands as well:

```yaml
hide_plugins:
   - '*'
   - minecraft
   - bukkit
```

This will make virtually all commands invisible to non-privileged players.

### Configuration Guide

#### Basic Concepts:
- `hide_plugins`: List of plugins to hide
- `show_plugins`: List of plugins to explicitly show
- `'*'` in `hide_plugins`: Hides all plugins not listed in `show_plugins`
- `show_plugins` takes priority over `hide_plugins`

#### Example Scenarios

Let's say you have these plugins installed:
- pluginA
- pluginB
- pluginC
- pluginD
- pluginE

1. **Hide a Single Plugin**  
   To hide only pluginB:
   ```yaml
   hide_plugins:
     - pluginB
   ```
   Result: All plugins except pluginB will be visible

2. **Show Only One Plugin**  
   To show only pluginB:
   ```yaml
   hide_plugins:
     - '*'
   show_plugins:
     - pluginB
   ```
   Result: Only pluginB and default Minecraft/Bukkit commands will be visible

3. **Hide Multiple Specific Plugins**  
   To hide pluginB and pluginD:
   ```yaml
   hide_plugins:
     - pluginB
     - pluginD
   ```
   Result: All plugins except pluginB and pluginD will be visible

4. **Show Only Selected Plugins**  
   To show only pluginA and pluginC:
   ```yaml
   hide_plugins:
     - '*'
   show_plugins:
     - pluginA
     - pluginC
   ```
   Result: Only pluginA, pluginC, and default Minecraft/Bukkit commands will be visible

5. **Hide All Plugins**  
   To hide all plugins except Minecraft/Bukkit commands:
   ```yaml
   hide_plugins:
     - '*'
   ```
   Result: Only default Minecraft/Bukkit commands will be visible

6. **Hide Everything Including Minecraft/Bukkit**  
   To hide absolutely everything:
   ```yaml
   hide_plugins:
     - '*'
     - minecraft
     - bukkit
   ```
   Result: No commands will be visible at all

### Player Permission Control

By default only the **server console** sees the full, unfiltered list. Everyone else — **operators
included** — gets the filtered view. This is deliberate: op status is easy to grant and easy to
abuse, so it must never become a way to enumerate the hidden plugins.

To give specific people more, list their UUID under **`player_plugins`**:

```yaml
player_plugins:
   # This player sees everything (the equivalent of the old whitelist).
   00000000-0000-0000-0000-000000000000: "*"
   # This player additionally sees just Essentials and WorldEdit — in tab-completion,
   # /plugins, /version and /help — even though everything is hidden globally.
   11111111-1111-1111-1111-111111111111:
      - Essentials
      - WorldEdit
```

This grants **visibility**, not permission: whether a player may actually *run* a command is still
governed by Bukkit permissions. `player_plugins` only controls what shows up for them.

A handy pattern is to hide everything and reveal per-person:

```yaml
hide_plugins:
   - '*'
player_plugins:
   <moderator-uuid>:
      - Essentials
```

### Tab Completion Format

- `should_allow_colon_tabcompletion`: Controls whether plugin commands can be tab-completed with the plugin name
  prefix (e.g., "pluginname:command")

## Commands

- `/pluginhider help` - Shows available commands
- `/pluginhider reload` - Reloads the configuration from disk

## Showcase

![Short explanation](docs/short_explanation.png)

## Usage

![Server usage](https://bstats.org/signatures/bukkit/PluginHider.svg)

## Links

- **Spigot**: https://www.spigotmc.org/resources/plugin-hider.117705/
- **bStats**: https://bstats.org/plugin/bukkit/PluginHider/22462
- **GitHub**: https://github.com/AvarionMC/PluginHider