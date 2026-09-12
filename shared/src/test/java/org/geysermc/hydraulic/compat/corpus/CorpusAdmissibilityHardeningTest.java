package org.geysermc.hydraulic.compat.corpus;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CorpusAdmissibilityHardeningTest {
    @Test
    void acceptsOnlyGitHubSourcesWithClearRedistributionRights() {
        AddonCorpusEntry.AddonSource source = new AddonCorpusEntry.AddonSource(
            AddonCorpusEntry.SourceType.GITHUB,
            "https://github.com/example/repo",
            "https://github.com/example/repo",
            "https://example.com",
            null
        );
        AddonCorpusEntry.AddonLicense license = new AddonCorpusEntry.AddonLicense(
            "MIT",
            "https://opensource.org/licenses/MIT",
            "MIT license",
            true,
            true,
            true,
            true
        );

        AddonCorpusEntry.AddonAdmissibility admissibility = CorpusAdmissibilityChecker.checkSourceAdmissibility(source, license);

        assertTrue(admissibility.isAdmissible());
    }

    @Test
    void rejectsMarketplaceOnlySourcesEvenWhenDownloadIsFree() {
        AddonCorpusEntry.AddonSource source = new AddonCorpusEntry.AddonSource(
            AddonCorpusEntry.SourceType.CURSEFORGE,
            "https://www.curseforge.com/minecraft/texture-packs/example",
            null,
            "https://www.curseforge.com/minecraft/texture-packs/example",
            null
        );
        AddonCorpusEntry.AddonLicense license = new AddonCorpusEntry.AddonLicense(
            "Unknown",
            null,
            "No published license",
            false,
            false,
            false,
            false
        );

        AddonCorpusEntry.AddonAdmissibility admissibility = CorpusAdmissibilityChecker.checkSourceAdmissibility(source, license);

        assertFalse(admissibility.isAdmissible());
    }

    @Test
    void rejectsDirectDownloadSourcesWithoutGitHubLicenseEvidence() {
        AddonCorpusEntry.AddonSource source = new AddonCorpusEntry.AddonSource(
            AddonCorpusEntry.SourceType.DIRECT_DOWNLOAD,
            "https://example.com/downloads/example-addon.zip",
            null,
            "https://example.com",
            null
        );
        AddonCorpusEntry.AddonLicense license = new AddonCorpusEntry.AddonLicense(
            "Unknown",
            null,
            "No authoritative license",
            false,
            false,
            false,
            false
        );

        AddonCorpusEntry.AddonAdmissibility admissibility = CorpusAdmissibilityChecker.checkSourceAdmissibility(source, license);

        assertFalse(admissibility.isAdmissible());
    }

    @Test
    void validationRejectsNonGitHubCorpusEntriesAndUnclearLicensing() {
        AddonCorpusEntry entry = new AddonCorpusEntry(
            new AddonCorpusEntry.AddonIdentity("curated_example", "example:example", "Example", "Example", "Example addon"),
            new AddonCorpusEntry.AddonSource(
                AddonCorpusEntry.SourceType.CURSEFORGE,
                "https://www.curseforge.com/minecraft/texture-packs/example",
                null,
                "https://www.curseforge.com/minecraft/texture-packs/example",
                null
            ),
            new AddonCorpusEntry.AddonLicense("Unknown", null, "No license", false, false, false, false),
            new AddonCorpusEntry.AddonAdmissibility(false, AddonCorpusEntry.AdmissibilityReason.UNCLEAR_LICENSE, "No clear license", List.of()),
            new AddonCorpusEntry.AddonVersions("1.0.0", java.util.Set.of("1.21.0"), "1.21.0", "1.21.0", List.of("1.0.0")),
            new AddonCorpusEntry.AddonBehaviorPack(false, null, java.util.Set.of(), java.util.Set.of(), java.util.Set.of(), java.util.Set.of(), java.util.Set.of(), java.util.Set.of(), java.util.Map.of()),
            new AddonCorpusEntry.AddonResourcePack(false, null, java.util.Set.of(), java.util.Set.of(), java.util.Set.of(), java.util.Set.of(), java.util.Set.of(), java.util.Set.of(), java.util.Map.of()),
            new AddonCorpusEntry.AddonCapabilities(java.util.Set.of(), java.util.Set.of(), java.util.Set.of(), java.util.Set.of(), java.util.Set.of(), java.util.Set.of(), java.util.Set.of(), java.util.Map.of()),
            new AddonCorpusEntry.AddonEvidence(List.of("manifest.json"), List.of(), List.of(), List.of(), List.of(), java.util.Map.of()),
            new AddonCorpusEntry.AddonConfidence(0.7, "heuristic", List.of("manifest"), List.of(), List.of()),
            new AddonCorpusEntry.AddonImplementationFacts("static_pack", List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), "moderate", "visual_fallback"),
            new AddonCorpusEntry.AddonProvenance("test", 0L, null, null, "1.0", List.of())
        );

        assertFalse(AddonCorpusValidator.validate(entry).isEmpty());
    }
}
