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
        tracker.recordModelResolutionCache(new PerformanceReport.CacheMetrics(5, 2));
        tracker.recordModelProviderCache(new PerformanceReport.ModelProviderMetrics(12, 4, 3, 7, 99));
        tracker.recordTextureResolutionCache(new PerformanceReport.TextureResolutionMetrics(20, 5, 2, 11));
        tracker.recordArtifactCache(new PerformanceReport.ArtifactCacheMetrics(
            new PerformanceReport.CacheMetrics(1, 0),
            new PerformanceReport.CacheMetrics(0, 1),
            new PerformanceReport.CacheMetrics(2, 3),
            new PerformanceReport.CacheMetrics(4, 5)
        ));
        tracker.recordRuntimeDispatch(new PerformanceReport.RuntimeDispatchMetrics(
            new PerformanceReport.CacheMetrics(6, 1),
            new PerformanceReport.CacheMetrics(7, 2),
            new PerformanceReport.CacheMetrics(8, 3),
            new PerformanceReport.CacheMetrics(9, 4),
            new PerformanceReport.CacheMetrics(10, 5),
            new PerformanceReport.CacheMetrics(11, 6)
        ));
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
            30,
            20,
            10,
            4,
            Map.of("examplemod", new PerformanceReport.ModConversionMetrics("converted", 21, 9, 1, 2, 3, 8, 6, 2, 1))
        ));

        PerformanceReport snapshot = tracker.snapshot();
        assertNotNull(snapshot.startup());
        assertNotNull(snapshot.lastPackConversion());
        assertEquals(11, snapshot.startup().indexedResourcesMillis());
        assertEquals(44, snapshot.startup().indexedBlockStates());
        assertEquals(55, snapshot.startup().indexedItemAssets());
        assertEquals(42, snapshot.lastPackConversion().totalMillis());
        assertEquals(30, snapshot.lastPackConversion().discoveredTextures());
        assertEquals(20, snapshot.lastPackConversion().selectedTextures());
        assertEquals(5, snapshot.modelResolutionCache().hits());
        assertEquals(2, snapshot.modelResolutionCache().misses());
        assertEquals(12, snapshot.modelProviderCache().hits());
        assertEquals(3, snapshot.modelProviderCache().evictions());
        assertEquals(20, snapshot.textureResolutionCache().hits());
        assertEquals(1, snapshot.artifactCache().index().hits());
        assertEquals(1, snapshot.artifactCache().compatibility().misses());
        assertEquals(7, snapshot.runtimeDispatch().items().hits());
        assertEquals(11, snapshot.runtimeDispatch().fluids().hits());
        assertEquals(9, snapshot.lastPackConversion().perMod().get("examplemod").validationMillis());
        assertEquals(8, snapshot.lastPackConversion().perMod().get("examplemod").discoveredTextures());
        assertEquals("converted", snapshot.lastPackConversion().perMod().get("examplemod").outcome());

        PerformanceReport written;
        try (Reader reader = Files.newBufferedReader(reportPath)) {
            written = Constants.GSON.fromJson(reader, PerformanceReport.class);
        }
        assertNotNull(written);
        assertEquals(15, written.startup().modelIndexBuildMillis());
        assertEquals(44, written.startup().indexedBlockStates());
        assertEquals(7, written.modelResolutionCache().requests());
        assertEquals(16, written.modelProviderCache().requests());
        assertEquals(99, written.modelProviderCache().indexedModels());
        assertEquals(25, written.textureResolutionCache().requests());
        assertEquals(11, written.textureResolutionCache().size());
        assertEquals(10, written.lastPackConversion().omittedTextures());
        assertEquals(1, written.lastPackConversion().perMod().get("examplemod").textureDependencySources());
        assertEquals(5, written.artifactCache().validation().misses());
        assertEquals(13, written.runtimeDispatch().menus().requests());
        assertEquals(17, written.runtimeDispatch().fluids().requests());
        assertEquals(3, written.lastPackConversion().perMod().get("examplemod").validationManualActions());
        assertEquals(21, written.lastPackConversion().perMod().get("examplemod").millis());
    }
}
