package org.geysermc.hydraulic.compat.discovery;

import net.minecraft.resources.Identifier;
import org.geysermc.hydraulic.compat.runtime.TransferBridgeFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * MachineProfile holding multi-resource slot roles, recipe contracts, transfer capabilities,
 * automation rules, state properties, and evidence trails.
 */
public record MachineProfile(
    @NotNull Identifier identifier,
    @NotNull Map<String, String> slotRoles,
    @NotNull List<RecipeContract> recipes,
    boolean supportsItemTransfer,
    boolean supportsFluidTransfer,
    boolean supportsEnergyTransfer,
    boolean supportsAutomation,
    @NotNull Map<String, String> stateProperties,
    @NotNull List<SemanticDiscoveryEngine.DiscoveryEvidence> evidence
) {
    public record RecipeContract(
        @NotNull List<TransferBridgeFactory.ItemStackView> itemInputs,
        @NotNull List<TransferBridgeFactory.FluidStackView> fluidInputs,
        int energyInput,
        @NotNull List<TransferBridgeFactory.ItemStackView> itemOutputs,
        @NotNull List<TransferBridgeFactory.FluidStackView> fluidOutputs,
        int energyOutput,
        int duration
    ) {}

    @NotNull
    public static MachineProfile fromFacts(
        @NotNull Identifier identifier,
        @NotNull Map<String, String> facts,
        @NotNull List<SemanticDiscoveryEngine.DiscoveryEvidence> evidence
    ) {
        Map<String, String> slots = Map.of(
            "input_slot", facts.getOrDefault("machine.input_slot", "0"),
            "output_slot", facts.getOrDefault("machine.output_slot", "1")
        );

        boolean item = Boolean.parseBoolean(facts.getOrDefault("can_insert", "false")) || Boolean.parseBoolean(facts.getOrDefault("can_extract", "false"));
        boolean fluid = Boolean.parseBoolean(facts.getOrDefault("can_insert_fluid", "false")) || Boolean.parseBoolean(facts.getOrDefault("can_extract_fluid", "false"));
        boolean energy = Boolean.parseBoolean(facts.getOrDefault("can_receive_energy", "false")) || Boolean.parseBoolean(facts.getOrDefault("can_provide_energy", "false"));
        boolean automation = Boolean.parseBoolean(facts.getOrDefault("sided_insert", "false")) || Boolean.parseBoolean(facts.getOrDefault("sided_extract", "false"));

        List<RecipeContract> recipeContracts = List.of();
        if (facts.containsKey("machine.processing.recipe.0.input") && facts.containsKey("machine.processing.recipe.0.output")) {
            recipeContracts = List.of(new RecipeContract(
                List.of(new TransferBridgeFactory.ItemStackView(facts.get("machine.processing.recipe.0.input"), Integer.parseInt(facts.getOrDefault("machine.processing.recipe.0.input_count", "1")))),
                List.of(),
                Integer.parseInt(facts.getOrDefault("machine.processing.recipe.0.energy_input", "0")),
                List.of(new TransferBridgeFactory.ItemStackView(facts.get("machine.processing.recipe.0.output"), Integer.parseInt(facts.getOrDefault("machine.processing.recipe.0.output_count", "1")))),
                List.of(),
                0,
                Integer.parseInt(facts.getOrDefault("machine.processing.recipe.0.duration", "40"))
            ));
        }

        return new MachineProfile(
            identifier,
            slots,
            recipeContracts,
            item,
            fluid,
            energy,
            automation,
            Map.copyOf(facts),
            List.copyOf(evidence)
        );
    }
}
