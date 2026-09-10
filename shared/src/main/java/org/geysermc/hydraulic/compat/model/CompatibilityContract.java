package org.geysermc.hydraulic.compat.model;

import org.geysermc.hydraulic.compat.CompatibilityStatus;
import org.geysermc.hydraulic.compat.runtime.RuntimeBridgeKind;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Typed execution contract derived from analyzer output for one Java object.
 * The contract is the boundary between compatibility analysis and runtime planning.
 */
public record CompatibilityContract(
    @NotNull String contentType,
    @NotNull String javaIdentifier,
    @NotNull SupportLevel overallLevel,
    @NotNull CompatibilityStatus overallStatus,
    int overallScore,
    @NotNull Map<Domain, DomainContract> domains,
    @NotNull List<RuntimeBridgeKind> requiredBridges,
    boolean executable
) {
    public CompatibilityContract {
        domains = Map.copyOf(new LinkedHashMap<>(domains));
        requiredBridges = List.copyOf(requiredBridges);
    }

    @NotNull
    public static CompatibilityContract from(@NotNull CompatibilityObject object) {
        Map<Domain, DomainContract> domains = new LinkedHashMap<>();
        for (Map.Entry<String, SupportResult> entry : object.supportResults().entrySet()) {
            Domain domain = Domain.fromScope(entry.getKey());
            if (domain == null) {
                continue;
            }
            SupportResult result = entry.getValue();
            domains.put(domain, DomainContract.from(result));
        }

        List<RuntimeBridgeKind> requiredBridges = RuntimeBridgeKind.resolve(object.runtimeRequirements());
        boolean executable = object.overallLevel() != SupportLevel.UNSUPPORTED
            && object.overallLevel() != SupportLevel.VISUAL_ONLY
            && object.supportResults().values().stream().noneMatch(result -> result.level() == SupportLevel.UNSUPPORTED)
            && !"true".equals(object.inventoryFacts().get("critical_failure"));
        return new CompatibilityContract(
            object.contentType(),
            object.javaIdentifier(),
            object.overallLevel(),
            object.overallStatus(),
            object.overallScore(),
            domains,
            requiredBridges,
            executable
        );
    }

    public enum Domain {
        CONTENT,
        PRESENTATION,
        STATE,
        INTERACTION,
        BEHAVIOR,
        NETWORK;

        private static Domain fromScope(@NotNull String scope) {
            return switch (scope) {
                case "content" -> CONTENT;
                case "presentation" -> PRESENTATION;
                case "state_data" -> STATE;
                case "interaction" -> INTERACTION;
                case "behavior" -> BEHAVIOR;
                case "network" -> NETWORK;
                default -> null;
            };
        }
    }

    public record DomainContract(
        @NotNull SupportLevel level,
        @NotNull CompatibilityStatus status,
        @NotNull Action action,
        @NotNull List<String> supportedCapabilities,
        @NotNull List<String> missingCapabilities,
        @NotNull List<String> notes
    ) {
        public DomainContract {
            supportedCapabilities = List.copyOf(supportedCapabilities);
            missingCapabilities = List.copyOf(missingCapabilities);
            notes = List.copyOf(notes);
        }

        @NotNull
        private static DomainContract from(@NotNull SupportResult result) {
            return new DomainContract(
                result.level(),
                result.status(),
                Action.from(result),
                result.supportedCapabilities(),
                result.missingCapabilities(),
                result.notes()
            );
        }
    }

    public enum Action {
        NATIVE,
        ADAPT,
        APPROXIMATE,
        VISUAL_ONLY,
        OMIT;

        @NotNull
        private static Action from(@NotNull SupportResult result) {
            return switch (result.level()) {
                case NATIVE, AUTOMATIC -> NATIVE;
                case ADAPTED -> ADAPT;
                case APPROXIMATED -> APPROXIMATE;
                case VISUAL_ONLY -> VISUAL_ONLY;
                case UNSUPPORTED -> OMIT;
            };
        }
    }
}
