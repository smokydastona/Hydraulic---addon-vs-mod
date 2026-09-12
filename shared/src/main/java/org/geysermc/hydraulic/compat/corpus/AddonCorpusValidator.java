package org.geysermc.hydraulic.compat.corpus;

import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Performs semantic validation on corpus records after JSON deserialization.
 */
public final class AddonCorpusValidator {
    private AddonCorpusValidator() {
    }

    @NotNull
    public static List<String> validate(@NotNull AddonCorpusEntry entry) {
        List<String> errors = new ArrayList<>();
        if (entry.identity() == null || entry.source() == null || entry.license() == null
            || entry.admissibility() == null || entry.confidence() == null || entry.implementationFacts() == null) {
            return List.of("required corpus sections must not be null");
        }

        if (entry.source().sourceType() != AddonCorpusEntry.SourceType.GITHUB && entry.source().sourceType() != AddonCorpusEntry.SourceType.LOCAL_FILE) {
            errors.add("source.sourceType must be GITHUB or LOCAL_FILE; marketplace and direct-download sources are rejected");
        }

        if (entry.source().sourceType() == AddonCorpusEntry.SourceType.GITHUB) {
            String sourceUrl = entry.source().sourceUrl();
            if (sourceUrl == null || sourceUrl.isBlank() || !sourceUrl.contains("github.com")) {
                errors.add("source.sourceUrl must be a GitHub HTTPS URL");
            }

            String repositoryUrl = entry.source().repositoryUrl();
            if (repositoryUrl == null || repositoryUrl.isBlank() || !repositoryUrl.contains("github.com")) {
                errors.add("source.repositoryUrl must be a GitHub HTTPS URL for admissible corpus entries");
            }
        }

        requireNonBlank(errors, "identity.corpusId", entry.identity().corpusId());
        requireIdentifier(errors, "identity.bedrockIdentifier", entry.identity().bedrockIdentifier());
        if (entry.source().sourceType() == AddonCorpusEntry.SourceType.LOCAL_FILE) {
            requireNonBlank(errors, "source.sourceUrl", entry.source().sourceUrl());
        } else {
            requireUrl(errors, "source.sourceUrl", entry.source().sourceUrl());
        }
        if (entry.source().repositoryUrl() != null && !entry.source().repositoryUrl().isBlank()) {
            requireUrl(errors, "source.repositoryUrl", entry.source().repositoryUrl());
        }
        if (entry.confidence().overallScore() < 0.0D || entry.confidence().overallScore() > 1.0D) {
            errors.add("confidence.overallScore must be between 0 and 1");
        }
        if (entry.admissibility().isAdmissible()
            && (!entry.license().allowsRedistribution() || !entry.license().allowsModification())) {
            errors.add("admissible entries must permit redistribution and modification");
        }
        requireNonBlank(errors, "implementationFacts.implementationPattern", entry.implementationFacts().implementationPattern());
        requireNonBlank(errors, "implementationFacts.reusability", entry.implementationFacts().reusability());
        requireNonBlank(errors, "implementationFacts.adapterCandidate", entry.implementationFacts().adapterCandidate());
        return List.copyOf(errors);
    }

    private static void requireIdentifier(@NotNull List<String> errors, @NotNull String field, @NotNull String value) {
        requireNonBlank(errors, field, value);
        if (!value.contains(":")) {
            errors.add(field + " must be a namespaced identifier");
        }
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