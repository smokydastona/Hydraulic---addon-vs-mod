package org.geysermc.hydraulic.pack;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.geysermc.hydraulic.Constants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;

public final class TextureAnimationMetadataReader {
    private TextureAnimationMetadataReader() {
    }

    @Nullable
    public static Integer frameTime(@NotNull Path texturePath) {
        Path metadataPath = texturePath.resolveSibling(texturePath.getFileName() + ".mcmeta");
        if (!Files.isRegularFile(metadataPath)) {
            return null;
        }

        try (Reader reader = Files.newBufferedReader(metadataPath)) {
            JsonObject root = Constants.GSON.fromJson(reader, JsonObject.class);
            if (root == null) {
                return null;
            }

            JsonObject animation = object(root.get("animation"));
            if (animation == null) {
                return null;
            }

            JsonElement frameTime = animation.get("frametime");
            if (frameTime != null && frameTime.isJsonPrimitive() && frameTime.getAsJsonPrimitive().isNumber()) {
                int value = frameTime.getAsInt();
                return value > 0 ? value : 1;
            }
            return 1;
        } catch (IOException | RuntimeException ignored) {
            return null;
        }
    }

    @Nullable
    private static JsonObject object(@Nullable JsonElement element) {
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }
}