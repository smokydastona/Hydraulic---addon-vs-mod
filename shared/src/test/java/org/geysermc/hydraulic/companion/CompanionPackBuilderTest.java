package org.geysermc.hydraulic.companion;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompanionPackBuilderTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(CompanionPackBuilderTest.class);

    @Test
    void buildsDeterministicMcpackAndReusesCacheWhenUnchanged(@TempDir Path tempDir) throws IOException {
        Path resourcePack = tempDir.resolve("resource_pack");
        Files.createDirectories(resourcePack.resolve("textures"));
        Files.writeString(resourcePack.resolve("manifest.json"), "{ \"header\": {} }");
        Files.writeString(resourcePack.resolve("textures").resolve("a.png"), "fake-texture-bytes");

        Path cacheDir = tempDir.resolve("cache");
        CompanionPackBuilder builder = new CompanionPackBuilder(LOGGER, cacheDir);

        CompanionBuildResult first = builder.build("test-companion", resourcePack);
        assertTrue(first.rebuilt());
        assertTrue(Files.isRegularFile(first.mcpackPath()));

        CompanionBuildResult second = builder.build("test-companion", resourcePack);
        assertFalse(second.rebuilt());
        assertEquals(first.mcpackPath(), second.mcpackPath());
        assertEquals(first.sha256(), second.sha256());

        // Changing content must invalidate the cached build and produce a new fingerprint.
        Files.writeString(resourcePack.resolve("textures").resolve("a.png"), "different-texture-bytes");
        CompanionBuildResult third = builder.build("test-companion", resourcePack);
        assertTrue(third.rebuilt());
        assertFalse(third.sha256().equals(first.sha256()));
    }
}
