package org.geysermc.hydraulic.compat.corpus;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Converts a local Bedrock addon source tree or archive into one normalized,
 * license-aware corpus entry. It never contacts a remote service and never
 * copies source code or addon assets into the corpus.
 */
public final class CorpusSnapshotImporter {
    private static final long MAX_FILE_BYTES = 4L * 1024L * 1024L;
    private static final long MAX_TOTAL_BYTES = 64L * 1024L * 1024L;
    private static final int MAX_FILES = 50_000;

    private final Logger logger;
    private final AddonCorpusLoader loader;

    public CorpusSnapshotImporter(@NotNull Logger logger, @NotNull AddonCorpusLoader loader) {
        this.logger = logger;
        this.loader = loader;
    }

    @NotNull
    public ImportResult importSnapshot(@NotNull Path snapshot, @NotNull ImportRequest request) {
        try {
            SnapshotFiles files = snapshot.toString().toLowerCase(Locale.ROOT).endsWith(".zip")
                ? SnapshotFiles.fromZip(snapshot)
                : SnapshotFiles.fromDirectory(snapshot);
            AddonCorpusEntry entry = this.extract(files, request);
            List<String> validationErrors = AddonCorpusValidator.validate(entry);
            if (!validationErrors.isEmpty()) {
                throw new IllegalArgumentException("Corpus entry validation failed: " + validationErrors);
            }
            this.loader.storeEntry(entry, "generated");
            this.loader.refreshIndexFromSnapshots();
            return new ImportResult(entry, files.paths().size(), List.copyOf(files.warnings()));
        } catch (IOException | RuntimeException exception) {
            this.logger.warn("Failed to import local corpus snapshot {}", snapshot, exception);
            throw new IllegalArgumentException("Failed to import corpus snapshot " + snapshot, exception);
        }
    }

    @NotNull
    private AddonCorpusEntry extract(@NotNull SnapshotFiles files, @NotNull ImportRequest request) {
        Set<String> behaviorEntities = new TreeSet<>();
        Set<String> behaviorBlocks = new TreeSet<>();
        Set<String> behaviorItems = new TreeSet<>();
        Set<String> recipes = new TreeSet<>();
        Set<String> functions = new TreeSet<>();
        Set<String> scripts = new TreeSet<>();
        Set<String> textures = new TreeSet<>();
        Set<String> models = new TreeSet<>();
        Set<String> animations = new TreeSet<>();
        Set<String> particles = new TreeSet<>();
        Set<String> sounds = new TreeSet<>();
        Set<String> uiElements = new TreeSet<>();
        Set<String> storageTypes = new TreeSet<>();
        Set<String> machineTypes = new TreeSet<>();
        Set<String> transferTypes = new TreeSet<>();
        Set<String> fluidTypes = new TreeSet<>();
        Set<String> energyTypes = new TreeSet<>();
        Set<String> automationTypes = new TreeSet<>();
        Set<String> networkingTypes = new TreeSet<>();
        Set<String> dependencies = new TreeSet<>();
        List<String> manifestEvidence = new ArrayList<>();
        List<String> scriptEvidence = new ArrayList<>();
        List<String> functionEvidence = new ArrayList<>();
        List<String> componentEvidence = new ArrayList<>();
        List<String> uiEvidence = new ArrayList<>();
        Map<String, String> customComponents = new LinkedHashMap<>();
        Map<String, String> renderControllers = new LinkedHashMap<>();
        Map<String, String> heuristicScores = new LinkedHashMap<>();
        List<String> limitations = new ArrayList<>();
        List<String> performance = new ArrayList<>();
        List<String> persistence = new ArrayList<>();
        List<String> runtimeHooks = new ArrayList<>();
        List<String> transferSemantics = new ArrayList<>();
        List<String> uiMethods = new ArrayList<>();
        List<String> implementationDependencies = new ArrayList<>();
        List<String> strongIndicators = new ArrayList<>();
        List<String> weakIndicators = new ArrayList<>();
        List<String> gaps = new ArrayList<>();

        boolean hasBehaviorPack = false;
        boolean hasResourcePack = false;
        String behaviorFormat = null;
        String resourceFormat = null;
        boolean usesGameTest = false;
        boolean usesScriptApi = false;
        boolean hasCustomComponents = false;
        boolean hasUi = false;
        int manifestCount = 0;

        for (String path : files.paths()) {
            String normalized = path.toLowerCase(Locale.ROOT);
            boolean behaviorPath = normalized.contains("behavior_packs/") || normalized.contains("behavior-pack/");
            boolean resourcePath = normalized.contains("resource_packs/") || normalized.contains("resource-pack/");
            String fileName = normalized.substring(normalized.lastIndexOf('/') + 1);

            if (fileName.equals("manifest.json")) {
                manifestCount++;
                manifestEvidence.add(path);
                ManifestFacts manifest = parseManifest(files.read(path));
                dependencies.addAll(manifest.dependencies());
                if (behaviorPath || (!resourcePath && "data".equals(manifest.moduleType()))) {
                    hasBehaviorPack = true;
                    behaviorFormat = firstNonBlank(behaviorFormat, manifest.formatVersion());
                }
                if (resourcePath || (!behaviorPath && "resources".equals(manifest.moduleType()))) {
                    hasResourcePack = true;
                    resourceFormat = firstNonBlank(resourceFormat, manifest.formatVersion());
                }
                strongIndicators.add("manifest:" + path);
                continue;
            }

            if (isScript(normalized)) {
                scripts.add(path);
                scriptEvidence.add(path);
                String content = files.read(path).toLowerCase(Locale.ROOT);
                usesScriptApi |= content.contains("@minecraft/") || content.contains("script api") || content.contains("system.run");
                usesGameTest |= content.contains("gametest") || content.contains("@minecraft/server-gametest");
                hasCustomComponents |= content.contains("custom component") || content.contains("registercustomcomponent");
                addPatternFacts(content, storageTypes, machineTypes, transferTypes, fluidTypes, energyTypes, automationTypes, networkingTypes, persistence, runtimeHooks, transferSemantics);
                continue;
            }
            if (normalized.contains("functions/") && normalized.endsWith(".mcfunction")) {
                functions.add(path);
                functionEvidence.add(path);
                continue;
            }
            if (normalized.contains("recipes/") && normalized.endsWith(".json")) {
                recipes.add(path);
                continue;
            }
            if (normalized.contains("textures/") && isAsset(normalized)) {
                textures.add(path);
                continue;
            }
            if (normalized.contains("models/") && isAsset(normalized)) {
                models.add(path);
                continue;
            }
            if (normalized.contains("animations/") && isAsset(normalized)) {
                animations.add(path);
                continue;
            }
            if (normalized.contains("particles/") && isAsset(normalized)) {
                particles.add(path);
                continue;
            }
            if (normalized.contains("sounds/") && isAsset(normalized)) {
                sounds.add(path);
                continue;
            }
            if (normalized.contains("ui/") || normalized.contains("ui/") || normalized.endsWith(".json")) {
                if (normalized.contains("ui/") || normalized.contains("_ui.json")) {
                    uiElements.add(path);
                    uiEvidence.add(path);
                    hasUi = true;
                }
            }
            if (normalized.contains("blocks/") && normalized.endsWith(".json")) {
                behaviorBlocks.add(path);
            }
            if (normalized.contains("items/") && normalized.endsWith(".json")) {
                behaviorItems.add(path);
            }
            if (normalized.contains("entities/") && normalized.endsWith(".json")) {
                behaviorEntities.add(path);
            }
            String content = files.readIfText(path);
            if (content != null && (content.contains("minecraft:custom_components") || content.contains("menu_category"))) {
                hasCustomComponents = true;
                componentEvidence.add(path);
                customComponents.put(path, "declared");
            }
        }

        if (usesScriptApi) {
            strongIndicators.add("script_api_usage");
            runtimeHooks.add("script_api");
        } else {
            gaps.add("script_api_usage_not_detected");
        }
        if (usesGameTest) {
            strongIndicators.add("gametest_usage");
            runtimeHooks.add("gametest");
        }
        if (hasCustomComponents) {
            strongIndicators.add("custom_components");
        }
        if (hasUi) {
            uiMethods.add("bedrock_ui");
        }
        if (!functions.isEmpty()) {
            runtimeHooks.add("mcfunction");
        }
        if (!recipes.isEmpty()) {
            machineTypes.add("recipe");
        }
        if (!textures.isEmpty() || !models.isEmpty() || !animations.isEmpty()) {
            strongIndicators.add("resource_pack_assets");
        }
        if (storageTypes.isEmpty() && machineTypes.isEmpty() && transferTypes.isEmpty() && fluidTypes.isEmpty() && energyTypes.isEmpty() && automationTypes.isEmpty()) {
            gaps.add("no_capability_pattern_detected");
        }
        if (files.paths().size() > 10_000) {
            performance.add("large_snapshot");
            limitations.add("extraction is metadata-only and does not execute scripts");
        }

        int evidenceGroups = 0;
        evidenceGroups += manifestCount > 0 ? 1 : 0;
        evidenceGroups += !scriptEvidence.isEmpty() ? 1 : 0;
        evidenceGroups += !componentEvidence.isEmpty() ? 1 : 0;
        evidenceGroups += !recipes.isEmpty() ? 1 : 0;
        evidenceGroups += !textures.isEmpty() || !models.isEmpty() ? 1 : 0;
        double confidence = Math.min(0.95D, 0.25D + evidenceGroups * 0.14D);
        heuristicScores.put("manifest", String.format(Locale.ROOT, "%.2f", manifestCount > 0 ? 1.0D : 0.0D));
        heuristicScores.put("script", String.format(Locale.ROOT, "%.2f", usesScriptApi ? 1.0D : 0.0D));
        heuristicScores.put("components", String.format(Locale.ROOT, "%.2f", hasCustomComponents ? 1.0D : 0.0D));
        heuristicScores.put("assets", String.format(Locale.ROOT, "%.2f", textures.isEmpty() && models.isEmpty() ? 0.0D : 1.0D));

        AddonCorpusEntry.AddonSource source = new AddonCorpusEntry.AddonSource(
            request.sourceType(), request.sourceUrl(), request.repositoryUrl(), request.homepageUrl(), request.documentationUrl()
        );
        AddonCorpusEntry.AddonLicense license = new AddonCorpusEntry.AddonLicense(
            request.licenseType(), request.licenseUrl(), request.licenseText(), request.allowsRedistribution(),
            request.allowsModification(), request.allowsCommercialUse(), request.requiresAttribution()
        );
        AddonCorpusEntry.AddonAdmissibility sourceAdmissibility = CorpusAdmissibilityChecker.checkSourceAdmissibility(source, license);
        AddonCorpusEntry.AddonEvidence evidence = new AddonCorpusEntry.AddonEvidence(
            List.copyOf(manifestEvidence), List.copyOf(scriptEvidence), List.copyOf(functionEvidence),
            List.copyOf(componentEvidence), List.copyOf(uiEvidence), Map.copyOf(heuristicScores)
        );
        AddonCorpusEntry.AddonAdmissibility evidenceAdmissibility = CorpusAdmissibilityChecker.checkEvidenceAdmissibility(evidence, source);
        AddonCorpusEntry.AddonAdmissibility admissibility = sourceAdmissibility.isAdmissible() && evidenceAdmissibility.isAdmissible()
            ? sourceAdmissibility
            : new AddonCorpusEntry.AddonAdmissibility(
                false,
                sourceAdmissibility.isAdmissible() ? evidenceAdmissibility.reason() : sourceAdmissibility.reason(),
                sourceAdmissibility.isAdmissible() ? evidenceAdmissibility.denialReason() : sourceAdmissibility.denialReason(),
                List.copyOf(new LinkedHashSet<>(List.of(
                    sourceAdmissibility.constraints(), evidenceAdmissibility.constraints()
                ).stream().flatMap(List::stream).toList()))
            );

        return new AddonCorpusEntry(
            new AddonCorpusEntry.AddonIdentity(request.corpusId(), request.bedrockIdentifier(), request.author(), request.displayName(), request.description()),
            source,
            license,
            admissibility,
            new AddonCorpusEntry.AddonVersions(request.latestVersion(), Set.copyOf(request.supportedBedrockVersions()), request.minBedrockVersion(), request.maxBedrockVersion(), List.copyOf(request.allVersions())),
            new AddonCorpusEntry.AddonBehaviorPack(hasBehaviorPack, behaviorFormat, behaviorEntities, behaviorBlocks, behaviorItems, recipes, functions, scripts, customComponents),
            new AddonCorpusEntry.AddonResourcePack(hasResourcePack, resourceFormat, textures, models, animations, particles, sounds, uiElements, renderControllers),
            new AddonCorpusEntry.AddonCapabilities(storageTypes, machineTypes, transferTypes, fluidTypes, energyTypes, automationTypes, networkingTypes, Map.copyOf(customComponents)),
            evidence,
            new AddonCorpusEntry.AddonConfidence(confidence, "deterministic_local_snapshot_extraction", strongIndicators, weakIndicators, gaps),
            new AddonCorpusEntry.AddonImplementationFacts(
                implementationPattern(storageTypes, machineTypes, transferTypes, fluidTypes, energyTypes, automationTypes),
                limitations,
                performance,
                persistence,
                runtimeHooks,
                transferSemantics,
                uiMethods,
                implementationDependencies,
                reusability(storageTypes, machineTypes, transferTypes),
                adapterCandidate(machineTypes, transferTypes, fluidTypes, energyTypes, automationTypes)
            ),
            new AddonCorpusEntry.AddonProvenance(
                request.addedBy(), System.currentTimeMillis(), request.lastUpdatedBy(), request.lastUpdatedEpochMillis(),
                AddonCorpusIndex.empty().corpusVersion(), List.copyOf(files.warnings())
            )
        );
    }

    private static void addPatternFacts(
        @NotNull String content,
        @NotNull Set<String> storage,
        @NotNull Set<String> machines,
        @NotNull Set<String> transfers,
        @NotNull Set<String> fluids,
        @NotNull Set<String> energy,
        @NotNull Set<String> automation,
        @NotNull Set<String> networking,
        @NotNull List<String> persistence,
        @NotNull List<String> runtimeHooks,
        @NotNull List<String> transferSemantics
    ) {
        if (containsAny(content, "inventory", "container", "storage", "dynamicproperty")) {
            storage.add("storage");
            persistence.add("dynamic_properties_or_script_state");
        }
        if (containsAny(content, "machine", "processor", "generator", "recipe")) {
            machines.add("machine");
        }
        if (containsAny(content, "pipe", "transfer", "insert", "extract", "hopper", "conveyor")) {
            transfers.add("item_transfer");
            automation.add("routing");
            transferSemantics.add("scripted_item_transfer");
        }
        if (containsAny(content, "fluid", "liquid", "tank")) {
            fluids.add("fluid");
            transfers.add("fluid_transfer");
        }
        if (containsAny(content, "energy", "power", "battery")) {
            energy.add("energy");
            transfers.add("energy_transfer");
        }
        if (containsAny(content, "packet", "network", "scriptevent", "custom event")) {
            networking.add("custom_sync");
        }
        if (content.contains("system.run") || content.contains("world.afterevents")) {
            runtimeHooks.add("server_tick_or_event");
        }
    }

    private static boolean containsAny(@NotNull String value, @NotNull String... needles) {
        for (String needle : needles) {
            if (value.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isScript(@NotNull String path) {
        return path.endsWith(".js") || path.endsWith(".ts") || path.endsWith(".mjs") || path.endsWith(".mcfunction");
    }

    private static boolean isAsset(@NotNull String path) {
        return path.endsWith(".json") || path.endsWith(".png") || path.endsWith(".tga") || path.endsWith(".mcmeta") || path.endsWith(".lang");
    }

    private static String implementationPattern(Set<String> storage, Set<String> machines, Set<String> transfers, Set<String> fluids, Set<String> energy, Set<String> automation) {
        if (!machines.isEmpty()) return "machine";
        if (!transfers.isEmpty()) return "transfer";
        if (!fluids.isEmpty()) return "fluid_system";
        if (!energy.isEmpty()) return "energy_system";
        if (!automation.isEmpty()) return "automation_network";
        if (!storage.isEmpty()) return "storage";
        return "declared_content";
    }

    private static String reusability(Set<String> storage, Set<String> machines, Set<String> transfers) {
        return !machines.isEmpty() || !transfers.isEmpty() || !storage.isEmpty() ? "medium" : "low";
    }

    private static String adapterCandidate(Set<String> machines, Set<String> transfers, Set<String> fluids, Set<String> energy, Set<String> automation) {
        if (!machines.isEmpty()) return "generic_machine";
        if (!transfers.isEmpty() || !automation.isEmpty()) return "generic_transfer";
        if (!fluids.isEmpty()) return "fluid_bridge";
        if (!energy.isEmpty()) return "energy_bridge";
        return "presentation_only";
    }

    private static String firstNonBlank(String current, String candidate) {
        return current == null || current.isBlank() ? candidate : current;
    }

    private static ManifestFacts parseManifest(String content) {
        try {
            JsonObject object = JsonParser.parseString(content).getAsJsonObject();
            String formatVersion = object.has("format_version") ? object.get("format_version").getAsString() : null;
            String moduleType = null;
            Set<String> dependencies = new TreeSet<>();
            if (object.has("modules") && object.get("modules").isJsonArray()) {
                for (JsonElement module : object.getAsJsonArray("modules")) {
                    if (!module.isJsonObject()) continue;
                    JsonObject moduleObject = module.getAsJsonObject();
                    if (moduleObject.has("type")) moduleType = firstNonBlank(moduleType, moduleObject.get("type").getAsString());
                }
            }
            if (object.has("dependencies") && object.get("dependencies").isJsonArray()) {
                for (JsonElement dependency : object.getAsJsonArray("dependencies")) {
                    if (dependency.isJsonPrimitive()) dependencies.add(dependency.getAsString());
                    if (dependency.isJsonObject() && dependency.getAsJsonObject().has("uuid")) dependencies.add(dependency.getAsJsonObject().get("uuid").getAsString());
                }
            }
            return new ManifestFacts(formatVersion, moduleType, dependencies);
        } catch (RuntimeException exception) {
            return new ManifestFacts(null, null, Set.of());
        }
    }

    private record ManifestFacts(String formatVersion, String moduleType, Set<String> dependencies) {
    }

    public record ImportRequest(
        @NotNull String corpusId,
        @NotNull String bedrockIdentifier,
        @NotNull AddonCorpusEntry.SourceType sourceType,
        @NotNull String sourceUrl,
        String repositoryUrl,
        String homepageUrl,
        String documentationUrl,
        @NotNull String licenseType,
        String licenseUrl,
        String licenseText,
        boolean allowsRedistribution,
        boolean allowsModification,
        boolean allowsCommercialUse,
        boolean requiresAttribution,
        String author,
        String displayName,
        String description,
        @NotNull String latestVersion,
        @NotNull Set<String> supportedBedrockVersions,
        String minBedrockVersion,
        String maxBedrockVersion,
        @NotNull List<String> allVersions,
        @NotNull String addedBy,
        String lastUpdatedBy,
        Long lastUpdatedEpochMillis
    ) {
    }

    public record ImportResult(@NotNull AddonCorpusEntry entry, int inspectedFileCount, @NotNull List<String> warnings) {
    }

    private static final class SnapshotFiles {
        private final Map<String, String> textFiles;
        private final List<String> paths;
        private final List<String> warnings;

        private SnapshotFiles(Map<String, String> textFiles, List<String> paths, List<String> warnings) {
            this.textFiles = textFiles;
            this.paths = paths;
            this.warnings = warnings;
        }

        private static SnapshotFiles fromDirectory(Path root) throws IOException {
            if (!Files.isDirectory(root)) throw new IOException("snapshot is not a directory");
            Map<String, String> text = new LinkedHashMap<>();
            List<String> paths = new ArrayList<>();
            List<String> warnings = new ArrayList<>();
            long[] total = {0L};
            try (var stream = Files.walk(root)) {
                List<Path> files = stream.filter(Files::isRegularFile).sorted().toList();
                if (files.size() > MAX_FILES) throw new IOException("snapshot contains too many files");
                for (Path file : files) {
                    if (Files.isSymbolicLink(file)) {
                        warnings.add("ignored symbolic link: " + file.getFileName());
                        continue;
                    }
                    BasicFileAttributes attributes = Files.readAttributes(file, BasicFileAttributes.class);
                    if (attributes.size() > MAX_FILE_BYTES || (total[0] += attributes.size()) > MAX_TOTAL_BYTES) {
                        warnings.add("ignored oversized file: " + root.relativize(file));
                        continue;
                    }
                    String relative = root.relativize(file).toString().replace('\\', '/');
                    paths.add(relative);
                    if (isTextPath(relative)) text.put(relative, Files.readString(file, StandardCharsets.UTF_8));
                }
            }
            return new SnapshotFiles(text, List.copyOf(paths), List.copyOf(warnings));
        }

        private static SnapshotFiles fromZip(Path zipPath) throws IOException {
            Map<String, String> text = new LinkedHashMap<>();
            List<String> paths = new ArrayList<>();
            List<String> warnings = new ArrayList<>();
            long total = 0L;
            try (ZipFile zip = new ZipFile(zipPath.toFile())) {
                if (zip.size() > MAX_FILES) throw new IOException("snapshot contains too many entries");
                var entries = zip.stream().filter(entry -> !entry.isDirectory()).toList();
                for (ZipEntry entry : entries) {
                    String normalized = entry.getName().replace('\\', '/');
                    Path safe = Path.of(normalized).normalize();
                    if (safe.isAbsolute() || safe.startsWith("..") || normalized.contains("../")) {
                        warnings.add("ignored unsafe archive path: " + entry.getName());
                        continue;
                    }
                    long declaredSize = entry.getSize();
                    if (declaredSize > MAX_FILE_BYTES || (declaredSize >= 0 && total + declaredSize > MAX_TOTAL_BYTES)) {
                        warnings.add("ignored oversized archive entry: " + normalized);
                        continue;
                    }
                    byte[] bytes;
                    try (InputStream input = zip.getInputStream(entry)) {
                        bytes = input.readNBytes((int) MAX_FILE_BYTES + 1);
                    }
                    if (bytes.length > MAX_FILE_BYTES || (total += bytes.length) > MAX_TOTAL_BYTES) {
                        warnings.add("ignored oversized archive entry: " + normalized);
                        continue;
                    }
                    paths.add(normalized);
                    if (isTextPath(normalized)) text.put(normalized, new String(bytes, StandardCharsets.UTF_8));
                }
            }
            return new SnapshotFiles(text, List.copyOf(paths), List.copyOf(warnings));
        }

        private String read(String path) {
            return this.textFiles.getOrDefault(path, "");
        }

        private String readIfText(String path) {
            return this.textFiles.get(path);
        }

        private List<String> paths() {
            return this.paths;
        }

        private List<String> warnings() {
            return this.warnings;
        }

        private static boolean isTextPath(String path) {
            String normalized = path.toLowerCase(Locale.ROOT);
            return normalized.endsWith(".json") || normalized.endsWith(".js") || normalized.endsWith(".ts")
                || normalized.endsWith(".mjs") || normalized.endsWith(".mcfunction") || normalized.endsWith(".lang");
        }
    }
}
