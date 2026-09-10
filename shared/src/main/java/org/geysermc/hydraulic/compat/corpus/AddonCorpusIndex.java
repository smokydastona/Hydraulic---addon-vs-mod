package org.geysermc.hydraulic.compat.corpus;

import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Index of the Bedrock addon corpus.
 *
 * This index provides fast lookup for corpus entries without requiring
 * parsing individual entry files during normal operation.
 */
public record AddonCorpusIndex(
    @NotNull String corpusVersion,
    @NotNull String algorithm,
    @NotNull Map<String, IndexedEntry> entries,
    @NotNull CorpusMetadata metadata
) {
    public AddonCorpusIndex {
        entries = Map.copyOf(new LinkedHashMap<>(entries));
    }

    @NotNull
    public static AddonCorpusIndex empty() {
        return new AddonCorpusIndex(
            "v2.0.0",
            "HYDRAULIC_CORPUS_INDEX_V2",
            Map.of(),
            new CorpusMetadata(0, 0, 0, System.currentTimeMillis())
        );
    }

    /**
     * Represents a single indexed entry.
     */
    public record IndexedEntry(
        @NotNull String corpusId,
        @NotNull String bedrockIdentifier,
        @NotNull String storageLocation,
        @NotNull boolean isAdmissible,
        @NotNull String admissibilityReason,
        @NotNull double capabilityScore,
        @NotNull long lastModifiedEpochMillis
    ) {
    }

    /**
     * Metadata about the corpus as a whole.
     */
    public record CorpusMetadata(
        int totalEntries,
        int admissibleEntries,
        int inadmissibleEntries,
        long lastUpdatedEpochMillis
    ) {
    }
}