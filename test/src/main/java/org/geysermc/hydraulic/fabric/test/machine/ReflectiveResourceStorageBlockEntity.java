package org.geysermc.hydraulic.fabric.test.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.geysermc.hydraulic.compat.runtime.TransferBridgeFactory.FluidStackView;
import org.jetbrains.annotations.NotNull;

public abstract class ReflectiveResourceStorageBlockEntity extends ReflectiveItemStorageBlockEntity {
    private static final int TANK_CAPACITY = 4_000;
    private static final int ENERGY_CAPACITY = 10_000;

    private FluidStackView fluid = new FluidStackView("minecraft:empty", 0);
    private int energy;

    protected ReflectiveResourceStorageBlockEntity(
        @NotNull BlockEntityType<?> type,
        @NotNull BlockPos pos,
        @NotNull BlockState state,
        int slotCount
    ) {
        super(type, pos, state, slotCount);
    }

    public int getTanks() {
        return 1;
    }

    public int getTankCapacity(int tank) {
        return tank == 0 ? TANK_CAPACITY : 0;
    }

    @NotNull
    public FluidStackView getFluidInTank(int tank) {
        return tank == 0 ? this.fluid : new FluidStackView("minecraft:empty", 0);
    }

    public int fill(int tank, @NotNull FluidStackView resource, boolean simulate) {
        if (tank != 0 || empty(resource) || !empty(this.fluid) && !sameFluid(this.fluid, resource)) {
            return 0;
        }
        int moved = Math.min(resource.amount(), TANK_CAPACITY - this.fluid.amount());
        if (!simulate && moved > 0) {
            this.fluid = new FluidStackView(resource.fluidId(), this.fluid.amount() + moved);
            this.setChanged();
        }
        return moved;
    }

    public int drain(int tank, @NotNull FluidStackView resource, int maxAmount, boolean simulate) {
        if (tank != 0 || empty(this.fluid) || !sameFluid(this.fluid, resource)) {
            return 0;
        }
        int moved = Math.min(Math.max(maxAmount, 0), this.fluid.amount());
        if (!simulate && moved > 0) {
            int remaining = this.fluid.amount() - moved;
            this.fluid = remaining == 0
                ? new FluidStackView("minecraft:empty", 0)
                : new FluidStackView(this.fluid.fluidId(), remaining);
            this.setChanged();
        }
        return moved;
    }

    public int getEnergyStored() {
        return this.energy;
    }

    public int getMaxEnergyStored() {
        return ENERGY_CAPACITY;
    }

    public int receiveEnergy(int amount, boolean simulate) {
        int moved = Math.min(Math.max(amount, 0), ENERGY_CAPACITY - this.energy);
        if (!simulate && moved > 0) {
            this.energy += moved;
            this.setChanged();
        }
        return moved;
    }

    public int extractEnergy(int amount, boolean simulate) {
        int moved = Math.min(Math.max(amount, 0), this.energy);
        if (!simulate && moved > 0) {
            this.energy -= moved;
            this.setChanged();
        }
        return moved;
    }

    private static boolean empty(@NotNull FluidStackView fluid) {
        return fluid.amount() <= 0 || "minecraft:empty".equals(fluid.fluidId());
    }

    private static boolean sameFluid(@NotNull FluidStackView first, @NotNull FluidStackView second) {
        return first.fluidId().equals(second.fluidId());
    }

    @Override
    protected void saveAdditional(@NotNull ValueOutput output) {
        super.saveAdditional(output);
        output.putString("fluid_id", this.fluid.fluidId());
        output.putInt("fluid_amount", this.fluid.amount());
        output.putInt("energy", this.energy);
    }

    @Override
    protected void loadAdditional(@NotNull ValueInput input) {
        super.loadAdditional(input);
        this.fluid = new FluidStackView(
            input.getStringOr("fluid_id", "minecraft:empty"),
            input.getIntOr("fluid_amount", 0)
        );
        this.energy = Math.clamp(input.getIntOr("energy", 0), 0, ENERGY_CAPACITY);
    }
}