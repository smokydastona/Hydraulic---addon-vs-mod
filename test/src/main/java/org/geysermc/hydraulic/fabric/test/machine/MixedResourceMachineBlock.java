package org.geysermc.hydraulic.fabric.test.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.geysermc.hydraulic.HydraulicImpl;
import org.geysermc.hydraulic.compat.ir.CompiledCompatibilityPlan;
import org.geysermc.hydraulic.compat.runtime.MachineBridgeFactory;
import org.geysermc.hydraulic.compat.runtime.MixedResourceMachineProcessingBridge;
import org.geysermc.hydraulic.compat.runtime.TransferBridgeFactory;
import org.geysermc.hydraulic.fabric.test.ModBlockEntities;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class MixedResourceMachineBlock extends ResourceMachineBlock {
    public MixedResourceMachineBlock(@NotNull BlockBehaviour.Properties properties) {
        super(properties, MixedResourceMachineBlockEntity::new);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
        @NotNull Level level,
        @NotNull BlockState state,
        @NotNull BlockEntityType<T> type
    ) {
        if (level.isClientSide() || type != ModBlockEntities.MIXED_RESOURCE_MACHINE) {
            return null;
        }
        return (ignoredLevel, ignoredPos, blockState, blockEntity) -> {
            if (blockEntity instanceof MixedResourceMachineBlockEntity machine) {
                tickProcessing(blockState, machine);
            }
        };
    }

    private static void tickProcessing(@NotNull BlockState state, @NotNull MixedResourceMachineBlockEntity machine) {
        Identifier identifier = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (identifier == null) {
            return;
        }
        CompiledCompatibilityPlan plan = HydraulicImpl.instance().getPackManager().compatibilityRegistry().dispatchTable().block(identifier);
        if (plan == null) {
            return;
        }
        TransferBridgeFactory.ItemTransferBridge items = TransferBridgeFactory.createItemTransfer(plan, machine);
        TransferBridgeFactory.FluidTransferBridge fluids = TransferBridgeFactory.createFluidTransfer(plan, machine);
        TransferBridgeFactory.EnergyTransferBridge energy = TransferBridgeFactory.createEnergyTransfer(plan, machine);
        MixedResourceMachineProcessingBridge processing = MachineBridgeFactory.createMixedProcessing(plan, items, fluids, energy);
        if (processing != null) {
            processing.tick(identifier);
        }
    }
}