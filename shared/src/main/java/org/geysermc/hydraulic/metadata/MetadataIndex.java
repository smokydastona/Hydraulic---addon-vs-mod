package org.geysermc.hydraulic.metadata;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public final class MetadataIndex {
    private final Map<Identifier, BlockMapping> blockMappings;

    public MetadataIndex(@NotNull Map<Identifier, BlockMapping> blockMappings) {
        this.blockMappings = blockMappings;
    }

    @Nullable
    public BlockMapping blockMapping(@NotNull Identifier javaIdentifier) {
        return this.blockMappings.get(javaIdentifier);
    }

    @Nullable
    public BlockStateRule blockRule(@NotNull Identifier javaIdentifier, @NotNull BlockState state) {
        BlockMapping mapping = this.blockMappings.get(javaIdentifier);
        if (mapping == null) {
            return null;
        }
        return mapping.findRule(state);
    }

    public boolean isEmpty() {
        return this.blockMappings.isEmpty();
    }
}