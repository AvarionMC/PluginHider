# PluginHider

Hide your Minecraft plugins from prying eyes. The plugin commands can still be executed, but they won't show up in tab
completion, `/version <plugin name>` command results, or when players use the `/plugins` command.

## Required:

! [***PacketEvents v2.4.0+***](https://www.spigotmc.org/resources/packetevents-api.80279/) !

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

# When true, server operators (ops) can see all plugin commands regardless of hide/show settings.
# Set to false if you want hiding rules to apply to operators as well.
operator_can_see_everything: false

# List of player UUIDs that should see all commands, even when not operators
# or when operator_can_see_everything is false.
whitelisted_uuids:
   - 01234567-89ab-cdef-0123-456789abcdef

# List of player UUIDs that should always be treated as normal users, even when
# they're operators and operator_can_see_everything is true.
blacklisted_uuids:
   - fedcba98-7654-3210-fedc-ba9876543210
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

PluginHider provides fine-grained control over which players can see all plugins:

- **Operator Control**:
   - `operator_can_see_everything`: When true, server operators can see all plugins regardless of hide/show settings

- **Whitelist/Blacklist System**:
   - `whitelisted_uuids`: Players who can see all plugins, even when not operators or when `operator_can_see_everything`
     is false
   - `blacklisted_uuids`: Players who are treated as normal users and can't see hidden plugins, even when they're
     operators and `operator_can_see_everything` is true

This system gives you precise control over who can see what, regardless of their operator status.

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