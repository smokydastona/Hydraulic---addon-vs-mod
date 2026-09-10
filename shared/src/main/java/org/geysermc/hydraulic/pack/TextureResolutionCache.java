package org.geysermc.hydraulic.pack;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalCause;
import net.kyori.adventure.key.Key;
import org.apache.commons.lang3.StringUtils;
import org.geysermc.hydraulic.Constants;
import org.geysermc.pack.converter.type.texture.TextureConverter;
import org.geysermc.pack.converter.util.JsonMappings;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicLong;

final class TextureResolutionCache {
    private static final int DEFAULT_CACHE_SIZE = 4096;

    private final LoadingCache<ResolutionKey, String> outputCache;
    private final AtomicLong evictions = new AtomicLong();

    TextureResolutionCache() {
        this(DEFAULT_CACHE_SIZE);
    }

    TextureResolutionCache(int maximumSize) {
        this.outputCache = CacheBuilder.newBuilder()
            .maximumSize(Math.max(1, maximumSize))
            .recordStats()
            .removalListener(notification -> {
                if (notification.getCause() == RemovalCause.SIZE) {
                    this.evictions.incrementAndGet();
                }
            })
            .build(CacheLoader.from(this::resolveUncached));
    }

    @NotNull
    String resolveModelOutput(@NotNull String modId, @NotNull Key key) {
        return this.outputCache.getUnchecked(new ResolutionKey(ResolutionKind.MODEL, modId, key.namespace(), key.value()));
    }

    @NotNull
    String resolveBlockTextureOutput(@NotNull String modId, @NotNull Key key) {
        return this.outputCache.getUnchecked(new ResolutionKey(ResolutionKind.BLOCK_TEXTURE, modId, key.namespace(), key.value()));
    }

    @NotNull
    CacheMetrics metrics() {
        return new CacheMetrics(
            this.outputCache.stats().hitCount(),
            this.outputCache.stats().missCount(),
            this.evictions.get(),
            this.outputCache.size()
        );
    }

    @NotNull
    private String resolveUncached(@NotNull ResolutionKey key) {
        return switch (key.kind()) {
            case MODEL -> resolveModelOutputUncached(key.modId(), key.value());
            case BLOCK_TEXTURE -> resolveBlockTextureOutputUncached(key.modId(), key.value());
        };
    }

    @NotNull
    private static String resolveModelOutputUncached(@NotNull String modId, @NotNull String value) {
        String normalizedValue = normalizeTextureValue(value);
        String mappedValue = JsonMappings.getMapping("textures").map(normalizedValue).stream()
            .findFirst()
            .orElse(normalizedValue);
        String directory = StringUtils.substringBefore(mappedValue, "/");
        String remaining = StringUtils.substringAfter(mappedValue, "/");
        String finalDir = TextureConverter.DIRECTORY_LOCATIONS.getOrDefault(directory, directory) + "/" + modId;
        return String.format(Constants.BEDROCK_TEXTURE_LOCATION, finalDir + "/" + remaining);
    }

    @NotNull
    private static String resolveBlockTextureOutputUncached(@NotNull String modId, @NotNull String value) {
        String cleanPath = value.replace("block/", "").replace(".png", "");
        return String.format(Constants.BEDROCK_TEXTURE_LOCATION, "blocks/" + modId + "/" + cleanPath);
    }

    @NotNull
    private static String normalizeTextureValue(@NotNull String value) {
        String normalized = value.replace('\\', '/');
        if (normalized.startsWith("textures/")) {
            normalized = normalized.substring("textures/".length());
        }
        if (normalized.endsWith(".png")) {
            return normalized.substring(0, normalized.length() - 4);
        }
        if (normalized.endsWith(".tga")) {
            return normalized.substring(0, normalized.length() - 4);
        }
        return normalized;
    }

    record CacheMetrics(long hits, long misses, long evictions, long size) {
        long requests() {
            return this.hits + this.misses;
        }
    }

    private record ResolutionKey(@NotNull ResolutionKind kind, @NotNull String modId, @NotNull String namespace, @NotNull String value) {
    }

    private enum ResolutionKind {
        MODEL,
        BLOCK_TEXTURE
    }
}