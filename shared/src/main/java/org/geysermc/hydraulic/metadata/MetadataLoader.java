package org.geysermc.hydraulic.metadata;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;
import org.geysermc.hydraulic.Constants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
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
            return new MetadataIndex(Map.of());
        }

        Map<Identifier, BlockMapping> blockMappings = new HashMap<>();

        try (Stream<Path> stream = Files.list(directory)) {
            for (Path path : stream.filter(Files::isRegularFile).filter(file -> file.toString().endsWith(".json")).toList()) {
                this.loadFile(path, blockMappings);
            }
        } catch (IOException e) {
            this.logger.error("Failed to list metadata directory {}", directory, e);
        }

        return new MetadataIndex(Map.copyOf(blockMappings));
    }

    private void loadFile(@NotNull Path path, @NotNull Map<Identifier, BlockMapping> blockMappings) {
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            JsonObject root = Constants.GSON.fromJson(reader, JsonObject.class);
            if (root == null) {
                this.logger.warn("Ignoring empty metadata file {}", path);
                return;
            }

            JsonArray blocks = root.getAsJsonArray("blocks");
            if (blocks == null) {
                return;
            }

            for (JsonElement element : blocks) {
                if (!element.isJsonObject()) {
                    continue;
                }

                BlockMapping mapping = this.parseBlockMapping(element.getAsJsonObject(), path);
                if (mapping == null) {
                    continue;
                }

                BlockMapping previous = blockMappings.put(mapping.javaIdentifier(), mapping);
                if (previous != null) {
                    this.logger.warn("Replacing metadata mapping for {} from {}", mapping.javaIdentifier(), path);
                }
            }
        } catch (Exception e) {
            this.logger.error("Failed to load metadata file {}", path, e);
        }
    }

    @Nullable
    private BlockMapping parseBlockMapping(@NotNull JsonObject object, @NotNull Path path) {
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
        for (JsonElement ruleElement : rulesArray) {
            if (!ruleElement.isJsonObject()) {
                continue;
            }

            BlockStateRule rule = this.parseRule(ruleElement.getAsJsonObject(), path);
            if (rule != null) {
                rules.add(rule);
            }
        }

        return new BlockMapping(javaIdentifier, List.copyOf(rules));
    }

    @Nullable
    private BlockStateRule parseRule(@NotNull JsonObject object, @NotNull Path path) {
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
            behaviorTag
        );
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
}