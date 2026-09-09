package org.geysermc.hydraulic.pack;

import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TextureResolutionCacheTest {
    @Test
    void cachesModelAndBlockTextureOutputs() {
        TextureResolutionCache cache = new TextureResolutionCache(2);

        assertEquals("textures/items/examplemod/gizmo.png", cache.resolveModelOutput("examplemod", Key.key("examplemod", "item/gizmo")));
        assertEquals("textures/items/examplemod/gizmo.png", cache.resolveModelOutput("examplemod", Key.key("examplemod", "item/gizmo")));
        assertEquals("textures/blocks/examplemod/ore.png", cache.resolveBlockTextureOutput("examplemod", Key.key("examplemod", "block/ore.png")));
        assertEquals("textures/items/examplemod/widget.png", cache.resolveModelOutput("examplemod", Key.key("examplemod", "item/widget")));

        TextureResolutionCache.CacheMetrics metrics = cache.metrics();
        assertEquals(1, metrics.hits());
        assertEquals(3, metrics.misses());
        assertEquals(1, metrics.evictions());
        assertEquals(2, metrics.size());
    }
}