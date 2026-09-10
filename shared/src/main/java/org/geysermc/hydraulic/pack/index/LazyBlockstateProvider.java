package org.geysermc.hydraulic.pack.index;

import com.google.gson.JsonObject;
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
 * Lazy blockstate provider that deserializes blockstates on demand.
 * This replaces eager parsed-pack blockstate walks with indexed, bounded access.
 */
public final class LazyBlockstateProvider {
    private final Map<String, ModResourceIndex> modIndexes;
    private final Map<Identifier, JsonObject> cache;
    private final Logger logger;
    private final int maxCacheSize;
    private int cacheHits;
    private int cacheMisses;

    public LazyBlockstateProvider(@NotNull Map<String, ModResourceIndex> modIndexes, @NotNull Logger logger, int maxCacheSize) {
        this.modIndexes = Map.copyOf(modIndexes);
        this.cache = new LinkedHashMap<>();
        this.logger = logger;
        this.maxCacheSize = maxCacheSize;
        this.cacheHits = 0;
        this.cacheMisses = 0;
    }

    /**
     * Gets a blockstate for the given block identifier.
     * Returns null if the blockstate cannot be found or parsed.
     */
    @Nullable
    public JsonObject getBlockstate(@NotNull Identifier block) {
        JsonObject cached = this.cache.get(block);
        if (cached != null) {
            this.cacheHits++;
            return cached;
        }

        this.cacheMisses++;
        JsonObject loaded = this.loadBlockstate(block);
        if (loaded != null) {
            this.cache.put(block, loaded);
            this.evictIfNeeded();
        }
        return loaded;
    }

    /**
     * Checks if a blockstate exists for the given block identifier.
     */
    public boolean hasBlockstate(@NotNull Identifier block) {
        if (this.cache.containsKey(block)) {
            return true;
        }

        String namespace = block.getNamespace();
        for (ModResourceIndex index : this.modIndexes.values()) {
            if (index.namespaces().contains(namespace) && index.hasBlockState(block)) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    private JsonObject loadBlockstate(@NotNull Identifier block) {
        String namespace = block.getNamespace();
        for (ModResourceIndex index : this.modIndexes.values()) {
            if (!index.namespaces().contains(namespace)) {
                continue;
            }

            Path blockstatePath = index.resolveBlockStatePath(block);
            if (blockstatePath == null) {
                continue;
            }

            try {
                String content = Files.readString(blockstatePath);
                return com.google.gson.JsonParser.parseString(content).getAsJsonObject();
            } catch (IOException e) {
                this.logger.warn("Failed to load blockstate for {} from {}", block, blockstatePath, e);
            } catch (Exception e) {
                this.logger.warn("Failed to parse blockstate for {} from {}", block, blockstatePath, e);
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