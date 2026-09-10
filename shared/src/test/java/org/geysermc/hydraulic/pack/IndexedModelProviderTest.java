package org.geysermc.hydraulic.pack;

import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;
import team.unnamed.creative.model.Model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class IndexedModelProviderTest {
    @TempDir
    Path tempDir;

    @Test
    void loadsModelsOnDemandAndEvictsWhenCacheIsBounded() throws IOException {
        Path firstModel = this.writeModel("assets/examplemod/models/item/first.json", "minecraft:item/generated", "minecraft:item/apple");
        Path secondModel = this.writeModel("assets/examplemod/models/item/second.json", "minecraft:item/generated", "minecraft:item/carrot");

        Map<Key, Path> modelPaths = new LinkedHashMap<>();
        modelPaths.put(Key.key("examplemod", "item/first"), firstModel);
        modelPaths.put(Key.key("examplemod", "item/second"), secondModel);

        IndexedModelProvider provider = new IndexedModelProvider(LoggerFactory.getLogger("IndexedModelProviderTest"), modelPaths, null, 1);

        Model first = provider.model(Key.key("examplemod", "item/first"));
        assertNotNull(first);
        assertEquals(Key.key("minecraft", "item/generated"), first.parent());

        Model firstAgain = provider.model(Key.key("examplemod", "item/first"));
        assertNotNull(firstAgain);

        Model second = provider.model(Key.key("examplemod", "item/second"));
        assertNotNull(second);

        Model firstReloaded = provider.model(Key.key("examplemod", "item/first"));
        assertNotNull(firstReloaded);
        assertNull(provider.model(Key.key("examplemod", "item/missing")));

        IndexedModelProvider.CacheMetrics metrics = provider.cacheMetrics();
        assertEquals(1, metrics.hits());
        assertEquals(4, metrics.misses());
        assertEquals(3, metrics.evictions());
        assertEquals(1, metrics.size());
        assertEquals(2, metrics.indexedModels());
    }

    @Test
    void isolatesMalformedVanillaModelsAndNegativeCachesTheFailure() throws IOException {
        Path vanillaPack = this.tempDir.resolve("vanilla.zip");
        try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(vanillaPack))) {
            output.putNextEntry(new ZipEntry("assets/minecraft/models/block/invalid_rotation.json"));
            output.write("""
                {
                  "elements": [
                    {
                      "from": [0, 0, 0],
                      "to": [16, 16, 16],
                      "rotation": { "origin": [8, 8, 8], "axis": "x", "angle": 67.5 }
                    }
                  ]
                }
                """.getBytes());
            output.closeEntry();
        }

        IndexedModelProvider provider = new IndexedModelProvider(LoggerFactory.getLogger("IndexedModelProviderTest"), Map.of(), vanillaPack, 4);

        assertNull(provider.model(Key.key("minecraft", "block/invalid_rotation")));
        assertNull(provider.model(Key.key("minecraft", "block/invalid_rotation")));
        assertEquals(1, provider.cacheMetrics().misses());
        assertEquals(1, provider.cacheMetrics().hits());
    }

    private Path writeModel(String relativePath, String parent, String layer0) throws IOException {
        Path path = this.tempDir.resolve(relativePath);
        Files.createDirectories(path.getParent());
        Files.writeString(path, """
            {
              "parent": "%s",
              "textures": {
                "layer0": "%s"
              }
            }
            """.formatted(parent, layer0));
        return path;
    }
}