package org.geysermc.hydraulic.fabric.test;

import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.inventory.MenuType;

public final class ModMenus {
    public static final ResourceKey<MenuType<?>> BARREL_MENU_KEY = ResourceKey.create(
        Registries.MENU,
        Identifier.fromNamespaceAndPath(HydraulicTestMod.MOD_ID, "barrel_menu")
    );
    public static final MenuType<BarrelMenu> BARREL_MENU = Registry.register(
        BuiltInRegistries.MENU,
        BARREL_MENU_KEY,
        new ExtendedMenuType<>(BarrelMenu::new, ByteBufCodecs.STRING_UTF8)
    );

    private ModMenus() {
    }

    public static void init() {
    }
}