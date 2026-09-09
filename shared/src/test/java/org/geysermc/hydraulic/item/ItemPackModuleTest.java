package org.geysermc.hydraulic.item;

import com.google.gson.JsonParser;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;
import team.unnamed.creative.item.ReferenceItemModel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemPackModuleTest {
    @Test
    void parsesSupportedModernItemDefinition() {
        ItemPackModule.ParsedIndexedItemDefinition parsed = ItemPackModule.parseIndexedItemDefinition(
            JsonParser.parseString("""
                {
                  \"model\": {
                    \"type\": \"minecraft:model\",
                    \"model\": \"example:item/test\"
                  }
                }
                """),
            Identifier.fromNamespaceAndPath("example", "test_item")
        );

        assertNotNull(parsed.itemDefinition());
        assertNull(parsed.failureReason());
    assertTrue(parsed.itemDefinition().model() instanceof ReferenceItemModel);
    assertEquals("example:item/test", ((ReferenceItemModel) parsed.itemDefinition().model()).model().toString());
    }

    @Test
    void toleratesUnsupportedCustomItemModelSchema() {
        ItemPackModule.ParsedIndexedItemDefinition parsed = ItemPackModule.parseIndexedItemDefinition(
            JsonParser.parseString("""
                {
                  \"model\": {
                    \"type\": \"citadel:custom_item_model\",
                    \"model\": \"citadel:item/icon_item\"
                  }
                }
                """),
            Identifier.fromNamespaceAndPath("citadel", "icon_item")
        );

        assertNull(parsed.itemDefinition());
        assertNotNull(parsed.failureReason());
        assertTrue(parsed.failureReason().contains("Unknown item model type: citadel:custom_item_model"));
    }
}