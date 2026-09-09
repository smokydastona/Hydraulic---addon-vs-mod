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
import java.util.concurrent.atomic.AtomicLong;

public final class RuntimeDispatchTable {
    private final Map<String, CompiledCompatibilityPlan> plansByTypeAndIdentifier;
    private final Map<String, List<CompiledCompatibilityPlan>> plansByModAndType;
    private final Map<String, List<MappingResolver.ResolvedBlockDefinition>> blockDefinitionsByIdentifier;
    private final Map<String, MappingResolver.ResolvedBlockState> blockStatesByIdentifierAndState;
    private final List<CompiledCompatibilityPlan> menuBridgePlans;
    private final List<CompiledCompatibilityPlan> blockEntityBridgePlans;
    private final Map<String, LookupCounters> countersByType;

    private RuntimeDispatchTable(
        @NotNull Map<String, CompiledCompatibilityPlan> plansByTypeAndIdentifier,
        @NotNull Map<String, List<CompiledCompatibilityPlan>> plansByModAndType,
        @NotNull Map<String, List<MappingResolver.ResolvedBlockDefinition>> blockDefinitionsByIdentifier,
        @NotNull Map<String, MappingResolver.ResolvedBlockState> blockStatesByIdentifierAndState,
        @NotNull List<CompiledCompatibilityPlan> menuBridgePlans,
        @NotNull List<CompiledCompatibilityPlan> blockEntityBridgePlans
    ) {
        this.plansByTypeAndIdentifier = Collections.unmodifiableMap(new LinkedHashMap<>(plansByTypeAndIdentifier));
        Map<String, List<CompiledCompatibilityPlan>> copy = new LinkedHashMap<>();
        for (Map.Entry<String, List<CompiledCompatibilityPlan>> entry : plansByModAndType.entrySet()) {
            copy.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        this.plansByModAndType = Collections.unmodifiableMap(copy);
        Map<String, List<MappingResolver.ResolvedBlockDefinition>> blockDefinitionCopy = new LinkedHashMap<>();
        for (Map.Entry<String, List<MappingResolver.ResolvedBlockDefinition>> entry : blockDefinitionsByIdentifier.entrySet()) {
            blockDefinitionCopy.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        this.blockDefinitionsByIdentifier = Collections.unmodifiableMap(blockDefinitionCopy);
        this.blockStatesByIdentifierAndState = Collections.unmodifiableMap(new LinkedHashMap<>(blockStatesByIdentifierAndState));
        this.menuBridgePlans = List.copyOf(menuBridgePlans);
        this.blockEntityBridgePlans = List.copyOf(blockEntityBridgePlans);
        this.countersByType = Map.of(
            "block", new LookupCounters(),
            "item", new LookupCounters(),
            "entity", new LookupCounters(),
            "menu", new LookupCounters(),
            "block_entity", new LookupCounters()
        );
    }

    @NotNull
    public static RuntimeDispatchTable empty() {
        return new RuntimeDispatchTable(Map.of(), Map.of(), Map.of(), Map.of(), List.of(), List.of());
    }

    @NotNull
    public static RuntimeDispatchTable compile(@NotNull CompatibilityReport report, @NotNull MappingResolver mappingResolver) {
        Map<String, CompiledCompatibilityPlan> plansByIdentifier = new LinkedHashMap<>();
        Map<String, List<CompiledCompatibilityPlan>> plansByModAndType = new LinkedHashMap<>();
        Map<String, List<MappingResolver.ResolvedBlockDefinition>> blockDefinitionsByIdentifier = new LinkedHashMap<>();
        Map<String, MappingResolver.ResolvedBlockState> blockStatesByIdentifierAndState = new LinkedHashMap<>();
        List<CompiledCompatibilityPlan> menuBridgePlans = new ArrayList<>();
        List<CompiledCompatibilityPlan> blockEntityBridgePlans = new ArrayList<>();
        for (CompatibilityProfile profile : report.mods().values()) {
            for (CompatibilityObject object : profile.objects()) {
                CompiledCompatibilityPlan plan = compilePlan(object, mappingResolver);
                plansByIdentifier.put(key(object.contentType(), object.javaIdentifier()), plan);
                plansByModAndType.computeIfAbsent(key(profile.modId(), object.contentType()), ignored -> new ArrayList<>()).add(plan);
                compileBlockStatePlans(object, mappingResolver, blockDefinitionsByIdentifier, blockStatesByIdentifierAndState);
                if (plan.requiresMenuBridge()) {
                    menuBridgePlans.add(plan);
                }
                if (plan.requiresBlockEntityRuntime()) {
                    blockEntityBridgePlans.add(plan);
                }
            }
        }
        menuBridgePlans.sort(planComparator());
        blockEntityBridgePlans.sort(planComparator());
        return new RuntimeDispatchTable(
            plansByIdentifier,
            plansByModAndType,
            blockDefinitionsByIdentifier,
            blockStatesByIdentifierAndState,
            menuBridgePlans,
            blockEntityBridgePlans
        );
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
        CompiledCompatibilityPlan plan = this.plansByTypeAndIdentifier.get(key(contentType, javaIdentifier));
        this.recordLookup(contentType, plan != null);
        return plan;
    }

    @NotNull
    public List<CompiledCompatibilityPlan> plans(@NotNull String modId, @NotNull String contentType) {
        List<CompiledCompatibilityPlan> plans = this.plansByModAndType.getOrDefault(key(modId, contentType), List.of());
        this.recordLookup(contentType, !plans.isEmpty());
        return plans;
    }

    @NotNull
    public List<CompiledCompatibilityPlan> entityPlans(@NotNull String modId) {
        return this.plans(modId, "entity");
    }

    @NotNull
    public List<MappingResolver.ResolvedBlockDefinition> blockDefinitions(@NotNull Identifier javaIdentifier) {
        List<MappingResolver.ResolvedBlockDefinition> definitions = this.blockDefinitionsByIdentifier.getOrDefault(javaIdentifier.toString(), List.of());
        this.recordLookup("block", !definitions.isEmpty());
        return definitions;
    }

    @Nullable
    public MappingResolver.ResolvedBlockState blockState(@NotNull Identifier javaIdentifier, @NotNull net.minecraft.world.level.block.state.BlockState state) {
        MappingResolver.ResolvedBlockState resolvedState = this.blockStatesByIdentifierAndState.get(blockStateKey(javaIdentifier.toString(), Block.getId(state)));
        this.recordLookup("block", resolvedState != null);
        return resolvedState;
    }

    @NotNull
    public List<CompiledCompatibilityPlan> menuBridgePlans() {
        return this.menuBridgePlans;
    }

    @NotNull
    public List<CompiledCompatibilityPlan> blockEntityBridgePlans() {
        return this.blockEntityBridgePlans;
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
        boolean requiresMenuBridge = object.runtimeRequirements().contains("container_bridge");
        List<String> menuRuntimeRequirements = object.runtimeRequirements().stream()
            .filter(requirement -> requirement.equals("container_bridge") || requirement.startsWith("menu_"))
            .sorted()
            .toList();
        List<String> blockEntityRuntimeRequirements = object.runtimeRequirements().stream()
            .filter(requirement -> requirement.startsWith("block_entity_"))
            .sorted()
            .toList();

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
            requiresMenuBridge,
            menuFallbackContainerType(object, javaIdentifier, mappingResolver),
            menuRuntimeRequirements,
            blockEntityRuntimeRequirements,
            blockEntityPatchTemplate(object, javaIdentifier, mappingResolver),
            !blockEntityRuntimeRequirements.isEmpty(),
            behaviorLevel,
            behaviorTag
        );
    }

    @NotNull
    private static java.util.Comparator<CompiledCompatibilityPlan> planComparator() {
        return java.util.Comparator
            .comparing(CompiledCompatibilityPlan::modId)
            .thenComparing(CompiledCompatibilityPlan::javaIdentifier);
    }

    private static void compileBlockStatePlans(
        @NotNull CompatibilityObject object,
        @NotNull MappingResolver mappingResolver,
        @NotNull Map<String, List<MappingResolver.ResolvedBlockDefinition>> blockDefinitionsByIdentifier,
        @NotNull Map<String, MappingResolver.ResolvedBlockState> blockStatesByIdentifierAndState
    ) {
        if (!"block".equals(object.contentType())) {
            return;
        }

        Identifier javaIdentifier = Identifier.parse(object.javaIdentifier());
        Block block = BuiltInRegistries.BLOCK.getValue(javaIdentifier);
        if (block == null) {
            return;
        }

        Map<Identifier, List<net.minecraft.world.level.block.state.BlockState>> groupedStates = new LinkedHashMap<>();
        Map<Identifier, Boolean> overridden = new LinkedHashMap<>();
        for (net.minecraft.world.level.block.state.BlockState state : block.getStateDefinition().getPossibleStates()) {
            MappingResolver.ResolvedBlockState resolvedState = mappingResolver.resolveBlockState(javaIdentifier, state);
            blockStatesByIdentifierAndState.put(blockStateKey(object.javaIdentifier(), Block.getId(state)), resolvedState);
            groupedStates.computeIfAbsent(resolvedState.identifier(), ignored -> new ArrayList<>()).add(state);
            overridden.merge(resolvedState.identifier(), resolvedState.overridden(), Boolean::logicalOr);
        }

        List<MappingResolver.ResolvedBlockDefinition> definitions = new ArrayList<>();
        for (Map.Entry<Identifier, List<net.minecraft.world.level.block.state.BlockState>> entry : groupedStates.entrySet()) {
            definitions.add(new MappingResolver.ResolvedBlockDefinition(entry.getKey(), List.copyOf(entry.getValue()), overridden.getOrDefault(entry.getKey(), false)));
        }
        blockDefinitionsByIdentifier.put(object.javaIdentifier(), List.copyOf(definitions));
    }

    @NotNull
    private static String blockStateKey(@NotNull String javaIdentifier, int stateId) {
        return javaIdentifier + '|' + stateId;
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

    @NotNull
    public org.geysermc.hydraulic.pack.PerformanceReport.RuntimeDispatchMetrics metrics() {
        return new org.geysermc.hydraulic.pack.PerformanceReport.RuntimeDispatchMetrics(
            this.cacheMetrics("block"),
            this.cacheMetrics("item"),
            this.cacheMetrics("entity"),
            this.cacheMetrics("menu"),
            this.cacheMetrics("block_entity")
        );
    }

    private void recordLookup(@NotNull String contentType, boolean hit) {
        LookupCounters counters = this.countersByType.get(contentType);
        if (counters == null) {
            return;
        }
        counters.record(hit);
    }

    @NotNull
    private org.geysermc.hydraulic.pack.PerformanceReport.CacheMetrics cacheMetrics(@NotNull String contentType) {
        LookupCounters counters = this.countersByType.get(contentType);
        return counters == null
            ? new org.geysermc.hydraulic.pack.PerformanceReport.CacheMetrics(0, 0)
            : new org.geysermc.hydraulic.pack.PerformanceReport.CacheMetrics(counters.hits(), counters.misses());
    }

    private static final class LookupCounters {
        private final AtomicLong hits = new AtomicLong();
        private final AtomicLong misses = new AtomicLong();

        private void record(boolean hit) {
            if (hit) {
                this.hits.incrementAndGet();
            } else {
                this.misses.incrementAndGet();
            }
        }

        private long hits() {
            return this.hits.get();
        }

        private long misses() {
            return this.misses.get();
        }
    }
}