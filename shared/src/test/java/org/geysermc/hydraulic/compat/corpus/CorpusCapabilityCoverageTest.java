package org.geysermc.hydraulic.compat.corpus;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CorpusCapabilityCoverageTest {
    @Test
    void countsAdmissibleImplementationCoverageSeparatelyFromInadmissibleDocumentationEvidence() {
        AddonCorpusEntry admissibleItemTransfer = entry("admissible-item", true, Set.of("item_transfer"));
        AddonCorpusEntry inadmissibleItemTransfer = entry("inadmissible-item", false, Set.of("item_transfer"));

        CorpusCapabilityCoverage.Result result = CorpusCapabilityCoverage.compute(
            List.of(admissibleItemTransfer),
            List.of(admissibleItemTransfer, inadmissibleItemTransfer)
        );

        assertEquals(1, result.implementationCoverage().get("ITEM_TRANSFER"));
        assertEquals(1, result.documentationCoverage().get("ITEM_TRANSFER"));
    }

    @Test
    void reportsGapCapabilitiesWithZeroAdmissibleImplementationCoverage() {
        AddonCorpusEntry admissibleItemTransfer = entry("admissible-item", true, Set.of("item_transfer"));

        CorpusCapabilityCoverage.Result result = CorpusCapabilityCoverage.compute(
            List.of(admissibleItemTransfer),
            List.of(admissibleItemTransfer)
        );

        assertTrue(result.gapCapabilities().contains("RPC"));
        assertTrue(result.gapCapabilities().contains("REPLICATED_STATE"));
        assertTrue(result.gapCapabilities().contains("MULTIBLOCK"));
        assertTrue(!result.gapCapabilities().contains("ITEM_TRANSFER"));
    }

    private static AddonCorpusEntry entry(String id, boolean admissible, Set<String> transferTypes) {
        return new AddonCorpusEntry(
            new AddonCorpusEntry.AddonIdentity(id, "example:" + id, "author", id, "test"),
            new AddonCorpusEntry.AddonSource(AddonCorpusEntry.SourceType.GITHUB, "https://github.com/example/" + id, "https://github.com/example/" + id, null, null),
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
            new AddonCorpusEntry.AddonCapabilities(Set.of(), Set.of(), transferTypes, Set.of(), Set.of(), Set.of(), Set.of(), Map.of()),
            new AddonCorpusEntry.AddonEvidence(List.of("manifest"), List.of(), List.of(), List.of(), List.of(), Map.of()),
            new AddonCorpusEntry.AddonConfidence(0.5D, "test", List.of(), List.of(), List.of()),
            new AddonCorpusEntry.AddonImplementationFacts("machine", List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), "medium", "generic-machine"),
            new AddonCorpusEntry.AddonProvenance("test", 1L, null, null, "v2.0.0", List.of())
        );
    }
}
