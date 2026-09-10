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
        @Nullable String javaIdentifier,
        @NotNull CompatibilityRegistry compatibilityRegistry
    ) {
        List<CompiledCompatibilityPlan> blockedMenus = compatibilityRegistry.dispatchTable().runtimeBridgePlans(RuntimeBridgeKind.menuRuntimeKinds());
        CompiledCompatibilityPlan matchedPlan = javaIdentifier != null ? compatibilityRegistry.dispatchTable().plan("menu", javaIdentifier) : null;
        StringBuilder builder = new StringBuilder("Geyser could not open Java container type ")
            .append(containerTypeName);
        if (title != null && !title.isBlank()) {
            builder.append(" (title: ").append(title).append(')');
        }
        if (javaIdentifier != null && !javaIdentifier.isBlank()) {
            builder.append(" for ").append(javaIdentifier);
        }
        builder.append(" because no inventory translator exists for this open-screen path. ");
        builder.append("Hydraulic could not resolve a metadata-backed fallback layout for this open-screen path. ");
        builder.append("This bridge only activates when Hydraulic can confirm the live Java menu identifier from server state and metadata declares a compatible fallback ContainerType.");

        if (matchedPlan != null && !matchedPlan.menuRuntimeRequirements().isEmpty()) {
            builder.append(' ')
                .append("Compatibility report still marks this menu as requiring ");
            appendRequirements(builder, matchedPlan.menuRuntimeRequirements());
            builder.append('.');
        }

        if (blockedMenus.isEmpty()) {
            builder.append(" The current compatibility report has no discovered menu objects marked with the ")
                .append(RuntimeBridgeKind.MENU_CONTAINER.requirementId())
                .append(" runtime requirement.");
            return builder.toString();
        }

        builder.append(" Compatibility report candidates still requiring menu runtime bridges: ");
        appendCandidates(builder, blockedMenus, matchedPlan);
        return builder.toString();
    }

    private static void appendRequirements(@NotNull StringBuilder builder, @NotNull List<String> runtimeRequirements) {
        for (int i = 0; i < runtimeRequirements.size(); i++) {
            if (i > 0) {
                builder.append(i == runtimeRequirements.size() - 1 ? " and " : ", ");
            }
            builder.append(runtimeRequirements.get(i));
        }
    }

    private static void appendCandidates(
        @NotNull StringBuilder builder,
        @NotNull List<CompiledCompatibilityPlan> blockedMenus,
        @Nullable CompiledCompatibilityPlan matchedPlan
    ) {
        List<CompiledCompatibilityPlan> candidates = blockedMenus.stream()
            .filter(plan -> matchedPlan == null || !plan.javaIdentifier().equals(matchedPlan.javaIdentifier()))
            .toList();
        if (candidates.isEmpty()) {
            builder.append("none.");
            return;
        }

        int displayCount = Math.min(candidates.size(), 3);
        for (int i = 0; i < displayCount; i++) {
            CompiledCompatibilityPlan plan = candidates.get(i);
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(plan.javaIdentifier());
        }

        int remaining = candidates.size() - displayCount;
        if (remaining > 0) {
            builder.append(" (+").append(remaining).append(" more)");
        }
        builder.append('.');
    }
}