package org.geysermc.hydraulic.fabric.test.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.geysermc.hydraulic.fabric.test.ModBlockEntities;
import org.jetbrains.annotations.NotNull;

public final class MixedResourceMachineBlockEntity extends ReflectiveResourceStorageBlockEntity {
    public MixedResourceMachineBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        super(ModBlockEntities.MIXED_RESOURCE_MACHINE, pos, state, 2);
    }
}