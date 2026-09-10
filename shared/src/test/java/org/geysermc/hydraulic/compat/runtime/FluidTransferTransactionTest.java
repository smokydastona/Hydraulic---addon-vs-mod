package org.geysermc.hydraulic.compat.runtime;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FluidTransferTransactionTest {
    @Test
    void commitsValidatedFluidOperationsAndRecordsDirtyTankState() {
        Identifier machine = Identifier.fromNamespaceAndPath("hydraulic", "tank");
        TestTank tank = new TestTank();
        TransferBridgeFactory.FluidTransferBridge bridge = new TestFluidBridge(tank);
        DirtyStateTracker dirty = new DirtyStateTracker();

        TransferResult result = new FluidTransferTransaction(bridge)
            .add(new FluidTransferRequest(machine, TransferDirection.INSERT, new TransferBridgeFactory.FluidStackView("minecraft:water", 1000), 0, null))
            .execute(dirty);

        assertTrue(result.committed());
        assertEquals(1000, result.moved());
        assertEquals("minecraft:water", tank.fluid.fluidId());
        assertEquals(1000, tank.fluid.amount());
        assertEquals("fluid.tank.0", dirty.drain().changes().getFirst().field());
    }

    @Test
    void rejectsAnyIncompleteOperationBeforeMutation() {
        Identifier machine = Identifier.fromNamespaceAndPath("hydraulic", "tank");
        TestTank tank = new TestTank();
        TransferBridgeFactory.FluidTransferBridge bridge = new TestFluidBridge(tank);

        TransferResult result = new FluidTransferTransaction(bridge)
            .add(new FluidTransferRequest(machine, TransferDirection.INSERT, new TransferBridgeFactory.FluidStackView("minecraft:water", 1000), 0, null))
            .add(new FluidTransferRequest(machine, TransferDirection.INSERT, new TransferBridgeFactory.FluidStackView("minecraft:lava", 1000), 0, null))
            .execute();

        assertFalse(result.committed());
        assertEquals(0, tank.fluid.amount());
    }

    private static final class TestTank {
        private TransferBridgeFactory.FluidStackView fluid = new TransferBridgeFactory.FluidStackView("minecraft:empty", 0);
    }

    private static final class TestFluidBridge implements TransferBridgeFactory.FluidTransferBridge {
        private final TestTank tank;

        private TestFluidBridge(TestTank tank) {
            this.tank = tank;
        }

        @Override
        public boolean executable() {
            return true;
        }

        @Override
        public boolean canInsertFluid(Identifier blockIdentifier) {
            return true;
        }

        @Override
        public boolean canExtractFluid(Identifier blockIdentifier) {
            return true;
        }

        @Override
        public String tankType(Identifier blockIdentifier) {
            return "generic";
        }

        @Override
        public int tankCount(Identifier blockIdentifier) {
            return 1;
        }

        @Override
        public int tankCapacity(Identifier blockIdentifier, int tank) {
            return 1000;
        }

        @Override
        public TransferBridgeFactory.FluidStackView tankAt(Identifier blockIdentifier, int tank) {
            return this.tank.fluid;
        }

        @Override
        public int insertFluid(Identifier blockIdentifier, TransferBridgeFactory.FluidStackView fluid, int tank, String side, boolean simulate) {
            if (this.tank.fluid.amount() > 0 && !this.tank.fluid.fluidId().equals(fluid.fluidId())) {
                return 0;
            }
            int moved = Math.min(fluid.amount(), 1000 - this.tank.fluid.amount());
            if (!simulate && moved > 0) {
                this.tank.fluid = new TransferBridgeFactory.FluidStackView(fluid.fluidId(), this.tank.fluid.amount() + moved);
            }
            return moved;
        }

        @Override
        public int extractFluid(Identifier blockIdentifier, TransferBridgeFactory.FluidStackView fluid, int tank, String side, boolean simulate) {
            if (!this.tank.fluid.fluidId().equals(fluid.fluidId())) {
                return 0;
            }
            int moved = Math.min(fluid.amount(), this.tank.fluid.amount());
            if (!simulate && moved > 0) {
                this.tank.fluid = new TransferBridgeFactory.FluidStackView(
                    moved == this.tank.fluid.amount() ? "minecraft:empty" : fluid.fluidId(),
                    this.tank.fluid.amount() - moved
                );
            }
            return moved;
        }
    }
}
