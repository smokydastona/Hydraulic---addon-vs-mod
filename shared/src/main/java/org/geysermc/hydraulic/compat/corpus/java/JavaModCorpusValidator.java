package org.geysermc.hydraulic.compat.corpus.java;

import org.geysermc.hydraulic.compat.corpus.CorpusCapabilityCoverage;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Performs semantic validation on Java-mod-corpus records after JSON deserialization.
 */
public final class JavaModCorpusValidator {
    private JavaModCorpusValidator() {
    }

    @NotNull
    public static List<String> validate(@NotNull JavaModCorpusEntry entry) {
        List<String> errors = new ArrayList<>();
        if (entry.identity() == null || entry.source() == null || entry.license() == null
            || entry.admissibility() == null || entry.semanticFacts() == null || entry.confidence() == null) {
            return List.of("required corpus sections must not be null");
        }

        requireNonBlank(errors, "identity.corpusId", entry.identity().corpusId());
        requireNonBlank(errors, "identity.displayName", entry.identity().displayName());

        if (entry.source().sourceType() != JavaModCorpusEntry.SourceType.UNKNOWN) {
            requireUrl(errors, "source.sourceUrl", entry.source().sourceUrl());
        } else {
            requireNonBlank(errors, "source.sourceUrl", entry.source().sourceUrl());
        }
        if (entry.source().repositoryUrl() != null && !entry.source().repositoryUrl().isBlank()) {
            requireUrl(errors, "source.repositoryUrl", entry.source().repositoryUrl());
        }

        requireNonBlank(errors, "capability", entry.capability());
        if (!entry.capability().isBlank() && !CorpusCapabilityCoverage.CANONICAL_CAPABILITIES.contains(entry.capability())) {
            errors.add("capability must be one of the canonical capability vocabulary values");
        }

        if (entry.confidence().overallScore() < 0.0D || entry.confidence().overallScore() > 1.0D) {
            errors.add("confidence.overallScore must be between 0 and 1");
        }

        if (entry.admissibility().isAdmissible()
            && (!entry.license().allowsRedistribution() || !entry.license().allowsModification())) {
            errors.add("admissible entries must permit redistribution and modification");
        }

        requireNonBlank(errors, "implementationPattern", entry.implementationPattern());
        requireNonBlank(errors, "bedrockFeasibilityHint", entry.bedrockFeasibilityHint());
        return List.copyOf(errors);
    }

    private static void requireUrl(@NotNull List<String> errors, @NotNull String field, @NotNull String value) {
        try {
            URI uri = URI.create(value);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) {
                errors.add(field + " must be an HTTPS URL");
            }
        } catch (IllegalArgumentException exception) {
            errors.add(field + " must be a valid HTTPS URL");
        }
    }

    private static void requireNonBlank(@NotNull List<String> errors, @NotNull String field, @NotNull String value) {
        if (value.isBlank()) {
            errors.add(field + " must not be blank");
        }
    }
}
