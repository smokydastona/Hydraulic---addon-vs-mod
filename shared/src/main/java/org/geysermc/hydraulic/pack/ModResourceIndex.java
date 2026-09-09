package org.geysermc.hydraulic.pack;

import net.minecraft.resources.Identifier;
import org.geysermc.hydraulic.platform.mod.ModInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public final class ModResourceIndex {
    private final Set<String> namespaces;
    private final Map<Identifier, Path> blockStates;
    private final Map<Identifier, Path> itemDefinitions;
    private final Map<Identifier, Path> legacyItemModels;
    private final Map<String, Set<String>> assetEntries;
    private final boolean hasAssetFiles;

    private ModResourceIndex(
        @NotNull Set<String> namespaces,
        @NotNull Map<Identifier, Path> blockStates,
        @NotNull Map<Identifier, Path> itemDefinitions,
        @NotNull Map<Identifier, Path> legacyItemModels,
        @NotNull Map<String, Set<String>> assetEntries,
        boolean hasAssetFiles
    ) {
        this.namespaces = Set.copyOf(namespaces);
        this.blockStates = Map.copyOf(blockStates);
        this.itemDefinitions = Map.copyOf(itemDefinitions);
        this.legacyItemModels = Map.copyOf(legacyItemModels);
        this.assetEntries = copyAssetEntries(assetEntries);
        this.hasAssetFiles = hasAssetFiles;
    }

    @NotNull
    public static ModResourceIndex create(@NotNull ModInfo mod, @NotNull Logger logger) {
        Set<String> namespaces = new LinkedHashSet<>();
        Map<Identifier, Path> blockStates = new LinkedHashMap<>();
        Map<Identifier, Path> itemDefinitions = new LinkedHashMap<>();
        Map<Identifier, Path> legacyItemModels = new LinkedHashMap<>();
        Map<String, Set<String>> assetEntries = new LinkedHashMap<>();
        boolean hasAssetFiles = false;

        for (Path root : mod.roots()) {
            Path assets = root.resolve("assets");
            if (!Files.isDirectory(assets)) {
                // Continue into the data scan even when this root has no assets.
            } else {
                try (Stream<Path> stream = Files.walk(assets)) {
                    java.util.List<Path> assetFiles = stream.filter(Files::isRegularFile).toList();
                    if (!assetFiles.isEmpty()) {
                        hasAssetFiles = true;
                    }
                    for (Path path : assetFiles) {
                        indexAssetFile(path, assets, namespaces, blockStates, itemDefinitions, legacyItemModels, assetEntries);
                    }
                } catch (IOException e) {
                    logger.error("Failed to index assets for mod {}", mod.id(), e);
                }
            }

            Path data = root.resolve("data");
            if (!Files.isDirectory(data)) {
                continue;
            }

            try (Stream<Path> stream = Files.walk(data)) {
                for (Path path : stream.filter(Files::isRegularFile).toList()) {
                    indexDataFile(path, data, assetEntries);
                }
            } catch (IOException e) {
                logger.error("Failed to index data for mod {}", mod.id(), e);
            }
        }

        return new ModResourceIndex(namespaces, blockStates, itemDefinitions, legacyItemModels, assetEntries, hasAssetFiles);
    }

    @NotNull
    public Set<String> namespaces() {
        return this.namespaces;
    }

    public boolean hasAssetFiles() {
        return this.hasAssetFiles;
    }

    public boolean hasBlockState(@NotNull Identifier block) {
        return this.blockStates.containsKey(block);
    }

    public int blockStateCount() {
        return this.blockStates.size();
    }

    public boolean hasItemAsset(@NotNull Identifier itemModel) {
        return this.itemDefinitions.containsKey(itemModel) || this.legacyItemModels.containsKey(itemModel);
    }

    public int itemAssetCount() {
        return this.itemDefinitions.size() + this.legacyItemModels.size();
    }

    @NotNull
    public Set<String> assetEntries(@NotNull String category) {
        return this.assetEntries.getOrDefault(category, Set.of());
    }

    @Nullable
    public Path resolveItemAssetPath(@NotNull Identifier itemModel) {
        Path itemDefinition = this.itemDefinitions.get(itemModel);
        if (itemDefinition != null) {
            return itemDefinition;
        }
        return this.legacyItemModels.get(itemModel);
    }

    private static void indexAssetFile(
        @NotNull Path file,
        @NotNull Path assetsRoot,
        @NotNull Set<String> namespaces,
        @NotNull Map<Identifier, Path> blockStates,
        @NotNull Map<Identifier, Path> itemDefinitions,
        @NotNull Map<Identifier, Path> legacyItemModels,
        @NotNull Map<String, Set<String>> assetEntries
    ) {
        Path relative = assetsRoot.relativize(file);
        if (relative.getNameCount() < 3) {
            return;
        }

        String namespace = relative.getName(0).toString();
        namespaces.add(namespace);

        String firstSegment = relative.getName(1).toString();
        if ("blockstates".equals(firstSegment)) {
            addRelativeAsset(assetEntries, "blockstates", relative.subpath(2, relative.getNameCount()));
            Identifier identifier = identifier(namespace, relative.subpath(2, relative.getNameCount()));
            if (identifier != null) {
                blockStates.putIfAbsent(identifier, file);
            }
            return;
        }

        if ("items".equals(firstSegment)) {
            addRelativeAsset(assetEntries, "item_models", relative.subpath(2, relative.getNameCount()));
            Identifier identifier = identifier(namespace, relative.subpath(2, relative.getNameCount()));
            if (identifier != null) {
                itemDefinitions.putIfAbsent(identifier, file);
            }
            return;
        }

        if ("models".equals(firstSegment) && file.getFileName().toString().endsWith(".json")) {
            addRelativeAsset(assetEntries, "models", relative.subpath(2, relative.getNameCount()));
        }

        if ("textures".equals(firstSegment) && isTextureAsset(file)) {
            addRelativeAsset(assetEntries, "textures", relative.subpath(2, relative.getNameCount()));
        }

        if ("sounds".equals(firstSegment) && isSoundAsset(file)) {
            addRelativeAsset(assetEntries, "sounds", relative.subpath(2, relative.getNameCount()));
        }

        if ("lang".equals(firstSegment) && file.getFileName().toString().endsWith(".json")) {
            addRelativeAsset(assetEntries, "lang", relative.subpath(2, relative.getNameCount()));
        }

        if (!"models".equals(firstSegment) || relative.getNameCount() < 4 || !"item".equals(relative.getName(2).toString())) {
            return;
        }

        Identifier identifier = identifier(namespace, relative.subpath(3, relative.getNameCount()));
        if (identifier != null) {
            legacyItemModels.putIfAbsent(identifier, file);
        }
    }

    private static void indexDataFile(
        @NotNull Path file,
        @NotNull Path dataRoot,
        @NotNull Map<String, Set<String>> assetEntries
    ) {
        Path relative = dataRoot.relativize(file);
        if (relative.getNameCount() < 3) {
            return;
        }

        String namespace = relative.getName(0).toString();
        String firstSegment = relative.getName(1).toString();
        Path namespacedRelative = relative.subpath(2, relative.getNameCount());

        if ("recipes".equals(firstSegment)) {
            Identifier identifier = identifier(namespace, namespacedRelative);
            if (identifier != null) {
                assetEntries.computeIfAbsent("recipes", ignored -> new LinkedHashSet<>()).add(identifier.toString());
            }
            return;
        }

        if ("tags".equals(firstSegment) && file.getFileName().toString().endsWith(".json")) {
            addRelativeAsset(assetEntries, "tags", namespacedRelative);
            return;
        }

        if ("loot_tables".equals(firstSegment) && file.getFileName().toString().endsWith(".json")) {
            addRelativeAsset(assetEntries, "loot_tables", namespacedRelative);
        }
    }

    private static void addRelativeAsset(
        @NotNull Map<String, Set<String>> assetEntries,
        @NotNull String category,
        @NotNull Path relativePath
    ) {
        String normalized = relativePath.toString().replace('\\', '/');
        assetEntries.computeIfAbsent(category, ignored -> new LinkedHashSet<>()).add(normalized);
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
    private static Map<String, Set<String>> copyAssetEntries(@NotNull Map<String, Set<String>> assetEntries) {
        Map<String, Set<String>> copy = new LinkedHashMap<>();
        for (Map.Entry<String, Set<String>> entry : assetEntries.entrySet()) {
            copy.put(entry.getKey(), Set.copyOf(entry.getValue()));
        }
        return Map.copyOf(copy);
    }

    @Nullable
    private static Identifier identifier(@NotNull String namespace, @NotNull Path relativePath) {
        String normalized = relativePath.toString().replace('\\', '/');
        if (!normalized.endsWith(".json")) {
            return null;
        }

        String path = normalized.substring(0, normalized.length() - ".json".length());
        if (path.isEmpty()) {
            return null;
        }
        return Identifier.fromNamespaceAndPath(namespace, path);
    }
}
