package org.geysermc.hydraulic.pack;

import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.kyori.adventure.key.Key;
import net.minecraft.resources.Identifier;
import org.geysermc.hydraulic.platform.mod.ModInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public final class ModResourceIndex {
    private static final String FINGERPRINT_ALGORITHM = "HYDRAULIC_INDEX_V1";

    private final Set<String> namespaces;
    private final Map<Identifier, Path> blockStates;
    private final Map<Identifier, Path> itemDefinitions;
    private final Map<Identifier, Path> legacyItemModels;
    private final Map<Identifier, Path> models;
    private final Map<Key, Path> textures;
    private final Set<String> dependencyNamespaces;
    private final Map<String, Set<String>> assetEntries;
    private final ResourceFingerprint fingerprint;
    private final boolean hasAssetFiles;
    private final List<ScanRoot> scanRoots;
    private final List<FileStamp> fileStamps;
    private final List<DirectoryStamp> directoryStamps;

    private ModResourceIndex(
        @NotNull Set<String> namespaces,
        @NotNull Map<Identifier, Path> blockStates,
        @NotNull Map<Identifier, Path> itemDefinitions,
        @NotNull Map<Identifier, Path> legacyItemModels,
        @NotNull Map<Identifier, Path> models,
        @NotNull Map<Key, Path> textures,
        @NotNull Set<String> dependencyNamespaces,
        @NotNull Map<String, Set<String>> assetEntries,
        @NotNull ResourceFingerprint fingerprint,
        boolean hasAssetFiles,
        @NotNull List<ScanRoot> scanRoots,
        @NotNull List<FileStamp> fileStamps,
        @NotNull List<DirectoryStamp> directoryStamps
    ) {
        this.namespaces = Set.copyOf(namespaces);
        this.blockStates = Map.copyOf(blockStates);
        this.itemDefinitions = Map.copyOf(itemDefinitions);
        this.legacyItemModels = Map.copyOf(legacyItemModels);
        this.models = Map.copyOf(models);
        this.textures = Map.copyOf(textures);
        this.dependencyNamespaces = Set.copyOf(dependencyNamespaces);
        this.assetEntries = copyAssetEntries(assetEntries);
        this.fingerprint = fingerprint;
        this.hasAssetFiles = hasAssetFiles;
        this.scanRoots = List.copyOf(scanRoots);
        this.fileStamps = List.copyOf(fileStamps);
        this.directoryStamps = List.copyOf(directoryStamps);
    }

    @NotNull
    public static ModResourceIndex create(@NotNull ModInfo mod, @NotNull Logger logger) {
        Set<String> namespaces = new LinkedHashSet<>();
        Map<Identifier, Path> blockStates = new LinkedHashMap<>();
        Map<Identifier, Path> itemDefinitions = new LinkedHashMap<>();
        Map<Identifier, Path> legacyItemModels = new LinkedHashMap<>();
        Map<Identifier, Path> models = new LinkedHashMap<>();
        Map<Key, Path> textures = new LinkedHashMap<>();
        Set<String> dependencyNamespaces = new LinkedHashSet<>();
        Map<String, Set<String>> assetEntries = new LinkedHashMap<>();
        List<ScanRoot> scanRoots = new ArrayList<>();
        List<FileStamp> fileStamps = new ArrayList<>();
        List<DirectoryStamp> directoryStamps = new ArrayList<>();
        Hasher fingerprintHasher = Hashing.sha256().newHasher();
        int indexedFileCount = 0;
        long indexedTotalSizeBytes = 0;
        long latestModifiedEpochMillis = 0;
        boolean hasAssetFiles = false;

        int rootOrdinal = 0;
        for (Path root : mod.roots()) {
            Path assets = root.resolve("assets");
            scanRoots.add(scanRoot(assets, rootOrdinal, "assets"));
            if (!Files.isDirectory(assets)) {
                // Continue into the data scan even when this root has no assets.
            } else {
                try (Stream<Path> stream = Files.walk(assets)) {
                    java.util.List<Path> assetPaths = stream.sorted().toList();
                    boolean sawAssetFile = false;
                    for (Path path : assetPaths) {
                        if (Files.isDirectory(path)) {
                            directoryStamps.add(directoryStamp(path, assets, rootOrdinal, "assets"));
                            continue;
                        }
                        if (!Files.isRegularFile(path)) {
                            continue;
                        }

                        sawAssetFile = true;
                        fileStamps.add(fileStamp(path, assets, rootOrdinal, "assets"));
                        indexAssetFile(path, assets, namespaces, blockStates, itemDefinitions, legacyItemModels, models, textures, dependencyNamespaces, assetEntries);
                        FileMetadata metadata = fileMetadata(path, assets, rootOrdinal, "assets");
                        fingerprintHasher.putString(metadata.stablePath(), java.nio.charset.StandardCharsets.UTF_8);
                        fingerprintHasher.putLong(metadata.size());
                        fingerprintHasher.putLong(metadata.lastModifiedEpochMillis());
                        indexedFileCount++;
                        indexedTotalSizeBytes += metadata.size();
                        latestModifiedEpochMillis = Math.max(latestModifiedEpochMillis, metadata.lastModifiedEpochMillis());
                    }
                    if (sawAssetFile) {
                        hasAssetFiles = true;
                    }
                } catch (IOException e) {
                    logger.error("Failed to index assets for mod {}", mod.id(), e);
                }
            }

            Path data = root.resolve("data");
            scanRoots.add(scanRoot(data, rootOrdinal, "data"));
            if (!Files.isDirectory(data)) {
                rootOrdinal++;
                continue;
            }

            try (Stream<Path> stream = Files.walk(data)) {
                for (Path path : stream.sorted().toList()) {
                    if (Files.isDirectory(path)) {
                        directoryStamps.add(directoryStamp(path, data, rootOrdinal, "data"));
                        continue;
                    }
                    if (!Files.isRegularFile(path)) {
                        continue;
                    }

                    fileStamps.add(fileStamp(path, data, rootOrdinal, "data"));
                    indexDataFile(path, data, assetEntries);
                    FileMetadata metadata = fileMetadata(path, data, rootOrdinal, "data");
                    fingerprintHasher.putString(metadata.stablePath(), java.nio.charset.StandardCharsets.UTF_8);
                    fingerprintHasher.putLong(metadata.size());
                    fingerprintHasher.putLong(metadata.lastModifiedEpochMillis());
                    indexedFileCount++;
                    indexedTotalSizeBytes += metadata.size();
                    latestModifiedEpochMillis = Math.max(latestModifiedEpochMillis, metadata.lastModifiedEpochMillis());
                }
            } catch (IOException e) {
                logger.error("Failed to index data for mod {}", mod.id(), e);
            }
            rootOrdinal++;
        }

        return new ModResourceIndex(
            namespaces,
            blockStates,
            itemDefinitions,
            legacyItemModels,
            models,
            textures,
            dependencyNamespaces,
            assetEntries,
            new ResourceFingerprint(FINGERPRINT_ALGORITHM, indexedFileCount, indexedTotalSizeBytes, latestModifiedEpochMillis, fingerprintHasher.hash().toString()),
            hasAssetFiles,
            scanRoots,
            fileStamps,
            directoryStamps
        );
    }

    @NotNull
    public static ModResourceIndex rehydrate(@NotNull Snapshot snapshot) {
        return new ModResourceIndex(
            snapshot.namespaces(),
            toIdentifierPathMap(snapshot.blockStates()),
            toIdentifierPathMap(snapshot.itemDefinitions()),
            toIdentifierPathMap(snapshot.legacyItemModels()),
            toIdentifierPathMap(snapshot.models()),
            toKeyPathMap(snapshot.textures()),
            snapshot.dependencyNamespaces(),
            snapshot.assetEntries(),
            snapshot.fingerprint(),
            snapshot.hasAssetFiles(),
            snapshot.scanRoots(),
            snapshot.fileStamps(),
            snapshot.directoryStamps()
        );
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

    public int modelCount() {
        return this.models.size();
    }

    public int textureCount() {
        return this.textures.size();
    }

    @NotNull
    public Set<String> dependencyNamespaces() {
        return this.dependencyNamespaces;
    }

    @NotNull
    public Snapshot snapshot() {
        return new Snapshot(
            this.namespaces,
            stringifyIdentifierPaths(this.blockStates),
            stringifyIdentifierPaths(this.itemDefinitions),
            stringifyIdentifierPaths(this.legacyItemModels),
            stringifyIdentifierPaths(this.models),
            stringifyKeyPaths(this.textures),
            this.dependencyNamespaces,
            this.assetEntries,
            this.fingerprint,
            this.hasAssetFiles,
            this.scanRoots,
            this.fileStamps,
            this.directoryStamps
        );
    }

    @NotNull
    public ResourceFingerprint fingerprint() {
        return this.fingerprint;
    }

    @NotNull
    public Set<String> assetEntries(@NotNull String category) {
        return this.assetEntries.getOrDefault(category, Set.of());
    }

    @Nullable
    public Path resolveModelPath(@NotNull Key modelKey) {
        return this.models.get(Identifier.fromNamespaceAndPath(modelKey.namespace(), modelKey.value()));
    }

    @NotNull
    public Map<Key, Path> modelPaths() {
        Map<Key, Path> resolved = new LinkedHashMap<>();
        for (Map.Entry<Identifier, Path> entry : this.models.entrySet()) {
            resolved.put(Key.key(entry.getKey().getNamespace(), entry.getKey().getPath()), entry.getValue());
        }
        return Map.copyOf(resolved);
    }

    @Nullable
    public Path resolveTexturePath(@NotNull Key textureKey) {
        return this.textures.get(textureKey);
    }

    @NotNull
    public Map<Key, Path> texturePaths() {
        return this.textures;
    }

    @NotNull
    public List<ScanRoot> scanRoots() {
        return this.scanRoots;
    }

    @NotNull
    public List<FileStamp> fileStamps() {
        return this.fileStamps;
    }

    @NotNull
    public List<DirectoryStamp> directoryStamps() {
        return this.directoryStamps;
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
        @NotNull Map<Identifier, Path> models,
        @NotNull Map<Key, Path> textures,
        @NotNull Set<String> dependencyNamespaces,
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
            Identifier identifier = identifier(namespace, relative.subpath(2, relative.getNameCount()));
            if (identifier != null) {
                models.putIfAbsent(identifier, file);
            }
            collectModelDependencyNamespaces(file, namespace, dependencyNamespaces);
        }

        if ("textures".equals(firstSegment) && isTextureAsset(file)) {
            addRelativeAsset(assetEntries, "textures", relative.subpath(2, relative.getNameCount()));
            Key textureKey = textureKey(namespace, relative.subpath(2, relative.getNameCount()));
            if (textureKey != null) {
                textures.putIfAbsent(textureKey, file);
            }
        }

        if ("sounds".equals(firstSegment) && isSoundAsset(file)) {
            addRelativeAsset(assetEntries, "sounds", relative.subpath(2, relative.getNameCount()));
        }

        if ("lang".equals(firstSegment) && file.getFileName().toString().endsWith(".json")) {
            addRelativeAsset(assetEntries, "lang", relative.subpath(2, relative.getNameCount()));
        }

        if ("equipment".equals(firstSegment) && file.getFileName().toString().endsWith(".json")) {
            collectEquipmentDependencyNamespaces(file, namespace, dependencyNamespaces);
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
    private static ScanRoot scanRoot(@NotNull Path root, int rootOrdinal, @NotNull String category) {
        boolean exists = Files.isDirectory(root);
        long lastModifiedEpochMillis = 0L;
        if (exists) {
            try {
                lastModifiedEpochMillis = Files.getLastModifiedTime(root).toMillis();
            } catch (IOException ignored) {
                lastModifiedEpochMillis = 0L;
            }
        }
        return new ScanRoot(rootOrdinal + ":" + category, root.toString(), exists, lastModifiedEpochMillis);
    }

    @NotNull
    private static FileStamp fileStamp(@NotNull Path file, @NotNull Path root, int rootOrdinal, @NotNull String category) throws IOException {
        FileMetadata metadata = fileMetadata(file, root, rootOrdinal, category);
        return new FileStamp(metadata.stablePath(), file.toString(), metadata.size(), metadata.lastModifiedEpochMillis());
    }

    @NotNull
    private static DirectoryStamp directoryStamp(@NotNull Path directory, @NotNull Path root, int rootOrdinal, @NotNull String category) throws IOException {
        String relative = root.equals(directory) ? "" : root.relativize(directory).toString().replace('\\', '/');
        return new DirectoryStamp(rootOrdinal + ":" + category + ":" + relative, directory.toString(), Files.getLastModifiedTime(directory).toMillis());
    }

    @NotNull
    private static FileMetadata fileMetadata(@NotNull Path file, @NotNull Path root, int rootOrdinal, @NotNull String category) throws IOException {
        long size = Files.size(file);
        long lastModifiedEpochMillis = Files.getLastModifiedTime(file).toMillis();
        String relative = root.relativize(file).toString().replace('\\', '/');
        return new FileMetadata(rootOrdinal + ":" + category + ":" + relative, size, lastModifiedEpochMillis);
    }

    @NotNull
    private static Map<String, Set<String>> copyAssetEntries(@NotNull Map<String, Set<String>> assetEntries) {
        Map<String, Set<String>> copy = new LinkedHashMap<>();
        for (Map.Entry<String, Set<String>> entry : assetEntries.entrySet()) {
            copy.put(entry.getKey(), Set.copyOf(entry.getValue()));
        }
        return Map.copyOf(copy);
    }

    @NotNull
    private static Map<String, String> stringifyIdentifierPaths(@NotNull Map<Identifier, Path> values) {
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<Identifier, Path> entry : values.entrySet()) {
            result.put(entry.getKey().toString(), entry.getValue().toString());
        }
        return Map.copyOf(result);
    }

    @NotNull
    private static Map<String, String> stringifyKeyPaths(@NotNull Map<Key, Path> values) {
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<Key, Path> entry : values.entrySet()) {
            result.put(entry.getKey().asString(), entry.getValue().toString());
        }
        return Map.copyOf(result);
    }

    @NotNull
    private static Map<Identifier, Path> toIdentifierPathMap(@NotNull Map<String, String> values) {
        Map<Identifier, Path> result = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            String identifier = entry.getKey();
            int separator = identifier.indexOf(':');
            if (separator <= 0 || separator == identifier.length() - 1) {
                continue;
            }
            result.put(
                Identifier.fromNamespaceAndPath(identifier.substring(0, separator), identifier.substring(separator + 1)),
                Path.of(entry.getValue())
            );
        }
        return Map.copyOf(result);
    }

    @NotNull
    private static Map<Key, Path> toKeyPathMap(@NotNull Map<String, String> values) {
        Map<Key, Path> result = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            try {
                result.put(Key.key(entry.getKey()), Path.of(entry.getValue()));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return Map.copyOf(result);
    }

    public record Snapshot(
        @NotNull Set<String> namespaces,
        @NotNull Map<String, String> blockStates,
        @NotNull Map<String, String> itemDefinitions,
        @NotNull Map<String, String> legacyItemModels,
        @NotNull Map<String, String> models,
        @NotNull Map<String, String> textures,
        @NotNull Set<String> dependencyNamespaces,
        @NotNull Map<String, Set<String>> assetEntries,
        @NotNull ResourceFingerprint fingerprint,
        boolean hasAssetFiles,
        @NotNull List<ScanRoot> scanRoots,
        @NotNull List<FileStamp> fileStamps,
        @NotNull List<DirectoryStamp> directoryStamps
    ) {
        public Snapshot {
            namespaces = namespaces == null ? Set.of() : Set.copyOf(namespaces);
            blockStates = immutableStringMap(blockStates);
            itemDefinitions = immutableStringMap(itemDefinitions);
            legacyItemModels = immutableStringMap(legacyItemModels);
            models = immutableStringMap(models);
            textures = immutableStringMap(textures);
            dependencyNamespaces = dependencyNamespaces == null ? Set.of() : Set.copyOf(dependencyNamespaces);
            assetEntries = assetEntries == null ? Map.of() : copyAssetEntries(assetEntries);
            scanRoots = scanRoots == null ? List.of() : List.copyOf(scanRoots);
            fileStamps = fileStamps == null ? List.of() : List.copyOf(fileStamps);
            directoryStamps = directoryStamps == null ? List.of() : List.copyOf(directoryStamps);
        }
    }

    public record ResourceFingerprint(
        @NotNull String algorithm,
        int fileCount,
        long totalSizeBytes,
        long latestModifiedEpochMillis,
        @NotNull String digest
    ) {
        @NotNull
        public String stableValue() {
            return String.join(":",
                this.algorithm,
                Integer.toString(this.fileCount),
                Long.toString(this.totalSizeBytes),
                Long.toString(this.latestModifiedEpochMillis),
                this.digest
            );
        }
    }

    private record FileMetadata(@NotNull String stablePath, long size, long lastModifiedEpochMillis) {
    }

    public record ScanRoot(@NotNull String stablePath, @NotNull String path, boolean exists, long lastModifiedEpochMillis) {
    }

    public record FileStamp(@NotNull String stablePath, @NotNull String path, long size, long lastModifiedEpochMillis) {
    }

    public record DirectoryStamp(@NotNull String stablePath, @NotNull String path, long lastModifiedEpochMillis) {
    }

    @NotNull
    private static Map<String, String> immutableStringMap(Map<String, String> values) {
        return values == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(values));
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

    private static void collectModelDependencyNamespaces(@NotNull Path path, @NotNull String ownerNamespace, @NotNull Set<String> dependencyNamespaces) {
        JsonObject root = readObject(path);
        if (root == null) {
            return;
        }

        collectDependencyNamespace(root.get("parent"), ownerNamespace, dependencyNamespaces);

        JsonObject textures = object(root.get("textures"));
        if (textures != null) {
            for (Map.Entry<String, JsonElement> entry : textures.entrySet()) {
                collectDependencyNamespace(entry.getValue(), ownerNamespace, dependencyNamespaces);
            }
        }

        JsonArray overrides = array(root.get("overrides"));
        if (overrides == null) {
            return;
        }

        for (JsonElement overrideElement : overrides) {
            JsonObject override = object(overrideElement);
            if (override != null) {
                collectDependencyNamespace(override.get("model"), ownerNamespace, dependencyNamespaces);
            }
        }
    }

    private static void collectEquipmentDependencyNamespaces(@NotNull Path path, @NotNull String ownerNamespace, @NotNull Set<String> dependencyNamespaces) {
        JsonObject root = readObject(path);
        if (root == null) {
            return;
        }

        JsonObject layers = object(root.get("layers"));
        if (layers == null) {
            return;
        }

        for (Map.Entry<String, JsonElement> entry : layers.entrySet()) {
            JsonArray values = array(entry.getValue());
            if (values == null) {
                continue;
            }
            for (JsonElement element : values) {
                JsonObject layer = object(element);
                if (layer != null) {
                    collectDependencyNamespace(layer.get("texture"), ownerNamespace, dependencyNamespaces);
                }
            }
        }
    }

    private static void collectDependencyNamespace(@Nullable JsonElement element, @NotNull String ownerNamespace, @NotNull Set<String> dependencyNamespaces) {
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
            return;
        }

        String raw = element.getAsString();
        if (raw.isBlank() || raw.startsWith("#")) {
            return;
        }

        try {
            Key key = raw.indexOf(':') >= 0 ? Key.key(raw) : Key.key(ownerNamespace, raw);
            if (!ownerNamespace.equals(key.namespace()) && !Key.MINECRAFT_NAMESPACE.equals(key.namespace())) {
                dependencyNamespaces.add(key.namespace());
            }
        } catch (IllegalArgumentException ignored) {
        }
    }

    @Nullable
    private static JsonObject readObject(@NotNull Path path) {
        try (java.io.Reader reader = Files.newBufferedReader(path)) {
            return object(JsonParser.parseReader(reader));
        } catch (IOException | RuntimeException ignored) {
            return null;
        }
    }

    @Nullable
    private static JsonObject object(@Nullable JsonElement element) {
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }

    @Nullable
    private static JsonArray array(@Nullable JsonElement element) {
        return element != null && element.isJsonArray() ? element.getAsJsonArray() : null;
    }

    @Nullable
    private static Key textureKey(@NotNull String namespace, @NotNull Path relativePath) {
        String normalized = relativePath.toString().replace('\\', '/');
        if (normalized.endsWith(".png") || normalized.endsWith(".tga")) {
            normalized = normalized.substring(0, normalized.length() - 4);
        }
        if (normalized.isEmpty()) {
            return null;
        }
        return Key.key(namespace, normalized);
    }
}
