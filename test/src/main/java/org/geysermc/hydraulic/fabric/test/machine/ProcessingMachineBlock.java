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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import org.geysermc.hydraulic.HydraulicImpl;
import org.geysermc.hydraulic.compat.CompatibilityRegistry;
import org.geysermc.hydraulic.compat.ir.CompiledCompatibilityPlan;
import org.geysermc.hydraulic.compat.runtime.MachineBridgeFactory;
import org.geysermc.hydraulic.compat.runtime.MachineProcessingBridge;
import org.geysermc.hydraulic.compat.runtime.TransferBridgeFactory;
import org.geysermc.hydraulic.compat.runtime.BedrockRuntimeActionRouter;
import org.geysermc.hydraulic.compat.runtime.RuntimeTraceId;
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
    protected InteractionResult useItemOn(
        @NotNull ItemStack stack,
        @NotNull BlockState state,
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull Player player,
        @NotNull InteractionHand hand,
        @NotNull BlockHitResult hitResult
    ) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }
        var result = BedrockRuntimeActionRouter.executeBlockUse(
            serverPlayer,
            pos,
            HydraulicImpl.instance().getPackManager().compatibilityRegistry(),
            RuntimeTraceId.create()
        );
        return result.status() == BedrockRuntimeActionRouter.Status.MUTATED
            ? InteractionResult.SUCCESS_SERVER
            : super.useItemOn(stack, state, level, pos, player, hand, hitResult);
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
