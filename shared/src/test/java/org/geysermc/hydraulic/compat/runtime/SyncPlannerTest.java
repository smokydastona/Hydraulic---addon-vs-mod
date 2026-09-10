package org.geysermc.hydraulic.compat.runtime;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class SyncPlannerTest {
    @Test
    void coalescesRepeatedFieldChangesAndPreservesBoundaries() {
        Identifier machine = Identifier.fromNamespaceAndPath("test", "machine");
        StateChangeSet changes = new StateChangeSet(List.of(
            new StateChangeSet.FieldChange(machine, "inventory.slot.0", "stone:64", "stone:63"),
            new StateChangeSet.FieldChange(machine, "inventory.slot.0", "stone:63", "stone:62"),
            new StateChangeSet.FieldChange(machine, "progress", 1, 2)
        ));

        SyncBatch batch = new SyncPlanner().plan(changes);

        assertFalse(batch.empty());
        assertEquals(2, batch.changes().size());
        SyncChange inventory = batch.changes().stream().filter(change -> change.field().equals("inventory.slot.0")).findFirst().orElseThrow();
        assertEquals("stone:64", inventory.before());
        assertEquals("stone:62", inventory.after());
        assertEquals(SyncPriority.IMMEDIATE, inventory.priority());
    }

    @Test
    void dirtyTrackerFeedsPlannerThroughACompleteCycle() {
        Identifier machine = Identifier.fromNamespaceAndPath("test", "machine");
        DirtyStateTracker tracker = new DirtyStateTracker();
        tracker.record(new StateChangeSet(List.of(new StateChangeSet.FieldChange(machine, "energy.amount", 1000, 900))));

        SyncBatch batch = new SyncPlanner().plan(tracker.drain());

        assertEquals(1, batch.changes().size());
        assertEquals(SyncPriority.IMMEDIATE, batch.changes().getFirst().priority());
    }
}
