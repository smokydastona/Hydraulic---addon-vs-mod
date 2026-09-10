package org.geysermc.hydraulic.compat.runtime;

import net.minecraft.resources.Identifier;
import org.geysermc.hydraulic.compat.CompatibilityRegistry;
import org.geysermc.hydraulic.compat.ir.CompiledCompatibilityPlan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;

/**
 * Factory for creating machine behavior and inventory bridges based on compiled compatibility plans.
 * This factory consumes typed RuntimeBridgeKind categories instead of freeform requirement strings.
 */
public final class MachineBridgeFactory {
    private MachineBridgeFactory() {
    }

    /**
     * Checks if a machine behavior bridge can be created for the given block identifier.
     */
    public static boolean supportsMachineBehavior(@NotNull Identifier blockIdentifier, @NotNull CompatibilityRegistry compatibilityRegistry) {
        CompiledCompatibilityPlan plan = compatibilityRegistry.dispatchTable().block(blockIdentifier);
        return BridgeAdapterSupport.supportsMachineBehavior(plan);
    }

    /**
     * Checks if a machine inventory bridge can be created for the given block identifier.
     */
    public static boolean supportsMachineInventory(@NotNull Identifier blockIdentifier, @NotNull CompatibilityRegistry compatibilityRegistry) {
        CompiledCompatibilityPlan plan = compatibilityRegistry.dispatchTable().block(blockIdentifier);
        return BridgeAdapterSupport.supportsMachineInventory(plan);
    }

    /**
     * Creates a machine behavior bridge if supported by the compiled plan.
     */
    @Nullable
    public static MachineBehaviorBridge createMachineBehavior(@NotNull Identifier blockIdentifier, @NotNull CompatibilityRegistry compatibilityRegistry) {
        CompiledCompatibilityPlan plan = compatibilityRegistry.dispatchTable().block(blockIdentifier);
        return createMachineBehavior(plan);
    }

    @Nullable
    public static MachineBehaviorBridge createMachineBehavior(@Nullable CompiledCompatibilityPlan plan) {
        if (!BridgeAdapterSupport.supportsMachineBehavior(plan)) {
            return null;
        }
        return new MetadataBackedMachineBehaviorBridge(plan);
    }

    /**
     * Creates a machine inventory bridge if supported by the compiled plan.
     */
    @Nullable
    public static MachineInventoryBridge createMachineInventory(@NotNull Identifier blockIdentifier, @NotNull CompatibilityRegistry compatibilityRegistry) {
        CompiledCompatibilityPlan plan = compatibilityRegistry.dispatchTable().block(blockIdentifier);
        return createMachineInventory(plan);
    }

    @Nullable
    public static MachineInventoryBridge createMachineInventory(@Nullable CompiledCompatibilityPlan plan) {
        if (!BridgeAdapterSupport.supportsMachineInventory(plan)) {
            return null;
        }
        return new MetadataBackedMachineInventoryBridge(plan);
    }

    @Nullable
    public static InventoryAccess createInventoryAccess(
        @Nullable CompiledCompatibilityPlan plan,
        @Nullable TransferBridgeFactory.ItemTransferBridge inventory
    ) {
        if (!BridgeAdapterSupport.supportsMachineInventory(plan)
            || inventory == null
            || !inventory.executable()) {
            return null;
        }
        return new RuntimeInventoryAccess(plan, inventory);
    }

    @Nullable
    public static AutomationAccess createAutomation(
        @Nullable CompiledCompatibilityPlan plan,
        @Nullable TransferBridgeFactory.ItemTransferBridge inventory
    ) {
        if (!BridgeAdapterSupport.supportsAutomation(plan)
            || inventory == null
            || !inventory.executable()) {
            return null;
        }
        return new RuntimeAutomationAccess(plan, inventory);
    }

    @Nullable
    public static MachineProcessingBridge createProcessing(
        @Nullable CompiledCompatibilityPlan plan,
        @Nullable TransferBridgeFactory.ItemTransferBridge inventory,
        int inputSlot,
        int outputSlot,
        @NotNull List<MachineProcessingBridge.MachineRecipe> recipes
    ) {
        if (!BridgeAdapterSupport.supportsMachineBehavior(plan)
            || !BridgeAdapterSupport.supportsMachineInventory(plan)
            || inventory == null
            || !inventory.executable()
            || inputSlot < 0
            || outputSlot < 0
            || recipes.isEmpty()) {
            return null;
        }
        return new MachineProcessingBridge(plan, inventory, inputSlot, outputSlot, recipes);
    }

    @Nullable
    public static MixedResourceMachineProcessingBridge createMixedProcessing(
        @Nullable CompiledCompatibilityPlan plan,
        @Nullable TransferBridgeFactory.ItemTransferBridge items,
        @Nullable TransferBridgeFactory.FluidTransferBridge fluids,
        @Nullable TransferBridgeFactory.EnergyTransferBridge energy,
        @NotNull List<MixedResourceMachineProcessingBridge.MixedMachineRecipe> recipes
    ) {
        if (!BridgeAdapterSupport.supportsMachineBehavior(plan)
            || !BridgeAdapterSupport.supportsMachineInventory(plan)
            || recipes.isEmpty()
            || !supportsRecipeResources(items, fluids, energy, recipes)) {
            return null;
        }
        return new MixedResourceMachineProcessingBridge(plan, items, fluids, energy, recipes);
    }

    @Nullable
    public static MachineProcessingBridge createProcessing(
        @Nullable CompiledCompatibilityPlan plan,
        @Nullable TransferBridgeFactory.ItemTransferBridge inventory
    ) {
        if (plan == null) {
            return null;
        }
        Integer inputSlot = integerFact(plan.inventoryFacts(), "machine.input_slot");
        Integer outputSlot = integerFact(plan.inventoryFacts(), "machine.output_slot");
        if (inputSlot == null) {
            inputSlot = firstSlot(plan.slotRoles().get(SlotRole.INPUT));
        }
        if (outputSlot == null) {
            outputSlot = firstSlot(plan.slotRoles().get(SlotRole.OUTPUT));
        }
        List<MachineProcessingBridge.MachineRecipe> recipes = compileRecipes(plan.inventoryFacts());
        if (inputSlot == null || outputSlot == null || recipes.isEmpty()) {
            return null;
        }
        return createProcessing(plan, inventory, inputSlot, outputSlot, recipes);
    }

    @Nullable
    private static Integer firstSlot(@Nullable List<Integer> slots) {
        return slots == null || slots.isEmpty() ? null : slots.getFirst();
    }

    private static boolean supportsRecipeResources(
        @Nullable TransferBridgeFactory.ItemTransferBridge items,
        @Nullable TransferBridgeFactory.FluidTransferBridge fluids,
        @Nullable TransferBridgeFactory.EnergyTransferBridge energy,
        @NotNull List<MixedResourceMachineProcessingBridge.MixedMachineRecipe> recipes
    ) {
        boolean needsItems = false;
        boolean needsFluids = false;
        boolean needsEnergy = false;
        for (MixedResourceMachineProcessingBridge.MixedMachineRecipe recipe : recipes) {
            needsItems |= !recipe.itemInputs().isEmpty() || !recipe.itemOutputs().isEmpty();
            needsFluids |= !recipe.fluidInputs().isEmpty() || !recipe.fluidOutputs().isEmpty();
            needsEnergy |= recipe.energyInput() > 0 || recipe.energyOutput() > 0;
        }
        return (!needsItems || items != null && items.executable())
            && (!needsFluids || fluids != null && fluids.executable())
            && (!needsEnergy || energy != null && energy.executable());
    }

    @Nullable
    private static Integer integerFact(@NotNull Map<String, String> facts, @NotNull String key) {
        String value = facts.get(key);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            int parsed = Integer.parseInt(value);
            return parsed < 0 ? null : parsed;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    @NotNull
    private static List<MachineProcessingBridge.MachineRecipe> compileRecipes(@NotNull Map<String, String> facts) {
        List<MachineProcessingBridge.MachineRecipe> recipes = new ArrayList<>();
        for (int index = 0; ; index++) {
            String prefix = "machine.processing.recipe." + index + ".";
            String input = facts.get(prefix + "input");
            if (input == null) {
                break;
            }
            String output = facts.get(prefix + "output");
            Integer inputCount = integerFact(facts, prefix + "input_count");
            Integer outputCount = integerFact(facts, prefix + "output_count");
            Integer duration = integerFact(facts, prefix + "duration");
            if (output == null || inputCount == null || inputCount == 0 || outputCount == null || outputCount == 0 || duration == null || duration == 0) {
                return List.of();
            }
            try {
                recipes.add(new MachineProcessingBridge.MachineRecipe(
                    new TransferBridgeFactory.ItemStackView(input, inputCount),
                    new TransferBridgeFactory.ItemStackView(output, outputCount),
                    duration
                ));
            } catch (IllegalArgumentException ignored) {
                return List.of();
            }
        }
        return List.copyOf(recipes);
    }

    /**
     * Interface for machine behavior bridges.
     */
    public interface MachineBehaviorBridge {
        boolean hasProcessingBehavior(@NotNull Identifier blockIdentifier);
        @Nullable String processingType(@NotNull Identifier blockIdentifier);
        @Nullable String redstoneControl(@NotNull Identifier blockIdentifier);
    }

    /**
     * Interface for machine inventory bridges.
     */
    public interface MachineInventoryBridge {
        boolean hasInventory(@NotNull Identifier blockIdentifier);
        @Nullable String inventoryLayout(@NotNull Identifier blockIdentifier);
        @Nullable String slotSemantics(@NotNull Identifier blockIdentifier);
    }

    public interface InventoryAccess {
        int slotCount(@NotNull Identifier blockIdentifier);
        @Nullable TransferBridgeFactory.ItemStackView itemAt(@NotNull Identifier blockIdentifier, int slot);
        @Nullable String inventoryLayout(@NotNull Identifier blockIdentifier);
        @Nullable String slotSemantics(@NotNull Identifier blockIdentifier);
    }

    private static final class RuntimeInventoryAccess implements InventoryAccess {
        private final CompiledCompatibilityPlan plan;
        private final TransferBridgeFactory.ItemTransferBridge inventory;

        private RuntimeInventoryAccess(@NotNull CompiledCompatibilityPlan plan, @NotNull TransferBridgeFactory.ItemTransferBridge inventory) {
            this.plan = plan;
            this.inventory = inventory;
        }

        @Override
        public int slotCount(@NotNull Identifier blockIdentifier) {
            return this.inventory.slotCount(blockIdentifier);
        }

        @Override
        @Nullable
        public TransferBridgeFactory.ItemStackView itemAt(@NotNull Identifier blockIdentifier, int slot) {
            return this.inventory.itemAt(blockIdentifier, slot);
        }

        @Override
        @Nullable
        public String inventoryLayout(@NotNull Identifier blockIdentifier) {
            return this.plan.inventoryFacts().get("inventory_layout");
        }

        @Override
        @Nullable
        public String slotSemantics(@NotNull Identifier blockIdentifier) {
            return this.plan.inventoryFacts().get("slot_semantics");
        }
    }

    public interface AutomationAccess {
        boolean supportsSidedInsertion(@NotNull Identifier blockIdentifier);
        boolean supportsSidedExtraction(@NotNull Identifier blockIdentifier);
        @Nullable String filterType(@NotNull Identifier blockIdentifier);
        int insert(@NotNull Identifier blockIdentifier, @NotNull TransferBridgeFactory.ItemStackView item, int slot, @NotNull String side, boolean simulate);
        int extract(@NotNull Identifier blockIdentifier, @NotNull TransferBridgeFactory.ItemStackView item, int slot, @NotNull String side, boolean simulate);
    }

    private static final class RuntimeAutomationAccess implements AutomationAccess {
        private final CompiledCompatibilityPlan plan;
        private final TransferBridgeFactory.ItemTransferBridge inventory;

        private RuntimeAutomationAccess(@NotNull CompiledCompatibilityPlan plan, @NotNull TransferBridgeFactory.ItemTransferBridge inventory) {
            this.plan = plan;
            this.inventory = inventory;
        }

        @Override
        public boolean supportsSidedInsertion(@NotNull Identifier blockIdentifier) {
            return Boolean.parseBoolean(this.plan.inventoryFacts().getOrDefault("sided_insert", "false"));
        }

        @Override
        public boolean supportsSidedExtraction(@NotNull Identifier blockIdentifier) {
            return Boolean.parseBoolean(this.plan.inventoryFacts().getOrDefault("sided_extract", "false"));
        }

        @Override
        @Nullable
        public String filterType(@NotNull Identifier blockIdentifier) {
            return this.plan.inventoryFacts().get("filtering");
        }

        @Override
        public int insert(@NotNull Identifier blockIdentifier, @NotNull TransferBridgeFactory.ItemStackView item, int slot, @NotNull String side, boolean simulate) {
            return this.inventory.insert(blockIdentifier, item, slot, side, simulate);
        }

        @Override
        public int extract(@NotNull Identifier blockIdentifier, @NotNull TransferBridgeFactory.ItemStackView item, int slot, @NotNull String side, boolean simulate) {
            return this.inventory.extract(blockIdentifier, item, slot, side, simulate);
        }
    }

    private static final class MetadataBackedMachineBehaviorBridge implements MachineBehaviorBridge {
        private final CompiledCompatibilityPlan plan;

        private MetadataBackedMachineBehaviorBridge(@NotNull CompiledCompatibilityPlan plan) {
            this.plan = plan;
        }

        @Override
        public boolean hasProcessingBehavior(@NotNull Identifier blockIdentifier) {
            return plan.inventoryFacts().containsKey("has_processing")
                && Boolean.parseBoolean(plan.inventoryFacts().get("has_processing"));
        }

        @Override
        @Nullable
        public String processingType(@NotNull Identifier blockIdentifier) {
            return plan.inventoryFacts().get("processing_type");
        }

        @Override
        @Nullable
        public String redstoneControl(@NotNull Identifier blockIdentifier) {
            return plan.inventoryFacts().get("redstone_control");
        }
    }

    private static final class MetadataBackedMachineInventoryBridge implements MachineInventoryBridge {
        private final CompiledCompatibilityPlan plan;

        private MetadataBackedMachineInventoryBridge(@NotNull CompiledCompatibilityPlan plan) {
            this.plan = plan;
        }

        @Override
        public boolean hasInventory(@NotNull Identifier blockIdentifier) {
            return plan.inventoryFacts().containsKey("has_inventory")
                && Boolean.parseBoolean(plan.inventoryFacts().get("has_inventory"));
        }

        @Override
        @Nullable
        public String inventoryLayout(@NotNull Identifier blockIdentifier) {
            return plan.inventoryFacts().get("inventory_layout");
        }

        @Override
        @Nullable
        public String slotSemantics(@NotNull Identifier blockIdentifier) {
            return plan.inventoryFacts().get("slot_semantics");
        }
    }
}