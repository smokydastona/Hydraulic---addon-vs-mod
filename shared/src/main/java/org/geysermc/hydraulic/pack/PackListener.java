package org.geysermc.hydraulic.pack;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.Gson;
import com.mojang.logging.LogUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.geysermc.event.PostOrder;
import org.geysermc.event.subscribe.Subscribe;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineResourcePacksEvent;
import org.geysermc.geyser.api.pack.PackCodec;
import org.geysermc.geyser.api.pack.ResourcePack;
import org.geysermc.geyser.api.pack.option.PriorityOption;
import org.geysermc.hydraulic.cache.ConversionKey;
import org.geysermc.hydraulic.Constants;
import org.geysermc.hydraulic.HydraulicImpl;
import org.geysermc.hydraulic.platform.mod.ModInfo;
import org.geysermc.hydraulic.storage.ModStorage;
import org.geysermc.hydraulic.util.FormatUtil;
import org.geysermc.pack.bedrock.resource.Manifest;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipFile;

/**
 * Listens for events related to packs.
 */
public class PackListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();
    private static final ExecutorService THREAD_POOL;

    private final HydraulicImpl hydraulic;
    private final PackManager manager;
    private volatile PreparedPacks preparedPacks;

    static {
        THREAD_POOL = Executors.newFixedThreadPool(
            Math.max(1, Runtime.getRuntime().availableProcessors() * 3 / 8),
            new ThreadFactoryBuilder()
                .setNameFormat(Constants.MOD_NAME + " Conversion Thread #%d")
                .setUncaughtExceptionHandler((thread, throwable) -> LOGGER.error("Uncaught exception in thread {}", thread.getName(), throwable))
                .build()
        );
    }

    public PackListener(HydraulicImpl hydraulic, PackManager manager) {
        this.hydraulic = hydraulic;
        this.manager = manager;

        hydraulic.registerServerStop(server -> {
            THREAD_POOL.shutdown(); // Prevents the server from locking up on stop
        });
    }

    @Subscribe(postOrder = PostOrder.LATE)
    public void onLoadResourcePacks(GeyserDefineResourcePacksEvent event) {
        PreparedPacks prepared = this.ensurePacksPrepared();
        prepared.registerablePacks().forEach((modId, packPath) -> {
            if (!Files.exists(packPath)) {
                LOGGER.warn("Skipping prepared pack registration for mod {} because {} no longer exists", modId, packPath);
                return;
            }

            event.register(ResourcePack.create(PackCodec.path(packPath)), PriorityOption.NORMAL);
        });
    }

    synchronized PreparedPacks ensurePacksPrepared() {
        if (this.preparedPacks != null) {
            return this.preparedPacks;
        }

        this.preparedPacks = this.preparePacks();
        return this.preparedPacks;
    }

    @NotNull
    private PreparedPacks preparePacks() {
        // Check if hydraulic has updated since the last pack conversion
        // This is so we can regenerate packs on update in case the pack generation logic has changed
        ModInfo hydraulicMod = this.hydraulic.mod(Constants.MOD_ID);
        boolean hydraulicUpdated = checkNeedsConversion(hydraulicMod, this.hydraulic.modStorage(hydraulicMod).pack());

        if (hydraulicUpdated) {
            LOGGER.info("Hydraulic has updated since the last pack conversion, regenerating all packs!");
        }

        // Go over all mods and load the pack or mark them for conversion
        Map<String, Pair<ModInfo, Path>> packsToLoad = new HashMap<>();
        Map<String, Path> registerablePacks = new LinkedHashMap<>();
        int ignoredMods = 0;
        int generatedMods = 0;
        int skippedWithoutAssets = 0;
        int registeredFromCache = 0;
        for (ModInfo mod : this.hydraulic.mods()) {
            if (this.manager.shouldIgnoreMod(mod)) {
                ignoredMods++;
                continue;
            }

            // Ignore generated mods
            if (mod.id().startsWith("generated_")) {
                generatedMods++;
                continue;
            }

            if (!this.manager.shouldConvertModAssets(mod)) {
                skippedWithoutAssets++;
                continue;
            }

            ModStorage storage = this.hydraulic.modStorage(mod);

            Path packPath = storage.pack();
            if (this.hydraulic.isDev() || hydraulicUpdated || checkNeedsConversion(mod, packPath)) {
                packsToLoad.put(mod.id(), Pair.of(mod, packPath));
            } else {
                LOGGER.info("Using already converted pack for mod {}", mod.id());
                registerablePacks.put(mod.id(), packPath);
                registeredFromCache++;
            }
        }
        this.manager.recordConversionCacheUsage(registeredFromCache, packsToLoad.size());

        long start = System.currentTimeMillis();
        if (packsToLoad.isEmpty()) {
            PerformanceReport.PackConversionMetrics metrics = new PerformanceReport.PackConversionMetrics(
                System.currentTimeMillis() - start,
                this.hydraulic.mods().size(),
                ignoredMods,
                generatedMods,
                skippedWithoutAssets,
                0,
                registeredFromCache,
                0,
                0,
                Map.of()
            );
            this.manager.recordPackConversionMetrics(metrics);
            this.manager.syncCompatibilityValidation();
            this.manager.recordModelProviderMetrics();
            this.manager.recordTextureResolutionMetrics();
            this.manager.recordRuntimeDispatchMetrics();
            if (skippedWithoutAssets > 0) {
                LOGGER.info("Skipped {} mods with no asset-pack files requiring Hydraulic conversion", skippedWithoutAssets);
            }
            return new PreparedPacks(registerablePacks, metrics);
        }

        LOGGER.info("Found {} packs to convert!", packsToLoad.size());
        if (skippedWithoutAssets > 0) {
            LOGGER.info("Skipped {} mods with no asset-pack files requiring Hydraulic conversion", skippedWithoutAssets);
        }

        AtomicInteger convertedPacks = new AtomicInteger();
        AtomicInteger failedPacks = new AtomicInteger();
        ConcurrentMap<String, PerformanceReport.ModConversionMetrics> perModMetrics = new ConcurrentHashMap<>();
        ConcurrentMap<String, Path> convertedPackPaths = new ConcurrentHashMap<>();

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (var entry : packsToLoad.entrySet()) {
            futures.add(CompletableFuture.runAsync(() -> {
                LOGGER.info("Converting pack for mod {}", entry.getKey());
                long modStart = System.currentTimeMillis();
                try {
                    ModInfo mod = entry.getValue().getLeft();
                    ConversionKey conversionKey = this.manager.conversionKey(mod);
                    PackManager.PackCreationResult result = this.manager.createPack(mod, entry.getValue().getRight());
                    long modMillis = System.currentTimeMillis() - modStart;
                    PerformanceReport.ModConversionMetrics metrics = new PerformanceReport.ModConversionMetrics(
                        result.success() ? "converted" : "failed",
                        modMillis,
                        result.validation().durationMillis(),
                        result.validation().errorCount(),
                        result.validation().warningCount(),
                        result.validation().manualActionCount()
                    );
                    if (result.success()) {
                        ModStorage storage = this.hydraulic.modStorage(mod);
                        storage.conversionKey(conversionKey);
                        storage.save();
                        this.manager.artifactCache().storeConversionArtifact(mod.id(), new org.geysermc.hydraulic.cache.ArtifactCache.ConversionArtifact(
                            mod.id(),
                            conversionKey,
                            entry.getValue().getRight().toString(),
                            conversionKey.packUuid().toString()
                        ));
                        convertedPacks.incrementAndGet();
                        convertedPackPaths.put(entry.getKey(), entry.getValue().getRight());
                        perModMetrics.put(entry.getKey(), metrics);
                    } else {
                        failedPacks.incrementAndGet();
                        perModMetrics.put(entry.getKey(), metrics);
                    }
                } catch (Throwable t) {
                    failedPacks.incrementAndGet();
                    perModMetrics.put(entry.getKey(), new PerformanceReport.ModConversionMetrics("failed", System.currentTimeMillis() - modStart, 0, 1, 0, 1));
                    LOGGER.error("Failed to convert pack for mod {}", entry.getKey(), t);
                }
            }, THREAD_POOL));
        }

        // Wait for all futures to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0])).join();

        registerablePacks.putAll(new LinkedHashMap<>(convertedPackPaths));

        long totalMillis = System.currentTimeMillis() - start;
        PerformanceReport.PackConversionMetrics metrics = new PerformanceReport.PackConversionMetrics(
            totalMillis,
            this.hydraulic.mods().size(),
            ignoredMods,
            generatedMods,
            skippedWithoutAssets,
            packsToLoad.size(),
            registeredFromCache,
            convertedPacks.get(),
            failedPacks.get(),
            new LinkedHashMap<>(perModMetrics)
        );
        this.manager.recordPackConversionMetrics(metrics);
        this.manager.syncCompatibilityValidation();
        this.manager.recordModelProviderMetrics();
        this.manager.recordTextureResolutionMetrics();
        this.manager.recordRuntimeDispatchMetrics();

        LOGGER.info("Converted {} packs for mods in {}", packsToLoad.size(), FormatUtil.humanReadableFormat(totalMillis));
        return new PreparedPacks(registerablePacks, metrics);
    }

    /**
     * Checks if the pack needs to be converted based on the generated UUID.
     * This allows pack regeneration if the mod file has changed.
     *
     * @param mod The mod to check.
     * @param packPath The path to the pack.
     * @return {@code true} if the pack needs to be converted.
     */
    private boolean checkNeedsConversion(ModInfo mod, Path packPath) {
        // Read the uuid from the pack manifest
        String packUUID;
        try (
            ZipFile zip = new ZipFile(packPath.toFile());
            InputStream inputStream = zip.getInputStream(zip.getEntry("manifest.json"));
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream)
        ) {
            packUUID = GSON.fromJson(inputStreamReader, Manifest.class).header().uuid();
        } catch (IOException e) {
            return true;
        }

        ConversionKey currentKey = this.manager.conversionKey(mod);
        ModStorage storage = this.hydraulic.modStorage(mod);
        ConversionKey storedKey = storage.conversionKey();
        if (storedKey != null && !storedKey.equals(currentKey)) {
            return true;
        }

        return !currentKey.packUuid().equals(packUUID);
    }

    record PreparedPacks(
        @org.jetbrains.annotations.NotNull Map<String, Path> registerablePacks,
        @org.jetbrains.annotations.NotNull PerformanceReport.PackConversionMetrics metrics
    ) {
        PreparedPacks {
            registerablePacks = Map.copyOf(new LinkedHashMap<>(registerablePacks));
        }
    }
}
