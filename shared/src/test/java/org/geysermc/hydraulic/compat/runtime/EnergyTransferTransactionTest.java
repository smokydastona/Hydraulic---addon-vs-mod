package org.geysermc.hydraulic.compat.runtime;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnergyTransferTransactionTest {
    @Test
    void commitsEnergyAndRecordsStateChange() {
        Identifier machine = Identifier.fromNamespaceAndPath("hydraulic", "battery");
        TestEnergyBridge bridge = new TestEnergyBridge(0, 1000);
        DirtyStateTracker dirty = new DirtyStateTracker();

        TransferResult result = new EnergyTransferTransaction(bridge)
            .add(new EnergyTransferRequest(machine, TransferDirection.INSERT, 250, null))
            .execute(dirty);

        assertTrue(result.committed());
        assertEquals(250, result.moved());
        assertEquals(250, bridge.energy);
        assertEquals("energy.amount", dirty.drain().changes().getFirst().field());
    }

    @Test
    void rejectsIncompleteEnergyBatchBeforeMutation() {
        Identifier machine = Identifier.fromNamespaceAndPath("hydraulic", "battery");
        TestEnergyBridge bridge = new TestEnergyBridge(0, 1000);

        TransferResult result = new EnergyTransferTransaction(bridge)
            .add(new EnergyTransferRequest(machine, TransferDirection.INSERT, 500, null))
            .add(new EnergyTransferRequest(machine, TransferDirection.INSERT, 600, null))
            .execute();

        assertFalse(result.committed());
        assertEquals(0, bridge.energy);
    }

    private static final class TestEnergyBridge implements TransferBridgeFactory.EnergyTransferBridge {
        private int energy;
        private final int capacity;

        private TestEnergyBridge(int energy, int capacity) {
            this.energy = energy;
            this.capacity = capacity;
        }

        @Override
        public boolean executable() {
            return true;
        }

        @Override
        public boolean canReceiveEnergy(Identifier blockIdentifier) {
            return true;
        }

        @Override
        public boolean canProvideEnergy(Identifier blockIdentifier) {
            return true;
        }

        @Override
        public String energyType(Identifier blockIdentifier) {
            return "forge_energy";
        }

        @Override
        public int getEnergyStored(Identifier blockIdentifier) {
            return this.energy;
        }

        @Override
        public int getMaxEnergy(Identifier blockIdentifier) {
            return this.capacity;
        }

        @Override
        public int receiveEnergy(Identifier blockIdentifier, int amount, String side, boolean simulate) {
            int moved = Math.min(amount, this.capacity - this.energy);
            if (!simulate) {
                this.energy += moved;
            }
            return moved;
        }

        @Override
        public int extractEnergy(Identifier blockIdentifier, int amount, String side, boolean simulate) {
            int moved = Math.min(amount, this.energy);
            if (!simulate) {
                this.energy -= moved;
            }
            return moved;
        }
    }
}
