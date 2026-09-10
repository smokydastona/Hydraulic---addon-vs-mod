package org.geysermc.hydraulic.compat.runtime;

import net.minecraft.resources.Identifier;
import org.geysermc.hydraulic.compat.CompatibilityRegistry;
import org.geysermc.hydraulic.compat.ir.CompiledCompatibilityPlan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Factory for creating transfer system bridges (item, fluid, energy) based on compiled compatibility plans.
 * This factory consumes typed RuntimeBridgeKind categories instead of freeform requirement strings.
 */
public final class TransferBridgeFactory {
    private TransferBridgeFactory() {
    }

    /**
     * Checks if an item transfer bridge can be created for the given block identifier.
     */
    public static boolean supportsItemTransfer(@NotNull Identifier blockIdentifier, @NotNull CompatibilityRegistry compatibilityRegistry) {
        CompiledCompatibilityPlan plan = compatibilityRegistry.dispatchTable().block(blockIdentifier);
        return BridgeAdapterSupport.supportsItemTransfer(plan);
    }

    /**
     * Checks if a fluid transfer bridge can be created for the given block identifier.
     */
    public static boolean supportsFluidTransfer(@NotNull Identifier blockIdentifier, @NotNull CompatibilityRegistry compatibilityRegistry) {
        CompiledCompatibilityPlan plan = compatibilityRegistry.dispatchTable().block(blockIdentifier);
        return BridgeAdapterSupport.supportsFluidTransfer(plan);
    }

    /**
     * Checks if an energy transfer bridge can be created for the given block identifier.
     */
    public static boolean supportsEnergyTransfer(@NotNull Identifier blockIdentifier, @NotNull CompatibilityRegistry compatibilityRegistry) {
        CompiledCompatibilityPlan plan = compatibilityRegistry.dispatchTable().block(blockIdentifier);
        return BridgeAdapterSupport.supportsEnergyTransfer(plan);
    }

    /**
     * Creates an item transfer bridge if supported by the compiled plan.
     */
    @Nullable
    public static ItemTransferBridge createItemTransfer(@NotNull Identifier blockIdentifier, @NotNull CompatibilityRegistry compatibilityRegistry) {
        CompiledCompatibilityPlan plan = compatibilityRegistry.dispatchTable().block(blockIdentifier);
        if (!BridgeAdapterSupport.supportsItemTransfer(plan)) {
            return null;
        }
        return new MetadataBackedItemTransferBridge(plan);
    }

    /**
     * Creates a fluid transfer bridge if supported by the compiled plan.
     */
    @Nullable
    public static FluidTransferBridge createFluidTransfer(@NotNull Identifier blockIdentifier, @NotNull CompatibilityRegistry compatibilityRegistry) {
        CompiledCompatibilityPlan plan = compatibilityRegistry.dispatchTable().block(blockIdentifier);
        if (!BridgeAdapterSupport.supportsFluidTransfer(plan)) {
            return null;
        }
        return new MetadataBackedFluidTransferBridge(plan);
    }

    /**
     * Creates an energy transfer bridge if supported by the compiled plan.
     */
    @Nullable
    public static EnergyTransferBridge createEnergyTransfer(@NotNull Identifier blockIdentifier, @NotNull CompatibilityRegistry compatibilityRegistry) {
        CompiledCompatibilityPlan plan = compatibilityRegistry.dispatchTable().block(blockIdentifier);
        if (!BridgeAdapterSupport.supportsEnergyTransfer(plan)) {
            return null;
        }
        return new MetadataBackedEnergyTransferBridge(plan);
    }

    /**
     * Interface for item transfer bridges.
     */
    public interface ItemTransferBridge {
        boolean canInsert(@NotNull Identifier blockIdentifier);
        boolean canExtract(@NotNull Identifier blockIdentifier);
        @Nullable String inventoryType(@NotNull Identifier blockIdentifier);
    }

    /**
     * Interface for fluid transfer bridges.
     */
    public interface FluidTransferBridge {
        boolean canInsertFluid(@NotNull Identifier blockIdentifier);
        boolean canExtractFluid(@NotNull Identifier blockIdentifier);
        @Nullable String tankType(@NotNull Identifier blockIdentifier);
    }

    /**
     * Interface for energy transfer bridges.
     */
    public interface EnergyTransferBridge {
        boolean canReceiveEnergy(@NotNull Identifier blockIdentifier);
        boolean canProvideEnergy(@NotNull Identifier blockIdentifier);
        @Nullable String energyType(@NotNull Identifier blockIdentifier);
    }

    private static final class MetadataBackedItemTransferBridge implements ItemTransferBridge {
        private final CompiledCompatibilityPlan plan;

        private MetadataBackedItemTransferBridge(@NotNull CompiledCompatibilityPlan plan) {
            this.plan = plan;
        }

        @Override
        public boolean canInsert(@NotNull Identifier blockIdentifier) {
            return plan.inventoryFacts().containsKey("can_insert") 
                && Boolean.parseBoolean(plan.inventoryFacts().get("can_insert"));
        }

        @Override
        public boolean canExtract(@NotNull Identifier blockIdentifier) {
            return plan.inventoryFacts().containsKey("can_extract") 
                && Boolean.parseBoolean(plan.inventoryFacts().get("can_extract"));
        }

        @Override
        @Nullable
        public String inventoryType(@NotNull Identifier blockIdentifier) {
            return plan.inventoryFacts().get("inventory_type");
        }
    }

    private static final class MetadataBackedFluidTransferBridge implements FluidTransferBridge {
        private final CompiledCompatibilityPlan plan;

        private MetadataBackedFluidTransferBridge(@NotNull CompiledCompatibilityPlan plan) {
            this.plan = plan;
        }

        @Override
        public boolean canInsertFluid(@NotNull Identifier blockIdentifier) {
            return plan.inventoryFacts().containsKey("can_insert_fluid") 
                && Boolean.parseBoolean(plan.inventoryFacts().get("can_insert_fluid"));
        }

        @Override
        public boolean canExtractFluid(@NotNull Identifier blockIdentifier) {
            return plan.inventoryFacts().containsKey("can_extract_fluid") 
                && Boolean.parseBoolean(plan.inventoryFacts().get("can_extract_fluid"));
        }

        @Override
        @Nullable
        public String tankType(@NotNull Identifier blockIdentifier) {
            return plan.inventoryFacts().get("tank_type");
        }
    }

    private static final class MetadataBackedEnergyTransferBridge implements EnergyTransferBridge {
        private final CompiledCompatibilityPlan plan;

        private MetadataBackedEnergyTransferBridge(@NotNull CompiledCompatibilityPlan plan) {
            this.plan = plan;
        }

        @Override
        public boolean canReceiveEnergy(@NotNull Identifier blockIdentifier) {
            return plan.inventoryFacts().containsKey("can_receive_energy") 
                && Boolean.parseBoolean(plan.inventoryFacts().get("can_receive_energy"));
        }

        @Override
        public boolean canProvideEnergy(@NotNull Identifier blockIdentifier) {
            return plan.inventoryFacts().containsKey("can_provide_energy") 
                && Boolean.parseBoolean(plan.inventoryFacts().get("can_provide_energy"));
        }

        @Override
        @Nullable
        public String energyType(@NotNull Identifier blockIdentifier) {
            return plan.inventoryFacts().get("energy_type");
        }
    }
}