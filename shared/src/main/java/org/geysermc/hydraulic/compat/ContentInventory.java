package org.geysermc.hydraulic.compat;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ContentInventory {
    private final Map<String, ModContentInventory> mods;

    public ContentInventory(@NotNull Map<String, ModContentInventory> mods) {
        this.mods = Collections.unmodifiableMap(new LinkedHashMap<>(mods));
    }

    @NotNull
    public static ContentInventory empty() {
        return new ContentInventory(Map.of());
    }

    @NotNull
    public Map<String, ModContentInventory> mods() {
        return this.mods;
    }

    public record ModContentInventory(
        @NotNull String modId,
        @NotNull String namespace,
        @NotNull String name,
        @NotNull String version,
        @NotNull List<String> roots,
        @NotNull Map<String, Integer> registryCounts,
        @NotNull Map<String, Integer> assetCounts,
        int metadataMappings
    ) {
        public ModContentInventory {
            roots = List.copyOf(roots);
            registryCounts = Collections.unmodifiableMap(new LinkedHashMap<>(registryCounts));
            assetCounts = Collections.unmodifiableMap(new LinkedHashMap<>(assetCounts));
        }
    }
}