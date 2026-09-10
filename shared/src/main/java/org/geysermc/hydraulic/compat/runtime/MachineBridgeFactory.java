package org.geysermc.hydraulic.compat.runtime;

import net.minecraft.resources.Identifier;
import org.geysermc.hydraulic.compat.CompatibilityRegistry;
import org.geysermc.hydraulic.compat.ir.CompiledCompatibilityPlan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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