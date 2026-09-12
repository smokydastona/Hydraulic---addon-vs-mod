package org.geysermc.hydraulic.compat.corpus.java;

import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Index of the Java-mod capability-semantics corpus, mirroring
 * {@link org.geysermc.hydraulic.compat.corpus.AddonCorpusIndex}'s tamper-evident,
 * fast-lookup design but for the distinct Java-side schema.
 */
public record JavaModCorpusIndex(
    @NotNull String corpusVersion,
    @NotNull String algorithm,
    @NotNull Map<String, IndexedEntry> entries,
    @NotNull CorpusMetadata metadata
) {
    public JavaModCorpusIndex {
        entries = Map.copyOf(new LinkedHashMap<>(entries));
    }

    @NotNull
    public static JavaModCorpusIndex empty() {
        return new JavaModCorpusIndex(
            "v1.0.0",
            "HYDRAULIC_JAVA_CORPUS_INDEX_V1",
            Map.of(),
            new CorpusMetadata(0, 0, 0, System.currentTimeMillis())
        );
    }

    public record IndexedEntry(
        @NotNull String corpusId,
        @NotNull String capability,
        @NotNull String storageLocation,
        boolean isAdmissible,
        @NotNull String admissibilityReason,
        double confidenceScore,
        long lastModifiedEpochMillis
    ) {
    }

    public record CorpusMetadata(
        int totalEntries,
        int admissibleEntries,
        int inadmissibleEntries,
        long lastUpdatedEpochMillis
    ) {
    }
}
