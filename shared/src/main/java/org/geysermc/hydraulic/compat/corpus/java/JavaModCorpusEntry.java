package org.geysermc.hydraulic.compat.corpus.java;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Represents one Java-side capability semantic reference: what a Java modding API (Forge
 * Capabilities, Fabric Transfer API, or an equivalent inspectable abstraction) actually requires
 * and exposes for a canonical capability, independent of any Bedrock implementation.
 *
 * This is a distinct schema from {@link org.geysermc.hydraulic.compat.corpus.AddonCorpusEntry}:
 * the Bedrock addon corpus answers "how can Bedrock implement this?", while this corpus answers
 * "what semantics does the Java side actually require?". Entries here are reference/documentation
 * evidence about a Java API's contract; Hydraulic never redistributes Java mod or API source code,
 * only records facts about its documented public contract.
 */
public record JavaModCorpusEntry(
    @NotNull Identity identity,
    @NotNull Source source,
    @NotNull License license,
    @NotNull Admissibility admissibility,
    @NotNull String capability,
    @NotNull List<String> javaApiReferences,
    @NotNull SemanticFacts semanticFacts,
    @NotNull String implementationPattern,
    @NotNull List<String> limitations,
    @NotNull String bedrockFeasibilityHint,
    @NotNull Confidence confidence,
    @NotNull Provenance provenance
) {
    public record Identity(
        @NotNull String corpusId,
        @NotNull String displayName,
        @NotNull String description
    ) {
    }

    public record Source(
        @NotNull SourceType sourceType,
        @NotNull String sourceUrl,
        @Nullable String repositoryUrl,
        @Nullable String documentationUrl
    ) {
    }

    public enum SourceType {
        GITHUB,
        OFFICIAL_DOCUMENTATION,
        UNKNOWN
    }

    public record License(
        @NotNull String licenseType,
        @Nullable String licenseUrl,
        boolean allowsRedistribution,
        boolean allowsModification,
        boolean allowsCommercialUse,
        boolean requiresAttribution
    ) {
    }

    public record Admissibility(
        boolean isAdmissible,
        @NotNull String reason,
        @Nullable String denialReason,
        @NotNull List<String> constraints
    ) {
    }

    /**
     * The concrete semantic contract a Java API requires for this capability: what state must
     * exist, what inputs/outputs look like, and whether simulation, sided access, and filtering
     * are part of the contract. This is what a Bedrock adapter must actually satisfy to be
     * considered a faithful implementation, not merely a visual approximation.
     */
    public record SemanticFacts(
        @NotNull List<String> requiredState,
        @NotNull List<String> inputSemantics,
        @NotNull List<String> outputSemantics,
        boolean simulationSupport,
        boolean sidedAccess,
        boolean filteringSupport
    ) {
    }

    public record Confidence(
        double overallScore,
        @NotNull String assessmentMethod,
        @NotNull List<String> strongIndicators,
        @NotNull List<String> weakIndicators,
        @NotNull List<String> gaps
    ) {
    }

    public record Provenance(
        @NotNull String addedBy,
        long addedEpochMillis,
        @NotNull String corpusVersion,
        @NotNull List<String> reviewNotes
    ) {
    }
}
