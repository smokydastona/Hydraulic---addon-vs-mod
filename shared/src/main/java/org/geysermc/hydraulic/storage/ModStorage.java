package org.geysermc.hydraulic.storage;

import com.mojang.logging.LogUtils;
import org.geysermc.hydraulic.Constants;
import org.geysermc.hydraulic.HydraulicImpl;
import org.geysermc.hydraulic.cache.ConversionKey;
import org.geysermc.hydraulic.block.Materials;
import org.geysermc.hydraulic.platform.mod.ModInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Stores data relevant to a mod.
 */
public class ModStorage {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String MATERIALS_FILE = "materials.json";
    private static final String CONVERSION_KEY_FILE = "conversion-key.json";

    private ModInfo mod;
    private Materials materials = new Materials();
    private ConversionKey conversionKey;
    private Path pack;

    private ModStorage(@NotNull ModInfo mod) {
        this.mod = mod;
        this.pack = storagePath(mod).resolve(mod.id() + ".mcpack");
    }

    /**
     * Gets the materials for this mod.
     *
     * @return the materials
     */
    @NotNull
    public Materials materials() {
        return this.materials;
    }

    /**
     * Sets the materials for this mod.
     *
     * @param materials the materials
     */
    public void materials(@NotNull Materials materials) {
        this.materials = materials;
    }

    @Nullable
    public ConversionKey conversionKey() {
        return this.conversionKey;
    }

    public void conversionKey(@Nullable ConversionKey conversionKey) {
        this.conversionKey = conversionKey;
    }

    /**
     * Gets the path to the pack for this mod.
     *
     * @return the path to the pack
     */
    @NotNull
    public Path pack() {
        return this.pack;
    }

    /**
     * Saves the mod storage.
     */
    public void save() {
        try {
            Path path = storagePath(this.mod);
            if (Files.notExists(path)) {
                Files.createDirectories(path);
            }

            try (BufferedWriter writer = Files.newBufferedWriter(path.resolve(MATERIALS_FILE))) {
                Constants.GSON.toJson(this.materials, writer);
            }

            Path conversionKeyPath = path.resolve(CONVERSION_KEY_FILE);
            if (this.conversionKey != null) {
                try (BufferedWriter writer = Files.newBufferedWriter(conversionKeyPath)) {
                    Constants.GSON.toJson(this.conversionKey, writer);
                }
            } else {
                Files.deleteIfExists(conversionKeyPath);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to save mod storage for {}", this.mod.id());
        }
    }

    /**
     * Loads the mod storage for the given mod.
     *
     * @param mod the mod
     * @return the mod storage
     */
    public static ModStorage load(@NotNull ModInfo mod) {
        Path path = storagePath(mod);
        ModStorage storage = new ModStorage(mod);

        if (Files.notExists(path)) {
            return storage;
        }

        try {
            try (BufferedReader reader = Files.newBufferedReader(path.resolve(MATERIALS_FILE))) {
                Materials materials = Constants.GSON.fromJson(reader, Materials.class);
                storage.materials(materials);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load mod storage for {}", mod.id());
        }

        Path conversionKeyPath = path.resolve(CONVERSION_KEY_FILE);
        if (Files.isRegularFile(conversionKeyPath)) {
            try (BufferedReader reader = Files.newBufferedReader(conversionKeyPath)) {
                storage.conversionKey(Constants.GSON.fromJson(reader, ConversionKey.class));
            } catch (IOException e) {
                LOGGER.error("Failed to load conversion key for {}", mod.id());
            }
        }

        return storage;
    }

    private static Path storagePath(@NotNull ModInfo mod) {
        return HydraulicImpl.instance().dataFolder(Constants.MOD_ID)
                .resolve("storage")
                .resolve(mod.id());
    }
}
