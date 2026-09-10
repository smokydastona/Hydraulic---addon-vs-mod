package org.geysermc.hydraulic.compat.corpus;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads and manages the Bedrock addon corpus.
 *
 * Hydraulic startup must not depend on live crawling, remote availability, or background scraping.
 * This loader works with validated local cache snapshots only.
 */
public final class AddonCorpusLoader {
    private static final String CORPUS_MANIFEST = "corpus-manifest.json";
    private static final String CORPUS_INDEX = "corpus-index.json";
    private static final String CURATED_DIR = "curated";
    private static final String GENERATED_DIR = "generated";

    private final Logger logger;
    private final Path corpusRoot;
    private AddonCorpusIndex index;

    public AddonCorpusLoader(@NotNull Logger logger, @NotNull Path configRoot) {
        this.logger = logger;
        this.corpusRoot = configRoot.resolve("corpus");
        this.index = AddonCorpusIndex.empty();
    }

    public void ensureLayout() {
        try {
            Files.createDirectories(this.corpusRoot);
            Files.createDirectories(this.corpusRoot.resolve(CURATED_DIR));
            Files.createDirectories(this.corpusRoot.resolve(GENERATED_DIR));
        } catch (Exception e) {
            this.logger.error("Failed to initialize corpus layout at {}", this.corpusRoot, e);
        }
    }

    /**
     * Loads the corpus index from local cache.
     * Returns empty index if no cached index exists.
     */
    @NotNull
    public AddonCorpusIndex loadIndex() {
        Path indexPath = this.corpusRoot.resolve(CORPUS_INDEX);
        if (!Files.isRegularFile(indexPath)) {
            this.logger.debug("No corpus index found at {}, returning empty index", indexPath);
            return AddonCorpusIndex.empty();
        }

        try (var reader = Files.newBufferedReader(indexPath)) {
            AddonCorpusIndex loaded = org.geysermc.hydraulic.Constants.GSON.fromJson(reader, AddonCorpusIndex.class);
            this.index = loaded;
            this.logger.info("Loaded corpus index (entries={}, version={})", loaded.entries().size(), loaded.corpusVersion());
            return loaded;
        } catch (Exception e) {
            this.logger.error("Failed to load corpus index from {}", indexPath, e);
            return AddonCorpusIndex.empty();
        }
    }

    /**
     * Loads a specific corpus entry by ID.
     */
    @Nullable
    public AddonCorpusEntry loadEntry(@NotNull String corpusId) {
        AddonCorpusIndex.IndexedEntry indexed = this.index.entries().get(corpusId);
        if (indexed == null) {
            return null;
        }

        Path entryPath = this.resolveEntryPath(indexed.storageLocation(), corpusId);
        if (!Files.isRegularFile(entryPath)) {
            this.logger.warn("Corpus entry file not found at {} for corpusId {}", entryPath, corpusId);
            return null;
        }

        try (var reader = Files.newBufferedReader(entryPath)) {
            return org.geysermc.hydraulic.Constants.GSON.fromJson(reader, AddonCorpusEntry.class);
        } catch (Exception e) {
            this.logger.error("Failed to load corpus entry from {} for corpusId {}", entryPath, corpusId, e);
            return null;
        }
    }

    /**
     * Loads all admissible corpus entries.
     */
    @NotNull
    public List<AddonCorpusEntry> loadAdmissibleEntries() {
        List<AddonCorpusEntry> entries = new ArrayList<>();
        for (Map.Entry<String, AddonCorpusIndex.IndexedEntry> entry : this.index.entries().entrySet()) {
            if (entry.getValue().isAdmissible()) {
                AddonCorpusEntry loaded = this.loadEntry(entry.getKey());
                if (loaded != null) {
                    entries.add(loaded);
                }
            }
        }
        return entries;
    }

    /**
     * Stores the corpus index.
     */
    public void storeIndex(@NotNull AddonCorpusIndex index) {
        Path indexPath = this.corpusRoot.resolve(CORPUS_INDEX);
        try {
            Files.createDirectories(indexPath.getParent());
            try (var writer = Files.newBufferedWriter(indexPath)) {
                org.geysermc.hydraulic.Constants.GSON.toJson(index, writer);
            }
            this.index = index;
            this.logger.info("Stored corpus index (entries={}, version={})", index.entries().size(), index.corpusVersion());
        } catch (Exception e) {
            this.logger.error("Failed to store corpus index to {}", indexPath, e);
        }
    }

    /**
     * Stores a corpus entry.
     */
    public void storeEntry(@NotNull AddonCorpusEntry entry, @NotNull String storageLocation) {
        String corpusId = entry.identity().corpusId();
        Path entryPath = this.resolveEntryPath(storageLocation, corpusId);
        try {
            Files.createDirectories(entryPath.getParent());
            try (var writer = Files.newBufferedWriter(entryPath)) {
                org.geysermc.hydraulic.Constants.GSON.toJson(entry, writer);
            }
            this.logger.debug("Stored corpus entry {} at {}", corpusId, entryPath);
        } catch (Exception e) {
            this.logger.error("Failed to store corpus entry {} to {}", corpusId, entryPath, e);
        }
    }

    @NotNull
    private Path resolveEntryPath(@NotNull String storageLocation, @NotNull String corpusId) {
        return this.corpusRoot.resolve(storageLocation).resolve(corpusId + ".json");
    }

    /**
     * Returns the current corpus index.
     */
    @NotNull
    public AddonCorpusIndex index() {
        return this.index;
    }
}