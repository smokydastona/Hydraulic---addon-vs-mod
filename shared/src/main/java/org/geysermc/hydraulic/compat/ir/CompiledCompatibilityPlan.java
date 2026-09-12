package org.geysermc.hydraulic.compat.ir;

import org.geysermc.mcprotocollib.protocol.data.game.inventory.ContainerType;
import org.geysermc.hydraulic.compat.CompatibilityStatus;
import org.geysermc.hydraulic.compat.adapter.AdapterBinding;
import org.geysermc.hydraulic.compat.adapter.AdapterFeature;
import org.geysermc.hydraulic.compat.model.Confidence;
import org.geysermc.hydraulic.compat.model.SupportLevel;
import org.geysermc.hydraulic.compat.runtime.BlockEntityPatchTemplate;
import org.geysermc.hydraulic.compat.runtime.ContainerArchetype;
import org.geysermc.hydraulic.compat.runtime.RuntimeBridgeKind;
import org.geysermc.hydraulic.compat.runtime.SlotRole;
import org.geysermc.hydraulic.compat.model.CompatibilityContract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.EnumMap;
import java.util.ArrayList;

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
    @NotNull List<RuntimeBridgeKind> runtimeBridgeKinds,
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
    @Nullable ContainerType menuFallbackContainerType,
    @Nullable String interactionPrompt,
    @NotNull List<String> menuRuntimeRequirements,
    @NotNull List<String> blockEntityRuntimeRequirements,
    @NotNull List<String> fluidRuntimeRequirements,
    @Nullable BlockEntityPatchTemplate blockEntityPatchTemplate,
    boolean requiresBlockEntityRuntime,
    boolean requiresFluidRuntime,
    @Nullable SupportLevel behaviorLevel,
    @Nullable String behaviorTag,
    @NotNull List<CorpusEvidenceRef> corpusEvidence
) {
    public CompiledCompatibilityPlan {
        adapterBindings = List.copyOf(adapterBindings);
        runtimeRequirements = List.copyOf(runtimeRequirements);
        runtimeBridgeKinds = List.copyOf(runtimeBridgeKinds);
        inventoryFacts = Collections.unmodifiableMap(new LinkedHashMap<>(inventoryFacts));
        menuRuntimeRequirements = List.copyOf(menuRuntimeRequirements);
        blockEntityRuntimeRequirements = List.copyOf(blockEntityRuntimeRequirements);
        fluidRuntimeRequirements = List.copyOf(fluidRuntimeRequirements);
        corpusEvidence = List.copyOf(corpusEvidence);
    }

    public boolean hasMenuFallback() {
        return this.menuFallbackContainerType != null;
    }

    public boolean hasBlockEntityPatch() {
        return this.blockEntityPatchTemplate != null;
    }

    public boolean hasCorpusEvidence() {
        return !this.corpusEvidence.isEmpty();
    }

    @NotNull
    public List<CorpusEvidenceRef> corpusEvidenceForBridgeKind(@NotNull RuntimeBridgeKind kind) {
        return this.corpusEvidence.stream().filter(ref -> ref.relatedBridgeKind() == kind).toList();
    }

    public boolean supportsAdapterFeature(@NotNull AdapterFeature feature) {
        return this.adapterBindings.stream().anyMatch(binding -> binding.feature() == feature);
    }

    public boolean requiresRuntimeBridge(@NotNull RuntimeBridgeKind kind) {
        return this.runtimeBridgeKinds.contains(kind);
    }

    public boolean hasRuntimeBridges() {
        return !this.runtimeBridgeKinds.isEmpty();
    }

    @NotNull
    public ContainerArchetype containerArchetype() {
        return ContainerArchetype.parse(this.inventoryFacts.get("container.archetype"));
    }

    @NotNull
    public Map<SlotRole, List<Integer>> slotRoles() {
        Map<SlotRole, List<Integer>> roles = new EnumMap<>(SlotRole.class);
        for (SlotRole role : SlotRole.values()) {
            String raw = this.inventoryFacts.get("container.slot." + role.name().toLowerCase());
            if (raw == null || raw.isBlank()) {
                continue;
            }
            List<Integer> slots = new ArrayList<>();
            for (String value : raw.split(",")) {
                try {
                    int slot = Integer.parseInt(value.trim());
                    if (slot >= 0) {
                        slots.add(slot);
                    }
                } catch (NumberFormatException ignored) {
                    return Map.of();
                }
            }
            if (!slots.isEmpty()) {
                roles.put(role, List.copyOf(slots));
            }
        }
        return Map.copyOf(roles);
    }

    @NotNull
    public CompatibilityContract contract() {
        return CompatibilityContract.from(this);
    }

    public @NotNull List<String> runtimeBridgeRequirementIds() {
        return this.runtimeBridgeKinds.stream()
            .map(RuntimeBridgeKind::requirementId)
            .toList();
    }
}