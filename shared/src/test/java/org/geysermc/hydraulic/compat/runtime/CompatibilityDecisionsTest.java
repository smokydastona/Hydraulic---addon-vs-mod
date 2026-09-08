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

    @Test
    void wearablePresentationRequiresSupportedContentAndPresentation() {
        CompatibilityObject supported = blockObject(
            support("content", SupportLevel.AUTOMATIC, List.of("registered"), List.of()),
            support("presentation", SupportLevel.ADAPTED, List.of("item_asset"), List.of()),
            support("interaction", SupportLevel.AUTOMATIC, List.of("offhand"), List.of()),
            support("behavior", SupportLevel.APPROXIMATED, List.of("runtime_behavior"), List.of())
        );
        CompatibilityObject unsupported = blockObject(
            support("content", SupportLevel.UNSUPPORTED, List.of(), List.of("registered")),
            support("presentation", SupportLevel.AUTOMATIC, List.of("item_asset"), List.of()),
            support("interaction", SupportLevel.AUTOMATIC, List.of("offhand"), List.of()),
            support("behavior", SupportLevel.AUTOMATIC, List.of("runtime_behavior"), List.of())
        );

        assertTrue(CompatibilityDecisions.supportsWearableItemPresentation(supported));
        assertFalse(CompatibilityDecisions.supportsWearableItemPresentation(unsupported));
    }

    @Test
    void attachablePresentationRejectsUnsupportedPresentation() {
        CompatibilityObject supported = blockObject(
            support("content", SupportLevel.AUTOMATIC, List.of("registered"), List.of()),
            support("presentation", SupportLevel.AUTOMATIC, List.of("item_asset"), List.of()),
            support("interaction", SupportLevel.AUTOMATIC, List.of("offhand"), List.of()),
            support("behavior", SupportLevel.AUTOMATIC, List.of("runtime_behavior"), List.of())
        );
        CompatibilityObject unsupported = blockObject(
            support("content", SupportLevel.AUTOMATIC, List.of("registered"), List.of()),
            support("presentation", SupportLevel.UNSUPPORTED, List.of(), List.of("item_asset")),
            support("interaction", SupportLevel.AUTOMATIC, List.of("offhand"), List.of()),
            support("behavior", SupportLevel.AUTOMATIC, List.of("runtime_behavior"), List.of())
        );

        assertTrue(CompatibilityDecisions.supportsAttachableItemPresentation(supported));
        assertFalse(CompatibilityDecisions.supportsAttachableItemPresentation(unsupported));
    }

    @Test
    void suppressesCustomItemRegistrationWhenPresentationIsUnsupported() {
        CompatibilityObject unsupported = blockObject(
            support("content", SupportLevel.AUTOMATIC, List.of("registered"), List.of()),
            support("presentation", SupportLevel.UNSUPPORTED, List.of(), List.of("item_asset")),
            support("interaction", SupportLevel.AUTOMATIC, List.of("offhand"), List.of()),
            support("behavior", SupportLevel.AUTOMATIC, List.of("runtime_behavior"), List.of())
        );

        assertFalse(CompatibilityDecisions.allowsCustomItemRegistration(unsupported));
    }

    @Test
    void suppressesCreativeExposureWhenItemBehaviorIsApproximated() {
        CompatibilityObject approximated = blockObject(
            support("content", SupportLevel.AUTOMATIC, List.of("registered"), List.of()),
            support("presentation", SupportLevel.AUTOMATIC, List.of("item_asset"), List.of()),
            support("interaction", SupportLevel.AUTOMATIC, List.of("offhand"), List.of()),
            support("behavior", SupportLevel.APPROXIMATED, List.of(), List.of("runtime_behavior"))
        );

        assertFalse(CompatibilityDecisions.allowsItemCreativeExposure(approximated));
        CompatibilityObject tagged = new CompatibilityObject(
            approximated.javaIdentifier(),
            approximated.contentType(),
            approximated.modId(),
            java.util.Map.of("behavior_tag", "custom_pack_behavior"),
            approximated.capabilityProfile(),
            approximated.supportResults(),
            approximated.overallLevel(),
            approximated.overallStatus(),
            approximated.overallScore(),
            approximated.confidence(),
            approximated.provenance(),
            approximated.findings()
        );

        assertEquals("behavior domain is approximated (tag: custom_pack_behavior)", CompatibilityDecisions.itemCreativeExposureReason(tagged));
    }

    @Test
    void allowsCreativeExposureForTaggedBowWhenAttachableAdapterApplies() {
        CompatibilityObject tagged = objectWithFacts(
            Map.of("behavior_tag", "bow_attachable"),
            support("content", SupportLevel.AUTOMATIC, List.of("registered"), List.of()),
            support("presentation", SupportLevel.AUTOMATIC, List.of("item_asset"), List.of()),
            support("interaction", SupportLevel.AUTOMATIC, List.of("offhand"), List.of()),
            support("behavior", SupportLevel.APPROXIMATED, List.of(), List.of("runtime_behavior"))
        );

        assertTrue(CompatibilityDecisions.supportsAttachableItemPresentation(tagged, null));
        assertTrue(CompatibilityDecisions.allowsItemCreativeExposure(tagged, null));
    }

    @Test
    void allowsCreativeExposureForTaggedWearableWhenAdapterTagApplies() {
        CompatibilityObject tagged = objectWithFacts(
            Map.of("behavior_tag", "wearable"),
            support("content", SupportLevel.AUTOMATIC, List.of("registered"), List.of()),
            support("presentation", SupportLevel.AUTOMATIC, List.of("item_asset"), List.of()),
            support("interaction", SupportLevel.AUTOMATIC, List.of("offhand"), List.of()),
            support("behavior", SupportLevel.APPROXIMATED, List.of(), List.of("runtime_behavior"))
        );

        assertTrue(CompatibilityDecisions.supportsWearableItemPresentation(tagged, null));
        assertTrue(CompatibilityDecisions.allowsItemCreativeExposure(tagged, null));
    }

    @Test
    void suppressesCustomEntityRegistrationWhenPresentationIsUnsupported() {
        CompatibilityObject unsupported = blockObject(
            support("content", SupportLevel.AUTOMATIC, List.of("registered"), List.of()),
            support("presentation", SupportLevel.UNSUPPORTED, List.of(), List.of("presentation_mapping")),
            support("interaction", SupportLevel.UNSUPPORTED, List.of(), List.of("entity_interaction")),
            support("behavior", SupportLevel.UNSUPPORTED, List.of(), List.of("runtime_behavior"))
        );

        assertFalse(CompatibilityDecisions.allowsCustomEntityRegistration(unsupported));
        assertEquals("presentation domain is unsupported", CompatibilityDecisions.entityRegistrationReason(unsupported));
    }

    private static CompatibilityObject blockObject(SupportResult content, SupportResult presentation, SupportResult interaction, SupportResult behavior) {
        return objectWithFacts(Map.of(), content, presentation, interaction, behavior);
    }

    private static CompatibilityObject objectWithFacts(Map<String, String> inventoryFacts, SupportResult content, SupportResult presentation, SupportResult interaction, SupportResult behavior) {
        return new CompatibilityObject(
            "example:test_block",
            "block",
            "examplemod",
            inventoryFacts,
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