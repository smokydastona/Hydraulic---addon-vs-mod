package org.geysermc.hydraulic.compat.runtime;

import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record StateChangeSet(@NotNull List<FieldChange> changes, @Nullable RuntimeTraceId traceId) {
    public StateChangeSet(@NotNull List<FieldChange> changes) {
        this(changes, null);
    }

    public StateChangeSet {
        changes = List.copyOf(changes);
    }

    public static StateChangeSet empty() {
        return new StateChangeSet(List.of());
    }

    @NotNull
    public StateChangeSet withTrace(@Nullable RuntimeTraceId traceId) {
        if (traceId == null) {
            return this;
        }
        return new StateChangeSet(this.changes.stream()
            .map(change -> change.withTrace(traceId))
            .toList(), traceId);
    }

    public record FieldChange(
        @NotNull Identifier blockIdentifier,
        @NotNull String field,
        @Nullable Object before,
        @Nullable Object after,
        @Nullable RuntimeTraceId traceId
    ) {
        public FieldChange(
            @NotNull Identifier blockIdentifier,
            @NotNull String field,
            @Nullable Object before,
            @Nullable Object after
        ) {
            this(blockIdentifier, field, before, after, null);
        }

        @NotNull
        FieldChange withTrace(@NotNull RuntimeTraceId traceId) {
            return new FieldChange(this.blockIdentifier, this.field, this.before, this.after, traceId);
        }
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
                : new FieldChange(change.blockIdentifier(), change.field(), previous.before(), change.after(), combineTrace(previous.traceId(), change.traceId())));
        }
        return new StateChangeSet(List.copyOf(merged.values()), combineTrace(this.traceId, other.traceId()));
    }

    @Nullable
    private static RuntimeTraceId combineTrace(@Nullable RuntimeTraceId first, @Nullable RuntimeTraceId second) {
        if (first == null) {
            return second;
        }
        if (second == null || first.equals(second)) {
            return first;
        }
        return null;
    }

    private static String key(FieldChange change) {
        return change.blockIdentifier() + "|" + change.field();
    }
}
