package org.geysermc.hydraulic.fabric.test;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.geysermc.hydraulic.fabric.test.machine.ItemTransferMachineBlockEntity;
import org.geysermc.hydraulic.fabric.test.machine.ProcessingMachineBlockEntity;
import org.geysermc.hydraulic.fabric.test.machine.EnergyMachineBlockEntity;
import org.geysermc.hydraulic.fabric.test.machine.FluidMachineBlockEntity;
import org.geysermc.hydraulic.fabric.test.machine.MixedResourceMachineBlockEntity;
import org.geysermc.hydraulic.fabric.test.machine.MenuMachineBlockEntity;

import java.util.Set;

public final class ModBlockEntities {
    public static final ResourceKey<BlockEntityType<?>> ITEM_TRANSFER_MACHINE_KEY = keyOf("item_transfer_machine");
    public static final BlockEntityType<ItemTransferMachineBlockEntity> ITEM_TRANSFER_MACHINE = Registry.register(
        BuiltInRegistries.BLOCK_ENTITY_TYPE,
        ITEM_TRANSFER_MACHINE_KEY,
        new BlockEntityType<>(ItemTransferMachineBlockEntity::new, Set.of(ModBlocks.ITEM_TRANSFER_MACHINE))
    );

    public static final ResourceKey<BlockEntityType<?>> PROCESSING_MACHINE_KEY = keyOf("processing_machine");
    public static final BlockEntityType<ProcessingMachineBlockEntity> PROCESSING_MACHINE = Registry.register(
        BuiltInRegistries.BLOCK_ENTITY_TYPE,
        PROCESSING_MACHINE_KEY,
        new BlockEntityType<>(ProcessingMachineBlockEntity::new, Set.of(ModBlocks.PROCESSING_MACHINE))
    );

    public static final BlockEntityType<FluidMachineBlockEntity> FLUID_MACHINE = Registry.register(
        BuiltInRegistries.BLOCK_ENTITY_TYPE,
        keyOf("fluid_machine"),
        new BlockEntityType<>(FluidMachineBlockEntity::new, Set.of(ModBlocks.FLUID_MACHINE))
    );

    public static final BlockEntityType<EnergyMachineBlockEntity> ENERGY_MACHINE = Registry.register(
        BuiltInRegistries.BLOCK_ENTITY_TYPE,
        keyOf("energy_machine"),
        new BlockEntityType<>(EnergyMachineBlockEntity::new, Set.of(ModBlocks.ENERGY_MACHINE))
    );

    public static final BlockEntityType<MixedResourceMachineBlockEntity> MIXED_RESOURCE_MACHINE = Registry.register(
        BuiltInRegistries.BLOCK_ENTITY_TYPE,
        keyOf("mixed_resource_machine"),
        new BlockEntityType<>(MixedResourceMachineBlockEntity::new, Set.of(ModBlocks.MIXED_RESOURCE_MACHINE))
    );

    public static final BlockEntityType<MenuMachineBlockEntity> MENU_MACHINE = Registry.register(
        BuiltInRegistries.BLOCK_ENTITY_TYPE,
        keyOf("menu_machine"),
        new BlockEntityType<>(MenuMachineBlockEntity::new, Set.of(ModBlocks.MENU_MACHINE))
    );

    private ModBlockEntities() {
    }

    private static ResourceKey<BlockEntityType<?>> keyOf(String name) {
        return ResourceKey.create(Registries.BLOCK_ENTITY_TYPE, Identifier.fromNamespaceAndPath(HydraulicTestMod.MOD_ID, name));
    }

    public static void init() {
    }
}
