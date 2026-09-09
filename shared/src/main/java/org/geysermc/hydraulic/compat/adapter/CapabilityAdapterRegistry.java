package org.geysermc.hydraulic.compat.adapter;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;
import org.geysermc.hydraulic.compat.model.CompatibilityObject;
import org.geysermc.hydraulic.compat.model.SupportLevel;
import org.geysermc.hydraulic.compat.model.SupportResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class CapabilityAdapterRegistry {
    private static final List<CapabilityAdapter> ADAPTERS = List.of(
        new BlockTextureFallbackAdapter(),
        new BlockPlacementAdapter(),
        new BlockCreativeExposureAdapter(),
        new MenuFallbackAdapter(),
        new BlockEntityPatchAdapter(),
        new CustomItemRegistrationAdapter(),
        new WearableItemAdapter(),
        new BowItemAdapter(),
        new EntityDefinitionAdapter()
    );

    private CapabilityAdapterRegistry() {
    }

    public static boolean supports(@NotNull AdapterFeature feature, @Nullable CompatibilityObject compatibilityObject, @Nullable Object runtimeObject) {
        return binding(feature, compatibilityObject, runtimeObject).isPresent();
    }

    public static @NotNull List<AdapterBinding> bindings(@Nullable CompatibilityObject compatibilityObject) {
        return bindings(compatibilityObject, null);
    }

    public static @NotNull List<AdapterBinding> bindings(@Nullable CompatibilityObject compatibilityObject, @Nullable Object runtimeObject) {
        if (compatibilityObject == null) {
            return List.of();
        }

        return relevantFeatures(compatibilityObject.contentType()).stream()
            .map(feature -> binding(feature, compatibilityObject, runtimeObject))
            .flatMap(Optional::stream)
            .toList();
    }

    public static @NotNull Optional<AdapterBinding> binding(@NotNull AdapterFeature feature, @Nullable CompatibilityObject compatibilityObject, @Nullable Object runtimeObject) {
        if (compatibilityObject == null) {
            return Optional.empty();
        }

        return ADAPTERS.stream()
            .filter(adapter -> adapter.features().contains(feature))
            .filter(adapter -> adapter.supports(compatibilityObject, runtimeObject, feature))
            .sorted(Comparator.comparingInt(CapabilityAdapter::priority).reversed())
            .findFirst()
            .map(adapter -> new AdapterBinding(adapter.id(), feature, adapter.reason(compatibilityObject, runtimeObject, feature)));
    }

    private static @NotNull List<AdapterFeature> relevantFeatures(@NotNull String contentType) {
        return switch (contentType) {
            case "block" -> List.of(
                AdapterFeature.BLOCK_ITEM_TEXTURE_FALLBACK,
                AdapterFeature.BLOCK_PLACEMENT,
                AdapterFeature.BLOCK_CREATIVE_EXPOSURE
            );
            case "menu" -> List.of(AdapterFeature.MENU_FALLBACK_TRANSLATION);
            case "block_entity" -> List.of(AdapterFeature.BLOCK_ENTITY_PATCH_TRANSLATION);
            case "item" -> List.of(
                AdapterFeature.CUSTOM_ITEM_REGISTRATION,
                AdapterFeature.WEARABLE_ITEM_PRESENTATION,
                AdapterFeature.ATTACHABLE_ITEM_PRESENTATION,
                AdapterFeature.ITEM_CREATIVE_EXPOSURE
            );
            case "entity" -> List.of(AdapterFeature.CUSTOM_ENTITY_REGISTRATION);
            default -> List.of();
        };
    }

    private static boolean contentSupported(@NotNull CompatibilityObject object) {
        return !isUnsupported(object.supportResults().get("content"));
    }

    private static boolean presentationSupported(@NotNull CompatibilityObject object) {
        return !isUnsupported(object.supportResults().get("presentation"));
    }

    private static boolean behaviorDegraded(@NotNull CompatibilityObject object) {
        SupportResult behavior = object.supportResults().get("behavior");
        return behavior != null && (behavior.level() == SupportLevel.UNSUPPORTED || behavior.level() == SupportLevel.APPROXIMATED);
    }

    private static boolean supportsCapability(@NotNull CompatibilityObject object, @NotNull String domain, @NotNull String capability) {
        SupportResult supportResult = object.supportResults().get(domain);
        return supportResult != null && supportResult.supportedCapabilities().contains(capability);
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

    private static boolean isUnsupported(@Nullable SupportResult supportResult) {
        return supportResult != null && supportResult.level() == SupportLevel.UNSUPPORTED;
    }

    private static final class BlockTextureFallbackAdapter implements CapabilityAdapter {
        @Override
        public @NotNull String id() {
            return "block.texture_fallback";
        }

        @Override
        public @NotNull Set<AdapterFeature> features() {
            return Set.of(AdapterFeature.BLOCK_ITEM_TEXTURE_FALLBACK);
        }

        @Override
        public int priority() {
            return 10;
        }

        @Override
        public boolean supports(@NotNull CompatibilityObject compatibilityObject, @Nullable Object runtimeObject, @NotNull AdapterFeature feature) {
            return contentSupported(compatibilityObject) && presentationSupported(compatibilityObject);
        }

        @Override
        public @NotNull String reason(@NotNull CompatibilityObject compatibilityObject, @Nullable Object runtimeObject, @NotNull AdapterFeature feature) {
            return "existing block-model to item-texture bridge applies";
        }
    }

    private static final class BlockPlacementAdapter implements CapabilityAdapter {
        @Override
        public @NotNull String id() {
            return "block.placement";
        }

        @Override
        public @NotNull Set<AdapterFeature> features() {
            return Set.of(AdapterFeature.BLOCK_PLACEMENT);
        }

        @Override
        public int priority() {
            return 20;
        }

        @Override
        public boolean supports(@NotNull CompatibilityObject compatibilityObject, @Nullable Object runtimeObject, @NotNull AdapterFeature feature) {
            return supportsCapability(compatibilityObject, "interaction", "placement")
                || hasBehaviorTag(compatibilityObject, "block_placement", "placeable_block");
        }

        @Override
        public @NotNull String reason(@NotNull CompatibilityObject compatibilityObject, @Nullable Object runtimeObject, @NotNull AdapterFeature feature) {
            return "existing custom item block placer bridge applies";
        }
    }

    private static final class BlockCreativeExposureAdapter implements CapabilityAdapter {
        @Override
        public @NotNull String id() {
            return "block.creative_exposure";
        }

        @Override
        public @NotNull Set<AdapterFeature> features() {
            return Set.of(AdapterFeature.BLOCK_CREATIVE_EXPOSURE);
        }

        @Override
        public int priority() {
            return 5;
        }

        @Override
        public boolean supports(@NotNull CompatibilityObject compatibilityObject, @Nullable Object runtimeObject, @NotNull AdapterFeature feature) {
            if (!contentSupported(compatibilityObject) || !presentationSupported(compatibilityObject)) {
                return false;
            }
            if (!CapabilityAdapterRegistry.supports(AdapterFeature.BLOCK_PLACEMENT, compatibilityObject, runtimeObject)) {
                return false;
            }
            return !behaviorDegraded(compatibilityObject)
                || hasBehaviorTag(compatibilityObject, "block_placement", "placeable_block", "visual_only_runtime");
        }

        @Override
        public @NotNull String reason(@NotNull CompatibilityObject compatibilityObject, @Nullable Object runtimeObject, @NotNull AdapterFeature feature) {
            return "block registration remains safe for Bedrock creative exposure";
        }
    }

    private static final class CustomItemRegistrationAdapter implements CapabilityAdapter {
        @Override
        public @NotNull String id() {
            return "item.custom_registration";
        }

        @Override
        public @NotNull Set<AdapterFeature> features() {
            return Set.of(AdapterFeature.CUSTOM_ITEM_REGISTRATION);
        }

        @Override
        public int priority() {
            return 10;
        }

        @Override
        public boolean supports(@NotNull CompatibilityObject compatibilityObject, @Nullable Object runtimeObject, @NotNull AdapterFeature feature) {
            return contentSupported(compatibilityObject) && presentationSupported(compatibilityObject);
        }

        @Override
        public @NotNull String reason(@NotNull CompatibilityObject compatibilityObject, @Nullable Object runtimeObject, @NotNull AdapterFeature feature) {
            return "existing Geyser custom item registration bridge applies";
        }
    }

    private static final class MenuFallbackAdapter implements CapabilityAdapter {
        @Override
        public @NotNull String id() {
            return "menu.fallback_translator";
        }

        @Override
        public @NotNull Set<AdapterFeature> features() {
            return Set.of(AdapterFeature.MENU_FALLBACK_TRANSLATION);
        }

        @Override
        public int priority() {
            return 25;
        }

        @Override
        public boolean supports(@NotNull CompatibilityObject compatibilityObject, @Nullable Object runtimeObject, @NotNull AdapterFeature feature) {
            return contentSupported(compatibilityObject)
                && supportsCapability(compatibilityObject, "interaction", "container_interaction");
        }

        @Override
        public @NotNull String reason(@NotNull CompatibilityObject compatibilityObject, @Nullable Object runtimeObject, @NotNull AdapterFeature feature) {
            return "existing metadata-backed menu fallback translator applies";
        }
    }

    private static final class BlockEntityPatchAdapter implements CapabilityAdapter {
        @Override
        public @NotNull String id() {
            return "block_entity.patch_translator";
        }

        @Override
        public @NotNull Set<AdapterFeature> features() {
            return Set.of(AdapterFeature.BLOCK_ENTITY_PATCH_TRANSLATION);
        }

        @Override
        public int priority() {
            return 25;
        }

        @Override
        public boolean supports(@NotNull CompatibilityObject compatibilityObject, @Nullable Object runtimeObject, @NotNull AdapterFeature feature) {
            return contentSupported(compatibilityObject)
                && supportsCapability(compatibilityObject, "state_data", "persistent_data");
        }

        @Override
        public @NotNull String reason(@NotNull CompatibilityObject compatibilityObject, @Nullable Object runtimeObject, @NotNull AdapterFeature feature) {
            return "existing metadata-backed block entity patch translator applies";
        }
    }

    private static final class WearableItemAdapter implements CapabilityAdapter {
        @Override
        public @NotNull String id() {
            return "item.wearable_attachable";
        }

        @Override
        public @NotNull Set<AdapterFeature> features() {
            return Set.of(AdapterFeature.WEARABLE_ITEM_PRESENTATION, AdapterFeature.ITEM_CREATIVE_EXPOSURE);
        }

        @Override
        public int priority() {
            return 30;
        }

        @Override
        public boolean supports(@NotNull CompatibilityObject compatibilityObject, @Nullable Object runtimeObject, @NotNull AdapterFeature feature) {
            if (!contentSupported(compatibilityObject) || !presentationSupported(compatibilityObject)) {
                return false;
            }

            return hasBehaviorTag(compatibilityObject, "wearable", "wearable_attachable")
                || runtimeObject instanceof Item item
                && item.components().has(DataComponents.EQUIPPABLE)
                && item.components().get(DataComponents.EQUIPPABLE).assetId().isPresent();
        }

        @Override
        public @NotNull String reason(@NotNull CompatibilityObject compatibilityObject, @Nullable Object runtimeObject, @NotNull AdapterFeature feature) {
            return "existing armor attachable bridge applies";
        }
    }

    private static final class BowItemAdapter implements CapabilityAdapter {
        @Override
        public @NotNull String id() {
            return "item.bow_attachable";
        }

        @Override
        public @NotNull Set<AdapterFeature> features() {
            return Set.of(AdapterFeature.ATTACHABLE_ITEM_PRESENTATION, AdapterFeature.ITEM_CREATIVE_EXPOSURE);
        }

        @Override
        public int priority() {
            return 30;
        }

        @Override
        public boolean supports(@NotNull CompatibilityObject compatibilityObject, @Nullable Object runtimeObject, @NotNull AdapterFeature feature) {
            if (!contentSupported(compatibilityObject) || !presentationSupported(compatibilityObject)) {
                return false;
            }

            return hasBehaviorTag(compatibilityObject, "chargeable_bow", "bow_attachable") || runtimeObject instanceof BowItem;
        }

        @Override
        public @NotNull String reason(@NotNull CompatibilityObject compatibilityObject, @Nullable Object runtimeObject, @NotNull AdapterFeature feature) {
            return "existing bow attachable bridge applies";
        }
    }

    private static final class EntityDefinitionAdapter implements CapabilityAdapter {
        @Override
        public @NotNull String id() {
            return "entity.custom_definition";
        }

        @Override
        public @NotNull Set<AdapterFeature> features() {
            return Set.of(AdapterFeature.CUSTOM_ENTITY_REGISTRATION);
        }

        @Override
        public int priority() {
            return 10;
        }

        @Override
        public boolean supports(@NotNull CompatibilityObject compatibilityObject, @Nullable Object runtimeObject, @NotNull AdapterFeature feature) {
            return contentSupported(compatibilityObject) && presentationSupported(compatibilityObject);
        }

        @Override
        public @NotNull String reason(@NotNull CompatibilityObject compatibilityObject, @Nullable Object runtimeObject, @NotNull AdapterFeature feature) {
            return "existing metadata-backed custom entity definition bridge applies";
        }
    }
}
