package org.geysermc.hydraulic.compat;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Locale;

public enum MappingOwnership {
    BUILTIN("builtin", 0),
    MOD("mods", 100),
    SERVER("server", 500),
    USER("user", 1000),
    LEGACY("", 500);

    private final String folderName;
    private final int priority;

    MappingOwnership(@NotNull String folderName, int priority) {
        this.folderName = folderName;
        this.priority = priority;
    }

    @NotNull
    public String folderName() {
        return this.folderName;
    }

    public int priority() {
        return this.priority;
    }

    @NotNull
    public static MappingOwnership fromRelativePath(@NotNull Path relativePath) {
        if (relativePath.getNameCount() == 0) {
            return LEGACY;
        }

        String firstSegment = relativePath.getName(0).toString().toLowerCase(Locale.ROOT);
        for (MappingOwnership ownership : values()) {
            if (!ownership.folderName.isEmpty() && ownership.folderName.equals(firstSegment)) {
                return ownership;
            }
        }
        return LEGACY;
    }
}