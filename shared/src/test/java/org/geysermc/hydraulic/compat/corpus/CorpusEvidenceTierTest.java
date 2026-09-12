package org.geysermc.hydraulic.compat.corpus;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CorpusEvidenceTierTest {
    @Test
    void classifiesAdmissibleEntryWithCapabilityEvidenceAsLicensedImplementation() {
        assertEquals(CorpusEvidenceTier.LICENSED_IMPLEMENTATION, CorpusEvidenceTier.classify(entry(
            true, Set.of("machine_api"), List.of(), List.of()
        )));
    }

    @Test
    void classifiesAdmissibleEntryWithoutCapabilityEvidenceAsReviewedDocumentation() {
        assertEquals(CorpusEvidenceTier.REVIEWED_DOCUMENTATION, CorpusEvidenceTier.classify(entry(
            true, Set.of(), List.of(), List.of()
        )));
    }

    @Test
    void classifiesInadmissibleEntryWithEvidenceAsDerivedTeachingMaterial() {
        assertEquals(CorpusEvidenceTier.DERIVED_TEACHING_MATERIAL, CorpusEvidenceTier.classify(entry(
            false, Set.of(), List.of("BP/scripts/main.js"), List.of()
        )));
    }

    @Test
    void classifiesInadmissibleEntryWithoutEvidenceAsRejected() {
        assertEquals(CorpusEvidenceTier.REJECTED, CorpusEvidenceTier.classify(entry(
            false, Set.of(), List.of(), List.of()
        )));
    }

    @Test
    void confidenceWeightsDescendFromLicensedImplementationToRejected() {
        assertEquals(1.0D, CorpusEvidenceTier.LICENSED_IMPLEMENTATION.confidenceWeight());
        assertEquals(0.6D, CorpusEvidenceTier.REVIEWED_DOCUMENTATION.confidenceWeight());
        assertEquals(0.3D, CorpusEvidenceTier.DERIVED_TEACHING_MATERIAL.confidenceWeight());
        assertEquals(0.0D, CorpusEvidenceTier.REJECTED.confidenceWeight());
    }

    private static AddonCorpusEntry entry(boolean admissible, Set<String> machineTypes, List<String> scriptEvidence, List<String> manifestEvidence) {
        return new AddonCorpusEntry(
            new AddonCorpusEntry.AddonIdentity("id", "example:id", "author", "id", "test"),
            new AddonCorpusEntry.AddonSource(AddonCorpusEntry.SourceType.GITHUB, "https://github.com/example/id", "https://github.com/example/id", null, null),
            new AddonCorpusEntry.AddonLicense("MIT", null, null, admissible, admissible, admissible, false),
            new AddonCorpusEntry.AddonAdmissibility(
                admissible,
                admissible ? AddonCorpusEntry.AdmissibilityReason.FULLY_PERMISSIVE : AddonCorpusEntry.AdmissibilityReason.UNCLEAR_LICENSE,
                admissible ? null : "no license",
                List.of()
            ),
            new AddonCorpusEntry.AddonVersions("1.0.0", Set.of("1.21.0"), "1.21.0", "1.21.0", List.of("1.0.0")),
            new AddonCorpusEntry.AddonBehaviorPack(true, "1.21.0", Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), Map.of()),
            new AddonCorpusEntry.AddonResourcePack(true, "1.21.0", Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), Map.of()),
            new AddonCorpusEntry.AddonCapabilities(Set.of(), machineTypes, Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), Map.of()),
            new AddonCorpusEntry.AddonEvidence(manifestEvidence, scriptEvidence, List.of(), List.of(), List.of(), Map.of()),
            new AddonCorpusEntry.AddonConfidence(0.5D, "test", List.of(), List.of(), List.of()),
            new AddonCorpusEntry.AddonImplementationFacts("machine", List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), "medium", "generic-machine"),
            new AddonCorpusEntry.AddonProvenance("test", 1L, null, null, "v2.0.0", List.of())
        );
    }
}
