package org.geysermc.hydraulic.metadata;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Seeds Hydraulic-provided built-in metadata packs into {@code config/hydraulic/metadata/builtin}
 * on startup for curated mod setups (e.g. Immersive Engineering multiblock machines, Create kinetic mixers).
 */
public final class MetadataBuiltinBootstrap {
    private static final String RESOURCE_DIRECTORY = "/hydraulic/metadata/builtin/";
    private static final String[] BUNDLED_METADATA_FILES = {
        "immersiveengineering.crusher.json",
        "immersiveengineering.diesel_generator.json",
        "immersiveengineering.arc_furnace.json",
        "create.mechanical_mixer.json",
        "mekanism.digital_miner.json",
        "mekanism.thermoelectric_generator.json",
        "mekanism.chemical_crystallizer.json",
        "botania.mana_pool.json",
        "ae2.inscriber.json",
        "thermal.machine_pulverizer.json"
    };

    private MetadataBuiltinBootstrap() {
    }

    /**
     * Installs the bundled built-in metadata files into {@code metadataRoot/builtin}.
     *
     * @param logger       logger to report issues to
     * @param metadataRoot the metadata root directory (typically {@code config/hydraulic/metadata})
     */
    public static void installBuiltinMetadata(@NotNull Logger logger, @NotNull Path metadataRoot) {
        Path builtinDirectory = metadataRoot.resolve("builtin");
        try {
            Files.createDirectories(builtinDirectory);
        } catch (IOException e) {
            logger.error("Failed to create builtin metadata directory {}", builtinDirectory, e);
            return;
        }

        for (String fileName : BUNDLED_METADATA_FILES) {
            Path destination = builtinDirectory.resolve(fileName);
            try (InputStream inputStream = MetadataBuiltinBootstrap.class.getResourceAsStream(RESOURCE_DIRECTORY + fileName)) {
                if (inputStream == null) {
                    throw new IOException("Missing bundled metadata resource " + RESOURCE_DIRECTORY + fileName);
                }
                Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                logger.warn("Failed to install builtin metadata pack {}", fileName, e);
            }
        }
    }
}
