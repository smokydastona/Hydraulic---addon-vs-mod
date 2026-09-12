package org.geysermc.hydraulic.compat.corpus;

import org.geysermc.hydraulic.Constants;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Writes offline corpus summary and admissibility reports without touching hot runtime paths.
 */
public final class CorpusReportWriter {
    private final Logger logger;
    private final Path reportsPath;

    public CorpusReportWriter(@NotNull Logger logger, @NotNull Path dataPath) {
        this.logger = logger;
        this.reportsPath = dataPath.resolve("reports");
    }

    public void writeReports(@NotNull AddonCorpusIndex index, @NotNull List<AddonCorpusEntry> admissibleEntries) {
        this.writeReports(index, admissibleEntries, admissibleEntries);
    }

    public void writeReports(
        @NotNull AddonCorpusIndex index,
        @NotNull List<AddonCorpusEntry> admissibleEntries,
        @NotNull List<AddonCorpusEntry> allEntries
    ) {
        try {
            Files.createDirectories(this.reportsPath);
            this.writeJson(this.reportsPath.resolve("corpus-summary.json"), buildSummary(index, admissibleEntries, allEntries));
            this.writeJson(this.reportsPath.resolve("corpus-admissibility-report.json"), buildAdmissibilityReport(index));
            this.logger.info(
                "Wrote corpus reports (entries={}, admissible={})",
                index.entries().size(),
                admissibleEntries.size()
            );
        } catch (IOException e) {
            this.logger.error("Failed to write corpus reports to {}", this.reportsPath, e);
        }
    }

    @NotNull
    private static Map<String, Object> buildSummary(
        @NotNull AddonCorpusIndex index,
        @NotNull List<AddonCorpusEntry> admissibleEntries,
        @NotNull List<AddonCorpusEntry> allEntries
    ) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("corpusVersion", index.corpusVersion());
        summary.put("algorithm", index.algorithm());
        summary.put("totalEntries", index.metadata().totalEntries());
        summary.put("admissibleEntries", index.metadata().admissibleEntries());
        summary.put("inadmissibleEntries", index.metadata().inadmissibleEntries());
        summary.put("loadedAdmissibleEntries", admissibleEntries.size());
        summary.put("lastUpdatedEpochMillis", index.metadata().lastUpdatedEpochMillis());

        CorpusCapabilityCoverage.Result coverage = CorpusCapabilityCoverage.compute(admissibleEntries, allEntries);
        Map<String, Object> capabilityCoverage = new LinkedHashMap<>();
        capabilityCoverage.put("implementationCoverage", coverage.implementationCoverage());
        capabilityCoverage.put("documentationCoverage", coverage.documentationCoverage());
        capabilityCoverage.put("gapCapabilities", coverage.gapCapabilities());
        summary.put("capabilityCoverage", capabilityCoverage);

        List<Map<String, Object>> entries = new ArrayList<>();
        for (AddonCorpusEntry entry : admissibleEntries) {
            Map<String, Object> record = new LinkedHashMap<>();
            record.put("corpusId", entry.identity().corpusId());
            record.put("bedrockIdentifier", entry.identity().bedrockIdentifier());
            record.put("sourceType", entry.source().sourceType().name());
            record.put("sourceUrl", entry.source().sourceUrl());
            record.put("repositoryUrl", entry.source().repositoryUrl());
            record.put("license", entry.license().licenseType());
            record.put("requiresAttribution", entry.license().requiresAttribution());
            record.put("evidenceTier", CorpusEvidenceTier.classify(entry).name());
            record.put("confidence", entry.confidence().overallScore());
            record.put("assessmentMethod", entry.confidence().assessmentMethod());
            record.put("strongIndicators", entry.confidence().strongIndicators());
            record.put("weakIndicators", entry.confidence().weakIndicators());
            record.put("gaps", entry.confidence().gaps());
            record.put("manifestEvidence", entry.evidence().manifestEvidence());
            record.put("scriptEvidence", entry.evidence().scriptEvidence());
            record.put("componentEvidence", entry.evidence().componentEvidence());
            record.put("machineTypes", entry.capabilities().machineTypes());
            record.put("transferTypes", entry.capabilities().transferTypes());
            record.put("implementationPattern", entry.implementationFacts().implementationPattern());
            record.put("limitations", entry.implementationFacts().limitations());
            record.put("performanceCharacteristics", entry.implementationFacts().performanceCharacteristics());
            record.put("persistenceMethods", entry.implementationFacts().persistenceMethods());
            record.put("runtimeHooks", entry.implementationFacts().runtimeHooks());
            record.put("transferSemantics", entry.implementationFacts().transferSemantics());
            record.put("uiMethods", entry.implementationFacts().uiMethods());
            record.put("dependencies", entry.implementationFacts().dependencies());
            record.put("reusability", entry.implementationFacts().reusability());
            record.put("adapterCandidate", entry.implementationFacts().adapterCandidate());
            entries.add(record);
        }
        summary.put("admissible", entries);
        return summary;
    }

    @NotNull
    private static Map<String, Object> buildAdmissibilityReport(@NotNull AddonCorpusIndex index) {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("corpusVersion", index.corpusVersion());
        report.put("algorithm", index.algorithm());

        List<Map<String, Object>> entries = new ArrayList<>();
        for (AddonCorpusIndex.IndexedEntry entry : index.entries().values()) {
            Map<String, Object> record = new LinkedHashMap<>();
            record.put("corpusId", entry.corpusId());
            record.put("bedrockIdentifier", entry.bedrockIdentifier());
            record.put("admissible", entry.isAdmissible());
            record.put("reason", entry.admissibilityReason());
            record.put("capabilityScore", entry.capabilityScore());
            record.put("storageLocation", entry.storageLocation());
            entries.add(record);
        }
        report.put("entries", entries);
        return report;
    }

    private void writeJson(@NotNull Path path, @NotNull Object value) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            Constants.GSON.toJson(value, writer);
        }
    }
}
