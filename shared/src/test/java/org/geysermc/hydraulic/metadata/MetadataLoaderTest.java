package org.geysermc.hydraulic.metadata;

import net.minecraft.resources.Identifier;
import org.geysermc.hydraulic.compat.MappingOwnership;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MetadataLoaderTest {
    @Test
    void loadsRecursivelyAndOrdersRulesByPriorityAndSpecificity(@TempDir Path tempDir) throws IOException {
        Files.createDirectories(tempDir.resolve("builtin"));
        Files.writeString(tempDir.resolve("builtin/base.json"), """
            {
              "blocks": [
                {
                  "java_id": "example:test_block",
                  "rules": [
                    {
                      "java_when": {
                        "facing": "north"
                      },
                      "geometry": "example:north"
                    },
                    {
                      "geometry": "example:default"
                    }
                  ]
                }
              ]
            }
            """);

        Files.createDirectories(tempDir.resolve("user"));
        Files.writeString(tempDir.resolve("user/override.json"), """
            {
              "java_id": "example:test_block",
              "rules": [
                {
                  "geometry": "example:user"
                }
              ]
            }
            """);

        MetadataIndex index = new MetadataLoader(LoggerFactory.getLogger("MetadataLoaderTest")).load(tempDir);

        BlockMapping mapping = index.blockMapping(Identifier.fromNamespaceAndPath("example", "test_block"));
        assertNotNull(mapping);
        assertEquals(3, mapping.rules().size());
        assertEquals("example:user", mapping.rules().get(0).geometryId());
        assertEquals(MappingOwnership.USER, mapping.rules().get(0).ownership());
        assertEquals("example:north", mapping.rules().get(1).geometryId());
        assertEquals(1, mapping.rules().get(1).specificity());
        assertEquals("example:default", mapping.rules().get(2).geometryId());
        assertEquals(2, index.summary().fileCount());
        assertEquals(2, index.summary().blockMappingCount());
        assertEquals(0, index.summary().itemMappingCount());
        assertEquals(0, index.summary().recipeMappingCount());
        assertEquals(3, index.summary().ruleCount());
        assertEquals(Map.of("builtin", 1, "user", 1), index.summary().ownershipFileCounts());
    }

    @Test
    void supportsLegacySingleMappingFiles(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("legacy.json"), """
            {
              "java_id": "example:legacy_block",
              "rules": [
                {
                  "material": "example:block/legacy"
                }
              ]
            }
            """);

        MetadataIndex index = new MetadataLoader(LoggerFactory.getLogger("MetadataLoaderTest")).load(tempDir);

        BlockMapping mapping = index.blockMapping(Identifier.fromNamespaceAndPath("example", "legacy_block"));
        assertNotNull(mapping);
        assertEquals(1, mapping.rules().size());
        assertEquals(MappingOwnership.LEGACY, mapping.rules().get(0).ownership());
        assertEquals("legacy.json", mapping.rules().get(0).sourcePath());
        assertEquals(1, index.summary().fileCount());
        assertEquals(1, index.summary().blockMappingCount());
        assertEquals(0, index.summary().itemMappingCount());
        assertEquals(0, index.summary().recipeMappingCount());
        assertEquals(1, index.summary().ruleCount());
        assertEquals(Map.of("legacy", 1), index.summary().ownershipFileCounts());
    }

    @Test
    void loadsItemAndRecipeMappings(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("compat.json"), """
            {
              "items": [
                {
                  "java_id": "example:test_item",
                  "bedrock_identifier": "example:bedrock_item"
                }
              ],
              "recipes": [
                {
                  "java_id": "example:test_recipe",
                  "bedrock_identifier": "example:bedrock_recipe"
                }
              ]
            }
            """);

        MetadataIndex index = new MetadataLoader(LoggerFactory.getLogger("MetadataLoaderTest")).load(tempDir);

        assertNotNull(index.itemMapping(Identifier.fromNamespaceAndPath("example", "test_item")));
        assertNotNull(index.recipeMapping(Identifier.fromNamespaceAndPath("example", "test_recipe")));
        assertEquals(1, index.summary().itemMappingCount());
        assertEquals(1, index.summary().recipeMappingCount());
    }
}