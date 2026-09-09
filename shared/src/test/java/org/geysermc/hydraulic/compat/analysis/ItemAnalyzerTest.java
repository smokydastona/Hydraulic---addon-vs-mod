package org.geysermc.hydraulic.compat.analysis;

import net.minecraft.SharedConstants;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import org.geysermc.hydraulic.compat.ContentInventory;
import org.geysermc.hydraulic.compat.MappingOwnership;
import org.geysermc.hydraulic.compat.mapping.ContentPatch;
import org.geysermc.hydraulic.compat.model.CompatibilityObject;
import org.geysermc.hydraulic.compat.model.ModFingerprint;
import org.geysermc.hydraulic.compat.model.SupportLevel;
import org.geysermc.hydraulic.metadata.MetadataIndex;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ItemAnalyzerTest {
    private static final Identifier FLUID_ID = Identifier.fromNamespaceAndPath("minecraft", "water");
    private static final Identifier BUCKET_ID = Identifier.fromNamespaceAndPath("minecraft", "water_bucket");

    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void inheritsFluidBucketTextureFallbackWhenItemAssetIsMissing() {
        ContentPatch patch = new ContentPatch(
            FLUID_ID,
            "fluid",
            Map.of("visual.bucket_texture", "hydraulic_test_mod:barrel_pack"),
            MappingOwnership.USER,
            "user/fluids.json",
            MappingOwnership.USER.priority(),
            0
        );
        MetadataIndex metadataIndex = new MetadataIndex(Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(FLUID_ID, List.of(patch)), List.of(), MetadataIndex.Summary.empty());
        ContentInventory.ContentDescriptor descriptor = new ContentInventory.ContentDescriptor("item", "example", BUCKET_ID.toString(), true, false, List.of());

        CompatibilityObject object = new ItemAnalyzer().analyze(descriptor, emptyInventory(), metadataIndex);

        assertEquals(SupportLevel.ADAPTED, object.supportResults().get("presentation").level());
        assertEquals(FLUID_ID.toString(), object.inventoryFacts().get("fluid_source"));
        assertEquals("hydraulic_test_mod:barrel_pack", object.inventoryFacts().get("bucket_texture"));
        assertFalse(object.findings().stream().anyMatch(finding -> finding.code().equals("item.asset.missing")));
    }

    private static ContentInventory.ModContentInventory emptyInventory() {
        return new ContentInventory.ModContentInventory(
            "example",
            "example",
            "Example",
            "1.0.0",
            List.of(),
            new ModFingerprint("example", "example", "1.0.0", "test", "1.21.0", 0, 0, 0, 0, 0, 0, 0, false, false, false, false, false, false, false, false),
            Map.of(),
            Map.of(),
            Map.of(),
            Map.of(),
            Map.of(),
            Map.of(),
            Map.of(),
            Map.of()
        );
    }
}