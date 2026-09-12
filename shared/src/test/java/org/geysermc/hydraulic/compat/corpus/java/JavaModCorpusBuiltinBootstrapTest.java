package org.geysermc.hydraulic.compat.corpus.java;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaModCorpusBuiltinBootstrapTest {
    @TempDir
    Path tempDir;

    @Test
    void installsBundledJavaModCorpusEntriesIntoCuratedBuiltinDirectory() {
        JavaModCorpusBuiltinBootstrap.installBuiltinEntries(LoggerFactory.getLogger("JavaModCorpusBuiltinBootstrapTest"), this.tempDir);

        Path builtinDirectory = this.tempDir.resolve("curated").resolve("builtin");
        assertTrue(Files.isDirectory(builtinDirectory));
        for (String fileName : List.of("forge-capabilities.json", "fabric-transfer-api.json")) {
            assertTrue(Files.isRegularFile(builtinDirectory.resolve(fileName)), "missing " + fileName);
        }
    }

    @Test
    void bundledEntriesLoadIntoJavaModCorpusIndexAsAdmissible() {
        JavaModCorpusLoader loader = new JavaModCorpusLoader(LoggerFactory.getLogger("JavaModCorpusBuiltinBootstrapTest"), this.tempDir);
        loader.ensureLayout();
        JavaModCorpusBuiltinBootstrap.installBuiltinEntries(LoggerFactory.getLogger("JavaModCorpusBuiltinBootstrapTest"), this.tempDir.resolve("corpus").resolve("java"));

        JavaModCorpusIndex index = loader.refreshIndexFromSnapshots();

        assertEquals(2, index.entries().size());
        assertTrue(index.entries().get("forge-capabilities").isAdmissible());
        assertTrue(index.entries().get("fabric-transfer-api").isAdmissible());
        assertEquals("ITEM_TRANSFER", index.entries().get("forge-capabilities").capability());
        assertEquals(2, loader.loadAdmissibleEntries().size());
    }
}
