package org.geysermc.hydraulic.compat.runtime;

import org.geysermc.hydraulic.compat.CompatibilityRegistry;
import org.geysermc.hydraulic.compat.ir.CompiledCompatibilityPlan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

final class UnsupportedBlockEntityDiagnosticFormatter {
    private UnsupportedBlockEntityDiagnosticFormatter() {
    }

    @Nullable
    static String format(
        @NotNull String protocolTypeName,
        @Nullable String javaIdentifier,
        @NotNull String position,
        @NotNull CompatibilityRegistry compatibilityRegistry
    ) {
        List<CompiledCompatibilityPlan> blockedBlockEntities = compatibilityRegistry.dispatchTable().runtimeBridgePlans(RuntimeBridgeKind.blockEntityRuntimeKinds());
        CompiledCompatibilityPlan matchedObject = javaIdentifier != null ? compatibilityRegistry.dispatchTable().plan("block_entity", javaIdentifier) : null;
        if (matchedObject != null && !matchedObject.requiresBlockEntityRuntime()) {
            return null;
        }
        if (matchedObject == null && blockedBlockEntities.isEmpty()) {
            return null;
        }

        StringBuilder builder = new StringBuilder("Geyser received block entity data");
        if (javaIdentifier != null && !javaIdentifier.isBlank()) {
            builder.append(" for ").append(javaIdentifier);
        } else {
            builder.append(" for unresolved Java block entity");
        }
        builder.append(" at ").append(position)
            .append(" using the default EmptyBlockEntityTranslator")
            .append(" (protocol type: ").append(protocolTypeName).append("). ");

        if (matchedObject != null) {
            builder.append("Compatibility report still marks this object as requiring ");
            appendRequirements(builder, matchedObject.blockEntityRuntimeRequirements());
            builder.append(".");
        } else if (javaIdentifier != null && !javaIdentifier.isBlank()) {
            builder.append("Hydraulic has no matching block_entity compatibility object for this identifier.");
        } else {
            builder.append("Hydraulic could not resolve the live Java block entity identifier from the current server/session context.");
        }

        if (!blockedBlockEntities.isEmpty()) {
            builder.append(' ');
            if (matchedObject != null) {
                builder.append("Other discovered block entity objects still requiring runtime bridges: ");
            } else {
                builder.append("Discovered compatibility objects still requiring block-entity runtime bridges: ");
            }
            appendCandidates(builder, blockedBlockEntities, matchedObject);
        }

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
        @NotNull List<CompiledCompatibilityPlan> blockedBlockEntities,
        @Nullable CompiledCompatibilityPlan matchedObject
    ) {
        List<CompiledCompatibilityPlan> candidates = blockedBlockEntities.stream()
            .filter(object -> matchedObject == null || !object.javaIdentifier().equals(matchedObject.javaIdentifier()))
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