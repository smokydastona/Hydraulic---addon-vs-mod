package org.geysermc.hydraulic.compat.corpus;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
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
        Path manifestPath = this.corpusRoot.resolve(CORPUS_MANIFEST);
        if (!Files.isRegularFile(indexPath)) {
            this.logger.debug("No corpus index found at {}, returning empty index", indexPath);
            return AddonCorpusIndex.empty();
        }

        try (var reader = Files.newBufferedReader(indexPath);
             var manifestReader = Files.newBufferedReader(manifestPath)) {
            AddonCorpusIndex loaded = org.geysermc.hydraulic.Constants.GSON.fromJson(reader, AddonCorpusIndex.class);
            CorpusManifest manifest = org.geysermc.hydraulic.Constants.GSON.fromJson(manifestReader, CorpusManifest.class);
            if (loaded == null || manifest == null || !manifest.matches(loaded, Files.readString(indexPath))) {
                this.logger.warn("Rejected corpus snapshot at {} because its manifest does not match", this.corpusRoot);
                return AddonCorpusIndex.empty();
            }
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
        if (entryPath == null || !Files.isRegularFile(entryPath)) {
            this.logger.warn("Corpus entry file not found at {} for corpusId {}", entryPath, corpusId);
            return null;
        }

        try (var reader = Files.newBufferedReader(entryPath)) {
            AddonCorpusEntry loaded = org.geysermc.hydraulic.Constants.GSON.fromJson(reader, AddonCorpusEntry.class);
            if (loaded == null
                || loaded.implementationFacts() == null
                || !corpusId.equals(loaded.identity().corpusId())
                || !indexed.bedrockIdentifier().equals(loaded.identity().bedrockIdentifier())) {
                this.logger.warn("Rejected corpus entry {} because its identity does not match the index", corpusId);
                return null;
            }
            List<String> validationErrors = AddonCorpusValidator.validate(loaded);
            if (!validationErrors.isEmpty()) {
                this.logger.warn("Rejected corpus entry {} because validation failed: {}", corpusId, validationErrors);
                return null;
            }
            return loaded;
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
        Path manifestPath = this.corpusRoot.resolve(CORPUS_MANIFEST);
        try {
            Files.createDirectories(indexPath.getParent());
            String serializedIndex = org.geysermc.hydraulic.Constants.GSON.toJson(index);
            Files.writeString(indexPath, serializedIndex, StandardCharsets.UTF_8);
            CorpusManifest manifest = new CorpusManifest(
                index.corpusVersion(),
                index.algorithm(),
                index.entries().size(),
                sha256(serializedIndex)
            );
            Files.writeString(manifestPath, org.geysermc.hydraulic.Constants.GSON.toJson(manifest), StandardCharsets.UTF_8);
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
        if (entryPath == null) {
            this.logger.error("Rejected corpus entry path for corpusId {} and storage location {}", corpusId, storageLocation);
            return;
        }
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
    @Nullable
    private Path resolveEntryPath(@NotNull String storageLocation, @NotNull String corpusId) {
        if (storageLocation.isBlank() || corpusId.isBlank() || corpusId.contains("..") || corpusId.contains("/") || corpusId.contains("\\")) {
            return null;
        }
        Path root = this.corpusRoot.toAbsolutePath().normalize();
        Path resolved = root.resolve(storageLocation).resolve(corpusId + ".json").normalize();
        return resolved.startsWith(root) ? resolved : null;
    }

    /**
     * Returns the current corpus index.
     */
    @NotNull
    public AddonCorpusIndex index() {
        return this.index;
    }

    private static String sha256(@NotNull String value) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        StringBuilder result = new StringBuilder(digest.length * 2);
        for (byte current : digest) {
            result.append(String.format("%02x", current));
        }
        return result.toString();
    }

    private record CorpusManifest(
        String corpusVersion,
        String algorithm,
        int entryCount,
        String indexSha256
    ) {
        private boolean matches(@NotNull AddonCorpusIndex index, @NotNull String serializedIndex) throws Exception {
            return this.corpusVersion.equals(index.corpusVersion())
                && this.algorithm.equals(index.algorithm())
                && this.entryCount == index.entries().size()
                && this.indexSha256.equals(sha256(serializedIndex));
        }
    }
}