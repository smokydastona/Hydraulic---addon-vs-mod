package org.geysermc.hydraulic.compat.corpus;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CorpusBuiltinBootstrapTest {
    @TempDir
    Path tempDir;

    @Test
    void installsBundledCorpusEntriesIntoCuratedBuiltinDirectory() {
        CorpusBuiltinBootstrap.installBuiltinEntries(LoggerFactory.getLogger("CorpusBuiltinBootstrapTest"), this.tempDir);

        Path builtinDirectory = this.tempDir.resolve("curated").resolve("builtin");
        assertTrue(Files.isDirectory(builtinDirectory));
        for (String fileName : List.of(
            "bedrock-energistics-core.json",
            "mojang-bedrock-samples.json",
            "utilitycraft-addon-template.json",
            "utilitycraft.json",
            "bedrock-core-server.json",
            "bedrock-core-ui.json",
            "bedrock-core-network.json",
            "engineering-tools.json",
            "bedrock-core-regolith-filters.json",
            "bedrock-oss-regolith.json",
            "bedrock-oss-bedrock-boost.json",
            "bedrock-oss-add-on-registry.json",
            "farmers-delight-bedrock.json"
        )) {
            assertTrue(Files.isRegularFile(builtinDirectory.resolve(fileName)), "missing " + fileName);
        }
    }

    @Test
    void bundledEntriesLoadIntoCorpusIndexWithExpectedAdmissibility() {
        AddonCorpusLoader loader = new AddonCorpusLoader(LoggerFactory.getLogger("CorpusBuiltinBootstrapTest"), this.tempDir);
        loader.ensureLayout();
        CorpusBuiltinBootstrap.installBuiltinEntries(LoggerFactory.getLogger("CorpusBuiltinBootstrapTest"), this.tempDir.resolve("corpus"));

        AddonCorpusIndex index = loader.refreshIndexFromSnapshots();

        assertEquals(13, index.entries().size());
        assertTrue(index.entries().get("bedrock-energistics-core").isAdmissible());
        assertTrue(index.entries().get("mojang-bedrock-samples").isAdmissible());
        assertTrue(!index.entries().get("utilitycraft-addon-template").isAdmissible());
        assertTrue(!index.entries().get("utilitycraft").isAdmissible());
        assertTrue(index.entries().get("bedrock-core-server").isAdmissible());
        assertTrue(index.entries().get("bedrock-core-ui").isAdmissible());
        assertTrue(index.entries().get("bedrock-core-network").isAdmissible());
        assertTrue(index.entries().get("engineering-tools").isAdmissible());
        assertTrue(!index.entries().get("bedrock-core-regolith-filters").isAdmissible());
        assertTrue(index.entries().get("bedrock-oss-regolith").isAdmissible());
        assertTrue(index.entries().get("bedrock-oss-bedrock-boost").isAdmissible());
        assertTrue(index.entries().get("bedrock-oss-add-on-registry").isAdmissible());
        assertTrue(index.entries().get("farmers-delight-bedrock").isAdmissible());
        assertEquals(10, loader.loadAdmissibleEntries().size());
    }
}
