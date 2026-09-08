package org.geysermc.hydraulic.compat;

import com.google.common.collect.ListMultimap;
import net.minecraft.core.DefaultedRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import org.geysermc.hydraulic.Constants;
import org.geysermc.hydraulic.metadata.MetadataIndex;
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
        this.incrementMenuRegistry(inventories, namespacesToMods);

        for (MutableInventory inventory : inventories.values()) {
            this.scanModAssets(inventory);
            inventory.registryCounts.put("block_assets", modsToBlocks.get(inventory.mod.id()).size());
            inventory.registryCounts.put("item_assets", modsToItems.get(inventory.mod.id()).size());
        }

        for (Identifier javaId : metadataIndex.blockMappings().keySet()) {
            for (ModInfo mod : namespacesToMods.get(javaId.getNamespace())) {
                MutableInventory inventory = inventories.get(mod.id());
                if (inventory != null) {
                    inventory.blockMetadataMappings++;
                }
            }
        }

        for (Identifier javaId : metadataIndex.itemMappings().keySet()) {
            for (ModInfo mod : namespacesToMods.get(javaId.getNamespace())) {
                MutableInventory inventory = inventories.get(mod.id());
                if (inventory != null) {
                    inventory.itemMetadataMappings++;
                }
            }
        }

        for (Identifier javaId : metadataIndex.recipeMappings().keySet()) {
            for (ModInfo mod : namespacesToMods.get(javaId.getNamespace())) {
                MutableInventory inventory = inventories.get(mod.id());
                if (inventory != null) {
                    inventory.recipeMetadataMappings++;
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
                    inventory.incrementRegistry(category);
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
    }

    private void scanDataNamespace(@NotNull MutableInventory inventory, @NotNull Path namespacePath) {
        inventory.addAssets("recipes", namespacePath.resolve("recipes"), path -> path.toString().endsWith(".json"));
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
    private CompatibilityReport buildReport(@NotNull ContentInventory inventory, @NotNull MetadataIndex metadataIndex) {
        Map<String, CompatibilityProfile> profiles = new LinkedHashMap<>();
        for (ContentInventory.ModContentInventory modInventory : inventory.mods().values()) {
            Map<String, CompatibilityProfile.CapabilityMetric> metrics = new LinkedHashMap<>();
            metrics.put("blocks", coverageMetric(modInventory.registryCounts().get("blocks"), modInventory.assetCounts().get("blockstates"), "blockstate json present"));
            metrics.put("items", coverageMetric(modInventory.registryCounts().get("items"), Math.max(valueOrZero(modInventory.assetCounts().get("item_models")), modInventory.itemMetadataMappings()), "item model json or item metadata mapping present"));
            metrics.put("entities", unknownMetric(modInventory.registryCounts().get("entities"), "registry discovered; behavior analyzer not implemented yet"));
            metrics.put("fluids", unknownMetric(modInventory.registryCounts().get("fluids"), "registry discovered; fluid analyzer not implemented yet"));
            metrics.put("recipes", coverageMetric(modInventory.assetCounts().get("recipes"), modInventory.recipeMetadataMappings(), "recipe json present with optional recipe metadata mapping"));
            metrics.put("menus", unknownMetric(modInventory.registryCounts().get("menus"), "registry discovered; menu analyzer not implemented yet"));
            metrics.put("textures", presenceMetric(modInventory.assetCounts().get("textures"), "texture asset present"));
            metrics.put("models", presenceMetric(modInventory.assetCounts().get("models"), "model json present"));
            metrics.put("sounds", presenceMetric(modInventory.assetCounts().get("sounds"), "sound asset present"));
            metrics.put("metadata_blocks", coverageMetric(modInventory.registryCounts().get("blocks"), modInventory.blockMetadataMappings(), "block metadata mapping present"));
            metrics.put("metadata_items", coverageMetric(modInventory.registryCounts().get("items"), modInventory.itemMetadataMappings(), "item metadata mapping present"));
            metrics.put("metadata_recipes", coverageMetric(modInventory.assetCounts().get("recipes"), modInventory.recipeMetadataMappings(), "recipe metadata mapping present"));

            List<String> notes = new ArrayList<>();
            notes.add("Early compatibility profile is inventory-backed and intended for regression tracking before deeper analyzers exist.");
            if (modInventory.blockMetadataMappings() == 0) {
                notes.add("No metadata block mappings discovered for this mod.");
            }
            if (modInventory.itemMetadataMappings() == 0) {
                notes.add("No metadata item mappings discovered for this mod.");
            }
            if (valueOrZero(modInventory.assetCounts().get("recipes")) > 0 && modInventory.recipeMetadataMappings() == 0) {
                notes.add("Recipe assets were discovered without recipe metadata mappings.");
            }

            profiles.put(
                modInventory.modId(),
                new CompatibilityProfile(modInventory.modId(), overallStatus(metrics), metrics, notes)
            );
        }

        return new CompatibilityReport(Instant.now().toString(), metadataIndex.summary(), profiles);
    }

    @NotNull
    private static CompatibilityProfile.CapabilityMetric coverageMetric(Integer total, Integer covered, @NotNull String basis) {
        if (total == null || total == 0) {
            return new CompatibilityProfile.CapabilityMetric(total, covered != null ? covered : 0, total == null ? null : 0, CompatibilityStatus.NONE, basis);
        }

        int safeCovered = Math.min(covered != null ? covered : 0, total);
        int percent = (int) Math.round((safeCovered * 100.0d) / total);
        CompatibilityStatus status = safeCovered == 0 ? CompatibilityStatus.NONE : safeCovered == total ? CompatibilityStatus.COMPLETE : CompatibilityStatus.PARTIAL;
        return new CompatibilityProfile.CapabilityMetric(total, safeCovered, percent, status, basis);
    }

    @NotNull
    private static CompatibilityProfile.CapabilityMetric presenceMetric(Integer count, @NotNull String basis) {
        int safeCount = count != null ? count : 0;
        CompatibilityStatus status = safeCount == 0 ? CompatibilityStatus.NONE : CompatibilityStatus.COMPLETE;
        return new CompatibilityProfile.CapabilityMetric(safeCount, safeCount, safeCount == 0 ? 0 : 100, status, basis);
    }

    private static int valueOrZero(Integer value) {
        return value != null ? value : 0;
    }

    @NotNull
    private static CompatibilityProfile.CapabilityMetric unknownMetric(Integer total, @NotNull String basis) {
        int safeTotal = total != null ? total : 0;
        CompatibilityStatus status = safeTotal == 0 ? CompatibilityStatus.NONE : CompatibilityStatus.UNKNOWN;
        return new CompatibilityProfile.CapabilityMetric(safeTotal, null, null, status, basis);
    }

    @NotNull
    private static CompatibilityStatus overallStatus(@NotNull Map<String, CompatibilityProfile.CapabilityMetric> metrics) {
        boolean hasKnown = false;
        boolean hasCoverage = false;
        boolean hasMissing = false;
        boolean hasPartial = false;

        for (CompatibilityProfile.CapabilityMetric metric : metrics.values()) {
            if (metric.status() == CompatibilityStatus.UNKNOWN) {
                continue;
            }

            hasKnown = true;
            if (metric.status() == CompatibilityStatus.PARTIAL) {
                hasPartial = true;
            }
            if (metric.status() == CompatibilityStatus.NONE && (metric.total() == null || metric.total() > 0)) {
                hasMissing = true;
            }
            if (metric.status() == CompatibilityStatus.COMPLETE) {
                hasCoverage = true;
            }
        }

        if (!hasKnown) {
            return CompatibilityStatus.UNKNOWN;
        }
        if (hasPartial || hasMissing) {
            return CompatibilityStatus.PARTIAL;
        }
        if (hasCoverage) {
            return CompatibilityStatus.COMPLETE;
        }
        return CompatibilityStatus.NONE;
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
        private final Map<String, Set<String>> assetKeys = new LinkedHashMap<>();
        private int blockMetadataMappings;
        private int itemMetadataMappings;
        private int recipeMetadataMappings;

        private MutableInventory(@NotNull ModInfo mod) {
            this.mod = mod;
        }

        private void incrementRegistry(@NotNull String category) {
            this.registryCounts.merge(category, 1, Integer::sum);
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

        @NotNull
        private ContentInventory.ModContentInventory freeze() {
            Map<String, Integer> assetCounts = new LinkedHashMap<>();
            for (Map.Entry<String, Set<String>> entry : this.assetKeys.entrySet()) {
                assetCounts.put(entry.getKey(), entry.getValue().size());
            }

            return new ContentInventory.ModContentInventory(
                this.mod.id(),
                this.mod.namespace(),
                this.mod.name(),
                this.mod.version(),
                this.mod.roots().stream().map(Path::toString).toList(),
                this.registryCounts,
                assetCounts,
                this.blockMetadataMappings,
                this.itemMetadataMappings,
                this.recipeMetadataMappings
            );
        }
    }
}