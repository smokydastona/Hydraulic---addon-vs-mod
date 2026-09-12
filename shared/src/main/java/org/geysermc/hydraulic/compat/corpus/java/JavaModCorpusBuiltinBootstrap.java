package org.geysermc.hydraulic.compat.corpus.java;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Seeds Hydraulic-reviewed Java-mod capability-semantics snapshots into
 * {@code config/hydraulic/corpus/java/curated/builtin} on startup, mirroring
 * {@link org.geysermc.hydraulic.compat.corpus.CorpusBuiltinBootstrap} for the Bedrock corpus.
 *
 * These entries record documented facts about official Java modding API contracts (Forge
 * Capabilities, Fabric Transfer API); no Forge/Fabric source code is bundled or copied.
 */
public final class JavaModCorpusBuiltinBootstrap {
    private static final String RESOURCE_DIRECTORY = "/hydraulic/corpus/java/builtin/";
    private static final String[] BUNDLED_CORPUS_FILES = {
        "forge-capabilities.json",
        "fabric-transfer-api.json"
    };

    private JavaModCorpusBuiltinBootstrap() {
    }

    public static void installBuiltinEntries(@NotNull Logger logger, @NotNull Path javaCorpusRoot) {
        Path builtinDirectory = javaCorpusRoot.resolve("curated").resolve("builtin");
        try {
            Files.createDirectories(builtinDirectory);
        } catch (IOException e) {
            logger.error("Failed to create builtin Java-mod corpus directory {}", builtinDirectory, e);
            return;
        }

        for (String fileName : BUNDLED_CORPUS_FILES) {
            Path destination = builtinDirectory.resolve(fileName);
            try (InputStream inputStream = JavaModCorpusBuiltinBootstrap.class.getResourceAsStream(RESOURCE_DIRECTORY + fileName)) {
                if (inputStream == null) {
                    throw new IOException("Missing bundled Java-mod corpus resource " + RESOURCE_DIRECTORY + fileName);
                }
                Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                logger.warn("Failed to install builtin Java-mod corpus entry {}", fileName, e);
            }
        }
    }
}
