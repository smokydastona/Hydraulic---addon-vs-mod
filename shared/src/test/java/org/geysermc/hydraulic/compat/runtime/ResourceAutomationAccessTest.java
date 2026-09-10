package org.geysermc.hydraulic.compat.runtime;

import net.minecraft.resources.Identifier;
import org.geysermc.hydraulic.compat.CompatibilityStatus;
import org.geysermc.hydraulic.compat.ir.CompiledCompatibilityPlan;
import org.geysermc.hydraulic.compat.model.Confidence;
import org.geysermc.hydraulic.compat.model.SupportLevel;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceAutomationAccessTest {
    private static final Identifier MACHINE = Identifier.fromNamespaceAndPath("hydraulic", "automation_machine");

    @Test
    void executesItemFluidAndEnergyRequestsThroughTransactions() {
        TestItemBridge items = new TestItemBridge();
        TestFluidBridge fluids = new TestFluidBridge();
        TestEnergyBridge energy = new TestEnergyBridge();
        MachineBridgeFactory.ResourceAutomationAccess automation = MachineBridgeFactory.createResourceAutomation(
            plan(),
            items,
            fluids,
            energy
        );
        DirtyStateTracker dirty = new DirtyStateTracker();

        assertNotNull(automation);
        assertTrue(automation.supportsSidedItemInsertion(MACHINE));
        assertTrue(automation.supportsSidedFluidInsertion(MACHINE));
        assertTrue(automation.supportsSidedEnergyReceive(MACHINE));
        assertEquals("tag", automation.filterType(MACHINE));

        TransferResult item = automation.transferItem(
            new TransferRequest(MACHINE, TransferDirection.INSERT, new TransferBridgeFactory.ItemStackView("minecraft:stone", 3), 0, "north"),
            dirty
        );
        TransferResult fluid = automation.transferFluid(
            new FluidTransferRequest(MACHINE, TransferDirection.INSERT, new TransferBridgeFactory.FluidStackView("minecraft:water", 250), 0, "east"),
            dirty
        );
        TransferResult energyResult = automation.transferEnergy(
            new EnergyTransferRequest(MACHINE, TransferDirection.INSERT, 100, "up"),
            dirty
        );

        assertTrue(item.committed());
        assertTrue(fluid.committed());
        assertTrue(energyResult.committed());
        assertEquals("north", items.lastSide);
        assertEquals("east", fluids.lastSide);
        assertEquals("up", energy.lastSide);
        assertEquals(3, items.stack.count());
        assertEquals(250, fluids.fluid.amount());
        assertEquals(100, energy.energy);
        assertEquals(3, dirty.drain().changes().size());
    }

    @Test
    void missingResourceBridgeRejectsThatRequestWithoutMutation() {
        MachineBridgeFactory.ResourceAutomationAccess automation = MachineBridgeFactory.createResourceAutomation(
            plan(),
            new TestItemBridge(),
            null,
            null
        );

        assertNotNull(automation);
        TransferResult result = automation.transferFluid(new FluidTransferRequest(
            MACHINE,
            TransferDirection.INSERT,
            new TransferBridgeFactory.FluidStackView("minecraft:water", 250),
            0,
            "east"
        ));

        assertFalse(result.committed());
        assertEquals(TransferBridgeFactory.OperationStatus.REJECTED, result.status());
    }

    private static CompiledCompatibilityPlan plan() {
        return new CompiledCompatibilityPlan(
            "hydraulic",
            "block",
            MACHINE.toString(),
            null,
            SupportLevel.ADAPTED,
            CompatibilityStatus.COMPLETE,
            100,
            new Confidence(1.0D, "test"),
            List.of(),
            List.of(),
            List.of(RuntimeBridgeKind.AUTOMATION_ACCESS),
            Map.of(
                "sided_insert", "true",
                "sided_extract", "true",
                "sided_insert_fluid", "true",
                "sided_extract_fluid", "true",
                "sided_receive_energy", "true",
                "sided_extract_energy", "true",
                "filtering", "tag"
            ),
            true,
            null,
            true,
            null,
            false,
            true,
            false,
            false,
            false,
            null,
            null,
            List.of(),
            List.of(),
            List.of(),
            null,
            false,
            false,
            SupportLevel.ADAPTED,
            "automation"
        );
    }

    private static final class TestItemBridge implements TransferBridgeFactory.ItemTransferBridge {
        private TransferBridgeFactory.ItemStackView stack = new TransferBridgeFactory.ItemStackView("minecraft:air", 0);
        private String lastSide;

        @Override
        public boolean executable() {
            return true;
        }

        @Override
        public boolean canInsert(Identifier blockIdentifier) {
            return true;
        }

        @Override
        public boolean canExtract(Identifier blockIdentifier) {
            return true;
        }

        @Override
        public String inventoryType(Identifier blockIdentifier) {
            return "automation";
        }

        @Override
        public TransferBridgeFactory.ItemStackView itemAt(Identifier blockIdentifier, int slot) {
            return this.stack;
        }

        @Override
        public int insert(Identifier blockIdentifier, TransferBridgeFactory.ItemStackView item, int slot, String side, boolean simulate) {
            this.lastSide = side;
            if (!this.stack.isEmpty() && !this.stack.matches(item)) {
                return 0;
            }
            int moved = Math.min(item.count(), 64 - this.stack.count());
            if (!simulate) {
                this.stack = new TransferBridgeFactory.ItemStackView(item.itemId(), this.stack.count() + moved);
            }
            return moved;
        }

        @Override
        public int extract(Identifier blockIdentifier, TransferBridgeFactory.ItemStackView item, int slot, String side, boolean simulate) {
            this.lastSide = side;
            if (!this.stack.matches(item)) {
                return 0;
            }
            int moved = Math.min(item.count(), this.stack.count());
            if (!simulate) {
                int remaining = this.stack.count() - moved;
                this.stack = remaining == 0
                    ? new TransferBridgeFactory.ItemStackView("minecraft:air", 0)
                    : new TransferBridgeFactory.ItemStackView(this.stack.itemId(), remaining);
            }
            return moved;
        }
    }

    private static final class TestFluidBridge implements TransferBridgeFactory.FluidTransferBridge {
        private TransferBridgeFactory.FluidStackView fluid = new TransferBridgeFactory.FluidStackView("minecraft:empty", 0);
        private String lastSide;

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
            return "automation";
        }

        @Override
        public int tankCapacity(Identifier blockIdentifier, int tank) {
            return 1000;
        }

        @Override
        public TransferBridgeFactory.FluidStackView tankAt(Identifier blockIdentifier, int tank) {
            return this.fluid;
        }

        @Override
        public int insertFluid(Identifier blockIdentifier, TransferBridgeFactory.FluidStackView fluid, int tank, String side, boolean simulate) {
            this.lastSide = side;
            if (this.fluid.amount() > 0 && !this.fluid.fluidId().equals(fluid.fluidId())) {
                return 0;
            }
            int moved = Math.min(fluid.amount(), 1000 - this.fluid.amount());
            if (!simulate) {
                this.fluid = new TransferBridgeFactory.FluidStackView(fluid.fluidId(), this.fluid.amount() + moved);
            }
            return moved;
        }

        @Override
        public int extractFluid(Identifier blockIdentifier, TransferBridgeFactory.FluidStackView fluid, int tank, String side, boolean simulate) {
            this.lastSide = side;
            if (!this.fluid.fluidId().equals(fluid.fluidId())) {
                return 0;
            }
            int moved = Math.min(fluid.amount(), this.fluid.amount());
            if (!simulate) {
                int remaining = this.fluid.amount() - moved;
                this.fluid = remaining == 0
                    ? new TransferBridgeFactory.FluidStackView("minecraft:empty", 0)
                    : new TransferBridgeFactory.FluidStackView(this.fluid.fluidId(), remaining);
            }
            return moved;
        }
    }

    private static final class TestEnergyBridge implements TransferBridgeFactory.EnergyTransferBridge {
        private int energy;
        private String lastSide;

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
            return 1000;
        }

        @Override
        public int receiveEnergy(Identifier blockIdentifier, int amount, String side, boolean simulate) {
            this.lastSide = side;
            int moved = Math.min(amount, 1000 - this.energy);
            if (!simulate) {
                this.energy += moved;
            }
            return moved;
        }

        @Override
        public int extractEnergy(Identifier blockIdentifier, int amount, String side, boolean simulate) {
            this.lastSide = side;
            int moved = Math.min(amount, this.energy);
            if (!simulate) {
                this.energy -= moved;
            }
            return moved;
        }
    }
}
