package org.geysermc.hydraulic.compat;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.common.collect.MultimapBuilder;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.geysermc.hydraulic.compat.adapter.AdapterFeature;
import org.geysermc.hydraulic.compat.model.CompatibilityObject;
import org.geysermc.hydraulic.compat.model.SupportLevel;
import org.geysermc.hydraulic.Constants;
import org.geysermc.hydraulic.metadata.MetadataLoader;
import org.geysermc.hydraulic.metadata.MetadataIndex;
import org.geysermc.hydraulic.pack.ModResourceIndex;
import org.junit.jupiter.api.BeforeAll;
import org.geysermc.hydraulic.platform.mod.ModInfo;
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

class CompatibilityManagerTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void initializesInventoryFromSharedResourceIndex(@TempDir Path tempDir) throws IOException {
        Path root = tempDir.resolve("example-root");
        Path blockstate = root.resolve("assets/example/blockstates/machines/crusher.json");
        Path texture = root.resolve("assets/example/textures/block/crusher.png");
        Path recipe = root.resolve("data/example/recipes/machines/crusher.json");
        Files.createDirectories(blockstate.getParent());
        Files.createDirectories(texture.getParent());
        Files.createDirectories(recipe.getParent());
        Files.writeString(blockstate, "{}");
        Files.writeString(texture, "png");
        Files.writeString(recipe, "{}");

        ModInfo mod = new ModInfo("examplemod", "example", "Example Mod", "1.0.0", null, List.of(root));
        ModResourceIndex resourceIndex = ModResourceIndex.create(mod, LoggerFactory.getLogger("CompatibilityManagerTest"));
        CompatibilityRegistry registry = new CompatibilityManager(LoggerFactory.getLogger("CompatibilityManagerTest"), tempDir).initialize(
            List.of(mod),
            MultimapBuilder.hashKeys().arrayListValues().build(),
            MultimapBuilder.hashKeys().arrayListValues().build(),
            MultimapBuilder.hashKeys().arrayListValues().build(),
            Map.of(mod.id(), resourceIndex),
            MetadataIndex.empty(),
            ignored -> false
        );

        ContentInventory.ModContentInventory inventory = registry.inventory().mods().get(mod.id());
        assertNotNull(inventory);
        assertEquals(1, inventory.assetCounts().get("blockstates"));
        assertEquals(1, inventory.assetCounts().get("textures"));
        assertEquals(1, inventory.assetCounts().get("recipes"));
        assertTrue(inventory.assetEntries().get("blockstates").contains("machines/crusher.json"));
        assertTrue(inventory.assetEntries().get("textures").contains("block/crusher.png"));
        assertTrue(inventory.assetEntries().get("recipes").contains("example:machines/crusher"));
    }

    @Test
    void buildsCapabilityDrivenReport(@TempDir Path tempDir) {
        ContentInventory.ModContentInventory inventory = new ContentInventory.ModContentInventory(
            "examplemod",
            "example",
            "Example Mod",
            "1.0.0",
            List.of(tempDir.toString()),
            new org.geysermc.hydraulic.compat.model.ModFingerprint("examplemod", "example", "1.0.0", "unknown", "test", 1, 1, 1, 0, 0, 1, 1, false, true, false, true, true, true, false, false),
            Map.of("blocks", 1, "items", 1, "entities", 1, "menus", 1),
            Map.of("blocks", List.of("example:test_block"), "items", List.of("example:test_item"), "entities", List.of("example:test_entity"), "menus", List.of("example:test_menu")),
            Map.of("block_assets", 1, "item_assets", 1, "recipes", 1),
            Map.of("block_assets", List.of("example:test_block"), "item_assets", List.of("example:test_item"), "recipes", List.of("example:test_recipe")),
            Map.of("blocks", 1, "items", 1, "entities", 1, "menus", 1),
            Map.of("blocks", List.of("example:test_block"), "items", List.of("example:test_item"), "entities", List.of("example:test_entity"), "menus", List.of("example:test_menu")),
            Map.of("blocks", 1),
            Map.of("blocks", List.of("example:test_block"))
        );
        ContentInventory inventoryRoot = new ContentInventory(Map.of("examplemod", inventory));
        MetadataIndex metadataIndex = MetadataIndex.empty();

        CompatibilityReport report = new CompatibilityManager(LoggerFactory.getLogger("CompatibilityManagerTest"), tempDir).buildReport(inventoryRoot, metadataIndex);

        CompatibilityProfile profile = report.mods().get("examplemod");
        assertNotNull(profile);
        assertFalse(profile.objects().isEmpty());
        assertTrue(profile.supportResults().containsKey("interaction"));
        assertEquals(SupportLevel.UNSUPPORTED, profile.overallLevel());
        assertTrue(profile.overallScore() >= 0);
    }

        @Test
        void propagatesItemBehaviorPatchFactsIntoReportObjects(@TempDir Path tempDir) throws IOException {
                ContentInventory.ModContentInventory inventory = new ContentInventory.ModContentInventory(
                        "examplemod",
                        "example",
                        "Example Mod",
                        "1.0.0",
                        List.of(tempDir.toString()),
                        new org.geysermc.hydraulic.compat.model.ModFingerprint("examplemod", "example", "1.0.0", "unknown", "test", 0, 1, 0, 0, 0, 0, 0, false, false, false, true, false, false, false, false),
                        Map.of("items", 1),
                        Map.of("items", List.of("example:test_item")),
                        Map.of("item_assets", 1),
                        Map.of("item_assets", List.of("example:test_item")),
                        Map.of("items", 1),
                        Map.of("items", List.of("example:test_item")),
                        Map.of(),
                        Map.of()
                );
                ContentInventory inventoryRoot = new ContentInventory(Map.of("examplemod", inventory));

                Files.writeString(tempDir.resolve("compat.json"), """
                        {
                            "patches": [
                                {
                                    "target": "example:test_item",
                                    "content_type": "item",
                                    "patch": {
                                        "behavior": {
                                            "required": true,
                                            "tag": "custom_pack_behavior"
                                        }
                                    }
                                }
                            ]
                        }
                        """);
                MetadataIndex metadataIndex = new MetadataLoader(LoggerFactory.getLogger("CompatibilityManagerTest")).load(tempDir);

                CompatibilityReport report = new CompatibilityManager(LoggerFactory.getLogger("CompatibilityManagerTest"), tempDir).buildReport(inventoryRoot, metadataIndex);

                CompatibilityObject item = report.object("examplemod", "example:test_item", "item");
                assertNotNull(item);
                assertEquals("true", item.inventoryFacts().get("behavior_required"));
                assertEquals("custom_pack_behavior", item.inventoryFacts().get("behavior_tag"));
                assertEquals(SupportLevel.APPROXIMATED, item.supportResults().get("behavior").level());
                assertTrue(item.adapterBindings().stream().anyMatch(binding -> binding.adapterId().equals("item.custom_registration")));
                assertFalse(item.adapterBindings().stream().anyMatch(binding -> binding.feature() == AdapterFeature.ITEM_CREATIVE_EXPOSURE));
                assertTrue(item.runtimeRequirements().contains("item_behavior_bridge"));
                assertTrue(item.findings().stream().anyMatch(finding -> finding.code().equals("item.behavior.required")));
        }

        @Test
        void surfacesTaggedItemAdapterBindingsInReportObjects(@TempDir Path tempDir) throws IOException {
                ContentInventory.ModContentInventory inventory = new ContentInventory.ModContentInventory(
                        "examplemod",
                        "example",
                        "Example Mod",
                        "1.0.0",
                        List.of(tempDir.toString()),
                        new org.geysermc.hydraulic.compat.model.ModFingerprint("examplemod", "example", "1.0.0", "unknown", "test", 0, 1, 0, 0, 0, 0, 0, false, false, false, true, false, false, false, false),
                        Map.of("items", 1),
                        Map.of("items", List.of("example:test_bow")),
                        Map.of("item_assets", 1),
                        Map.of("item_assets", List.of("example:test_bow")),
                        Map.of("items", 1),
                        Map.of("items", List.of("example:test_bow")),
                        Map.of(),
                        Map.of()
                );
                ContentInventory inventoryRoot = new ContentInventory(Map.of("examplemod", inventory));

                Files.writeString(tempDir.resolve("compat.json"), """
                        {
                            "patches": [
                                {
                                    "target": "example:test_bow",
                                    "content_type": "item",
                                    "patch": {
                                        "behavior": {
                                            "required": true,
                                            "tag": "bow_attachable"
                                        }
                                    }
                                }
                            ]
                        }
                        """);
                MetadataIndex metadataIndex = new MetadataLoader(LoggerFactory.getLogger("CompatibilityManagerTest")).load(tempDir);

                CompatibilityReport report = new CompatibilityManager(LoggerFactory.getLogger("CompatibilityManagerTest"), tempDir).buildReport(inventoryRoot, metadataIndex);

                CompatibilityObject item = report.object("examplemod", "example:test_bow", "item");
                assertNotNull(item);
                assertTrue(item.adapterBindings().stream().anyMatch(binding -> binding.adapterId().equals("item.custom_registration")));
                assertTrue(item.adapterBindings().stream().anyMatch(binding -> binding.adapterId().equals("item.bow_attachable") && binding.feature() == AdapterFeature.ATTACHABLE_ITEM_PRESENTATION));
                assertTrue(item.adapterBindings().stream().anyMatch(binding -> binding.adapterId().equals("item.bow_attachable") && binding.feature() == AdapterFeature.ITEM_CREATIVE_EXPOSURE));
                assertTrue(item.runtimeRequirements().contains("item_behavior_bridge"));
        }

        @Test
        void serializesAdapterBindingsIntoCompatibilityReportJson(@TempDir Path tempDir) throws IOException {
                ContentInventory.ModContentInventory inventory = new ContentInventory.ModContentInventory(
                        "examplemod",
                        "example",
                        "Example Mod",
                        "1.0.0",
                        List.of(tempDir.toString()),
                        new org.geysermc.hydraulic.compat.model.ModFingerprint("examplemod", "example", "1.0.0", "unknown", "test", 0, 1, 0, 0, 0, 0, 0, false, false, false, true, false, false, false, false),
                        Map.of("items", 1),
                        Map.of("items", List.of("example:test_bow")),
                        Map.of("item_assets", 1),
                        Map.of("item_assets", List.of("example:test_bow")),
                        Map.of("items", 1),
                        Map.of("items", List.of("example:test_bow")),
                        Map.of(),
                        Map.of()
                );
                ContentInventory inventoryRoot = new ContentInventory(Map.of("examplemod", inventory));

                Files.writeString(tempDir.resolve("compat.json"), """
                        {
                            "patches": [
                                {
                                    "target": "example:test_bow",
                                    "content_type": "item",
                                    "patch": {
                                        "behavior": {
                                            "required": true,
                                            "tag": "bow_attachable"
                                        }
                                    }
                                }
                            ]
                        }
                        """);
                MetadataIndex metadataIndex = new MetadataLoader(LoggerFactory.getLogger("CompatibilityManagerTest")).load(tempDir);

                CompatibilityReport report = new CompatibilityManager(LoggerFactory.getLogger("CompatibilityManagerTest"), tempDir).buildReport(inventoryRoot, metadataIndex);
                JsonObject root = Constants.GSON.toJsonTree(report).getAsJsonObject();
                JsonObject mod = root.getAsJsonObject("mods").getAsJsonObject("examplemod");
                JsonArray objects = mod.getAsJsonArray("objects");
                JsonObject item = objects.get(0).getAsJsonObject();
                JsonArray bindings = item.getAsJsonArray("adapterBindings");
                JsonArray requirements = item.getAsJsonArray("runtimeRequirements");

                assertEquals("bow_attachable", item.getAsJsonObject("inventoryFacts").get("behavior_tag").getAsString());
                assertTrue(bindings.asList().stream()
                    .map(element -> element.getAsJsonObject())
                    .anyMatch(binding -> binding.get("adapterId").getAsString().equals("item.bow_attachable")
                        && binding.get("feature").getAsString().equals(AdapterFeature.ITEM_CREATIVE_EXPOSURE.name())));
                assertTrue(requirements.asList().stream().anyMatch(element -> element.getAsString().equals("item_behavior_bridge")));
        }
}