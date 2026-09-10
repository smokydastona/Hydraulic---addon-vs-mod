package org.geysermc.hydraulic.compat.runtime;

import net.minecraft.resources.Identifier;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerId;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.cloudburstmc.protocol.bedrock.packet.ContainerSetDataPacket;
import org.cloudburstmc.protocol.bedrock.packet.InventorySlotPacket;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeyserSyncTransportTest {
    @Test
    void sendsInventorySlotPacketToGeyserSessionBoundary() {
        List<BedrockPacket> packets = new ArrayList<>();
        EncodedSyncChange change = inventoryChange(5, "minecraft:iron_ingot", 3);
        GeyserSyncTransport transport = new GeyserSyncTransport(
            null,
            packets::add,
            (session, itemId, count) -> ItemData.AIR,
            () -> 12
        );

        List<SyncDeliveryResult> results = transport.deliver(List.of(change));

        assertEquals(SyncDeliveryStatus.SENT, results.getFirst().status());
        assertTrue(results.getFirst().successfulHandoff());
        InventorySlotPacket packet = assertInstanceOf(InventorySlotPacket.class, packets.getFirst());
        assertEquals(12, packet.getContainerId());
        assertEquals(5, packet.getSlot());
    }

    @Test
    void fallsBackToInventoryContainerWhenNoOpenContainerExists() {
        List<BedrockPacket> packets = new ArrayList<>();
        GeyserSyncTransport transport = new GeyserSyncTransport(
            null,
            packets::add,
            (session, itemId, count) -> ItemData.AIR,
            () -> ContainerId.NONE
        );

        transport.deliver(List.of(inventoryChange(1, "minecraft:stone", 1)));

        InventorySlotPacket packet = assertInstanceOf(InventorySlotPacket.class, packets.getFirst());
        assertEquals(ContainerId.INVENTORY, packet.getContainerId());
    }

    @Test
    void reportsUnsupportedGenericStateWithoutSendingPacket() {
        List<BedrockPacket> packets = new ArrayList<>();
        EncodedSyncChange change = new EncodedSyncChange(
            Identifier.fromNamespaceAndPath("hydraulic", "machine"),
            "energy.amount",
            EncodedSyncKind.GENERIC_STATE,
            -1,
            null,
            0,
            null,
            0,
            0,
            100,
            SyncPriority.IMMEDIATE,
            "energy.amount"
        );
        GeyserSyncTransport transport = new GeyserSyncTransport(
            null,
            packets::add,
            (session, itemId, count) -> ItemData.AIR,
            () -> 1
        );

        List<SyncDeliveryResult> results = transport.deliver(List.of(change));

        assertEquals(SyncDeliveryStatus.UNSUPPORTED, results.getFirst().status());
        assertTrue(packets.isEmpty());
    }

    @Test
    void sendsContainerPropertyPacketToGeyserSessionBoundary() {
        List<BedrockPacket> packets = new ArrayList<>();
        EncodedSyncChange change = new EncodedSyncChange(
            Identifier.fromNamespaceAndPath("hydraulic", "machine"),
            "container.property.3",
            EncodedSyncKind.CONTAINER_PROPERTY,
            3,
            null,
            0,
            null,
            0,
            4,
            12,
            SyncPriority.IMMEDIATE,
            "container.property.3"
        );
        GeyserSyncTransport transport = new GeyserSyncTransport(
            null,
            packets::add,
            (session, itemId, count) -> ItemData.AIR,
            () -> 7
        );

        List<SyncDeliveryResult> results = transport.deliver(List.of(change));

        assertEquals(SyncDeliveryStatus.SENT, results.getFirst().status());
        ContainerSetDataPacket packet = assertInstanceOf(ContainerSetDataPacket.class, packets.getFirst());
        assertEquals(7, packet.getWindowId());
        assertEquals(3, packet.getProperty());
        assertEquals(12, packet.getValue());
    }

    @Test
    void reportsEncodingFailureForNonNumericContainerPropertyValue() {
        List<BedrockPacket> packets = new ArrayList<>();
        EncodedSyncChange change = new EncodedSyncChange(
            Identifier.fromNamespaceAndPath("hydraulic", "machine"),
            "container.property.3",
            EncodedSyncKind.CONTAINER_PROPERTY,
            3,
            null,
            0,
            null,
            0,
            4,
            "not-a-number",
            SyncPriority.IMMEDIATE,
            "container.property.3"
        );
        GeyserSyncTransport transport = new GeyserSyncTransport(
            null,
            packets::add,
            (session, itemId, count) -> ItemData.AIR,
            () -> 7
        );

        List<SyncDeliveryResult> results = transport.deliver(List.of(change));

        assertEquals(SyncDeliveryStatus.ENCODING_FAILED, results.getFirst().status());
        assertTrue(packets.isEmpty());
    }

    @Test
    void reportsTransportFailureWithoutThrowing() {
        EncodedSyncChange change = inventoryChange(2, "minecraft:stone", 1);
        GeyserSyncTransport transport = new GeyserSyncTransport(
            null,
            packet -> {
                throw new IllegalStateException("session closed");
            },
            (session, itemId, count) -> ItemData.AIR,
            () -> 1
        );

        List<SyncDeliveryResult> results = transport.deliver(List.of(change));

        assertEquals(SyncDeliveryStatus.TRANSPORT_FAILED, results.getFirst().status());
        assertEquals("session closed", results.getFirst().reason());
    }

    private static EncodedSyncChange inventoryChange(int slot, String itemId, int count) {
        return new EncodedSyncChange(
            Identifier.fromNamespaceAndPath("hydraulic", "machine"),
            "inventory.slot." + slot,
            EncodedSyncKind.INVENTORY_SLOT,
            slot,
            "minecraft:air",
            0,
            itemId,
            count,
            null,
            null,
            SyncPriority.IMMEDIATE,
            "inventory.slot." + slot
        );
    }
}
