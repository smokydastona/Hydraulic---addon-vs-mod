package org.geysermc.hydraulic.compat.corpus.java;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaModCorpusLoaderTest {
    @TempDir
    Path tempDir;

    @Test
    void storesAndLoadsJavaModCorpusIndexAndEntries() {
        JavaModCorpusLoader loader = new JavaModCorpusLoader(LoggerFactory.getLogger("JavaModCorpusLoaderTest"), this.tempDir);
        loader.ensureLayout();
        loader.storeEntry(sampleEntry("example-api"), "curated");
        JavaModCorpusIndex refreshed = loader.refreshIndexFromSnapshots();

        assertEquals(1, refreshed.entries().size());
        assertTrue(Files.isRegularFile(this.tempDir.resolve("corpus/java/java-corpus-manifest.json")));

        JavaModCorpusLoader reloaded = new JavaModCorpusLoader(LoggerFactory.getLogger("JavaModCorpusLoaderTest"), this.tempDir);
        JavaModCorpusIndex loadedIndex = reloaded.loadIndex();
        assertEquals(1, loadedIndex.entries().size());

        JavaModCorpusEntry loadedEntry = reloaded.loadEntry("example-api");
        assertNotNull(loadedEntry);
        assertEquals("ITEM_TRANSFER", loadedEntry.capability());
        assertEquals(1, reloaded.loadAdmissibleEntries().size());
    }

    @Test
    void rejectsCorpusEntryPathTraversal() {
        JavaModCorpusLoader loader = new JavaModCorpusLoader(LoggerFactory.getLogger("JavaModCorpusLoaderTest"), this.tempDir);
        loader.ensureLayout();
        JavaModCorpusIndex index = new JavaModCorpusIndex(
            "v1.0.0",
            "HYDRAULIC_JAVA_CORPUS_INDEX_V1",
            java.util.Map.of("../outside", new JavaModCorpusIndex.IndexedEntry(
                "../outside", "ITEM_TRANSFER", "../", true, "FULLY_PERMISSIVE", 0.9D, System.currentTimeMillis()
            )),
            new JavaModCorpusIndex.CorpusMetadata(1, 1, 0, System.currentTimeMillis())
        );
        loader.storeIndex(index);

        assertNull(loader.loadEntry("../outside"));
        assertTrue(!Files.exists(this.tempDir.resolve("outside.json")));
    }

    @Test
    void rejectsEntryWithNonCanonicalCapability() {
        JavaModCorpusEntry invalid = sampleEntryWithCapability("bad-capability", "NOT_A_REAL_CAPABILITY");
        List<String> errors = JavaModCorpusValidator.validate(invalid);
        assertTrue(errors.stream().anyMatch(error -> error.contains("canonical capability vocabulary")));
    }

    @Test
    void rejectsAdmissibleEntryWithoutRedistributionRights() {
        JavaModCorpusEntry entry = new JavaModCorpusEntry(
            new JavaModCorpusEntry.Identity("id", "Display", "desc"),
            new JavaModCorpusEntry.Source(JavaModCorpusEntry.SourceType.GITHUB, "https://github.com/example/api", null, null),
            new JavaModCorpusEntry.License("Proprietary", null, false, false, false, false),
            new JavaModCorpusEntry.Admissibility(true, "FULLY_PERMISSIVE", null, List.of()),
            "ITEM_TRANSFER",
            List.of("com.example.Api"),
            new JavaModCorpusEntry.SemanticFacts(List.of(), List.of(), List.of(), true, true, true),
            "pattern",
            List.of(),
            "hint",
            new JavaModCorpusEntry.Confidence(0.9, "test", List.of(), List.of(), List.of()),
            new JavaModCorpusEntry.Provenance("test", 1L, "v1.0.0", List.of())
        );
        List<String> errors = JavaModCorpusValidator.validate(entry);
        assertTrue(errors.stream().anyMatch(error -> error.contains("redistribution and modification")));
    }

    private static JavaModCorpusEntry sampleEntry(String corpusId) {
        return sampleEntryWithCapability(corpusId, "ITEM_TRANSFER");
    }

    private static JavaModCorpusEntry sampleEntryWithCapability(String corpusId, String capability) {
        return new JavaModCorpusEntry(
            new JavaModCorpusEntry.Identity(corpusId, "Example API", "test entry"),
            new JavaModCorpusEntry.Source(JavaModCorpusEntry.SourceType.GITHUB, "https://github.com/example/api", "https://github.com/example/api", null),
            new JavaModCorpusEntry.License("Apache-2.0", null, true, true, true, true),
            new JavaModCorpusEntry.Admissibility(true, "FULLY_PERMISSIVE", null, List.of()),
            capability,
            List.of("com.example.Api"),
            new JavaModCorpusEntry.SemanticFacts(List.of("state"), List.of("input"), List.of("output"), true, true, true),
            "example implementation pattern",
            List.of(),
            "example feasibility hint",
            new JavaModCorpusEntry.Confidence(0.9, "test", List.of(), List.of(), List.of()),
            new JavaModCorpusEntry.Provenance("test", 1L, "v1.0.0", List.of())
        );
    }
}
