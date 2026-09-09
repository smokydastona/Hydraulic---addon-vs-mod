package org.geysermc.hydraulic.compat;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import org.geysermc.hydraulic.metadata.BlockMapping;
import org.geysermc.hydraulic.metadata.BlockStateRule;
import org.geysermc.hydraulic.metadata.IdentifierMapping;
import org.geysermc.hydraulic.metadata.MetadataIndex;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MappingResolver {
    private final MetadataIndex metadataIndex;

    public MappingResolver(@NotNull MetadataIndex metadataIndex) {
        this.metadataIndex = metadataIndex;
    }

    @Nullable
    public BlockMapping blockMapping(@NotNull Identifier javaIdentifier) {
        return this.metadataIndex.blockMapping(javaIdentifier);
    }

    @Nullable
    public BlockStateRule blockRule(@NotNull Identifier javaIdentifier, @NotNull BlockState state) {
        return this.metadataIndex.blockRule(javaIdentifier, state);
    }

    @Nullable
    public IdentifierMapping itemMapping(@NotNull Identifier javaIdentifier) {
        return this.metadataIndex.itemMapping(javaIdentifier);
    }

    @Nullable
    public IdentifierMapping recipeMapping(@NotNull Identifier javaIdentifier) {
        return this.metadataIndex.recipeMapping(javaIdentifier);
    }

    @Nullable
    public IdentifierMapping entityMapping(@NotNull Identifier javaIdentifier) {
        return this.metadataIndex.entityMapping(javaIdentifier);
    }

    @Nullable
    public IdentifierMapping menuMapping(@NotNull Identifier javaIdentifier) {
        return this.metadataIndex.menuMapping(javaIdentifier);
    }

    @NotNull
    public ResolvedBlockState resolveBlockState(@NotNull Identifier javaIdentifier, @NotNull BlockState state) {
        BlockMapping mapping = this.blockMapping(javaIdentifier);
        if (mapping == null) {
            return new ResolvedBlockState(javaIdentifier, null, new BlockMapping.RuntimeMetadata(Map.of(), null, null, false, null), false);
        }

        BlockMapping.MatchResult resolved = mapping.resolve(state);
        return new ResolvedBlockState(resolved.identifier(), resolved.rule(), resolved.metadata(), resolved.overridden());
    }

    @NotNull
    public List<ResolvedBlockDefinition> resolveBlockDefinitions(@NotNull Identifier javaIdentifier, @NotNull Iterable<BlockState> states) {
        Map<Identifier, List<BlockState>> groupedStates = new LinkedHashMap<>();
        Map<Identifier, Boolean> overridden = new LinkedHashMap<>();

        for (BlockState state : states) {
            ResolvedBlockState resolved = this.resolveBlockState(javaIdentifier, state);
            groupedStates.computeIfAbsent(resolved.identifier(), ignored -> new ArrayList<>()).add(state);
            overridden.merge(resolved.identifier(), resolved.overridden(), Boolean::logicalOr);
        }

        List<ResolvedBlockDefinition> definitions = new ArrayList<>();
        for (Map.Entry<Identifier, List<BlockState>> entry : groupedStates.entrySet()) {
            definitions.add(new ResolvedBlockDefinition(entry.getKey(), List.copyOf(entry.getValue()), overridden.getOrDefault(entry.getKey(), false)));
        }
        return List.copyOf(definitions);
    }

    @NotNull
    public ResolvedIdentifier resolveBlockIdentifier(@NotNull Identifier javaIdentifier, @NotNull Iterable<BlockState> states) {
        List<ResolvedBlockDefinition> definitions = this.resolveBlockDefinitions(javaIdentifier, states);
        if (definitions.isEmpty()) {
            return new ResolvedIdentifier(javaIdentifier, false, false);
        }

        if (definitions.size() > 1) {
            boolean overridden = definitions.stream().anyMatch(ResolvedBlockDefinition::overridden);
            return new ResolvedIdentifier(javaIdentifier, overridden, true);
        }

        ResolvedBlockDefinition resolved = definitions.getFirst();
        if (!resolved.overridden()) {
            return new ResolvedIdentifier(javaIdentifier, false, false);
        }

        return new ResolvedIdentifier(resolved.identifier(), true, false);
    }

    @NotNull
    public ResolvedIdentifier resolveItemIdentifier(@NotNull Identifier javaIdentifier) {
        IdentifierMapping mapping = this.itemMapping(javaIdentifier);
        if (mapping == null) {
            return new ResolvedIdentifier(javaIdentifier, false, false);
        }
        return new ResolvedIdentifier(mapping.bedrockIdentifier(), true, false);
    }

    @NotNull
    public ResolvedIdentifier resolveRecipeIdentifier(@NotNull Identifier javaIdentifier) {
        IdentifierMapping mapping = this.recipeMapping(javaIdentifier);
        if (mapping == null) {
            return new ResolvedIdentifier(javaIdentifier, false, false);
        }
        return new ResolvedIdentifier(mapping.bedrockIdentifier(), true, false);
    }

    @NotNull
    public ResolvedIdentifier resolveEntityIdentifier(@NotNull Identifier javaIdentifier) {
        IdentifierMapping mapping = this.entityMapping(javaIdentifier);
        if (mapping == null) {
            return new ResolvedIdentifier(javaIdentifier, false, false);
        }
        return new ResolvedIdentifier(mapping.bedrockIdentifier(), true, false);
    }

    @NotNull
    public ResolvedIdentifier resolveMenuIdentifier(@NotNull Identifier javaIdentifier) {
        IdentifierMapping mapping = this.menuMapping(javaIdentifier);
        if (mapping == null) {
            return new ResolvedIdentifier(javaIdentifier, false, false);
        }
        return new ResolvedIdentifier(mapping.bedrockIdentifier(), true, false);
    }

    public record ResolvedBlockState(@NotNull Identifier identifier, @Nullable BlockStateRule rule, @NotNull BlockMapping.RuntimeMetadata metadata, boolean overridden) {
    }

    public record ResolvedBlockDefinition(@NotNull Identifier identifier, @NotNull List<BlockState> states, boolean overridden) {
        @NotNull
        public BlockState representativeState(@NotNull BlockState fallback) {
            return this.states.contains(fallback) ? fallback : this.states.getFirst();
        }
    }

    public record ResolvedIdentifier(@NotNull Identifier identifier, boolean overridden, boolean conflicting) {
    }
}