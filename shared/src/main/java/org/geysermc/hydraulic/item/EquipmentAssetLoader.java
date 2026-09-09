package org.geysermc.hydraulic.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.kyori.adventure.key.Key;
import net.minecraft.resources.Identifier;
import org.geysermc.hydraulic.Constants;
import org.geysermc.hydraulic.platform.mod.ModInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import team.unnamed.creative.equipment.EquipmentLayerType;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

public final class EquipmentAssetLoader {
    private EquipmentAssetLoader() {
    }

    @Nullable
    static EquipmentAsset load(@NotNull ModInfo mod, @NotNull Identifier assetId, @NotNull Logger logger) {
        Path path = mod.resolveFile("assets/" + assetId.getNamespace() + "/equipment/" + assetId.getPath() + ".json");
        if (path == null) {
            logger.warn("Equipment asset {} is missing for mod {}", assetId, mod.id());
            return null;
        }

        try (Reader reader = Files.newBufferedReader(path)) {
            JsonObject root = Constants.GSON.fromJson(reader, JsonObject.class);
            if (root == null || !root.has("layers") || !root.get("layers").isJsonObject()) {
                logger.warn("Equipment asset {} in {} has no layers object", assetId, path);
                return null;
            }

            Map<EquipmentLayerType, List<Key>> layers = new EnumMap<>(EquipmentLayerType.class);
            JsonObject layerObject = root.getAsJsonObject("layers");
            for (Map.Entry<String, JsonElement> entry : layerObject.entrySet()) {
                EquipmentLayerType layerType = parseLayerType(entry.getKey());
                if (layerType == null) {
                    logger.warn("Ignoring unsupported equipment layer {} in {}", entry.getKey(), path);
                    continue;
                }
                if (!entry.getValue().isJsonArray()) {
                    logger.warn("Ignoring non-array equipment layer {} in {}", entry.getKey(), path);
                    continue;
                }

                List<Key> textures = parseTextures(entry.getValue().getAsJsonArray(), path, entry.getKey(), logger);
                if (!textures.isEmpty()) {
                    layers.put(layerType, textures);
                }
            }

            if (layers.isEmpty()) {
                logger.warn("Equipment asset {} in {} did not yield any usable layers", assetId, path);
                return null;
            }
            return new EquipmentAsset(layers);
        } catch (IOException | RuntimeException exception) {
            logger.warn("Failed to read equipment asset {} from {}", assetId, path, exception);
            return null;
        }
    }

    public static void collectTextureDependencies(@NotNull ModInfo mod, @NotNull Identifier assetId, @NotNull Logger logger, @NotNull Consumer<Key> sink) {
        EquipmentAsset equipment = load(mod, assetId, logger);
        if (equipment == null) {
            return;
        }

        for (List<Key> textures : equipment.layers.values()) {
            for (Key texture : textures) {
                sink.accept(texture);
            }
        }
    }

    @Nullable
    private static EquipmentLayerType parseLayerType(@NotNull String layerName) {
        try {
            return EquipmentLayerType.valueOf(layerName.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    @NotNull
    private static List<Key> parseTextures(@NotNull JsonArray layerEntries, @NotNull Path path, @NotNull String layerName, @NotNull Logger logger) {
        List<Key> textures = new ArrayList<>();
        for (JsonElement element : layerEntries) {
            if (!element.isJsonObject()) {
                logger.warn("Ignoring non-object entry in equipment layer {} from {}", layerName, path);
                continue;
            }

            JsonObject layer = element.getAsJsonObject();
            if (!layer.has("texture") || !layer.get("texture").isJsonPrimitive()) {
                logger.warn("Ignoring equipment layer {} entry without texture in {}", layerName, path);
                continue;
            }

            try {
                textures.add(Key.key(layer.get("texture").getAsString()));
            } catch (IllegalArgumentException exception) {
                logger.warn("Ignoring invalid equipment texture {} in {}", layer.get("texture").getAsString(), path);
            }
        }
        return List.copyOf(textures);
    }

    record EquipmentAsset(@NotNull Map<EquipmentLayerType, List<Key>> layers) {
        EquipmentAsset {
            layers = Collections.unmodifiableMap(new EnumMap<>(layers));
        }

        @Nullable
        List<Key> layers(@NotNull EquipmentLayerType layerType) {
            return this.layers.get(layerType);
        }
    }
}