package org.geysermc.hydraulic.compat.handoff;

import org.geysermc.hydraulic.cache.ArtifactCache;
import org.geysermc.hydraulic.compat.CompatibilityReport;
import org.geysermc.hydraulic.compat.ContentInventory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompatibilityHandoffExporterTest {
    @TempDir
    Path tempDir;

    @Test
    void exportsPendingEntriesAsynchronously() throws Exception {
        CompatibilityHandoffQueue queue = new CompatibilityHandoffQueue(LoggerFactory.getLogger("HandoffExporterTest"), this.tempDir);
        queue.ensureLayout();
        queue.enqueue(CompatibilityReport.empty(), envelope("fingerprint-export"));

        CompatibilityHandoffExporter exporter = new CompatibilityHandoffExporter(LoggerFactory.getLogger("HandoffExporterTest"), queue, this.tempDir);
        exporter.scheduleRetryProcessing();
        exporter.shutdown();

        for (int attempt = 0; attempt < 20 && queue.completedEntries().isEmpty(); attempt++) {
            TimeUnit.MILLISECONDS.sleep(50);
        }

        assertEquals(1, queue.completedEntries().size());
        assertTrue(Files.list(this.tempDir.resolve("handoff-queue/exports")).findAny().isPresent());
    }

    private static HandoffEnvelope envelope(String fingerprint) {
        ArtifactCache.StartupCompatibilityKey key = new ArtifactCache.StartupCompatibilityKey(fingerprint);
        ArtifactCache.CompatibilityManifest manifest = new ArtifactCache.CompatibilityManifest(
            key,
            "metadata",
            "engine",
            "adapter-catalog",
            0,
            Map.of()
        );
        return HandoffEnvelope.create(
            manifest,
            ContentInventory.empty(),
            CompatibilityReport.empty(),
            "test",
            "26.2",
            "geyser-test",
            "geyser-test"
        );
    }
}
