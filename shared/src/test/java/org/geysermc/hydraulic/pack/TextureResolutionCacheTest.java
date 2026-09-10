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
        assertEquals("textures/items/examplemod/gizmo.png", cache.resolveModelOutput("examplemod", Key.key("examplemod", "item/gizmo.png")));
        assertEquals("textures/entity/examplemod/equipment/humanoid/widget.png", cache.resolveModelOutput("examplemod", Key.key("examplemod", "textures/entity/equipment/humanoid/widget.png")));
        assertEquals("textures/blocks/examplemod/ore.png", cache.resolveBlockTextureOutput("examplemod", Key.key("examplemod", "block/ore.png")));
        assertEquals("textures/items/examplemod/widget.png", cache.resolveModelOutput("examplemod", Key.key("examplemod", "item/widget")));

        TextureResolutionCache.CacheMetrics metrics = cache.metrics();
        assertEquals(1, metrics.hits());
        assertEquals(5, metrics.misses());
        assertEquals(3, metrics.evictions());
        assertEquals(2, metrics.size());
    }

    @Test
    void usesConverterMappingForHumanoidArmorTextures() {
        TextureResolutionCache cache = new TextureResolutionCache();

        assertEquals(
            "textures/models/create/armor/copper_1.png",
            cache.resolveModelOutput("create", Key.key("create", "entity/equipment/humanoid/copper"))
        );
        assertEquals(
            "textures/entity/create/equipment/humanoid/cardboard.png",
            cache.resolveModelOutput("create", Key.key("create", "entity/equipment/humanoid/cardboard"))
        );
    }
}