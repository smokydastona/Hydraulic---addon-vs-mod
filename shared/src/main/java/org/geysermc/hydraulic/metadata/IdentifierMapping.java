package org.geysermc.hydraulic.metadata;

import net.minecraft.resources.Identifier;
import org.geysermc.hydraulic.compat.MappingOwnership;
import org.jetbrains.annotations.NotNull;

public final class IdentifierMapping {
    private final Identifier javaIdentifier;
    private final Identifier bedrockIdentifier;
    private final MappingOwnership ownership;
    private final String sourcePath;
    private final int priority;
    private final int order;

    public IdentifierMapping(
        @NotNull Identifier javaIdentifier,
        @NotNull Identifier bedrockIdentifier,
        @NotNull MappingOwnership ownership,
        @NotNull String sourcePath,
        int priority,
        int order
    ) {
        this.javaIdentifier = javaIdentifier;
        this.bedrockIdentifier = bedrockIdentifier;
        this.ownership = ownership;
        this.sourcePath = sourcePath;
        this.priority = priority;
        this.order = order;
    }

    @NotNull
    public Identifier javaIdentifier() {
        return this.javaIdentifier;
    }

    @NotNull
    public Identifier bedrockIdentifier() {
        return this.bedrockIdentifier;
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
}