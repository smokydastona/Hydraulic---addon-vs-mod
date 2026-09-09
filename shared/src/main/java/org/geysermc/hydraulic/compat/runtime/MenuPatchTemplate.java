package org.geysermc.hydraulic.compat.runtime;

import org.geysermc.hydraulic.compat.mapping.ContentPatch;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class MenuPatchTemplate {
    private static final String FALLBACK_CONTAINER_TYPE = "bedrock.menu.container_type";
    private static final Set<String> SUPPORTED_CONTAINER_TYPES = Set.of(
        "GENERIC_9X1",
        "GENERIC_9X2",
        "GENERIC_9X3",
        "GENERIC_9X4",
        "GENERIC_9X5",
        "GENERIC_9X6",
        "GENERIC_3X3",
        "CRAFTER_3X3",
        "ANVIL",
        "BEACON",
        "BLAST_FURNACE",
        "BREWING_STAND",
        "CRAFTING",
        "ENCHANTMENT",
        "FURNACE",
        "GRINDSTONE",
        "HOPPER",
        "LECTERN",
        "LOOM",
        "MERCHANT",
        "SHULKER_BOX",
        "SMITHING",
        "SMOKER",
        "CARTOGRAPHY",
        "STONECUTTER"
    );

    private final @NotNull String fallbackContainerType;

    private MenuPatchTemplate(@NotNull String fallbackContainerType) {
        this.fallbackContainerType = fallbackContainerType;
    }

    @Nullable
    public static MenuPatchTemplate resolve(@NotNull List<ContentPatch> patches) {
        String fallbackContainerType = null;
        for (ContentPatch patch : patches) {
            String rawValue = patch.operation(FALLBACK_CONTAINER_TYPE);
            if (rawValue == null) {
                continue;
            }

            String normalized = normalizeContainerTypeName(rawValue);
            if (isSupportedContainerType(normalized)) {
                fallbackContainerType = normalized;
            }
        }

        return fallbackContainerType != null ? new MenuPatchTemplate(fallbackContainerType) : null;
    }

    public static boolean supports(@NotNull List<ContentPatch> patches) {
        return resolve(patches) != null;
    }

    @Nullable
    public static String normalizeContainerTypeName(@Nullable String rawValue) {
        if (rawValue == null) {
            return null;
        }

        String normalized = rawValue.trim();
        if (normalized.isEmpty()) {
            return null;
        }

        normalized = normalized
            .replace('-', '_')
            .replace('.', '_')
            .toUpperCase(Locale.ROOT);
        return normalized;
    }

    @NotNull
    public String fallbackContainerType() {
        return this.fallbackContainerType;
    }

    public static boolean isSupportedContainerType(@Nullable String normalizedContainerType) {
        return normalizedContainerType != null && SUPPORTED_CONTAINER_TYPES.contains(normalizedContainerType);
    }
}