package org.geysermc.hydraulic.block;

import org.geysermc.pack.converter.type.model.ModelStitcher;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import team.unnamed.creative.blockstate.BlockState;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

public final class StateDefinition {
        private static final AtomicLong CACHE_HITS = new AtomicLong();
        private static final AtomicLong CACHE_MISSES = new AtomicLong();

        private final @NotNull BlockState state;
        private final @NotNull ModelStitcher.Provider modelProvider;
        private final @NotNull ConcurrentMap<net.minecraft.world.level.block.state.BlockState, Optional<ModelDefinition>> resolvedModels = new ConcurrentHashMap<>();

        public StateDefinition(@NotNull BlockState state, @NotNull ModelStitcher.Provider modelProvider) {
                this.state = state;
                this.modelProvider = modelProvider;
        }

        @NotNull
        public BlockState state() {
                return this.state;
        }

        @NotNull
        public ModelStitcher.Provider modelProvider() {
                return this.modelProvider;
        }

        @NotNull
        public static CacheMetrics cacheMetrics() {
                return new CacheMetrics(CACHE_HITS.get(), CACHE_MISSES.get());
        }

        static void resetCacheMetrics() {
                CACHE_HITS.set(0);
                CACHE_MISSES.set(0);
        }

        @Nullable
        public ModelDefinition resolveModel(
                @NotNull net.minecraft.world.level.block.state.BlockState blockState,
                @NotNull Function<net.minecraft.world.level.block.state.BlockState, @Nullable ModelDefinition> resolver
        ) {
                final boolean[] cacheMiss = { false };
                ModelDefinition definition = this.resolvedModels.computeIfAbsent(blockState, ignored -> {
                        cacheMiss[0] = true;
                        return Optional.ofNullable(resolver.apply(ignored));
                }).orElse(null);

                if (cacheMiss[0]) {
                        CACHE_MISSES.incrementAndGet();
                } else {
                        CACHE_HITS.incrementAndGet();
                }

                return definition;
        }

        public record CacheMetrics(long hits, long misses) {
                public long requests() {
                        return this.hits + this.misses;
                }
        }
}