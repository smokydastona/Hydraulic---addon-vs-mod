package org.geysermc.hydraulic.item;

import net.kyori.adventure.key.Key;
import net.minecraft.resources.Identifier;
import org.geysermc.hydraulic.platform.mod.ModInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;
import team.unnamed.creative.equipment.EquipmentLayerType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class EquipmentAssetLoaderTest {
    @TempDir
    Path tempDir;

  @Test
  void resolvesLogicalEquipmentTextureToLayerQualifiedSourcePath() {
    Key resolved = EquipmentAssetLoader.sourceTextureKey(EquipmentLayerType.HUMANOID, Key.key("create", "copper"));

    assertEquals("create:entity/equipment/humanoid/copper", resolved.asString());
  }

  @Test
  void preservesAlreadyQualifiedEquipmentTexturePaths() {
    Key resolved = EquipmentAssetLoader.sourceTextureKey(EquipmentLayerType.HUMANOID, Key.key("example", "entity/equipment/humanoid/widget"));

    assertEquals("example:entity/equipment/humanoid/widget", resolved.asString());
  }

    @Test
    void loadsEquipmentLayersFromModAssets() throws IOException {
        Path asset = this.tempDir.resolve("assets/examplemod/equipment/barrel.json");
        Files.createDirectories(asset.getParent());
        Files.writeString(asset, """
            {
              "layers": {
                "humanoid": [
                  { "texture": "examplemod:barrel" }
                ],
                "humanoid_leggings": [
                  { "texture": "examplemod:barrel_leggings" }
                ],
                "horse_body": [
                  { "texture": "examplemod:horse_barrel" }
                ]
              }
            }
            """);

        ModInfo mod = new ModInfo("examplemod", "examplemod", "Example Mod", "1.0.0", null, List.of(this.tempDir));
        EquipmentAssetLoader.EquipmentAsset equipment = EquipmentAssetLoader.load(mod, Identifier.parse("examplemod:barrel"), LoggerFactory.getLogger("test"));

        assertNotNull(equipment);
        assertEquals(List.of(Key.key("examplemod:barrel")), equipment.layers(EquipmentLayerType.HUMANOID));
        assertEquals(List.of(Key.key("examplemod:barrel_leggings")), equipment.layers(EquipmentLayerType.HUMANOID_LEGGINGS));
        assertEquals(List.of(Key.key("examplemod:horse_barrel")), equipment.layers(EquipmentLayerType.HORSE_BODY));
    }

    @Test
    void returnsNullForMissingEquipmentAsset() {
        ModInfo mod = new ModInfo("examplemod", "examplemod", "Example Mod", "1.0.0", null, List.of(this.tempDir));
        EquipmentAssetLoader.EquipmentAsset equipment = EquipmentAssetLoader.load(mod, Identifier.parse("examplemod:missing"), LoggerFactory.getLogger("test"));

        assertNull(equipment);
    }
}