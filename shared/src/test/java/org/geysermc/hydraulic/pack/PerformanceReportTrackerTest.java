package org.geysermc.hydraulic.pack;

import org.geysermc.hydraulic.Constants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PerformanceReportTrackerTest {
    @TempDir
    Path tempDir;

    @Test
    void recordsAndWritesStartupAndPackConversionMetrics() throws IOException {
        Path reportPath = this.tempDir.resolve("reports/performance-report.json");
        PerformanceReportTracker tracker = new PerformanceReportTracker(LoggerFactory.getLogger("PerformanceReportTrackerTest"), reportPath);

        tracker.recordStartup(new PerformanceReport.StartupMetrics(11, 12, 13, 14, 15, 16, 9, 7, 5, 44, 55, 4, 3, 2, 1, 6, 8, 10, 12, 14));
        tracker.recordPackConversion(new PerformanceReport.PackConversionMetrics(
            42,
            10,
            1,
            2,
            3,
            4,
            5,
            6,
            7,
            Map.of("examplemod", new PerformanceReport.ModConversionMetrics("converted", 21))
        ));

        PerformanceReport snapshot = tracker.snapshot();
        assertNotNull(snapshot.startup());
        assertNotNull(snapshot.lastPackConversion());
        assertEquals(11, snapshot.startup().indexedResourcesMillis());
        assertEquals(44, snapshot.startup().indexedBlockStates());
        assertEquals(55, snapshot.startup().indexedItemAssets());
        assertEquals(42, snapshot.lastPackConversion().totalMillis());
        assertEquals("converted", snapshot.lastPackConversion().perMod().get("examplemod").outcome());

        PerformanceReport written;
        try (Reader reader = Files.newBufferedReader(reportPath)) {
            written = Constants.GSON.fromJson(reader, PerformanceReport.class);
        }
        assertNotNull(written);
        assertEquals(15, written.startup().modelIndexBuildMillis());
        assertEquals(44, written.startup().indexedBlockStates());
        assertEquals(21, written.lastPackConversion().perMod().get("examplemod").millis());
    }
}
