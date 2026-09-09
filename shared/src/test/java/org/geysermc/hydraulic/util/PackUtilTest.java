package org.geysermc.hydraulic.util;

import net.minecraft.SharedConstants;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import org.geysermc.hydraulic.cache.ConversionKey;
import org.geysermc.hydraulic.compat.MappingOwnership;
import org.geysermc.hydraulic.compat.mapping.ContentPatch;
import org.geysermc.hydraulic.metadata.BlockMapping;
import org.geysermc.hydraulic.metadata.BlockStateRule;
import org.geysermc.hydraulic.metadata.IdentifierMapping;
import org.geysermc.hydraulic.metadata.MetadataIndex;
import org.geysermc.hydraulic.pack.ModResourceIndex;
import org.geysermc.hydraulic.platform.mod.ModInfo;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class PackUtilTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @TempDir
    Path tempDir;

    @Test
    void conversionKeyChangesWhenIndexedResourceFingerprintChanges() throws IOException {
        Path root = this.tempDir.resolve("root");
        Path item = root.resolve("assets/example/items/test_item.json");
        Files.createDirectories(item.getParent());
        Files.writeString(item, "{}");

        ModInfo mod = new ModInfo("examplemod", "example", "Example Mod", "1.0.0", null, List.of(root));
        ConversionKey first = PackUtil.conversionKey(mod, ModResourceIndex.create(mod, LoggerFactory.getLogger("PackUtilTest")), MetadataIndex.empty());

        Files.writeString(item, "{\"changed\":true}");
        ConversionKey second = PackUtil.conversionKey(mod, ModResourceIndex.create(mod, LoggerFactory.getLogger("PackUtilTest")), MetadataIndex.empty());

        assertNotEquals(first.resourceFingerprint(), second.resourceFingerprint());
        assertNotEquals(first.packUuid(), second.packUuid());
    }

    @Test
    void conversionKeyChangesWhenMetadataChanges() throws IOException {
        Path root = this.tempDir.resolve("root");
        Path item = root.resolve("assets/example/items/test_item.json");
        Files.createDirectories(item.getParent());
        Files.writeString(item, "{}");

        ModInfo mod = new ModInfo("examplemod", "example", "Example Mod", "1.0.0", null, List.of(root));
        ModResourceIndex resourceIndex = ModResourceIndex.create(mod, LoggerFactory.getLogger("PackUtilTest"));
        ConversionKey withoutMetadata = PackUtil.conversionKey(mod, resourceIndex, MetadataIndex.empty());
        MetadataIndex withMetadata = new MetadataIndex(
            Map.of(),
            Map.of(
                Identifier.fromNamespaceAndPath("example", "test_item"),
                new IdentifierMapping(
                    Identifier.fromNamespaceAndPath("example", "test_item"),
                    Identifier.fromNamespaceAndPath("example", "bedrock_item"),
                    MappingOwnership.USER,
                    "test.json",
                    MappingOwnership.USER.priority(),
                    0
                )
            ),
            Map.of(),
            Map.of(),
            Map.of(),
            Map.of(),
            List.of(),
            new MetadataIndex.Summary(1, 0, 1, 0, 0, 0, 0, 0, 0, Map.of("user", 1))
        );

        ConversionKey withMetadataKey = PackUtil.conversionKey(mod, resourceIndex, withMetadata);

        assertNotEquals(withoutMetadata.metadataFingerprint(), withMetadataKey.metadataFingerprint());
        assertNotEquals(withoutMetadata.packUuid(), withMetadataKey.packUuid());
        assertEquals(withoutMetadata.resourceFingerprint(), withMetadataKey.resourceFingerprint());
    }

    @Test
    void metadataFingerprintHashesAuthoringFieldsWithoutRuntimeReflection() {
        Identifier javaId = Identifier.fromNamespaceAndPath("example", "test_block");
        MetadataIndex metadataIndex = new MetadataIndex(
            Map.of(
                javaId,
                new BlockMapping(
                    javaId,
                    List.of(new BlockStateRule(
                        Map.of("facing", "north"),
                        Identifier.fromNamespaceAndPath("example", "bedrock_block"),
                        Map.of("direction", "north"),
                        "minecraft:geometry.full_block",
                        "example:block/test_block",
                        true,
                        "machine",
                        MappingOwnership.USER,
                        "mods/example.json",
                        MappingOwnership.USER.priority(),
                        0
                    ))
                )
            ),
            Map.of(),
            Map.of(),
            Map.of(),
            Map.of(),
            Map.of(
                javaId,
                List.of(new ContentPatch(
                    javaId,
                    "block",
                    Map.of("behavior.tag", "machine", "bedrock.identifier", "example:bedrock_block"),
                    MappingOwnership.USER,
                    "mods/example.patch.json",
                    MappingOwnership.USER.priority(),
                    0
                ))
            ),
            List.of(),
            new MetadataIndex.Summary(1, 1, 0, 0, 0, 0, 1, 1, 0, Map.of("user", 1))
        );

        String first = PackUtil.metadataFingerprint(metadataIndex);
        String second = PackUtil.metadataFingerprint(metadataIndex);

        assertEquals(first, second);
        assertNotEquals("", first);
    }
}