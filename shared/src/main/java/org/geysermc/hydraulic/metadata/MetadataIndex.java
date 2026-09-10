package org.geysermc.hydraulic.metadata;

import org.geysermc.hydraulic.compat.runtime.BlockEntityPatchTemplate;
import org.geysermc.hydraulic.compat.mapping.ContentPatch;
import org.geysermc.hydraulic.compat.runtime.MenuPatchTemplate;
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
    private final Map<Identifier, MenuPatchTemplate> menuPatchTemplates;
    private final Map<Identifier, BlockEntityPatchTemplate> blockEntityPatchTemplates;
    private final Map<String, java.util.List<Identifier>> blockMappingsByNamespace;
    private final Map<String, java.util.List<Identifier>> itemMappingsByNamespace;
    private final Map<String, java.util.List<Identifier>> recipeMappingsByNamespace;
    private final Map<String, java.util.List<Identifier>> entityMappingsByNamespace;
    private final Map<String, java.util.List<Identifier>> menuMappingsByNamespace;
    private final Map<String, Map<Identifier, java.util.List<ContentPatch>>> contentPatchesByNamespace;
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
        this.menuPatchTemplates = indexMenuPatchTemplates(this.contentPatches);
        this.blockEntityPatchTemplates = indexBlockEntityPatchTemplates(this.contentPatches);
        this.blockMappingsByNamespace = indexIdentifiersByNamespace(this.blockMappings.keySet());
        this.itemMappingsByNamespace = indexIdentifiersByNamespace(this.itemMappings.keySet());
        this.recipeMappingsByNamespace = indexIdentifiersByNamespace(this.recipeMappings.keySet());
        this.entityMappingsByNamespace = indexIdentifiersByNamespace(this.entityMappings.keySet());
        this.menuMappingsByNamespace = indexIdentifiersByNamespace(this.menuMappings.keySet());
        this.contentPatchesByNamespace = indexPatchesByNamespace(this.contentPatches);
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
    public java.util.Set<String> namespaces() {
        java.util.LinkedHashSet<String> namespaces = new java.util.LinkedHashSet<>();
        namespaces.addAll(this.blockMappingsByNamespace.keySet());
        namespaces.addAll(this.itemMappingsByNamespace.keySet());
        namespaces.addAll(this.recipeMappingsByNamespace.keySet());
        namespaces.addAll(this.entityMappingsByNamespace.keySet());
        namespaces.addAll(this.menuMappingsByNamespace.keySet());
        namespaces.addAll(this.contentPatchesByNamespace.keySet());
        return java.util.Set.copyOf(namespaces);
    }

    @NotNull
    public java.util.List<Identifier> blockMappings(@NotNull String namespace) {
        return this.blockMappingsByNamespace.getOrDefault(namespace, java.util.List.of());
    }

    @NotNull
    public java.util.List<Identifier> itemMappings(@NotNull String namespace) {
        return this.itemMappingsByNamespace.getOrDefault(namespace, java.util.List.of());
    }

    @NotNull
    public java.util.List<Identifier> recipeMappings(@NotNull String namespace) {
        return this.recipeMappingsByNamespace.getOrDefault(namespace, java.util.List.of());
    }

    @NotNull
    public java.util.List<Identifier> entityMappings(@NotNull String namespace) {
        return this.entityMappingsByNamespace.getOrDefault(namespace, java.util.List.of());
    }

    @NotNull
    public java.util.List<Identifier> menuMappings(@NotNull String namespace) {
        return this.menuMappingsByNamespace.getOrDefault(namespace, java.util.List.of());
    }

    @NotNull
    public Map<Identifier, java.util.List<ContentPatch>> contentPatches(@NotNull String namespace) {
        return this.contentPatchesByNamespace.getOrDefault(namespace, Map.of());
    }

    public boolean hasNamespaceEntries(@NotNull String namespace) {
        return this.blockMappingsByNamespace.containsKey(namespace)
            || this.itemMappingsByNamespace.containsKey(namespace)
            || this.recipeMappingsByNamespace.containsKey(namespace)
            || this.entityMappingsByNamespace.containsKey(namespace)
            || this.menuMappingsByNamespace.containsKey(namespace)
            || this.contentPatchesByNamespace.containsKey(namespace);
    }

    @NotNull
    public java.util.List<ContentPatch> contentPatches(@NotNull Identifier javaIdentifier) {
        return this.contentPatches.getOrDefault(javaIdentifier, java.util.List.of());
    }

    @Nullable
    public MenuPatchTemplate menuPatchTemplate(@NotNull Identifier javaIdentifier) {
        return this.menuPatchTemplates.get(javaIdentifier);
    }

    @Nullable
    public BlockEntityPatchTemplate blockEntityPatchTemplate(@NotNull Identifier javaIdentifier) {
        return this.blockEntityPatchTemplates.get(javaIdentifier);
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

    @NotNull
    private static Map<String, java.util.List<Identifier>> indexIdentifiersByNamespace(@NotNull java.util.Set<Identifier> identifiers) {
        Map<String, java.util.List<Identifier>> byNamespace = new LinkedHashMap<>();
        for (Identifier identifier : identifiers) {
            byNamespace.computeIfAbsent(identifier.getNamespace(), ignored -> new java.util.ArrayList<>()).add(identifier);
        }

        Map<String, java.util.List<Identifier>> finalized = new LinkedHashMap<>();
        for (Map.Entry<String, java.util.List<Identifier>> entry : byNamespace.entrySet()) {
            finalized.put(entry.getKey(), java.util.List.copyOf(entry.getValue()));
        }
        return Collections.unmodifiableMap(finalized);
    }

    @NotNull
    private static Map<String, Map<Identifier, java.util.List<ContentPatch>>> indexPatchesByNamespace(@NotNull Map<Identifier, java.util.List<ContentPatch>> contentPatches) {
        Map<String, Map<Identifier, java.util.List<ContentPatch>>> byNamespace = new LinkedHashMap<>();
        for (Map.Entry<Identifier, java.util.List<ContentPatch>> entry : contentPatches.entrySet()) {
            byNamespace.computeIfAbsent(entry.getKey().getNamespace(), ignored -> new LinkedHashMap<>()).put(entry.getKey(), entry.getValue());
        }

        Map<String, Map<Identifier, java.util.List<ContentPatch>>> finalized = new LinkedHashMap<>();
        for (Map.Entry<String, Map<Identifier, java.util.List<ContentPatch>>> entry : byNamespace.entrySet()) {
            finalized.put(entry.getKey(), Collections.unmodifiableMap(new LinkedHashMap<>(entry.getValue())));
        }
        return Collections.unmodifiableMap(finalized);
    }

    @NotNull
    private static Map<Identifier, MenuPatchTemplate> indexMenuPatchTemplates(@NotNull Map<Identifier, java.util.List<ContentPatch>> contentPatches) {
        Map<Identifier, MenuPatchTemplate> templates = new LinkedHashMap<>();
        for (Map.Entry<Identifier, java.util.List<ContentPatch>> entry : contentPatches.entrySet()) {
            MenuPatchTemplate template = MenuPatchTemplate.resolve(entry.getValue());
            if (template != null) {
                templates.put(entry.getKey(), template);
            }
        }
        return Collections.unmodifiableMap(templates);
    }

    @NotNull
    private static Map<Identifier, BlockEntityPatchTemplate> indexBlockEntityPatchTemplates(@NotNull Map<Identifier, java.util.List<ContentPatch>> contentPatches) {
        Map<Identifier, BlockEntityPatchTemplate> templates = new LinkedHashMap<>();
        for (Map.Entry<Identifier, java.util.List<ContentPatch>> entry : contentPatches.entrySet()) {
            BlockEntityPatchTemplate template = BlockEntityPatchTemplate.resolve(entry.getValue());
            if (template != null) {
                templates.put(entry.getKey(), template);
            }
        }
        return Collections.unmodifiableMap(templates);
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