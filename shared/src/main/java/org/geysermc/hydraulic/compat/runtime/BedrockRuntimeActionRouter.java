package org.geysermc.hydraulic.compat.runtime;

import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
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
        ServerPlayer player = player(session);
        if (player == null) {
            return new RuntimeActionResult(traceId, Status.TARGET_UNAVAILABLE, null, null, "No Java player for Bedrock session");
        }
        return route(
            packet,
            RuntimeTargetDiscovery.forGeyserSession(compatibilityRegistry.dispatchTable(), session),
            player.level().dimension().identifier().toString(),
            traceId
        );
    }

    @NotNull
    public static RuntimeActionResult executeBlockUse(
        @NotNull ServerPlayer player,
        @NotNull BlockPos blockPosition,
        @NotNull CompatibilityRegistry compatibilityRegistry,
        @NotNull RuntimeTraceId traceId
    ) {
        RuntimeTargetDiscovery.Position position = new RuntimeTargetDiscovery.Position(
            player.level().dimension().identifier().toString(),
            blockPosition.getX(),
            blockPosition.getY(),
            blockPosition.getZ()
        );
        RuntimeTargetDiscovery discovery = new RuntimeTargetDiscovery(
            compatibilityRegistry.dispatchTable(),
            new MinecraftRuntimeTargetSource(player.level())
        );
        RuntimeTargetDiscovery.Resolution resolution = discovery.discover(position, traceId);
        if (!resolution.resolved()) {
            return new RuntimeActionResult(
                traceId,
                resolution.status() == RuntimeTargetDiscovery.Status.TARGET_UNAVAILABLE ? Status.TARGET_UNAVAILABLE : Status.CAPABILITY_UNAVAILABLE,
                position,
                resolution.blockIdentifier(),
                resolution.reason()
            );
        }
        RuntimeActionResult routed = new RuntimeActionResult(traceId, Status.TARGET_RESOLVED, position, resolution.blockIdentifier(), null);
        var plan = compatibilityRegistry.dispatchTable().block(resolution.blockIdentifier());
        BlockUseActionPlan action = plan == null ? null : BlockUseActionPlan.from(plan.inventoryFacts());
        if (action == null) {
            return routed;
        }
        return executeItemAction(routed, discovery, action, new PlayerHeldItemAccess(player));
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

    @NotNull
    static RuntimeActionResult executeItemAction(
        @NotNull RuntimeActionResult routed,
        @NotNull RuntimeTargetDiscovery discovery,
        @NotNull BlockUseActionPlan action,
        @NotNull HeldItemAccess heldItemAccess
    ) {
        if (routed.status() != Status.TARGET_RESOLVED || routed.position() == null || routed.blockIdentifier() == null) {
            return routed;
        }
        TransferBridgeFactory.ItemStackView heldItem = heldItemAccess.heldItem();
        if (heldItem == null || heldItem.isEmpty() || heldItem.count() < action.count()) {
            return new RuntimeActionResult(routed.traceId(), Status.MUTATION_REJECTED, routed.position(), routed.blockIdentifier(), "Held item does not satisfy the compiled block-use action");
        }

        TransferResult transfer = discovery.transferItem(
            routed.position(),
            TransferDirection.INSERT,
            new TransferBridgeFactory.ItemStackView(heldItem.itemId(), action.count()),
            action.slot(),
            action.side(),
            null,
            routed.traceId()
        );
        if (!transfer.committed() || transfer.moved() != action.count()) {
            return new RuntimeActionResult(routed.traceId(), Status.MUTATION_REJECTED, routed.position(), routed.blockIdentifier(), transfer.failureReason());
        }

        heldItemAccess.consume(action.count());
        return new RuntimeActionResult(routed.traceId(), Status.MUTATED, routed.position(), routed.blockIdentifier(), null);
    }

    @Nullable
    private static ServerPlayer player(@NotNull GeyserSession session) {
        try {
            return HydraulicImpl.instance().server().getPlayerList().getPlayer(session.javaUuid());
        } catch (IllegalStateException ignored) {
            return null;
        }
    }

    public enum Status {
        IGNORED,
        TARGET_RESOLVED,
        TARGET_UNAVAILABLE,
        CAPABILITY_UNAVAILABLE,
        MUTATED,
        MUTATION_REJECTED
    }

    public record RuntimeActionResult(
        @NotNull RuntimeTraceId traceId,
        @NotNull Status status,
        @Nullable RuntimeTargetDiscovery.Position position,
        @Nullable Identifier blockIdentifier,
        @Nullable String reason
    ) {
    }

    interface HeldItemAccess {
        @Nullable TransferBridgeFactory.ItemStackView heldItem();

        void consume(int count);
    }

    private record PlayerHeldItemAccess(@NotNull ServerPlayer player) implements HeldItemAccess {
        @Override
        public TransferBridgeFactory.ItemStackView heldItem() {
            ItemStack stack = this.player.getMainHandItem();
            Identifier identifier = BuiltInRegistries.ITEM.getKey(stack.getItem());
            return stack.isEmpty() || identifier == null
                ? null
                : new TransferBridgeFactory.ItemStackView(identifier.toString(), stack.getCount());
        }

        @Override
        public void consume(int count) {
            if (!this.player.hasInfiniteMaterials()) {
                this.player.getMainHandItem().shrink(count);
            }
            this.player.containerMenu.broadcastChanges();
        }
    }
}
