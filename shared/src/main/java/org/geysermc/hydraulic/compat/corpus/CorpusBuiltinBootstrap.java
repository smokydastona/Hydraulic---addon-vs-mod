package org.geysermc.hydraulic.compat.corpus;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Seeds Hydraulic-reviewed, offline Bedrock addon corpus snapshots into
 * {@code config/hydraulic/corpus/curated/builtin} on startup.
 *
 * These entries are curated by Hydraulic maintainers from public repository
 * README/manifest/release evidence only; no third-party source code or assets
 * are bundled or copied. Each entry records its own license and admissibility
 * facts, and inadmissible entries (for example, repositories with no published
 * license) are still recorded for research/backlog purposes but are excluded
 * from {@link AddonCorpusLoader#loadAdmissibleEntries()}.
 *
 * This directory is always overwritten with the bundled snapshot on startup,
 * since it is Hydraulic-owned content. Server owners who want to curate their
 * own corpus entries should use {@code config/hydraulic/corpus/curated} directly
 * (outside the {@code builtin} subdirectory), which this bootstrap never touches.
 */
public final class CorpusBuiltinBootstrap {
    private static final String RESOURCE_DIRECTORY = "/hydraulic/corpus/builtin/";
    private static final String[] BUNDLED_CORPUS_FILES = {
        "bedrock-energistics-core.json",
        "mojang-bedrock-samples.json",
        "utilitycraft-addon-template.json",
        "utilitycraft.json",
        "bedrock-core-server.json",
        "bedrock-core-ui.json",
        "bedrock-core-network.json",
        "engineering-tools.json",
        "bedrock-core-regolith-filters.json",
        "bedrock-oss-regolith.json",
        "bedrock-oss-bedrock-boost.json",
        "bedrock-oss-add-on-registry.json",
        "farmers-delight-bedrock.json"
    };

    private CorpusBuiltinBootstrap() {
    }

    /**
     * Installs the bundled builtin corpus snapshots into {@code corpusRoot/curated/builtin}.
     *
     * @param logger    logger to report failures to; a failure to install one entry does not
     *                  prevent the others from being installed
     * @param corpusRoot the corpus root directory (typically {@code config/hydraulic/corpus})
     */
    public static void installBuiltinEntries(@NotNull Logger logger, @NotNull Path corpusRoot) {
        Path builtinDirectory = corpusRoot.resolve("curated").resolve("builtin");
        try {
            Files.createDirectories(builtinDirectory);
        } catch (IOException e) {
            logger.error("Failed to create builtin corpus directory {}", builtinDirectory, e);
            return;
        }

        for (String fileName : BUNDLED_CORPUS_FILES) {
            Path destination = builtinDirectory.resolve(fileName);
            try (InputStream inputStream = CorpusBuiltinBootstrap.class.getResourceAsStream(RESOURCE_DIRECTORY + fileName)) {
                if (inputStream == null) {
                    throw new IOException("Missing bundled corpus resource " + RESOURCE_DIRECTORY + fileName);
                }
                Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                logger.warn("Failed to install builtin corpus entry {}", fileName, e);
            }
        }
    }
}
