package org.geysermc.hydraulic.pack;

import net.kyori.adventure.key.Key;
import org.geysermc.pack.converter.pipeline.ExtractionContext;
import org.geysermc.hydraulic.platform.mod.ModInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;
import org.geysermc.pack.converter.util.LogListener;
import team.unnamed.creative.ResourcePack;
import team.unnamed.creative.base.Writable;
import team.unnamed.creative.texture.Texture;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SelectiveTextureExtractorTest {
    @TempDir
    Path tempDir;

    @Test
    void extractsIndexedTexturesWithoutDependingOnParsedPackTextures() throws IOException {
        this.writeTexture("assets/examplemod/textures/item/indexed.png");

        ModResourceIndex resourceIndex = ModResourceIndex.create(mod(), LoggerFactory.getLogger("SelectiveTextureExtractorTest"));
        TextureDependencyGraph dependencies = new TextureDependencyGraph();
        dependencies.recordDependency("model", "examplemod:item/test", Key.key("examplemod", "item/indexed"));
        SelectiveTextureExtractor extractor = new SelectiveTextureExtractor(resourceIndex, dependencies);

        ResourcePack parsedPack = ResourcePack.resourcePack();
        parsedPack.texture(Texture.texture(Key.key("examplemod", "item/parsed_only"), Writable.bytes(new byte[] {1})));

        Collection<Texture> extracted = extractor.extract(parsedPack, new ExtractionContext(null, Optional.empty(), new NoOpLogListener()));

        assertEquals(1, extracted.size());
        assertTrue(extracted.stream().anyMatch(texture -> Key.key("examplemod", "item/indexed").equals(texture.key())));
        assertFalse(extracted.stream().anyMatch(texture -> Key.key("examplemod", "item/parsed_only").equals(texture.key())));
        assertEquals(List.of(Key.key("examplemod", "item/indexed")), extracted.stream().map(Texture::key).toList());
    }

    private void writeTexture(String relativePath) throws IOException {
        Path texturePath = this.tempDir.resolve(relativePath);
        Files.createDirectories(texturePath.getParent());
        Files.write(texturePath, new byte[] {1, 2, 3});
    }

    private ModInfo mod() {
        return new ModInfo("examplemod", "examplemod", "Example Mod", "1.0.0", null, List.of(this.tempDir));
    }

    private static final class NoOpLogListener implements LogListener {
        @Override
        public void debugUnchecked(String message) {
        }

        @Override
        public void info(String message) {
        }

        @Override
        public void warn(String message) {
        }

        @Override
        public void error(String message) {
        }

        @Override
        public void error(String message, Throwable throwable) {
        }
    }
}