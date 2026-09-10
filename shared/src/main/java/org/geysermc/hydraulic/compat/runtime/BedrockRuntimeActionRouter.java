package org.geysermc.hydraulic.compat.runtime;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.inventory.transaction.InventoryTransactionType;
import org.cloudburstmc.protocol.bedrock.packet.InventoryTransactionPacket;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.hydraulic.HydraulicImpl;
import org.geysermc.hydraulic.compat.CompatibilityRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BedrockRuntimeActionRouter {
    private static final Logger LOGGER = LoggerFactory.getLogger("HydraulicRuntimeActions");
    private static final int ITEM_USE_ON_BLOCK = 0;

    private BedrockRuntimeActionRouter() {
    }

    @NotNull
    public static RuntimeActionResult route(
        @NotNull GeyserSession session,
        @NotNull InventoryTransactionPacket packet,
        @NotNull CompatibilityRegistry compatibilityRegistry
    ) {
        RuntimeTraceId traceId = RuntimeTraceId.create();
        String level = level(session);
        if (level == null) {
            return new RuntimeActionResult(traceId, Status.TARGET_UNAVAILABLE, null, null, "No Java player level for Bedrock session");
        }
        return route(packet, RuntimeTargetDiscovery.forGeyserSession(compatibilityRegistry.dispatchTable(), session), level, traceId);
    }

    @NotNull
    static RuntimeActionResult route(
        @NotNull InventoryTransactionPacket packet,
        @NotNull RuntimeTargetDiscovery discovery,
        @NotNull String level,
        @NotNull RuntimeTraceId traceId
    ) {
        if (packet.getTransactionType() != InventoryTransactionType.ITEM_USE || packet.getActionType() != ITEM_USE_ON_BLOCK) {
            return new RuntimeActionResult(traceId, Status.IGNORED, null, null, "Inventory transaction is not a block item-use action");
        }
        Vector3i blockPosition = packet.getBlockPosition();
        if (blockPosition == null) {
            return new RuntimeActionResult(traceId, Status.TARGET_UNAVAILABLE, null, null, "Block item-use action has no block position");
        }

        RuntimeTargetDiscovery.Position position = new RuntimeTargetDiscovery.Position(
            level,
            blockPosition.getX(),
            blockPosition.getY(),
            blockPosition.getZ()
        );
        RuntimeTargetDiscovery.Resolution resolution = discovery.discover(position, traceId);
        RuntimeActionResult result = new RuntimeActionResult(
            traceId,
            switch (resolution.status()) {
                case RESOLVED -> Status.TARGET_RESOLVED;
                case TARGET_UNAVAILABLE -> Status.TARGET_UNAVAILABLE;
                case CAPABILITY_UNAVAILABLE -> Status.CAPABILITY_UNAVAILABLE;
            },
            position,
            resolution.blockIdentifier(),
            resolution.reason()
        );
        if (result.status() != Status.IGNORED) {
            LOGGER.debug("Runtime action {} routed with status {} at {} for {}", traceId.value(), result.status(), position.asKey(), result.blockIdentifier());
        }
        return result;
    }

    @Nullable
    private static String level(@NotNull GeyserSession session) {
        try {
            ServerPlayer player = HydraulicImpl.instance().server().getPlayerList().getPlayer(session.javaUuid());
            return player == null ? null : player.level().dimension().identifier().toString();
        } catch (IllegalStateException ignored) {
            return null;
        }
    }

    public enum Status {
        IGNORED,
        TARGET_RESOLVED,
        TARGET_UNAVAILABLE,
        CAPABILITY_UNAVAILABLE
    }

    public record RuntimeActionResult(
        @NotNull RuntimeTraceId traceId,
        @NotNull Status status,
        @Nullable RuntimeTargetDiscovery.Position position,
        @Nullable Identifier blockIdentifier,
        @Nullable String reason
    ) {
    }
}
