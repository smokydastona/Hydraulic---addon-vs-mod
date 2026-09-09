package org.geysermc.hydraulic.compat.runtime;

import org.geysermc.hydraulic.compat.CompatibilityRegistry;
import org.geysermc.hydraulic.compat.ir.CompiledCompatibilityPlan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
        List<CompiledCompatibilityPlan> blockedMenus = compatibilityRegistry.dispatchTable().menuBridgePlans();
        StringBuilder builder = new StringBuilder("Geyser could not open Java container type ")
            .append(containerTypeName);
        if (title != null && !title.isBlank()) {
            builder.append(" (title: ").append(title).append(')');
        }
        builder.append(" because no inventory translator exists for this open-screen path. ");
        builder.append("Hydraulic could not resolve a metadata-backed fallback layout for this open-screen path. ");
        builder.append("This bridge only activates when Hydraulic can confirm the live Java menu identifier from server state and metadata declares a compatible fallback ContainerType.");

        if (blockedMenus.isEmpty()) {
            builder.append(" The current compatibility report has no discovered menu objects marked with the container_bridge runtime requirement.");
            return builder.toString();
        }

        builder.append(" Compatibility report candidates still requiring container_bridge: ");
        appendCandidates(builder, blockedMenus);
        return builder.toString();
    }

    private static void appendCandidates(@NotNull StringBuilder builder, @NotNull List<CompiledCompatibilityPlan> blockedMenus) {
        int displayCount = Math.min(blockedMenus.size(), 3);
        for (int i = 0; i < displayCount; i++) {
            CompiledCompatibilityPlan plan = blockedMenus.get(i);
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(plan.javaIdentifier());
        }

        int remaining = blockedMenus.size() - displayCount;
        if (remaining > 0) {
            builder.append(" (+").append(remaining).append(" more)");
        }
        builder.append('.');
    }
}