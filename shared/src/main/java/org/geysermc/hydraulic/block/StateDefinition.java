package org.geysermc.hydraulic.block;

import org.geysermc.pack.converter.type.model.ModelStitcher;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import team.unnamed.creative.blockstate.BlockState;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

public final class StateDefinition {
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

        @Nullable
        public ModelDefinition resolveModel(
                @NotNull net.minecraft.world.level.block.state.BlockState blockState,
                @NotNull Function<net.minecraft.world.level.block.state.BlockState, @Nullable ModelDefinition> resolver
        ) {
                return this.resolvedModels.computeIfAbsent(blockState, ignored -> Optional.ofNullable(resolver.apply(ignored))).orElse(null);
        }
}