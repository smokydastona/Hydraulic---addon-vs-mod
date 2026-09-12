package org.geysermc.hydraulic.compat;

import org.geysermc.hydraulic.compat.adapter.AdapterBinding;
import org.geysermc.hydraulic.compat.adapter.AdapterFeature;
import org.geysermc.hydraulic.compat.capability.CapabilityProfile;
import org.geysermc.hydraulic.compat.corpus.CorpusEvidenceTier;
import org.geysermc.hydraulic.compat.model.CompatibilityObject;
import org.geysermc.hydraulic.compat.model.Confidence;
import org.geysermc.hydraulic.compat.model.ModFingerprint;
import org.geysermc.hydraulic.compat.model.Provenance;
import org.geysermc.hydraulic.compat.model.SupportLevel;
import org.geysermc.hydraulic.metadata.MetadataIndex;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdapterOpportunityReportTest {
    @Test
    void surfacesLicensedCorpusEvidenceInformationallyWithoutChangingImpactBasedPriority() {
        CompatibilityReport withEvidence = report(Map.of(
            "testmod",
            List.of(new CompatibilityReport.CorpusMatch("machine", "corpus-x", 0.8, 1, 1, CorpusEvidenceTier.LICENSED_IMPLEMENTATION))
        ));
        CompatibilityReport withoutEvidence = report(Map.of());

        AdapterOpportunityReport withReport = AdapterOpportunityReport.from(withEvidence);
        AdapterOpportunityReport withoutReport = AdapterOpportunityReport.from(withoutEvidence);

        AdapterOpportunityReport.Opportunity withOpportunity = find(withReport, "bridge:machine_behavior_bridge");
        AdapterOpportunityReport.Opportunity withoutOpportunity = find(withoutReport, "bridge:machine_behavior_bridge");

        assertTrue(withOpportunity.hasLicensedCorpusEvidence());
        assertEquals(List.of("corpus-x"), withOpportunity.corpusEvidenceIds());
        assertFalse(withoutOpportunity.hasLicensedCorpusEvidence());
        assertEquals(List.of(), withoutOpportunity.corpusEvidenceIds());
        assertEquals(withoutOpportunity.priority(), withOpportunity.priority());
        assertEquals(withoutOpportunity.affectedObjectCount(), withOpportunity.affectedObjectCount());
    }

    @Test
    void doesNotAttachDocumentationTierEvidenceAsLicensed() {
        CompatibilityReport withDocumentationOnly = report(Map.of(
            "testmod",
            List.of(new CompatibilityReport.CorpusMatch("machine", "corpus-doc", 0.5, 1, 0, CorpusEvidenceTier.REVIEWED_DOCUMENTATION))
        ));

        AdapterOpportunityReport reportOut = AdapterOpportunityReport.from(withDocumentationOnly);
        AdapterOpportunityReport.Opportunity opportunity = find(reportOut, "bridge:machine_behavior_bridge");

        assertFalse(opportunity.hasLicensedCorpusEvidence());
        assertEquals(List.of("corpus-doc"), opportunity.corpusEvidenceIds());
    }

    @NotNull
    private static AdapterOpportunityReport.Opportunity find(@NotNull AdapterOpportunityReport report, @NotNull String key) {
        Optional<AdapterOpportunityReport.Opportunity> found = report.opportunities().stream()
            .filter(opportunity -> opportunity.key().equals(key))
            .findFirst();
        assertTrue(found.isPresent(), "missing opportunity " + key);
        return found.get();
    }

    @NotNull
    private static CompatibilityReport report(@NotNull Map<String, List<CompatibilityReport.CorpusMatch>> corpusEvidence) {
        CompatibilityObject machineObject = new CompatibilityObject(
            "example:machine",
            "block",
            "testmod",
            Map.of(),
            new CapabilityProfile("example:machine", List.of(), List.of()),
            List.of(new AdapterBinding("adapter", AdapterFeature.CUSTOM_ITEM_REGISTRATION, "reason")),
            List.of("machine_behavior_bridge"),
            Map.of(),
            SupportLevel.ADAPTED,
            CompatibilityStatus.COMPLETE,
            90,
            new Confidence(0.9, "high"),
            List.of(new Provenance("analyzer", "generated", "synthetic", false)),
            List.of()
        );

        CompatibilityProfile profile = new CompatibilityProfile(
            "testmod",
            new ModFingerprint("testmod", "testmod", "1.0.0", "fabric", "26.2", 0, 0, 0, 0, 0, 0, 0, false, false, false, false, false, false, false, false),
            SupportLevel.ADAPTED,
            CompatibilityStatus.COMPLETE,
            90,
            Map.of(),
            Map.of(),
            List.of(machineObject),
            List.of(),
            List.of()
        );

        return new CompatibilityReport(
            "2026-09-11T00:00:00Z",
            MetadataIndex.empty().summary(),
            List.of(),
            Map.of("testmod", profile),
            Map.of(),
            corpusEvidence
        );
    }
}
