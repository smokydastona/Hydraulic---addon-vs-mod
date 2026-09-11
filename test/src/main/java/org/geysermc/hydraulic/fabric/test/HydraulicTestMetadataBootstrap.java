package org.geysermc.hydraulic.fabric.test;

import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

final class HydraulicTestMetadataBootstrap {
    private static final Logger LOGGER = LoggerFactory.getLogger(HydraulicTestMetadataBootstrap.class);
    private static final String RESOURCE_DIRECTORY = "/hydraulic/metadata/";
    private static final String[] BUNDLED_METADATA_FILES = {
        "hydraulic_test_mod.golden_barrel.json",
        "hydraulic_test_mod.item_transfer_machine.json",
        "hydraulic_test_mod.processing_machine.json",
        "hydraulic_test_mod.fluid_machine.json",
        "hydraulic_test_mod.energy_machine.json",
        "hydraulic_test_mod.mixed_resource_machine.json",
        "hydraulic_test_mod.menu_machine.json"
    };

    private HydraulicTestMetadataBootstrap() {
    }

    static void installBundledMetadata() {
        try {
            installBundledMetadata(FabricLoader.getInstance().getConfigDir());
        } catch (IOException exception) {
            LOGGER.warn("Failed to install bundled Hydraulic test metadata fixture", exception);
        }
    }

    static void installBundledMetadata(@NotNull Path configDirectory) throws IOException {
        Path metadataDirectory = configDirectory.resolve("hydraulic").resolve("metadata");
        Files.createDirectories(metadataDirectory);

        for (String fileName : BUNDLED_METADATA_FILES) {
            Path destination = metadataDirectory.resolve(fileName);
            try (InputStream inputStream = HydraulicTestMetadataBootstrap.class.getResourceAsStream(RESOURCE_DIRECTORY + fileName)) {
                if (inputStream == null) {
                    throw new IOException("Missing bundled metadata resource " + RESOURCE_DIRECTORY + fileName);
                }

                Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }
}