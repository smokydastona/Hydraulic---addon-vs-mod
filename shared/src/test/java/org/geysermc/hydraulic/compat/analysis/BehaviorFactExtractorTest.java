package org.geysermc.hydraulic.compat.analysis;

import net.minecraft.resources.Identifier;
import org.geysermc.hydraulic.compat.MappingOwnership;
import org.geysermc.hydraulic.compat.mapping.ContentPatch;
import org.geysermc.hydraulic.compat.runtime.RuntimeBridgeKind;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BehaviorFactExtractorTest {
    @Test
    void extractsMachineAndTransferFactsFromMetadataPatches() {
        ContentPatch patch = new ContentPatch(
            Identifier.fromNamespaceAndPath("create", "mechanical_press"),
            "block",
            Map.ofEntries(
                Map.entry("machine.processing.enabled", "true"),
                Map.entry("machine.processing.type", "pressing"),
                Map.entry("machine.inventory.enabled", "true"),
                Map.entry("machine.inventory.layout", "3x3"),
                Map.entry("machine.inventory.input_slot", "0"),
                Map.entry("machine.inventory.output_slot", "1"),
                Map.entry("interaction.block_use.action", "insert_held_item"),
                Map.entry("interaction.block_use.slot", "0"),
                Map.entry("interaction.block_use.count", "1"),
                Map.entry("interaction.block_use.side", "up"),
                Map.entry("machine.processing.recipe.0.input", "minecraft:stone"),
                Map.entry("machine.processing.recipe.0.input_count", "1"),
                Map.entry("machine.processing.recipe.0.output", "minecraft:iron_ingot"),
                Map.entry("machine.processing.recipe.0.output_count", "1"),
                Map.entry("machine.processing.recipe.0.fluid_input.0.fluid", "minecraft:water"),
                Map.entry("machine.processing.recipe.0.fluid_input.0.amount", "500"),
                Map.entry("machine.processing.recipe.0.fluid_input.0.tank", "0"),
                Map.entry("machine.processing.recipe.0.energy_input", "50"),
                Map.entry("machine.processing.recipe.0.fluid_output.0.fluid", "minecraft:steam"),
                Map.entry("machine.processing.recipe.0.fluid_output.0.amount", "250"),
                Map.entry("machine.processing.recipe.0.fluid_output.0.tank", "1"),
                Map.entry("machine.processing.recipe.0.duration", "20"),
                Map.entry("transfer.item.can_insert", "true"),
                Map.entry("transfer.fluid.can_extract", "true"),
                Map.entry("transfer.energy.can_receive", "true"),
                Map.entry("transfer.energy.type", "forge_energy")
            ),
            MappingOwnership.USER,
            "create.json",
            1,
            0
        );

        Map<String, String> facts = BehaviorFactExtractor.extractFacts(List.of(patch));
        assertEquals("true", facts.get("has_processing"));
        assertEquals("pressing", facts.get("processing_type"));
        assertEquals("true", facts.get("has_inventory"));
        assertEquals("3x3", facts.get("inventory_layout"));
        assertEquals("0", facts.get("machine.input_slot"));
        assertEquals("1", facts.get("machine.output_slot"));
        assertEquals("insert_held_item", facts.get("interaction.block_use.action"));
        assertEquals("0", facts.get("interaction.block_use.slot"));
        assertEquals("1", facts.get("interaction.block_use.count"));
        assertEquals("up", facts.get("interaction.block_use.side"));
        assertEquals("minecraft:stone", facts.get("machine.processing.recipe.0.input"));
        assertEquals("minecraft:water", facts.get("machine.processing.recipe.0.fluid_input.0.fluid"));
        assertEquals("50", facts.get("machine.processing.recipe.0.energy_input"));
        assertEquals("minecraft:steam", facts.get("machine.processing.recipe.0.fluid_output.0.fluid"));
        assertEquals("20", facts.get("machine.processing.recipe.0.duration"));
        assertEquals("true", facts.get("can_insert"));
        assertEquals("true", facts.get("can_extract_fluid"));
        assertEquals("true", facts.get("can_receive_energy"));
        assertEquals("forge_energy", facts.get("energy_type"));
    }

    @Test
    void compilesTypedRuntimeBridgeRequirements() {
        Map<String, String> facts = Map.of(
            "has_processing", "true",
            "has_inventory", "true",
            "can_insert", "true",
            "can_extract_fluid", "true",
            "can_provide_energy", "true"
        );

        List<String> requirements = BehaviorFactExtractor.runtimeRequirements(facts);
        assertTrue(requirements.contains(RuntimeBridgeKind.MACHINE_BEHAVIOR.requirementId()));
        assertTrue(requirements.contains(RuntimeBridgeKind.MACHINE_INVENTORY.requirementId()));
        assertTrue(requirements.contains(RuntimeBridgeKind.ITEM_TRANSFER.requirementId()));
        assertTrue(requirements.contains(RuntimeBridgeKind.FLUID_TRANSFER.requirementId()));
        assertTrue(requirements.contains(RuntimeBridgeKind.ENERGY_TRANSFER.requirementId()));
    }
}
