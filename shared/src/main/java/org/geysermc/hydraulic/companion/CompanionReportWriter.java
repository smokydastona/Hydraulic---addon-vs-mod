package org.geysermc.hydraulic.companion;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Writes {@code config/hydraulic/reports/companion-report.json}, an explicit,
 * human-readable record of every discovered companion package, its build artifact,
 * and the honest support status of every declared capability.
 */
public final class CompanionReportWriter {
    private final Logger logger;
    private final Path reportPath;

    public CompanionReportWriter(@NotNull Logger logger, @NotNull Path dataFolder) {
        this.logger = logger;
        this.reportPath = dataFolder.resolve("reports").resolve("companion-report.json");
    }

    public void write(@NotNull CompanionRegistry registry, boolean signalBridgeInstalled) {
        JsonObject root = new JsonObject();
        root.addProperty("signalBridgeInstalled", signalBridgeInstalled);
        root.addProperty("signalObjective", CompanionSignalBridge.OBJECTIVE_NAME);

        JsonArray companions = new JsonArray();
        for (CompanionRegistry.Entry entry : registry.entries().values()) {
            companions.add(writeCompanion(entry));
        }
        root.add("companions", companions);

        JsonArray rejectedJson = new JsonArray();
        for (CompanionPackage rejectedPackage : registry.rejected()) {
            rejectedJson.add(writeRejected(rejectedPackage));
        }
        root.add("rejected", rejectedJson);

        try {
            Files.createDirectories(this.reportPath.getParent());
            Files.writeString(this.reportPath, new GsonBuilder().setPrettyPrinting().create().toJson(root), StandardCharsets.UTF_8);
        } catch (IOException e) {
            this.logger.error("Failed to write companion report to {}", this.reportPath, e);
        }
    }

    @NotNull
    private JsonObject writeCompanion(@NotNull CompanionRegistry.Entry entry) {
        CompanionPackage pkg = entry.companionPackage();
        CompanionManifest manifest = pkg.manifest();

        JsonObject companionJson = new JsonObject();
        companionJson.addProperty("id", pkg.id());
        if (manifest != null) {
            companionJson.addProperty("name", manifest.name());
            companionJson.addProperty("version", manifest.version());
            companionJson.addProperty("executionMode", manifest.executionMode().name());
        }
        companionJson.addProperty("resourcePackMcpack", entry.buildResult().mcpackPath().toString());
        companionJson.addProperty("sha256", entry.buildResult().sha256());
        companionJson.addProperty("sizeBytes", entry.buildResult().sizeBytes());
        companionJson.addProperty("rebuilt", entry.buildResult().rebuilt());
        companionJson.addProperty("hasBehaviorPack", pkg.behaviorPackPath() != null);
        companionJson.addProperty("deliveredToGeyser", true);
        companionJson.addProperty("behaviorPackExecutedByGeyser", false);

        JsonArray issuesJson = new JsonArray();
        for (CompanionValidationIssue issue : pkg.issues()) {
            issuesJson.add(writeIssue(issue));
        }
        companionJson.add("issues", issuesJson);

        JsonArray capabilitiesJson = new JsonArray();
        for (CompanionCapabilityResult result : entry.capabilityResults()) {
            JsonObject capabilityJson = new JsonObject();
            capabilityJson.addProperty("id", result.capability().id());
            capabilityJson.addProperty("description", result.capability().description());
            capabilityJson.addProperty("status", result.status().name());
            capabilityJson.addProperty("reason", result.reason());
            capabilitiesJson.add(capabilityJson);
        }
        companionJson.add("capabilities", capabilitiesJson);

        return companionJson;
    }

    @NotNull
    private JsonObject writeRejected(@NotNull CompanionPackage rejectedPackage) {
        JsonObject rejectedJson = new JsonObject();
        rejectedJson.addProperty("id", rejectedPackage.id());
        rejectedJson.addProperty("root", rejectedPackage.root().toString());

        JsonArray issuesJson = new JsonArray();
        for (CompanionValidationIssue issue : rejectedPackage.issues()) {
            issuesJson.add(writeIssue(issue));
        }
        rejectedJson.add("issues", issuesJson);
        return rejectedJson;
    }

    @NotNull
    private JsonObject writeIssue(@NotNull CompanionValidationIssue issue) {
        JsonObject issueJson = new JsonObject();
        issueJson.addProperty("level", issue.level().name());
        issueJson.addProperty("message", issue.message());
        return issueJson;
    }
}
