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
    private static final String RESOURCE_PATH = "/hydraulic/metadata/hydraulic_test_mod.golden_barrel.json";
    private static final String FILE_NAME = "hydraulic_test_mod.golden_barrel.json";

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

        Path destination = metadataDirectory.resolve(FILE_NAME);
        try (InputStream inputStream = HydraulicTestMetadataBootstrap.class.getResourceAsStream(RESOURCE_PATH)) {
            if (inputStream == null) {
                throw new IOException("Missing bundled metadata resource " + RESOURCE_PATH);
            }

            Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}