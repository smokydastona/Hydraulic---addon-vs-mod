package org.geysermc.hydraulic.compat;

import org.geysermc.hydraulic.compat.corpus.AddonCorpusMatcher;
import org.geysermc.hydraulic.compat.model.CompatibilityFinding;
import org.geysermc.hydraulic.compat.model.CompatibilityObject;
import org.geysermc.hydraulic.metadata.MetadataIndex;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CompatibilityReport {
    private final String generatedAt;
    private final MetadataIndex.Summary metadata;
    private final List<CompatibilityFinding> metadataFindings;
    private final Map<String, CompatibilityProfile> mods;
    private final Map<String, List<CorpusMatch>> corpusEvidence;
    private final Map<String, PackValidationSummary> packValidation;

    public CompatibilityReport(
        @NotNull String generatedAt,
        @NotNull MetadataIndex.Summary metadata,
        @NotNull List<CompatibilityFinding> metadataFindings,
        @NotNull Map<String, CompatibilityProfile> mods
    ) {
        this(generatedAt, metadata, metadataFindings, mods, Map.of(), Map.of());
    }

    public CompatibilityReport(
        @NotNull String generatedAt,
        @NotNull MetadataIndex.Summary metadata,
        @NotNull List<CompatibilityFinding> metadataFindings,
        @NotNull Map<String, CompatibilityProfile> mods,
        @NotNull Map<String, PackValidationSummary> packValidation
    ) {
        this(generatedAt, metadata, metadataFindings, mods, packValidation, Map.of());
    }

    public CompatibilityReport(
        @NotNull String generatedAt,
        @NotNull MetadataIndex.Summary metadata,
        @NotNull List<CompatibilityFinding> metadataFindings,
        @NotNull Map<String, CompatibilityProfile> mods,
        @NotNull Map<String, PackValidationSummary> packValidation,
        @NotNull Map<String, List<CorpusMatch>> corpusEvidence
    ) {
        this.generatedAt = generatedAt;
        this.metadata = metadata;
        this.metadataFindings = List.copyOf(metadataFindings);
        this.mods = Collections.unmodifiableMap(new LinkedHashMap<>(mods));
        Map<String, List<CorpusMatch>> copiedEvidence = new LinkedHashMap<>();
        for (Map.Entry<String, List<CorpusMatch>> entry : corpusEvidence.entrySet()) {
            copiedEvidence.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        this.corpusEvidence = Collections.unmodifiableMap(copiedEvidence);
        this.packValidation = Collections.unmodifiableMap(new LinkedHashMap<>(packValidation));
    }

    @NotNull
    public static CompatibilityReport empty() {
        return new CompatibilityReport("", MetadataIndex.Summary.empty(), List.of(), Map.of());
    }

    @NotNull
    public String generatedAt() {
        return this.generatedAt;
    }

    @NotNull
    public MetadataIndex.Summary metadata() {
        return this.metadata;
    }

    @NotNull
    public List<CompatibilityFinding> metadataFindings() {
        return this.metadataFindings;
    }

    @NotNull
    public Map<String, CompatibilityProfile> mods() {
        return this.mods;
    }

    @NotNull
    public Map<String, List<CorpusMatch>> corpusEvidence() {
        return this.corpusEvidence;
    }

    @Nullable
    public CompatibilityProfile profile(@NotNull String modId) {
        return this.mods.get(modId);
    }

    @Nullable
    public CompatibilityObject object(@NotNull String modId, @NotNull String javaIdentifier, @NotNull String contentType) {
        CompatibilityProfile profile = this.mods.get(modId);
        return profile != null ? profile.object(javaIdentifier, contentType) : null;
    }

    /**
     * Post-generation pack validation summaries per mod, merged in after pack conversion completes.
     * Empty until {@link org.geysermc.hydraulic.pack.PackManager} syncs validation results into this report.
     *
     * @return the pack validation summaries by mod id
     */
    @NotNull
    public Map<String, PackValidationSummary> packValidation() {
        return this.packValidation;
    }

    @NotNull
    public CompatibilityReport withPackValidation(@NotNull Map<String, PackValidationSummary> packValidation) {
        return new CompatibilityReport(this.generatedAt, this.metadata, this.metadataFindings, this.mods, packValidation, this.corpusEvidence);
    }

    public record CorpusMatch(
        @NotNull String capability,
        @NotNull String corpusId,
        double score,
        int capabilityMatches,
        int patternMatches,
        @NotNull org.geysermc.hydraulic.compat.corpus.CorpusEvidenceTier tier
    ) {
        public static CorpusMatch from(@NotNull String capability, @NotNull AddonCorpusMatcher.Match match) {
            return new CorpusMatch(capability, match.entry().identity().corpusId(), match.score(), match.capabilityMatches(), match.patternMatches(), match.tier());
        }
    }

    /**
     * A compact summary of a mod's post-generation pack validation result, surfaced alongside
     * compatibility findings so manual actions are visible in one report.
     */
    public record PackValidationSummary(
        boolean valid,
        int errorCount,
        int warningCount,
        int manualActionCount,
        @NotNull List<String> manualActions
    ) {
        public PackValidationSummary {
            manualActions = List.copyOf(manualActions);
        }
    }
}