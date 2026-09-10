package org.geysermc.hydraulic.fabric.test;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class ModEntities {
    public static final ResourceKey<EntityType<?>> BARREL_CUBE_KEY = keyOf("barrel_cube");
    public static final EntityType<BarrelTestEntity> BARREL_CUBE = Registry.register(
        BuiltInRegistries.ENTITY_TYPE,
        BARREL_CUBE_KEY,
        EntityType.Builder.of(BarrelTestEntity::new, MobCategory.MISC)
            .sized(0.75F, 0.75F)
            .clientTrackingRange(8)
            .updateInterval(20)
            .build(BARREL_CUBE_KEY)
    );

    private ModEntities() {
    }

    private static ResourceKey<EntityType<?>> keyOf(String name) {
        return ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(HydraulicTestMod.MOD_ID, name));
    }

    public static void init() {
    }
}