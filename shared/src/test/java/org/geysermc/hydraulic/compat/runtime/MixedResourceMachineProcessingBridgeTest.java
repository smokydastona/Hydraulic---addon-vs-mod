package org.geysermc.hydraulic.compat.runtime;

import net.minecraft.resources.Identifier;
import org.geysermc.hydraulic.compat.CompatibilityStatus;
import org.geysermc.hydraulic.compat.ir.CompiledCompatibilityPlan;
import org.geysermc.hydraulic.compat.model.Confidence;
import org.geysermc.hydraulic.compat.model.SupportLevel;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MixedResourceMachineProcessingBridgeTest {
    private static final Identifier MACHINE = Identifier.fromNamespaceAndPath("hydraulic", "mixed_machine");

    @Test
    void factoryRequiresEveryRecipeResourceBridge() {
        TestItemBridge items = TestItemBridge.ready();
        TestFluidBridge fluids = TestFluidBridge.ready();
        TestEnergyBridge energy = new TestEnergyBridge(100, 1000);
        CompiledCompatibilityPlan plan = plan();

        assertNotNull(MachineBridgeFactory.createMixedProcessing(plan, items, fluids, energy, List.of(recipe())));
        assertNull(MachineBridgeFactory.createMixedProcessing(plan, items, null, energy, List.of(recipe())));
        assertNull(MachineBridgeFactory.createMixedProcessing(plan, items, fluids, null, List.of(recipe())));
        assertNull(MachineBridgeFactory.createMixedProcessing(plan, items, fluids, energy, List.of()));
    }

    @Test
    void processesMixedResourcesThroughOneTransactionAndSyncPipeline() {
        TestItemBridge items = TestItemBridge.ready();
        TestFluidBridge fluids = TestFluidBridge.ready();
        TestEnergyBridge energy = new TestEnergyBridge(100, 1000);
        DirtyStateTracker dirty = new DirtyStateTracker();
        MixedResourceMachineProcessingBridge processing = MachineBridgeFactory.createMixedProcessing(
            plan(),
            items,
            fluids,
            energy,
            List.of(recipe())
        );
        List<EncodedSyncChange> delivered = new ArrayList<>();
        SyncDispatcher dispatcher = new SyncDispatcher(
            dirty,
            new SyncPlanner(),
            new SyncEncoder(),
            changes -> {
                delivered.addAll(changes);
                return changes.stream()
                    .map(change -> new SyncDeliveryResult(change, SyncDeliveryStatus.SENT, null))
                    .toList();
            }
        );

        assertNotNull(processing);
        assertFalse(processing.active());
        assertEquals(1, processing.duration(MACHINE));
        assertTrue(processing.tick(MACHINE, dirty));
        assertTrue(processing.active());
        assertEquals(1, processing.progress());
        assertTrue(processing.tick(MACHINE, dirty));

        assertFalse(processing.active());
        assertEquals(1, items.slot(0).count());
        assertEquals(1, items.slot(1).count());
        assertEquals("minecraft:iron_ingot", items.slot(2).itemId());
        assertEquals("minecraft:copper_ingot", items.slot(3).itemId());
        assertEquals(500, fluids.tank(0).amount());
        assertEquals(250, fluids.tank(1).amount());
        assertEquals(50, energy.energy);
        assertEquals(7, dispatcher.flush().size());
        assertEquals(7, delivered.size());
    }

    @Test
    void compilesMixedResourceRecipesFromCompatibilityPlanFacts() {
        TestItemBridge items = TestItemBridge.ready();
        TestFluidBridge fluids = TestFluidBridge.ready();
        TestEnergyBridge energy = new TestEnergyBridge(100, 1000);
        DirtyStateTracker dirty = new DirtyStateTracker();
        MixedResourceMachineProcessingBridge processing = MachineBridgeFactory.createMixedProcessing(
            plan(recipeFacts()),
            items,
            fluids,
            energy
        );

        assertNotNull(processing);
        assertEquals(1, processing.duration(MACHINE));
        assertTrue(processing.tick(MACHINE));
        assertTrue(processing.tick(MACHINE, dirty));

        assertEquals(1, items.slot(0).count());
        assertEquals(1, items.slot(1).count());
        assertEquals("minecraft:iron_ingot", items.slot(2).itemId());
        assertEquals("minecraft:copper_ingot", items.slot(3).itemId());
        assertEquals(500, fluids.tank(0).amount());
        assertEquals(250, fluids.tank(1).amount());
        assertEquals(50, energy.energy);
        assertEquals(7, dirty.drain().changes().size());
    }

    @Test
    void malformedMixedResourceRecipeFactsFailClosed() {
        Map<String, String> malformed = new java.util.LinkedHashMap<>(recipeFacts());
        malformed.remove("machine.processing.recipe.0.fluid_output.0.tank");

        assertNull(MachineBridgeFactory.createMixedProcessing(
            plan(malformed),
            TestItemBridge.ready(),
            TestFluidBridge.ready(),
            new TestEnergyBridge(100, 1000)
        ));
        assertFalse(MachineBridgeFactory.hasExecutableProcessingContract(withGlobalSlots(malformed)));
    }

    @Test
    void legacyItemRecipeUsesGlobalMachineSlots() {
        Map<String, String> facts = Map.ofEntries(
            Map.entry("has_processing", "true"),
            Map.entry("has_inventory", "true"),
            Map.entry("machine.input_slot", "0"),
            Map.entry("machine.output_slot", "1"),
            Map.entry("machine.processing.recipe.0.input", "minecraft:stone"),
            Map.entry("machine.processing.recipe.0.input_count", "1"),
            Map.entry("machine.processing.recipe.0.output", "minecraft:iron_ingot"),
            Map.entry("machine.processing.recipe.0.output_count", "1"),
            Map.entry("machine.processing.recipe.0.fluid_input.0.fluid", "minecraft:water"),
            Map.entry("machine.processing.recipe.0.fluid_input.0.amount", "500"),
            Map.entry("machine.processing.recipe.0.fluid_input.0.tank", "0"),
            Map.entry("machine.processing.recipe.0.energy_input", "50"),
            Map.entry("machine.processing.recipe.0.duration", "1")
        );
        TestItemBridge items = new TestItemBridge(
            new TransferBridgeFactory.ItemStackView("minecraft:stone", 1),
            new TransferBridgeFactory.ItemStackView("minecraft:air", 0)
        );

        assertNotNull(MachineBridgeFactory.createMixedProcessing(
            plan(facts),
            items,
            TestFluidBridge.ready(),
            new TestEnergyBridge(100, 1000)
        ));
    }

    private static MixedResourceMachineProcessingBridge.MixedMachineRecipe recipe() {
        return new MixedResourceMachineProcessingBridge.MixedMachineRecipe(
            List.of(
                new MixedResourceMachineProcessingBridge.ItemSlotStack(0, new TransferBridgeFactory.ItemStackView("minecraft:stone", 1), null),
                new MixedResourceMachineProcessingBridge.ItemSlotStack(1, new TransferBridgeFactory.ItemStackView("minecraft:coal", 1), null)
            ),
            List.of(new MixedResourceMachineProcessingBridge.FluidTankStack(0, new TransferBridgeFactory.FluidStackView("minecraft:water", 500), null)),
            50,
            List.of(
                new MixedResourceMachineProcessingBridge.ItemSlotStack(2, new TransferBridgeFactory.ItemStackView("minecraft:iron_ingot", 1), null),
                new MixedResourceMachineProcessingBridge.ItemSlotStack(3, new TransferBridgeFactory.ItemStackView("minecraft:copper_ingot", 1), null)
            ),
            List.of(new MixedResourceMachineProcessingBridge.FluidTankStack(1, new TransferBridgeFactory.FluidStackView("minecraft:steam", 250), null)),
            0,
            null,
            1
        );
    }

    private static CompiledCompatibilityPlan plan() {
        return plan(Map.of("has_processing", "true", "has_inventory", "true"));
    }

    private static CompiledCompatibilityPlan plan(Map<String, String> facts) {
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
            List.of(RuntimeBridgeKind.MACHINE_BEHAVIOR, RuntimeBridgeKind.MACHINE_INVENTORY),
            facts,
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
            "mixed_machine",
            List.of()
        );
    }

    private static Map<String, String> recipeFacts() {
        return Map.ofEntries(
            Map.entry("has_processing", "true"),
            Map.entry("has_inventory", "true"),
            Map.entry("machine.processing.recipe.0.item_input.0.item", "minecraft:stone"),
            Map.entry("machine.processing.recipe.0.item_input.0.count", "1"),
            Map.entry("machine.processing.recipe.0.item_input.0.slot", "0"),
            Map.entry("machine.processing.recipe.0.item_input.1.item", "minecraft:coal"),
            Map.entry("machine.processing.recipe.0.item_input.1.count", "1"),
            Map.entry("machine.processing.recipe.0.item_input.1.slot", "1"),
            Map.entry("machine.processing.recipe.0.fluid_input.0.fluid", "minecraft:water"),
            Map.entry("machine.processing.recipe.0.fluid_input.0.amount", "500"),
            Map.entry("machine.processing.recipe.0.fluid_input.0.tank", "0"),
            Map.entry("machine.processing.recipe.0.energy_input", "50"),
            Map.entry("machine.processing.recipe.0.item_output.0.item", "minecraft:iron_ingot"),
            Map.entry("machine.processing.recipe.0.item_output.0.count", "1"),
            Map.entry("machine.processing.recipe.0.item_output.0.slot", "2"),
            Map.entry("machine.processing.recipe.0.item_output.1.item", "minecraft:copper_ingot"),
            Map.entry("machine.processing.recipe.0.item_output.1.count", "1"),
            Map.entry("machine.processing.recipe.0.item_output.1.slot", "3"),
            Map.entry("machine.processing.recipe.0.fluid_output.0.fluid", "minecraft:steam"),
            Map.entry("machine.processing.recipe.0.fluid_output.0.amount", "250"),
            Map.entry("machine.processing.recipe.0.fluid_output.0.tank", "1"),
            Map.entry("machine.processing.recipe.0.duration", "1")
        );
    }

    private static Map<String, String> withGlobalSlots(Map<String, String> facts) {
        Map<String, String> copy = new java.util.LinkedHashMap<>(facts);
        copy.put("machine.input_slot", "0");
        copy.put("machine.output_slot", "2");
        copy.put("can_insert_fluid", "true");
        copy.put("can_extract_fluid", "true");
        copy.put("can_receive_energy", "true");
        return Map.copyOf(copy);
    }

    private static final class TestItemBridge implements TransferBridgeFactory.ItemTransferBridge {
        private final TransferBridgeFactory.ItemStackView[] slots;

        private TestItemBridge(TransferBridgeFactory.ItemStackView... slots) {
            this.slots = slots;
        }

        private static TestItemBridge ready() {
            return new TestItemBridge(
                new TransferBridgeFactory.ItemStackView("minecraft:stone", 2),
                new TransferBridgeFactory.ItemStackView("minecraft:coal", 2),
                new TransferBridgeFactory.ItemStackView("minecraft:air", 0),
                new TransferBridgeFactory.ItemStackView("minecraft:air", 0)
            );
        }

        private TransferBridgeFactory.ItemStackView slot(int index) {
            return this.slots[index];
        }

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
            return "machine";
        }

        @Override
        public TransferBridgeFactory.ItemStackView itemAt(Identifier blockIdentifier, int slot) {
            return this.slots[slot];
        }

        @Override
        public int insert(Identifier blockIdentifier, TransferBridgeFactory.ItemStackView item, int slot, String side, boolean simulate) {
            TransferBridgeFactory.ItemStackView current = this.slots[slot];
            if (!current.isEmpty() && !current.matches(item)) {
                return 0;
            }
            int moved = Math.min(item.count(), 64 - current.count());
            if (!simulate && moved > 0) {
                this.slots[slot] = new TransferBridgeFactory.ItemStackView(item.itemId(), current.count() + moved);
            }
            return moved;
        }

        @Override
        public int extract(Identifier blockIdentifier, TransferBridgeFactory.ItemStackView item, int slot, String side, boolean simulate) {
            TransferBridgeFactory.ItemStackView current = this.slots[slot];
            if (!current.matches(item)) {
                return 0;
            }
            int moved = Math.min(item.count(), current.count());
            if (!simulate && moved > 0) {
                int remaining = current.count() - moved;
                this.slots[slot] = remaining == 0
                    ? new TransferBridgeFactory.ItemStackView("minecraft:air", 0)
                    : new TransferBridgeFactory.ItemStackView(current.itemId(), remaining);
            }
            return moved;
        }
    }

    private static final class TestFluidBridge implements TransferBridgeFactory.FluidTransferBridge {
        private final TransferBridgeFactory.FluidStackView[] tanks;

        private TestFluidBridge(TransferBridgeFactory.FluidStackView... tanks) {
            this.tanks = tanks;
        }

        private static TestFluidBridge ready() {
            return new TestFluidBridge(
                new TransferBridgeFactory.FluidStackView("minecraft:water", 1000),
                new TransferBridgeFactory.FluidStackView("minecraft:empty", 0)
            );
        }

        private TransferBridgeFactory.FluidStackView tank(int index) {
            return this.tanks[index];
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
            return "machine";
        }

        @Override
        public int tankCapacity(Identifier blockIdentifier, int tank) {
            return 1000;
        }

        @Override
        public TransferBridgeFactory.FluidStackView tankAt(Identifier blockIdentifier, int tank) {
            return this.tanks[tank];
        }

        @Override
        public int insertFluid(Identifier blockIdentifier, TransferBridgeFactory.FluidStackView fluid, int tank, String side, boolean simulate) {
            TransferBridgeFactory.FluidStackView current = this.tanks[tank];
            if (current.amount() > 0 && !current.fluidId().equals(fluid.fluidId())) {
                return 0;
            }
            int moved = Math.min(fluid.amount(), 1000 - current.amount());
            if (!simulate && moved > 0) {
                this.tanks[tank] = new TransferBridgeFactory.FluidStackView(fluid.fluidId(), current.amount() + moved);
            }
            return moved;
        }

        @Override
        public int extractFluid(Identifier blockIdentifier, TransferBridgeFactory.FluidStackView fluid, int tank, String side, boolean simulate) {
            TransferBridgeFactory.FluidStackView current = this.tanks[tank];
            if (!current.fluidId().equals(fluid.fluidId())) {
                return 0;
            }
            int moved = Math.min(fluid.amount(), current.amount());
            if (!simulate && moved > 0) {
                int remaining = current.amount() - moved;
                this.tanks[tank] = remaining == 0
                    ? new TransferBridgeFactory.FluidStackView("minecraft:empty", 0)
                    : new TransferBridgeFactory.FluidStackView(current.fluidId(), remaining);
            }
            return moved;
        }
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
