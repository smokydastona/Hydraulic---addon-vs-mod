package org.geysermc.hydraulic.compat;

import org.geysermc.hydraulic.compat.model.CompatibilityContract;
import org.geysermc.hydraulic.compat.model.CompatibilityObject;
import org.geysermc.hydraulic.compat.runtime.RuntimeBridgeKind;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Advisory ranking of reusable compatibility work. It never changes runtime decisions.
 */
public record AdapterOpportunityReport(
    @NotNull String generatedAt,
    @NotNull List<Opportunity> opportunities
) {
    public AdapterOpportunityReport {
        opportunities = List.copyOf(opportunities);
    }

    @NotNull
    public static AdapterOpportunityReport from(@NotNull CompatibilityReport report) {
        Map<String, MutableOpportunity> grouped = new LinkedHashMap<>();
        for (Map.Entry<String, CompatibilityProfile> mod : report.mods().entrySet()) {
            for (CompatibilityObject object : mod.getValue().objects()) {
                CompatibilityContract contract = object.contract();
                for (RuntimeBridgeKind bridge : contract.requiredBridges()) {
                    MutableOpportunity opportunity = grouped.computeIfAbsent(
                        "bridge:" + bridge.requirementId(),
                        ignored -> new MutableOpportunity("bridge", bridge.requirementId(), bridge.contentType())
                    );
                    opportunity.add(mod.getKey(), object.javaIdentifier(), contract.executable());
                }
                for (Map.Entry<CompatibilityContract.Domain, CompatibilityContract.DomainContract> domainEntry : contract.domains().entrySet()) {
                    CompatibilityContract.DomainContract domain = domainEntry.getValue();
                    for (String missing : domain.missingCapabilities()) {
                        MutableOpportunity opportunity = grouped.computeIfAbsent(
                            "capability:" + missing,
                            ignored -> new MutableOpportunity("capability", missing, domainEntry.getKey().name().toLowerCase())
                        );
                        opportunity.add(mod.getKey(), object.javaIdentifier(), contract.executable());
                    }
                }
            }
        }

        List<Opportunity> opportunities = grouped.values().stream()
            .map(MutableOpportunity::freeze)
            .sorted(Comparator.comparingInt(Opportunity::priority).reversed().thenComparing(Opportunity::key))
            .toList();
        return new AdapterOpportunityReport(report.generatedAt(), opportunities);
    }

    public record Opportunity(
        @NotNull String key,
        @NotNull String kind,
        @NotNull String target,
        @NotNull String domain,
        int affectedObjectCount,
        int affectedModCount,
        int blockedObjectCount,
        int priority,
        @NotNull List<String> exampleObjects,
        @NotNull List<String> affectedMods
    ) {
        public Opportunity {
            exampleObjects = List.copyOf(exampleObjects);
            affectedMods = List.copyOf(affectedMods);
        }
    }

    private static final class MutableOpportunity {
        private final String key;
        private final String kind;
        private final String target;
        private final String domain;
        private final Set<String> objects = new LinkedHashSet<>();
        private final Set<String> mods = new LinkedHashSet<>();
        private int blockedObjects;

        private MutableOpportunity(String kind, String target, String domain) {
            this.key = kind + ":" + target;
            this.kind = kind;
            this.target = target;
            this.domain = domain;
        }

        private void add(String mod, String object, boolean executable) {
            this.objects.add(object);
            this.mods.add(mod);
            if (!executable) {
                this.blockedObjects++;
            }
        }

        private Opportunity freeze() {
            List<String> examples = new ArrayList<>(this.objects);
            if (examples.size() > 5) {
                examples = examples.subList(0, 5);
            }
            int priority = Math.min(100, this.objects.size() + (this.blockedObjects * 2));
            return new Opportunity(
                this.key,
                this.kind,
                this.target,
                this.domain,
                this.objects.size(),
                this.mods.size(),
                this.blockedObjects,
                priority,
                examples,
                List.copyOf(this.mods)
            );
        }
    }
}
