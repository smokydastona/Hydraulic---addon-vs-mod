package org.geysermc.hydraulic.compat.corpus;

import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Computes corpus capability coverage against the canonical Bedrock capability vocabulary,
 * so the corpus can report a measurable roadmap ("we have 3 item-transfer implementations but
 * zero for replicated state") instead of only a raw entry count.
 *
 * This is a report-time heuristic classifier only: it never influences {@link AddonCorpusMatcher}
 * ranking or any compiled runtime plan, matching the corpus rule that raw corpus data must stay
 * out of hot runtime paths.
 */
public final class CorpusCapabilityCoverage {
    /** Canonical capability vocabulary this coverage report is measured against. */
    public static final List<String> CANONICAL_CAPABILITIES = List.of(
        "ITEM_STORAGE",
        "ITEM_TRANSFER",
        "FLUID_STORAGE",
        "FLUID_TRANSFER",
        "ENERGY_STORAGE",
        "ENERGY_TRANSFER",
        "MACHINE_PROCESSING",
        "MACHINE_INVENTORY",
        "AUTOMATION",
        "FILTERING",
        "SIDED_ACCESS",
        "MULTIBLOCK",
        "NETWORK_TOPOLOGY",
        "NETWORK_SYNC",
        "PERSISTENCE",
        "CUSTOM_MENU",
        "REPLICATED_STATE",
        "RPC",
        "CUSTOM_ENTITY_BEHAVIOR"
    );

    private static final Map<String, List<String>> KEYWORDS = Map.ofEntries(
        Map.entry("ITEM_STORAGE", List.of("item_storage", "shared_network_storage", "resource_container", "item-storage")),
        Map.entry("ITEM_TRANSFER", List.of("item_transfer", "item_transport", "item_processing", "item-transfer")),
        Map.entry("FLUID_STORAGE", List.of("fluid_storage", "fluid_tanks", "fluid_tank")),
        Map.entry("FLUID_TRANSFER", List.of("fluid_transport", "fluid_transfer", "liquid_processing", "gas_transport")),
        Map.entry("ENERGY_STORAGE", List.of("energy_storage")),
        Map.entry("ENERGY_TRANSFER", List.of("energy_transfer", "energy_generation", "energy_cost", "energy_efficiency")),
        Map.entry("MACHINE_PROCESSING", List.of("processing", "crusher", "infuser", "reactor", "furnace", "recipe", "generator")),
        Map.entry("MACHINE_INVENTORY", List.of("machine_api", "machine_inventory", "resource_container")),
        Map.entry("AUTOMATION", List.of("automation", "multiblock_ports", "link_node_registration", "upgrade")),
        Map.entry("FILTERING", List.of("filter")),
        Map.entry("SIDED_ACCESS", List.of("sided", "port")),
        Map.entry("MULTIBLOCK", List.of("multiblock")),
        Map.entry("NETWORK_TOPOLOGY", List.of("shared_network", "shared_link_node", "network")),
        Map.entry("NETWORK_SYNC", List.of("cross_addon", "shared_link_node_io", "sync")),
        Map.entry("PERSISTENCE", List.of("persistence", "persistent")),
        Map.entry("CUSTOM_MENU", List.of("machine_ui", "custom_ui", "ui")),
        Map.entry("REPLICATED_STATE", List.of("replicated_state", "replicated state")),
        Map.entry("RPC", List.of("typed_rpc", "rpc")),
        Map.entry("CUSTOM_ENTITY_BEHAVIOR", List.of("custom_entity", "entity_behavior", "bonsai entity"))
    );

    private CorpusCapabilityCoverage() {
    }

    @NotNull
    public static Result compute(@NotNull List<AddonCorpusEntry> admissibleEntries, @NotNull List<AddonCorpusEntry> allEntries) {
        Map<String, Set<String>> implementationCoverage = new TreeMap<>();
        Map<String, Set<String>> documentationCoverage = new TreeMap<>();
        for (String capability : CANONICAL_CAPABILITIES) {
            implementationCoverage.put(capability, new LinkedHashSet<>());
            documentationCoverage.put(capability, new LinkedHashSet<>());
        }

        for (AddonCorpusEntry entry : allEntries) {
            boolean isAdmissible = admissibleEntries.stream()
                .anyMatch(admissible -> admissible.identity().corpusId().equals(entry.identity().corpusId()));
            Set<String> haystack = collectSearchableEvidence(entry);
            for (String capability : CANONICAL_CAPABILITIES) {
                boolean matches = KEYWORDS.getOrDefault(capability, List.of()).stream()
                    .anyMatch(keyword -> haystack.stream().anyMatch(value -> value.contains(keyword)));
                if (!matches) {
                    continue;
                }
                if (isAdmissible) {
                    implementationCoverage.get(capability).add(entry.identity().corpusId());
                } else {
                    documentationCoverage.get(capability).add(entry.identity().corpusId());
                }
            }
        }

        Map<String, Integer> implementationCounts = new LinkedHashMap<>();
        Map<String, Integer> documentationCounts = new LinkedHashMap<>();
        List<String> gaps = new java.util.ArrayList<>();
        for (String capability : CANONICAL_CAPABILITIES) {
            int implementationCount = implementationCoverage.get(capability).size();
            int documentationCount = documentationCoverage.get(capability).size();
            implementationCounts.put(capability, implementationCount);
            documentationCounts.put(capability, documentationCount);
            if (implementationCount == 0) {
                gaps.add(capability);
            }
        }

        return new Result(Map.copyOf(implementationCounts), Map.copyOf(documentationCounts), List.copyOf(gaps));
    }

    @NotNull
    private static Set<String> collectSearchableEvidence(@NotNull AddonCorpusEntry entry) {
        Set<String> values = new LinkedHashSet<>();
        values.addAll(lower(entry.capabilities().storageTypes()));
        values.addAll(lower(entry.capabilities().machineTypes()));
        values.addAll(lower(entry.capabilities().transferTypes()));
        values.addAll(lower(entry.capabilities().fluidTypes()));
        values.addAll(lower(entry.capabilities().energyTypes()));
        values.addAll(lower(entry.capabilities().automationTypes()));
        values.addAll(lower(entry.capabilities().networkingTypes()));
        values.addAll(lower(entry.capabilities().customCapabilities().keySet()));
        values.addAll(lower(entry.capabilities().customCapabilities().values()));
        values.add(entry.implementationFacts().implementationPattern().toLowerCase(Locale.ROOT));
        values.addAll(lower(entry.implementationFacts().transferSemantics()));
        values.addAll(lower(entry.implementationFacts().runtimeHooks()));
        values.addAll(lower(entry.behaviorPack().entities()));
        return values;
    }

    @NotNull
    private static Set<String> lower(@NotNull Iterable<String> values) {
        Set<String> lowered = new LinkedHashSet<>();
        for (String value : values) {
            lowered.add(value.toLowerCase(Locale.ROOT));
        }
        return lowered;
    }

    /**
     * @param implementationCoverage count of admissible (implementation-tier) entries matching each canonical capability
     * @param documentationCoverage  count of inadmissible (documentation/teaching-tier) entries matching each canonical capability
     * @param gapCapabilities        canonical capabilities with zero admissible implementation coverage
     */
    public record Result(
        @NotNull Map<String, Integer> implementationCoverage,
        @NotNull Map<String, Integer> documentationCoverage,
        @NotNull List<String> gapCapabilities
    ) {
    }
}
