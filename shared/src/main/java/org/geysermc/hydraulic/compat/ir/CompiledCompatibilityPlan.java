package org.geysermc.hydraulic.compat.ir;

import org.geysermc.hydraulic.compat.CompatibilityStatus;
import org.geysermc.hydraulic.compat.adapter.AdapterBinding;
import org.geysermc.hydraulic.compat.model.Confidence;
import org.geysermc.hydraulic.compat.model.SupportLevel;
import org.geysermc.hydraulic.compat.runtime.BlockEntityPatchTemplate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record CompiledCompatibilityPlan(
    @NotNull String modId,
    @NotNull String contentType,
    @NotNull String javaIdentifier,
    @Nullable String resolvedIdentifier,
    @NotNull SupportLevel overallLevel,
    @NotNull CompatibilityStatus overallStatus,
    int overallScore,
    @NotNull Confidence confidence,
    @NotNull List<AdapterBinding> adapterBindings,
    @NotNull List<String> runtimeRequirements,
    @NotNull Map<String, String> inventoryFacts,
    boolean allowsCreativeExposure,
    @Nullable String creativeExposureReason,
    boolean allowsCustomRegistration,
    @Nullable String customRegistrationReason,
    boolean supportsBlockItemTextureFallback,
    boolean supportsBlockPlacement,
    boolean supportsWearablePresentation,
    boolean supportsAttachablePresentation,
    boolean requiresMenuBridge,
    @Nullable String menuFallbackContainerType,
    @NotNull List<String> menuRuntimeRequirements,
    @NotNull List<String> blockEntityRuntimeRequirements,
    @Nullable BlockEntityPatchTemplate blockEntityPatchTemplate,
    boolean requiresBlockEntityRuntime,
    @Nullable SupportLevel behaviorLevel,
    @Nullable String behaviorTag
) {
    public CompiledCompatibilityPlan {
        adapterBindings = List.copyOf(adapterBindings);
        runtimeRequirements = List.copyOf(runtimeRequirements);
        inventoryFacts = Collections.unmodifiableMap(new LinkedHashMap<>(inventoryFacts));
        menuRuntimeRequirements = List.copyOf(menuRuntimeRequirements);
        blockEntityRuntimeRequirements = List.copyOf(blockEntityRuntimeRequirements);
    }

    public boolean hasMenuFallback() {
        return this.menuFallbackContainerType != null;
    }

    public boolean hasBlockEntityPatch() {
        return this.blockEntityPatchTemplate != null;
    }
}