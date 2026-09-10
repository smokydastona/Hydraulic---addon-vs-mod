package org.geysermc.hydraulic.compat.runtime;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SyncEncoderTest {
    @Test
    void encodesCoalescedInventorySlotAsBedrockFacingUpdate() {
        Identifier machine = Identifier.fromNamespaceAndPath("test", "machine");
        StateChangeSet state = new StateChangeSet(List.of(
            new StateChangeSet.FieldChange(
                machine,
                "inventory.slot.3",
                new TransferBridgeFactory.ItemStackView("minecraft:iron_ingot", 64),
                new TransferBridgeFactory.ItemStackView("minecraft:iron_ingot", 60)
            )
        ));
        SyncBatch batch = new SyncPlanner().plan(state);

        EncodedSyncChange encoded = new SyncEncoder().encode(batch).getFirst();

        assertEquals(EncodedSyncKind.INVENTORY_SLOT, encoded.kind());
        assertEquals(3, encoded.slot());
        assertEquals("minecraft:iron_ingot", encoded.beforeItemId());
        assertEquals(64, encoded.beforeCount());
        assertEquals("minecraft:iron_ingot", encoded.afterItemId());
        assertEquals(60, encoded.afterCount());
        assertEquals("inventory.slot.3", encoded.sourceField());
    }

    @Test
    void preservesUnsupportedStateAsTraceableGenericUpdate() {
        Identifier machine = Identifier.fromNamespaceAndPath("test", "machine");
        SyncBatch batch = new SyncPlanner().plan(new StateChangeSet(List.of(
            new StateChangeSet.FieldChange(machine, "progress", 1, 2)
        )));

        EncodedSyncChange encoded = new SyncEncoder().encode(batch).getFirst();

        assertEquals(EncodedSyncKind.GENERIC_STATE, encoded.kind());
        assertEquals(1, encoded.beforeValue());
        assertEquals(2, encoded.afterValue());
        assertEquals(-1, encoded.slot());
    }

    @Test
    void encodesContainerPropertiesAsMenuStateUpdates() {
        Identifier machine = Identifier.fromNamespaceAndPath("test", "machine");
        SyncBatch batch = new SyncPlanner().plan(new StateChangeSet(List.of(
            new StateChangeSet.FieldChange(machine, "container.property.2", 10, 20)
        )));

        EncodedSyncChange encoded = new SyncEncoder().encode(batch).getFirst();

        assertEquals(EncodedSyncKind.CONTAINER_PROPERTY, encoded.kind());
        assertEquals(2, encoded.slot());
        assertEquals(10, encoded.beforeValue());
        assertEquals(20, encoded.afterValue());
    }
}
