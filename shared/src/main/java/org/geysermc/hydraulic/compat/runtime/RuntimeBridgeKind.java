package org.geysermc.hydraulic.compat.runtime;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public enum RuntimeBridgeKind {
    BLOCK_PLACEMENT("block_placement_bridge", "block"),
    BLOCK_BEHAVIOR("block_behavior_bridge", "block"),
    ITEM_REGISTRATION("item_registration_bridge", "item"),
    ITEM_BEHAVIOR("item_behavior_bridge", "item"),
    ENTITY_INTERACTION("entity_interaction_bridge", "entity"),
    ENTITY_BEHAVIOR("entity_behavior_bridge", "entity"),
    MENU_CONTAINER("container_bridge", "menu"),
    MENU_BEHAVIOR("menu_behavior_bridge", "menu"),
    BLOCK_ENTITY_DATA("block_entity_data_bridge", "block_entity"),
    BLOCK_ENTITY_INTERACTION("block_entity_interaction_bridge", "block_entity"),
    BLOCK_ENTITY_BEHAVIOR("block_entity_behavior_bridge", "block_entity"),
    FLUID_TRANSLATOR("fluid_translator", "fluid"),
    FLUID_RUNTIME("fluid_runtime_bridge", "fluid");

    private static final List<RuntimeBridgeKind> MENU_RUNTIME_KINDS = List.of(MENU_CONTAINER, MENU_BEHAVIOR);
    private static final List<RuntimeBridgeKind> BLOCK_ENTITY_RUNTIME_KINDS = List.of(BLOCK_ENTITY_DATA, BLOCK_ENTITY_INTERACTION, BLOCK_ENTITY_BEHAVIOR);
    private static final List<RuntimeBridgeKind> FLUID_RUNTIME_KINDS = List.of(FLUID_TRANSLATOR, FLUID_RUNTIME);

    private final String requirementId;
    private final String contentType;

    RuntimeBridgeKind(@NotNull String requirementId, @NotNull String contentType) {
        this.requirementId = requirementId;
        this.contentType = contentType;
    }

    @NotNull
    public String requirementId() {
        return this.requirementId;
    }

    @NotNull
    public String contentType() {
        return this.contentType;
    }

    @NotNull
    public static List<RuntimeBridgeKind> menuRuntimeKinds() {
        return MENU_RUNTIME_KINDS;
    }

    @NotNull
    public static List<RuntimeBridgeKind> blockEntityRuntimeKinds() {
        return BLOCK_ENTITY_RUNTIME_KINDS;
    }

    @NotNull
    public static List<RuntimeBridgeKind> fluidRuntimeKinds() {
        return FLUID_RUNTIME_KINDS;
    }

    @NotNull
    public static List<String> requirementIds(@NotNull List<RuntimeBridgeKind> kinds) {
        List<String> requirementIds = new ArrayList<>(kinds.size());
        for (RuntimeBridgeKind kind : kinds) {
            requirementIds.add(kind.requirementId());
        }
        Collections.sort(requirementIds);
        return List.copyOf(requirementIds);
    }

    @Nullable
    public static RuntimeBridgeKind fromRequirement(@NotNull String requirementId) {
        for (RuntimeBridgeKind kind : values()) {
            if (kind.requirementId.equals(requirementId)) {
                return kind;
            }
        }
        return null;
    }

    @NotNull
    public static List<RuntimeBridgeKind> resolve(@NotNull List<String> runtimeRequirements) {
        Set<RuntimeBridgeKind> resolved = new LinkedHashSet<>();
        for (String requirement : runtimeRequirements) {
            RuntimeBridgeKind kind = fromRequirement(requirement);
            if (kind != null) {
                resolved.add(kind);
            }
        }
        return List.copyOf(resolved);
    }
}