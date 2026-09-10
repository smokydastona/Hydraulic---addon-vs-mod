package org.geysermc.hydraulic.fabric.test.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.geysermc.hydraulic.HydraulicImpl;
import org.geysermc.hydraulic.compat.CompatibilityRegistry;
import org.geysermc.hydraulic.compat.ir.CompiledCompatibilityPlan;
import org.geysermc.hydraulic.compat.runtime.MachineBridgeFactory;
import org.geysermc.hydraulic.compat.runtime.MachineProcessingBridge;
import org.geysermc.hydraulic.compat.runtime.TransferBridgeFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ProcessingMachineBlock extends Block implements EntityBlock {
    public ProcessingMachineBlock(@NotNull BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new ProcessingMachineBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
        @NotNull Level level,
        @NotNull BlockState state,
        @NotNull BlockEntityType<T> type
    ) {
        if (level.isClientSide() || type != org.geysermc.hydraulic.fabric.test.ModBlockEntities.PROCESSING_MACHINE) {
            return null;
        }
        return (lvl, pos, blockState, blockEntity) -> {
            if (blockEntity instanceof ProcessingMachineBlockEntity machine) {
                tickProcessing(machine, blockState);
            }
        };
    }

    /**
     * Drives the real, production {@code MachineProcessingBridge} against this block entity's
     * reflectively-adapted item transfer contract every server tick.
     */
    private static void tickProcessing(@NotNull ProcessingMachineBlockEntity machine, @NotNull BlockState state) {
        Identifier blockIdentifier = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (blockIdentifier == null) {
            return;
        }

        CompatibilityRegistry registry = HydraulicImpl.instance().getPackManager().compatibilityRegistry();
        CompiledCompatibilityPlan plan = registry.dispatchTable().block(blockIdentifier);
        if (plan == null) {
            return;
        }

        TransferBridgeFactory.ItemTransferBridge itemTransfer = TransferBridgeFactory.createItemTransfer(plan, machine);
        if (itemTransfer == null) {
            return;
        }

        MachineProcessingBridge processing = MachineBridgeFactory.createProcessing(plan, itemTransfer);
        if (processing == null) {
            return;
        }

        processing.tick(blockIdentifier);
    }
}
