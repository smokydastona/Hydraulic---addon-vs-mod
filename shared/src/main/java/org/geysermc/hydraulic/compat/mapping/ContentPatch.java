package org.geysermc.hydraulic.compat.mapping;

import net.minecraft.resources.Identifier;
import org.geysermc.hydraulic.compat.MappingOwnership;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record ContentPatch(
    @NotNull Identifier target,
    @Nullable String contentType,
    @NotNull Map<String, String> operations,
    @NotNull MappingOwnership ownership,
    @NotNull String sourcePath,
    int priority,
    int order
) {
    public ContentPatch {
        operations = Collections.unmodifiableMap(new LinkedHashMap<>(operations));
    }

    public boolean hasOperationPrefix(@NotNull String prefix) {
        return this.operations.keySet().stream().anyMatch(key -> key.startsWith(prefix));
    }

    public boolean booleanOperation(@NotNull String key) {
        return Boolean.parseBoolean(this.operations.getOrDefault(key, "false"));
    }

    @Nullable
    public String operation(@NotNull String key) {
        return this.operations.get(key);
    }
}