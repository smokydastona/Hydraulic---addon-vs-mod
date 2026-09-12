package org.geysermc.hydraulic.compat;

import org.geysermc.hydraulic.compat.corpus.CorpusEvidenceTier;
import org.geysermc.hydraulic.compat.ir.CorpusEvidenceRef;
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
 *
 * Corpus evidence tier/coverage data is surfaced here as informational fields only
 * ({@code hasLicensedCorpusEvidence}, {@code corpusEvidenceIds}); it deliberately does not change
 * {@code priority}, which stays purely impact-based (affected/blocked object counts). Feasibility
 * evidence and impact are different signals, and conflating them would let corpus confidence mask
 * how many mods are actually blocked, which the corpus rules explicitly prohibit.
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
        Map<RuntimeBridgeKind, CorpusEvidenceSummary> corpusEvidenceByBridgeKind = summarizeCorpusEvidence(report);
        Map<String, MutableOpportunity> grouped = new LinkedHashMap<>();
        for (Map.Entry<String, CompatibilityProfile> mod : report.mods().entrySet()) {
            for (CompatibilityObject object : mod.getValue().objects()) {
                CompatibilityContract contract = object.contract();
                for (RuntimeBridgeKind bridge : contract.requiredBridges()) {
                    MutableOpportunity opportunity = grouped.computeIfAbsent(
                        "bridge:" + bridge.requirementId(),
                        ignored -> new MutableOpportunity("bridge", bridge.requirementId(), bridge.contentType(), corpusEvidenceByBridgeKind.get(bridge))
                    );
                    opportunity.add(mod.getKey(), object.javaIdentifier(), contract.executable());
                }
                for (Map.Entry<CompatibilityContract.Domain, CompatibilityContract.DomainContract> domainEntry : contract.domains().entrySet()) {
                    CompatibilityContract.DomainContract domain = domainEntry.getValue();
                    for (String missing : domain.missingCapabilities()) {
                        MutableOpportunity opportunity = grouped.computeIfAbsent(
                            "capability:" + missing,
                            ignored -> new MutableOpportunity("capability", missing, domainEntry.getKey().name().toLowerCase(), null)
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

    /**
     * Aggregates admissible corpus evidence across all mods, keyed by the {@link RuntimeBridgeKind}
     * it is evidence for. Only capabilities with an explicit {@link CorpusEvidenceRef} bridge-kind
     * mapping are included; presentation/reference-only evidence is intentionally excluded.
     */
    @NotNull
    private static Map<RuntimeBridgeKind, CorpusEvidenceSummary> summarizeCorpusEvidence(@NotNull CompatibilityReport report) {
        Map<RuntimeBridgeKind, Set<String>> corpusIdsByKind = new LinkedHashMap<>();
        Map<RuntimeBridgeKind, Boolean> hasLicensedByKind = new LinkedHashMap<>();
        for (List<CompatibilityReport.CorpusMatch> matches : report.corpusEvidence().values()) {
            for (CompatibilityReport.CorpusMatch match : matches) {
                CorpusEvidenceRef ref = CorpusEvidenceRef.of(match.capability(), match.corpusId(), match.tier(), match.score());
                if (ref.relatedBridgeKind() == null) {
                    continue;
                }
                corpusIdsByKind.computeIfAbsent(ref.relatedBridgeKind(), ignored -> new LinkedHashSet<>()).add(ref.corpusId());
                if (ref.tier() == CorpusEvidenceTier.LICENSED_IMPLEMENTATION) {
                    hasLicensedByKind.put(ref.relatedBridgeKind(), true);
                }
            }
        }

        Map<RuntimeBridgeKind, CorpusEvidenceSummary> summaries = new LinkedHashMap<>();
        for (Map.Entry<RuntimeBridgeKind, Set<String>> entry : corpusIdsByKind.entrySet()) {
            summaries.put(entry.getKey(), new CorpusEvidenceSummary(
                hasLicensedByKind.getOrDefault(entry.getKey(), false),
                List.copyOf(entry.getValue())
            ));
        }
        return Map.copyOf(summaries);
    }

    private record CorpusEvidenceSummary(boolean hasLicensedEvidence, @NotNull List<String> corpusIds) {
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
        @NotNull List<String> affectedMods,
        boolean hasLicensedCorpusEvidence,
        @NotNull List<String> corpusEvidenceIds
    ) {
        public Opportunity {
            exampleObjects = List.copyOf(exampleObjects);
            affectedMods = List.copyOf(affectedMods);
            corpusEvidenceIds = List.copyOf(corpusEvidenceIds);
        }
    }

    private static final class MutableOpportunity {
        private final String key;
        private final String kind;
        private final String target;
        private final String domain;
        private final CorpusEvidenceSummary corpusEvidence;
        private final Set<String> objects = new LinkedHashSet<>();
        private final Set<String> mods = new LinkedHashSet<>();
        private int blockedObjects;

        private MutableOpportunity(String kind, String target, String domain, CorpusEvidenceSummary corpusEvidence) {
            this.key = kind + ":" + target;
            this.kind = kind;
            this.target = target;
            this.domain = domain;
            this.corpusEvidence = corpusEvidence;
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
                List.copyOf(this.mods),
                this.corpusEvidence != null && this.corpusEvidence.hasLicensedEvidence(),
                this.corpusEvidence != null ? this.corpusEvidence.corpusIds() : List.of()
            );
        }
    }
}
