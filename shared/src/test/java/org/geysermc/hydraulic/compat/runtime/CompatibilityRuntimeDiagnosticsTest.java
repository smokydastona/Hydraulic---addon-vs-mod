package org.geysermc.hydraulic.compat.runtime;

import org.geysermc.hydraulic.compat.CompatibilityProfile;
import org.geysermc.hydraulic.compat.CompatibilityRegistry;
import org.geysermc.hydraulic.compat.CompatibilityReport;
import org.geysermc.hydraulic.compat.ContentInventory;
import org.geysermc.hydraulic.compat.CompatibilityStatus;
import org.geysermc.hydraulic.compat.model.CompatibilityFinding;
import org.geysermc.hydraulic.compat.model.CompatibilityObject;
import org.geysermc.hydraulic.compat.model.Confidence;
import org.geysermc.hydraulic.compat.model.ModFingerprint;
import org.geysermc.hydraulic.compat.model.Provenance;
import org.geysermc.hydraulic.compat.model.SupportLevel;
import org.geysermc.hydraulic.compat.model.SupportResult;
import org.geysermc.hydraulic.metadata.MetadataIndex;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompatibilityRuntimeDiagnosticsTest {
    @Test
    void surfacesCompatibilityCandidatesForUnsupportedMenuOpen() {
        CompatibilityRegistry registry = compatibilityRegistry();

        String message = UnsupportedMenuDiagnosticFormatter.format("GENERIC_9X3", "Barrel Menu", "example:test_menu", registry);

        assertTrue(message.contains("GENERIC_9X3"));
        assertTrue(message.contains("Barrel Menu"));
        assertTrue(message.contains("example:test_menu"));
        assertTrue(message.contains("container_bridge"));
        assertTrue(message.contains("menu_behavior_bridge"));
    }

    @Test
    void fallsBackToGenericReasonWhenNoMenuCandidatesExist() {
        String message = UnsupportedMenuDiagnosticFormatter.format("GENERIC_9X3", null, null, CompatibilityRegistry.empty());

        assertTrue(message.contains("GENERIC_9X3"));
        assertTrue(message.contains("no discovered menu objects"));
    }

    @Test
    void suppressesDuplicateMatchedMenuFromCandidateTail() {
        CompatibilityRegistry registry = compatibilityRegistry();

        String message = UnsupportedMenuDiagnosticFormatter.format("GENERIC_9X3", "Barrel Menu", "example:test_menu", registry);

        assertTrue(message.contains("Compatibility report still marks this menu as requiring"));
        assertTrue(message.contains("Compatibility report candidates still requiring menu runtime bridges: none."));
    }

    @Test
    void surfacesMatchingBlockEntityBridgeRequirements() {
        CompatibilityRegistry registry = compatibilityRegistry();

        String message = UnsupportedBlockEntityDiagnosticFormatter.format("BARREL", "example:test_block_entity", "(12, 64, 12)", registry);

        assertTrue(message.contains("example:test_block_entity"));
        assertTrue(message.contains("BARREL"));
        assertTrue(message.contains("block_entity_data_bridge"));
        assertTrue(message.contains("block_entity_behavior_bridge"));
    }

    @Test
    void surfacesBlockEntityCandidatesWhenLiveIdentifierCannotBeResolved() {
        CompatibilityRegistry registry = compatibilityRegistry();

        String message = UnsupportedBlockEntityDiagnosticFormatter.format("BARREL", null, "(12, 64, 12)", registry);

        assertTrue(message.contains("could not resolve the live Java block entity identifier"));
        assertTrue(message.contains("example:test_block_entity"));
    }

    @Test
    void suppressesBlockEntityWarningWhenNoCompatibilityEvidenceExists() {
        String message = UnsupportedBlockEntityDiagnosticFormatter.format("BARREL", "example:untracked", "(12, 64, 12)", CompatibilityRegistry.empty());

        assertNull(message);
    }

    private static CompatibilityRegistry compatibilityRegistry() {
        MetadataIndex metadataIndex = MetadataIndex.empty();
        CompatibilityObject menuObject = new CompatibilityObject(
            "example:test_menu",
            "menu",
            "examplemod",
            Map.of(),
            new org.geysermc.hydraulic.compat.capability.CapabilityProfile("example:test_menu", List.of(), List.of()),
            List.of(),
            List.of("container_bridge", "menu_behavior_bridge"),
            Map.of(
                "interaction",
                new SupportResult("interaction", SupportLevel.UNSUPPORTED, CompatibilityStatus.NONE, 0, List.of(), List.of("container_interaction"), List.of("Generic Bedrock menu interaction bridges are not implemented."))
            ),
            SupportLevel.UNSUPPORTED,
            CompatibilityStatus.NONE,
            0,
            new Confidence(0.14D, "Menu analysis is currently metadata-backed and runtime-constrained."),
            List.of(new Provenance("ANALYZER", "MenuAnalyzer", null, false)),
            List.of(new CompatibilityFinding("menu.bridge.missing", CompatibilityFinding.Severity.WARNING, "interaction", "Missing menu bridge", "No generic container bridge exists.", "Implement container bridge support.", null))
        );
        CompatibilityObject blockEntityObject = new CompatibilityObject(
            "example:test_block_entity",
            "block_entity",
            "examplemod",
            Map.of(),
            new org.geysermc.hydraulic.compat.capability.CapabilityProfile("example:test_block_entity", List.of(), List.of()),
            List.of(),
            List.of("block_entity_data_bridge", "block_entity_interaction_bridge", "block_entity_behavior_bridge"),
            Map.of(
                "state_data",
                new SupportResult("state_data", SupportLevel.UNSUPPORTED, CompatibilityStatus.NONE, 0, List.of(), List.of("persistent_data"), List.of("Block entity data bridge is missing.")),
                "interaction",
                new SupportResult("interaction", SupportLevel.UNSUPPORTED, CompatibilityStatus.NONE, 0, List.of(), List.of("container_interaction"), List.of("Block entity interaction bridge is missing.")),
                "behavior",
                new SupportResult("behavior", SupportLevel.UNSUPPORTED, CompatibilityStatus.NONE, 0, List.of(), List.of("runtime_behavior"), List.of("Block entity behavior bridge is missing."))
            ),
            SupportLevel.UNSUPPORTED,
            CompatibilityStatus.NONE,
            0,
            new Confidence(0.10D, "Block entity analysis is registry-backed with optional patch evidence only."),
            List.of(new Provenance("ANALYZER", "BlockEntityAnalyzer", null, false)),
            List.of(new CompatibilityFinding("block_entity.bridge.missing", CompatibilityFinding.Severity.WARNING, "behavior", "Missing block entity bridge", "No block entity bridge exists.", "Implement block entity bridge support.", null))
        );
        CompatibilityProfile profile = new CompatibilityProfile(
            "examplemod",
            new ModFingerprint("examplemod", "example", "1.0.0", "unknown", "test", 0, 0, 0, 0, 1, 0, 0, false, false, false, false, false, false, false, false),
            SupportLevel.UNSUPPORTED,
            CompatibilityStatus.NONE,
            0,
            Map.of(),
            Map.of(SupportLevel.UNSUPPORTED.name(), 2),
            List.of(menuObject, blockEntityObject),
            List.of(),
            List.of()
        );
        ContentInventory inventory = new ContentInventory(Map.of(
            "examplemod",
            new ContentInventory.ModContentInventory(
                "examplemod",
                "example",
                "Example Mod",
                "1.0.0",
                List.of(),
                profile.fingerprint(),
                Map.of("menus", 1, "block_entities", 1),
                Map.of("menus", List.of("example:test_menu"), "block_entities", List.of("example:test_block_entity")),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of()
            )
        ));
        CompatibilityReport report = new CompatibilityReport("", MetadataIndex.Summary.empty(), List.of(), Map.of("examplemod", profile));
        return new CompatibilityRegistry(metadataIndex, new org.geysermc.hydraulic.compat.MappingResolver(metadataIndex), inventory, report);
    }
}