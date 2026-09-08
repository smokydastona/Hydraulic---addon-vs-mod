package org.geysermc.hydraulic.compat;

import com.google.common.collect.ListMultimap;
import net.minecraft.SharedConstants;
import net.minecraft.core.DefaultedRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import org.geysermc.hydraulic.compat.analysis.BlockAnalyzer;
import org.geysermc.hydraulic.compat.analysis.BlockEntityAnalyzer;
import org.geysermc.hydraulic.compat.analysis.CompatibilityAnalyzer;
import org.geysermc.hydraulic.compat.analysis.EntityAnalyzer;
import org.geysermc.hydraulic.compat.analysis.FluidAnalyzer;
import org.geysermc.hydraulic.compat.analysis.ItemAnalyzer;
import org.geysermc.hydraulic.compat.analysis.MenuAnalyzer;
import org.geysermc.hydraulic.compat.analysis.RecipeAnalyzer;
import org.geysermc.hydraulic.compat.mapping.ContentPatch;
import org.geysermc.hydraulic.compat.model.CompatibilityFinding;
import org.geysermc.hydraulic.compat.model.CompatibilityObject;
import org.geysermc.hydraulic.compat.model.ModFingerprint;
import org.geysermc.hydraulic.compat.model.SupportLevel;
import org.geysermc.hydraulic.compat.model.SupportResult;
import org.geysermc.hydraulic.Constants;
import org.geysermc.hydraulic.metadata.MetadataIndex;
import org.geysermc.hydraulic.metadata.MetadataValidationIssue;
import org.geysermc.hydraulic.platform.mod.ModInfo;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

public final class CompatibilityManager {
    private static final List<CompatibilityAnalyzer> ANALYZERS = List.of(
        new BlockAnalyzer(),
        new ItemAnalyzer(),
        new EntityAnalyzer(),
        new FluidAnalyzer(),
        new BlockEntityAnalyzer(),
        new MenuAnalyzer(),
        new RecipeAnalyzer()
    );

    private final Logger logger;
    private final Path dataPath;

    public CompatibilityManager(@NotNull Logger logger, @NotNull Path dataPath) {
        this.logger = logger;
        this.dataPath = dataPath;
    }

    @NotNull
    public CompatibilityRegistry initialize(
        @NotNull Collection<ModInfo> mods,
        @NotNull ListMultimap<String, ModInfo> namespacesToMods,
        @NotNull ListMultimap<String, Identifier> modsToBlocks,
        @NotNull ListMultimap<String, Identifier> modsToItems,
        @NotNull MetadataIndex metadataIndex,
        @NotNull Predicate<ModInfo> ignored
    ) {
        ContentInventory inventory = this.buildInventory(mods, namespacesToMods, modsToBlocks, modsToItems, metadataIndex, ignored);
        CompatibilityReport report = this.buildReport(inventory, metadataIndex);
        this.writeJson(this.dataPath.resolve("reports/content-inventory.json"), inventory);
        this.writeJson(this.dataPath.resolve("reports/compatibility-report.json"), report);
        return new CompatibilityRegistry(metadataIndex, new MappingResolver(metadataIndex), inventory, report);
    }

    @NotNull
    private ContentInventory buildInventory(
        @NotNull Collection<ModInfo> mods,
        @NotNull ListMultimap<String, ModInfo> namespacesToMods,
        @NotNull ListMultimap<String, Identifier> modsToBlocks,
        @NotNull ListMultimap<String, Identifier> modsToItems,
        @NotNull MetadataIndex metadataIndex,
        @NotNull Predicate<ModInfo> ignored
    ) {
        Map<String, MutableInventory> inventories = new LinkedHashMap<>();
        for (ModInfo mod : mods) {
            if (ignored.test(mod)) {
                continue;
            }
            inventories.put(mod.id(), new MutableInventory(mod));
        }

        this.incrementRegistry(inventories, namespacesToMods, BuiltInRegistries.BLOCK.keySet(), "blocks");
        this.incrementRegistry(inventories, namespacesToMods, BuiltInRegistries.ITEM.keySet(), "items");
        this.incrementRegistry(inventories, namespacesToMods, BuiltInRegistries.ENTITY_TYPE.keySet(), "entities");
        this.incrementRegistry(inventories, namespacesToMods, BuiltInRegistries.FLUID.keySet(), "fluids");
        this.incrementRegistry(inventories, namespacesToMods, BuiltInRegistries.BLOCK_ENTITY_TYPE.keySet(), "block_entities");
        this.incrementRegistry(inventories, namespacesToMods, BuiltInRegistries.SOUND_EVENT.keySet(), "sound_events");
        this.incrementRegistry(inventories, namespacesToMods, BuiltInRegistries.PARTICLE_TYPE.keySet(), "particles");
        this.incrementMenuRegistry(inventories, namespacesToMods);

        for (MutableInventory inventory : inventories.values()) {
            this.scanModAssets(inventory);
            inventory.assetKeys.computeIfAbsent("block_assets", key -> new LinkedHashSet<>()).addAll(modsToBlocks.get(inventory.mod.id()).stream().map(Identifier::toString).toList());
            inventory.assetKeys.computeIfAbsent("item_assets", key -> new LinkedHashSet<>()).addAll(modsToItems.get(inventory.mod.id()).stream().map(Identifier::toString).toList());
        }

        for (String namespace : metadataIndex.namespaces()) {
            for (ModInfo mod : namespacesToMods.get(namespace)) {
                MutableInventory inventory = inventories.get(mod.id());
                if (inventory == null) {
                    continue;
                }

                for (Identifier javaId : metadataIndex.blockMappings(namespace)) {
                    inventory.addMetadata("blocks", javaId.toString());
                }
                for (Identifier javaId : metadataIndex.itemMappings(namespace)) {
                    inventory.addMetadata("items", javaId.toString());
                }
                for (Identifier javaId : metadataIndex.recipeMappings(namespace)) {
                    inventory.addMetadata("recipes", javaId.toString());
                }
                for (Identifier javaId : metadataIndex.entityMappings(namespace)) {
                    inventory.addMetadata("entities", javaId.toString());
                }
                for (Identifier javaId : metadataIndex.menuMappings(namespace)) {
                    inventory.addMetadata("menus", javaId.toString());
                }
                for (Map.Entry<Identifier, List<ContentPatch>> entry : metadataIndex.contentPatches(namespace).entrySet()) {
                    String kind = this.inferPatchKind(inventory, entry.getKey(), entry.getValue());
                    inventory.addPatch(kind, entry.getKey().toString());
                }
            }
        }

        Map<String, ContentInventory.ModContentInventory> finalized = new LinkedHashMap<>();
        for (MutableInventory inventory : inventories.values()) {
            finalized.put(inventory.mod.id(), inventory.freeze());
        }
        return new ContentInventory(finalized);
    }

    private void incrementRegistry(
        @NotNull Map<String, MutableInventory> inventories,
        @NotNull ListMultimap<String, ModInfo> namespacesToMods,
        @NotNull Iterable<Identifier> identifiers,
        @NotNull String category
    ) {
        for (Identifier identifier : identifiers) {
            if (identifier.getNamespace().equals("minecraft")) {
                continue;
            }

            for (ModInfo mod : namespacesToMods.get(identifier.getNamespace())) {
                MutableInventory inventory = inventories.get(mod.id());
                if (inventory != null) {
                    inventory.incrementRegistry(category, identifier.toString());
                }
            }
        }
    }

    private void incrementMenuRegistry(
        @NotNull Map<String, MutableInventory> inventories,
        @NotNull ListMultimap<String, ModInfo> namespacesToMods
    ) {
        try {
            java.lang.reflect.Field menuField = BuiltInRegistries.class.getField("MENU");
            Object menuRegistry = menuField.get(null);
            if (menuRegistry instanceof DefaultedRegistry<?> registry) {
                this.incrementRegistry(inventories, namespacesToMods, registry.keySet(), "menus");
            }
        } catch (ReflectiveOperationException e) {
            this.logger.debug("Menu registry is unavailable on this runtime", e);
        }
    }

    private void scanModAssets(@NotNull MutableInventory inventory) {
        for (Path root : inventory.mod.roots()) {
            Path assetsRoot = root.resolve("assets");
            if (Files.isDirectory(assetsRoot)) {
                try (Stream<Path> namespaces = Files.list(assetsRoot)) {
                    for (Path namespacePath : namespaces.filter(Files::isDirectory).toList()) {
                        this.scanAssetNamespace(inventory, namespacePath);
                    }
                } catch (IOException e) {
                    this.logger.warn("Failed to scan assets for mod {}", inventory.mod.id(), e);
                }
            }

            Path dataRoot = root.resolve("data");
            if (Files.isDirectory(dataRoot)) {
                try (Stream<Path> namespaces = Files.list(dataRoot)) {
                    for (Path namespacePath : namespaces.filter(Files::isDirectory).toList()) {
                        this.scanDataNamespace(inventory, namespacePath);
                    }
                } catch (IOException e) {
                    this.logger.warn("Failed to scan data for mod {}", inventory.mod.id(), e);
                }
            }
        }
    }

    private void scanAssetNamespace(@NotNull MutableInventory inventory, @NotNull Path namespacePath) {
        inventory.addAssets("blockstates", namespacePath.resolve("blockstates"), path -> path.toString().endsWith(".json"));
        inventory.addAssets("item_models", namespacePath.resolve("items"), path -> path.toString().endsWith(".json"));
        inventory.addAssets("models", namespacePath.resolve("models"), path -> path.toString().endsWith(".json"));
        inventory.addAssets("textures", namespacePath.resolve("textures"), CompatibilityManager::isTextureAsset);
        inventory.addAssets("sounds", namespacePath.resolve("sounds"), CompatibilityManager::isSoundAsset);
        inventory.addAssets("lang", namespacePath.resolve("lang"), path -> path.toString().endsWith(".json"));
    }

    private void scanDataNamespace(@NotNull MutableInventory inventory, @NotNull Path namespacePath) {
        inventory.addIdentifierAssets("recipes", namespacePath.resolve("recipes"), namespacePath.getFileName().toString(), path -> path.toString().endsWith(".json"));
        inventory.addAssets("tags", namespacePath.resolve("tags"), path -> path.toString().endsWith(".json"));
        inventory.addAssets("loot_tables", namespacePath.resolve("loot_tables"), path -> path.toString().endsWith(".json"));
    }

    private static boolean isTextureAsset(@NotNull Path path) {
        String value = path.toString().toLowerCase();
        return value.endsWith(".png") || value.endsWith(".tga");
    }

    private static boolean isSoundAsset(@NotNull Path path) {
        String value = path.toString().toLowerCase();
        return value.endsWith(".ogg") || value.endsWith(".wav") || value.endsWith(".fsb");
    }

    @NotNull
    CompatibilityReport buildReport(@NotNull ContentInventory inventory, @NotNull MetadataIndex metadataIndex) {
        Map<String, CompatibilityProfile> profiles = new LinkedHashMap<>();
        List<CompatibilityFinding> metadataFindings = metadataIndex.validationIssues().stream().map(this::toFinding).toList();
        for (ContentInventory.ModContentInventory modInventory : inventory.mods().values()) {
            List<CompatibilityObject> objects = this.analyzeObjects(modInventory, metadataIndex);
            Map<String, SupportResult> supportResults = aggregateSupportResults(objects);
            List<String> notes = new ArrayList<>();
            notes.add("Compatibility output now combines inventory-backed facts, analyzer heuristics, metadata mappings, and Metadata V2 patches.");
            notes.add("Behavior and runtime interaction domains remain conservative until dedicated bridges are implemented.");

            List<CompatibilityFinding> findings = new ArrayList<>(objects.stream().flatMap(object -> object.findings().stream()).toList());
            findings.addAll(metadataFindings.stream().filter(finding -> appliesToMod(finding, modInventory)).toList());
            Map<String, Integer> levelCounts = countLevels(objects);

            profiles.put(
                modInventory.modId(),
                new CompatibilityProfile(
                    modInventory.modId(),
                    modInventory.fingerprint(),
                    overallLevel(supportResults),
                    overallStatus(supportResults),
                    overallScore(supportResults),
                    supportResults,
                    levelCounts,
                    objects,
                    findings,
                    notes
                )
            );
        }

        return new CompatibilityReport(Instant.now().toString(), metadataIndex.summary(), metadataFindings, profiles);
    }

    @NotNull
    private List<CompatibilityObject> analyzeObjects(@NotNull ContentInventory.ModContentInventory modInventory, @NotNull MetadataIndex metadataIndex) {
        List<CompatibilityObject> objects = new ArrayList<>();
        for (ContentInventory.ContentDescriptor descriptor : modInventory.contentDescriptors()) {
            CompatibilityAnalyzer analyzer = ANALYZERS.stream()
                .filter(candidate -> candidate.supports(descriptor))
                .findFirst()
                .orElse(null);
            if (analyzer == null) {
                continue;
            }
            objects.add(analyzer.analyze(descriptor, modInventory, metadataIndex));
        }
        return List.copyOf(objects);
    }

    @NotNull
    private static Map<String, SupportResult> aggregateSupportResults(@NotNull List<CompatibilityObject> objects) {
        Map<String, List<SupportResult>> grouped = new LinkedHashMap<>();
        for (CompatibilityObject object : objects) {
            for (Map.Entry<String, SupportResult> entry : object.supportResults().entrySet()) {
                grouped.computeIfAbsent(entry.getKey(), ignored -> new ArrayList<>()).add(entry.getValue());
            }
        }

        Map<String, SupportResult> aggregated = new LinkedHashMap<>();
        for (Map.Entry<String, List<SupportResult>> entry : grouped.entrySet()) {
            List<SupportResult> values = entry.getValue();
            int scored = 0;
            int totalScore = 0;
            boolean hasComplete = false;
            boolean hasPartial = false;
            boolean hasNone = false;
            boolean hasUnsupported = false;
            boolean hasAdapted = false;
            boolean hasApproximated = false;
            boolean hasVisualOnly = false;
            Set<String> supported = new LinkedHashSet<>();
            Set<String> missing = new LinkedHashSet<>();
            List<String> notes = new ArrayList<>();

            for (SupportResult result : values) {
                if (result.scorePercent() != null) {
                    scored++;
                    totalScore += result.scorePercent();
                }
                hasComplete |= result.status() == CompatibilityStatus.COMPLETE;
                hasPartial |= result.status() == CompatibilityStatus.PARTIAL;
                hasNone |= result.status() == CompatibilityStatus.NONE;
                hasUnsupported |= result.level() == SupportLevel.UNSUPPORTED;
                hasVisualOnly |= result.level() == SupportLevel.VISUAL_ONLY;
                hasApproximated |= result.level() == SupportLevel.APPROXIMATED;
                hasAdapted |= result.level() == SupportLevel.ADAPTED;
                supported.addAll(result.supportedCapabilities());
                missing.addAll(result.missingCapabilities());
                notes.addAll(result.notes());
            }

            CompatibilityStatus status = hasNone || hasPartial ? CompatibilityStatus.PARTIAL : hasComplete ? CompatibilityStatus.COMPLETE : CompatibilityStatus.UNKNOWN;
            SupportLevel level = hasUnsupported ? SupportLevel.UNSUPPORTED : hasVisualOnly ? SupportLevel.VISUAL_ONLY : hasApproximated ? SupportLevel.APPROXIMATED : hasAdapted ? SupportLevel.ADAPTED : SupportLevel.AUTOMATIC;
            aggregated.put(entry.getKey(), new SupportResult(entry.getKey(), level, status, scored == 0 ? null : (int) Math.round(totalScore / (double) scored), List.copyOf(supported), List.copyOf(missing), List.copyOf(notes)));
        }
        return aggregated;
    }

    @NotNull
    private static Map<String, Integer> countLevels(@NotNull List<CompatibilityObject> objects) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (CompatibilityObject object : objects) {
            counts.merge(object.overallLevel().name(), 1, Integer::sum);
        }
        return counts;
    }

    @NotNull
    private static CompatibilityStatus overallStatus(@NotNull Map<String, SupportResult> supportResults) {
        boolean hasComplete = false;
        boolean hasPartial = false;
        boolean hasNone = false;
        for (SupportResult result : supportResults.values()) {
            hasComplete |= result.status() == CompatibilityStatus.COMPLETE;
            hasPartial |= result.status() == CompatibilityStatus.PARTIAL;
            hasNone |= result.status() == CompatibilityStatus.NONE;
        }
        if (hasNone || hasPartial) {
            return CompatibilityStatus.PARTIAL;
        }
        if (hasComplete) {
            return CompatibilityStatus.COMPLETE;
        }
        return CompatibilityStatus.UNKNOWN;
    }

    @NotNull
    private static SupportLevel overallLevel(@NotNull Map<String, SupportResult> supportResults) {
        boolean hasUnsupported = false;
        boolean hasVisualOnly = false;
        boolean hasApproximated = false;
        boolean hasAdapted = false;
        for (SupportResult result : supportResults.values()) {
            hasUnsupported |= result.level() == SupportLevel.UNSUPPORTED;
            hasVisualOnly |= result.level() == SupportLevel.VISUAL_ONLY;
            hasApproximated |= result.level() == SupportLevel.APPROXIMATED;
            hasAdapted |= result.level() == SupportLevel.ADAPTED;
        }
        if (hasUnsupported) {
            return SupportLevel.UNSUPPORTED;
        }
        if (hasVisualOnly) {
            return SupportLevel.VISUAL_ONLY;
        }
        if (hasApproximated) {
            return SupportLevel.APPROXIMATED;
        }
        if (hasAdapted) {
            return SupportLevel.ADAPTED;
        }
        return SupportLevel.AUTOMATIC;
    }

    private static int overallScore(@NotNull Map<String, SupportResult> supportResults) {
        int total = 0;
        int counted = 0;
        for (SupportResult result : supportResults.values()) {
            if (result.scorePercent() != null) {
                total += result.scorePercent();
                counted++;
            }
        }
        return counted == 0 ? 0 : (int) Math.round(total / (double) counted);
    }

    @NotNull
    private CompatibilityFinding toFinding(@NotNull MetadataValidationIssue issue) {
        CompatibilityFinding.Severity severity;
        try {
            severity = CompatibilityFinding.Severity.valueOf(issue.severity());
        } catch (IllegalArgumentException ex) {
            severity = CompatibilityFinding.Severity.WARNING;
        }
        return new CompatibilityFinding(issue.code(), severity, "metadata", issue.message(), issue.target(), "Fix the metadata patch or mapping entry described by this validation issue.", issue.sourcePath());
    }

    private boolean appliesToMod(@NotNull CompatibilityFinding finding, @NotNull ContentInventory.ModContentInventory inventory) {
        return finding.reason() != null && finding.reason().startsWith(inventory.namespace() + ":");
    }

    @NotNull
    private String inferPatchKind(@NotNull MutableInventory inventory, @NotNull Identifier identifier, @NotNull List<ContentPatch> patches) {
        String explicit = patches.stream().map(ContentPatch::contentType).filter(java.util.Objects::nonNull).findFirst().orElse(null);
        if (explicit != null) {
            return pluralize(explicit);
        }
        if (inventory.registryEntries.getOrDefault("blocks", Set.of()).contains(identifier.toString())) {
            return "blocks";
        }
        if (inventory.registryEntries.getOrDefault("items", Set.of()).contains(identifier.toString())) {
            return "items";
        }
        if (inventory.registryEntries.getOrDefault("entities", Set.of()).contains(identifier.toString())) {
            return "entities";
        }
        if (inventory.registryEntries.getOrDefault("fluids", Set.of()).contains(identifier.toString())) {
            return "fluids";
        }
        if (inventory.registryEntries.getOrDefault("block_entities", Set.of()).contains(identifier.toString())) {
            return "block_entities";
        }
        if (inventory.registryEntries.getOrDefault("menus", Set.of()).contains(identifier.toString())) {
            return "menus";
        }
        if (inventory.assetKeys.getOrDefault("recipes", Set.of()).contains(identifier.toString())) {
            return "recipes";
        }
        return "content";
    }

    @NotNull
    private static String pluralize(@NotNull String value) {
        return switch (value) {
            case "block" -> "blocks";
            case "item" -> "items";
            case "entity" -> "entities";
            case "fluid" -> "fluids";
            case "block_entity" -> "block_entities";
            case "menu" -> "menus";
            case "recipe" -> "recipes";
            default -> value.endsWith("s") ? value : value + "s";
        };
    }

    private void writeJson(@NotNull Path path, @NotNull Object value) {
        try {
            Files.createDirectories(path.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(path)) {
                Constants.GSON.toJson(value, writer);
            }
        } catch (IOException e) {
            this.logger.error("Failed to write compatibility artifact {}", path, e);
        }
    }

    private final class MutableInventory {
        private final ModInfo mod;
        private final Map<String, Integer> registryCounts = new LinkedHashMap<>();
        private final Map<String, Set<String>> registryEntries = new LinkedHashMap<>();
        private final Map<String, Set<String>> assetKeys = new LinkedHashMap<>();
        private final Map<String, Set<String>> metadataEntries = new LinkedHashMap<>();
        private final Map<String, Set<String>> patchEntries = new LinkedHashMap<>();

        private MutableInventory(@NotNull ModInfo mod) {
            this.mod = mod;
        }

        private void incrementRegistry(@NotNull String category, @NotNull String identifier) {
            this.registryCounts.merge(category, 1, Integer::sum);
            this.registryEntries.computeIfAbsent(category, ignored -> new LinkedHashSet<>()).add(identifier);
        }

        private void addAssets(@NotNull String category, @NotNull Path root, @NotNull Predicate<Path> filter) {
            if (!Files.isDirectory(root)) {
                return;
            }

            try (Stream<Path> stream = Files.walk(root)) {
                for (Path path : stream.filter(Files::isRegularFile).filter(filter).toList()) {
                    this.assetKeys.computeIfAbsent(category, ignored -> new LinkedHashSet<>()).add(root.relativize(path).toString().replace('\\', '/'));
                }
            } catch (IOException e) {
                logger.warn("Failed to scan asset root {} for mod {}", root, this.mod.id(), e);
            }
        }

        private void addIdentifierAssets(@NotNull String category, @NotNull Path root, @NotNull String namespace, @NotNull Predicate<Path> filter) {
            if (!Files.isDirectory(root)) {
                return;
            }

            try (Stream<Path> stream = Files.walk(root)) {
                for (Path path : stream.filter(Files::isRegularFile).filter(filter).toList()) {
                    String relative = root.relativize(path).toString().replace('\\', '/');
                    this.assetKeys.computeIfAbsent(category, ignored -> new LinkedHashSet<>()).add(namespace + ":" + relative.substring(0, relative.length() - ".json".length()));
                }
            } catch (IOException e) {
                logger.warn("Failed to scan identifier asset root {} for mod {}", root, this.mod.id(), e);
            }
        }

        private void addMetadata(@NotNull String category, @NotNull String identifier) {
            this.metadataEntries.computeIfAbsent(category, ignored -> new LinkedHashSet<>()).add(identifier);
        }

        private void addPatch(@NotNull String category, @NotNull String identifier) {
            this.patchEntries.computeIfAbsent(category, ignored -> new LinkedHashSet<>()).add(identifier);
        }

        @NotNull
        private ContentInventory.ModContentInventory freeze() {
            Map<String, Integer> assetCounts = new LinkedHashMap<>();
            Map<String, List<String>> assetEntries = new LinkedHashMap<>();
            for (Map.Entry<String, Set<String>> entry : this.assetKeys.entrySet()) {
                assetCounts.put(entry.getKey(), entry.getValue().size());
                assetEntries.put(entry.getKey(), List.copyOf(entry.getValue()));
            }

            Map<String, List<String>> registryEntries = new LinkedHashMap<>();
            for (Map.Entry<String, Set<String>> entry : this.registryEntries.entrySet()) {
                registryEntries.put(entry.getKey(), List.copyOf(entry.getValue()));
            }

            Map<String, Integer> metadataCounts = new LinkedHashMap<>();
            Map<String, List<String>> metadataEntries = new LinkedHashMap<>();
            for (Map.Entry<String, Set<String>> entry : this.metadataEntries.entrySet()) {
                metadataCounts.put(entry.getKey(), entry.getValue().size());
                metadataEntries.put(entry.getKey(), List.copyOf(entry.getValue()));
            }

            Map<String, Integer> patchCounts = new LinkedHashMap<>();
            Map<String, List<String>> patchEntries = new LinkedHashMap<>();
            for (Map.Entry<String, Set<String>> entry : this.patchEntries.entrySet()) {
                patchCounts.put(entry.getKey(), entry.getValue().size());
                patchEntries.put(entry.getKey(), List.copyOf(entry.getValue()));
            }

            ModFingerprint fingerprint = new ModFingerprint(
                this.mod.id(),
                this.mod.namespace(),
                this.mod.version(),
                "unknown",
                SharedConstants.getCurrentVersion().id(),
                this.registryCounts.getOrDefault("blocks", 0),
                this.registryCounts.getOrDefault("items", 0),
                this.registryCounts.getOrDefault("entities", 0),
                this.registryCounts.getOrDefault("fluids", 0),
                this.registryCounts.getOrDefault("block_entities", 0),
                this.registryCounts.getOrDefault("menus", 0),
                assetCounts.getOrDefault("recipes", 0),
                this.registryCounts.getOrDefault("block_entities", 0) > 0,
                !metadataEntries.getOrDefault("blocks", List.of()).isEmpty(),
                false,
                this.registryCounts.getOrDefault("menus", 0) > 0 || this.registryCounts.getOrDefault("fluids", 0) > 0 || this.registryCounts.getOrDefault("block_entities", 0) > 0,
                !metadataEntries.getOrDefault("items", List.of()).isEmpty(),
                assetCounts.getOrDefault("models", 0) > 0,
                this.registryCounts.getOrDefault("particles", 0) > 0,
                assetCounts.getOrDefault("sounds", 0) > 0 || this.registryCounts.getOrDefault("sound_events", 0) > 0
            );

            return new ContentInventory.ModContentInventory(
                this.mod.id(),
                this.mod.namespace(),
                this.mod.name(),
                this.mod.version(),
                this.mod.roots().stream().map(Path::toString).toList(),
                fingerprint,
                this.registryCounts,
                registryEntries,
                assetCounts,
                assetEntries,
                metadataCounts,
                metadataEntries,
                patchCounts,
                patchEntries
            );
        }
    }
}