package org.geysermc.hydraulic.fabric.test;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.geysermc.hydraulic.fabric.test.machine.ItemTransferMachineBlockEntity;
import org.geysermc.hydraulic.fabric.test.machine.ProcessingMachineBlockEntity;

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

    private ModBlockEntities() {
    }

    private static ResourceKey<BlockEntityType<?>> keyOf(String name) {
        return ResourceKey.create(Registries.BLOCK_ENTITY_TYPE, Identifier.fromNamespaceAndPath(HydraulicTestMod.MOD_ID, name));
    }

    public static void init() {
    }
}
