package org.geysermc.hydraulic.compat.adapter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AdapterCatalogCacheTest {
    @TempDir
    Path tempDir;

    @Test
    void persistsTypedModSpecificBindingsAndTheirPrecedence() {
        AdapterCatalogCache cache = new AdapterCatalogCache(LoggerFactory.getLogger("test"), this.tempDir);
        AdapterCatalog catalog = AdapterCatalog.from(
            "catalog-v1",
            Map.of("machine.processor", new AdapterBinding("builtin.processor", AdapterFeature.MENU_FALLBACK_TRANSLATION, "builtin")),
            Map.of("machine.processor", new AdapterBinding("mod.processor", AdapterFeature.BLOCK_ENTITY_PATCH_TRANSLATION, "mod-specific"))
        );

        cache.storeCatalog(catalog);
        AdapterCatalog loaded = cache.loadCatalog();

        assertNotNull(loaded);
        assertEquals("catalog-v1", loaded.fingerprint());
        assertEquals(AdapterFeature.BLOCK_ENTITY_PATCH_TRANSLATION, loaded.resolve("machine.processor").feature());
        assertEquals("mod.processor", loaded.resolve("machine.processor").adapterId());
    }
}