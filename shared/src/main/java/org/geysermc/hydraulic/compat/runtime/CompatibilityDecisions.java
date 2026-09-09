package org.geysermc.hydraulic.compat.runtime;

import org.geysermc.hydraulic.compat.adapter.AdapterFeature;
import org.geysermc.hydraulic.compat.adapter.CapabilityAdapterRegistry;
import org.geysermc.hydraulic.compat.model.CompatibilityObject;
import org.geysermc.hydraulic.compat.model.SupportLevel;
import org.geysermc.hydraulic.compat.model.SupportResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class CompatibilityDecisions {
    private CompatibilityDecisions() {
    }

    public static boolean supportsBlockItemTextureFallback(@Nullable CompatibilityObject compatibilityObject) {
        return CapabilityAdapterRegistry.supports(AdapterFeature.BLOCK_ITEM_TEXTURE_FALLBACK, compatibilityObject, null);
    }

    public static boolean allowsBlockCreativeExposure(@Nullable CompatibilityObject compatibilityObject) {
        return allowsBlockCreativeExposure(compatibilityObject, null);
    }

    public static boolean allowsBlockCreativeExposure(@Nullable CompatibilityObject compatibilityObject, @Nullable Object runtimeObject) {
        if (compatibilityObject == null) {
            return true;
        }

        return CapabilityAdapterRegistry.supports(AdapterFeature.BLOCK_CREATIVE_EXPOSURE, compatibilityObject, runtimeObject);
    }

    public static boolean shouldApplyBlockPlacementBridge(@Nullable CompatibilityObject compatibilityObject) {
        return shouldApplyBlockPlacementBridge(compatibilityObject, null);
    }

    public static boolean shouldApplyBlockPlacementBridge(@Nullable CompatibilityObject compatibilityObject, @Nullable Object runtimeObject) {
        return compatibilityObject == null || CapabilityAdapterRegistry.supports(AdapterFeature.BLOCK_PLACEMENT, compatibilityObject, runtimeObject);
    }

    public static boolean supportsWearableItemPresentation(@Nullable CompatibilityObject compatibilityObject) {
        if (compatibilityObject == null) {
            return true;
        }
        SupportResult content = support(compatibilityObject, "content");
        SupportResult presentation = support(compatibilityObject, "presentation");
        return !isUnsupported(content) && !isUnsupported(presentation);
    }

    public static boolean supportsWearableItemPresentation(@Nullable CompatibilityObject compatibilityObject, @Nullable Object runtimeObject) {
        if (compatibilityObject == null) {
            return true;
        }
        return CapabilityAdapterRegistry.supports(AdapterFeature.WEARABLE_ITEM_PRESENTATION, compatibilityObject, runtimeObject);
    }

    public static boolean supportsAttachableItemPresentation(@Nullable CompatibilityObject compatibilityObject) {
        if (compatibilityObject == null) {
            return true;
        }

        SupportResult content = support(compatibilityObject, "content");
        SupportResult presentation = support(compatibilityObject, "presentation");
        return !isUnsupported(content) && !isUnsupported(presentation);
    }

    public static boolean supportsAttachableItemPresentation(@Nullable CompatibilityObject compatibilityObject, @Nullable Object runtimeObject) {
        if (compatibilityObject == null) {
            return true;
        }

        SupportResult content = support(compatibilityObject, "content");
        SupportResult presentation = support(compatibilityObject, "presentation");
        return !isUnsupported(content)
            && !isUnsupported(presentation)
            && (CapabilityAdapterRegistry.supports(AdapterFeature.ATTACHABLE_ITEM_PRESENTATION, compatibilityObject, runtimeObject)
            || !isUnsupported(support(compatibilityObject, "behavior")));
    }

    public static boolean allowsCustomItemRegistration(@Nullable CompatibilityObject compatibilityObject) {
        return allowsCustomItemRegistration(compatibilityObject, null);
    }

    public static boolean allowsCustomItemRegistration(@Nullable CompatibilityObject compatibilityObject, @Nullable Object runtimeObject) {
        if (compatibilityObject == null) {
            return true;
        }

        return CapabilityAdapterRegistry.supports(AdapterFeature.CUSTOM_ITEM_REGISTRATION, compatibilityObject, runtimeObject);
    }

    public static boolean allowsItemCreativeExposure(@Nullable CompatibilityObject compatibilityObject) {
        return allowsItemCreativeExposure(compatibilityObject, null);
    }

    public static boolean allowsItemCreativeExposure(@Nullable CompatibilityObject compatibilityObject, @Nullable Object runtimeObject) {
        if (!allowsCustomItemRegistration(compatibilityObject, runtimeObject)) {
            return false;
        }

        if (requiresItemBehaviorBridge(compatibilityObject)) {
            return false;
        }

        SupportResult behavior = support(compatibilityObject, "behavior");
        return behavior == null
            || (behavior.level() != SupportLevel.UNSUPPORTED && behavior.level() != SupportLevel.APPROXIMATED)
            || CapabilityAdapterRegistry.supports(AdapterFeature.ITEM_CREATIVE_EXPOSURE, compatibilityObject, runtimeObject);
    }

    @Nullable
    public static String itemCreativeExposureReason(@Nullable CompatibilityObject compatibilityObject) {
        return itemCreativeExposureReason(compatibilityObject, null);
    }

    @Nullable
    public static String itemCreativeExposureReason(@Nullable CompatibilityObject compatibilityObject, @Nullable Object runtimeObject) {
        if (compatibilityObject == null || allowsItemCreativeExposure(compatibilityObject, runtimeObject)) {
            return null;
        }

        if (!allowsCustomItemRegistration(compatibilityObject, runtimeObject)) {
            return "content or presentation support is insufficient";
        }

        if (requiresItemBehaviorBridge(compatibilityObject)) {
            return behaviorReason("item behavior runtime bridge is required", compatibilityObject);
        }

        SupportResult behavior = support(compatibilityObject, "behavior");
        if (behavior != null && behavior.level() == SupportLevel.UNSUPPORTED) {
            return behaviorReason("behavior domain is unsupported", compatibilityObject);
        }
        if (behavior != null && behavior.level() == SupportLevel.APPROXIMATED) {
            return behaviorReason("behavior domain is approximated", compatibilityObject);
        }
        return "compatibility evidence is insufficient";
    }

    public static boolean allowsCustomEntityRegistration(@Nullable CompatibilityObject compatibilityObject) {
        if (!CapabilityAdapterRegistry.supports(AdapterFeature.CUSTOM_ENTITY_REGISTRATION, compatibilityObject, null)) {
            return false;
        }

        return !requiresEntityRuntimeBridge(compatibilityObject) || hasBehaviorTag(compatibilityObject, "visual_only_runtime");
    }

    @Nullable
    public static String entityRegistrationReason(@Nullable CompatibilityObject compatibilityObject) {
        if (compatibilityObject == null) {
            return "compatibility object is missing";
        }
        if (allowsCustomEntityRegistration(compatibilityObject)) {
            return null;
        }

        if (requiresEntityRuntimeBridge(compatibilityObject) && !hasBehaviorTag(compatibilityObject, "visual_only_runtime")) {
            return behaviorReason("entity interaction or behavior runtime bridge is required", compatibilityObject);
        }

        SupportResult presentation = support(compatibilityObject, "presentation");
        if (isUnsupported(presentation)) {
            return "presentation domain is unsupported";
        }

        SupportResult content = support(compatibilityObject, "content");
        if (isUnsupported(content)) {
            return "content domain is unsupported";
        }
        return "compatibility evidence is insufficient";
    }

    @Nullable
    public static String creativeExposureReason(@Nullable CompatibilityObject compatibilityObject) {
        if (compatibilityObject == null || allowsBlockCreativeExposure(compatibilityObject)) {
            return null;
        }

        SupportResult behavior = support(compatibilityObject, "behavior");
        if (isUnsupported(behavior)) {
            return behaviorReason("behavior domain is unsupported", compatibilityObject);
        }

        SupportResult presentation = support(compatibilityObject, "presentation");
        if (isUnsupported(presentation)) {
            return "presentation domain is unsupported";
        }

        SupportResult interaction = support(compatibilityObject, "interaction");
        if (interaction != null && !interaction.supportedCapabilities().contains("placement")) {
            return "interaction domain does not support placement";
        }

        SupportResult content = support(compatibilityObject, "content");
        if (isUnsupported(content)) {
            return "content domain is unsupported";
        }
        return "compatibility evidence is insufficient";
    }

    private static boolean isUnsupported(@Nullable SupportResult supportResult) {
        return supportResult != null && supportResult.level() == SupportLevel.UNSUPPORTED;
    }

    private static boolean requiresItemBehaviorBridge(@Nullable CompatibilityObject compatibilityObject) {
        return compatibilityObject != null
            && RuntimeBridgeKind.resolve(compatibilityObject.runtimeRequirements()).contains(RuntimeBridgeKind.ITEM_BEHAVIOR);
    }

    private static boolean requiresEntityRuntimeBridge(@Nullable CompatibilityObject compatibilityObject) {
        if (compatibilityObject == null) {
            return false;
        }

        List<RuntimeBridgeKind> runtimeBridgeKinds = RuntimeBridgeKind.resolve(compatibilityObject.runtimeRequirements());
        return runtimeBridgeKinds.contains(RuntimeBridgeKind.ENTITY_INTERACTION)
            || runtimeBridgeKinds.contains(RuntimeBridgeKind.ENTITY_BEHAVIOR);
    }

    private static boolean hasBehaviorTag(@Nullable CompatibilityObject compatibilityObject, @NotNull String... tags) {
        if (compatibilityObject == null) {
            return false;
        }

        String behaviorTag = compatibilityObject.inventoryFacts().get("behavior_tag");
        if (behaviorTag == null || behaviorTag.isBlank()) {
            return false;
        }

        for (String tag : tags) {
            if (behaviorTag.equals(tag)) {
                return true;
            }
        }
        return false;
    }

    @NotNull
    private static String behaviorReason(@NotNull String baseReason, @NotNull CompatibilityObject compatibilityObject) {
        String behaviorTag = compatibilityObject.inventoryFacts().get("behavior_tag");
        if (behaviorTag == null || behaviorTag.isBlank()) {
            return baseReason;
        }
        return baseReason + " (tag: " + behaviorTag + ")";
    }

    @Nullable
    private static SupportResult support(@Nullable CompatibilityObject compatibilityObject, @NotNull String domain) {
        return compatibilityObject != null ? compatibilityObject.supportResults().get(domain) : null;
    }
}