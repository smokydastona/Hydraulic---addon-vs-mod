package org.geysermc.hydraulic.compat.runtime;

import org.geysermc.hydraulic.compat.CompatibilityProfile;
import org.geysermc.hydraulic.compat.CompatibilityRegistry;
import org.geysermc.hydraulic.compat.model.CompatibilityObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
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
        List<CompatibilityObject> blockedBlockEntities = blockedBlockEntities(compatibilityRegistry);
        CompatibilityObject matchedObject = javaIdentifier != null ? blockEntity(javaIdentifier, compatibilityRegistry) : null;
        if (matchedObject != null && !requiresRuntimeBridge(matchedObject)) {
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
            appendRequirements(builder, matchedObject.runtimeRequirements());
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

    @Nullable
    private static CompatibilityObject blockEntity(@NotNull String javaIdentifier, @NotNull CompatibilityRegistry compatibilityRegistry) {
        for (CompatibilityProfile profile : compatibilityRegistry.report().mods().values()) {
            for (CompatibilityObject object : profile.objects()) {
                if ("block_entity".equals(object.contentType()) && javaIdentifier.equals(object.javaIdentifier())) {
                    return object;
                }
            }
        }
        return null;
    }

    @NotNull
    private static List<CompatibilityObject> blockedBlockEntities(@NotNull CompatibilityRegistry compatibilityRegistry) {
        List<CompatibilityObject> blockedBlockEntities = new ArrayList<>();
        for (CompatibilityProfile profile : compatibilityRegistry.report().mods().values()) {
            for (CompatibilityObject object : profile.objects()) {
                if (!"block_entity".equals(object.contentType())) {
                    continue;
                }
                if (!requiresRuntimeBridge(object)) {
                    continue;
                }
                blockedBlockEntities.add(object);
            }
        }

        blockedBlockEntities.sort(Comparator
            .comparing(CompatibilityObject::modId)
            .thenComparing(CompatibilityObject::javaIdentifier));
        return List.copyOf(blockedBlockEntities);
    }

    private static boolean requiresRuntimeBridge(@NotNull CompatibilityObject object) {
        return object.runtimeRequirements().stream().anyMatch(requirement -> requirement.startsWith("block_entity_"));
    }

    private static void appendRequirements(@NotNull StringBuilder builder, @NotNull List<String> runtimeRequirements) {
        List<String> blockEntityRequirements = runtimeRequirements.stream()
            .filter(requirement -> requirement.startsWith("block_entity_"))
            .sorted()
            .toList();
        for (int i = 0; i < blockEntityRequirements.size(); i++) {
            if (i > 0) {
                builder.append(i == blockEntityRequirements.size() - 1 ? " and " : ", ");
            }
            builder.append(blockEntityRequirements.get(i));
        }
    }

    private static void appendCandidates(
        @NotNull StringBuilder builder,
        @NotNull List<CompatibilityObject> blockedBlockEntities,
        @Nullable CompatibilityObject matchedObject
    ) {
        List<CompatibilityObject> candidates = blockedBlockEntities.stream()
            .filter(object -> matchedObject == null || !object.javaIdentifier().equals(matchedObject.javaIdentifier()))
            .toList();
        if (candidates.isEmpty()) {
            builder.append("none.");
            return;
        }

        int displayCount = Math.min(candidates.size(), 3);
        for (int i = 0; i < displayCount; i++) {
            CompatibilityObject object = candidates.get(i);
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(object.javaIdentifier());
        }

        int remaining = candidates.size() - displayCount;
        if (remaining > 0) {
            builder.append(" (+").append(remaining).append(" more)");
        }
        builder.append('.');
    }
}