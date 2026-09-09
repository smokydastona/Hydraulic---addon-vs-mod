package org.geysermc.hydraulic.pack;

import org.geysermc.pack.converter.pipeline.AssetExtractor;
import org.geysermc.pack.converter.pipeline.ExtractionContext;
import org.geysermc.pack.converter.type.texture.TextureConverter;
import org.jetbrains.annotations.NotNull;
import team.unnamed.creative.ResourcePack;
import team.unnamed.creative.texture.Texture;

import java.util.Collection;

final class SelectiveTextureExtractor implements AssetExtractor<Texture> {
    private final TextureDependencyGraph dependencies;

    SelectiveTextureExtractor(@NotNull TextureDependencyGraph dependencies) {
        this.dependencies = dependencies;
    }

    @Override
    public Collection<Texture> extract(ResourcePack pack, ExtractionContext context) {
        Collection<Texture> extracted = TextureConverter.INSTANCE.extract(pack, context);
        TextureDependencyGraph.SelectionResult selection = this.dependencies.selectTextures(extracted);
        TextureDependencyGraph.SelectionMetrics metrics = selection.metrics();
        context.logListener().info("Selected " + metrics.selectedTextures() + "/" + metrics.discoveredTextures() + " textures from " + metrics.dependencySources() + " dependency sources");
        return selection.textures();
    }
}