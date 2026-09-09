package org.geysermc.hydraulic.pack;

import org.geysermc.hydraulic.Constants;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

final class PerformanceReportTracker {
    private final Logger logger;
    private final Path reportPath;
    private PerformanceReport report = PerformanceReport.empty();

    PerformanceReportTracker(@NotNull Logger logger, @NotNull Path reportPath) {
        this.logger = logger;
        this.reportPath = reportPath;
    }

    synchronized void recordStartup(@NotNull PerformanceReport.StartupMetrics metrics) {
        this.report = this.report.withStartup(metrics);
        this.write();
    }

    synchronized void recordPackConversion(@NotNull PerformanceReport.PackConversionMetrics metrics) {
        this.report = this.report.withPackConversion(metrics);
        this.write();
    }

    synchronized void recordModelResolutionCache(@NotNull PerformanceReport.CacheMetrics metrics) {
        this.report = this.report.withModelResolutionCache(metrics);
        this.write();
    }

    synchronized void recordModelProviderCache(@NotNull PerformanceReport.ModelProviderMetrics metrics) {
        this.report = this.report.withModelProviderCache(metrics);
        this.write();
    }

    synchronized void recordArtifactCache(@NotNull PerformanceReport.ArtifactCacheMetrics metrics) {
        this.report = this.report.withArtifactCache(metrics);
        this.write();
    }

    synchronized void recordRuntimeDispatch(@NotNull PerformanceReport.RuntimeDispatchMetrics metrics) {
        this.report = this.report.withRuntimeDispatch(metrics);
        this.write();
    }

    @NotNull
    synchronized PerformanceReport snapshot() {
        return this.report;
    }

    private void write() {
        try {
            Files.createDirectories(this.reportPath.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(this.reportPath)) {
                Constants.GSON.toJson(this.report, writer);
            }
        } catch (IOException e) {
            this.logger.error("Failed to write performance artifact {}", this.reportPath, e);
        }
    }
}
