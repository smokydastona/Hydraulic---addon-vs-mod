package org.geysermc.hydraulic.fabric.test.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.geysermc.hydraulic.fabric.test.ModBlockEntities;
import org.jetbrains.annotations.NotNull;

/**
 * Real, persistent 3-slot machine inventory (no processing) - exercises ITEM_TRANSFER/MACHINE_INVENTORY only.
 */
public final class ItemTransferMachineBlockEntity extends ReflectiveItemStorageBlockEntity {
    public static final int SLOT_COUNT = 3;

    public ItemTransferMachineBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        super(ModBlockEntities.ITEM_TRANSFER_MACHINE, pos, state, SLOT_COUNT);
    }
}

