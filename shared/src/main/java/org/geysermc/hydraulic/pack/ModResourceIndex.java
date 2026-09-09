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

final class ModResourceIndex {
    private final Set<String> namespaces;
    private final Map<Identifier, Path> blockStates;
    private final Map<Identifier, Path> itemDefinitions;
    private final Map<Identifier, Path> legacyItemModels;
    private final boolean hasAssetFiles;

    private ModResourceIndex(
        @NotNull Set<String> namespaces,
        @NotNull Map<Identifier, Path> blockStates,
        @NotNull Map<Identifier, Path> itemDefinitions,
        @NotNull Map<Identifier, Path> legacyItemModels,
        boolean hasAssetFiles
    ) {
        this.namespaces = Set.copyOf(namespaces);
        this.blockStates = Map.copyOf(blockStates);
        this.itemDefinitions = Map.copyOf(itemDefinitions);
        this.legacyItemModels = Map.copyOf(legacyItemModels);
        this.hasAssetFiles = hasAssetFiles;
    }

    @NotNull
    static ModResourceIndex create(@NotNull ModInfo mod, @NotNull Logger logger) {
        Set<String> namespaces = new LinkedHashSet<>();
        Map<Identifier, Path> blockStates = new LinkedHashMap<>();
        Map<Identifier, Path> itemDefinitions = new LinkedHashMap<>();
        Map<Identifier, Path> legacyItemModels = new LinkedHashMap<>();
        boolean hasAssetFiles = false;

        for (Path root : mod.roots()) {
            Path assets = root.resolve("assets");
            if (!Files.isDirectory(assets)) {
                continue;
            }

            try (Stream<Path> stream = Files.walk(assets)) {
                java.util.List<Path> assetFiles = stream.filter(Files::isRegularFile).toList();
                if (!assetFiles.isEmpty()) {
                    hasAssetFiles = true;
                }
                assetFiles.stream()
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .forEach(path -> indexFile(path, assets, namespaces, blockStates, itemDefinitions, legacyItemModels));
            } catch (IOException e) {
                logger.error("Failed to index assets for mod {}", mod.id(), e);
            }
        }

        return new ModResourceIndex(namespaces, blockStates, itemDefinitions, legacyItemModels, hasAssetFiles);
    }

    @NotNull
    Set<String> namespaces() {
        return this.namespaces;
    }

    boolean hasAssetFiles() {
        return this.hasAssetFiles;
    }

    boolean hasBlockState(@NotNull Identifier block) {
        return this.blockStates.containsKey(block);
    }

    boolean hasItemAsset(@NotNull Identifier itemModel) {
        return this.itemDefinitions.containsKey(itemModel) || this.legacyItemModels.containsKey(itemModel);
    }

    @Nullable
    Path resolveItemAssetPath(@NotNull Identifier itemModel) {
        Path itemDefinition = this.itemDefinitions.get(itemModel);
        if (itemDefinition != null) {
            return itemDefinition;
        }
        return this.legacyItemModels.get(itemModel);
    }

    private static void indexFile(
        @NotNull Path file,
        @NotNull Path assetsRoot,
        @NotNull Set<String> namespaces,
        @NotNull Map<Identifier, Path> blockStates,
        @NotNull Map<Identifier, Path> itemDefinitions,
        @NotNull Map<Identifier, Path> legacyItemModels
    ) {
        Path relative = assetsRoot.relativize(file);
        if (relative.getNameCount() < 3) {
            return;
        }

        String namespace = relative.getName(0).toString();
        namespaces.add(namespace);

        String firstSegment = relative.getName(1).toString();
        if ("blockstates".equals(firstSegment)) {
            Identifier identifier = identifier(namespace, relative.subpath(2, relative.getNameCount()));
            if (identifier != null) {
                blockStates.putIfAbsent(identifier, file);
            }
            return;
        }

        if ("items".equals(firstSegment)) {
            Identifier identifier = identifier(namespace, relative.subpath(2, relative.getNameCount()));
            if (identifier != null) {
                itemDefinitions.putIfAbsent(identifier, file);
            }
            return;
        }

        if (!"models".equals(firstSegment) || relative.getNameCount() < 4 || !"item".equals(relative.getName(2).toString())) {
            return;
        }

        Identifier identifier = identifier(namespace, relative.subpath(3, relative.getNameCount()));
        if (identifier != null) {
            legacyItemModels.putIfAbsent(identifier, file);
        }
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
