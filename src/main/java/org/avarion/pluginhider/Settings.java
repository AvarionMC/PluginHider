package org.avarion.pluginhider;

import org.avarion.yaml.*;

import java.util.List;

@YamlFile(
        lenient = Leniency.LENIENT, header = """
        How to hide and show plugins:
        
        Let's say you have these plugins:
        - pluginA
        - pluginB
        - pluginC
        - pluginD
        - pluginE
        
        Now on to the configuration:
        
        1) I just want to hide pluginB:
          hide_plugins:
            - pluginB
        
        This means pluginA, pluginC, pluginD, and pluginE will be shown,
        but pluginB will be hidden.
        
        2) I want to only show pluginB:
          hide_plugins:
            - '*'
          show_plugins:
            - pluginB
        
        This will hide all plugins except pluginB. Only pluginB will be visible.
        Using '*' will hide all plugins initially. Then, specifying pluginB in show_plugins will make them visible.
        
        3) I want to hide multiple plugins, say pluginB and pluginD:
          hide_plugins:
            - pluginB
            - pluginD
        
        This will hide pluginB and pluginD. The rest of the plugins (pluginA, pluginC, and pluginE) will be shown.
        
        4) I want to show multiple plugins, say pluginA and pluginC:
          hide_plugins:
            - '*'
          show_plugins:
            - pluginA
            - pluginC
        
        This will hide all other plugins except pluginA and pluginC. Only pluginA and pluginC will be visible.
        
        5) I want to ensure no plugins are shown:
          hide_plugins:
            - '*'
        
        This will hide all plugins, regardless of what plugins are available.
        
        6) I want to ensure all plugins are shown:
        Just don't install this plugin 😋!
        
        Remember, the show_plugins configuration takes precedence over hide_plugins.
        If a plugin is listed in both show_plugins and hide_plugins, it will be shown.
        
        Default minecraft/bukkit commands will always be shown when you add '*' to `hide_plugins`.
        If you want to explicitly hide these, add 'minecraft' or 'bukkit' (or both) into `hide_plugins`
        """
)
public class Settings extends YamlFileInterface {
    @YamlComment("")
    @YamlKey("hide_plugins")
    public List<String> hidePlugins = List.of("PluginHider", "ProtocolLib", "packetevents");

    @YamlComment("")
    @YamlKey("show_plugins")
    public List<String> showPlugins = List.of("PluginHider", "ProtocolLib", "packetevents");

    @YamlComment(
            """
                    By default, minecraft adds "<pluginname>:" in front of all the commands and allows it.
                     However, this is not so nice. So if you set this to false, these won't be shown.
                    """
    )
    @YamlKey("should_allow_colon_tabcompletion")
    public boolean shouldAllowColonTabcompletion = false;

    @YamlComment(
            """
                    Should an operator see all commands (ie: PluginHider won't work for ops?)
                    """
    )
    @YamlKey("operator_can_see_everything")
    public boolean operatorCanSeeEverything = false;

    @YamlComment(
            """
                    This setting can be used in order to allow ops to see everything, but some ops not
                    """
    )
    @YamlKey("uuids_to_explicitly_disallow")
    public List<String> uuidsToExplicitlyDisallow = List.of();
}
