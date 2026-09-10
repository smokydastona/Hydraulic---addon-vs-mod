package org.geysermc.hydraulic.pack;

import org.geysermc.hydraulic.Constants;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

final class PackValidationTracker {
    private final Logger logger;
    private final Path reportPath;
    private PackValidationReport report = PackValidationReport.empty();

    PackValidationTracker(@NotNull Logger logger, @NotNull Path reportPath) {
        this.logger = logger;
        this.reportPath = reportPath;
    }

    synchronized void record(@NotNull String modId, @NotNull PackValidationReport.ModValidation validation) {
        this.report = this.report.withValidation(modId, validation);
        this.write();
    }

    @NotNull
    synchronized PackValidationReport snapshot() {
        return this.report;
    }

    private void write() {
        try {
            Files.createDirectories(this.reportPath.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(this.reportPath)) {
                Constants.GSON.toJson(this.report, writer);
            }
        } catch (IOException e) {
            this.logger.error("Failed to write pack validation artifact {}", this.reportPath, e);
        }
    }
}