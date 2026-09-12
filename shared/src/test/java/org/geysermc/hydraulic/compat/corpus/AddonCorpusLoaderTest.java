package org.geysermc.hydraulic.compat.corpus;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;

class AddonCorpusLoaderTest {
    @TempDir
    Path tempDir;

    @Test
    void storesAndLoadsCorpusIndexAndEntries() throws Exception {
        AddonCorpusLoader loader = new AddonCorpusLoader(LoggerFactory.getLogger("CorpusLoaderTest"), this.tempDir);
        loader.ensureLayout();

        AddonCorpusIndex index = new AddonCorpusIndex(
            "v2.0.0",
            "HYDRAULIC_CORPUS_INDEX_V2",
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
        assertTrue(Files.isRegularFile(this.tempDir.resolve("corpus/corpus-manifest.json")));

        AddonCorpusEntry loadedEntry = reloaded.loadEntry("example-addon");
        assertNotNull(loadedEntry);
        assertEquals("example:machine", loadedEntry.identity().bedrockIdentifier());
        assertTrue(Files.isRegularFile(this.tempDir.resolve("corpus/curated/example-addon.json")));
    }

    @Test
    void refreshesIndexFromSnapshotsWithCuratedPrecedence() {
        AddonCorpusLoader loader = new AddonCorpusLoader(LoggerFactory.getLogger("CorpusLoaderTest"), this.tempDir);
        loader.ensureLayout();

        loader.storeEntry(sampleEntry("example-addon"), "sources");
        loader.storeEntry(sampleEntry("example-addon"), "generated");
        loader.storeEntry(sampleEntry("example-addon"), "curated");

        AddonCorpusIndex refreshed = loader.refreshIndexFromSnapshots();

        assertEquals(1, refreshed.entries().size());
        assertEquals("curated", refreshed.entries().get("example-addon").storageLocation());
        assertNotNull(loader.loadEntry("example-addon"));
        assertEquals(1, loader.loadAdmissibleEntries().size());
        assertTrue(Files.isRegularFile(this.tempDir.resolve("corpus/corpus-manifest.json")));
    }

    @Test
    void rejectsCorpusEntryPathTraversal() throws Exception {
        AddonCorpusLoader loader = new AddonCorpusLoader(LoggerFactory.getLogger("CorpusLoaderTest"), this.tempDir);
        loader.ensureLayout();
        AddonCorpusIndex index = new AddonCorpusIndex(
            "v2.0.0",
            "HYDRAULIC_CORPUS_INDEX_V2",
            Map.of("../outside", new AddonCorpusIndex.IndexedEntry(
                "../outside", "example:machine", "../", true, "FULLY_PERMISSIVE", 0.9D, System.currentTimeMillis()
            )),
            new AddonCorpusIndex.CorpusMetadata(1, 1, 0, System.currentTimeMillis())
        );
        loader.storeIndex(index);

        assertNull(loader.loadEntry("../outside"));
        assertTrue(!Files.exists(this.tempDir.resolve("outside.json")));
    }

    @Test
    void rejectsEntryWhoseIdentityDiffersFromIndex() throws Exception {
        AddonCorpusLoader loader = new AddonCorpusLoader(LoggerFactory.getLogger("CorpusLoaderTest"), this.tempDir);
        loader.ensureLayout();
        AddonCorpusIndex index = new AddonCorpusIndex(
            "v2.0.0",
            "HYDRAULIC_CORPUS_INDEX_V2",
            Map.of("example-addon", new AddonCorpusIndex.IndexedEntry(
                "example-addon", "example:machine", "curated", true, "FULLY_PERMISSIVE", 0.9D, System.currentTimeMillis()
            )),
            new AddonCorpusIndex.CorpusMetadata(1, 1, 0, System.currentTimeMillis())
        );
        loader.storeIndex(index);
        loader.storeEntry(sampleEntry("different-addon"), "curated");
        Files.move(
            this.tempDir.resolve("corpus/curated/different-addon.json"),
            this.tempDir.resolve("corpus/curated/example-addon.json")
        );

        AddonCorpusLoader reloaded = new AddonCorpusLoader(LoggerFactory.getLogger("CorpusLoaderTest"), this.tempDir);
        reloaded.loadIndex();
        assertNull(reloaded.loadEntry("example-addon"));
    }

    @Test
    void rejectsTamperedCorpusIndexManifest() throws Exception {
        AddonCorpusLoader loader = new AddonCorpusLoader(LoggerFactory.getLogger("CorpusLoaderTest"), this.tempDir);
        loader.ensureLayout();
        AddonCorpusIndex index = new AddonCorpusIndex(
            "v2.0.0",
            "HYDRAULIC_CORPUS_INDEX_V2",
            Map.of(),
            new AddonCorpusIndex.CorpusMetadata(0, 0, 0, System.currentTimeMillis())
        );
        loader.storeIndex(index);
        Files.writeString(
            this.tempDir.resolve("corpus/corpus-index.json"),
            Files.readString(this.tempDir.resolve("corpus/corpus-index.json")).replace("v2.0.0", "tampered")
        );

        AddonCorpusLoader reloaded = new AddonCorpusLoader(LoggerFactory.getLogger("CorpusLoaderTest"), this.tempDir);
        assertTrue(reloaded.loadIndex().entries().isEmpty());
    }

    @Test
    void changesFingerprintWhenLocalCorpusIndexChanges() {
        AddonCorpusLoader loader = new AddonCorpusLoader(LoggerFactory.getLogger("CorpusLoaderTest"), this.tempDir);
        loader.ensureLayout();

        loader.storeIndex(new AddonCorpusIndex(
            "v2.0.0",
            "HYDRAULIC_CORPUS_INDEX_V2",
            Map.of(),
            new AddonCorpusIndex.CorpusMetadata(0, 0, 0, 1L)
        ));
        String first = loader.fingerprint();

        loader.storeIndex(new AddonCorpusIndex(
            "v2.0.0",
            "HYDRAULIC_CORPUS_INDEX_V2",
            Map.of(),
            new AddonCorpusIndex.CorpusMetadata(0, 0, 0, 2L)
        ));

        assertTrue(!first.equals(loader.fingerprint()));
    }

    @Test
    void rejectsSemanticallyInvalidEntry() throws Exception {
        AddonCorpusLoader loader = new AddonCorpusLoader(LoggerFactory.getLogger("CorpusLoaderTest"), this.tempDir);
        loader.ensureLayout();
        AddonCorpusIndex index = new AddonCorpusIndex(
            "v2.0.0", "HYDRAULIC_CORPUS_INDEX_V2",
            Map.of("example-addon", new AddonCorpusIndex.IndexedEntry(
                "example-addon", "example:machine", "curated", true, "FULLY_PERMISSIVE", 0.9D, System.currentTimeMillis()
            )),
            new AddonCorpusIndex.CorpusMetadata(1, 1, 0, System.currentTimeMillis())
        );
        loader.storeIndex(index);
        AddonCorpusEntry valid = sampleEntry("example-addon");
        AddonCorpusEntry invalid = new AddonCorpusEntry(
            valid.identity(),
            new AddonCorpusEntry.AddonSource(AddonCorpusEntry.SourceType.GITHUB, "http://github.com/example/addon", valid.source().repositoryUrl(), null, null),
            valid.license(),
            valid.admissibility(),
            valid.versions(),
            valid.behaviorPack(),
            valid.resourcePack(),
            valid.capabilities(),
            valid.evidence(),
            new AddonCorpusEntry.AddonConfidence(1.5D, "test", List.of(), List.of(), List.of()),
            valid.implementationFacts(),
            valid.provenance()
        );
        loader.storeEntry(invalid, "curated");

        AddonCorpusLoader reloaded = new AddonCorpusLoader(LoggerFactory.getLogger("CorpusLoaderTest"), this.tempDir);
        reloaded.loadIndex();
        assertNull(reloaded.loadEntry("example-addon"));
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
            new AddonCorpusEntry.AddonImplementationFacts(
                "custom component machine",
                java.util.List.of("single-block processing only"),
                java.util.List.of("bounded per-tick transfer"),
                java.util.List.of("dynamic properties"),
                java.util.List.of("player interact", "server tick"),
                java.util.List.of("slot-checked insertion and extraction"),
                java.util.List.of("custom form"),
                java.util.List.of("example:core"),
                "high",
                "generic-machine"
            ),
            new AddonCorpusEntry.AddonProvenance("test", System.currentTimeMillis(), null, null, "v1.0.0", java.util.List.of())
        );
    }
}
