package org.geysermc.hydraulic.pack;

import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import team.unnamed.creative.model.Model;
import team.unnamed.creative.model.ModelTexture;
import team.unnamed.creative.model.ModelTextures;
import team.unnamed.creative.texture.Texture;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class TextureDependencyGraph {
    private final Map<String, Set<Key>> dependenciesBySource = new LinkedHashMap<>();
    private final Set<Key> requiredTextures = new LinkedHashSet<>();
    private final Map<String, Set<String>> requiredTexturePathsByNamespace = new LinkedHashMap<>();
    private volatile Set<Key> lastSelectedTextures = Set.of();
    private volatile SelectionMetrics lastSelectionMetrics = new SelectionMetrics(0, 0, 0, 0);

    public void recordModel(@Nullable Model model) {
        if (model == null) {
            return;
        }

        String sourceId = model.key() == null ? "<anonymous-model>" : model.key().asString();
        Map<String, ModelTexture> textures = flattenTextures(model.textures());
        for (String name : textures.keySet()) {
            ModelTexture texture = resolveTexture(textures, name, new LinkedHashSet<>());
            if (texture != null && texture.key() != null) {
                this.recordDependency("model", sourceId, texture.key());
            }
        }
    }

    public void recordEquipmentTexture(@NotNull String sourceId, @NotNull Key textureKey) {
        this.recordDependency("equipment", sourceId, textureKey);
    }

    public void recordDependency(@NotNull String sourceType, @NotNull String sourceId, @NotNull Key textureKey) {
        String source = sourceType + ":" + sourceId;
        this.dependenciesBySource.computeIfAbsent(source, ignored -> new LinkedHashSet<>()).add(textureKey);
        this.requiredTextures.add(textureKey);
        this.requiredTexturePathsByNamespace.computeIfAbsent(textureKey.namespace(), ignored -> new LinkedHashSet<>()).add(normalizePath(textureKey.value()));
    }

    @NotNull
    public SelectionResult selectTextures(@NotNull Collection<Texture> availableTextures) {
        List<Texture> discovered = List.copyOf(availableTextures);
        if (this.requiredTextures.isEmpty()) {
            Set<Key> selectedKeys = new LinkedHashSet<>(this.lastSelectedTextures);
            for (Texture texture : discovered) {
                selectedKeys.add(texture.key());
            }
            this.lastSelectedTextures = Set.copyOf(selectedKeys);
            SelectionMetrics metrics = this.lastSelectionMetrics.add(discovered.size(), discovered.size(), 0, this.dependenciesBySource.size());
            this.lastSelectionMetrics = metrics;
            return new SelectionResult(discovered, metrics);
        }

        List<Texture> selected = new ArrayList<>();
        for (Texture texture : discovered) {
            if (this.shouldInclude(texture.key())) {
                selected.add(texture);
            }
        }

        Set<Key> selectedKeys = new LinkedHashSet<>(this.lastSelectedTextures);
        for (Texture texture : selected) {
            selectedKeys.add(texture.key());
        }
        this.lastSelectedTextures = Set.copyOf(selectedKeys);
        SelectionMetrics metrics = this.lastSelectionMetrics.add(discovered.size(), selected.size(), Math.max(0, discovered.size() - selected.size()), this.dependenciesBySource.size());
        this.lastSelectionMetrics = metrics;
        return new SelectionResult(List.copyOf(selected), metrics);
    }

    public boolean shouldInclude(@NotNull Key textureKey) {
        if (this.requiredTextures.isEmpty()) {
            return true;
        }

        Set<String> requiredPaths = this.requiredTexturePathsByNamespace.get(textureKey.namespace());
        if (requiredPaths == null || requiredPaths.isEmpty()) {
            return false;
        }

        String availablePath = normalizePath(textureKey.value());
        for (String requiredPath : requiredPaths) {
            if (availablePath.equals(requiredPath) || availablePath.endsWith("/" + requiredPath) || requiredPath.endsWith("/" + availablePath)) {
                return true;
            }
        }
        return false;
    }

    @NotNull
    public SelectionMetrics lastSelectionMetrics() {
        return this.lastSelectionMetrics;
    }

    @NotNull
    public Set<Key> requiredTextures() {
        return Set.copyOf(this.requiredTextures);
    }

    @NotNull
    public Set<Key> lastSelectedTextures() {
        return this.lastSelectedTextures;
    }

    @NotNull
    private static Map<String, ModelTexture> flattenTextures(@NotNull ModelTextures modelTextures) {
        Map<String, ModelTexture> textures = new LinkedHashMap<>(modelTextures.variables());
        textures.put("particle", modelTextures.particle());
        for (int index = 0; index < modelTextures.layers().size(); index++) {
            textures.put("layer" + index, modelTextures.layers().get(index));
        }
        return textures;
    }

    @Nullable
    private static ModelTexture resolveTexture(@NotNull Map<String, ModelTexture> textures, @NotNull String name, @NotNull Set<String> visited) {
        if (!visited.add(name)) {
            return null;
        }

        ModelTexture texture = textures.get(name);
        if (texture == null) {
            return null;
        }
        if (texture.reference() != null) {
            return resolveTexture(textures, texture.reference(), visited);
        }
        return texture;
    }

    @NotNull
    private static String normalizePath(@NotNull String path) {
        String normalized = path.replace('\\', '/').toLowerCase();
        if (normalized.startsWith("textures/")) {
            normalized = normalized.substring("textures/".length());
        }
        if (normalized.endsWith(".png")) {
            normalized = normalized.substring(0, normalized.length() - 4);
        } else if (normalized.endsWith(".tga")) {
            normalized = normalized.substring(0, normalized.length() - 4);
        }
        return normalized;
    }

    public record SelectionResult(@NotNull List<Texture> textures, @NotNull SelectionMetrics metrics) {
    }

    public record SelectionMetrics(int discoveredTextures, int selectedTextures, int omittedTextures, int dependencySources) {
        SelectionMetrics add(int discoveredTextures, int selectedTextures, int omittedTextures, int dependencySources) {
            return new SelectionMetrics(
                this.discoveredTextures + discoveredTextures,
                this.selectedTextures + selectedTextures,
                this.omittedTextures + omittedTextures,
                Math.max(this.dependencySources, dependencySources)
            );
        }
    }
}