package org.geysermc.hydraulic.metadata;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class BlockMapping {
    private final Identifier javaIdentifier;
    private final List<BlockStateRule> rules;
    private final ConcurrentMap<BlockState, MatchResult> resolvedStates = new ConcurrentHashMap<>();
    private final ConcurrentMap<StateDefinition<?, ?>, Map<String, Property<?>>> propertiesByDefinition = new ConcurrentHashMap<>();

    public BlockMapping(@NotNull Identifier javaIdentifier, @NotNull List<BlockStateRule> rules) {
        this.javaIdentifier = javaIdentifier;
        this.rules = List.copyOf(rules);
    }

    @NotNull
    public Identifier javaIdentifier() {
        return this.javaIdentifier;
    }

    @NotNull
    public List<BlockStateRule> rules() {
        return this.rules;
    }

    @NotNull
    public MatchResult resolve(@NotNull BlockState state) {
        return this.resolvedStates.computeIfAbsent(state, this::resolveUncached);
    }

    @Nullable
    public BlockStateRule findRule(@NotNull BlockState state) {
        return this.resolve(state).rule();
    }

    int cachedStateCount() {
        return this.resolvedStates.size();
    }

    @NotNull
    private MatchResult resolveUncached(@NotNull BlockState state) {
        Map<String, Property<?>> propertiesByName = this.propertiesFor(state);
        for (BlockStateRule rule : this.rules) {
            if (rule.matches(state, propertiesByName)) {
                Identifier resolvedIdentifier = rule.bedrockIdentifier() != null ? rule.bedrockIdentifier() : this.javaIdentifier;
                return new MatchResult(rule, resolvedIdentifier, rule.bedrockIdentifier() != null);
            }
        }
        return new MatchResult(null, this.javaIdentifier, false);
    }

    @NotNull
    private Map<String, Property<?>> propertiesFor(@NotNull BlockState state) {
        return this.propertiesByDefinition.computeIfAbsent(state.getBlock().getStateDefinition(), ignored -> {
            Map<String, Property<?>> propertiesByName = new LinkedHashMap<>();
            for (Property<?> property : state.getProperties()) {
                propertiesByName.put(property.getName(), property);
            }
            return Map.copyOf(propertiesByName);
        });
    }

    public record MatchResult(@Nullable BlockStateRule rule, @NotNull Identifier identifier, boolean overridden) {
    }
}