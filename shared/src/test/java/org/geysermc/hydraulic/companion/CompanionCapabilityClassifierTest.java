package org.geysermc.hydraulic.companion;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CompanionCapabilityClassifierTest {
    private final CompanionCapabilityClassifier classifier = new CompanionCapabilityClassifier();

    @Test
    void clientLocalCapabilityIsAlwaysSupported() {
        CompanionCapability capability = new CompanionCapability("jei_inventory_search", "search", false);
        List<CompanionCapabilityResult> results = classifier.classify(manifestWith(capability), false);

        assertEquals(CompanionCapabilityStatus.CLIENT_LOCAL_SUPPORTED, results.get(0).status());
    }

    @Test
    void knownServerBridgeCapabilityIsSupportedOnlyWhenSignalInstalled() {
        CompanionCapability capability = new CompanionCapability("companion_detection_signal", "signal", true);

        List<CompanionCapabilityResult> withoutSignal = classifier.classify(manifestWith(capability), false);
        assertEquals(CompanionCapabilityStatus.UNSUPPORTED_NO_BRIDGE, withoutSignal.get(0).status());

        List<CompanionCapabilityResult> withSignal = classifier.classify(manifestWith(capability), true);
        assertEquals(CompanionCapabilityStatus.SERVER_SIGNAL_SUPPORTED, withSignal.get(0).status());
    }

    @Test
    void unknownServerBridgeCapabilityIsNeverFakedAsSupported() {
        CompanionCapability capability = new CompanionCapability("custom_multiblock_sync", "unimplemented", true);

        List<CompanionCapabilityResult> results = classifier.classify(manifestWith(capability), true);

        assertEquals(CompanionCapabilityStatus.UNSUPPORTED_NO_BRIDGE, results.get(0).status());
    }

    private static CompanionManifest manifestWith(CompanionCapability capability) {
        return new CompanionManifest(
                "test",
                "Test Companion",
                "1.0.0",
                CompanionExecutionMode.CLIENT_GLOBAL_BEHAVIOR_PACK,
                "resource_pack",
                "behavior_pack",
                List.of(capability)
        );
    }
}
