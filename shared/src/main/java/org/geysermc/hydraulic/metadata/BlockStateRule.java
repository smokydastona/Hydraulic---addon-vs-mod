package org.geysermc.hydraulic.metadata;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.geysermc.hydraulic.compat.MappingOwnership;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public final class BlockStateRule {
    private final Map<String, String> javaWhen;
    private final Identifier bedrockIdentifier;
    private final Map<String, String> bedrockState;
    private final String geometryId;
    private final String materialId;
    private final boolean behaviorRequired;
    private final String behaviorTag;
    private final MappingOwnership ownership;
    private final String sourcePath;
    private final int priority;
    private final int order;

    public BlockStateRule(
        @NotNull Map<String, String> javaWhen,
        @Nullable Identifier bedrockIdentifier,
        @Nullable Map<String, String> bedrockState,
        @Nullable String geometryId,
        @Nullable String materialId,
        boolean behaviorRequired,
        @Nullable String behaviorTag,
        @NotNull MappingOwnership ownership,
        @NotNull String sourcePath,
        int priority,
        int order
    ) {
        this.javaWhen = javaWhen;
        this.bedrockIdentifier = bedrockIdentifier;
        this.bedrockState = bedrockState;
        this.geometryId = geometryId;
        this.materialId = materialId;
        this.behaviorRequired = behaviorRequired;
        this.behaviorTag = behaviorTag;
        this.ownership = ownership;
        this.sourcePath = sourcePath;
        this.priority = priority;
        this.order = order;
    }

    public boolean matches(@NotNull BlockState state) {
        for (Map.Entry<String, String> entry : this.javaWhen.entrySet()) {
            Property<?> property = property(state, entry.getKey());
            if (property == null) {
                return false;
            }

            String actualValue = state.getValue(property).toString();
            if (!entry.getValue().equals(actualValue)) {
                return false;
            }
        }
        return true;
    }

    @Nullable
    private static Property<?> property(@NotNull BlockState state, @NotNull String name) {
        for (Property<?> property : state.getProperties()) {
            if (property.getName().equals(name)) {
                return property;
            }
        }
        return null;
    }

    @NotNull
    public Map<String, String> javaWhen() {
        return this.javaWhen;
    }

    @Nullable
    public Identifier bedrockIdentifier() {
        return this.bedrockIdentifier;
    }

    @Nullable
    public Map<String, String> bedrockState() {
        return this.bedrockState;
    }

    @Nullable
    public String geometryId() {
        return this.geometryId;
    }

    @Nullable
    public String materialId() {
        return this.materialId;
    }

    public boolean behaviorRequired() {
        return this.behaviorRequired;
    }

    @Nullable
    public String behaviorTag() {
        return this.behaviorTag;
    }

    @NotNull
    public MappingOwnership ownership() {
        return this.ownership;
    }

    @NotNull
    public String sourcePath() {
        return this.sourcePath;
    }

    public int priority() {
        return this.priority;
    }

    public int order() {
        return this.order;
    }

    public int specificity() {
        return this.javaWhen.size();
    }
}