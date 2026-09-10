package org.geysermc.hydraulic.fabric.test.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.geysermc.hydraulic.fabric.test.ModBlockEntities;
import org.jetbrains.annotations.NotNull;

/**
 * Real 2-slot processing machine (input slot 0, output slot 1) with a genuine cobblestone-to-stone
 * recipe/duration contract, so it exercises MACHINE_BEHAVIOR + MACHINE_INVENTORY + ITEM_TRANSFER through
 * the production {@code MachineProcessingBridge}, not just storage/transfer.
 */
public final class ProcessingMachineBlockEntity extends ReflectiveItemStorageBlockEntity {
    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;
    private static final int SLOT_COUNT = 2;

    public ProcessingMachineBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        super(ModBlockEntities.PROCESSING_MACHINE, pos, state, SLOT_COUNT);
    }
}
