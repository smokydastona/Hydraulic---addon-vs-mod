package org.geysermc.hydraulic.compat.runtime;

import net.minecraft.resources.Identifier;
import org.geysermc.hydraulic.compat.CompatibilityStatus;
import org.geysermc.hydraulic.compat.adapter.AdapterBinding;
import org.geysermc.hydraulic.compat.adapter.AdapterFeature;
import org.geysermc.hydraulic.compat.ir.CompiledCompatibilityPlan;
import org.geysermc.hydraulic.compat.model.Confidence;
import org.geysermc.hydraulic.compat.model.SupportLevel;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class EntityInteractionPromptResolverTest {
    @Test
    void resolvesPromptOnlyWhenAdapterBacked() {
        assertEquals("Open Barrel Cube", EntityInteractionPromptResolver.resolvePrompt(plan(true, "Open Barrel Cube")));
        assertNull(EntityInteractionPromptResolver.resolvePrompt(plan(false, "Open Barrel Cube")));
        assertNull(EntityInteractionPromptResolver.resolvePrompt(plan(true, null)));
    }

    private static CompiledCompatibilityPlan plan(boolean includeAdapter, String interactionPrompt) {
        return new CompiledCompatibilityPlan(
            "testmod",
            "entity",
            Identifier.fromNamespaceAndPath("example", "test_entity").toString(),
            Identifier.fromNamespaceAndPath("example", "bedrock_entity").toString(),
            SupportLevel.ADAPTED,
            CompatibilityStatus.PARTIAL,
            75,
            new Confidence(0.8D, "test"),
            includeAdapter ? List.of(new AdapterBinding("entity.interaction_prompt", AdapterFeature.ENTITY_INTERACTION_PROMPT, "test")) : List.of(),
            List.of(),
            List.of(),
            interactionPrompt == null ? Map.of() : Map.of("interaction_prompt", interactionPrompt),
            false,
            null,
            false,
            null,
            false,
            false,
            false,
            false,
            false,
            null,
            interactionPrompt,
            List.of(),
            List.of(),
            List.of(),
            null,
            false,
            false,
            SupportLevel.UNSUPPORTED,
            null
        );
    }
}