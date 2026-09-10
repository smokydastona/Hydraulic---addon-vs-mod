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
import java.util.TreeMap;
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
        void conversionKeyChangesWhenDependentModFingerprintChanges() throws IOException {
                Path baseRoot = this.tempDir.resolve("base");
                Path dependentRoot = this.tempDir.resolve("dependent");
                Path baseModel = baseRoot.resolve("assets/base/models/item/test_item.json");
                Path dependentModel = dependentRoot.resolve("assets/dependent/models/item/shared.json");
                Files.createDirectories(baseModel.getParent());
                Files.createDirectories(dependentModel.getParent());
                Files.writeString(baseModel, """
                        {
                            "parent": "dependent:item/shared"
                        }
                        """);
                Files.writeString(dependentModel, """
                        {
                            "textures": {
                                "layer0": "dependent:item/shared"
                            }
                        }
                        """);

                ModInfo baseMod = new ModInfo("base", "base", "Base Mod", "1.0.0", null, List.of(baseRoot));
                ModInfo dependentMod = new ModInfo("dependent", "dependent", "Dependent Mod", "1.0.0", null, List.of(dependentRoot));
                ModResourceIndex baseIndex = ModResourceIndex.create(baseMod, LoggerFactory.getLogger("PackUtilTest"));
                ModResourceIndex dependentIndex = ModResourceIndex.create(dependentMod, LoggerFactory.getLogger("PackUtilTest"));

                ConversionKey first = PackUtil.conversionKey(baseMod, baseIndex, Map.of(
                        baseMod.id(), baseIndex,
                        dependentMod.id(), dependentIndex
                ), MetadataIndex.empty());

                Files.writeString(dependentModel, """
                        {
                            "textures": {
                                "layer0": "dependent:item/changed"
                            }
                        }
                        """);
                ModResourceIndex changedDependentIndex = ModResourceIndex.create(dependentMod, LoggerFactory.getLogger("PackUtilTest"));

                ConversionKey second = PackUtil.conversionKey(baseMod, baseIndex, Map.of(
                        baseMod.id(), baseIndex,
                        dependentMod.id(), changedDependentIndex
                ), MetadataIndex.empty());

                assertEquals(first.resourceFingerprint(), second.resourceFingerprint());
                assertNotEquals(first.dependencyFingerprint(), second.dependencyFingerprint());
                assertNotEquals(first.packUuid(), second.packUuid());
        }

            @Test
            void conversionKeyIgnoresUnrelatedDependentResourcesOutsideExplicitEdges() throws IOException {
            Path baseRoot = this.tempDir.resolve("base-explicit");
            Path dependentRoot = this.tempDir.resolve("dependent-explicit");
            Path baseModel = baseRoot.resolve("assets/base/models/item/test_item.json");
            Path dependentModel = dependentRoot.resolve("assets/dependent/models/item/shared.json");
            Path dependentTexture = dependentRoot.resolve("assets/dependent/textures/item/shared.png");
            Path unrelatedTexture = dependentRoot.resolve("assets/dependent/textures/item/unrelated.png");
            Files.createDirectories(baseModel.getParent());
            Files.createDirectories(dependentModel.getParent());
            Files.createDirectories(dependentTexture.getParent());
            Files.writeString(baseModel, """
                {
                    "parent": "dependent:item/shared"
                }
                """);
            Files.writeString(dependentModel, """
                {
                    "textures": {
                    "layer0": "dependent:item/shared"
                    }
                }
                """);
            Files.writeString(dependentTexture, "png");
            Files.writeString(unrelatedTexture, "png-a");

            ModInfo baseMod = new ModInfo("base", "base", "Base Mod", "1.0.0", null, List.of(baseRoot));
            ModInfo dependentMod = new ModInfo("dependent", "dependent", "Dependent Mod", "1.0.0", null, List.of(dependentRoot));
            ModResourceIndex baseIndex = ModResourceIndex.create(baseMod, LoggerFactory.getLogger("PackUtilTest"));
            ModResourceIndex dependentIndex = ModResourceIndex.create(dependentMod, LoggerFactory.getLogger("PackUtilTest"));

            ConversionKey first = PackUtil.conversionKey(baseMod, baseIndex, Map.of(
                baseMod.id(), baseIndex,
                dependentMod.id(), dependentIndex
            ), MetadataIndex.empty());

            Files.writeString(unrelatedTexture, "png-b");
            ModResourceIndex changedDependentIndex = ModResourceIndex.create(dependentMod, LoggerFactory.getLogger("PackUtilTest"));

            ConversionKey second = PackUtil.conversionKey(baseMod, baseIndex, Map.of(
                baseMod.id(), baseIndex,
                dependentMod.id(), changedDependentIndex
            ), MetadataIndex.empty());

            assertEquals(first.dependencyFingerprint(), second.dependencyFingerprint());
            assertEquals(first.packUuid(), second.packUuid());
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

    @Test
    void compatibilityEngineFingerprintIsDeterministicAndNonEmpty() {
        String first = PackUtil.compatibilityEngineFingerprint();
        String second = PackUtil.compatibilityEngineFingerprint();

        assertEquals(first, second);
        assertNotEquals("", first);
    }

    @Test
    void compatibilityCacheFingerprintChangesWhenEngineFingerprintChanges() {
        TreeMap<String, String> modFingerprints = new TreeMap<>();
        modFingerprints.put("example", "fingerprint-1");

        String first = PackUtil.compatibilityCacheFingerprint("metadata-1", modFingerprints, "engine-1");
        String second = PackUtil.compatibilityCacheFingerprint("metadata-1", modFingerprints, "engine-2");

        assertNotEquals(first, second);
    }

    @Test
    void startupCompatibilityFingerprintChangesWhenAdapterCatalogFingerprintChanges() {
        TreeMap<String, String> modFingerprints = new TreeMap<>();
        modFingerprints.put("example", "fingerprint-1");

        String first = PackUtil.startupCompatibilityFingerprint("metadata-1", modFingerprints, "engine-1", "adapter-catalog-1");
        String second = PackUtil.startupCompatibilityFingerprint("metadata-1", modFingerprints, "engine-1", "adapter-catalog-2");

        assertNotEquals(first, second);
    }

    @Test
    void adapterCatalogFingerprintDefaultsToStableBuiltinOnlyValue() {
        assertEquals(PackUtil.adapterCatalogFingerprint(), PackUtil.adapterCatalogFingerprint());
        assertNotEquals("", PackUtil.adapterCatalogFingerprint());
    }
}