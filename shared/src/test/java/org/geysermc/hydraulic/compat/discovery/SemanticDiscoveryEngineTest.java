package org.geysermc.hydraulic.compat.discovery;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SemanticDiscoveryEngineTest {
    @Test
    void unregisteredContentDoesNotReceiveRegistryCapabilityFacts() {
        SemanticDiscoveryEngine.DiscoveredSemanticProfile profile = SemanticDiscoveryEngine.discover(
            Identifier.fromNamespaceAndPath("example", "missing"),
            Map.of(),
            false
        );

        assertFalse(profile.facts().containsKey("channel_a.registry_discovered"));
        assertTrue(profile.evidenceList().stream().noneMatch(evidence -> evidence.channel() == SemanticDiscoveryEngine.Channel.MINECRAFT_REGISTRY));
    }

    @Test
    void registeredContentReceivesRegistryEvidence() {
        SemanticDiscoveryEngine.DiscoveredSemanticProfile profile = SemanticDiscoveryEngine.discover(
            Identifier.fromNamespaceAndPath("example", "registered"),
            Map.of(),
            true
        );

        assertTrue(profile.facts().containsKey("channel_a.registry_discovered"));
        assertTrue(profile.evidenceList().stream().anyMatch(evidence -> evidence.channel() == SemanticDiscoveryEngine.Channel.MINECRAFT_REGISTRY));
    }
}