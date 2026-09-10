package org.geysermc.hydraulic.pack;

import net.kyori.adventure.key.Key;
import org.geysermc.pack.converter.pipeline.AssetExtractor;
import org.geysermc.pack.converter.pipeline.ExtractionContext;
import org.geysermc.pack.converter.type.texture.TextureConverter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import team.unnamed.creative.base.Writable;
import team.unnamed.creative.ResourcePack;
import team.unnamed.creative.texture.Texture;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

final class SelectiveTextureExtractor implements AssetExtractor<Texture> {
    private final @Nullable ModResourceIndex resourceIndex;
    private final TextureDependencyGraph dependencies;

    SelectiveTextureExtractor(@Nullable ModResourceIndex resourceIndex, @NotNull TextureDependencyGraph dependencies) {
        this.resourceIndex = resourceIndex;
        this.dependencies = dependencies;
    }

    @Override
    public Collection<Texture> extract(ResourcePack pack, ExtractionContext context) {
        Collection<Texture> extracted = this.resourceIndex != null ? indexedTextures() : TextureConverter.INSTANCE.extract(pack, context);
        TextureDependencyGraph.SelectionResult selection = this.dependencies.selectTextures(extracted);
        TextureDependencyGraph.SelectionMetrics metrics = selection.metrics();
        context.logListener().info("Selected " + metrics.selectedTextures() + "/" + metrics.discoveredTextures() + " textures from " + metrics.dependencySources() + " dependency sources");
        return selection.textures();
    }

    @NotNull
    private Collection<Texture> indexedTextures() {
        return this.resourceIndex.texturePaths().entrySet().stream()
            .map(SelectiveTextureExtractor::texture)
            .filter(Objects::nonNull)
            .toList();
    }

    @Nullable
    private static Texture texture(@NotNull Map.Entry<Key, java.nio.file.Path> entry) {
        if (!java.nio.file.Files.isRegularFile(entry.getValue())) {
            return null;
        }
        return Texture.texture(entry.getKey(), Writable.path(entry.getValue()));
    }
}