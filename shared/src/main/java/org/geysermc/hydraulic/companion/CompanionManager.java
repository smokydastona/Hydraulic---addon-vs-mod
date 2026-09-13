package org.geysermc.hydraulic.companion;

import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Discovers, validates, fingerprints, builds, and registers Phlodgate companion
 * packages: Bedrock resource+behavior pack pairs that Hydraulic can automatically
 * deliver to Bedrock players through Geyser (resource pack only), while honestly
 * reporting which declared behavior-pack capabilities do or do not have a real
 * Java-side runtime bridge.
 *
 * <p>Resource packs are content-addressed and registered through the same
 * {@code GeyserDefineResourcePacksEvent} mechanism Hydraulic already uses for
 * mod-converted packs, so they only ever reach Bedrock sessions connecting through
 * Geyser. Behavior packs are never sent over the network; Geyser does not install
 * or execute Bedrock behavior-pack scripts on the client.
 */
public final class CompanionManager {
    private final Logger logger;
    private final Path companionsRoot;
    private final CompanionValidator validator = new CompanionValidator();
    private final CompanionPackBuilder packBuilder;
    private final CompanionCapabilityClassifier capabilityClassifier = new CompanionCapabilityClassifier();
    private final CompanionSignalBridge signalBridge;
    private final CompanionReportWriter reportWriter;
    private final CompanionRegistry registry = new CompanionRegistry();

    public CompanionManager(@NotNull Logger logger, @NotNull Path dataFolder) {
        this.logger = logger;
        this.companionsRoot = dataFolder.resolve("companions");
        this.packBuilder = new CompanionPackBuilder(logger, dataFolder.resolve("cache").resolve("companions"));
        this.signalBridge = new CompanionSignalBridge(logger);
        this.reportWriter = new CompanionReportWriter(logger, dataFolder);
    }

    /**
     * Discovers, validates, and builds every companion package under the companions
     * directory. Must be called before packs are registered with Geyser.
     */
    public void initialize() {
        CompanionBuiltinBootstrap.ensureLayout(this.logger, this.companionsRoot);

        if (!Files.isDirectory(this.companionsRoot)) {
            return;
        }

        try (var children = Files.list(this.companionsRoot)) {
            children.filter(Files::isDirectory).forEach(this::processCompanionDirectory);
        } catch (IOException e) {
            this.logger.error("Failed to scan companions directory {}", this.companionsRoot, e);
        }

        int total = this.registry.entries().size() + this.registry.rejected().size();
        if (total > 0) {
            this.logger.info(
                    "Discovered {} companion package(s): {} valid, {} rejected",
                    total, this.registry.entries().size(), this.registry.rejected().size()
            );
        }
    }

    private void processCompanionDirectory(@NotNull Path directory) {
        CompanionPackage companionPackage = this.validator.validate(directory);

        if (!companionPackage.isValid()) {
            for (CompanionValidationIssue issue : companionPackage.issues()) {
                if (issue.level() == CompanionValidationIssue.Level.ERROR) {
                    this.logger.warn("Rejecting companion package '{}': {}", companionPackage.id(), issue.message());
                } else {
                    this.logger.info("Companion package '{}' warning: {}", companionPackage.id(), issue.message());
                }
            }
            this.registry.reject(companionPackage);
            return;
        }

        CompanionManifest manifest = companionPackage.manifest();
        try {
            CompanionBuildResult buildResult = this.packBuilder.build(companionPackage.id(), companionPackage.resourcePackPath());
            var capabilityResults = this.capabilityClassifier.classify(manifest, this.signalBridge.isInstalled());
            this.registry.register(new CompanionRegistry.Entry(companionPackage, buildResult, capabilityResults));
            this.logger.info(
                    "Registered companion package '{}' v{} ({} capability declaration(s))",
                    manifest.id(), manifest.version(), manifest.capabilities().size()
            );
        } catch (IOException e) {
            this.logger.error("Failed to build resource pack for companion '{}'", companionPackage.id(), e);
            this.registry.reject(companionPackage);
        }
    }

    /**
     * Installs the Java-side companion detection signal (a Bedrock-visible scoreboard
     * objective), re-classifies every companion's declared capabilities against the
     * now-known signal bridge state, and writes the final companion report. Must be
     * called once the server has started, after {@link #initialize()}.
     */
    public void installSignalBridge(@NotNull MinecraftServer server) {
        this.signalBridge.install(server);

        Map<String, CompanionRegistry.Entry> current = new LinkedHashMap<>(this.registry.entries());
        for (CompanionRegistry.Entry entry : current.values()) {
            CompanionManifest manifest = entry.companionPackage().manifest();
            var capabilityResults = this.capabilityClassifier.classify(manifest, this.signalBridge.isInstalled());
            this.registry.register(new CompanionRegistry.Entry(entry.companionPackage(), entry.buildResult(), capabilityResults));
        }

        this.reportWriter.write(this.registry, this.signalBridge.isInstalled());
    }

    /**
     * @return companion id to built {@code .mcpack} path, to register with Geyser
     *         alongside converted mod resource packs.
     */
    @NotNull
    public Map<String, Path> registerableResourcePacks() {
        return this.registry.registerableResourcePacks();
    }

    @NotNull
    public CompanionRegistry registry() {
        return this.registry;
    }
}
