package org.geysermc.hydraulic.compat.runtime;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SyncDispatcherTest {
    @Test
    void drainsPlansEncodesAndDeliversDirtyStateOnce() {
        DirtyStateTracker dirty = new DirtyStateTracker();
        Identifier machine = Identifier.fromNamespaceAndPath("hydraulic", "machine");
        dirty.record(new StateChangeSet(List.of(new StateChangeSet.FieldChange(
            machine,
            "inventory.slot.0",
            new TransferBridgeFactory.ItemStackView("minecraft:stone", 2),
            new TransferBridgeFactory.ItemStackView("minecraft:stone", 1)
        ))));
        List<EncodedSyncChange> delivered = new ArrayList<>();
        SyncDispatcher dispatcher = new SyncDispatcher(
            dirty,
            new SyncPlanner(),
            new SyncEncoder(),
            changes -> {
                delivered.addAll(changes);
                return changes.stream()
                    .map(change -> new SyncDeliveryResult(change, SyncDeliveryStatus.SENT, null))
                    .toList();
            }
        );

        List<SyncDeliveryResult> results = dispatcher.flush();

        assertEquals(1, results.size());
        assertEquals(EncodedSyncKind.INVENTORY_SLOT, delivered.getFirst().kind());
        assertFalse(dirty.dirty());
        assertTrue(dispatcher.flush().isEmpty());
    }
}
