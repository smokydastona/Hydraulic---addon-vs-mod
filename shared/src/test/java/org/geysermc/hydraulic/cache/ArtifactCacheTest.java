package org.geysermc.hydraulic.cache;

import org.geysermc.hydraulic.Constants;
import org.geysermc.hydraulic.compat.CompatibilityProfile;
import org.geysermc.hydraulic.compat.CompatibilityReport;
import org.geysermc.hydraulic.compat.ContentInventory;
import org.geysermc.hydraulic.compat.CompatibilityStatus;
import org.geysermc.hydraulic.compat.model.ModFingerprint;
import org.geysermc.hydraulic.compat.model.SupportResult;
import org.geysermc.hydraulic.compat.model.SupportLevel;
import org.geysermc.hydraulic.metadata.MetadataIndex;
import org.geysermc.hydraulic.pack.ModResourceIndex;
import org.geysermc.hydraulic.pack.PackValidationReport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ArtifactCacheTest {
    @TempDir
    Path tempDir;

    @Test
    void storesAndLoadsCompatibilitySnapshotByCacheKey() {
        ArtifactCache cache = new ArtifactCache(LoggerFactory.getLogger("ArtifactCacheTest"), this.tempDir.resolve("cache"));
        cache.ensureLayout();

        ArtifactCache.CompatibilityCacheKey key = new ArtifactCache.CompatibilityCacheKey("compat-key-1");
        ContentInventory inventory = sampleInventory();
        CompatibilityReport report = sampleReport();
        cache.storeCompatibilitySnapshot(new ArtifactCache.CompatibilitySnapshot(
            new ArtifactCache.CompatibilityManifest(key.value(), "metadata-1", 1, Map.of("testmod", "fingerprint-1")),
            inventory,
            report
        ));

        ArtifactCache.CompatibilitySnapshot loaded = cache.loadCompatibilitySnapshot(key);
        assertNotNull(loaded);
        assertEquals("compat-key-1", loaded.manifest().cacheKey());
        assertEquals(1, loaded.inventory().mods().size());
        assertEquals("testmod", loaded.report().profile("testmod").modId());
        assertNull(cache.loadCompatibilitySnapshot(new ArtifactCache.CompatibilityCacheKey("compat-key-2")));
    }

    @Test
    void storesIndexConversionAndValidationArtifacts() throws IOException {
        ArtifactCache cache = new ArtifactCache(LoggerFactory.getLogger("ArtifactCacheTest"), this.tempDir.resolve("cache"));
        cache.ensureLayout();

        ArtifactCache.IndexSnapshot snapshot = new ArtifactCache.IndexSnapshot(
            "HYDRAULIC_INDEX_SNAPSHOT_V1",
            1,
            Map.of("testmod", new ArtifactCache.IndexedMod(
                new ModResourceIndex.ResourceFingerprint("HYDRAULIC_INDEX_V1", 3, 42, 99, "abc"),
                1,
                2,
                4,
                true,
                Map.of("textures", 2)
            ))
        );
        cache.storeIndexSnapshot(snapshot);
        cache.storeConversionArtifact("testmod", new ArtifactCache.ConversionArtifact(
            "testmod",
            new ConversionKey("alg", "testmod", "1.0.0", "hydraulic", "26.2", "rfp", 2, 42, "metadata"),
            "packs/testmod.mcpack",
            "uuid-1"
        ));
        PackValidationReport report = new PackValidationReport(Map.of(
            "testmod",
            new PackValidationReport.ModValidation("packs/testmod.mcpack", true, true, 12, List.of(), List.of(), List.of("manual"))
        ));
        cache.storeValidationArtifact(report, "compat-key-1");

        assertNotNull(cache.loadIndexSnapshot());

        try (Reader reader = Files.newBufferedReader(this.tempDir.resolve("cache/conversions/testmod/conversion-manifest.json"))) {
            ArtifactCache.ConversionArtifact conversion = Constants.GSON.fromJson(reader, ArtifactCache.ConversionArtifact.class);
            assertEquals("uuid-1", conversion.packUuid());
        }
        try (Reader reader = Files.newBufferedReader(this.tempDir.resolve("cache/validation/pack-validation-report.json"))) {
            PackValidationReport written = Constants.GSON.fromJson(reader, PackValidationReport.class);
            assertEquals(1, written.perMod().size());
            assertEquals(1, written.perMod().get("testmod").manualActionCount());
        }
    }

    private static ContentInventory sampleInventory() {
        return new ContentInventory(Map.of(
            "testmod",
            new ContentInventory.ModContentInventory(
                "testmod",
                "testmod",
                "Test Mod",
                "1.0.0",
                List.of("/tmp/testmod"),
                new ModFingerprint("testmod", "testmod", "1.0.0", "fabric", "26.2", 1, 1, 0, 0, 0, 0, 0, false, false, false, false, false, true, false, false),
                Map.of("blocks", 1),
                Map.of("blocks", List.of("testmod:block")),
                Map.of("block_assets", 1),
                Map.of("block_assets", List.of("testmod:block")),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of()
            )
        ));
    }

    private static CompatibilityReport sampleReport() {
        return new CompatibilityReport(
            "2026-09-08T00:00:00Z",
            MetadataIndex.Summary.empty(),
            List.of(),
            Map.of(
                "testmod",
                new CompatibilityProfile(
                    "testmod",
                    new ModFingerprint("testmod", "testmod", "1.0.0", "fabric", "26.2", 1, 1, 0, 0, 0, 0, 0, false, false, false, false, false, true, false, false),
                    SupportLevel.AUTOMATIC,
                    CompatibilityStatus.COMPLETE,
                    100,
                    Map.of("content", new SupportResult("content", SupportLevel.AUTOMATIC, CompatibilityStatus.COMPLETE, 100, List.of("present"), List.of(), List.of())),
                    Map.of("AUTOMATIC", 1),
                    List.of(),
                    List.of(),
                    List.of("note")
                )
            )
        );
    }
}