package org.geysermc.hydraulic.pack;

import net.kyori.adventure.key.Key;
import org.geysermc.hydraulic.pack.context.PackContext;
import org.jetbrains.annotations.NotNull;

public abstract class TexturePackModule<T extends PackModule<T>> extends PackModule<T> {
    /**
     * Gets the output location of the given key.
     *
     * @param packContext the pack context
     * @param key the key
     * @return the output location
     */
    protected static <T extends PackModule<T>> String getOutputFromModel(@NotNull PackContext<T> packContext, @NotNull Key key) {
        return packContext.hydraulic().getPackManager().textureResolutionCache().resolveModelOutput(packContext.mod().id(), key);
    }

    protected static <T extends PackModule<T>> String getOutputFromBlockTexture(@NotNull PackContext<T> packContext, @NotNull Key key) {
        return packContext.hydraulic().getPackManager().textureResolutionCache().resolveBlockTextureOutput(packContext.mod().id(), key);
    }
}
