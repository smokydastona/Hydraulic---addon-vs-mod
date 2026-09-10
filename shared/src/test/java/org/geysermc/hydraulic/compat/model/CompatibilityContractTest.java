package org.geysermc.hydraulic.compat.model;

import org.geysermc.hydraulic.compat.CompatibilityStatus;
import org.geysermc.hydraulic.compat.adapter.AdapterBinding;
import org.geysermc.hydraulic.compat.adapter.AdapterFeature;
import org.geysermc.hydraulic.compat.capability.CapabilityProfile;
import org.geysermc.hydraulic.compat.runtime.RuntimeBridgeKind;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompatibilityContractTest {
    @Test
    void derivesTypedDomainActionsAndRuntimeBridges() {
        CompatibilityObject object = new CompatibilityObject(
            "example:machine",
            "block",
            "example",
            Map.of(),
            new CapabilityProfile("example:machine", List.of(), List.of()),
            List.of(new AdapterBinding("machine", AdapterFeature.UNSUPPORTED, "generic")),
            List.of("item_transfer_bridge", "machine_behavior_bridge"),
            Map.of(
                "presentation", result(SupportLevel.AUTOMATIC, CompatibilityStatus.COMPLETE),
                "behavior", result(SupportLevel.ADAPTED, CompatibilityStatus.PARTIAL),
                "network", result(SupportLevel.UNSUPPORTED, CompatibilityStatus.NONE)
            ),
            SupportLevel.UNSUPPORTED,
            CompatibilityStatus.PARTIAL,
            40,
            new Confidence(0.8D, "test"),
            List.of(),
            List.of()
        );

        CompatibilityContract contract = object.contract();

        assertEquals(CompatibilityContract.Action.NATIVE, contract.domains().get(CompatibilityContract.Domain.PRESENTATION).action());
        assertEquals(CompatibilityContract.Action.ADAPT, contract.domains().get(CompatibilityContract.Domain.BEHAVIOR).action());
        assertEquals(CompatibilityContract.Action.OMIT, contract.domains().get(CompatibilityContract.Domain.NETWORK).action());
        assertEquals(List.of(RuntimeBridgeKind.ITEM_TRANSFER, RuntimeBridgeKind.MACHINE_BEHAVIOR), contract.requiredBridges());
        assertFalse(contract.executable());
        assertEquals(CapabilityExecutionStatus.ANALYZED, contract.executionStatus());
    }

    @Test
    void criticalFailureBlocksExecutionEvenWhenDomainsAreAdapted() {
        CompatibilityObject object = new CompatibilityObject(
            "example:machine",
            "block",
            "example",
            Map.of("critical_failure", "true"),
            new CapabilityProfile("example:machine", List.of(), List.of()),
            List.of(),
            List.of(),
            Map.of("behavior", result(SupportLevel.ADAPTED, CompatibilityStatus.COMPLETE)),
            SupportLevel.ADAPTED,
            CompatibilityStatus.COMPLETE,
            90,
            new Confidence(0.9D, "test"),
            List.of(),
            List.of()
        );

        assertFalse(object.contract().executable());
        assertEquals(CapabilityExecutionStatus.ANALYZED, object.contract().executionStatus());
        assertTrue(object.contract().domains().containsKey(CompatibilityContract.Domain.BEHAVIOR));
    }

    @Test
    void convertsCrossCuttingNetworkAndRenderingFactsIntoConservativeActions() {
        CompatibilityObject object = new CompatibilityObject(
            "example:renderer",
            "entity",
            "example",
            Map.of("custom_networking", "true", "custom_rendering", "true"),
            new CapabilityProfile("example:renderer", List.of(), List.of()),
            List.of(),
            List.of(),
            Map.of("presentation", result(SupportLevel.AUTOMATIC, CompatibilityStatus.COMPLETE)),
            SupportLevel.AUTOMATIC,
            CompatibilityStatus.COMPLETE,
            100,
            new Confidence(0.9D, "test"),
            List.of(),
            List.of()
        );

        CompatibilityContract contract = object.contract();

        assertEquals(CompatibilityContract.Action.OMIT, contract.domains().get(CompatibilityContract.Domain.NETWORK).action());
        assertEquals(CompatibilityContract.Action.APPROXIMATE, contract.domains().get(CompatibilityContract.Domain.PRESENTATION).action());
        assertEquals(List.of("custom_networking"), contract.domains().get(CompatibilityContract.Domain.NETWORK).missingCapabilities());
    }

    private static SupportResult result(SupportLevel level, CompatibilityStatus status) {
        return new SupportResult("test", level, status, 80, List.of("available"), List.of(), List.of());
    }
}