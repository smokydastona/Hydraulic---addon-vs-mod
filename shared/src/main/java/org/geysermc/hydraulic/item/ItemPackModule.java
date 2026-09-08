package org.geysermc.hydraulic.item;

import com.google.auto.service.AutoService;
import net.kyori.adventure.key.Key;
import net.minecraft.core.DefaultedRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCustomItemsEvent;
import org.geysermc.geyser.api.item.custom.v2.CustomItemBedrockOptions;
import org.geysermc.geyser.api.item.custom.v2.NonVanillaCustomItemDefinition;
import org.geysermc.geyser.api.item.custom.v2.component.geyser.GeyserBlockPlacer;
import org.geysermc.geyser.api.item.custom.v2.component.geyser.GeyserChargeable;
import org.geysermc.geyser.api.item.custom.v2.component.geyser.GeyserItemDataComponents;
import org.geysermc.hydraulic.compat.CompatibilityRegistry;
import org.geysermc.hydraulic.compat.MappingResolver;
import org.geysermc.hydraulic.compat.model.CompatibilityObject;
import org.geysermc.hydraulic.compat.runtime.CompatibilityDecisions;
import org.geysermc.hydraulic.pack.PackLogListener;
import org.geysermc.hydraulic.pack.PackModule;
import org.geysermc.hydraulic.pack.TexturePackModule;
import org.geysermc.hydraulic.pack.context.PackContext;
import org.geysermc.hydraulic.pack.context.PackEventContext;
import org.geysermc.hydraulic.pack.context.PackPostProcessContext;
import org.geysermc.hydraulic.pack.context.PackPreProcessContext;
import org.geysermc.hydraulic.component.ComponentConverter;
import org.geysermc.hydraulic.util.HydraulicKey;
import org.geysermc.hydraulic.util.PackUtil;
import org.geysermc.pack.bedrock.resource.BedrockResourcePack;
import org.geysermc.pack.converter.type.model.ModelStitcher;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import team.unnamed.creative.ResourcePack;
import team.unnamed.creative.item.*;
import team.unnamed.creative.model.Model;
import team.unnamed.creative.model.ModelTexture;
import team.unnamed.creative.model.ModelTextures;

import java.util.*;

@SuppressWarnings({"rawtypes", "this-escape"})
@AutoService(PackModule.class)
public class ItemPackModule extends TexturePackModule<ItemPackModule> {
    private final Set<Identifier> itemsWith2dIcon = new LinkedHashSet<>();
    private final Set<Identifier> handheldItems = new LinkedHashSet<>();
    private final Map<String, String> itemBuiltinTexture = new HashMap<>();

    public ItemPackModule() {
        this.listenOn(GeyserDefineCustomItemsEvent.class, this::onDefineCustomItems);

        this.preProcess(this::preProcess);
        this.postProcess(this::postProcess);
    }

    private void handleModel(@NotNull PackPreProcessContext<ItemPackModule> context, ItemModel itemModel, Identifier itemLocation) {
        if (itemModel instanceof ReferenceItemModel referenceModel) {
            Key modelKey = referenceModel.model();
            Model model = context.modelProvider().model(modelKey);
            if (model == null) {
                context.logger().debug("Could not resolve model {} for item {}", modelKey, itemLocation);
                return;
            }

            // Build the list of all parents in the model chain
            List<Key> parents = PackUtil.modelParents(context.modelProvider(), model);

            if (parents.contains(Model.ITEM_HANDHELD)) {
                itemsWith2dIcon.add(itemLocation); // item/handheld has the parent item/generated, so lets assume it's 2D
                handheldItems.add(itemLocation);
            } else if (parents.contains(Model.ITEM_GENERATED) || parents.contains(Model.BUILT_IN_GENERATED)) {
                itemsWith2dIcon.add(itemLocation);
            }
        } else if (itemModel instanceof SelectItemModel selectModel) { // See if we can actually do select models here
            handleModel(context, selectModel.fallback(), itemLocation);
        } else if (itemModel instanceof ConditionItemModel conditionModel) {
            handleModel(context, conditionModel.onTrue(), itemLocation);
        } else if (itemModel instanceof CompositeItemModel compositeModel) { // TODO: See if we can stitch together item models, for now this will use just the first model
            List<ItemModel> models = compositeModel.models();
            if (!models.isEmpty()) {
                handleModel(context, models.getFirst(), itemLocation);
            }
        } else if (itemModel instanceof RangeDispatchItemModel rangeDispatchModel) {
            handleModel(context, rangeDispatchModel.fallback(), itemLocation);
        }
    }

    private void preProcess(@NotNull PackPreProcessContext<ItemPackModule> context) {
        for (team.unnamed.creative.item.Item item : context.assets(ResourcePack::items)) {
            Identifier itemLocation = HydraulicKey.of(item.key()).identifier();
            handleModel(context, item.model(), itemLocation);
        }

        List<Item> items = context.registryValues(BuiltInRegistries.ITEM);
        PackLogListener packLogListener = new PackLogListener(context.logger());
        for (Item item : items) {
            Identifier itemLocation = BuiltInRegistries.ITEM.getKey(item);

            Model baseModel = context.modelProvider().model(Key.key(itemLocation.getNamespace(), "item/" + itemLocation.getPath()));
            if (baseModel == null) {
                continue;
            }

            Model model = new ModelStitcher(context.modelProvider(), baseModel, packLogListener).stitch();
            if (model == null) {
                continue;
            }

            List<ModelTexture> layers = model.textures().layers();
            if (layers == null || layers.isEmpty()) {
                continue;
            }

            Key layer0 = layers.getFirst().key();

            if (layer0 != null && layer0.namespace().equals(Key.MINECRAFT_NAMESPACE)) {
                itemBuiltinTexture.put(itemLocation.toString(), PackUtil.getTextureName(layer0.toString()));
            }
        }
    }

    private void postProcess(@NotNull PackPostProcessContext<ItemPackModule> context) {
        ResourcePack assets = context.javaResourcePack();
        BedrockResourcePack bedrockPack = context.bedrockResourcePack();

        List<Item> items = context.registryValues(BuiltInRegistries.ITEM);

        context.logger().info("Items to convert: {} in mod {}", items.size(), context.mod().id());

        PackLogListener packLogListener = new PackLogListener(context.logger());
        for (Item item : items) {
            Identifier itemLocation = BuiltInRegistries.ITEM.getKey(item);

            ItemTextureBinding binding = this.resolveTextureBinding(context, assets, item, itemLocation, packLogListener);
            if (binding == null) {
                continue;
            }

            if (binding.derivedFromBlockModel()) {
                context.logger().info("Using compatibility-backed block item texture fallback for {} via {}", itemLocation, binding.sourceIdentifier());
            }

            bedrockPack.addItemTexture(itemLocation.toString(), binding.outputLocation().replace(".png", ""));
        }
    }

    @Override
    public boolean test(@NotNull PackPostProcessContext<ItemPackModule> context) {
        return !context.registryValues(BuiltInRegistries.ITEM).isEmpty();
    }

    private void onDefineCustomItems(PackEventContext<GeyserDefineCustomItemsEvent, ItemPackModule> context) {
        GeyserDefineCustomItemsEvent event = context.event();
        List<Item> items = context.registryValues(BuiltInRegistries.ITEM);

        DefaultedRegistry<Item> registry = BuiltInRegistries.ITEM;
        for (Item item : items) {
            Identifier itemLocation = registry.getKey(item);
            CompatibilityObject itemObject = this.compatibilityItemObject(context, itemLocation);

            try {
                if (!CompatibilityDecisions.allowsCustomItemRegistration(itemObject, item)) {
                    context.logger().info("Skipping custom item registration for {} because compatibility analysis does not support content/presentation", itemLocation);
                    continue;
                }

                NonVanillaCustomItemDefinition.Builder customItemDefinition = NonVanillaCustomItemDefinition.builder(
                        org.geysermc.geyser.api.util.Identifier.of(itemLocation.toString()),
                        org.geysermc.geyser.api.util.Identifier.of(itemLocation.toString()),
                        registry.getId(item)
                )
                        .displayName("%" + item.getDescriptionId());

                CustomItemBedrockOptions.Builder customItemOptions = CustomItemBedrockOptions.builder()
                        .allowOffhand(true);

                // Allow minecraft namespace texture to be used (remapped as hydraulic)
                if (itemBuiltinTexture.containsKey(itemLocation.toString())) {
                    customItemOptions.icon(itemBuiltinTexture.get(itemLocation.toString()));
                }

                // Add the icon if it should have an icon
                boolean is2d = itemsWith2dIcon.contains(itemLocation);
                if (is2d) {
                    customItemOptions.icon(itemLocation.toString());
                }

                if (item instanceof BlockItem blockItem && this.shouldUseBlockItemTextureBridge(context, blockItem)) {
                    customItemOptions.icon(itemLocation.toString());
                }

                // Make it handheld if need be
                if (handheldItems.contains(itemLocation)) {
                    customItemOptions.displayHandheld(true);
                }

                CompatibilityObject blockObject = item instanceof BlockItem blockItem ? this.compatibilityBlockObject(context, blockItem) : null;

                // Set the creative mappings
                if (item instanceof BlockItem blockItemForCreative) {
                    if (CompatibilityDecisions.allowsBlockCreativeExposure(blockObject, blockItemForCreative.getBlock())) {
                        CreativeMappings.setup(item, customItemOptions);
                    }
                } else if (CompatibilityDecisions.allowsItemCreativeExposure(itemObject, item)) {
                    CreativeMappings.setup(item, customItemOptions);
                } else {
                    context.logger().info("Skipping creative exposure for {} because {}", itemLocation, CompatibilityDecisions.itemCreativeExposureReason(itemObject, item));
                }

                // Set all bedrock components using what java components we have
                ComponentConverter.setGeyserComponents(
                        item.components(),
                        customItemDefinition,
                        customItemOptions
                );

                // Set the needed component for bows to work correctly
                if (item instanceof BowItem) {
                    customItemDefinition.component(
                            GeyserItemDataComponents.CHARGEABLE,
                            GeyserChargeable.builder()
                                    .maxDrawDuration(1f)
                                    .chargeOnDraw(false)
                    );

                    // Include the default icon, this won't change in the hotbar when used but this works the best for now
                    customItemOptions.icon(itemLocation.toString());
                }

                // Set the needed component for crossbows to work correctly
                if (item instanceof CrossbowItem) {
                    customItemDefinition.component(
                            GeyserItemDataComponents.CHARGEABLE,
                            GeyserChargeable.builder()
                                    .maxDrawDuration(0f)
                                    .chargeOnDraw(true)
                    );

                    // Include the default icon, this won't change in the hotbar when used but this works the best for now
                    customItemOptions.icon(itemLocation.toString());
                }

                if (item instanceof BlockItem blockItem) {
                    // Set the block_placer component to the correct block
                    // This fixes animations sometimes not showing
                    Block block = blockItem.getBlock();
                    Identifier javaBlockIdentifier = BuiltInRegistries.BLOCK.getKey(block);
                    MappingResolver.ResolvedBlockState resolvedPlacement = context.hydraulic()
                        .getPackManager()
                        .mappingResolver()
                        .resolveBlockState(javaBlockIdentifier, block.defaultBlockState());

                    if (CompatibilityDecisions.shouldApplyBlockPlacementBridge(blockObject, block)) {
                        customItemDefinition.component(
                                GeyserItemDataComponents.BLOCK_PLACER,
                            GeyserBlockPlacer.of(HydraulicKey.of(resolvedPlacement.identifier()), !is2d)
                        );
                    } else {
                        context.logger().info("Skipping block placement bridge for {} because compatibility analysis does not support placement", itemLocation);
                    }

                    if (CompatibilityDecisions.allowsBlockCreativeExposure(blockObject, block)) {
                        CreativeMappings.setupBlock(block, customItemOptions);
                    }
                }

                customItemDefinition.bedrockOptions(customItemOptions);

                event.register(customItemDefinition.build());
            } catch (Exception e) {
                context.logger().error("Unable to register {}:", itemLocation, e);
            }
        }
    }

    @Nullable
    private ItemTextureBinding resolveTextureBinding(
        @NotNull PackPostProcessContext<ItemPackModule> context,
        @NotNull ResourcePack assets,
        @NotNull Item item,
        @NotNull Identifier itemLocation,
        @NotNull PackLogListener packLogListener
    ) {
        Model baseModel = assets.model(Key.key(itemLocation.getNamespace(), "item/" + itemLocation.getPath()));
        if (baseModel != null) {
            Model model = new ModelStitcher(context.modelProvider(), baseModel, packLogListener).stitch();
            Key textureKey = primaryTexture(model);
            if (textureKey != null) {
                return new ItemTextureBinding(itemLocation.toString(), getOutputFromModel(context, textureKey), false);
            }

            if (!(item instanceof BlockItem)) {
                context.logger().warn("Item {} has no layer0 texture, skipping", itemLocation);
                return null;
            }
        }

        if (!(item instanceof BlockItem blockItem)) {
            context.logger().warn("Item {} has no item model, skipping", itemLocation);
            return null;
        }

        CompatibilityObject compatibilityObject = this.compatibilityBlockObject(context, blockItem);
        if (!CompatibilityDecisions.supportsBlockItemTextureFallback(compatibilityObject)) {
            context.logger().warn("Item {} has no item model and no compatibility-backed block fallback, skipping", itemLocation);
            return null;
        }

        Identifier blockLocation = BuiltInRegistries.BLOCK.getKey(blockItem.getBlock());
        Model blockModel = assets.model(Key.key(blockLocation.getNamespace(), "block/" + blockLocation.getPath()));
        if (blockModel == null) {
            context.logger().warn("Item {} has no item model and block model {} is missing, skipping", itemLocation, blockLocation);
            return null;
        }

        Model stitchedBlockModel = new ModelStitcher(context.modelProvider(), blockModel, packLogListener).stitch();
        Key textureKey = primaryTexture(stitchedBlockModel);
        if (textureKey == null) {
            context.logger().warn("Item {} block model {} has no resolvable texture, skipping", itemLocation, blockLocation);
            return null;
        }
        return new ItemTextureBinding(blockLocation.toString(), getOutputFromModel(context, textureKey), true);
    }

    private boolean shouldUseBlockItemTextureBridge(@NotNull PackEventContext<GeyserDefineCustomItemsEvent, ItemPackModule> context, @NotNull BlockItem blockItem) {
        return CompatibilityDecisions.supportsBlockItemTextureFallback(this.compatibilityBlockObject(context, blockItem));
    }

    @Nullable
    private CompatibilityObject compatibilityItemObject(@NotNull PackContext<ItemPackModule> context, @NotNull Identifier itemLocation) {
        CompatibilityRegistry compatibilityRegistry = context.hydraulic().getPackManager().compatibilityRegistry();
        return compatibilityRegistry.report().object(context.mod().id(), itemLocation.toString(), "item");
    }

    @Nullable
    private CompatibilityObject compatibilityBlockObject(@NotNull PackContext<ItemPackModule> context, @NotNull BlockItem blockItem) {
        CompatibilityRegistry compatibilityRegistry = context.hydraulic().getPackManager().compatibilityRegistry();
        Identifier blockLocation = BuiltInRegistries.BLOCK.getKey(blockItem.getBlock());
        return compatibilityRegistry.report().object(context.mod().id(), blockLocation.toString(), "block");
    }

    @Nullable
    private static Key primaryTexture(@Nullable Model model) {
        if (model == null) {
            return null;
        }

        List<ModelTexture> layers = model.textures().layers();
        if (layers != null && !layers.isEmpty() && layers.getFirst().key() != null) {
            return layers.getFirst().key();
        }

        Map<String, ModelTexture> textures = textures(model.textures());
        for (String candidate : List.of("particle", "all", "side", "top", "bottom", "front", "back", "north", "south", "east", "west")) {
            ModelTexture texture = texture(textures, candidate, new HashSet<>());
            if (texture != null && texture.key() != null) {
                return texture.key();
            }
        }

        for (ModelTexture texture : textures.values()) {
            if (texture != null && texture.key() != null) {
                return texture.key();
            }
        }
        return null;
    }

    @NotNull
    private static Map<String, ModelTexture> textures(@NotNull ModelTextures modelTextures) {
        Map<String, ModelTexture> textures = new LinkedHashMap<>(modelTextures.variables());
        textures.put("particle", modelTextures.particle());
        for (int index = 0; index < modelTextures.layers().size(); index++) {
            textures.put("layer" + index, modelTextures.layers().get(index));
        }
        return textures;
    }

    @Nullable
    private static ModelTexture texture(@NotNull Map<String, ModelTexture> textures, @NotNull String key, @NotNull Set<String> visited) {
        if (!visited.add(key)) {
            return null;
        }

        ModelTexture value = textures.get(key);
        if (value != null && value.reference() != null) {
            return texture(textures, value.reference(), visited);
        }

        return value;
    }

    private record ItemTextureBinding(@NotNull String sourceIdentifier, @NotNull String outputLocation, boolean derivedFromBlockModel) {
    }
}
