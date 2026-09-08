package org.geysermc.hydraulic.compat;

import org.geysermc.hydraulic.compat.model.CompatibilityFinding;
import org.geysermc.hydraulic.compat.model.CompatibilityObject;
import org.geysermc.hydraulic.compat.model.ModFingerprint;
import org.geysermc.hydraulic.compat.model.SupportLevel;
import org.geysermc.hydraulic.compat.model.SupportResult;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record CompatibilityProfile(
    @NotNull String modId,
    @NotNull ModFingerprint fingerprint,
    @NotNull SupportLevel overallLevel,
    @NotNull CompatibilityStatus overallStatus,
    int overallScore,
    @NotNull Map<String, SupportResult> supportResults,
    @NotNull Map<String, Integer> levelCounts,
    @NotNull List<CompatibilityObject> objects,
    @NotNull List<CompatibilityFinding> findings,
    @NotNull List<String> notes
) {
    public CompatibilityProfile {
        supportResults = Collections.unmodifiableMap(new LinkedHashMap<>(supportResults));
        levelCounts = Collections.unmodifiableMap(new LinkedHashMap<>(levelCounts));
        objects = List.copyOf(objects);
        findings = List.copyOf(findings);
        notes = List.copyOf(notes);
    }
}