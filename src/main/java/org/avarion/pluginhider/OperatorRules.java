package org.avarion.pluginhider;

import java.util.Set;
import java.util.UUID;

public record OperatorRules(
        boolean canSeeEverything,
        Set<UUID> whitelist,
        Set<UUID> blacklist
) {
    public static final OperatorRules DEFAULT = new OperatorRules(false, Set.of(), Set.of());
}
