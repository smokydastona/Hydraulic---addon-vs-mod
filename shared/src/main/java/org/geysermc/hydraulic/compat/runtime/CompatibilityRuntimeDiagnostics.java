package org.geysermc.hydraulic.compat.runtime;

import org.geysermc.hydraulic.HydraulicImpl;
import org.geysermc.hydraulic.compat.CompatibilityRegistry;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.ContainerType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class CompatibilityRuntimeDiagnostics {
    private static final Logger LOGGER = LoggerFactory.getLogger("HydraulicCompatibilityRuntime");
    private static final Set<String> WARNED_UNSUPPORTED_MENUS = ConcurrentHashMap.newKeySet();

    private CompatibilityRuntimeDiagnostics() {
    }

    public static void reportUnsupportedMenuOpen(@NotNull ContainerType containerType, @Nullable String title) {
        String warningKey = containerType.name() + '|' + normalizeTitle(title);
        if (!WARNED_UNSUPPORTED_MENUS.add(warningKey)) {
            return;
        }

        LOGGER.warn(UnsupportedMenuDiagnosticFormatter.format(containerType.name(), title, currentRegistry()));
    }

    @NotNull
    public static String unsupportedMenuReason(
        @NotNull ContainerType containerType,
        @Nullable String title,
        @NotNull CompatibilityRegistry compatibilityRegistry
    ) {
        return UnsupportedMenuDiagnosticFormatter.format(containerType.name(), title, compatibilityRegistry);
    }

    @NotNull
    static String unsupportedMenuReason(
        @NotNull String containerTypeName,
        @Nullable String title,
        @NotNull CompatibilityRegistry compatibilityRegistry
    ) {
        return UnsupportedMenuDiagnosticFormatter.format(containerTypeName, title, compatibilityRegistry);
    }

    @NotNull
    private static CompatibilityRegistry currentRegistry() {
        try {
            return HydraulicImpl.instance().getPackManager().compatibilityRegistry();
        } catch (IllegalStateException ignored) {
            return CompatibilityRegistry.empty();
        }
    }

    @NotNull
    private static String normalizeTitle(@Nullable String title) {
        return title == null ? "" : title.trim().toLowerCase();
    }
}