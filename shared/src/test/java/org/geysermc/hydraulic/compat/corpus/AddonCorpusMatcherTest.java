package org.geysermc.hydraulic.compat.corpus;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AddonCorpusMatcherTest {
    @Test
    void ranksAdmissibleReusableCapabilityEvidenceFirst() {
        AddonCorpusEntry generic = entry("generic", "high", "generic-machine", 0.8D, true);
        AddonCorpusEntry specific = entry("specific", "low", "mod-specific", 1.0D, true);
        AddonCorpusEntry denied = entry("denied", "high", "generic-machine", 1.0D, false);

        List<AddonCorpusMatcher.Match> matches = AddonCorpusMatcher.rank(
            List.of(specific, denied, generic),
            "item",
            Set.of("generic-machine")
        );

        assertEquals(List.of("generic", "specific"), matches.stream()
            .map(match -> match.entry().identity().corpusId())
            .toList());
        assertTrue(matches.getFirst().score() > matches.getLast().score());
    }

    @Test
    void weighsLicensedImplementationEvidenceAboveDocumentationOnlyEvidenceForEqualPatternMatches() {
        AddonCorpusEntry implementationEvidence = entryWithCapabilities(
            "implementation-evidence", Set.of("upgrades"), "generic-machine", 0.8D
        );
        AddonCorpusEntry documentationOnly = entryWithCapabilities(
            "documentation-only", Set.of(), "generic-machine", 0.8D
        );

        assertEquals(CorpusEvidenceTier.LICENSED_IMPLEMENTATION, CorpusEvidenceTier.classify(implementationEvidence));
        assertEquals(CorpusEvidenceTier.REVIEWED_DOCUMENTATION, CorpusEvidenceTier.classify(documentationOnly));

        List<AddonCorpusMatcher.Match> matches = AddonCorpusMatcher.rank(
            List.of(documentationOnly, implementationEvidence),
            "item",
            Set.of("generic-machine")
        );

        assertEquals(2, matches.size());
        assertEquals("implementation-evidence", matches.get(0).entry().identity().corpusId());
        assertEquals(CorpusEvidenceTier.LICENSED_IMPLEMENTATION, matches.get(0).tier());
        assertEquals("documentation-only", matches.get(1).entry().identity().corpusId());
        assertEquals(CorpusEvidenceTier.REVIEWED_DOCUMENTATION, matches.get(1).tier());
        assertTrue(matches.get(0).score() > matches.get(1).score());
    }

    private static AddonCorpusEntry entryWithCapabilities(String id, Set<String> automationTypes, String adapterCandidate, double confidence) {
        return new AddonCorpusEntry(
            new AddonCorpusEntry.AddonIdentity(id, "example:" + id, "author", id, "test"),
            new AddonCorpusEntry.AddonSource(AddonCorpusEntry.SourceType.GITHUB, "https://github.com/example/" + id, "https://github.com/example/" + id, null, null),
            new AddonCorpusEntry.AddonLicense("MIT", null, null, true, true, true, false),
            new AddonCorpusEntry.AddonAdmissibility(true, AddonCorpusEntry.AdmissibilityReason.FULLY_PERMISSIVE, null, List.of()),
            new AddonCorpusEntry.AddonVersions("1.0.0", Set.of("1.21.0"), "1.21.0", "1.21.0", List.of("1.0.0")),
            new AddonCorpusEntry.AddonBehaviorPack(true, "1.21.0", Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), Map.of()),
            new AddonCorpusEntry.AddonResourcePack(true, "1.21.0", Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), Map.of()),
            new AddonCorpusEntry.AddonCapabilities(Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), automationTypes, Set.of(), Map.of()),
            new AddonCorpusEntry.AddonEvidence(List.of("manifest"), List.of(), List.of(), List.of(), List.of(), Map.of()),
            new AddonCorpusEntry.AddonConfidence(confidence, "test", List.of(), List.of(), List.of()),
            new AddonCorpusEntry.AddonImplementationFacts("machine", List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), "high", adapterCandidate),
            new AddonCorpusEntry.AddonProvenance("test", 1L, null, null, "v2.0.0", List.of())
        );
    }

    private static AddonCorpusEntry entry(String id, String reusability, String adapter, double confidence, boolean admissible) {
        return new AddonCorpusEntry(
            new AddonCorpusEntry.AddonIdentity(id, "example:" + id, "author", id, "test"),
            new AddonCorpusEntry.AddonSource(AddonCorpusEntry.SourceType.GITHUB, "https://github.com/example/" + id, "https://github.com/example/" + id, null, null),
            new AddonCorpusEntry.AddonLicense("MIT", null, null, true, true, true, false),
            new AddonCorpusEntry.AddonAdmissibility(admissible, admissible ? AddonCorpusEntry.AdmissibilityReason.FULLY_PERMISSIVE : AddonCorpusEntry.AdmissibilityReason.DENIED, null, List.of()),
            new AddonCorpusEntry.AddonVersions("1.0.0", Set.of("1.21.0"), "1.21.0", "1.21.0", List.of("1.0.0")),
            new AddonCorpusEntry.AddonBehaviorPack(true, "1.21.0", Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), Map.of()),
            new AddonCorpusEntry.AddonResourcePack(true, "1.21.0", Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), Map.of()),
            new AddonCorpusEntry.AddonCapabilities(Set.of(), Set.of(), Set.of("item"), Set.of(), Set.of(), Set.of(), Set.of(), Map.of()),
            new AddonCorpusEntry.AddonEvidence(List.of("manifest"), List.of(), List.of(), List.of(), List.of(), Map.of()),
            new AddonCorpusEntry.AddonConfidence(confidence, "test", List.of(), List.of(), List.of()),
            new AddonCorpusEntry.AddonImplementationFacts("machine", List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), reusability, adapter),
            new AddonCorpusEntry.AddonProvenance("test", 1L, null, null, "v2.0.0", List.of())
        );
    }
}