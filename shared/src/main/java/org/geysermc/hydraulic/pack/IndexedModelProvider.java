package org.geysermc.hydraulic.pack;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalCause;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.kyori.adventure.key.Key;
import org.geysermc.pack.converter.type.model.ModelStitcher;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import team.unnamed.creative.metadata.pack.PackFormat;
import team.unnamed.creative.model.Model;
import team.unnamed.creative.serialize.minecraft.model.ModelSerializer;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

final class IndexedModelProvider implements ModelStitcher.Provider {
    private static final int DEFAULT_CACHE_SIZE = 4096;

    private final Logger logger;
    private final Map<Key, Path> modelPaths;
    private final @Nullable Path vanillaPackPath;
    private final LoadingCache<Key, Optional<Model>> modelCache;
    private final AtomicLong evictions = new AtomicLong();

    IndexedModelProvider(@NotNull Logger logger, @NotNull Map<Key, Path> modelPaths, @Nullable Path vanillaPackPath) {
        this(logger, modelPaths, vanillaPackPath, DEFAULT_CACHE_SIZE);
    }

    IndexedModelProvider(@NotNull Logger logger, @NotNull Map<Key, Path> modelPaths, @Nullable Path vanillaPackPath, int maximumCacheEntries) {
        this.logger = logger;
        this.modelPaths = Map.copyOf(new LinkedHashMap<>(modelPaths));
        this.vanillaPackPath = vanillaPackPath;
        this.modelCache = CacheBuilder.newBuilder()
            .maximumSize(Math.max(1, maximumCacheEntries))
            .recordStats()
            .removalListener(notification -> {
                if (notification.getCause() == RemovalCause.SIZE) {
                    this.evictions.incrementAndGet();
                }
            })
            .build(CacheLoader.from(this::loadModel));
    }

    @Override
    public @Nullable Model model(@NotNull Key key) {
        try {
            return this.modelCache.get(key).orElse(null);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            this.logger.error("Failed to resolve model {}", key, cause);
            return null;
        }
    }

    @NotNull
    CacheMetrics cacheMetrics() {
        return new CacheMetrics(
            this.modelCache.stats().hitCount(),
            this.modelCache.stats().missCount(),
            this.evictions.get(),
            this.modelCache.size(),
            this.modelPaths.size()
        );
    }

    private @NotNull Optional<Model> loadModel(@NotNull Key key) {
        Path modelPath = this.modelPaths.get(key);
        if (modelPath != null) {
            return Optional.ofNullable(this.parseModel(key, modelPath));
        }

        if (this.vanillaPackPath == null || !Files.isRegularFile(this.vanillaPackPath)) {
            return Optional.empty();
        }
        return Optional.ofNullable(this.parseVanillaModel(key));
    }

    private @Nullable Model parseModel(@NotNull Key key, @NotNull Path path) {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonElement json = JsonParser.parseReader(reader);
            return ModelSerializer.INSTANCE.deserializeFromJson(json, key, PackFormat.UNKNOWN);
        } catch (Exception e) {
            this.logger.error("Failed to load model {} from {}", key, path, e);
            return null;
        }
    }

    private @Nullable Model parseVanillaModel(@NotNull Key key) {
        String entryName = "assets/" + key.namespace() + "/models/" + key.value() + ".json";
        try (ZipFile zip = new ZipFile(this.vanillaPackPath.toFile())) {
            ZipEntry entry = zip.getEntry(entryName);
            if (entry == null) {
                return null;
            }

            try (InputStreamReader reader = new InputStreamReader(zip.getInputStream(entry), StandardCharsets.UTF_8)) {
                JsonElement json = JsonParser.parseReader(reader);
                return ModelSerializer.INSTANCE.deserializeFromJson(json, key, PackFormat.UNKNOWN);
            }
        } catch (IOException e) {
            this.logger.error("Failed to load vanilla model {} from {}", key, this.vanillaPackPath, e);
            return null;
        }
    }

    record CacheMetrics(long hits, long misses, long evictions, long size, long indexedModels) {
        long requests() {
            return this.hits + this.misses;
        }
    }
}