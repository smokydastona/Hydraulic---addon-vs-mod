package org.geysermc.hydraulic.metadata;

import net.minecraft.resources.Identifier;
import org.geysermc.hydraulic.compat.mapping.ContentPatch;
import org.geysermc.hydraulic.compat.MappingOwnership;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        assertEquals(0, index.summary().entityMappingCount());
        assertEquals(0, index.summary().menuMappingCount());
        assertEquals(0, index.summary().patchCount());
        assertEquals(3, index.summary().ruleCount());
        assertEquals(0, index.summary().validationIssueCount());
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
        assertEquals(0, index.summary().entityMappingCount());
        assertEquals(0, index.summary().menuMappingCount());
        assertEquals(0, index.summary().patchCount());
        assertEquals(1, index.summary().ruleCount());
        assertEquals(0, index.summary().validationIssueCount());
        assertEquals(Map.of("legacy", 1), index.summary().ownershipFileCounts());
    }

    @Test
      void loadsItemRecipeEntityAndMenuMappings(@TempDir Path tempDir) throws IOException {
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
              ],
              "entities": [
                {
                  "java_id": "example:test_entity",
                  "bedrock_identifier": "example:bedrock_entity"
                }
              ],
              "menus": [
                {
                  "java_id": "example:test_menu",
                  "bedrock_identifier": "example:bedrock_menu"
                }
              ]
            }
            """);

        MetadataIndex index = new MetadataLoader(LoggerFactory.getLogger("MetadataLoaderTest")).load(tempDir);

        assertNotNull(index.itemMapping(Identifier.fromNamespaceAndPath("example", "test_item")));
        assertNotNull(index.recipeMapping(Identifier.fromNamespaceAndPath("example", "test_recipe")));
        assertNotNull(index.entityMapping(Identifier.fromNamespaceAndPath("example", "test_entity")));
        assertNotNull(index.menuMapping(Identifier.fromNamespaceAndPath("example", "test_menu")));
        assertEquals(1, index.summary().itemMappingCount());
        assertEquals(1, index.summary().recipeMappingCount());
        assertEquals(1, index.summary().entityMappingCount());
        assertEquals(1, index.summary().menuMappingCount());
        assertEquals(0, index.summary().patchCount());
        assertEquals(0, index.summary().validationIssueCount());
    }

    @Test
    void loadsPatchMetadataAndSynthesizesMappings(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("patches.json"), """
            {
              "patches": [
                {
                  "target": "example:test_block",
                  "content_type": "block",
                  "patch": {
                    "visual": {
                      "geometry": "example:geometry.test"
                    },
                    "bedrock": {
                      "identifier": "example:test_block_bedrock",
                      "state": {
                        "variant": "default"
                      }
                    }
                  }
                },
                {
                  "target": "example:test_item",
                  "content_type": "item",
                  "patch": {
                    "bedrock": {
                      "identifier": "example:test_item_bedrock"
                    }
                  }
                }
              ]
            }
            """);

        MetadataIndex index = new MetadataLoader(LoggerFactory.getLogger("MetadataLoaderTest")).load(tempDir);

        assertEquals(2, index.summary().patchCount());
        assertEquals(0, index.summary().validationIssueCount());
        assertEquals(1, index.contentPatches(Identifier.fromNamespaceAndPath("example", "test_block")).size());
        ContentPatch blockPatch = index.contentPatches(Identifier.fromNamespaceAndPath("example", "test_block")).getFirst();
        assertEquals("example:geometry.test", blockPatch.operations().get("visual.geometry"));
        assertTrue(index.namespaces().contains("example"));
        assertEquals(List.of(Identifier.fromNamespaceAndPath("example", "test_block")), index.blockMappings("example"));
        assertEquals(List.of(Identifier.fromNamespaceAndPath("example", "test_item")), index.itemMappings("example"));
        assertEquals(2, index.contentPatches("example").size());
        assertTrue(index.contentPatches("example").containsKey(Identifier.fromNamespaceAndPath("example", "test_block")));
        assertTrue(index.contentPatches("example").containsKey(Identifier.fromNamespaceAndPath("example", "test_item")));
        assertTrue(index.hasNamespaceEntries("example"));
        assertFalse(index.hasNamespaceEntries("missing"));
        assertNotNull(index.blockMapping(Identifier.fromNamespaceAndPath("example", "test_block")));
        assertNotNull(index.itemMapping(Identifier.fromNamespaceAndPath("example", "test_item")));
    }

    @Test
    void loadsBlockEntityPatchMetadataWithoutValidationWarnings(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("block-entity-patches.json"), """
            {
              "patches": [
                {
                  "target": "example:test_block_entity",
                  "content_type": "block_entity",
                  "patch": {
                    "bedrock": {
                      "block_entity": {
                        "id": "Barrel",
                        "data": {
                          "TransferCooldown": 8,
                          "isMovable": true
                        }
                      }
                    }
                  }
                }
              ]
            }
            """);

        MetadataIndex index = new MetadataLoader(LoggerFactory.getLogger("MetadataLoaderTest")).load(tempDir);

        assertEquals(1, index.summary().patchCount());
        assertEquals(0, index.summary().validationIssueCount());
        ContentPatch patch = index.contentPatches(Identifier.fromNamespaceAndPath("example", "test_block_entity")).getFirst();
        assertEquals("Barrel", patch.operations().get("bedrock.block_entity.id"));
        assertEquals("8", patch.operations().get("bedrock.block_entity.data.TransferCooldown"));
        assertEquals("true", patch.operations().get("bedrock.block_entity.data.isMovable"));
        assertEquals("Barrel", index.blockEntityPatchTemplate(Identifier.fromNamespaceAndPath("example", "test_block_entity")).bedrockIdentifier());
        assertEquals(2, index.blockEntityPatchTemplate(Identifier.fromNamespaceAndPath("example", "test_block_entity")).mutations().size());
    }

    @Test
    void loadsMenuPatchMetadataWithoutValidationWarnings(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("menu-patches.json"), """
            {
              "patches": [
                {
                  "target": "test:barrel_menu",
                  "content_type": "menu",
                  "patch": {
                    "bedrock": {
                      "menu": {
                        "container_type": "generic_9x3"
                      }
                    }
                  }
                }
              ]
            }
            """);

        MetadataIndex index = new MetadataLoader(LoggerFactory.getLogger("MetadataLoaderTest")).load(tempDir);

        assertEquals(1, index.summary().patchCount());
        assertEquals(0, index.summary().validationIssueCount());
        ContentPatch patch = index.contentPatches(Identifier.fromNamespaceAndPath("test", "barrel_menu")).getFirst();
        assertEquals("generic_9x3", patch.operations().get("bedrock.menu.container_type"));
        assertEquals("GENERIC_9X3", index.menuPatchTemplate(Identifier.fromNamespaceAndPath("test", "barrel_menu")).fallbackContainerType());
    }

    @Test
    void recordsValidationIssueForInvalidMenuFallbackContainerType(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("invalid-menu-patch.json"), """
            {
              "patches": [
                {
                  "target": "test:barrel_menu",
                  "content_type": "menu",
                  "patch": {
                    "bedrock": {
                      "menu": {
                        "container_type": "not_real"
                      }
                    }
                  }
                }
              ]
            }
            """);

        MetadataIndex index = new MetadataLoader(LoggerFactory.getLogger("MetadataLoaderTest")).load(tempDir);

        assertEquals(1, index.summary().patchCount());
        assertEquals(1, index.summary().validationIssueCount());
        assertEquals("metadata.patch.menu.container_type", index.validationIssues().getFirst().code());
    }

    @Test
    void recordsValidationIssuesForInvalidPatchEntries(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("invalid.json"), """
            {
              "patches": [
                {
                  "target": "bad_target",
                  "patch": {}
                }
              ]
            }
            """);

        MetadataIndex index = new MetadataLoader(LoggerFactory.getLogger("MetadataLoaderTest")).load(tempDir);

        assertEquals(0, index.summary().patchCount());
        assertEquals(1, index.summary().validationIssueCount());
        assertEquals("metadata.patch.target", index.validationIssues().getFirst().code());
    }
}