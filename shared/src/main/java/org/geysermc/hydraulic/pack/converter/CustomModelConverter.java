package org.geysermc.hydraulic.pack.converter;

import org.geysermc.pack.converter.pipeline.AssetExtractor;
import org.geysermc.pack.converter.pipeline.ExtractionContext;
import org.geysermc.hydraulic.pack.TextureDependencyGraph;
import org.geysermc.pack.converter.type.model.ModelStitcher;
import team.unnamed.creative.ResourcePack;
import team.unnamed.creative.model.Model;

import java.util.Collection;

public class CustomModelConverter implements AssetExtractor<Model> {
    private final ModelStitcher.Provider modelProvider;
    private final TextureDependencyGraph textureDependencies;

    public CustomModelConverter(ModelStitcher.Provider modelProvider, TextureDependencyGraph textureDependencies) {
        this.modelProvider = modelProvider;
        this.textureDependencies = textureDependencies;
    }

    @Override
    public Collection<Model> extract(ResourcePack pack, ExtractionContext context) {
        return pack.models().stream()
                .map(model -> {
                    this.textureDependencies.recordModel(model);
                    Model stitched = new ModelStitcher(this.modelProvider, model, context.logListener()).stitch();
                    this.textureDependencies.recordModel(stitched);
                    return stitched;
                })
                .toList();
    }
}
