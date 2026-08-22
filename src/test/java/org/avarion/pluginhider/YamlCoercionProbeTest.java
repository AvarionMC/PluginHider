package org.avarion.pluginhider;

import org.avarion.yaml.Leniency;
import org.avarion.yaml.YamlFile;
import org.avarion.yaml.YamlFileInterface;
import org.avarion.yaml.YamlKey;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies the two things the player_plugins design leans on org.avarion:yaml for: parsing UUID map
 * keys directly (no manual conversion) and leniently coercing a scalar {@code "*"} value into a
 * single-element set.
 */
class YamlCoercionProbeTest {
    @YamlFile(lenient = Leniency.LENIENT)
    public static class ProbeConfig extends YamlFileInterface {
        @YamlKey("player_plugins")
        public Map<UUID, Set<String>> playerPlugins = Map.of();
    }

    @Test
    void parsesUuidKeysAndCoercesScalarStar(@TempDir Path dir) throws Exception {
        UUID everything = UUID.fromString("00000000-0000-0000-0000-000000000000");
        UUID scoped = UUID.fromString("11111111-1111-1111-1111-111111111111");

        File f = dir.resolve("probe.yml").toFile();
        Files.writeString(f.toPath(), """
                player_plugins:
                  00000000-0000-0000-0000-000000000000: "*"
                  11111111-1111-1111-1111-111111111111:
                    - Essentials
                    - WorldEdit
                """);

        ProbeConfig c = new ProbeConfig();
        c.load(f);

        assertEquals(Set.of("*"), c.playerPlugins.get(everything), "scalar \"*\" should coerce to [\"*\"]");
        assertEquals(Set.of("Essentials", "WorldEdit"), c.playerPlugins.get(scoped));
    }
}
