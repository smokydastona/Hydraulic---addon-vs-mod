package org.geysermc.hydraulic.compat.corpus;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents a single Bedrock addon entry in the corpus.
 * 
 * The corpus normalizes each addon into a typed record that can feed
 * compatibility analysis, reporting, ranking, and adapter planning.
 */
public record AddonCorpusEntry(
    @NotNull AddonIdentity identity,
    @NotNull AddonSource source,
    @NotNull AddonLicense license,
    @NotNull AddonAdmissibility admissibility,
    @NotNull AddonVersions versions,
    @NotNull AddonBehaviorPack behaviorPack,
    @NotNull AddonResourcePack resourcePack,
    @NotNull AddonCapabilities capabilities,
    @NotNull AddonEvidence evidence,
    @NotNull AddonConfidence confidence,
    @NotNull AddonProvenance provenance
) {
    /**
     * Unique identity information for the addon.
     */
    public record AddonIdentity(
        @NotNull String corpusId,
        @NotNull String bedrockIdentifier,
        @Nullable String author,
        @Nullable String displayName,
        @Nullable String description
    ) {
    }

    /**
     * Source information for the addon.
     */
    public record AddonSource(
        @NotNull SourceType sourceType,
        @NotNull String sourceUrl,
        @Nullable String repositoryUrl,
        @Nullable String homepageUrl,
        @Nullable String documentationUrl
    ) {
    }

    /**
     * The type of source for the addon.
     */
    public enum SourceType {
        GITHUB,
        CURSEFORGE,
        MODRINTH,
        DIRECT_DOWNLOAD,
        LOCAL_FILE,
        UNKNOWN
    }

    /**
     * License and admissibility information.
     */
    public record AddonLicense(
        @NotNull String licenseType,
        @Nullable String licenseUrl,
        @Nullable String licenseText,
        @NotNull boolean allowsRedistribution,
        @NotNull boolean allowsModification,
        @NotNull boolean allowsCommercialUse,
        @NotNull boolean requiresAttribution
    ) {
    }

    /**
     * Admissibility assessment for use in Hydraulic.
     */
    public record AddonAdmissibility(
        @NotNull boolean isAdmissible,
        @NotNull AdmissibilityReason reason,
        @Nullable String denialReason,
        @NotNull List<String> constraints
    ) {
    }

    /**
     * Why an addon is or is not admissible.
     */
    public enum AdmissibilityReason {
        FULLY_PERMISSIVE,
        ATTRIBUTION_REQUIRED,
        NON_COMMERCIAL_ONLY,
        NO_MODIFICATION,
        NO_REDISTRIBUTION,
        UNCLEAR_LICENSE,
        PROPRIETARY,
        DENIED
    }

    /**
     * Version information for the addon.
     */
    public record AddonVersions(
        @NotNull String latestVersion,
        @NotNull Set<String> supportedBedrockVersions,
        @Nullable String minBedrockVersion,
        @Nullable String maxBedrockVersion,
        @NotNull List<String> allVersions
    ) {
    }

    /**
     * Behavior pack information.
     */
    public record AddonBehaviorPack(
        @NotNull boolean hasBehaviorPack,
        @Nullable String behaviorPackFormat,
        @NotNull Set<String> entities,
        @NotNull Set<String> blocks,
        @NotNull Set<String> items,
        @NotNull Set<String> recipes,
        @NotNull Set<String> functions,
        @NotNull Set<String> scripts,
        @NotNull Map<String, String> customComponents
    ) {
    }

    /**
     * Resource pack information.
     */
    public record AddonResourcePack(
        @NotNull boolean hasResourcePack,
        @Nullable String resourcePackFormat,
        @NotNull Set<String> textures,
        @NotNull Set<String> models,
        @NotNull Set<String> animations,
        @NotNull Set<String> particles,
        @NotNull Set<String> sounds,
        @NotNull Set<String> uiElements,
        @NotNull Map<String, String> renderControllers
    ) {
    }

    /**
     * Capability information for the addon.
     */
    public record AddonCapabilities(
        @NotNull Set<String> storageTypes,
        @NotNull Set<String> machineTypes,
        @NotNull Set<String> transferTypes,
        @NotNull Set<String> fluidTypes,
        @NotNull Set<String> energyTypes,
        @NotNull Set<String> automationTypes,
        @NotNull Set<String> networkingTypes,
        @NotNull Map<String, String> customCapabilities
    ) {
    }

    /**
     * Evidence supporting the capability assessment.
     */
    public record AddonEvidence(
        @NotNull List<String> manifestEvidence,
        @NotNull List<String> scriptEvidence,
        @NotNull List<String> functionEvidence,
        @NotNull List<String> componentEvidence,
        @NotNull List<String> uiEvidence,
        @NotNull Map<String, String> heuristicScores
    ) {
    }

    /**
     * Confidence in the capability assessment.
     */
    public record AddonConfidence(
        @NotNull double overallScore,
        @NotNull String assessmentMethod,
        @NotNull List<String> strongIndicators,
        @NotNull List<String> weakIndicators,
        @NotNull List<String> gaps
    ) {
    }

    /**
     * Provenance information for the corpus entry.
     */
    public record AddonProvenance(
        @NotNull String addedBy,
        @NotNull long addedEpochMillis,
        @Nullable String lastUpdatedBy,
        @Nullable Long lastUpdatedEpochMillis,
        @NotNull String corpusVersion,
        @NotNull List<String> reviewNotes
    ) {
    }
}