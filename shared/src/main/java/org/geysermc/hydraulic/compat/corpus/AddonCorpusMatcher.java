package org.geysermc.hydraulic.compat.corpus;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Ranks already-loaded corpus evidence for a requested compatibility capability.
 * This matcher is deterministic and never performs network or filesystem access.
 */
public final class AddonCorpusMatcher {
    private AddonCorpusMatcher() {
    }

    @NotNull
    public static List<Match> rank(
        @NotNull List<AddonCorpusEntry> entries,
        @NotNull String capability,
        @NotNull Set<String> requiredPatterns
    ) {
        String normalizedCapability = normalize(capability);
        Set<String> normalizedPatterns = normalizeAll(requiredPatterns);
        List<Match> matches = new ArrayList<>();
        for (AddonCorpusEntry entry : entries) {
            if (!entry.admissibility().isAdmissible()) {
                continue;
            }
            int capabilityMatches = capabilityMatches(entry, normalizedCapability);
            int patternMatches = patternMatches(entry, normalizedPatterns);
            if (capabilityMatches == 0 && patternMatches == 0) {
                continue;
            }
            CorpusEvidenceTier tier = CorpusEvidenceTier.classify(entry);
            double score = ((capabilityMatches > 0 ? 0.55D : 0.0D)
                + (patternMatches > 0 ? 0.20D : 0.0D)
                + (entry.confidence().overallScore() * 0.20D)
                + reusabilityScore(entry.implementationFacts().reusability()) * 0.05D)
                * tier.confidenceWeight();
            matches.add(new Match(entry, score, capabilityMatches, patternMatches, tier));
        }
        matches.sort(Comparator
            .comparingDouble(Match::score).reversed()
            .thenComparing(match -> match.entry().identity().corpusId()));
        return List.copyOf(matches);
    }

    private static int capabilityMatches(@NotNull AddonCorpusEntry entry, @NotNull String capability) {
        Set<String> values = new HashSet<>();
        values.addAll(normalizeAll(entry.capabilities().storageTypes()));
        values.addAll(normalizeAll(entry.capabilities().machineTypes()));
        values.addAll(normalizeAll(entry.capabilities().transferTypes()));
        values.addAll(normalizeAll(entry.capabilities().fluidTypes()));
        values.addAll(normalizeAll(entry.capabilities().energyTypes()));
        values.addAll(normalizeAll(entry.capabilities().automationTypes()));
        values.addAll(normalizeAll(entry.capabilities().networkingTypes()));
        return values.contains(capability) ? 1 : 0;
    }

    private static int patternMatches(@NotNull AddonCorpusEntry entry, @NotNull Set<String> patterns) {
        if (patterns.isEmpty()) {
            return 0;
        }
        Set<String> values = normalizeAll(List.of(
            entry.implementationFacts().implementationPattern(),
            entry.implementationFacts().adapterCandidate()
        ));
        values.addAll(normalizeAll(entry.implementationFacts().runtimeHooks()));
        return (int) patterns.stream().filter(values::contains).count();
    }

    private static double reusabilityScore(@NotNull String reusability) {
        return switch (normalize(reusability)) {
            case "high", "highly reusable", "generic" -> 1.0D;
            case "medium", "moderate" -> 0.6D;
            case "low", "mod specific" -> 0.2D;
            default -> 0.0D;
        };
    }

    @NotNull
    private static Set<String> normalizeAll(@NotNull Iterable<String> values) {
        Set<String> normalized = new HashSet<>();
        for (String value : values) {
            normalized.add(normalize(value));
        }
        return normalized;
    }

    @NotNull
    private static String normalize(@NotNull String value) {
        return value.trim().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    public record Match(
        @NotNull AddonCorpusEntry entry,
        double score,
        int capabilityMatches,
        int patternMatches,
        @NotNull CorpusEvidenceTier tier
    ) {
    }
}