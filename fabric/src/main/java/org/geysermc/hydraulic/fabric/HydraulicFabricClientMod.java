package org.geysermc.hydraulic.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import com.mojang.logging.LogUtils;
import org.geysermc.hydraulic.pack.PackListener;
import org.slf4j.Logger;

/**
 * Client-side initializer for Hydraulic.
 * Registers shutdown hooks to ensure proper cleanup of resources on client exit.
 */
@Environment(EnvType.CLIENT)
public class HydraulicFabricClientMod implements ClientModInitializer {
    private static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public void onInitializeClient() {
        LOGGER.info("Hydraulic client mod initialized");
        
        // Register a shutdown hook to gracefully shut down the thread pool
        // when the client closes
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            LOGGER.info("Hydraulic client stopping, shutting down thread pool");
            PackListener.shutdownThreadPool();
        });
    }
}
