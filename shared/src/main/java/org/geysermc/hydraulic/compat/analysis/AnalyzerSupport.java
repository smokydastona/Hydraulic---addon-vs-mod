package org.geysermc.hydraulic.compat.analysis;

import org.geysermc.hydraulic.compat.CompatibilityStatus;
import org.geysermc.hydraulic.compat.capability.Capability;
import org.geysermc.hydraulic.compat.capability.CapabilityDomain;
import org.geysermc.hydraulic.compat.capability.CapabilityProfile;
import org.geysermc.hydraulic.compat.capability.CapabilityRequirement;
import org.geysermc.hydraulic.compat.capability.CapabilityResult;
import org.geysermc.hydraulic.compat.model.CompatibilityFinding;
import org.geysermc.hydraulic.compat.model.CompatibilityObject;
import org.geysermc.hydraulic.compat.model.Confidence;
import org.geysermc.hydraulic.compat.model.Provenance;
import org.geysermc.hydraulic.compat.model.SupportLevel;
import org.geysermc.hydraulic.compat.model.SupportResult;
import org.geysermc.hydraulic.compat.mapping.ContentPatch;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class AnalyzerSupport {
    private AnalyzerSupport() {
    }

    @NotNull
    static Capability capability(@NotNull CapabilityDomain domain, @NotNull String name, @NotNull String description) {
        return new Capability(domain, name, description);
    }

    @NotNull
    static CapabilityRequirement required(@NotNull Capability capability) {
        return new CapabilityRequirement(capability, true);
    }

    @NotNull
    static CapabilityResult result(@NotNull Capability capability, boolean supported, String details) {
        return new CapabilityResult(capability, supported, details);
    }

    @NotNull
    static SupportResult support(@NotNull String scope, @NotNull SupportLevel level, @NotNull List<CapabilityResult> results, @NotNull List<String> notes) {
        List<String> supportedCapabilities = new ArrayList<>();
        List<String> missingCapabilities = new ArrayList<>();
        for (CapabilityResult result : results) {
            if (result.supported()) {
                supportedCapabilities.add(result.capability().name());
            } else {
                missingCapabilities.add(result.capability().name());
            }
        }

        Integer score = results.isEmpty() ? null : (int) Math.round((supportedCapabilities.size() * 100D) / results.size());
        CompatibilityStatus status;
        if (results.isEmpty()) {
            status = CompatibilityStatus.UNKNOWN;
        } else if (supportedCapabilities.isEmpty()) {
            status = CompatibilityStatus.NONE;
        } else if (missingCapabilities.isEmpty()) {
            status = CompatibilityStatus.COMPLETE;
        } else {
            status = CompatibilityStatus.PARTIAL;
        }

        return new SupportResult(scope, level, status, score, supportedCapabilities, missingCapabilities, notes);
    }

    @NotNull
    static CompatibilityObject object(
        @NotNull String javaIdentifier,
        @NotNull String contentType,
        @NotNull String modId,
        @NotNull Map<String, String> inventoryFacts,
        @NotNull CapabilityProfile capabilityProfile,
        @NotNull Map<String, SupportResult> supportResults,
        @NotNull Confidence confidence,
        @NotNull List<Provenance> provenance,
        @NotNull List<CompatibilityFinding> findings
    ) {
        SupportLevel overallLevel = overallLevel(supportResults);
        CompatibilityStatus overallStatus = overallStatus(supportResults);
        int overallScore = overallScore(supportResults);
        return new CompatibilityObject(javaIdentifier, contentType, modId, inventoryFacts, capabilityProfile, supportResults, overallLevel, overallStatus, overallScore, confidence, provenance, findings);
    }

    @NotNull
    static Map<String, String> inventoryFacts(boolean registered, boolean assetPresent, int metadataCount, int patchCount) {
        Map<String, String> facts = new LinkedHashMap<>();
        facts.put("registered", Boolean.toString(registered));
        facts.put("asset_present", Boolean.toString(assetPresent));
        facts.put("metadata_count", Integer.toString(metadataCount));
        facts.put("patch_count", Integer.toString(patchCount));
        return facts;
    }

    @NotNull
    static List<Provenance> provenance(@NotNull String analyzerName, boolean overridden, @NotNull List<ContentPatch> patches, @NotNull List<String> metadataSources) {
        List<Provenance> provenance = new ArrayList<>();
        provenance.add(new Provenance("inventory", analyzerName, null, overridden));
        for (String source : metadataSources) {
            provenance.add(new Provenance("metadata-mapping", analyzerName, source, true));
        }
        for (ContentPatch patch : patches) {
            provenance.add(new Provenance("metadata-patch", analyzerName, patch.sourcePath(), true));
        }
        return List.copyOf(provenance);
    }

    @NotNull
    static SupportLevel overallLevel(@NotNull Map<String, SupportResult> supportResults) {
        boolean anyUnsupported = false;
        boolean anyVisualOnly = false;
        boolean anyApproximated = false;
        boolean anyAdapted = false;
        boolean allNative = !supportResults.isEmpty();

        for (SupportResult result : supportResults.values()) {
            anyUnsupported |= result.level() == SupportLevel.UNSUPPORTED;
            anyVisualOnly |= result.level() == SupportLevel.VISUAL_ONLY;
            anyApproximated |= result.level() == SupportLevel.APPROXIMATED;
            anyAdapted |= result.level() == SupportLevel.ADAPTED;
            allNative &= result.level() == SupportLevel.NATIVE;
        }

        if (anyUnsupported) {
            return SupportLevel.UNSUPPORTED;
        }
        if (anyVisualOnly) {
            return SupportLevel.VISUAL_ONLY;
        }
        if (anyApproximated) {
            return SupportLevel.APPROXIMATED;
        }
        if (anyAdapted) {
            return SupportLevel.ADAPTED;
        }
        if (allNative) {
            return SupportLevel.NATIVE;
        }
        return SupportLevel.AUTOMATIC;
    }

    @NotNull
    static CompatibilityStatus overallStatus(@NotNull Map<String, SupportResult> supportResults) {
        boolean hasComplete = false;
        boolean hasPartial = false;
        boolean hasNone = false;
        for (SupportResult result : supportResults.values()) {
            hasComplete |= result.status() == CompatibilityStatus.COMPLETE;
            hasPartial |= result.status() == CompatibilityStatus.PARTIAL;
            hasNone |= result.status() == CompatibilityStatus.NONE;
        }
        if (hasPartial || hasNone) {
            return CompatibilityStatus.PARTIAL;
        }
        if (hasComplete) {
            return CompatibilityStatus.COMPLETE;
        }
        return CompatibilityStatus.UNKNOWN;
    }

    static int overallScore(@NotNull Map<String, SupportResult> supportResults) {
        int scored = 0;
        int total = 0;
        for (SupportResult result : supportResults.values()) {
            if (result.scorePercent() != null) {
                scored += result.scorePercent();
                total++;
            }
        }
        return total == 0 ? 0 : (int) Math.round(scored / (double) total);
    }
}