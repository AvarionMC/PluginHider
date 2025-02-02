# 1.1.2

* An op could see everything unfiltered. Unless the op is explicitly mentioned in `uuids_to_explicitly_disallow`. (or `operator_can_see_everything` is false).
* By default `minecraft:*` & `bukkit:*` will be shown to everyone. Unless explicitly mentioned inside the `hide_plugins` section.

# 1.1.1

* Fix a bug where the cache wasn't updated on reload.

# 1.1.0

* minecraft 1.20.5+ support thanks to PacketEvents

# 1.0.0

First release