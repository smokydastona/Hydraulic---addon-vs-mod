package org.geysermc.hydraulic.fabric.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HydraulicTestMetadataBootstrapTest {
    @Test
    void installsBundledMetadataIntoHydraulicConfigDirectory(@TempDir Path tempDir) throws IOException {
        HydraulicTestMetadataBootstrap.installBundledMetadata(tempDir);

        Path installed = tempDir.resolve("hydraulic").resolve("metadata").resolve("hydraulic_test_mod.golden_barrel.json");
        assertTrue(Files.exists(installed));
        String content = Files.readString(installed);
        assertTrue(content.contains("\"hydraulic_test_mod:barrel_cube\""));
        assertTrue(content.contains("\"visual_only_runtime\""));
        String resourceContent = Files.readString(Path.of("src/main/resources/hydraulic/metadata/hydraulic_test_mod.golden_barrel.json"));
        assertEquals(resourceContent, content);
    }

    @Test
    void installsItemTransferMachineMetadataIntoHydraulicConfigDirectory(@TempDir Path tempDir) throws IOException {
        HydraulicTestMetadataBootstrap.installBundledMetadata(tempDir);

        Path installed = tempDir.resolve("hydraulic").resolve("metadata").resolve("hydraulic_test_mod.item_transfer_machine.json");
        assertTrue(Files.exists(installed));
        String content = Files.readString(installed);
        assertTrue(content.contains("\"hydraulic_test_mod:item_transfer_machine\""));
        assertTrue(content.contains("\"can_insert\""));
        String resourceContent = Files.readString(Path.of("src/main/resources/hydraulic/metadata/hydraulic_test_mod.item_transfer_machine.json"));
        assertEquals(resourceContent, content);
    }
}