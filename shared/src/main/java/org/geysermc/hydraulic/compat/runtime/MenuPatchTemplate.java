package org.geysermc.hydraulic.compat.runtime;

import org.geysermc.mcprotocollib.protocol.data.game.inventory.ContainerType;
import org.geysermc.hydraulic.compat.mapping.ContentPatch;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class MenuPatchTemplate {
    private static final String FALLBACK_CONTAINER_TYPE = "bedrock.menu.container_type";
    private static final Map<String, ContainerType> CONTAINER_TYPES_BY_NORMALIZED_NAME =
        java.util.Arrays.stream(ContainerType.values())
            .collect(Collectors.toUnmodifiableMap(
                type -> normalizeContainerTypeName(type.name()),
                type -> type,
                (left, right) -> left
            ));

    private final @NotNull ContainerType fallbackContainerType;

    private MenuPatchTemplate(@NotNull ContainerType fallbackContainerType) {
        this.fallbackContainerType = fallbackContainerType;
    }

    @Nullable
    public static MenuPatchTemplate resolve(@NotNull List<ContentPatch> patches) {
        ContainerType fallbackContainerType = null;
        for (ContentPatch patch : patches) {
            ContainerType resolved = parseContainerType(patch.operation(FALLBACK_CONTAINER_TYPE));
            if (resolved == null) {
                continue;
            }

            fallbackContainerType = resolved;
        }

        return fallbackContainerType != null ? new MenuPatchTemplate(fallbackContainerType) : null;
    }

    public static boolean supports(@NotNull List<ContentPatch> patches) {
        return resolve(patches) != null;
    }

    @Nullable
    public static ContainerType parseContainerType(@Nullable String rawValue) {
        String normalized = normalizeContainerTypeName(rawValue);
        return normalized != null ? CONTAINER_TYPES_BY_NORMALIZED_NAME.get(normalized) : null;
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
    public ContainerType fallbackContainerType() {
        return this.fallbackContainerType;
    }

    public static boolean isSupportedContainerType(@Nullable String normalizedContainerType) {
        return normalizedContainerType != null && CONTAINER_TYPES_BY_NORMALIZED_NAME.containsKey(normalizedContainerType);
    }
}