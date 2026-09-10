package org.geysermc.hydraulic.pack;

import net.minecraft.resources.Identifier;
import org.geysermc.hydraulic.platform.mod.ModInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

final class ItemAssetLocator {
    private ItemAssetLocator() {
    }

    @Nullable
    static Path resolveItemAssetPath(@NotNull ModInfo mod, @NotNull Identifier itemModel) {
        Path itemDefinition = mod.resolveFile("assets/" + itemModel.getNamespace() + "/items/" + itemModel.getPath() + ".json");
        if (itemDefinition != null) {
            return itemDefinition;
        }

        return mod.resolveFile("assets/" + itemModel.getNamespace() + "/models/item/" + itemModel.getPath() + ".json");
    }
}