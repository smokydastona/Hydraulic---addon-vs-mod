package org.geysermc.hydraulic.companion;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Holds the resolved state of every discovered companion package: which ones are
 * valid and registered, which were rejected, and their built resource pack artifacts.
 */
public final class CompanionRegistry {
    public record Entry(CompanionPackage companionPackage, CompanionBuildResult buildResult, List<CompanionCapabilityResult> capabilityResults) {
    }

    private final Map<String, Entry> entries = new LinkedHashMap<>();
    private final List<CompanionPackage> rejected = new ArrayList<>();

    void register(@NotNull Entry entry) {
        this.entries.put(entry.companionPackage().id(), entry);
    }

    void reject(@NotNull CompanionPackage rejectedPackage) {
        this.rejected.add(rejectedPackage);
    }

    @NotNull
    public Map<String, Entry> entries() {
        return this.entries;
    }

    @NotNull
    public List<CompanionPackage> rejected() {
        return this.rejected;
    }

    @NotNull
    public Map<String, Path> registerableResourcePacks() {
        Map<String, Path> result = new LinkedHashMap<>();
        for (Entry entry : this.entries.values()) {
            result.put(entry.companionPackage().id(), entry.buildResult().mcpackPath());
        }
        return result;
    }

    @Nullable
    public Entry get(@NotNull String id) {
        return this.entries.get(id);
    }
}
