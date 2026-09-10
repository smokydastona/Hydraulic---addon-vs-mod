package org.geysermc.hydraulic.pack;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TextureAnimationMetadataReaderTest {
    @TempDir
    Path tempDir;

    @Test
    void returnsNullWhenMetadataIsMissing() throws IOException {
        Path texture = this.tempDir.resolve("example.png");
        Files.writeString(texture, "png");

        assertNull(TextureAnimationMetadataReader.frameTime(texture));
    }

    @Test
    void readsExplicitFrameTime() throws IOException {
        Path texture = this.tempDir.resolve("animated.png");
        Files.writeString(texture, "png");
        Files.writeString(texture.resolveSibling("animated.png.mcmeta"), """
            {
              "animation": {
                "frametime": 4
              }
            }
            """);

        assertEquals(4, TextureAnimationMetadataReader.frameTime(texture));
    }

    @Test
    void defaultsFrameTimeToOneWhenAnimationMetadataExists() throws IOException {
        Path texture = this.tempDir.resolve("animated.png");
        Files.writeString(texture, "png");
        Files.writeString(texture.resolveSibling("animated.png.mcmeta"), """
            {
              "animation": {}
            }
            """);

        assertEquals(1, TextureAnimationMetadataReader.frameTime(texture));
    }
}