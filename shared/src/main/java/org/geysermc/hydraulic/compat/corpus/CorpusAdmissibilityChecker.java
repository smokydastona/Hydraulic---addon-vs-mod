package org.geysermc.hydraulic.compat.corpus;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

/**
 * Validates the admissibility of Bedrock addons for use in Hydraulic.
 *
 * This enforces the corpus contract and source admissibility rules:
 * - GitHub is the first-class source for inspectable Bedrock addons
 * - CurseForge is discovery metadata only unless linked source exists
 * - Free download pages are not proof of reuse rights
 * - Prefer evidence that can be inspected, versioned, diffed, and traced
 */
public final class CorpusAdmissibilityChecker {
    private CorpusAdmissibilityChecker() {
    }

    /**
     * Checks if a source is admissible for corpus inclusion.
     */
    @NotNull
    public static AddonCorpusEntry.AddonAdmissibility checkSourceAdmissibility(
        @NotNull AddonCorpusEntry.AddonSource source,
        @NotNull AddonCorpusEntry.AddonLicense license
    ) {
        // GitHub sources are always admissible if license permits
        if (source.sourceType() == AddonCorpusEntry.SourceType.GITHUB) {
            if (license.allowsRedistribution() && license.allowsModification()) {
                return new AddonCorpusEntry.AddonAdmissibility(
                    true,
                    AddonCorpusEntry.AdmissibilityReason.FULLY_PERMISSIVE,
                    null,
                    List.of("Requires attribution: " + license.requiresAttribution())
                );
            }
            return new AddonCorpusEntry.AddonAdmissibility(
                false,
                AddonCorpusEntry.AdmissibilityReason.NO_REDISTRIBUTION,
                "GitHub source exists but license does not permit redistribution",
                List.of()
            );
        }

        // CurseForge is admissible only if linked to a repository
        if (source.sourceType() == AddonCorpusEntry.SourceType.CURSEFORGE) {
            if (source.repositoryUrl() != null && !source.repositoryUrl().isBlank()) {
                return checkRepositoryAdmissibility(source.repositoryUrl(), license);
            }
            return new AddonCorpusEntry.AddonAdmissibility(
                false,
                AddonCorpusEntry.AdmissibilityReason.UNCLEAR_LICENSE,
                "CurseForge source has no linked repository for license verification",
                List.of("Add repository link to enable corpus inclusion")
            );
        }

        // Direct downloads are not admissible without explicit permission
        if (source.sourceType() == AddonCorpusEntry.SourceType.DIRECT_DOWNLOAD) {
            return new AddonCorpusEntry.AddonAdmissibility(
                false,
                AddonCorpusEntry.AdmissibilityReason.UNCLEAR_LICENSE,
                "Direct download source has no inspectable license or repository",
                List.of("Link to a GitHub repository with clear license terms")
            );
        }

        // Local files are admissible if license permits
        if (source.sourceType() == AddonCorpusEntry.SourceType.LOCAL_FILE) {
            if (license.allowsRedistribution() && license.allowsModification()) {
                return new AddonCorpusEntry.AddonAdmissibility(
                    true,
                    AddonCorpusEntry.AdmissibilityReason.FULLY_PERMISSIVE,
                    null,
                    List.of("Requires attribution: " + license.requiresAttribution())
                );
            }
            return new AddonCorpusEntry.AddonAdmissibility(
                false,
                AddonCorpusEntry.AdmissibilityReason.NO_REDISTRIBUTION,
                "Local file source has no redistribution permission",
                List.of()
            );
        }

        // Unknown sources are not admissible
        return new AddonCorpusEntry.AddonAdmissibility(
            false,
            AddonCorpusEntry.AdmissibilityReason.UNCLEAR_LICENSE,
            "Unknown source type: " + source.sourceType(),
            List.of("Provide a GitHub repository link with clear license terms")
        );
    }

    /**
     * Checks if a repository URL is admissible.
     */
    @NotNull
    private static AddonCorpusEntry.AddonAdmissibility checkRepositoryAdmissibility(
        @NotNull String repositoryUrl,
        @NotNull AddonCorpusEntry.AddonLicense license
    ) {
        if (!repositoryUrl.contains("github.com")) {
            return new AddonCorpusEntry.AddonAdmissibility(
                false,
                AddonCorpusEntry.AdmissibilityReason.UNCLEAR_LICENSE,
                "Repository is not on GitHub",
                List.of("Provide a GitHub repository link for corpus inclusion")
            );
        }

        if (license.allowsRedistribution() && license.allowsModification()) {
            return new AddonCorpusEntry.AddonAdmissibility(
                true,
                AddonCorpusEntry.AdmissibilityReason.FULLY_PERMISSIVE,
                null,
                List.of("Requires attribution: " + license.requiresAttribution())
            );
        }

        if (license.allowsRedistribution() && !license.allowsModification()) {
            return new AddonCorpusEntry.AddonAdmissibility(
                true,
                AddonCorpusEntry.AdmissibilityReason.NO_MODIFICATION,
                null,
                List.of("Requires attribution: " + license.requiresAttribution(), "No modifications allowed")
            );
        }

        if (!license.allowsRedistribution() && license.allowsModification()) {
            return new AddonCorpusEntry.AddonAdmissibility(
                false,
                AddonCorpusEntry.AdmissibilityReason.NO_REDISTRIBUTION,
                "License permits modification but not redistribution",
                List.of("Use for reference only, not for corpus inclusion")
            );
        }

        return new AddonCorpusEntry.AddonAdmissibility(
            false,
            AddonCorpusEntry.AdmissibilityReason.DENIED,
            "License does not permit redistribution or modification",
            List.of()
        );
    }

    /**
     * Validates that evidence is inspectable and versioned.
     */
    @NotNull
    public static AddonCorpusEntry.AddonAdmissibility checkEvidenceAdmissibility(
        @NotNull AddonCorpusEntry.AddonEvidence evidence,
        @NotNull AddonCorpusEntry.AddonSource source
    ) {
        // GitHub sources always have inspectable evidence
        if (source.sourceType() == AddonCorpusEntry.SourceType.GITHUB) {
            return new AddonCorpusEntry.AddonAdmissibility(
                true,
                AddonCorpusEntry.AdmissibilityReason.FULLY_PERMISSIVE,
                null,
                List.of()
            );
        }

        // Check if there's sufficient evidence for assessment
        boolean hasManifestEvidence = !evidence.manifestEvidence().isEmpty();
        boolean hasScriptEvidence = !evidence.scriptEvidence().isEmpty();
        boolean hasComponentEvidence = !evidence.componentEvidence().isEmpty();

        if (hasManifestEvidence || hasScriptEvidence || hasComponentEvidence) {
            return new AddonCorpusEntry.AddonAdmissibility(
                true,
                AddonCorpusEntry.AdmissibilityReason.ATTRIBUTION_REQUIRED,
                null,
                List.of("Evidence is inspectable but source repository is recommended")
            );
        }

        return new AddonCorpusEntry.AddonAdmissibility(
            false,
            AddonCorpusEntry.AdmissibilityReason.UNCLEAR_LICENSE,
            "Insufficient evidence for capability assessment",
            List.of("Provide GitHub repository or detailed manifest/script evidence")
        );
    }
}