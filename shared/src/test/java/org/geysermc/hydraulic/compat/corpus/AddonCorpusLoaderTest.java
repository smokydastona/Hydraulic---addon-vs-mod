package org.geysermc.hydraulic.compat.corpus;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AddonCorpusLoaderTest {
    @TempDir
    Path tempDir;

    @Test
    void storesAndLoadsCorpusIndexAndEntries() throws Exception {
        AddonCorpusLoader loader = new AddonCorpusLoader(LoggerFactory.getLogger("CorpusLoaderTest"), this.tempDir);
        loader.ensureLayout();

        AddonCorpusIndex index = new AddonCorpusIndex(
            "v1.0.0",
            "HYDRAULIC_CORPUS_INDEX_V1",
            Map.of("example-addon", new AddonCorpusIndex.IndexedEntry(
                "example-addon",
                "example:machine",
                "curated",
                true,
                "FULLY_PERMISSIVE",
                0.9D,
                System.currentTimeMillis()
            )),
            new AddonCorpusIndex.CorpusMetadata(1, 1, 0, System.currentTimeMillis())
        );
        loader.storeIndex(index);

        AddonCorpusEntry entry = sampleEntry("example-addon");
        loader.storeEntry(entry, "curated");

        AddonCorpusLoader reloaded = new AddonCorpusLoader(LoggerFactory.getLogger("CorpusLoaderTest"), this.tempDir);
        AddonCorpusIndex loadedIndex = reloaded.loadIndex();
        assertEquals(1, loadedIndex.entries().size());

        AddonCorpusEntry loadedEntry = reloaded.loadEntry("example-addon");
        assertNotNull(loadedEntry);
        assertEquals("example:machine", loadedEntry.identity().bedrockIdentifier());
        assertTrue(Files.isRegularFile(this.tempDir.resolve("corpus/curated/example-addon.json")));
    }

    private static AddonCorpusEntry sampleEntry(String corpusId) {
        return new AddonCorpusEntry(
            new AddonCorpusEntry.AddonIdentity(corpusId, "example:machine", "author", "Example Machine", "test entry"),
            new AddonCorpusEntry.AddonSource(AddonCorpusEntry.SourceType.GITHUB, "https://github.com/example/addon", "https://github.com/example/addon", null, null),
            new AddonCorpusEntry.AddonLicense("MIT", null, null, true, true, true, false),
            new AddonCorpusEntry.AddonAdmissibility(true, AddonCorpusEntry.AdmissibilityReason.FULLY_PERMISSIVE, null, java.util.List.of()),
            new AddonCorpusEntry.AddonVersions("1.0.0", Set.of("1.21.0"), "1.20.0", "1.21.0", java.util.List.of("1.0.0")),
            new AddonCorpusEntry.AddonBehaviorPack(true, "1.21.0", Set.of(), Set.of("example:machine"), Set.of(), Set.of(), Set.of(), Set.of(), Map.of()),
            new AddonCorpusEntry.AddonResourcePack(true, "1.21.0", Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), Map.of()),
            new AddonCorpusEntry.AddonCapabilities(Set.of("machine"), Set.of("processor"), Set.of("item"), Set.of(), Set.of(), Set.of(), Set.of(), Map.of()),
            new AddonCorpusEntry.AddonEvidence(java.util.List.of("manifest"), java.util.List.of(), java.util.List.of(), java.util.List.of(), java.util.List.of(), Map.of()),
            new AddonCorpusEntry.AddonConfidence(0.9D, "manifest", java.util.List.of("machine block"), java.util.List.of(), java.util.List.of()),
            new AddonCorpusEntry.AddonProvenance("test", System.currentTimeMillis(), null, null, "v1.0.0", java.util.List.of())
        );
    }
}
