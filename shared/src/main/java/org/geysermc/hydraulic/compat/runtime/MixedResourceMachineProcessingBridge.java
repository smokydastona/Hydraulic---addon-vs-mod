package org.geysermc.hydraulic.compat.runtime;

import net.minecraft.resources.Identifier;
import org.geysermc.hydraulic.compat.ir.CompiledCompatibilityPlan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class MixedResourceMachineProcessingBridge {
    public interface KineticBridge {
        float speed(@NotNull Identifier blockIdentifier);
        float stressCapacity(@NotNull Identifier blockIdentifier);
        float stressImpact(@NotNull Identifier blockIdentifier);
        boolean isOverstressed(@NotNull Identifier blockIdentifier);
        default boolean isOperational(@NotNull Identifier blockIdentifier) {
            return Math.abs(speed(blockIdentifier)) > 0.001f && !isOverstressed(blockIdentifier);
        }
    }

    public interface MultiBlockStructure {
        boolean isFormed(@NotNull Identifier blockIdentifier);
        @NotNull List<String> memberPositions(@NotNull Identifier blockIdentifier);
        int memberCount(@NotNull Identifier blockIdentifier);
    }

    private final CompiledCompatibilityPlan plan;
    private final TransferBridgeFactory.ItemTransferBridge items;
    private final TransferBridgeFactory.FluidTransferBridge fluids;
    private final TransferBridgeFactory.EnergyTransferBridge energy;
    private final KineticBridge kinetic;
    private final MultiBlockStructure multiBlock;
    private final List<MixedMachineRecipe> recipes;
    private MixedMachineRecipe activeRecipe;
    private int progress;

    MixedResourceMachineProcessingBridge(
        @NotNull CompiledCompatibilityPlan plan,
        @Nullable TransferBridgeFactory.ItemTransferBridge items,
        @Nullable TransferBridgeFactory.FluidTransferBridge fluids,
        @Nullable TransferBridgeFactory.EnergyTransferBridge energy,
        @NotNull List<MixedMachineRecipe> recipes
    ) {
        this(plan, items, fluids, energy, null, null, recipes);
    }

    MixedResourceMachineProcessingBridge(
        @NotNull CompiledCompatibilityPlan plan,
        @Nullable TransferBridgeFactory.ItemTransferBridge items,
        @Nullable TransferBridgeFactory.FluidTransferBridge fluids,
        @Nullable TransferBridgeFactory.EnergyTransferBridge energy,
        @Nullable KineticBridge kinetic,
        @Nullable MultiBlockStructure multiBlock,
        @NotNull List<MixedMachineRecipe> recipes
    ) {
        this.plan = plan;
        this.items = items;
        this.fluids = fluids;
        this.energy = energy;
        this.kinetic = kinetic;
        this.multiBlock = multiBlock;
        this.recipes = List.copyOf(recipes);
    }

    @Nullable
    public KineticBridge kinetic() {
        return this.kinetic;
    }

    @Nullable
    public MultiBlockStructure multiBlock() {
        return this.multiBlock;
    }

    @NotNull
    public List<MixedMachineRecipe> recipes() {
        return this.recipes;
    }

    public int progress() {
        return this.progress;
    }

    public boolean active() {
        return this.activeRecipe != null;
    }

    public void reset() {
        this.activeRecipe = null;
        this.progress = 0;
    }

    public boolean tick(@NotNull Identifier blockIdentifier) {
        return tick(blockIdentifier, null);
    }

    public boolean tick(@NotNull Identifier blockIdentifier, @Nullable DirtyStateTracker dirtyStateTracker) {
        MixedMachineRecipe recipe = findRecipe(blockIdentifier);
        if (recipe == null) {
            reset();
            return false;
        }
        if (!recipe.equals(this.activeRecipe)) {
            this.activeRecipe = recipe;
            this.progress = 0;
        }

        if (this.progress < recipe.duration()) {
            int step = 1;
            if (this.kinetic != null && this.kinetic.isOperational(blockIdentifier)) {
                float speed = Math.abs(this.kinetic.speed(blockIdentifier));
                if (speed > 16.0f) {
                    step = Math.max(1, Math.round(speed / 16.0f));
                }
            }
            this.progress += step;
            return true;
        }

        MultiResourceTransaction transaction = new MultiResourceTransaction();
        for (ItemSlotStack input : recipe.itemInputs()) {
            transaction.addItem(this.items, new TransferRequest(blockIdentifier, TransferDirection.EXTRACT, input.stack(), input.slot(), input.side()));
        }
        for (FluidTankStack input : recipe.fluidInputs()) {
            transaction.addFluid(this.fluids, new FluidTransferRequest(blockIdentifier, TransferDirection.EXTRACT, input.stack(), input.tank(), input.side()));
        }
        if (recipe.energyInput() > 0) {
            transaction.addEnergy(this.energy, new EnergyTransferRequest(blockIdentifier, TransferDirection.EXTRACT, recipe.energyInput(), recipe.energySide()));
        }
        for (ItemSlotStack output : recipe.itemOutputs()) {
            transaction.addItem(this.items, new TransferRequest(blockIdentifier, TransferDirection.INSERT, output.stack(), output.slot(), output.side()));
        }
        for (FluidTankStack output : recipe.fluidOutputs()) {
            transaction.addFluid(this.fluids, new FluidTransferRequest(blockIdentifier, TransferDirection.INSERT, output.stack(), output.tank(), output.side()));
        }
        if (recipe.energyOutput() > 0) {
            transaction.addEnergy(this.energy, new EnergyTransferRequest(blockIdentifier, TransferDirection.INSERT, recipe.energyOutput(), recipe.energySide()));
        }

        TransferResult result = dirtyStateTracker == null ? transaction.execute() : transaction.execute(dirtyStateTracker);
        if (!result.committed()) {
            reset();
            return false;
        }
        reset();
        return true;
    }

    public int duration(@NotNull Identifier blockIdentifier) {
        MixedMachineRecipe recipe = findRecipe(blockIdentifier);
        return recipe == null ? 0 : recipe.duration();
    }

    @Nullable
    private MixedMachineRecipe findRecipe(@NotNull Identifier blockIdentifier) {
        for (MixedMachineRecipe recipe : this.recipes) {
            if (matches(blockIdentifier, recipe)) {
                return recipe;
            }
        }
        return null;
    }

    private boolean matches(@NotNull Identifier blockIdentifier, @NotNull MixedMachineRecipe recipe) {
        if (!hasRequiredBridges(recipe)) {
            return false;
        }
        if (recipe.requiresMultiBlock() && (this.multiBlock == null || !this.multiBlock.isFormed(blockIdentifier))) {
            return false;
        }
        if (recipe.minKineticSpeed() > 0.0f) {
            if (this.kinetic == null || !this.kinetic.isOperational(blockIdentifier) || Math.abs(this.kinetic.speed(blockIdentifier)) < recipe.minKineticSpeed()) {
                return false;
            }
        }
        for (ItemSlotStack input : recipe.itemInputs()) {
            TransferBridgeFactory.ItemStackView current = this.items.itemAt(blockIdentifier, input.slot());
            if (current == null || !current.matches(input.stack()) || current.count() < input.stack().count()) {
                return false;
            }
        }
        for (FluidTankStack input : recipe.fluidInputs()) {
            TransferBridgeFactory.FluidStackView current = this.fluids.tankAt(blockIdentifier, input.tank());
            if (current == null || !current.fluidId().equals(input.stack().fluidId()) || current.amount() < input.stack().amount()) {
                return false;
            }
        }
        if (recipe.energyInput() > 0 && this.energy.getEnergyStored(blockIdentifier) < recipe.energyInput()) {
            return false;
        }
        return true;
    }

    private boolean hasRequiredBridges(@NotNull MixedMachineRecipe recipe) {
        return (recipe.itemInputs().isEmpty() && recipe.itemOutputs().isEmpty() || this.items != null && this.items.executable())
            && (recipe.fluidInputs().isEmpty() && recipe.fluidOutputs().isEmpty() || this.fluids != null && this.fluids.executable())
            && (recipe.energyInput() == 0 && recipe.energyOutput() == 0 || this.energy != null && this.energy.executable());
    }

    public record MixedMachineRecipe(
        @NotNull List<ItemSlotStack> itemInputs,
        @NotNull List<FluidTankStack> fluidInputs,
        int energyInput,
        @NotNull List<ItemSlotStack> itemOutputs,
        @NotNull List<FluidTankStack> fluidOutputs,
        int energyOutput,
        @Nullable String energySide,
        float minKineticSpeed,
        float stressImpact,
        boolean requiresMultiBlock,
        int duration
    ) {
        public MixedMachineRecipe(
            @NotNull List<ItemSlotStack> itemInputs,
            @NotNull List<FluidTankStack> fluidInputs,
            int energyInput,
            @NotNull List<ItemSlotStack> itemOutputs,
            @NotNull List<FluidTankStack> fluidOutputs,
            int energyOutput,
            @Nullable String energySide,
            int duration
        ) {
            this(itemInputs, fluidInputs, energyInput, itemOutputs, fluidOutputs, energyOutput, energySide, 0.0f, 0.0f, false, duration);
        }

        public MixedMachineRecipe {
            itemInputs = List.copyOf(itemInputs);
            fluidInputs = List.copyOf(fluidInputs);
            itemOutputs = List.copyOf(itemOutputs);
            fluidOutputs = List.copyOf(fluidOutputs);
            if (duration <= 0) {
                throw new IllegalArgumentException("Mixed machine recipe duration must be positive");
            }
            if (energyInput < 0 || energyOutput < 0) {
                throw new IllegalArgumentException("Mixed machine recipe energy amounts must not be negative");
            }
            if (minKineticSpeed < 0.0f || stressImpact < 0.0f) {
                throw new IllegalArgumentException("Kinetic parameters must not be negative");
            }
            if (itemInputs.isEmpty() && fluidInputs.isEmpty() && energyInput == 0 && minKineticSpeed == 0.0f) {
                throw new IllegalArgumentException("Mixed machine recipes require at least one input or kinetic requirement");
            }
            if (itemOutputs.isEmpty() && fluidOutputs.isEmpty() && energyOutput == 0) {
                throw new IllegalArgumentException("Mixed machine recipes require at least one output");
            }
        }
    }

    public record ItemSlotStack(
        int slot,
        @NotNull TransferBridgeFactory.ItemStackView stack,
        @Nullable String side
    ) {
        public ItemSlotStack {
            if (slot < 0) {
                throw new IllegalArgumentException("Item slot must not be negative");
            }
            if (stack.isEmpty()) {
                throw new IllegalArgumentException("Machine item stacks must be non-empty");
            }
        }
    }

    public record FluidTankStack(
        int tank,
        @NotNull TransferBridgeFactory.FluidStackView stack,
        @Nullable String side
    ) {
        public FluidTankStack {
            if (tank < 0) {
                throw new IllegalArgumentException("Fluid tank must not be negative");
            }
            if (stack.amount() <= 0) {
                throw new IllegalArgumentException("Machine fluid stacks require a positive amount");
            }
        }
    }
}
