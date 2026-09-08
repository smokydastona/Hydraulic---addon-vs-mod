package org.geysermc.hydraulic.compat;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import org.geysermc.hydraulic.metadata.BlockMapping;
import org.geysermc.hydraulic.metadata.BlockStateRule;
import org.geysermc.hydraulic.metadata.MetadataIndex;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class MappingResolver {
    private final MetadataIndex metadataIndex;

    public MappingResolver(@NotNull MetadataIndex metadataIndex) {
        this.metadataIndex = metadataIndex;
    }

    @Nullable
    public BlockMapping blockMapping(@NotNull Identifier javaIdentifier) {
        return this.metadataIndex.blockMapping(javaIdentifier);
    }

    @Nullable
    public BlockStateRule blockRule(@NotNull Identifier javaIdentifier, @NotNull BlockState state) {
        return this.metadataIndex.blockRule(javaIdentifier, state);
    }

    @NotNull
    public ResolvedIdentifier resolveBlockIdentifier(@NotNull Identifier javaIdentifier, @NotNull Iterable<BlockState> states) {
        Identifier resolved = null;
        boolean conflicting = false;

        for (BlockState state : states) {
            BlockStateRule rule = this.blockRule(javaIdentifier, state);
            if (rule == null || rule.bedrockIdentifier() == null) {
                continue;
            }

            if (resolved == null) {
                resolved = rule.bedrockIdentifier();
                continue;
            }

            if (!resolved.equals(rule.bedrockIdentifier())) {
                conflicting = true;
                break;
            }
        }

        if (conflicting || resolved == null) {
            return new ResolvedIdentifier(javaIdentifier, resolved != null, conflicting);
        }

        return new ResolvedIdentifier(resolved, true, false);
    }

    public record ResolvedIdentifier(@NotNull Identifier identifier, boolean overridden, boolean conflicting) {
    }
}