package org.geysermc.hydraulic.compat.runtime;

import org.geysermc.hydraulic.compat.CompatibilityProfile;
import org.geysermc.hydraulic.compat.CompatibilityRegistry;
import org.geysermc.hydraulic.compat.model.CompatibilityObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class UnsupportedMenuDiagnosticFormatter {
    private UnsupportedMenuDiagnosticFormatter() {
    }

    @NotNull
    static String format(
        @NotNull String containerTypeName,
        @Nullable String title,
        @NotNull CompatibilityRegistry compatibilityRegistry
    ) {
        List<CompatibilityObject> blockedMenus = blockedMenus(compatibilityRegistry);
        StringBuilder builder = new StringBuilder("Geyser could not open Java container type ")
            .append(containerTypeName);
        if (title != null && !title.isBlank()) {
            builder.append(" (title: ").append(title).append(')');
        }
        builder.append(" because no inventory translator exists for this open-screen path. ");
        builder.append("Hydraulic cannot safely remap the menu from current metadata because Geyser exposes only ContainerType here, not the originating Java menu identifier.");

        if (blockedMenus.isEmpty()) {
            builder.append(" The current compatibility report has no discovered menu objects marked with the container_bridge runtime requirement.");
            return builder.toString();
        }

        builder.append(" Compatibility report candidates still requiring container_bridge: ");
        appendCandidates(builder, blockedMenus);
        return builder.toString();
    }

    @NotNull
    private static List<CompatibilityObject> blockedMenus(@NotNull CompatibilityRegistry compatibilityRegistry) {
        List<CompatibilityObject> blockedMenus = new ArrayList<>();
        for (CompatibilityProfile profile : compatibilityRegistry.report().mods().values()) {
            for (CompatibilityObject object : profile.objects()) {
                if (!"menu".equals(object.contentType())) {
                    continue;
                }
                if (!object.runtimeRequirements().contains("container_bridge")) {
                    continue;
                }
                blockedMenus.add(object);
            }
        }

        blockedMenus.sort(Comparator
            .comparing(CompatibilityObject::modId)
            .thenComparing(CompatibilityObject::javaIdentifier));
        return List.copyOf(blockedMenus);
    }

    private static void appendCandidates(@NotNull StringBuilder builder, @NotNull List<CompatibilityObject> blockedMenus) {
        int displayCount = Math.min(blockedMenus.size(), 3);
        for (int i = 0; i < displayCount; i++) {
            CompatibilityObject object = blockedMenus.get(i);
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(object.javaIdentifier());
        }

        int remaining = blockedMenus.size() - displayCount;
        if (remaining > 0) {
            builder.append(" (+").append(remaining).append(" more)");
        }
        builder.append('.');
    }
}