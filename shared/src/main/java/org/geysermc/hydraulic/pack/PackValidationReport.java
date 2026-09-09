package org.geysermc.hydraulic.pack;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record PackValidationReport(
    @NotNull Map<String, ModValidation> perMod
) {
    public PackValidationReport {
        perMod = Map.copyOf(new LinkedHashMap<>(perMod));
    }

    @NotNull
    public static PackValidationReport empty() {
        return new PackValidationReport(Map.of());
    }

    @NotNull
    public PackValidationReport withValidation(@NotNull String modId, @NotNull ModValidation validation) {
        Map<String, ModValidation> updated = new LinkedHashMap<>(this.perMod);
        updated.put(modId, validation);
        return new PackValidationReport(updated);
    }

    public record ModValidation(
        @NotNull String packPath,
        boolean created,
        boolean valid,
        long durationMillis,
        @NotNull List<ValidationMessage> errors,
        @NotNull List<ValidationMessage> warnings,
        @NotNull List<String> manualActions
    ) {
        public ModValidation {
            errors = List.copyOf(errors);
            warnings = List.copyOf(warnings);
            manualActions = List.copyOf(manualActions);
        }

        public int errorCount() {
            return this.errors.size();
        }

        public int warningCount() {
            return this.warnings.size();
        }

        public int manualActionCount() {
            return this.manualActions.size();
        }
    }

    public record ValidationMessage(
        @NotNull String code,
        @NotNull String message,
        @Nullable String entry
    ) {
    }
}