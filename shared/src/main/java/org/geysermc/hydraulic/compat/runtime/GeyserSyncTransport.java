package org.geysermc.hydraulic.compat.runtime;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerId;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.cloudburstmc.protocol.bedrock.packet.InventorySlotPacket;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.item.ItemTranslator;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.IntSupplier;

public final class GeyserSyncTransport implements SyncTransport {
    private final GeyserSession session;
    private final PacketSender packetSender;
    private final BedrockItemEncoder itemEncoder;
    private final IntSupplier containerId;

    public GeyserSyncTransport(@NotNull GeyserSession session) {
        this(
            Objects.requireNonNull(session, "session"),
            session::sendUpstreamPacket,
            GeyserSyncTransport::translateItem,
            session::getPendingOrCurrentBedrockInventoryId
        );
    }

    GeyserSyncTransport(
        @Nullable GeyserSession session,
        @NotNull PacketSender packetSender,
        @NotNull BedrockItemEncoder itemEncoder,
        @NotNull IntSupplier containerId
    ) {
        this.session = session;
        this.packetSender = packetSender;
        this.itemEncoder = itemEncoder;
        this.containerId = containerId;
    }

    @Override
    @NotNull
    public List<SyncDeliveryResult> deliver(@NotNull List<EncodedSyncChange> changes) {
        List<SyncDeliveryResult> results = new ArrayList<>(changes.size());
        for (EncodedSyncChange change : changes) {
            results.add(deliver(change));
        }
        return List.copyOf(results);
    }

    @NotNull
    private SyncDeliveryResult deliver(@NotNull EncodedSyncChange change) {
        if (change.kind() != EncodedSyncKind.INVENTORY_SLOT) {
            return new SyncDeliveryResult(change, SyncDeliveryStatus.UNSUPPORTED, "No Geyser transport mapping exists for " + change.kind());
        }
        if (change.slot() < 0) {
            return new SyncDeliveryResult(change, SyncDeliveryStatus.ENCODING_FAILED, "Inventory synchronization requires a non-negative slot");
        }
        try {
            InventorySlotPacket packet = new InventorySlotPacket();
            packet.setContainerId(resolveContainerId());
            packet.setSlot(change.slot());
            packet.setItem(this.itemEncoder.encode(this.session, change.afterItemId(), change.afterCount()));
            this.packetSender.send(packet);
            return new SyncDeliveryResult(change, SyncDeliveryStatus.SENT, null);
        } catch (RuntimeException exception) {
            String reason = exception.getMessage();
            return new SyncDeliveryResult(
                change,
                SyncDeliveryStatus.TRANSPORT_FAILED,
                reason == null || reason.isBlank() ? exception.getClass().getSimpleName() : reason
            );
        }
    }

    private int resolveContainerId() {
        int id = this.containerId.getAsInt();
        return id == ContainerId.NONE ? ContainerId.INVENTORY : id;
    }

    @NotNull
    private static ItemData translateItem(@NotNull GeyserSession session, @Nullable String itemId, int count) {
        if (count <= 0 || itemId == null || itemId.isBlank() || "minecraft:air".equals(itemId)) {
            return ItemData.AIR;
        }
        Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(itemId));
        int javaId = BuiltInRegistries.ITEM.getId(item);
        return ItemTranslator.translateToBedrock(session, new ItemStack(javaId, count));
    }

    @FunctionalInterface
    interface PacketSender {
        void send(@NotNull BedrockPacket packet);
    }

    @FunctionalInterface
    interface BedrockItemEncoder {
        @NotNull ItemData encode(@Nullable GeyserSession session, @Nullable String itemId, int count);
    }
}
