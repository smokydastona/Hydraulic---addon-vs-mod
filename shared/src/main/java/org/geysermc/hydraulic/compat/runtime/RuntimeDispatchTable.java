package org.geysermc.hydraulic.compat.runtime;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.geysermc.hydraulic.compat.CompatibilityProfile;
import org.geysermc.hydraulic.compat.CompatibilityRegistry;
import org.geysermc.hydraulic.compat.CompatibilityReport;
import org.geysermc.hydraulic.compat.MappingResolver;
import org.geysermc.hydraulic.compat.ir.CompiledCompatibilityPlan;
import org.geysermc.hydraulic.compat.model.CompatibilityObject;
import org.geysermc.hydraulic.compat.model.SupportLevel;
import org.geysermc.hydraulic.compat.model.SupportResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RuntimeDispatchTable {
    private final Map<String, CompiledCompatibilityPlan> plansByTypeAndIdentifier;
    private final Map<String, List<CompiledCompatibilityPlan>> plansByModAndType;

    private RuntimeDispatchTable(
        @NotNull Map<String, CompiledCompatibilityPlan> plansByTypeAndIdentifier,
        @NotNull Map<String, List<CompiledCompatibilityPlan>> plansByModAndType
    ) {
        this.plansByTypeAndIdentifier = Collections.unmodifiableMap(new LinkedHashMap<>(plansByTypeAndIdentifier));
        Map<String, List<CompiledCompatibilityPlan>> copy = new LinkedHashMap<>();
        for (Map.Entry<String, List<CompiledCompatibilityPlan>> entry : plansByModAndType.entrySet()) {
            copy.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        this.plansByModAndType = Collections.unmodifiableMap(copy);
    }

    @NotNull
    public static RuntimeDispatchTable empty() {
        return new RuntimeDispatchTable(Map.of(), Map.of());
    }

    @NotNull
    public static RuntimeDispatchTable compile(@NotNull CompatibilityReport report, @NotNull MappingResolver mappingResolver) {
        Map<String, CompiledCompatibilityPlan> plansByIdentifier = new LinkedHashMap<>();
        Map<String, List<CompiledCompatibilityPlan>> plansByModAndType = new LinkedHashMap<>();
        for (CompatibilityProfile profile : report.mods().values()) {
            for (CompatibilityObject object : profile.objects()) {
                CompiledCompatibilityPlan plan = compilePlan(object, mappingResolver);
                plansByIdentifier.put(key(object.contentType(), object.javaIdentifier()), plan);
                plansByModAndType.computeIfAbsent(key(profile.modId(), object.contentType()), ignored -> new ArrayList<>()).add(plan);
            }
        }
        return new RuntimeDispatchTable(plansByIdentifier, plansByModAndType);
    }

    @Nullable
    public CompiledCompatibilityPlan block(@NotNull Identifier javaIdentifier) {
        return this.plan("block", javaIdentifier.toString());
    }

    @Nullable
    public CompiledCompatibilityPlan item(@NotNull Identifier javaIdentifier) {
        return this.plan("item", javaIdentifier.toString());
    }

    @Nullable
    public CompiledCompatibilityPlan entity(@NotNull Identifier javaIdentifier) {
        return this.plan("entity", javaIdentifier.toString());
    }

    @Nullable
    public CompiledCompatibilityPlan menu(@NotNull Identifier javaIdentifier) {
        return this.plan("menu", javaIdentifier.toString());
    }

    @Nullable
    public CompiledCompatibilityPlan blockEntity(@NotNull Identifier javaIdentifier) {
        return this.plan("block_entity", javaIdentifier.toString());
    }

    @Nullable
    public CompiledCompatibilityPlan plan(@NotNull String contentType, @NotNull String javaIdentifier) {
        return this.plansByTypeAndIdentifier.get(key(contentType, javaIdentifier));
    }

    @NotNull
    public List<CompiledCompatibilityPlan> plans(@NotNull String modId, @NotNull String contentType) {
        return this.plansByModAndType.getOrDefault(key(modId, contentType), List.of());
    }

    @NotNull
    public List<CompiledCompatibilityPlan> entityPlans(@NotNull String modId) {
        return this.plans(modId, "entity");
    }

    @NotNull
    private static CompiledCompatibilityPlan compilePlan(@NotNull CompatibilityObject object, @NotNull MappingResolver mappingResolver) {
        Identifier javaIdentifier = Identifier.parse(object.javaIdentifier());
        Block block = "block".equals(object.contentType()) ? BuiltInRegistries.BLOCK.getValue(javaIdentifier) : null;
        Item item = "item".equals(object.contentType()) ? BuiltInRegistries.ITEM.getValue(javaIdentifier) : null;

        String resolvedIdentifier = switch (object.contentType()) {
            case "item" -> mappingResolver.resolveItemIdentifier(javaIdentifier).identifier().toString();
            case "entity" -> mappingResolver.resolveEntityIdentifier(javaIdentifier).identifier().toString();
            case "menu" -> mappingResolver.resolveMenuIdentifier(javaIdentifier).identifier().toString();
            default -> javaIdentifier.toString();
        };

        boolean supportsWearablePresentation = supportsWearablePresentation(object, item);
        boolean supportsAttachablePresentation = supportsAttachablePresentation(object, item);

        boolean allowsItemCreativeExposure = allowsItemCreativeExposure(object, item, supportsWearablePresentation, supportsAttachablePresentation);
        String creativeExposureReason = switch (object.contentType()) {
            case "block" -> CompatibilityDecisions.creativeExposureReason(object);
            case "item" -> itemCreativeExposureReason(object, allowsItemCreativeExposure);
            default -> null;
        };

        boolean allowsCreativeExposure = switch (object.contentType()) {
            case "block" -> CompatibilityDecisions.allowsBlockCreativeExposure(object, block);
            case "item" -> allowsItemCreativeExposure;
            default -> false;
        };

        boolean allowsCustomRegistration = switch (object.contentType()) {
            case "item" -> CompatibilityDecisions.allowsCustomItemRegistration(object, item);
            case "entity" -> CompatibilityDecisions.allowsCustomEntityRegistration(object);
            default -> false;
        };

        String customRegistrationReason = switch (object.contentType()) {
            case "entity" -> CompatibilityDecisions.entityRegistrationReason(object);
            default -> null;
        };

        SupportLevel behaviorLevel = object.supportResults().containsKey("behavior") ? object.supportResults().get("behavior").level() : null;
        String behaviorTag = object.inventoryFacts().get("behavior_tag");

        return new CompiledCompatibilityPlan(
            object.modId(),
            object.contentType(),
            object.javaIdentifier(),
            resolvedIdentifier,
            object.overallLevel(),
            object.overallStatus(),
            object.overallScore(),
            object.confidence(),
            object.adapterBindings(),
            object.runtimeRequirements(),
            object.inventoryFacts(),
            allowsCreativeExposure,
            creativeExposureReason,
            allowsCustomRegistration,
            customRegistrationReason,
            CompatibilityDecisions.supportsBlockItemTextureFallback(object),
            CompatibilityDecisions.shouldApplyBlockPlacementBridge(object, block),
            supportsWearablePresentation,
            supportsAttachablePresentation,
            menuFallbackContainerType(object, javaIdentifier, mappingResolver),
            blockEntityPatchTemplate(object, javaIdentifier, mappingResolver),
            behaviorLevel,
            behaviorTag
        );
    }

    @Nullable
    private static String menuFallbackContainerType(
        @NotNull CompatibilityObject object,
        @NotNull Identifier javaIdentifier,
        @NotNull MappingResolver mappingResolver
    ) {
        if (!"menu".equals(object.contentType())) {
            return null;
        }

        MenuPatchTemplate template = mappingResolver.menuPatchTemplate(javaIdentifier);
        return template != null ? template.fallbackContainerType() : null;
    }

    @Nullable
    private static BlockEntityPatchTemplate blockEntityPatchTemplate(
        @NotNull CompatibilityObject object,
        @NotNull Identifier javaIdentifier,
        @NotNull MappingResolver mappingResolver
    ) {
        if (!"block_entity".equals(object.contentType())) {
            return null;
        }
        return mappingResolver.blockEntityPatchTemplate(javaIdentifier);
    }

    @NotNull
    private static String key(@NotNull String left, @NotNull String right) {
        return left + "|" + right;
    }

    private static boolean supportsWearablePresentation(@NotNull CompatibilityObject object, @Nullable Item item) {
        if (isUnsupported(object, "content") || isUnsupported(object, "presentation")) {
            return false;
        }
        return hasBehaviorTag(object, "wearable", "wearable_attachable") || hasEquippableAsset(item);
    }

    private static boolean supportsAttachablePresentation(@NotNull CompatibilityObject object, @Nullable Item item) {
        if (isUnsupported(object, "content") || isUnsupported(object, "presentation")) {
            return false;
        }
        return hasBehaviorTag(object, "chargeable_bow", "bow_attachable") || item instanceof BowItem || !isUnsupported(object, "behavior");
    }

    private static boolean allowsItemCreativeExposure(
        @NotNull CompatibilityObject object,
        @Nullable Item item,
        boolean supportsWearablePresentation,
        boolean supportsAttachablePresentation
    ) {
        if (!CompatibilityDecisions.allowsCustomItemRegistration(object, item)) {
            return false;
        }

        SupportResult behavior = object.supportResults().get("behavior");
        if (behavior == null || (behavior.level() != SupportLevel.UNSUPPORTED && behavior.level() != SupportLevel.APPROXIMATED)) {
            return true;
        }

        return supportsWearablePresentation || supportsAttachablePresentation;
    }

    @Nullable
    private static String itemCreativeExposureReason(@NotNull CompatibilityObject object, boolean allowsItemCreativeExposure) {
        if (allowsItemCreativeExposure) {
            return null;
        }

        if (!CompatibilityDecisions.allowsCustomItemRegistration(object)) {
            return "content or presentation support is insufficient";
        }

        SupportResult behavior = object.supportResults().get("behavior");
        if (behavior != null && behavior.level() == SupportLevel.UNSUPPORTED) {
            return behaviorReason("behavior domain is unsupported", object);
        }
        if (behavior != null && behavior.level() == SupportLevel.APPROXIMATED) {
            return behaviorReason("behavior domain is approximated", object);
        }
        return "compatibility evidence is insufficient";
    }

    @Nullable
    private static String behaviorReason(@NotNull String defaultReason, @NotNull CompatibilityObject object) {
        String behaviorTag = object.inventoryFacts().get("behavior_tag");
        if (behaviorTag == null || behaviorTag.isBlank()) {
            return defaultReason;
        }
        return defaultReason + " (tag: " + behaviorTag + ")";
    }

    private static boolean isUnsupported(@NotNull CompatibilityObject object, @NotNull String domain) {
        SupportResult supportResult = object.supportResults().get(domain);
        return supportResult != null && supportResult.level() == SupportLevel.UNSUPPORTED;
    }

    private static boolean hasBehaviorTag(@NotNull CompatibilityObject object, @NotNull String... tags) {
        String behaviorTag = object.inventoryFacts().get("behavior_tag");
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

    private static boolean hasEquippableAsset(@Nullable Item item) {
        if (item == null) {
            return false;
        }

        try {
            return item.components().has(DataComponents.EQUIPPABLE)
                && item.components().get(DataComponents.EQUIPPABLE).assetId().isPresent();
        } catch (NullPointerException ignored) {
            return false;
        }
    }
}