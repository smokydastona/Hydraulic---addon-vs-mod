package org.geysermc.hydraulic.fabric.test.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.geysermc.hydraulic.fabric.test.ModBlockEntities;
import org.jetbrains.annotations.NotNull;

public final class EnergyMachineBlockEntity extends ReflectiveResourceStorageBlockEntity {
    public EnergyMachineBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        super(ModBlockEntities.ENERGY_MACHINE, pos, state, 1);
    }
}