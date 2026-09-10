package org.geysermc.hydraulic.compat.runtime;

import net.minecraft.resources.Identifier;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.inventory.transaction.InventoryTransactionType;
import org.cloudburstmc.protocol.bedrock.packet.InventoryTransactionPacket;
import org.junit.jupiter.api.Test;

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

    private static final class NoopAutomationAccess implements MachineBridgeFactory.ResourceAutomationAccess {
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
