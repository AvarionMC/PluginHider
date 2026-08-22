package org.avarion.pluginhider;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettingsAccessTest {
    /** A minimal Player stub — the access checks only ever ask for the UUID. */
    private static Player playerWithId(UUID id) {
        return (Player) Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[]{Player.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getUniqueId" -> id;
                    case "toString" -> "StubPlayer";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );
    }

    @Test
    void onlyStarGrantSeesEverything() {
        Settings settings = new Settings();
        UUID everything = UUID.randomUUID();
        UUID scoped = UUID.randomUUID();
        UUID nobody = UUID.randomUUID();
        settings.grants = Map.of(everything, Set.of("*"), scoped, Set.of("essentials", "worldedit"));

        // "*" grant (and only that) sees everything — an op that isn't listed does not.
        assertTrue(settings.canSeeEverything(playerWithId(everything)));
        assertFalse(settings.canSeeEverything(playerWithId(scoped)), "a scoped grant is not \"see everything\"");
        assertFalse(settings.canSeeEverything(playerWithId(nobody)));
        assertFalse(settings.canSeeEverything(null));
    }

    @Test
    void grantsForReturnsThePlayersPlugins() {
        Settings settings = new Settings();
        UUID scoped = UUID.randomUUID();
        settings.grants = Map.of(scoped, Set.of("essentials", "worldedit"));

        assertEquals(Set.of("essentials", "worldedit"), settings.grantsFor(scoped));
        assertEquals(Set.of(), settings.grantsFor(UUID.randomUUID()));
        assertEquals(Set.of(), settings.grantsFor(null));
    }
}
