package org.geysermc.hydraulic.compat.corpus;

import org.jetbrains.annotations.NotNull;

/**
 * Classifies how strongly a corpus entry's evidence can be trusted as an implementation pattern
 * versus documentation-only or teaching-only material. Confidence weighting derived from this
 * tier keeps the corpus from treating a canonical reference (no capability implementation
 * evidence) the same as a real, licensed implementation.
 */
public enum CorpusEvidenceTier {
    /** Admissible source with concrete capability/implementation evidence (scripts, components, or declared capabilities). */
    LICENSED_IMPLEMENTATION,
    /** Admissible source with no capability/implementation evidence (for example, a canonical vanilla reference). */
    REVIEWED_DOCUMENTATION,
    /** Inadmissible source that still has inspectable evidence, recorded for research/backlog purposes only. */
    DERIVED_TEACHING_MATERIAL,
    /** Inadmissible source with no meaningful evidence at all. */
    REJECTED;

    @NotNull
    public static CorpusEvidenceTier classify(@NotNull AddonCorpusEntry entry) {
        boolean hasImplementationEvidence = !entry.evidence().scriptEvidence().isEmpty()
            || !entry.evidence().componentEvidence().isEmpty()
            || !entry.capabilities().storageTypes().isEmpty()
            || !entry.capabilities().machineTypes().isEmpty()
            || !entry.capabilities().transferTypes().isEmpty()
            || !entry.capabilities().fluidTypes().isEmpty()
            || !entry.capabilities().energyTypes().isEmpty()
            || !entry.capabilities().automationTypes().isEmpty()
            || !entry.capabilities().networkingTypes().isEmpty();

        if (!entry.admissibility().isAdmissible()) {
            boolean hasAnyEvidence = hasImplementationEvidence
                || !entry.evidence().manifestEvidence().isEmpty()
                || !entry.evidence().functionEvidence().isEmpty()
                || !entry.evidence().uiEvidence().isEmpty();
            return hasAnyEvidence ? DERIVED_TEACHING_MATERIAL : REJECTED;
        }

        return hasImplementationEvidence ? LICENSED_IMPLEMENTATION : REVIEWED_DOCUMENTATION;
    }

    /**
     * Multiplier applied to matcher confidence so documentation-only and teaching-only evidence
     * never outweighs a real, licensed implementation for the same capability.
     */
    public double confidenceWeight() {
        return switch (this) {
            case LICENSED_IMPLEMENTATION -> 1.0D;
            case REVIEWED_DOCUMENTATION -> 0.6D;
            case DERIVED_TEACHING_MATERIAL -> 0.3D;
            case REJECTED -> 0.0D;
        };
    }
}
