package org.geysermc.hydraulic.pack;

import net.minecraft.resources.Identifier;
import org.geysermc.hydraulic.platform.mod.ModInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class PackManagerItemAssetLookupTest {
    @TempDir
    Path tempDir;

    @Test
    void prefersModernItemDefinitionWhenPresent() throws IOException {
        Path modernItem = this.tempDir.resolve("assets/examplemod/items/test_item.json");
        Path legacyModel = this.tempDir.resolve("assets/examplemod/models/item/test_item.json");
        Files.createDirectories(modernItem.getParent());
        Files.createDirectories(legacyModel.getParent());
        Files.writeString(modernItem, "{}");
        Files.writeString(legacyModel, "{}");

        ModInfo mod = mod();
        Path resolved = ItemAssetLocator.resolveItemAssetPath(mod, Identifier.fromNamespaceAndPath("examplemod", "test_item"));

        assertNotNull(resolved);
        assertEquals(modernItem, resolved);
    }

    @Test
    void fallsBackToLegacyItemModelWhenModernDefinitionIsMissing() throws IOException {
        Path legacyModel = this.tempDir.resolve("assets/examplemod/models/item/test_item.json");
        Files.createDirectories(legacyModel.getParent());
        Files.writeString(legacyModel, "{}");

        Path resolved = ItemAssetLocator.resolveItemAssetPath(mod(), Identifier.fromNamespaceAndPath("examplemod", "test_item"));

        assertNotNull(resolved);
        assertEquals(legacyModel, resolved);
    }

    @Test
    void returnsNullWhenNoItemAssetsExist() {
        Path resolved = ItemAssetLocator.resolveItemAssetPath(mod(), Identifier.fromNamespaceAndPath("examplemod", "missing_item"));

        assertNull(resolved);
    }

    private ModInfo mod() {
        return new ModInfo("examplemod", "examplemod", "Example Mod", "1.0.0", null, List.of(this.tempDir));
    }
}