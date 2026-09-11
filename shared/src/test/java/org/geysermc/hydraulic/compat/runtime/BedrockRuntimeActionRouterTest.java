package org.geysermc.hydraulic.compat.runtime;

import net.minecraft.resources.Identifier;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.inventory.transaction.InventoryTransactionType;
import org.cloudburstmc.protocol.bedrock.packet.InventoryTransactionPacket;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BedrockRuntimeActionRouterTest {
    private static final Identifier MACHINE = Identifier.fromNamespaceAndPath("hydraulic", "runtime_machine");
    private static final String LEVEL = "minecraft:overworld";

    @Test
    void routesBlockUseActionIntoRuntimeTargetDiscovery() {
        RuntimeTraceId traceId = new RuntimeTraceId("bedrock-use-block");
        RuntimeTargetDiscovery discovery = discovery(new RuntimeTargetDiscovery.Target(MACHINE, new Object(), null, null), true);
        InventoryTransactionPacket packet = blockUsePacket();

        BedrockRuntimeActionRouter.RuntimeActionResult result = BedrockRuntimeActionRouter.route(packet, discovery, LEVEL, traceId);

        assertEquals(BedrockRuntimeActionRouter.Status.TARGET_RESOLVED, result.status());
        assertEquals(traceId, result.traceId());
        assertEquals(MACHINE, result.blockIdentifier());
        assertEquals("minecraft:overworld:4,70,9", result.position().asKey());
        assertNull(result.reason());
    }

    @Test
    void reportsMissingCapabilityForDiscoveredTarget() {
        RuntimeTraceId traceId = new RuntimeTraceId("bedrock-use-no-capability");
        RuntimeTargetDiscovery discovery = discovery(new RuntimeTargetDiscovery.Target(MACHINE, new Object(), null, null), false);

        BedrockRuntimeActionRouter.RuntimeActionResult result = BedrockRuntimeActionRouter.route(blockUsePacket(), discovery, LEVEL, traceId);

        assertEquals(BedrockRuntimeActionRouter.Status.CAPABILITY_UNAVAILABLE, result.status());
        assertEquals(traceId, result.traceId());
        assertEquals(MACHINE, result.blockIdentifier());
    }

    @Test
    void reportsMissingTargetForUnknownPosition() {
        RuntimeTraceId traceId = new RuntimeTraceId("bedrock-use-missing-target");
        RuntimeTargetDiscovery discovery = discovery(null, false);

        BedrockRuntimeActionRouter.RuntimeActionResult result = BedrockRuntimeActionRouter.route(blockUsePacket(), discovery, LEVEL, traceId);

        assertEquals(BedrockRuntimeActionRouter.Status.TARGET_UNAVAILABLE, result.status());
        assertEquals(traceId, result.traceId());
    }

    @Test
    void ignoresNonBlockUseInventoryTransactions() {
        RuntimeTraceId traceId = new RuntimeTraceId("bedrock-normal-inventory");
        InventoryTransactionPacket packet = new InventoryTransactionPacket();
        packet.setTransactionType(InventoryTransactionType.NORMAL);

        BedrockRuntimeActionRouter.RuntimeActionResult result = BedrockRuntimeActionRouter.route(packet, discovery(null, false), LEVEL, traceId);

        assertEquals(BedrockRuntimeActionRouter.Status.IGNORED, result.status());
        assertEquals(traceId, result.traceId());
    }

    @Test
    void reportsMissingBlockPosition() {
        RuntimeTraceId traceId = new RuntimeTraceId("bedrock-use-no-position");
        InventoryTransactionPacket packet = new InventoryTransactionPacket();
        packet.setTransactionType(InventoryTransactionType.ITEM_USE);
        packet.setActionType(0);

        BedrockRuntimeActionRouter.RuntimeActionResult result = BedrockRuntimeActionRouter.route(packet, discovery(null, false), LEVEL, traceId);

        assertEquals(BedrockRuntimeActionRouter.Status.TARGET_UNAVAILABLE, result.status());
        assertEquals(traceId, result.traceId());
    }

    @Test
    void executesCompiledHeldItemInsertionAndConsumesExactCount() {
        RuntimeTraceId traceId = new RuntimeTraceId("bedrock-owned-insert");
        RecordingAutomationAccess automation = new RecordingAutomationAccess(true);
        RuntimeTargetDiscovery discovery = discovery(new RuntimeTargetDiscovery.Target(MACHINE, new Object(), null, null), automation);
        MutableHeldItem held = new MutableHeldItem(new TransferBridgeFactory.ItemStackView("minecraft:cobblestone", 4));
        BedrockRuntimeActionRouter.RuntimeActionResult routed = BedrockRuntimeActionRouter.route(blockUsePacket(), discovery, LEVEL, traceId);

        BedrockRuntimeActionRouter.RuntimeActionResult result = BedrockRuntimeActionRouter.executeItemAction(
            routed,
            discovery,
            new BlockUseActionPlan(BlockUseActionPlan.Action.INSERT_HELD_ITEM, 2, 1, "up"),
            held
        );

        assertEquals(BedrockRuntimeActionRouter.Status.MUTATED, result.status());
        assertEquals(3, held.item.count());
        assertEquals(1, automation.lastRequest.item().count());
        assertEquals(2, automation.lastRequest.slot());
        assertEquals("up", automation.lastRequest.side());
    }

    @Test
    void rejectsMutationWithoutConsumingHeldItem() {
        RuntimeTraceId traceId = new RuntimeTraceId("bedrock-rejected-insert");
        RecordingAutomationAccess automation = new RecordingAutomationAccess(false);
        RuntimeTargetDiscovery discovery = discovery(new RuntimeTargetDiscovery.Target(MACHINE, new Object(), null, null), automation);
        MutableHeldItem held = new MutableHeldItem(new TransferBridgeFactory.ItemStackView("minecraft:cobblestone", 1));
        BedrockRuntimeActionRouter.RuntimeActionResult routed = BedrockRuntimeActionRouter.route(blockUsePacket(), discovery, LEVEL, traceId);

        BedrockRuntimeActionRouter.RuntimeActionResult result = BedrockRuntimeActionRouter.executeItemAction(
            routed,
            discovery,
            new BlockUseActionPlan(BlockUseActionPlan.Action.INSERT_HELD_ITEM, 0, 1, null),
            held
        );

        assertEquals(BedrockRuntimeActionRouter.Status.MUTATION_REJECTED, result.status());
        assertEquals(1, held.item.count());
    }

    @Test
    void rejectsInsufficientHeldItemBeforeTargetMutation() {
        RuntimeTraceId traceId = new RuntimeTraceId("bedrock-insufficient-held-item");
        RecordingAutomationAccess automation = new RecordingAutomationAccess(true);
        RuntimeTargetDiscovery discovery = discovery(new RuntimeTargetDiscovery.Target(MACHINE, new Object(), null, null), automation);
        MutableHeldItem held = new MutableHeldItem(new TransferBridgeFactory.ItemStackView("minecraft:cobblestone", 1));
        BedrockRuntimeActionRouter.RuntimeActionResult routed = BedrockRuntimeActionRouter.route(blockUsePacket(), discovery, LEVEL, traceId);

        BedrockRuntimeActionRouter.RuntimeActionResult result = BedrockRuntimeActionRouter.executeItemAction(
            routed,
            discovery,
            new BlockUseActionPlan(BlockUseActionPlan.Action.INSERT_HELD_ITEM, 0, 2, null),
            held
        );

        assertEquals(BedrockRuntimeActionRouter.Status.MUTATION_REJECTED, result.status());
        assertNull(automation.lastRequest);
        assertEquals(1, held.item.count());
    }

    private static InventoryTransactionPacket blockUsePacket() {
        InventoryTransactionPacket packet = new InventoryTransactionPacket();
        packet.setTransactionType(InventoryTransactionType.ITEM_USE);
        packet.setActionType(0);
        packet.setBlockPosition(Vector3i.from(4, 70, 9));
        return packet;
    }

    private static RuntimeTargetDiscovery discovery(RuntimeTargetDiscovery.Target target, boolean resolvable) {
        return new RuntimeTargetDiscovery(
            ignored -> resolvable ? new NoopAutomationAccess() : null,
            position -> target
        );
    }

    private static RuntimeTargetDiscovery discovery(RuntimeTargetDiscovery.Target target, MachineBridgeFactory.ResourceAutomationAccess automation) {
        return new RuntimeTargetDiscovery(ignored -> automation, position -> target);
    }

    private static final class MutableHeldItem implements BedrockRuntimeActionRouter.HeldItemAccess {
        private TransferBridgeFactory.ItemStackView item;

        private MutableHeldItem(TransferBridgeFactory.ItemStackView item) {
            this.item = item;
        }

        @Override
        public TransferBridgeFactory.ItemStackView heldItem() {
            return this.item;
        }

        @Override
        public void consume(int count) {
            this.item = new TransferBridgeFactory.ItemStackView(this.item.itemId(), this.item.count() - count);
        }
    }

    private static final class RecordingAutomationAccess extends NoopAutomationAccess {
        private final boolean commit;
        private TransferRequest lastRequest;

        private RecordingAutomationAccess(boolean commit) {
            this.commit = commit;
        }

        @Override
        public TransferResult transferItem(TransferRequest request) {
            this.lastRequest = request;
            if (!this.commit) {
                return TransferResult.rejected("target rejected insertion");
            }
            StateChangeSet changes = new StateChangeSet(List.of(new StateChangeSet.FieldChange(
                request.blockIdentifier(),
                "inventory.slot." + request.slot(),
                new TransferBridgeFactory.ItemStackView("minecraft:air", 0),
                request.item()
            )));
            return new TransferResult(true, request.item().count(), TransferBridgeFactory.OperationStatus.COMPLETED, null, List.of(), changes);
        }
    }

    private static class NoopAutomationAccess implements MachineBridgeFactory.ResourceAutomationAccess {
        @Override
        public boolean supportsSidedItemInsertion(Identifier blockIdentifier) {
            return true;
        }

        @Override
        public boolean supportsSidedItemExtraction(Identifier blockIdentifier) {
            return true;
        }

        @Override
        public boolean supportsSidedFluidInsertion(Identifier blockIdentifier) {
            return false;
        }

        @Override
        public boolean supportsSidedFluidExtraction(Identifier blockIdentifier) {
            return false;
        }

        @Override
        public boolean supportsSidedEnergyReceive(Identifier blockIdentifier) {
            return false;
        }

        @Override
        public boolean supportsSidedEnergyExtraction(Identifier blockIdentifier) {
            return false;
        }

        @Override
        public String filterType(Identifier blockIdentifier) {
            return null;
        }

        @Override
        public TransferResult transferItem(TransferRequest request) {
            return TransferResult.rejected("not used by action routing test");
        }

        @Override
        public TransferResult transferFluid(FluidTransferRequest request) {
            return TransferResult.rejected("not used by action routing test");
        }

        @Override
        public TransferResult transferEnergy(EnergyTransferRequest request) {
            return TransferResult.rejected("not used by action routing test");
        }
    }
}
