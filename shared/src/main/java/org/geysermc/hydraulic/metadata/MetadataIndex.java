package org.geysermc.hydraulic.metadata;

import org.geysermc.hydraulic.compat.mapping.ContentPatch;
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
    private final Map<Identifier, IdentifierMapping> entityMappings;
    private final Map<Identifier, IdentifierMapping> menuMappings;
    private final Map<Identifier, java.util.List<ContentPatch>> contentPatches;
    private final java.util.List<MetadataValidationIssue> validationIssues;
    private final Summary summary;

    public MetadataIndex(
        @NotNull Map<Identifier, BlockMapping> blockMappings,
        @NotNull Map<Identifier, IdentifierMapping> itemMappings,
        @NotNull Map<Identifier, IdentifierMapping> recipeMappings,
        @NotNull Map<Identifier, IdentifierMapping> entityMappings,
        @NotNull Map<Identifier, IdentifierMapping> menuMappings,
        @NotNull Map<Identifier, java.util.List<ContentPatch>> contentPatches,
        @NotNull java.util.List<MetadataValidationIssue> validationIssues,
        @NotNull Summary summary
    ) {
        this.blockMappings = Collections.unmodifiableMap(new LinkedHashMap<>(blockMappings));
        this.itemMappings = Collections.unmodifiableMap(new LinkedHashMap<>(itemMappings));
        this.recipeMappings = Collections.unmodifiableMap(new LinkedHashMap<>(recipeMappings));
        this.entityMappings = Collections.unmodifiableMap(new LinkedHashMap<>(entityMappings));
        this.menuMappings = Collections.unmodifiableMap(new LinkedHashMap<>(menuMappings));
        Map<Identifier, java.util.List<ContentPatch>> patchCopy = new LinkedHashMap<>();
        for (Map.Entry<Identifier, java.util.List<ContentPatch>> entry : contentPatches.entrySet()) {
            patchCopy.put(entry.getKey(), java.util.List.copyOf(entry.getValue()));
        }
        this.contentPatches = Collections.unmodifiableMap(patchCopy);
        this.validationIssues = java.util.List.copyOf(validationIssues);
        this.summary = summary;
    }

    @NotNull
    public static MetadataIndex empty() {
        return new MetadataIndex(Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), java.util.List.of(), Summary.empty());
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
    public IdentifierMapping entityMapping(@NotNull Identifier javaIdentifier) {
        return this.entityMappings.get(javaIdentifier);
    }

    @NotNull
    public Map<Identifier, IdentifierMapping> entityMappings() {
        return this.entityMappings;
    }

    @Nullable
    public IdentifierMapping menuMapping(@NotNull Identifier javaIdentifier) {
        return this.menuMappings.get(javaIdentifier);
    }

    @NotNull
    public Map<Identifier, IdentifierMapping> menuMappings() {
        return this.menuMappings;
    }

    @NotNull
    public Map<Identifier, java.util.List<ContentPatch>> contentPatches() {
        return this.contentPatches;
    }

    @NotNull
    public java.util.List<ContentPatch> contentPatches(@NotNull Identifier javaIdentifier) {
        return this.contentPatches.getOrDefault(javaIdentifier, java.util.List.of());
    }

    @NotNull
    public java.util.List<MetadataValidationIssue> validationIssues() {
        return this.validationIssues;
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
        return this.blockMappings.isEmpty()
            && this.itemMappings.isEmpty()
            && this.recipeMappings.isEmpty()
            && this.entityMappings.isEmpty()
            && this.menuMappings.isEmpty()
            && this.contentPatches.isEmpty();
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
        int entityMappingCount,
        int menuMappingCount,
        int patchCount,
        int ruleCount,
        int validationIssueCount,
        @NotNull Map<String, Integer> ownershipFileCounts
    ) {
        public Summary {
            ownershipFileCounts = Collections.unmodifiableMap(new LinkedHashMap<>(ownershipFileCounts));
        }

        @NotNull
        public static Summary empty() {
            return new Summary(0, 0, 0, 0, 0, 0, 0, 0, 0, Map.of());
        }
    }
}