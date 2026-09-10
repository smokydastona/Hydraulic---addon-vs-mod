package org.geysermc.hydraulic.mixin.ext;

import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MinecraftResourcePackReaderImplMixinTest {
    @Test
    void flagsUnsupportedCustomItemModelSchemas() throws Exception {
        assertTrue(invokeIsUnsupportedItemModelSchema(
            new IllegalArgumentException("Unknown item model type: citadel:custom_item_model")
        ));
    }

    @Test
    void ignoresOtherDeserializationFailures() throws Exception {
        assertFalse(invokeIsUnsupportedItemModelSchema(
            new IllegalArgumentException("Some other schema problem")
        ));
        assertFalse(invokeIsUnsupportedItemModelSchema(
            new RuntimeException("Unknown item model type: citadel:custom_item_model")
        ));
    }

    private static boolean invokeIsUnsupportedItemModelSchema(Exception exception) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method method = MinecraftResourcePackReaderImplMixin.class.getDeclaredMethod("isUnsupportedItemModelSchema", Exception.class);
        method.setAccessible(true);
        return (boolean) method.invoke(null, exception);
    }
}