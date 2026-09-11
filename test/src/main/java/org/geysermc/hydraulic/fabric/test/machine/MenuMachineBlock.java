package org.geysermc.hydraulic.fabric.test.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.geysermc.hydraulic.fabric.test.ModBlockEntities;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class MenuMachineBlock extends ResourceMachineBlock {
    public MenuMachineBlock(@NotNull BlockBehaviour.Properties properties) {
        super(properties, MenuMachineBlockEntity::new);
    }

    @Override
    protected InteractionResult useWithoutItem(
        @NotNull BlockState state,
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull Player player,
        @NotNull BlockHitResult hitResult
    ) {
        if (!(player instanceof ServerPlayer serverPlayer) || !(level.getBlockEntity(pos) instanceof MenuMachineBlockEntity machine)) {
            return InteractionResult.SUCCESS;
        }
        serverPlayer.openMenu(new SimpleMenuProvider(
            (containerId, inventory, menuPlayer) -> new MenuMachineMenu(containerId, inventory, machine),
            Component.literal("Hydraulic Menu Machine")
        ));
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
        @NotNull Level level,
        @NotNull BlockState state,
        @NotNull BlockEntityType<T> type
    ) {
        if (level.isClientSide() || type != ModBlockEntities.MENU_MACHINE) {
            return null;
        }
        return (ignoredLevel, ignoredPos, ignoredState, blockEntity) -> {
            if (blockEntity instanceof MenuMachineBlockEntity machine) {
                MenuMachineBlockEntity.tick(machine);
            }
        };
    }
}