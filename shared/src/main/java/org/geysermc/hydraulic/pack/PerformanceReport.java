package org.geysermc.hydraulic.pack;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

public record PerformanceReport(
    @Nullable StartupMetrics startup,
    @Nullable PackConversionMetrics lastPackConversion,
    @Nullable CacheMetrics modelResolutionCache
) {
    @NotNull
    public static PerformanceReport empty() {
        return new PerformanceReport(null, null, null);
    }

    @NotNull
    public PerformanceReport withStartup(@NotNull StartupMetrics startup) {
        return new PerformanceReport(startup, this.lastPackConversion, this.modelResolutionCache);
    }

    @NotNull
    public PerformanceReport withPackConversion(@NotNull PackConversionMetrics packConversion) {
        return new PerformanceReport(this.startup, packConversion, this.modelResolutionCache);
    }

    @NotNull
    public PerformanceReport withModelResolutionCache(@NotNull CacheMetrics cacheMetrics) {
        return new PerformanceReport(this.startup, this.lastPackConversion, cacheMetrics);
    }

    public record StartupMetrics(
        long indexedResourcesMillis,
        long metadataLoadMillis,
        long compatibilityInitializationMillis,
        long resourcePackReadMillis,
        long modelIndexBuildMillis,
        int modCount,
        int modsWithAssetFiles,
        int modsWithoutAssetFiles,
        int namespaces,
        int indexedBlockStates,
        int indexedItemAssets,
        int blockMatches,
        int skippedBlocks,
        int itemMatches,
        int skippedItems,
        int missingItemModelComponents,
        int metadataFiles,
        int metadataRules,
        int metadataPatches,
        int metadataValidationIssues
    ) {
    }

    public record PackConversionMetrics(
        long totalMillis,
        int discoveredMods,
        int ignoredMods,
        int generatedMods,
        int modsWithoutAssetFiles,
        int queuedPacks,
        int cachedPacksRegistered,
        int convertedPacks,
        int failedPacks,
        @NotNull Map<String, ModConversionMetrics> perMod
    ) {
        public PackConversionMetrics {
            perMod = Map.copyOf(new LinkedHashMap<>(perMod));
        }
    }

    public record ModConversionMetrics(
        @NotNull String outcome,
        long millis
    ) {
    }

    public record CacheMetrics(
        long hits,
        long misses
    ) {
        public long requests() {
            return this.hits + this.misses;
        }
    }
}
