package org.geysermc.hydraulic.compat.runtime;

import org.geysermc.hydraulic.compat.model.CompatibilityObject;
import org.geysermc.hydraulic.compat.model.SupportLevel;
import org.geysermc.hydraulic.compat.model.SupportResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class CompatibilityDecisions {
    private CompatibilityDecisions() {
    }

    public static boolean supportsBlockItemTextureFallback(@Nullable CompatibilityObject compatibilityObject) {
        SupportResult presentation = support(compatibilityObject, "presentation");
        return presentation != null
            && presentation.level() != SupportLevel.UNSUPPORTED
            && presentation.level() != SupportLevel.VISUAL_ONLY;
    }

    public static boolean allowsBlockCreativeExposure(@Nullable CompatibilityObject compatibilityObject) {
        if (compatibilityObject == null) {
            return true;
        }

        SupportResult content = support(compatibilityObject, "content");
        SupportResult presentation = support(compatibilityObject, "presentation");
        SupportResult interaction = support(compatibilityObject, "interaction");
        SupportResult behavior = support(compatibilityObject, "behavior");

        if (isUnsupported(content) || isUnsupported(presentation) || isUnsupported(behavior)) {
            return false;
        }

        return interaction == null || interaction.supportedCapabilities().contains("placement");
    }

    public static boolean shouldApplyBlockPlacementBridge(@Nullable CompatibilityObject compatibilityObject) {
        SupportResult interaction = support(compatibilityObject, "interaction");
        return interaction == null || interaction.supportedCapabilities().contains("placement");
    }

    public static boolean supportsWearableItemPresentation(@Nullable CompatibilityObject compatibilityObject) {
        if (compatibilityObject == null) {
            return true;
        }

        SupportResult content = support(compatibilityObject, "content");
        SupportResult presentation = support(compatibilityObject, "presentation");
        return !isUnsupported(content) && !isUnsupported(presentation);
    }

    @Nullable
    public static String creativeExposureReason(@Nullable CompatibilityObject compatibilityObject) {
        if (compatibilityObject == null || allowsBlockCreativeExposure(compatibilityObject)) {
            return null;
        }

        SupportResult behavior = support(compatibilityObject, "behavior");
        if (isUnsupported(behavior)) {
            return "behavior domain is unsupported";
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

    @Nullable
    private static SupportResult support(@Nullable CompatibilityObject compatibilityObject, @NotNull String domain) {
        return compatibilityObject != null ? compatibilityObject.supportResults().get(domain) : null;
    }
}