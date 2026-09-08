package org.geysermc.hydraulic.compat;

import org.geysermc.hydraulic.compat.model.SupportLevel;
import org.geysermc.hydraulic.metadata.MetadataIndex;
import org.geysermc.hydraulic.platform.mod.ModInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompatibilityManagerTest {
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
}