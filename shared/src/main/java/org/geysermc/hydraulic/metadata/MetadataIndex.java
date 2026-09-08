package org.geysermc.hydraulic.metadata;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class MetadataIndex {
    private final Map<Identifier, BlockMapping> blockMappings;
    private final Map<Identifier, IdentifierMapping> itemMappings;
    private final Map<Identifier, IdentifierMapping> recipeMappings;
    private final Summary summary;

    public MetadataIndex(
        @NotNull Map<Identifier, BlockMapping> blockMappings,
        @NotNull Map<Identifier, IdentifierMapping> itemMappings,
        @NotNull Map<Identifier, IdentifierMapping> recipeMappings,
        @NotNull Summary summary
    ) {
        this.blockMappings = Collections.unmodifiableMap(new LinkedHashMap<>(blockMappings));
        this.itemMappings = Collections.unmodifiableMap(new LinkedHashMap<>(itemMappings));
        this.recipeMappings = Collections.unmodifiableMap(new LinkedHashMap<>(recipeMappings));
        this.summary = summary;
    }

    @NotNull
    public static MetadataIndex empty() {
        return new MetadataIndex(Map.of(), Map.of(), Map.of(), Summary.empty());
    }

    @Nullable
    public BlockMapping blockMapping(@NotNull Identifier javaIdentifier) {
        return this.blockMappings.get(javaIdentifier);
    }

    @NotNull
    public Map<Identifier, BlockMapping> blockMappings() {
        return this.blockMappings;
    }

    @Nullable
    public IdentifierMapping itemMapping(@NotNull Identifier javaIdentifier) {
        return this.itemMappings.get(javaIdentifier);
    }

    @NotNull
    public Map<Identifier, IdentifierMapping> itemMappings() {
        return this.itemMappings;
    }

    @Nullable
    public IdentifierMapping recipeMapping(@NotNull Identifier javaIdentifier) {
        return this.recipeMappings.get(javaIdentifier);
    }

    @NotNull
    public Map<Identifier, IdentifierMapping> recipeMappings() {
        return this.recipeMappings;
    }

    @Nullable
    public BlockStateRule blockRule(@NotNull Identifier javaIdentifier, @NotNull BlockState state) {
        BlockMapping mapping = this.blockMappings.get(javaIdentifier);
        if (mapping == null) {
            return null;
        }
        return mapping.findRule(state);
    }

    public boolean isEmpty() {
        return this.blockMappings.isEmpty() && this.itemMappings.isEmpty() && this.recipeMappings.isEmpty();
    }

    @NotNull
    public Summary summary() {
        return this.summary;
    }

    public record Summary(
        int fileCount,
        int blockMappingCount,
        int itemMappingCount,
        int recipeMappingCount,
        int ruleCount,
        @NotNull Map<String, Integer> ownershipFileCounts
    ) {
        public Summary {
            ownershipFileCounts = Collections.unmodifiableMap(new LinkedHashMap<>(ownershipFileCounts));
        }

        @NotNull
        public static Summary empty() {
            return new Summary(0, 0, 0, 0, 0, Map.of());
        }
    }
}