package org.geysermc.hydraulic.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HydraulicConfigMappingTest {
    @Test
    void interfaceMappingsIncludeGeyserRuntimeConfigDefinitions() throws IOException {
        try (InputStream inputStream = getClass().getClassLoader()
            .getResourceAsStream("org/spongepowered/configurate/interfaces/interface_mappings.properties")) {
            assertNotNull(inputStream, "Resource should be packaged in the classpath");

            String contents = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(contents.contains("org.geysermc.geyser.configuration.GeyserPluginConfig=org.geysermc.geyser.configuration.GeyserPluginConfigImpl"),
                "GeyserPluginConfig mapping must be present for Fabric/Geyser runtime boot");
            assertTrue(contents.contains("org.geysermc.hydraulic.config.HydraulicConfig=org.geysermc.hydraulic.config.HydraulicConfigImpl"),
                "HydraulicConfig mapping must be present for the mod configuration");
        }
    }
}
