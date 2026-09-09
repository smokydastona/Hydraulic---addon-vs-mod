package org.geysermc.hydraulic.compat.analysis;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AnalyzerRegistry {
    private final Map<String, CompatibilityAnalyzer> analyzersByKind;

    private AnalyzerRegistry(@NotNull Map<String, CompatibilityAnalyzer> analyzersByKind) {
        this.analyzersByKind = Map.copyOf(analyzersByKind);
    }

    @NotNull
    public static AnalyzerRegistry create(@NotNull List<CompatibilityAnalyzer> analyzers) {
        Map<String, CompatibilityAnalyzer> analyzersByKind = new LinkedHashMap<>();
        for (CompatibilityAnalyzer analyzer : analyzers) {
            CompatibilityAnalyzer previous = analyzersByKind.putIfAbsent(analyzer.kind(), analyzer);
            if (previous != null) {
                throw new IllegalArgumentException("Duplicate analyzer kind: " + analyzer.kind());
            }
        }
        return new AnalyzerRegistry(analyzersByKind);
    }

    @Nullable
    public CompatibilityAnalyzer analyzer(@NotNull String kind) {
        return this.analyzersByKind.get(kind);
    }
}