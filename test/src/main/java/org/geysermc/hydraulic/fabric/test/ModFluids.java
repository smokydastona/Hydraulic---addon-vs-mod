package org.geysermc.hydraulic.fabric.test;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.WaterFluid;
import net.minecraft.world.level.material.PushReaction;

public final class ModFluids {
    public static final ResourceKey<Fluid> BARREL_FLUID_KEY = fluidKey("barrel_fluid");
    public static final ResourceKey<Fluid> FLOWING_BARREL_FLUID_KEY = fluidKey("flowing_barrel_fluid");
    public static final ResourceKey<Block> BARREL_FLUID_BLOCK_KEY = ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(HydraulicTestMod.MOD_ID, "barrel_fluid"));
    public static final ResourceKey<Item> BARREL_BUCKET_KEY = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(HydraulicTestMod.MOD_ID, "barrel_bucket"));

    public static final Fluid BARREL_FLUID = Registry.register(BuiltInRegistries.FLUID, BARREL_FLUID_KEY, new BarrelFluid.Source());
    public static final Fluid FLOWING_BARREL_FLUID = Registry.register(BuiltInRegistries.FLUID, FLOWING_BARREL_FLUID_KEY, new BarrelFluid.Flowing());
    public static final Block BARREL_FLUID_BLOCK = Registry.register(BuiltInRegistries.BLOCK, BARREL_FLUID_BLOCK_KEY, new HydraulicTestLiquidBlock((net.minecraft.world.level.material.FlowingFluid) BARREL_FLUID, BlockBehaviour.Properties.of().noCollision().replaceable().pushReaction(PushReaction.DESTROY).setId(BARREL_FLUID_BLOCK_KEY)));
    public static final Item BARREL_BUCKET = Registry.register(BuiltInRegistries.ITEM, BARREL_BUCKET_KEY, new BucketItem(BARREL_FLUID, new Item.Properties().setId(BARREL_BUCKET_KEY).craftRemainder(Items.BUCKET).stacksTo(1)));

    private ModFluids() {
    }

    private static ResourceKey<Fluid> fluidKey(String name) {
        return ResourceKey.create(Registries.FLUID, Identifier.fromNamespaceAndPath(HydraulicTestMod.MOD_ID, name));
    }

    public static void init() {
    }

    private abstract static class BarrelFluid extends WaterFluid {
        @Override
        public Fluid getFlowing() {
            return FLOWING_BARREL_FLUID;
        }

        @Override
        public Fluid getSource() {
            return BARREL_FLUID;
        }

        @Override
        public Item getBucket() {
            return BARREL_BUCKET;
        }

        @Override
        public net.minecraft.world.level.block.state.BlockState createLegacyBlock(FluidState state) {
            return BARREL_FLUID_BLOCK.defaultBlockState().setValue(LiquidBlock.LEVEL, getLegacyLevel(state));
        }

        private static final class Flowing extends BarrelFluid {
            @Override
            protected void createFluidStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<Fluid, FluidState> builder) {
                super.createFluidStateDefinition(builder);
                builder.add(LEVEL);
            }

            @Override
            public int getAmount(FluidState state) {
                return state.getValue(LEVEL);
            }

            @Override
            public boolean isSource(FluidState state) {
                return false;
            }
        }

        private static final class Source extends BarrelFluid {
            @Override
            public int getAmount(FluidState state) {
                return 8;
            }

            @Override
            public boolean isSource(FluidState state) {
                return true;
            }
        }
    }

    private static final class HydraulicTestLiquidBlock extends LiquidBlock {
        private HydraulicTestLiquidBlock(net.minecraft.world.level.material.FlowingFluid fluid, BlockBehaviour.Properties properties) {
            super(fluid, properties);
        }
    }
}