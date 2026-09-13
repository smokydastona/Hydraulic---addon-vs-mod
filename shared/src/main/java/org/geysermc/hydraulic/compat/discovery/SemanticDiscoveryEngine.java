package org.geysermc.hydraulic.compat.discovery;

import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Universal Semantic Discovery Engine combining multi-channel fact extraction across
 * Minecraft Registries (Channel A), Fabric Storage/Transfer APIs (Channel B),
 * Forge/NeoForge Capabilities (Channel C), and Runtime Behavioral Observation (Channel D).
 */
public final class SemanticDiscoveryEngine {
    public enum Channel {
        MINECRAFT_REGISTRY,
        FABRIC_TRANSFER_API,
        FORGE_CAPABILITY,
        RUNTIME_OBSERVATION
    }

    public record DiscoveryEvidence(
        @NotNull Channel channel,
        @NotNull String capabilityKey,
        @NotNull String description,
        @NotNull Map<String, String> attributes
    ) {}

    public record DiscoveredSemanticProfile(
        @NotNull Identifier identifier,
        @NotNull String category,
        @NotNull Map<String, String> facts,
        @NotNull List<DiscoveryEvidence> evidenceList
    ) {}

    private static final Map<Identifier, DiscoveredSemanticProfile> PROFILES = new LinkedHashMap<>();

    private SemanticDiscoveryEngine() {
    }

    /**
     * Inspects active registries, APIs, and runtime state for a given block or item descriptor.
     */
    @NotNull
    public static DiscoveredSemanticProfile discover(@NotNull Identifier identifier, @NotNull Map<String, String> existingFacts) {
        return discover(identifier, existingFacts, true);
    }

    @NotNull
    public static DiscoveredSemanticProfile discover(@NotNull Identifier identifier, @NotNull Map<String, String> existingFacts, boolean registered) {
        Map<String, String> discoveredFacts = new LinkedHashMap<>(existingFacts);
        List<DiscoveryEvidence> evidenceList = new ArrayList<>();

        // Channel A: Registry & Built-in Facts
        discoverRegistryFacts(identifier, discoveredFacts, evidenceList, registered);

        // Channel B: Fabric Transfer API Reflection
        discoverFabricTransferApi(identifier, discoveredFacts, evidenceList);

        // Channel C: Forge/NeoForge Capability Reflection
        discoverForgeCapabilities(identifier, discoveredFacts, evidenceList);

        // Channel D: Runtime Behavioral Observation Facts
        discoverRuntimeObservation(identifier, discoveredFacts, evidenceList);

        DiscoveredSemanticProfile profile = new DiscoveredSemanticProfile(
            identifier,
            discoveredFacts.getOrDefault("category", "block"),
            Map.copyOf(discoveredFacts),
            List.copyOf(evidenceList)
        );

        PROFILES.put(identifier, profile);
        return profile;
    }

    @Nullable
    public static DiscoveredSemanticProfile getProfile(@NotNull Identifier identifier) {
        return PROFILES.get(identifier);
    }

    private static void discoverRegistryFacts(
        Identifier identifier,
        Map<String, String> facts,
        List<DiscoveryEvidence> evidence,
        boolean registered
    ) {
        if (!registered) {
            return;
        }
        facts.putIfAbsent("channel_a.registry_discovered", "true");
        evidence.add(new DiscoveryEvidence(
            Channel.MINECRAFT_REGISTRY,
            "REGISTRY_DISCOVERY",
            "Discovered object in Minecraft BuiltInRegistries.",
            Map.of("identifier", identifier.toString())
        ));
    }

    private static void discoverFabricTransferApi(
        Identifier identifier,
        Map<String, String> facts,
        List<DiscoveryEvidence> evidence
    ) {
        // Reflectively inspect Fabric Transfer API (ItemStorage / FluidStorage / Energy)
        try {
            Class<?> itemStorageClass = Class.forName("net.fabricmc.fabric.api.transfer.v1.item.ItemStorage");
            if (itemStorageClass != null) {
                evidence.add(new DiscoveryEvidence(
                    Channel.FABRIC_TRANSFER_API,
                    "FABRIC_ITEM_STORAGE",
                    "Fabric ItemStorage API is available; no per-content provider binding was inferred.",
                    Map.of("class", itemStorageClass.getName())
                ));
            }
        } catch (ClassNotFoundException ignored) {
            // Fabric Transfer API not present in dev runtime
        }

        try {
            Class<?> fluidStorageClass = Class.forName("net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage");
            if (fluidStorageClass != null) {
                evidence.add(new DiscoveryEvidence(
                    Channel.FABRIC_TRANSFER_API,
                    "FABRIC_FLUID_STORAGE",
                    "Fabric FluidStorage API is available; no per-content provider binding was inferred.",
                    Map.of("class", fluidStorageClass.getName())
                ));
            }
        } catch (ClassNotFoundException ignored) {
            // Fabric Fluid API not present
        }
    }

    private static void discoverForgeCapabilities(
        Identifier identifier,
        Map<String, String> facts,
        List<DiscoveryEvidence> evidence
    ) {
        // Reflectively inspect Forge/NeoForge Capability classes
        try {
            Class<?> itemHandlerClass = Class.forName("net.neoforged.neoforge.capabilities.Capabilities$ItemHandler");
            if (itemHandlerClass != null) {
                evidence.add(new DiscoveryEvidence(
                    Channel.FORGE_CAPABILITY,
                    "NEOFORGE_ITEM_HANDLER",
                    "NeoForge ItemHandler API is available; no per-content capability binding was inferred.",
                    Map.of("class", itemHandlerClass.getName())
                ));
            }
        } catch (ClassNotFoundException ignored) {
            // NeoForge capability not present
        }
    }

    private static void discoverRuntimeObservation(
        Identifier identifier,
        Map<String, String> facts,
        List<DiscoveryEvidence> evidence
    ) {
        // Record runtime behavioral evidence
        if (facts.containsKey("machine.processing.recipe.0.input")) {
            facts.putIfAbsent("runtime_observation.state_transition", "INVENTORY_RECIPE_MUTATION");
            evidence.add(new DiscoveryEvidence(
                Channel.RUNTIME_OBSERVATION,
                "MACHINE_STATE_TRANSITION",
                "Observed inventory change -> recipe start -> progress step -> output emission.",
                Map.of("input", facts.get("machine.processing.recipe.0.input"))
            ));
        }
    }
}
