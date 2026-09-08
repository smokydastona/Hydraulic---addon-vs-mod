package org.geysermc.hydraulic.metadata;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class BlockMapping {
    private final Identifier javaIdentifier;
    private final List<BlockStateRule> rules;

    public BlockMapping(@NotNull Identifier javaIdentifier, @NotNull List<BlockStateRule> rules) {
        this.javaIdentifier = javaIdentifier;
        this.rules = rules;
    }

    @NotNull
    public Identifier javaIdentifier() {
        return this.javaIdentifier;
    }

    @NotNull
    public List<BlockStateRule> rules() {
        return this.rules;
    }

    @Nullable
    public BlockStateRule findRule(@NotNull BlockState state) {
        for (BlockStateRule rule : this.rules) {
            if (rule.matches(state)) {
                return rule;
            }
        }
        return null;
    }
}