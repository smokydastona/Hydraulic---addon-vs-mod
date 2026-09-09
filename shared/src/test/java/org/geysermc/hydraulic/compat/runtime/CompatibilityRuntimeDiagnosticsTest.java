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

import static org.junit.jupiter.api.Assertions.assertTrue;

class CompatibilityRuntimeDiagnosticsTest {
    @Test
    void surfacesCompatibilityCandidatesForUnsupportedMenuOpen() {
        CompatibilityRegistry registry = compatibilityRegistry();

        String message = UnsupportedMenuDiagnosticFormatter.format("GENERIC_9X3", "Barrel Menu", registry);

        assertTrue(message.contains("GENERIC_9X3"));
        assertTrue(message.contains("Barrel Menu"));
        assertTrue(message.contains("container_bridge"));
        assertTrue(message.contains("example:test_menu"));
    }

    @Test
    void fallsBackToGenericReasonWhenNoMenuCandidatesExist() {
        String message = UnsupportedMenuDiagnosticFormatter.format("GENERIC_9X3", null, CompatibilityRegistry.empty());

        assertTrue(message.contains("GENERIC_9X3"));
        assertTrue(message.contains("no discovered menu objects"));
    }

    private static CompatibilityRegistry compatibilityRegistry() {
        MetadataIndex metadataIndex = MetadataIndex.empty();
        CompatibilityObject object = new CompatibilityObject(
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
        CompatibilityProfile profile = new CompatibilityProfile(
            "examplemod",
            new ModFingerprint("examplemod", "example", "1.0.0", "unknown", "test", 0, 0, 0, 0, 1, 0, 0, false, false, false, false, false, false, false, false),
            SupportLevel.UNSUPPORTED,
            CompatibilityStatus.NONE,
            0,
            Map.of(),
            Map.of(SupportLevel.UNSUPPORTED.name(), 1),
            List.of(object),
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
                Map.of("menus", 1),
                Map.of("menus", List.of("example:test_menu")),
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