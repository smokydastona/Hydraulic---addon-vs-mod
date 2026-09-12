package org.geysermc.hydraulic.compat.corpus.java;

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
 * Writes an offline summary of the Java-mod capability-semantics corpus, mirroring
 * {@link org.geysermc.hydraulic.compat.corpus.CorpusReportWriter} for the Bedrock corpus but
 * covering Java-side API references instead.
 */
public final class JavaModCorpusReportWriter {
    private final Logger logger;
    private final Path reportsPath;

    public JavaModCorpusReportWriter(@NotNull Logger logger, @NotNull Path dataPath) {
        this.logger = logger;
        this.reportsPath = dataPath.resolve("reports");
    }

    public void writeReport(@NotNull JavaModCorpusIndex index, @NotNull List<JavaModCorpusEntry> admissibleEntries) {
        try {
            Files.createDirectories(this.reportsPath);
            this.writeJson(this.reportsPath.resolve("java-corpus-summary.json"), buildSummary(index, admissibleEntries));
            this.logger.info(
                "Wrote Java-mod corpus report (entries={}, admissible={})",
                index.entries().size(),
                admissibleEntries.size()
            );
        } catch (IOException e) {
            this.logger.error("Failed to write Java-mod corpus report to {}", this.reportsPath, e);
        }
    }

    @NotNull
    private static Map<String, Object> buildSummary(@NotNull JavaModCorpusIndex index, @NotNull List<JavaModCorpusEntry> admissibleEntries) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("corpusVersion", index.corpusVersion());
        summary.put("algorithm", index.algorithm());
        summary.put("totalEntries", index.metadata().totalEntries());
        summary.put("admissibleEntries", index.metadata().admissibleEntries());
        summary.put("inadmissibleEntries", index.metadata().inadmissibleEntries());
        summary.put("lastUpdatedEpochMillis", index.metadata().lastUpdatedEpochMillis());

        Map<String, Integer> capabilityCoverage = new LinkedHashMap<>();
        for (JavaModCorpusEntry entry : admissibleEntries) {
            capabilityCoverage.merge(entry.capability(), 1, Integer::sum);
        }
        summary.put("capabilityCoverage", capabilityCoverage);

        List<Map<String, Object>> entries = new ArrayList<>();
        for (JavaModCorpusEntry entry : admissibleEntries) {
            Map<String, Object> record = new LinkedHashMap<>();
            record.put("corpusId", entry.identity().corpusId());
            record.put("displayName", entry.identity().displayName());
            record.put("capability", entry.capability());
            record.put("sourceType", entry.source().sourceType().name());
            record.put("sourceUrl", entry.source().sourceUrl());
            record.put("license", entry.license().licenseType());
            record.put("javaApiReferences", entry.javaApiReferences());
            record.put("implementationPattern", entry.implementationPattern());
            record.put("bedrockFeasibilityHint", entry.bedrockFeasibilityHint());
            record.put("simulationSupport", entry.semanticFacts().simulationSupport());
            record.put("sidedAccess", entry.semanticFacts().sidedAccess());
            record.put("filteringSupport", entry.semanticFacts().filteringSupport());
            record.put("confidence", entry.confidence().overallScore());
            entries.add(record);
        }
        summary.put("admissible", entries);
        return summary;
    }

    private void writeJson(@NotNull Path path, @NotNull Object value) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            Constants.GSON.toJson(value, writer);
        }
    }
}
