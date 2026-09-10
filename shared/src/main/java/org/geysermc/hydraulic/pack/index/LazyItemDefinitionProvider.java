package org.geysermc.hydraulic.pack.index;

import com.google.gson.JsonElement;
import net.minecraft.resources.Identifier;
import org.geysermc.hydraulic.pack.ModResourceIndex;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Lazy item definition provider that deserializes modern item definitions on demand.
 * This replaces eager parsed-pack item definition walks with indexed, bounded access.
 */
public final class LazyItemDefinitionProvider {
    private final Map<String, ModResourceIndex> modIndexes;
    private final Map<Identifier, JsonElement> cache;
    private final Logger logger;
    private final int maxCacheSize;
    private int cacheHits;
    private int cacheMisses;

    public LazyItemDefinitionProvider(@NotNull Map<String, ModResourceIndex> modIndexes, @NotNull Logger logger, int maxCacheSize) {
        this.modIndexes = Map.copyOf(modIndexes);
        this.cache = new LinkedHashMap<>();
        this.logger = logger;
        this.maxCacheSize = maxCacheSize;
        this.cacheHits = 0;
        this.cacheMisses = 0;
    }

    /**
     * Gets an item definition for the given item identifier.
     * Returns null if the item definition cannot be found or parsed.
     */
    @Nullable
    public JsonElement getItemDefinition(@NotNull Identifier item) {
        JsonElement cached = this.cache.get(item);
        if (cached != null) {
            this.cacheHits++;
            return cached;
        }

        this.cacheMisses++;
        JsonElement loaded = this.loadItemDefinition(item);
        if (loaded != null) {
            this.cache.put(item, loaded);
            this.evictIfNeeded();
        }
        return loaded;
    }

    /**
     * Checks if an item definition exists for the given item identifier.
     */
    public boolean hasItemDefinition(@NotNull Identifier item) {
        if (this.cache.containsKey(item)) {
            return true;
        }

        String namespace = item.getNamespace();
        for (ModResourceIndex index : this.modIndexes.values()) {
            if (index.namespaces().contains(namespace) && index.hasItemAsset(item)) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    private JsonElement loadItemDefinition(@NotNull Identifier item) {
        String namespace = item.getNamespace();
        for (ModResourceIndex index : this.modIndexes.values()) {
            if (!index.namespaces().contains(namespace)) {
                continue;
            }

            Path itemAssetPath = index.resolveItemAssetPath(item);
            if (itemAssetPath == null) {
                continue;
            }

            try {
                String content = Files.readString(itemAssetPath);
                return com.google.gson.JsonParser.parseString(content);
            } catch (IOException e) {
                this.logger.warn("Failed to load item definition for {} from {}", item, itemAssetPath, e);
            } catch (Exception e) {
                this.logger.warn("Failed to parse item definition for {} from {}", item, itemAssetPath, e);
            }
        }
        return null;
    }

    private void evictIfNeeded() {
        if (this.cache.size() <= this.maxCacheSize) {
            return;
        }

        // Simple LRU eviction by removing the oldest entry
        Identifier oldest = this.cache.keySet().iterator().next();
        this.cache.remove(oldest);
    }

    /**
     * Cache metrics for monitoring.
     */
    public record CacheMetrics(int hits, int misses, int size, int maxSize) {
    }

    @NotNull
    public CacheMetrics cacheMetrics() {
        return new CacheMetrics(this.cacheHits, this.cacheMisses, this.cache.size(), this.maxCacheSize);
    }

    /**
     * Clears the cache.
     */
    public void clear() {
        this.cache.clear();
        this.cacheHits = 0;
        this.cacheMisses = 0;
    }
}