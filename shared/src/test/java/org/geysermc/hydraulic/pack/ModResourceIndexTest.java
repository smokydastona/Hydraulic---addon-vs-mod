package org.geysermc.hydraulic.pack;

import net.minecraft.resources.Identifier;
import org.geysermc.hydraulic.platform.mod.ModInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModResourceIndexTest {
    @TempDir
    Path tempDir;

    @Test
    void indexesNamespacesBlockstatesAndItemAssetsAcrossRoots() throws IOException {
        Path firstRoot = this.tempDir.resolve("rootA");
        Path secondRoot = this.tempDir.resolve("rootB");

        Path blockstate = firstRoot.resolve("assets/examplemod/blockstates/machines/crusher.json");
        Path modernItem = secondRoot.resolve("assets/examplemod/items/tools/wrench.json");
        Path legacyItem = firstRoot.resolve("assets/examplemod/models/item/tools/hammer.json");
        Files.createDirectories(blockstate.getParent());
        Files.createDirectories(modernItem.getParent());
        Files.createDirectories(legacyItem.getParent());
        Files.writeString(blockstate, "{}");
        Files.writeString(modernItem, "{}");
        Files.writeString(legacyItem, "{}");

        ModInfo mod = new ModInfo("examplemod", "examplemod", "Example Mod", "1.0.0", null, List.of(firstRoot, secondRoot));
        ModResourceIndex index = ModResourceIndex.create(mod, LoggerFactory.getLogger("ModResourceIndexTest"));

        assertTrue(index.namespaces().contains("examplemod"));
        assertTrue(index.hasBlockState(Identifier.fromNamespaceAndPath("examplemod", "machines/crusher")));
        assertEquals(modernItem, index.resolveItemAssetPath(Identifier.fromNamespaceAndPath("examplemod", "tools/wrench")));
        assertEquals(legacyItem, index.resolveItemAssetPath(Identifier.fromNamespaceAndPath("examplemod", "tools/hammer")));
        assertNull(index.resolveItemAssetPath(Identifier.fromNamespaceAndPath("examplemod", "tools/missing")));
    }

    @Test
    void prefersEarlierRootsWhenDuplicateAssetsExist() throws IOException {
        Path firstRoot = this.tempDir.resolve("rootA");
        Path secondRoot = this.tempDir.resolve("rootB");
        Path firstItem = firstRoot.resolve("assets/examplemod/items/test_item.json");
        Path secondItem = secondRoot.resolve("assets/examplemod/items/test_item.json");
        Files.createDirectories(firstItem.getParent());
        Files.createDirectories(secondItem.getParent());
        Files.writeString(firstItem, "{\"from\":\"first\"}");
        Files.writeString(secondItem, "{\"from\":\"second\"}");

        ModInfo mod = new ModInfo("examplemod", "examplemod", "Example Mod", "1.0.0", null, List.of(firstRoot, secondRoot));
        ModResourceIndex index = ModResourceIndex.create(mod, LoggerFactory.getLogger("ModResourceIndexTest"));

        Path resolved = index.resolveItemAssetPath(Identifier.fromNamespaceAndPath("examplemod", "test_item"));
        assertNotNull(resolved);
        assertEquals(firstItem, resolved);
    }
}
