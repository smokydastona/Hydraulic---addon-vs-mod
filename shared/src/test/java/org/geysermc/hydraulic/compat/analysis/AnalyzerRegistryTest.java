package org.geysermc.hydraulic.compat.analysis;

import org.geysermc.hydraulic.compat.ContentInventory;
import org.geysermc.hydraulic.compat.model.CompatibilityObject;
import org.geysermc.hydraulic.metadata.MetadataIndex;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AnalyzerRegistryTest {
    @Test
    void returnsAnalyzerByDeclaredKind() {
        CompatibilityAnalyzer block = analyzer("block");
        CompatibilityAnalyzer item = analyzer("item");

        AnalyzerRegistry registry = AnalyzerRegistry.create(List.of(block, item));

        assertSame(block, registry.analyzer("block"));
        assertSame(item, registry.analyzer("item"));
        assertNull(registry.analyzer("fluid"));
    }

    @Test
    void rejectsDuplicateAnalyzerKinds() {
        assertThrows(IllegalArgumentException.class, () -> AnalyzerRegistry.create(List.of(analyzer("block"), analyzer("block"))));
    }

    private static CompatibilityAnalyzer analyzer(String kind) {
        return new CompatibilityAnalyzer() {
            @Override
            public String kind() {
                return kind;
            }

            @Override
            public CompatibilityObject analyze(ContentInventory.ContentDescriptor descriptor, ContentInventory.ModContentInventory inventory, MetadataIndex metadataIndex) {
                throw new UnsupportedOperationException();
            }
        };
    }
}