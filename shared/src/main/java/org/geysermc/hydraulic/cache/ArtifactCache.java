package org.geysermc.hydraulic.cache;

import org.geysermc.hydraulic.Constants;
import org.geysermc.hydraulic.compat.CompatibilityReport;
import org.geysermc.hydraulic.compat.ContentInventory;
import org.geysermc.hydraulic.pack.ModResourceIndex;
import org.geysermc.hydraulic.pack.PackValidationReport;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ArtifactCache {
    private static final String INDEX_MANIFEST = "index-manifest.json";
    private static final String COMPATIBILITY_MANIFEST = "compatibility-manifest.json";
    private static final String CONTENT_INVENTORY = "content-inventory.json";
    private static final String COMPATIBILITY_REPORT = "compatibility-report.json";
    private static final String CONVERSION_MANIFEST = "conversion-manifest.json";
    private static final String VALIDATION_REPORT = "pack-validation-report.json";

    private final Logger logger;
    private final Path root;

    public ArtifactCache(@NotNull Logger logger, @NotNull Path root) {
        this.logger = logger;
        this.root = root;
    }

    public void ensureLayout() {
        try {
            Files.createDirectories(this.indexPath());
            Files.createDirectories(this.compatibilityPath());
            Files.createDirectories(this.conversionsPath());
            Files.createDirectories(this.validationPath());
            Files.createDirectories(this.manifestsPath());
        } catch (IOException e) {
            this.logger.error("Failed to initialize Hydraulic artifact cache layout at {}", this.root, e);
        }
    }

    public void storeIndexSnapshot(@NotNull IndexSnapshot snapshot) {
        this.writeJson(this.indexPath().resolve(INDEX_MANIFEST), snapshot);
    }

    @Nullable
    public IndexSnapshot loadIndexSnapshot() {
        return this.readJson(this.indexPath().resolve(INDEX_MANIFEST), IndexSnapshot.class);
    }

    public void storeCompatibilitySnapshot(@NotNull CompatibilitySnapshot snapshot) {
        Path compatibilityPath = this.compatibilityPath();
        this.writeJson(compatibilityPath.resolve(COMPATIBILITY_MANIFEST), snapshot.manifest());
        this.writeJson(compatibilityPath.resolve(CONTENT_INVENTORY), snapshot.inventory());
        this.writeJson(compatibilityPath.resolve(COMPATIBILITY_REPORT), snapshot.report());
    }

    @Nullable
    public CompatibilitySnapshot loadCompatibilitySnapshot(@NotNull CompatibilityCacheKey key) {
        CompatibilityManifest manifest = this.readJson(this.compatibilityPath().resolve(COMPATIBILITY_MANIFEST), CompatibilityManifest.class);
        if (manifest == null || !manifest.cacheKey().equals(key.value())) {
            return null;
        }

        ContentInventory inventory = this.readJson(this.compatibilityPath().resolve(CONTENT_INVENTORY), ContentInventory.class);
        CompatibilityReport report = this.readJson(this.compatibilityPath().resolve(COMPATIBILITY_REPORT), CompatibilityReport.class);
        if (inventory == null || report == null) {
            return null;
        }
        return new CompatibilitySnapshot(manifest, inventory, report);
    }

    public void storeConversionArtifact(@NotNull String modId, @NotNull ConversionArtifact artifact) {
        this.writeJson(this.conversionsPath().resolve(modId).resolve(CONVERSION_MANIFEST), artifact);
    }

    public void storeValidationArtifact(@NotNull PackValidationReport report, @NotNull String cacheKey) {
        Path validationPath = this.validationPath();
        this.writeJson(validationPath.resolve(VALIDATION_REPORT), report);
        this.writeJson(this.manifestsPath().resolve("validation-manifest.json"), new ValidationManifest(cacheKey));
    }

    @NotNull
    private Path indexPath() {
        return this.root.resolve("index");
    }

    @NotNull
    private Path compatibilityPath() {
        return this.root.resolve("compatibility");
    }

    @NotNull
    private Path conversionsPath() {
        return this.root.resolve("conversions");
    }

    @NotNull
    private Path validationPath() {
        return this.root.resolve("validation");
    }

    @NotNull
    private Path manifestsPath() {
        return this.root.resolve("manifests");
    }

    private void writeJson(@NotNull Path path, @NotNull Object value) {
        try {
            Files.createDirectories(path.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(path)) {
                Constants.GSON.toJson(value, writer);
            }
        } catch (IOException e) {
            this.logger.error("Failed to write Hydraulic artifact cache entry {}", path, e);
        }
    }

    @Nullable
    private <T> T readJson(@NotNull Path path, @NotNull Class<T> type) {
        if (!Files.isRegularFile(path)) {
            return null;
        }

        try (BufferedReader reader = Files.newBufferedReader(path)) {
            return Constants.GSON.fromJson(reader, type);
        } catch (IOException e) {
            this.logger.error("Failed to read Hydraulic artifact cache entry {}", path, e);
            return null;
        }
    }

    public record CompatibilityCacheKey(@NotNull String value) {
    }

    public record CompatibilityManifest(
        @NotNull String cacheKey,
        @NotNull String metadataFingerprint,
        int modCount,
        @NotNull Map<String, String> modFingerprints
    ) {
        public CompatibilityManifest {
            modFingerprints = Map.copyOf(new LinkedHashMap<>(modFingerprints));
        }
    }

    public record CompatibilitySnapshot(
        @NotNull CompatibilityManifest manifest,
        @NotNull ContentInventory inventory,
        @NotNull CompatibilityReport report
    ) {
    }

    public record IndexSnapshot(
        @NotNull String algorithm,
        int modCount,
        @NotNull Map<String, IndexedMod> mods
    ) {
        public IndexSnapshot {
            mods = Map.copyOf(new LinkedHashMap<>(mods));
        }

        public static IndexSnapshot from(@NotNull Map<String, ModResourceIndex> indexes) {
            Map<String, IndexedMod> mods = new LinkedHashMap<>();
            for (Map.Entry<String, ModResourceIndex> entry : indexes.entrySet()) {
                ModResourceIndex index = entry.getValue();
                mods.put(entry.getKey(), new IndexedMod(
                    index.fingerprint(),
                    index.namespaces().size(),
                    index.blockStateCount(),
                    index.itemAssetCount(),
                    index.hasAssetFiles(),
                    summarizeAssetCounts(index)
                ));
            }
            return new IndexSnapshot("HYDRAULIC_INDEX_SNAPSHOT_V1", mods.size(), mods);
        }

        @NotNull
        private static Map<String, Integer> summarizeAssetCounts(@NotNull ModResourceIndex index) {
            Map<String, Integer> counts = new LinkedHashMap<>();
            for (String category : java.util.List.of("blockstates", "item_models", "models", "textures", "sounds", "lang", "recipes", "tags", "loot_tables")) {
                counts.put(category, index.assetEntries(category).size());
            }
            return counts;
        }
    }

    public record IndexedMod(
        @NotNull ModResourceIndex.ResourceFingerprint fingerprint,
        int namespaceCount,
        int blockStateCount,
        int itemAssetCount,
        boolean hasAssetFiles,
        @NotNull Map<String, Integer> assetCounts
    ) {
        public IndexedMod {
            assetCounts = Map.copyOf(new LinkedHashMap<>(assetCounts));
        }
    }

    public record ConversionArtifact(
        @NotNull String modId,
        @NotNull ConversionKey conversionKey,
        @NotNull String packPath,
        @NotNull String packUuid
    ) {
    }

    public record ValidationManifest(@NotNull String cacheKey) {
    }
}