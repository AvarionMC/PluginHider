package org.avarion.pluginhider;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettingsAccessTest {
    /** A minimal Player stub — canSeeEverything only ever asks for the UUID. */
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
    void onlyWhitelistedPlayersSeeEverything() {
        Settings settings = new Settings();
        UUID whitelisted = UUID.randomUUID();
        UUID someoneElse = UUID.randomUUID();
        settings.whitelist = Set.of(whitelisted);

        assertTrue(settings.canSeeEverything(playerWithId(whitelisted)),
                "a whitelisted player must see everything");
        // A non-whitelisted player — an operator included — must NOT: op status grants nothing here.
        assertFalse(settings.canSeeEverything(playerWithId(someoneElse)),
                "a non-whitelisted player (op or not) must not see everything");
        assertFalse(settings.canSeeEverything(null), "null must be safe and denied");
    }
}
