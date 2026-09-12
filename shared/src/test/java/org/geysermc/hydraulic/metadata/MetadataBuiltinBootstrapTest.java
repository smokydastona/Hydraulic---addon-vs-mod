package org.geysermc.hydraulic.metadata;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MetadataBuiltinBootstrapTest {
    @TempDir
    Path tempDir;

    @Test
    void installsCuratedBuiltinMetadataFiles() {
        MetadataBuiltinBootstrap.installBuiltinMetadata(LoggerFactory.getLogger("MetadataBuiltinBootstrapTest"), this.tempDir);

        Path builtinDir = this.tempDir.resolve("builtin");
        assertTrue(Files.isDirectory(builtinDir));
        for (String fileName : List.of(
            "immersiveengineering.crusher.json",
            "immersiveengineering.diesel_generator.json",
            "immersiveengineering.arc_furnace.json",
            "create.mechanical_mixer.json"
        )) {
            assertTrue(Files.isRegularFile(builtinDir.resolve(fileName)), "missing " + fileName);
        }
    }

    @Test
    void loadsInstalledBuiltinMetadataIntoIndex() {
        MetadataBuiltinBootstrap.installBuiltinMetadata(LoggerFactory.getLogger("MetadataBuiltinBootstrapTest"), this.tempDir);
        MetadataIndex index = new MetadataLoader(LoggerFactory.getLogger("MetadataBuiltinBootstrapTest")).load(this.tempDir);

        assertNotNull(index.blockMapping(Identifier.fromNamespaceAndPath("immersiveengineering", "crusher")));
        assertNotNull(index.blockMapping(Identifier.fromNamespaceAndPath("immersiveengineering", "diesel_generator")));
        assertNotNull(index.blockMapping(Identifier.fromNamespaceAndPath("immersiveengineering", "arc_furnace")));
        assertNotNull(index.blockMapping(Identifier.fromNamespaceAndPath("create", "mechanical_mixer")));

        assertTrue(index.summary().patchCount() >= 4);
        assertTrue(index.summary().blockMappingCount() >= 4);
    }
}
