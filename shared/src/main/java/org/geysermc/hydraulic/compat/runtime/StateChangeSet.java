package org.geysermc.hydraulic.compat.runtime;

import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record StateChangeSet(@NotNull List<FieldChange> changes) {
    public StateChangeSet {
        changes = List.copyOf(changes);
    }

    public static StateChangeSet empty() {
        return new StateChangeSet(List.of());
    }

    public record FieldChange(
        @NotNull Identifier blockIdentifier,
        @NotNull String field,
        @Nullable Object before,
        @Nullable Object after
    ) {
    }

    @NotNull
    public StateChangeSet merge(@NotNull StateChangeSet other) {
        Map<String, FieldChange> merged = new LinkedHashMap<>();
        for (FieldChange change : this.changes) {
            merged.put(key(change), change);
        }
        for (FieldChange change : other.changes) {
            FieldChange previous = merged.get(key(change));
            merged.put(key(change), previous == null
                ? change
                : new FieldChange(change.blockIdentifier(), change.field(), previous.before(), change.after()));
        }
        return new StateChangeSet(List.copyOf(merged.values()));
    }

    private static String key(FieldChange change) {
        return change.blockIdentifier() + "|" + change.field();
    }
}
