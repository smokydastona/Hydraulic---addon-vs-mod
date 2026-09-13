package org.geysermc.hydraulic.companion;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;

/**
 * A discovered companion package directory under {@code config/hydraulic/companions}.
 */
public final class CompanionPackage {
    private final String discoveredId;
    private final Path root;
    private final CompanionManifest manifest;
    private final Path resourcePackPath;
    private final Path behaviorPackPath;
    private final List<CompanionValidationIssue> issues;

    public CompanionPackage(
            @NotNull String discoveredId,
            @NotNull Path root,
            @Nullable CompanionManifest manifest,
            @Nullable Path resourcePackPath,
            @Nullable Path behaviorPackPath,
            @NotNull List<CompanionValidationIssue> issues
    ) {
        this.discoveredId = discoveredId;
        this.root = root;
        this.manifest = manifest;
        this.resourcePackPath = resourcePackPath;
        this.behaviorPackPath = behaviorPackPath;
        this.issues = issues;
    }

    @NotNull
    public String id() {
        return manifest != null && manifest.id() != null ? manifest.id() : discoveredId;
    }

    @NotNull
    public Path root() {
        return root;
    }

    @Nullable
    public CompanionManifest manifest() {
        return manifest;
    }

    @Nullable
    public Path resourcePackPath() {
        return resourcePackPath;
    }

    @Nullable
    public Path behaviorPackPath() {
        return behaviorPackPath;
    }

    @NotNull
    public List<CompanionValidationIssue> issues() {
        return issues;
    }

    public boolean isValid() {
        return manifest != null
                && manifest.id() != null
                && !manifest.id().isBlank()
                && resourcePackPath != null
                && issues.stream().noneMatch(issue -> issue.level() == CompanionValidationIssue.Level.ERROR);
    }
}
