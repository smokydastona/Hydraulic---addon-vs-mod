package org.geysermc.hydraulic.fabric.test;

import net.fabricmc.api.ModInitializer;

public class HydraulicTestMod implements ModInitializer {
    public static final String MOD_ID = "hydraulic_test_mod";

    @Override
    public void onInitialize() {
        HydraulicTestMetadataBootstrap.installBundledMetadata();
        ModFluids.init();
        ModItems.init();
        ModBlocks.init();
        ModBlockEntities.init();
        ModEntities.init();
        ModMenus.init();
    }
}
