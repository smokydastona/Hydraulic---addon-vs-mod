package org.geysermc.hydraulic.metadata;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;
import org.geysermc.hydraulic.Constants;
import org.geysermc.hydraulic.compat.MappingOwnership;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public final class MetadataLoader {
    private final Logger logger;

    public MetadataLoader(@NotNull Logger logger) {
        this.logger = logger;
    }

    @NotNull
    public MetadataIndex load(@NotNull Path directory) {
        if (Files.notExists(directory)) {
            return MetadataIndex.empty();
        }

        Map<Identifier, List<BlockStateRule>> blockMappings = new LinkedHashMap<>();
        Map<Identifier, IdentifierMapping> itemMappings = new LinkedHashMap<>();
        Map<Identifier, IdentifierMapping> recipeMappings = new LinkedHashMap<>();
        Map<String, Integer> ownershipFileCounts = new LinkedHashMap<>();
        int fileCount = 0;
        int blockMappingCount = 0;
        int itemMappingCount = 0;
        int recipeMappingCount = 0;
        int ruleCount = 0;

        try (Stream<Path> stream = Files.walk(directory)) {
            List<Path> files = stream
                .filter(Files::isRegularFile)
                .filter(file -> file.toString().endsWith(".json"))
                .sorted(Comparator.comparing(path -> normalize(directory.relativize(path))))
                .toList();

            for (Path path : files) {
                LoadStats stats = this.loadFile(directory, path, blockMappings, itemMappings, recipeMappings, ownershipFileCounts);
                fileCount += stats.fileCount();
                blockMappingCount += stats.blockMappingCount();
                itemMappingCount += stats.itemMappingCount();
                recipeMappingCount += stats.recipeMappingCount();
                ruleCount += stats.ruleCount();
            }
        } catch (IOException e) {
            this.logger.error("Failed to list metadata directory {}", directory, e);
        }

        Map<Identifier, BlockMapping> finalizedMappings = new LinkedHashMap<>();
        for (Map.Entry<Identifier, List<BlockStateRule>> entry : blockMappings.entrySet()) {
            List<BlockStateRule> rules = new ArrayList<>(entry.getValue());
            rules.sort(
                Comparator.comparingInt(BlockStateRule::priority).reversed()
                    .thenComparing(Comparator.comparingInt(BlockStateRule::specificity).reversed())
                    .thenComparing(BlockStateRule::sourcePath)
                    .thenComparingInt(BlockStateRule::order)
            );
            finalizedMappings.put(entry.getKey(), new BlockMapping(entry.getKey(), List.copyOf(rules)));
        }

        return new MetadataIndex(
            finalizedMappings,
            itemMappings,
            recipeMappings,
            new MetadataIndex.Summary(fileCount, blockMappingCount, itemMappingCount, recipeMappingCount, ruleCount, ownershipFileCounts)
        );
    }

    @NotNull
    private LoadStats loadFile(
        @NotNull Path rootDirectory,
        @NotNull Path path,
        @NotNull Map<Identifier, List<BlockStateRule>> blockMappings,
        @NotNull Map<Identifier, IdentifierMapping> itemMappings,
        @NotNull Map<Identifier, IdentifierMapping> recipeMappings,
        @NotNull Map<String, Integer> ownershipFileCounts
    ) {
        Path relativePath = rootDirectory.relativize(path);
        String sourcePath = normalize(relativePath);
        MappingOwnership ownership = MappingOwnership.fromRelativePath(relativePath);

        try (BufferedReader reader = Files.newBufferedReader(path)) {
            JsonObject jsonRoot = Constants.GSON.fromJson(reader, JsonObject.class);
            if (jsonRoot == null) {
                this.logger.warn("Ignoring empty metadata file {}", path);
                return LoadStats.empty();
            }

            List<JsonObject> blockObjects = new ArrayList<>();
            JsonArray blocks = jsonRoot.getAsJsonArray("blocks");
            if (blocks != null) {
                for (JsonElement element : blocks) {
                    if (element.isJsonObject()) {
                        blockObjects.add(element.getAsJsonObject());
                    }
                }
            } else if (jsonRoot.has("java_id")) {
                blockObjects.add(jsonRoot);
            }

            JsonArray itemObjects = jsonRoot.getAsJsonArray("items");
            JsonArray recipeObjects = jsonRoot.getAsJsonArray("recipes");
            if (blockObjects.isEmpty() && itemObjects == null && recipeObjects == null) {
                this.logger.warn("Ignoring metadata file without blocks, items, recipes, or java_id in {}", path);
                return LoadStats.empty();
            }

            int blockMappingCount = 0;
            int itemMappingCount = 0;
            int recipeMappingCount = 0;
            int ruleCount = 0;
            for (int index = 0; index < blockObjects.size(); index++) {
                BlockMapping mapping = this.parseBlockMapping(blockObjects.get(index), path, ownership, sourcePath, index);
                if (mapping == null) {
                    continue;
                }

                blockMappings.computeIfAbsent(mapping.javaIdentifier(), ignored -> new ArrayList<>()).addAll(mapping.rules());
                blockMappingCount++;
                ruleCount += mapping.rules().size();
            }

            itemMappingCount += this.parseIdentifierMappings(itemObjects, "item", path, ownership, sourcePath, itemMappings);
            recipeMappingCount += this.parseIdentifierMappings(recipeObjects, "recipe", path, ownership, sourcePath, recipeMappings);

            ownershipFileCounts.merge(ownership.name().toLowerCase(), 1, Integer::sum);
            return new LoadStats(1, blockMappingCount, itemMappingCount, recipeMappingCount, ruleCount);
        } catch (Exception e) {
            this.logger.error("Failed to load metadata file {}", path, e);
            return LoadStats.empty();
        }
    }

    private int parseIdentifierMappings(
        @Nullable JsonArray array,
        @NotNull String kind,
        @NotNull Path path,
        @NotNull MappingOwnership ownership,
        @NotNull String sourcePath,
        @NotNull Map<Identifier, IdentifierMapping> mappings
    ) {
        if (array == null) {
            return 0;
        }

        int count = 0;
        int basePriority = ownership.priority();
        for (int index = 0; index < array.size(); index++) {
            JsonElement element = array.get(index);
            if (!element.isJsonObject()) {
                continue;
            }

            IdentifierMapping mapping = this.parseIdentifierMapping(element.getAsJsonObject(), kind, path, ownership, sourcePath, basePriority, index);
            if (mapping == null) {
                continue;
            }

            IdentifierMapping current = mappings.get(mapping.javaIdentifier());
            if (current == null || this.hasHigherPrecedence(mapping, current)) {
                mappings.put(mapping.javaIdentifier(), mapping);
            }
            count++;
        }
        return count;
    }

    @Nullable
    private BlockMapping parseBlockMapping(
        @NotNull JsonObject object,
        @NotNull Path path,
        @NotNull MappingOwnership ownership,
        @NotNull String sourcePath,
        int mappingIndex
    ) {
        Identifier javaIdentifier = this.parseIdentifier(object, "java_id", path, true);
        if (javaIdentifier == null) {
            return null;
        }

        JsonArray rulesArray = object.getAsJsonArray("rules");
        if (rulesArray == null) {
            this.logger.warn("Ignoring block metadata without rules for {} in {}", javaIdentifier, path);
            return null;
        }

        List<BlockStateRule> rules = new ArrayList<>();
        int basePriority = ownership.priority();
        for (JsonElement ruleElement : rulesArray) {
            if (!ruleElement.isJsonObject()) {
                continue;
            }

            BlockStateRule rule = this.parseRule(
                ruleElement.getAsJsonObject(),
                path,
                ownership,
                sourcePath,
                basePriority,
                (mappingIndex * 10_000) + rules.size()
            );
            if (rule != null) {
                rules.add(rule);
            }
        }

        if (rules.isEmpty()) {
            this.logger.warn("Ignoring block metadata without valid rules for {} in {}", javaIdentifier, path);
            return null;
        }

        return new BlockMapping(javaIdentifier, List.copyOf(rules));
    }

    @Nullable
    private BlockStateRule parseRule(
        @NotNull JsonObject object,
        @NotNull Path path,
        @NotNull MappingOwnership ownership,
        @NotNull String sourcePath,
        int priority,
        int order
    ) {
        Map<String, String> javaWhen = this.parseStringMap(object.getAsJsonObject("java_when"));
        Identifier bedrockIdentifier = this.parseIdentifier(object, "bedrock_identifier", path, false);
        Map<String, String> bedrockState = this.parseStringMap(object.getAsJsonObject("bedrock_state"));
        String geometryId = this.optionalString(object, "geometry");
        String materialId = this.optionalString(object, "material");
        boolean behaviorRequired = object.has("behavior_required") && object.get("behavior_required").getAsBoolean();
        String behaviorTag = this.optionalString(object, "behavior_tag");

        return new BlockStateRule(
            Map.copyOf(javaWhen),
            bedrockIdentifier,
            bedrockState.isEmpty() ? null : Map.copyOf(bedrockState),
            geometryId,
            materialId,
            behaviorRequired,
            behaviorTag,
            ownership,
            sourcePath,
            priority,
            order
        );
    }

    @Nullable
    private IdentifierMapping parseIdentifierMapping(
        @NotNull JsonObject object,
        @NotNull String kind,
        @NotNull Path path,
        @NotNull MappingOwnership ownership,
        @NotNull String sourcePath,
        int priority,
        int order
    ) {
        Identifier javaIdentifier = this.parseIdentifier(object, "java_id", path, true);
        Identifier bedrockIdentifier = this.parseIdentifier(object, "bedrock_identifier", path, true);
        if (javaIdentifier == null || bedrockIdentifier == null) {
            this.logger.warn("Ignoring {} metadata missing valid identifiers in {}", kind, path);
            return null;
        }

        return new IdentifierMapping(javaIdentifier, bedrockIdentifier, ownership, sourcePath, priority, order);
    }

    private boolean hasHigherPrecedence(@NotNull IdentifierMapping candidate, @NotNull IdentifierMapping existing) {
        if (candidate.priority() != existing.priority()) {
            return candidate.priority() > existing.priority();
        }
        int sourceCompare = candidate.sourcePath().compareTo(existing.sourcePath());
        if (sourceCompare != 0) {
            return sourceCompare < 0;
        }
        return candidate.order() < existing.order();
    }

    @NotNull
    private static String normalize(@NotNull Path path) {
        return path.toString().replace('\\', '/');
    }

    @NotNull
    private Map<String, String> parseStringMap(@Nullable JsonObject object) {
        if (object == null) {
            return Map.of();
        }

        Map<String, String> values = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            values.put(entry.getKey(), entry.getValue().getAsString());
        }
        return values;
    }

    @Nullable
    private Identifier parseIdentifier(@NotNull JsonObject object, @NotNull String key, @NotNull Path path, boolean required) {
        String value = this.optionalString(object, key);
        if (value == null) {
            if (required) {
                this.logger.warn("Ignoring metadata entry missing {} in {}", key, path);
            }
            return null;
        }

        int separator = value.indexOf(':');
        if (separator <= 0 || separator == value.length() - 1) {
            this.logger.warn("Ignoring invalid identifier {} in {}", value, path);
            return null;
        }

        return Identifier.fromNamespaceAndPath(value.substring(0, separator), value.substring(separator + 1));
    }

    @Nullable
    private String optionalString(@NotNull JsonObject object, @NotNull String key) {
        if (!object.has(key) || object.get(key).isJsonNull()) {
            return null;
        }
        return object.get(key).getAsString();
    }

    private record LoadStats(int fileCount, int blockMappingCount, int itemMappingCount, int recipeMappingCount, int ruleCount) {
        @NotNull
        private static LoadStats empty() {
            return new LoadStats(0, 0, 0, 0, 0);
        }
    }
}