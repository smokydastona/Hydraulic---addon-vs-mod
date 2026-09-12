package org.geysermc.hydraulic.compat.corpus.java;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Loads and manages the Java-mod capability-semantics corpus. This is a separate, offline
 * evidence store from the Bedrock addon corpus ({@link org.geysermc.hydraulic.compat.corpus.AddonCorpusLoader}):
 * it records what Java modding APIs (Forge Capabilities, Fabric Transfer API, and equivalent
 * inspectable abstractions) actually require, not how Bedrock could implement it.
 *
 * Hydraulic startup must not depend on live crawling, remote availability, or background
 * scraping; this loader works with validated local cache snapshots only.
 */
public final class JavaModCorpusLoader {
    private static final String CORPUS_MANIFEST = "java-corpus-manifest.json";
    private static final String CORPUS_INDEX = "java-corpus-index.json";
    private static final String SOURCES_DIR = "sources";
    private static final String CURATED_DIR = "curated";
    private static final String GENERATED_DIR = "generated";

    private final Logger logger;
    private final Path corpusRoot;
    private JavaModCorpusIndex index;

    public JavaModCorpusLoader(@NotNull Logger logger, @NotNull Path configRoot) {
        this.logger = logger;
        this.corpusRoot = configRoot.resolve("corpus").resolve("java");
        this.index = JavaModCorpusIndex.empty();
    }

    public void ensureLayout() {
        try {
            Files.createDirectories(this.corpusRoot);
            Files.createDirectories(this.corpusRoot.resolve(SOURCES_DIR));
            Files.createDirectories(this.corpusRoot.resolve(CURATED_DIR));
            Files.createDirectories(this.corpusRoot.resolve(GENERATED_DIR));
        } catch (Exception e) {
            this.logger.error("Failed to initialize Java-mod corpus layout at {}", this.corpusRoot, e);
        }
    }

    @NotNull
    public JavaModCorpusIndex refreshIndexFromSnapshots() {
        Map<String, Snapshot> snapshots = new LinkedHashMap<>();
        for (String storageLocation : List.of(SOURCES_DIR, GENERATED_DIR, CURATED_DIR)) {
            Path directory = this.corpusRoot.resolve(storageLocation);
            if (!Files.isDirectory(directory)) {
                continue;
            }

            try (Stream<Path> paths = Files.walk(directory)) {
                for (Path path : paths
                    .filter(Files::isRegularFile)
                    .filter(candidate -> candidate.getFileName().toString().endsWith(".json"))
                    .sorted()
                    .toList()) {
                    this.readSnapshot(path, storageLocation, snapshots);
                }
            } catch (Exception e) {
                this.logger.error("Failed to scan Java-mod corpus snapshot directory {}", directory, e);
            }
        }

        if (snapshots.isEmpty()) {
            return this.index;
        }

        Map<String, JavaModCorpusIndex.IndexedEntry> entries = new LinkedHashMap<>();
        int admissibleEntries = 0;
        for (Snapshot snapshot : snapshots.values().stream()
            .sorted(Comparator.comparing(snapshot -> snapshot.entry().identity().corpusId()))
            .toList()) {
            JavaModCorpusEntry entry = snapshot.entry();
            boolean admissible = entry.admissibility().isAdmissible();
            if (admissible) {
                admissibleEntries++;
            }
            entries.put(entry.identity().corpusId(), new JavaModCorpusIndex.IndexedEntry(
                entry.identity().corpusId(),
                entry.capability(),
                snapshot.storageLocation(),
                admissible,
                entry.admissibility().reason(),
                entry.confidence().overallScore(),
                snapshot.lastModifiedEpochMillis()
            ));
        }

        JavaModCorpusIndex template = JavaModCorpusIndex.empty();
        JavaModCorpusIndex refreshed = new JavaModCorpusIndex(
            template.corpusVersion(),
            template.algorithm(),
            entries,
            new JavaModCorpusIndex.CorpusMetadata(
                entries.size(),
                admissibleEntries,
                entries.size() - admissibleEntries,
                System.currentTimeMillis()
            )
        );
        this.storeIndex(refreshed);
        return refreshed;
    }

    private void readSnapshot(
        @NotNull Path path,
        @NotNull String rootStorageLocation,
        @NotNull Map<String, Snapshot> snapshots
    ) {
        String fileName = path.getFileName().toString();
        if (CORPUS_INDEX.equals(fileName) || CORPUS_MANIFEST.equals(fileName)) {
            return;
        }

        try (var reader = Files.newBufferedReader(path)) {
            JavaModCorpusEntry entry = org.geysermc.hydraulic.Constants.GSON.fromJson(reader, JavaModCorpusEntry.class);
            List<String> validationErrors = entry == null ? List.of("entry is null") : JavaModCorpusValidator.validate(entry);
            String corpusId = entry == null || entry.identity() == null ? "" : entry.identity().corpusId();
            String expectedFileName = corpusId + ".json";
            if (!validationErrors.isEmpty() || !fileName.equals(expectedFileName)) {
                this.logger.warn("Rejected Java-mod corpus snapshot {}: validation={}, expectedFileName={}", path, validationErrors, expectedFileName);
                return;
            }

            String storageLocation = this.corpusRoot.relativize(path.getParent()).toString().replace('\\', '/');
            Snapshot candidate = new Snapshot(
                entry,
                storageLocation,
                Files.getLastModifiedTime(path).toMillis(),
                storagePriority(rootStorageLocation)
            );
            Snapshot previous = snapshots.get(corpusId);
            if (previous == null || candidate.priority() >= previous.priority()) {
                snapshots.put(corpusId, candidate);
            }
        } catch (Exception e) {
            this.logger.warn("Rejected unreadable Java-mod corpus snapshot {}", path, e);
        }
    }

    private static int storagePriority(@NotNull String storageLocation) {
        return switch (storageLocation) {
            case SOURCES_DIR -> 1;
            case GENERATED_DIR -> 2;
            case CURATED_DIR -> 3;
            default -> 0;
        };
    }

    @NotNull
    public JavaModCorpusIndex loadIndex() {
        Path indexPath = this.corpusRoot.resolve(CORPUS_INDEX);
        Path manifestPath = this.corpusRoot.resolve(CORPUS_MANIFEST);
        if (!Files.isRegularFile(indexPath)) {
            this.logger.debug("No Java-mod corpus index found at {}, returning empty index", indexPath);
            return JavaModCorpusIndex.empty();
        }

        try (var reader = Files.newBufferedReader(indexPath);
             var manifestReader = Files.newBufferedReader(manifestPath)) {
            JavaModCorpusIndex loaded = org.geysermc.hydraulic.Constants.GSON.fromJson(reader, JavaModCorpusIndex.class);
            CorpusManifest manifest = org.geysermc.hydraulic.Constants.GSON.fromJson(manifestReader, CorpusManifest.class);
            if (loaded == null || manifest == null || !manifest.matches(loaded, Files.readString(indexPath))) {
                this.logger.warn("Rejected Java-mod corpus snapshot at {} because its manifest does not match", this.corpusRoot);
                return JavaModCorpusIndex.empty();
            }
            this.index = loaded;
            this.logger.info("Loaded Java-mod corpus index (entries={}, version={})", loaded.entries().size(), loaded.corpusVersion());
            return loaded;
        } catch (Exception e) {
            this.logger.error("Failed to load Java-mod corpus index from {}", indexPath, e);
            return JavaModCorpusIndex.empty();
        }
    }

    @Nullable
    public JavaModCorpusEntry loadEntry(@NotNull String corpusId) {
        JavaModCorpusIndex.IndexedEntry indexed = this.index.entries().get(corpusId);
        if (indexed == null) {
            return null;
        }

        Path entryPath = this.resolveEntryPath(indexed.storageLocation(), corpusId);
        if (entryPath == null || !Files.isRegularFile(entryPath)) {
            this.logger.warn("Java-mod corpus entry file not found at {} for corpusId {}", entryPath, corpusId);
            return null;
        }

        try (var reader = Files.newBufferedReader(entryPath)) {
            JavaModCorpusEntry loaded = org.geysermc.hydraulic.Constants.GSON.fromJson(reader, JavaModCorpusEntry.class);
            if (loaded == null
                || !corpusId.equals(loaded.identity().corpusId())
                || !indexed.capability().equals(loaded.capability())) {
                this.logger.warn("Rejected Java-mod corpus entry {} because its identity does not match the index", corpusId);
                return null;
            }
            List<String> validationErrors = JavaModCorpusValidator.validate(loaded);
            if (!validationErrors.isEmpty()) {
                this.logger.warn("Rejected Java-mod corpus entry {} because validation failed: {}", corpusId, validationErrors);
                return null;
            }
            return loaded;
        } catch (Exception e) {
            this.logger.error("Failed to load Java-mod corpus entry from {} for corpusId {}", entryPath, corpusId, e);
            return null;
        }
    }

    @NotNull
    public List<JavaModCorpusEntry> loadAdmissibleEntries() {
        List<JavaModCorpusEntry> entries = new ArrayList<>();
        for (Map.Entry<String, JavaModCorpusIndex.IndexedEntry> entry : this.index.entries().entrySet()) {
            if (entry.getValue().isAdmissible()) {
                JavaModCorpusEntry loaded = this.loadEntry(entry.getKey());
                if (loaded != null) {
                    entries.add(loaded);
                }
            }
        }
        return entries;
    }

    @NotNull
    public List<JavaModCorpusEntry> loadAllEntries() {
        List<JavaModCorpusEntry> entries = new ArrayList<>();
        for (String corpusId : this.index.entries().keySet()) {
            JavaModCorpusEntry loaded = this.loadEntry(corpusId);
            if (loaded != null) {
                entries.add(loaded);
            }
        }
        return entries;
    }

    public void storeIndex(@NotNull JavaModCorpusIndex index) {
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
            this.logger.info("Stored Java-mod corpus index (entries={}, version={})", index.entries().size(), index.corpusVersion());
        } catch (Exception e) {
            this.logger.error("Failed to store Java-mod corpus index to {}", indexPath, e);
        }
    }

    public void storeEntry(@NotNull JavaModCorpusEntry entry, @NotNull String storageLocation) {
        String corpusId = entry.identity().corpusId();
        Path entryPath = this.resolveEntryPath(storageLocation, corpusId);
        if (entryPath == null) {
            this.logger.error("Rejected Java-mod corpus entry path for corpusId {} and storage location {}", corpusId, storageLocation);
            return;
        }
        try {
            Files.createDirectories(entryPath.getParent());
            try (var writer = Files.newBufferedWriter(entryPath)) {
                org.geysermc.hydraulic.Constants.GSON.toJson(entry, writer);
            }
            this.logger.debug("Stored Java-mod corpus entry {} at {}", corpusId, entryPath);
        } catch (Exception e) {
            this.logger.error("Failed to store Java-mod corpus entry {} to {}", corpusId, entryPath, e);
        }
    }

    @Nullable
    private Path resolveEntryPath(@NotNull String storageLocation, @NotNull String corpusId) {
        if (storageLocation.isBlank() || corpusId.isBlank() || corpusId.contains("..") || corpusId.contains("/") || corpusId.contains("\\")) {
            return null;
        }
        Path root = this.corpusRoot.toAbsolutePath().normalize();
        Path resolved = root.resolve(storageLocation).resolve(corpusId + ".json").normalize();
        return resolved.startsWith(root) ? resolved : null;
    }

    @NotNull
    public JavaModCorpusIndex index() {
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
        private boolean matches(@NotNull JavaModCorpusIndex index, @NotNull String serializedIndex) throws Exception {
            return this.corpusVersion.equals(index.corpusVersion())
                && this.algorithm.equals(index.algorithm())
                && this.entryCount == index.entries().size()
                && this.indexSha256.equals(sha256(serializedIndex));
        }
    }

    private record Snapshot(
        @NotNull JavaModCorpusEntry entry,
        @NotNull String storageLocation,
        long lastModifiedEpochMillis,
        int priority
    ) {
    }
}
