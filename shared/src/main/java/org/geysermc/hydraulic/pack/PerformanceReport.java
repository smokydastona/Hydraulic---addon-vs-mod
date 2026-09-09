package org.geysermc.hydraulic.pack;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

public record PerformanceReport(
    @Nullable StartupMetrics startup,
    @Nullable PackConversionMetrics lastPackConversion
) {
    @NotNull
    public static PerformanceReport empty() {
        return new PerformanceReport(null, null);
    }

    @NotNull
    public PerformanceReport withStartup(@NotNull StartupMetrics startup) {
        return new PerformanceReport(startup, this.lastPackConversion);
    }

    @NotNull
    public PerformanceReport withPackConversion(@NotNull PackConversionMetrics packConversion) {
        return new PerformanceReport(this.startup, packConversion);
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
}
