package org.geysermc.hydraulic.pack;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

public record PerformanceReport(
    @Nullable StartupMetrics startup,
    @Nullable PackConversionMetrics lastPackConversion,
    @Nullable CacheMetrics modelResolutionCache,
    @Nullable ModelProviderMetrics modelProviderCache,
    @Nullable TextureResolutionMetrics textureResolutionCache,
    @Nullable LazyResourceProviderMetrics blockstateProviderCache,
    @Nullable LazyResourceProviderMetrics itemDefinitionProviderCache,
    @Nullable ArtifactCacheMetrics artifactCache,
    @Nullable RuntimeDispatchMetrics runtimeDispatch
) {
    @NotNull
    public static PerformanceReport empty() {
        return new PerformanceReport(null, null, null, null, null, null, null, null, null);
    }

    @NotNull
    public PerformanceReport withStartup(@NotNull StartupMetrics startup) {
        return new PerformanceReport(startup, this.lastPackConversion, this.modelResolutionCache, this.modelProviderCache, this.textureResolutionCache, this.blockstateProviderCache, this.itemDefinitionProviderCache, this.artifactCache, this.runtimeDispatch);
    }

    @NotNull
    public PerformanceReport withPackConversion(@NotNull PackConversionMetrics packConversion) {
        return new PerformanceReport(this.startup, packConversion, this.modelResolutionCache, this.modelProviderCache, this.textureResolutionCache, this.blockstateProviderCache, this.itemDefinitionProviderCache, this.artifactCache, this.runtimeDispatch);
    }

    @NotNull
    public PerformanceReport withModelResolutionCache(@NotNull CacheMetrics cacheMetrics) {
        return new PerformanceReport(this.startup, this.lastPackConversion, cacheMetrics, this.modelProviderCache, this.textureResolutionCache, this.blockstateProviderCache, this.itemDefinitionProviderCache, this.artifactCache, this.runtimeDispatch);
    }

    @NotNull
    public PerformanceReport withModelProviderCache(@NotNull ModelProviderMetrics modelProviderCache) {
        return new PerformanceReport(this.startup, this.lastPackConversion, this.modelResolutionCache, modelProviderCache, this.textureResolutionCache, this.blockstateProviderCache, this.itemDefinitionProviderCache, this.artifactCache, this.runtimeDispatch);
    }

    @NotNull
    public PerformanceReport withTextureResolutionCache(@NotNull TextureResolutionMetrics textureResolutionCache) {
        return new PerformanceReport(this.startup, this.lastPackConversion, this.modelResolutionCache, this.modelProviderCache, textureResolutionCache, this.blockstateProviderCache, this.itemDefinitionProviderCache, this.artifactCache, this.runtimeDispatch);
    }

    @NotNull
    public PerformanceReport withBlockstateProviderCache(@NotNull LazyResourceProviderMetrics blockstateProviderCache) {
        return new PerformanceReport(this.startup, this.lastPackConversion, this.modelResolutionCache, this.modelProviderCache, this.textureResolutionCache, blockstateProviderCache, this.itemDefinitionProviderCache, this.artifactCache, this.runtimeDispatch);
    }

    @NotNull
    public PerformanceReport withItemDefinitionProviderCache(@NotNull LazyResourceProviderMetrics itemDefinitionProviderCache) {
        return new PerformanceReport(this.startup, this.lastPackConversion, this.modelResolutionCache, this.modelProviderCache, this.textureResolutionCache, this.blockstateProviderCache, itemDefinitionProviderCache, this.artifactCache, this.runtimeDispatch);
    }

    @NotNull
    public PerformanceReport withArtifactCache(@NotNull ArtifactCacheMetrics artifactCache) {
        return new PerformanceReport(this.startup, this.lastPackConversion, this.modelResolutionCache, this.modelProviderCache, this.textureResolutionCache, this.blockstateProviderCache, this.itemDefinitionProviderCache, artifactCache, this.runtimeDispatch);
    }

    @NotNull
    public PerformanceReport withRuntimeDispatch(@NotNull RuntimeDispatchMetrics runtimeDispatch) {
        return new PerformanceReport(this.startup, this.lastPackConversion, this.modelResolutionCache, this.modelProviderCache, this.textureResolutionCache, this.blockstateProviderCache, this.itemDefinitionProviderCache, this.artifactCache, runtimeDispatch);
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
        int discoveredTextures,
        int selectedTextures,
        int omittedTextures,
        int textureDependencySources,
        @NotNull Map<String, ModConversionMetrics> perMod
    ) {
        public PackConversionMetrics {
            perMod = Map.copyOf(new LinkedHashMap<>(perMod));
        }
    }

    public record ModConversionMetrics(
        @NotNull String outcome,
        long millis,
        long validationMillis,
        int validationErrors,
        int validationWarnings,
        int validationManualActions,
        int discoveredTextures,
        int selectedTextures,
        int omittedTextures,
        int textureDependencySources
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

    public record ModelProviderMetrics(
        long hits,
        long misses,
        long evictions,
        long size,
        long indexedModels
    ) {
        public long requests() {
            return this.hits + this.misses;
        }
    }

    public record TextureResolutionMetrics(
        long hits,
        long misses,
        long evictions,
        long size
    ) {
        public long requests() {
            return this.hits + this.misses;
        }
    }

    public record LazyResourceProviderMetrics(
        long hits,
        long misses,
        long size,
        long maxSize
    ) {
        public long requests() {
            return this.hits + this.misses;
        }
    }

    public record ArtifactCacheMetrics(
        @NotNull CacheMetrics index,
        @NotNull CacheMetrics compatibility,
        @NotNull CacheMetrics conversions,
        @NotNull CacheMetrics validation
    ) {
    }

    public record RuntimeDispatchMetrics(
        @NotNull CacheMetrics blocks,
        @NotNull CacheMetrics items,
        @NotNull CacheMetrics entities,
        @NotNull CacheMetrics menus,
        @NotNull CacheMetrics blockEntities,
        @NotNull CacheMetrics fluids
    ) {
    }
}