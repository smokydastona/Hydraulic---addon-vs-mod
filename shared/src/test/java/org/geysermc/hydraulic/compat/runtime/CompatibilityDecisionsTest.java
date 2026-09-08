package org.geysermc.hydraulic.compat.runtime;

import org.geysermc.hydraulic.compat.CompatibilityStatus;
import org.geysermc.hydraulic.compat.capability.CapabilityProfile;
import org.geysermc.hydraulic.compat.model.CompatibilityObject;
import org.geysermc.hydraulic.compat.model.Confidence;
import org.geysermc.hydraulic.compat.model.SupportLevel;
import org.geysermc.hydraulic.compat.model.SupportResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompatibilityDecisionsTest {
    @Test
    void allowsCreativeExposureForPlaceableAutomaticBlock() {
        CompatibilityObject object = blockObject(
            support("content", SupportLevel.AUTOMATIC, List.of("registered"), List.of()),
            support("presentation", SupportLevel.AUTOMATIC, List.of("block_asset"), List.of()),
            support("interaction", SupportLevel.AUTOMATIC, List.of("placement", "breaking"), List.of()),
            support("behavior", SupportLevel.AUTOMATIC, List.of("runtime_behavior"), List.of())
        );

        assertTrue(CompatibilityDecisions.allowsBlockCreativeExposure(object));
        assertTrue(CompatibilityDecisions.shouldApplyBlockPlacementBridge(object));
        assertTrue(CompatibilityDecisions.supportsBlockItemTextureFallback(object));
    }

    @Test
    void suppressesCreativeExposureForBehaviorUnsupportedBlock() {
        CompatibilityObject object = blockObject(
            support("content", SupportLevel.AUTOMATIC, List.of("registered"), List.of()),
            support("presentation", SupportLevel.ADAPTED, List.of("block_asset"), List.of()),
            support("interaction", SupportLevel.APPROXIMATED, List.of("placement", "breaking"), List.of("contextual_use")),
            support("behavior", SupportLevel.UNSUPPORTED, List.of(), List.of("runtime_behavior"))
        );

        assertFalse(CompatibilityDecisions.allowsBlockCreativeExposure(object));
        assertTrue(CompatibilityDecisions.shouldApplyBlockPlacementBridge(object));
        assertEquals("behavior domain is unsupported", CompatibilityDecisions.creativeExposureReason(object));
    }

    @Test
    void suppressesPlacementBridgeWhenPlacementCapabilityIsMissing() {
        CompatibilityObject object = blockObject(
            support("content", SupportLevel.AUTOMATIC, List.of("registered"), List.of()),
            support("presentation", SupportLevel.AUTOMATIC, List.of("block_asset"), List.of()),
            support("interaction", SupportLevel.UNSUPPORTED, List.of(), List.of("placement")),
            support("behavior", SupportLevel.AUTOMATIC, List.of("runtime_behavior"), List.of())
        );

        assertFalse(CompatibilityDecisions.allowsBlockCreativeExposure(object));
        assertFalse(CompatibilityDecisions.shouldApplyBlockPlacementBridge(object));
    }

    private static CompatibilityObject blockObject(SupportResult content, SupportResult presentation, SupportResult interaction, SupportResult behavior) {
        return new CompatibilityObject(
            "example:test_block",
            "block",
            "examplemod",
            Map.of(),
            new CapabilityProfile("example:test_block", List.of(), List.of()),
            Map.of(
                "content", content,
                "presentation", presentation,
                "interaction", interaction,
                "behavior", behavior
            ),
            SupportLevel.AUTOMATIC,
            CompatibilityStatus.PARTIAL,
            75,
            new Confidence(0.8D, "test"),
            List.of(),
            List.of()
        );
    }

    private static SupportResult support(String scope, SupportLevel level, List<String> supported, List<String> missing) {
        return new SupportResult(scope, level, missing.isEmpty() ? CompatibilityStatus.COMPLETE : supported.isEmpty() ? CompatibilityStatus.NONE : CompatibilityStatus.PARTIAL, supported.isEmpty() && missing.isEmpty() ? null : 100, supported, missing, List.of());
    }
}