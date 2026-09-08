package org.geysermc.hydraulic.metadata;

import net.minecraft.SharedConstants;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.geysermc.hydraulic.compat.MappingOwnership;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class BlockMappingTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void cachesResolvedStatesAndPreservesRuleMatching() {
        BlockStateRule northRule = new BlockStateRule(
            Map.of("facing", "north"),
            Identifier.fromNamespaceAndPath("example", "north_block"),
            null,
            "example:north",
            null,
            false,
            null,
            MappingOwnership.USER,
            "test.json",
            MappingOwnership.USER.priority(),
            0
        );
        BlockStateRule fallbackRule = new BlockStateRule(
            Map.of(),
            Identifier.fromNamespaceAndPath("example", "fallback_block"),
            null,
            "example:fallback",
            null,
            false,
            null,
            MappingOwnership.USER,
            "test.json",
            MappingOwnership.USER.priority(),
            1
        );
        BlockMapping mapping = new BlockMapping(
            Identifier.fromNamespaceAndPath("example", "test_block"),
            List.of(northRule, fallbackRule)
        );

        BlockState northState = Blocks.PISTON.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.NORTH);
        BlockState southState = Blocks.PISTON.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.SOUTH);

        assertSame(northRule, mapping.findRule(northState));
        assertSame(northRule, mapping.findRule(northState));
        assertEquals(1, mapping.cachedStateCount());

        assertSame(fallbackRule, mapping.findRule(southState));
        assertSame(fallbackRule, mapping.findRule(southState));
        assertEquals(2, mapping.cachedStateCount());
        assertEquals("example:north_block", mapping.resolve(northState).identifier().toString());
        assertEquals("example:fallback_block", mapping.resolve(southState).identifier().toString());
    }
}
