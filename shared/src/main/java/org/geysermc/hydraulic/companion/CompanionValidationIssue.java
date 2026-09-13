package org.geysermc.hydraulic.companion;

public record CompanionValidationIssue(Level level, String message) {
    public enum Level {
        ERROR,
        WARNING
    }

    public static CompanionValidationIssue error(String message) {
        return new CompanionValidationIssue(Level.ERROR, message);
    }

    public static CompanionValidationIssue warning(String message) {
        return new CompanionValidationIssue(Level.WARNING, message);
    }
}
