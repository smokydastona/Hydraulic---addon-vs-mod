package org.geysermc.hydraulic.compat;

import net.minecraft.resources.Identifier;
import org.geysermc.hydraulic.metadata.IdentifierMapping;
import org.geysermc.hydraulic.metadata.MetadataIndex;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class MappingResolverTest {
    @Test
    void resolvesItemAndRecipeIdentifiers() {
        MetadataIndex index = new MetadataIndex(
            Map.of(),
            Map.of(
                Identifier.fromNamespaceAndPath("example", "test_item"),
                new IdentifierMapping(
                    Identifier.fromNamespaceAndPath("example", "test_item"),
                    Identifier.fromNamespaceAndPath("example", "bedrock_item"),
                    MappingOwnership.USER,
                    "test.json",
                    MappingOwnership.USER.priority(),
                    0
                )
            ),
            Map.of(
                Identifier.fromNamespaceAndPath("example", "test_recipe"),
                new IdentifierMapping(
                    Identifier.fromNamespaceAndPath("example", "test_recipe"),
                    Identifier.fromNamespaceAndPath("example", "bedrock_recipe"),
                    MappingOwnership.USER,
                    "test.json",
                    MappingOwnership.USER.priority(),
                    0
                )
            ),
            Map.of(
                Identifier.fromNamespaceAndPath("example", "test_entity"),
                new IdentifierMapping(
                    Identifier.fromNamespaceAndPath("example", "test_entity"),
                    Identifier.fromNamespaceAndPath("example", "bedrock_entity"),
                    MappingOwnership.USER,
                    "test.json",
                    MappingOwnership.USER.priority(),
                    0
                )
            ),
            new MetadataIndex.Summary(1, 0, 1, 1, 1, 0, Map.of("user", 1))
        );

        MappingResolver resolver = new MappingResolver(index);

        assertEquals("example:bedrock_item", resolver.resolveItemIdentifier(Identifier.fromNamespaceAndPath("example", "test_item")).identifier().toString());
        assertEquals("example:bedrock_recipe", resolver.resolveRecipeIdentifier(Identifier.fromNamespaceAndPath("example", "test_recipe")).identifier().toString());
        assertEquals("example:bedrock_entity", resolver.resolveEntityIdentifier(Identifier.fromNamespaceAndPath("example", "test_entity")).identifier().toString());
        assertFalse(resolver.resolveItemIdentifier(Identifier.fromNamespaceAndPath("example", "missing_item")).overridden());
    }
}