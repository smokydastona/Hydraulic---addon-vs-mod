package org.geysermc.hydraulic.compat.model;

import org.jetbrains.annotations.NotNull;

public record ModFingerprint(
    @NotNull String modId,
    @NotNull String namespace,
    @NotNull String version,
    @NotNull String loader,
    @NotNull String minecraftVersion,
    int registeredBlocks,
    int registeredItems,
    int registeredEntities,
    int registeredFluids,
    int registeredBlockEntities,
    int registeredMenus,
    int registeredRecipes,
    boolean usesBlockEntities,
    boolean usesCustomRenderers,
    boolean usesCustomNetworking,
    boolean usesCapabilities,
    boolean usesCustomItemComponents,
    boolean usesCustomModels,
    boolean usesCustomParticles,
    boolean usesCustomSounds
) {
}