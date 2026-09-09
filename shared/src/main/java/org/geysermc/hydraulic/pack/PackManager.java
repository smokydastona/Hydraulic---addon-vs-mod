package org.geysermc.hydraulic.pack;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.mojang.logging.LogUtils;
import net.kyori.adventure.key.Key;
import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import org.geysermc.event.Event;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.hydraulic.Constants;
import org.geysermc.hydraulic.HydraulicImpl;
import org.geysermc.hydraulic.block.StateDefinition;
import org.geysermc.hydraulic.compat.CompatibilityManager;
import org.geysermc.hydraulic.compat.CompatibilityRegistry;
import org.geysermc.hydraulic.compat.CompatibilityReport;
import org.geysermc.hydraulic.compat.MappingResolver;
import org.geysermc.hydraulic.metadata.MetadataIndex;
import org.geysermc.hydraulic.metadata.MetadataLoader;
import org.geysermc.hydraulic.pack.context.PackEventContext;
import org.geysermc.hydraulic.pack.context.PackPostProcessContext;
import org.geysermc.hydraulic.pack.context.PackPreProcessContext;
import org.geysermc.hydraulic.pack.converter.CustomModelConverter;
import org.geysermc.hydraulic.pack.modules.MetadataPackModule;
import org.geysermc.hydraulic.platform.mod.ModInfo;
import org.geysermc.pack.converter.PackConverter;
import org.geysermc.pack.converter.pipeline.AssetConverters;
import org.geysermc.pack.converter.pipeline.ConverterPipeline;
import org.geysermc.pack.converter.type.model.ModelStitcher;
import org.geysermc.pack.converter.util.NioDirectoryFileTreeReader;
import org.geysermc.pack.converter.util.VanillaPackProvider;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import team.unnamed.creative.ResourcePack;
import team.unnamed.creative.model.Model;
import team.unnamed.creative.serialize.minecraft.MinecraftResourcePackReader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

/**
 * Manages packs within Hydraulic. Most of the pack conversion
 * management is done within this class, and it is also responsible
 * for loading the packs onto the server.
 */
public class PackManager {
    private static final Logger LOGGER = LogUtils.getLogger();

    static final Set<String> IGNORED_MODS = Set.of(
            // Fabric
            "geyser-fabric",
            "fabric-permissions-api-v0",

            // NeoForge
            "geyser-neoforge",
            "neoforge",
            "minecraft",

            // Common
            "floodgate",
            "mixinextras",
            "cloud"
    );

    private final HydraulicImpl hydraulic;
    private final Path vanillaPath;
    private final PerformanceReportTracker performanceTracker;
    private final PackValidationTracker packValidationTracker;
    private final PackValidator packValidator = new PackValidator();
    private final List<PackModule<?>> modules = new ArrayList<>();

    private final ListMultimap<String, ModInfo> namespacesToMods = MultimapBuilder.hashKeys().arrayListValues(1).build();
    private final ListMultimap<String, Identifier> modsToBlocks = MultimapBuilder.hashKeys().arrayListValues().build();
    private final ListMultimap<String, Identifier> modsToItems = MultimapBuilder.hashKeys().arrayListValues().build();
    private final Map<String, ModResourceIndex> modResourceIndexes = new LinkedHashMap<>();

    private MetadataIndex metadataIndex = MetadataIndex.empty();
    private CompatibilityRegistry compatibilityRegistry = CompatibilityRegistry.empty();
    private CompatibilityManager compatibilityManager;

    private List<ConverterPipeline<?, ?>> packConverters;
    private ModelStitcher.Provider modelProvider;

    public PackManager(HydraulicImpl hydraulic) {
        this.hydraulic = hydraulic;
        this.vanillaPath = hydraulic.dataFolder(Constants.MOD_ID).resolve("cache/vanilla-assets.zip");
        this.performanceTracker = new PerformanceReportTracker(LOGGER, hydraulic.dataFolder(Constants.MOD_ID).resolve("reports/performance-report.json"));
        this.packValidationTracker = new PackValidationTracker(LOGGER, hydraulic.dataFolder(Constants.MOD_ID).resolve("reports/pack-validation-report.json"));
    }

    /**
     * Initializes the pack manager.
     */
    public void initialize() {
        long resourceIndexStarted = System.nanoTime();
        LookupSummary lookupSummary = initializeModLookups();
        long indexedResourcesMillis = nanosToMillis(System.nanoTime() - resourceIndexStarted);

        long metadataLoadStarted = System.nanoTime();
        this.metadataIndex = new MetadataLoader(LOGGER).load(this.hydraulic.dataFolder(Constants.MOD_ID).resolve("metadata"));
        long metadataLoadMillis = nanosToMillis(System.nanoTime() - metadataLoadStarted);

        long compatibilityStarted = System.nanoTime();
        initializeCompatibilityRegistry();
        long compatibilityInitializationMillis = nanosToMillis(System.nanoTime() - compatibilityStarted);

        final Collection<ModInfo> mods = this.hydraulic.mods();
        long resourcePackReadStarted = System.nanoTime();
        final Map<String, List<ResourcePack>> modPacks = Maps.newHashMapWithExpectedSize(mods.size());
        for (final ModInfo mod : mods) {
            modPacks.put(
                mod.id(),
                mod.roots()
                    .stream()
                    .map(path -> MinecraftResourcePackReader.minecraft().read(NioDirectoryFileTreeReader.read(path)))
                    .toList()
            );
        }
                long resourcePackReadMillis = nanosToMillis(System.nanoTime() - resourcePackReadStarted);

        try {
            Files.createDirectories(this.getVanillaPath().getParent());
        } catch (IOException e) {
            LOGGER.error("Failed to create cache dir");
        }

        VanillaPackProvider.create(
                this.getVanillaPath(),
                SharedConstants.getCurrentVersion().id(),
                new PackLogListener(LOGGER)
        );

        long modelProviderStarted = System.nanoTime();
        modelProvider = createModelProvider(mods, modPacks, this.getVanillaPath());
        long modelIndexBuildMillis = nanosToMillis(System.nanoTime() - modelProviderStarted);

        this.performanceTracker.recordStartup(new PerformanceReport.StartupMetrics(
            indexedResourcesMillis,
            metadataLoadMillis,
            compatibilityInitializationMillis,
            resourcePackReadMillis,
            modelIndexBuildMillis,
            mods.size(),
            lookupSummary.modsWithAssetFiles(),
            lookupSummary.modsWithoutAssetFiles(),
            lookupSummary.namespaces(),
            lookupSummary.indexedBlockStates(),
            lookupSummary.indexedItemAssets(),
            lookupSummary.blockMatches(),
            lookupSummary.skippedBlocks(),
            lookupSummary.itemMatches(),
            lookupSummary.skippedItems(),
            lookupSummary.missingItemModelComponents(),
            this.metadataIndex.summary().fileCount(),
            this.metadataIndex.summary().ruleCount(),
            this.metadataIndex.summary().patchCount(),
            this.metadataIndex.summary().validationIssueCount()
        ));

        this.packConverters = new ArrayList<>(AssetConverters.converters(hydraulic.isDev()));
        this.packConverters.remove(AssetConverters.MODEL);
        this.packConverters.remove(AssetConverters.MANIFEST);
        this.packConverters.add(AssetConverters.create(
                new CustomModelConverter(modelProvider),
                AssetConverters.MODEL,
                AssetConverters.MODEL
        ));

        for (PackModule<?> module : ServiceLoader.load(PackModule.class)) {
            this.modules.add(module);

            GeyserApi.api().eventBus().register(this.hydraulic, module);
            module.eventListeners().forEach((eventClass, listeners) -> {
                GeyserApi.api().eventBus().subscribe(this.hydraulic, eventClass, this::callEvents);
            });

            for (ModInfo mod : mods) {
                if (shouldIgnoreMod(mod)) {
                    continue;
                }

                if (!this.shouldConvertModAssets(mod)) {
                    continue;
                }

                if (module.hasPreProcessors()) {
                    try {
                        this.preProcessModule(module, mod, modPacks.get(mod.id()));
                    } catch (Throwable t) {
                        LOGGER.error("Failed to pre-process mod {} for module {}", mod.id(), module.getClass().getSimpleName(), t);
                    }
                }
            }
        }

        this.performanceTracker.recordModelResolutionCache(toPerformanceCacheMetrics(StateDefinition.cacheMetrics()));

        PackListener packListener = new PackListener(this.hydraulic, this);
        GeyserApi.api().eventBus().register(this.hydraulic, packListener);
        try {
            packListener.ensurePacksPrepared();
        } catch (Throwable t) {
            LOGGER.error("Failed to prepare Hydraulic resource packs during startup", t);
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void preProcessModule(@NotNull PackModule<?> rawModule, @NotNull ModInfo mod, @NotNull Collection<ResourcePack> packs) {
        PackModule module = rawModule;
        module.preProcess0(new PackPreProcessContext(this.hydraulic, mod, module, packs, modelProvider));
    }

    /**
     * Creates the pack for the given mod.
     *
     * @param mod the mod to create the pack for
     * @param packPath the path to the pack
     * @return {@code true} if the pack was created, {@code false} otherwise
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    PackCreationResult createPack(@NotNull ModInfo mod, @NotNull Path packPath) {
        List<ConverterPipeline<?, ?>> pipelines = new ArrayList<>(packConverters);
        pipelines.add(AssetConverters.create(new MetadataPackModule(mod)));

        PackConverter converter = new PackConverter()
                .packName(mod.name())
                .logListener(new PackLogListener(LoggerFactory.getLogger(LOGGER.getName() + "/" + mod.id())))
                .converters(pipelines)
                .output(packPath)
                .vanillaPackPath(vanillaPath)
                .vanillaPackVersion(SharedConstants.getCurrentVersion().id())
                .textureSubdirectory(mod.namespace())
                .packageHandler(new PackPackager());

        converter.postProcessor((javaPack, bedrockPack) -> {
            for (PackModule<?> module : this.modules) {
                PackPostProcessContext context = new PackPostProcessContext(this.hydraulic, mod, module, converter, javaPack, bedrockPack, packPath, modelProvider);
                if (!module.test(context)) {
                    continue;
                }

                module.postProcess0(context);
            }
        });

        boolean created;
        try {
            for (final Path root : mod.roots()) {
                converter.input(root, false).convert();
            }
        } catch (IOException ex) {
            LOGGER.error("Failed to convert mod {} to pack", mod.id(), ex);
            PackValidationReport.ModValidation validation = this.packValidator.conversionFailed(
                packPath,
                "pack.conversion.failed",
                "Pack conversion failed before export completed.",
                "Inspect the conversion logs for this mod and resolve the reported asset or conversion errors."
            );
            this.packValidationTracker.record(mod.id(), validation);
            this.performanceTracker.recordModelResolutionCache(toPerformanceCacheMetrics(StateDefinition.cacheMetrics()));
            return new PackCreationResult(false, validation);
        }

        // Now export the pack
        try {
            converter.pack();
        } catch (IOException ex) {
            LOGGER.error("Failed to export pack for mod {}", mod.id(), ex);
            PackValidationReport.ModValidation validation = this.packValidator.conversionFailed(
                packPath,
                "pack.export.failed",
                "Pack export failed before the generated archive could be finalized.",
                "Inspect the packaging logs and ensure the generated pack path is writable and not locked."
            );
            this.packValidationTracker.record(mod.id(), validation);
            this.performanceTracker.recordModelResolutionCache(toPerformanceCacheMetrics(StateDefinition.cacheMetrics()));
            return new PackCreationResult(false, validation);
        }

        created = Files.exists(packPath);
        PackValidationReport.ModValidation validation = this.packValidator.validate(packPath);
        this.packValidationTracker.record(mod.id(), validation);
        if (!validation.valid()) {
            LOGGER.warn(
                "Generated pack for mod {} failed validation (errors={}, warnings={}, manualActions={})",
                mod.id(),
                validation.errorCount(),
                validation.warningCount(),
                validation.manualActionCount()
            );
        }
        this.performanceTracker.recordModelResolutionCache(toPerformanceCacheMetrics(StateDefinition.cacheMetrics()));
        return new PackCreationResult(created && validation.valid(), validation);
    }

    private void callEvents(@NotNull Event event) {
        for (ModInfo mod : this.hydraulic.mods()) {
            if (shouldIgnoreMod(mod)) {
                continue;
            }

            this.callEvent(mod, event);
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void callEvent(@NotNull ModInfo mod, @NotNull Event event) {
        for (PackModule<?> module : this.modules) {
            module.call(event.getClass(), new PackEventContext(this.hydraulic, mod, module, event));
        }
    }

    private LookupSummary initializeModLookups() {
        Map<String, ModResourceIndex> modResourceIndexes = this.modResourceIndexes;
        modResourceIndexes.clear();
        int modsWithAssetFiles = 0;
        int modsWithoutAssetFiles = 0;
        int indexedBlockStates = 0;
        int indexedItemAssets = 0;

        // Step 1: Index each mod's resource roots once, then map namespaces to owning mods
        final Multimap<String, ModInfo> namespacesToMods = this.namespacesToMods;
        namespacesToMods.clear();
        for (final ModInfo mod : hydraulic.mods()) {
            ModResourceIndex resourceIndex = ModResourceIndex.create(mod, LOGGER);
            modResourceIndexes.put(mod.id(), resourceIndex);
            indexedBlockStates += resourceIndex.blockStateCount();
            indexedItemAssets += resourceIndex.itemAssetCount();
            if (resourceIndex.hasAssetFiles()) {
                modsWithAssetFiles++;
            } else {
                modsWithoutAssetFiles++;
            }
            for (String namespace : resourceIndex.namespaces()) {
                if (!namespace.equals("minecraft")) {
                    namespacesToMods.put(namespace, mod);
                }
            }
        }

        // Step 2: Use namespace information to lookup which mods contain what blockstates
        final Multimap<String, Identifier> modsToBlocks = this.modsToBlocks;
        modsToBlocks.clear();
        int skippedBlocks = 0;
        for (final Identifier block : BuiltInRegistries.BLOCK.keySet()) {
            if (block.getNamespace().equals("minecraft")) continue;
            boolean found = false;
            for (final ModInfo mod : namespacesToMods.get(block.getNamespace())) {
                ModResourceIndex resourceIndex = modResourceIndexes.get(mod.id());
                if (resourceIndex != null && resourceIndex.hasBlockState(block)) {
                    modsToBlocks.put(mod.id(), block);
                    found = true;
                    break;
                }
            }
            if (!found) {
                skippedBlocks++;
            }
        }

        // Step 3: Use namespace information to lookup which mods contain item definitions or legacy item models
        // There's no ordering requirement between this and Step 2.
        final Multimap<String, Identifier> modsToItems = this.modsToItems;
        modsToItems.clear();
        int missingItemModelComponents = 0;
        int skippedItems = 0;
        for (final Identifier itemId : BuiltInRegistries.ITEM.keySet()) {
            if (itemId.getNamespace().equals("minecraft")) continue;

            Item item = BuiltInRegistries.ITEM.getValue(itemId);
            Identifier itemModel = item.components().get(DataComponents.ITEM_MODEL);
            // Item model is missing, can't do much here
            if (itemModel == null) {
                missingItemModelComponents++;
                continue;
            }

            boolean found = false;
            for (final ModInfo mod : namespacesToMods.get(itemId.getNamespace())) {
                ModResourceIndex resourceIndex = modResourceIndexes.get(mod.id());
                if (resourceIndex != null && resourceIndex.hasItemAsset(itemModel)) {
                    modsToItems.put(mod.id(), itemId);
                    found = true;
                    break;
                }
            }

            if (!found) {
                skippedItems++;
            }
        }

        LOGGER.info(
            "Indexed mod resources for Hydraulic startup (mods={}, namespaces={}, blockMatches={}, skippedBlocks={}, itemMatches={}, skippedItems={}, missingItemModels={})",
            modResourceIndexes.size(),
            namespacesToMods.keySet().size(),
            modsToBlocks.size(),
            skippedBlocks,
            modsToItems.size(),
            skippedItems,
            missingItemModelComponents
        );

        return new LookupSummary(
            modResourceIndexes.size(),
            modsWithAssetFiles,
            modsWithoutAssetFiles,
            namespacesToMods.keySet().size(),
            indexedBlockStates,
            indexedItemAssets,
            modsToBlocks.size(),
            skippedBlocks,
            modsToItems.size(),
            skippedItems,
            missingItemModelComponents
        );
    }

    private void initializeCompatibilityRegistry() {
        Path dataPath = this.hydraulic.dataFolder(Constants.MOD_ID);
        Path metadataPath = dataPath.resolve("metadata");
        this.compatibilityManager = new CompatibilityManager(LOGGER, dataPath);
        this.compatibilityRegistry = this.compatibilityManager.initialize(
            this.hydraulic.mods(),
            this.namespacesToMods,
            this.modsToBlocks,
            this.modsToItems,
            this.modResourceIndexes,
            this.metadataIndex,
            this::shouldIgnoreMod
        );

        if (!this.metadataIndex.isEmpty()) {
            LOGGER.info(
                "Loaded structural metadata overrides from {} (files={}, blockMappings={}, itemMappings={}, recipeMappings={}, entityMappings={}, menuMappings={}, patches={}, rules={}, validationIssues={})",
                metadataPath,
                this.metadataIndex.summary().fileCount(),
                this.metadataIndex.summary().blockMappingCount(),
                this.metadataIndex.summary().itemMappingCount(),
                this.metadataIndex.summary().recipeMappingCount(),
                this.metadataIndex.summary().entityMappingCount(),
                this.metadataIndex.summary().menuMappingCount(),
                this.metadataIndex.summary().patchCount(),
                this.metadataIndex.summary().ruleCount(),
                this.metadataIndex.summary().validationIssueCount()
            );
        }
    }

    void recordPackConversionMetrics(@NotNull PerformanceReport.PackConversionMetrics metrics) {
        this.performanceTracker.recordPackConversion(metrics);
    }

    /**
     * Merges the current pack-validation findings into the compatibility report and rewrites
     * {@code compatibility-report.json}, so manual actions and validation issues are visible
     * alongside compatibility findings instead of only in the sibling pack-validation artifact.
     */
    void syncCompatibilityValidation() {
        PackValidationReport validationReport = this.packValidationTracker.snapshot();
        if (validationReport.perMod().isEmpty()) {
            return;
        }

        Map<String, CompatibilityReport.PackValidationSummary> summaries = new LinkedHashMap<>();
        for (Map.Entry<String, PackValidationReport.ModValidation> entry : validationReport.perMod().entrySet()) {
            PackValidationReport.ModValidation validation = entry.getValue();
            summaries.put(entry.getKey(), new CompatibilityReport.PackValidationSummary(
                validation.valid(),
                validation.errorCount(),
                validation.warningCount(),
                validation.manualActionCount(),
                validation.manualActions()
            ));
        }

        CompatibilityReport updatedReport = this.compatibilityRegistry.report().withPackValidation(summaries);
        this.compatibilityRegistry = this.compatibilityRegistry.withReport(updatedReport);
        if (this.compatibilityManager != null) {
            this.compatibilityManager.writeReport(updatedReport);
        }
    }

    @NotNull
    PerformanceReport performanceReport() {
        return this.performanceTracker.snapshot();
    }

    private static long nanosToMillis(long nanos) {
        return nanos / 1_000_000L;
    }

    record PackCreationResult(boolean success, @NotNull PackValidationReport.ModValidation validation) {
    }

    @NotNull
    private static PerformanceReport.CacheMetrics toPerformanceCacheMetrics(@NotNull StateDefinition.CacheMetrics cacheMetrics) {
        return new PerformanceReport.CacheMetrics(cacheMetrics.hits(), cacheMetrics.misses());
    }

    private record LookupSummary(
        int indexedMods,
        int modsWithAssetFiles,
        int modsWithoutAssetFiles,
        int namespaces,
        int indexedBlockStates,
        int indexedItemAssets,
        int blockMatches,
        int skippedBlocks,
        int itemMatches,
        int skippedItems,
        int missingItemModelComponents
    ) {
    }

    /**
     * Creates a {@link ModelStitcher.Provider} that first searches mods, then the Vanilla pack.
     *
     * @param mods The mods to search through.
     * @param modPacks A {@link Map} from mod ID to a {@link List} of {@link ResourcePack}s contained within that mod.
     *                 There may be multiple {@link ResourcePack}s in a mod if there are multiple resource roots for the
     *                 mod.
     * @return A {@link ModelStitcher.Provider} that searches through mods and the Vanilla pack.
     */
    private static ModelStitcher.Provider createModelProvider(
        Collection<ModInfo> mods,
        Map<String, List<ResourcePack>> modPacks,
        Path vanillaPath
    ) {
        final List<ResourcePack> flattenedPacks = mods.stream()
            .map(ModInfo::id)
            .map(modPacks::get)
            .flatMap(List::stream)
            .toList();

        ResourcePack vanillaResourcePack = MinecraftResourcePackReader.minecraft().readFromZipFile(vanillaPath);
        Map<Key, Model> modelIndex = new LinkedHashMap<>();
        for (ResourcePack pack : flattenedPacks) {
            for (Model model : pack.models()) {
                modelIndex.putIfAbsent(model.key(), model);
            }
        }
        ConcurrentMap<Key, Optional<Model>> resolvedModels = new ConcurrentHashMap<>(Math.max(16, modelIndex.size()));
        modelIndex.forEach((key, model) -> resolvedModels.put(key, Optional.of(model)));

        return key -> resolvedModels.computeIfAbsent(key, ignored -> Optional.ofNullable(vanillaResourcePack.model(ignored))).orElse(null);
    }

    public boolean shouldIgnoreMod(ModInfo mod) {
        return IGNORED_MODS.contains(mod.id()) || hydraulic.getConfig().ignoredMods().contains(mod.id());
    }

    boolean shouldConvertModAssets(@NotNull ModInfo mod) {
        ModResourceIndex resourceIndex = this.modResourceIndexes.get(mod.id());
        return resourceIndex != null && resourceIndex.hasAssetFiles();
    }

    public ListMultimap<String, ModInfo> getNamespacesToMods() {
        return namespacesToMods;
    }

    public ListMultimap<String, Identifier> getModsToBlocks() {
        return modsToBlocks;
    }

    public ListMultimap<String, Identifier> getModsToItems() {
        return modsToItems;
    }

    public Path getVanillaPath() {
        return vanillaPath;
    }

    @NotNull
    public MetadataIndex metadataIndex() {
        return this.metadataIndex;
    }

    @NotNull
    public CompatibilityRegistry compatibilityRegistry() {
        return this.compatibilityRegistry;
    }

    @NotNull
    public MappingResolver mappingResolver() {
        return this.compatibilityRegistry.mappingResolver();
    }
}
