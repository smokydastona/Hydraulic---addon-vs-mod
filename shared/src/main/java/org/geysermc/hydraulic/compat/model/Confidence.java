package org.geysermc.hydraulic.compat.model;

import org.jetbrains.annotations.NotNull;

public record Confidence(double score, @NotNull String basis) {
    public Confidence {
        score = Math.max(0D, Math.min(1D, score));
        basis = basis == null ? "" : basis;
    }

    public int percent() {
        return (int) Math.round(this.score * 100D);
    }
}