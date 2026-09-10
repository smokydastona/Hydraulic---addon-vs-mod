package org.geysermc.hydraulic.block;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class StateDefinitionTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @BeforeEach
    void resetCacheMetrics() {
        StateDefinition.resetCacheMetrics();
    }

    @Test
    void recordsCacheHitsAndMissesForResolvedModels() {
        StateDefinition definition = new StateDefinition(null, key -> null);
        AtomicInteger resolverCalls = new AtomicInteger();

        assertNull(definition.resolveModel(Blocks.STONE.defaultBlockState(), ignored -> {
            resolverCalls.incrementAndGet();
            return null;
        }));
        assertNull(definition.resolveModel(Blocks.STONE.defaultBlockState(), ignored -> {
            resolverCalls.incrementAndGet();
            return null;
        }));

        StateDefinition.CacheMetrics metrics = StateDefinition.cacheMetrics();
        assertEquals(1, resolverCalls.get());
        assertEquals(1, metrics.misses());
        assertEquals(1, metrics.hits());
        assertEquals(2, metrics.requests());
    }
}