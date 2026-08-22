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

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies that org.avarion:yaml leniently coerces a scalar string value into a single-element set
 * for a {@code Map<String, Set<String>>} field — the behaviour the player_plugins design relies on.
 */
class YamlCoercionProbeTest {
    @YamlFile(lenient = Leniency.LENIENT)
    public static class ProbeConfig extends YamlFileInterface {
        @YamlKey("player_plugins")
        public Map<String, Set<String>> playerPlugins = Map.of();
    }

    @Test
    void scalarStarCoercesToSingletonSet(@TempDir Path dir) throws Exception {
        File f = dir.resolve("probe.yml").toFile();
        Files.writeString(f.toPath(), """
                player_plugins:
                  aaaa: "*"
                  bbbb:
                    - Essentials
                    - WorldEdit
                """);

        ProbeConfig c = new ProbeConfig();
        c.load(f);

        assertEquals(Set.of("*"), c.playerPlugins.get("aaaa"), "scalar \"*\" should coerce to [\"*\"]");
        assertEquals(Set.of("Essentials", "WorldEdit"), c.playerPlugins.get("bbbb"));
    }
}
