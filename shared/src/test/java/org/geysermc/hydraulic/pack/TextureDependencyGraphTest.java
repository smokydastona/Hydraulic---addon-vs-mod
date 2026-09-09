package org.geysermc.hydraulic.pack;

import com.google.gson.JsonParser;
import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.Test;
import team.unnamed.creative.base.Writable;
import team.unnamed.creative.metadata.pack.PackFormat;
import team.unnamed.creative.model.Model;
import team.unnamed.creative.serialize.minecraft.model.ModelSerializer;
import team.unnamed.creative.texture.Texture;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextureDependencyGraphTest {
    @Test
    void recordsResolvedModelTextures() {
        Model model = ModelSerializer.INSTANCE.deserializeFromJson(
            JsonParser.parseString("""
                {
                  \"textures\": {
                    \"layer0\": \"examplemod:item/base\",
                    \"layer1\": \"#accent\",
                    \"accent\": \"examplemod:item/accent\",
                    \"particle\": \"#layer0\"
                  }
                }
                """),
            Key.key("examplemod", "item/test"),
            PackFormat.UNKNOWN
        );

        TextureDependencyGraph graph = new TextureDependencyGraph();
        graph.recordModel(model);

        assertEquals(Set.of(Key.key("examplemod", "item/base"), Key.key("examplemod", "item/accent")), graph.requiredTextures());
    }

    @Test
    void selectsOnlyRequiredTexturesAndAccumulatesMetricsAcrossRoots() {
        TextureDependencyGraph graph = new TextureDependencyGraph();
        graph.recordDependency("model", "examplemod:item/test", Key.key("examplemod", "item/base"));
        graph.recordEquipmentTexture("examplemod:barrel", Key.key("examplemod", "entity/barrel"));

        TextureDependencyGraph.SelectionResult first = graph.selectTextures(List.of(
            Texture.texture(Key.key("examplemod", "item/base"), Writable.bytes(new byte[] {1})),
            Texture.texture(Key.key("examplemod", "item/unused"), Writable.bytes(new byte[] {2}))
        ));
        TextureDependencyGraph.SelectionResult second = graph.selectTextures(List.of(
            Texture.texture(Key.key("examplemod", "entity/barrel"), Writable.bytes(new byte[] {3})),
            Texture.texture(Key.key("examplemod", "block/unused"), Writable.bytes(new byte[] {4}))
        ));

        assertEquals(1, first.textures().size());
        assertEquals(Key.key("examplemod", "item/base"), first.textures().getFirst().key());
        assertEquals(1, second.textures().size());
        assertEquals(Key.key("examplemod", "entity/barrel"), second.textures().getFirst().key());

        TextureDependencyGraph.SelectionMetrics metrics = graph.lastSelectionMetrics();
        assertEquals(4, metrics.discoveredTextures());
        assertEquals(2, metrics.selectedTextures());
        assertEquals(2, metrics.omittedTextures());
        assertEquals(2, metrics.dependencySources());
        assertTrue(graph.shouldInclude(Key.key("examplemod", "item/base")));
        assertTrue(!graph.shouldInclude(Key.key("examplemod", "item/unused")));
    }
}