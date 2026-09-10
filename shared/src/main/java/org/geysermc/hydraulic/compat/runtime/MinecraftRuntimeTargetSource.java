package org.geysermc.hydraulic.compat.runtime;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Resolves live Minecraft block entities into runtime targets for compiled capability dispatch. */
public final class MinecraftRuntimeTargetSource implements RuntimeTargetDiscovery.TargetSource {
    private final ServerLevel level;

    public MinecraftRuntimeTargetSource(@NotNull ServerLevel level) {
        this.level = level;
    }

    @Override
    @Nullable
    public RuntimeTargetDiscovery.Target targetAt(@NotNull RuntimeTargetDiscovery.Position position) {
        if (!this.level.dimension().identifier().toString().equals(position.level())) {
            return null;
        }

        BlockPos blockPos = new BlockPos(position.x(), position.y(), position.z());
        BlockEntity blockEntity = this.level.getBlockEntity(blockPos);
        if (blockEntity == null) {
            return null;
        }

        BlockState blockState = this.level.getBlockState(blockPos);
        Identifier blockIdentifier = BuiltInRegistries.BLOCK.getKey(blockState.getBlock());
        if (blockIdentifier == null) {
            return null;
        }
        return new RuntimeTargetDiscovery.Target(blockIdentifier, blockEntity, blockEntity, blockEntity);
    }
}
