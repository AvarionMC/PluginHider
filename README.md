# PluginHider

Hide your Minecraft plugins from prying eyes. The plugin commands can still be executed, but they won't show up in tab
completion, `/version <plugin name>` command results, or when players use the `/plugins` command.

## Required:

! [***PacketEvents v2.4.0+***](https://www.spigotmc.org/resources/packetevents-api.80279/) !

## Configuration

Configuration is managed through the `config.yml` file:

```yaml
# List of plugins to hide from players. Use '*' to hide all plugins.
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

# List of operator UUIDs that could see all commands, even when operator_can_see_everything is false.
# Format: List of player UUIDs
show_everything_to_these_uuids:
   - 01234567-89ab-cdef-0123-456789abcdef
```

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
   Result: Only pluginB will be visible

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
   Result: Only pluginA and pluginC will be visible

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

### Operator Settings

PluginHider allows you to configure whether operators (and specific players) can see all plugins:

- `operator_can_see_everything`: When true, server operators can see all plugins regardless of hide/show settings
- `show_everything_to_these_uuids`: Specify UUIDs of players who should see all plugins even when
  `operator_can_see_everything` is false

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