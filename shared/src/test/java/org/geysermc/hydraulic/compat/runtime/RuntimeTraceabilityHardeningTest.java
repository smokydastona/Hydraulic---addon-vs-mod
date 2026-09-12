package org.geysermc.hydraulic.compat.runtime;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeTraceabilityHardeningTest {
    @Test
    void runtimeTraceIdsAreUniqueAndNonBlank() {
        RuntimeTraceId first = RuntimeTraceId.create();
        RuntimeTraceId second = RuntimeTraceId.create();

        assertNotNull(first);
        assertNotNull(second);
        assertFalse(first.value().isBlank());
        assertFalse(second.value().isBlank());
        assertFalse(first.value().equals(second.value()));
    }

    @Test
    void syncDeliveryResultRequiresReasonWhenFailed() {
        EncodedSyncChange change = new EncodedSyncChange(
            net.minecraft.resources.Identifier.fromNamespaceAndPath("hydraulic", "test"),
            "container.property.0",
            EncodedSyncKind.CONTAINER_PROPERTY,
            0,
            null,
            0,
            null,
            0,
            null,
            7,
            org.geysermc.hydraulic.compat.runtime.SyncPriority.IMMEDIATE,
            "container.property.0"
        );

        SyncDeliveryResult result = new SyncDeliveryResult(change, SyncDeliveryStatus.UNSUPPORTED, "visual-only support is not gameplay support");

        assertEquals(SyncDeliveryStatus.UNSUPPORTED, result.status());
        assertTrue(result.reason().contains("visual-only"));
    }

    @Test
    void transportHandoffIsSeparatedFromClientObservation() {
        EncodedSyncChange change = new EncodedSyncChange(
            net.minecraft.resources.Identifier.fromNamespaceAndPath("hydraulic", "test"),
            "inventory.slot.0",
            EncodedSyncKind.INVENTORY_SLOT,
            0,
            null,
            0,
            "minecraft:stone",
            1,
            null,
            1,
            org.geysermc.hydraulic.compat.runtime.SyncPriority.IMMEDIATE,
            "inventory.slot.0"
        );

        SyncDeliveryResult delivery = new SyncDeliveryResult(change, SyncDeliveryStatus.SENT, "packet handed off to transport boundary", change.traceId());

        assertTrue(delivery.successfulHandoff());
        assertEquals(SyncDeliveryStatus.SENT, delivery.status());
        assertTrue(delivery.reason().contains("transport"));
    }
}
