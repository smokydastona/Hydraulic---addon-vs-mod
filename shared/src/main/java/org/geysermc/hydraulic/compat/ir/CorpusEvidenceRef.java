package org.geysermc.hydraulic.compat.ir;

import org.geysermc.hydraulic.compat.corpus.CorpusEvidenceTier;
import org.geysermc.hydraulic.compat.runtime.RuntimeBridgeKind;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * A compiled, typed reference to advisory Bedrock addon corpus evidence attached to a
 * {@link CompiledCompatibilityPlan}.
 *
 * This carries only stable, already-ranked facts (capability, the corpus entry id that matched,
 * its evidence tier, and its ranked score) computed once at compatibility-compile time. It never
 * exposes the raw {@code AddonCorpusEntry} to runtime dispatch or bridge factories, matching the
 * corpus rule that raw corpus data must stay out of hot runtime paths and remain advisory.
 */
public record CorpusEvidenceRef(
    @NotNull String capability,
    @Nullable RuntimeBridgeKind relatedBridgeKind,
    @NotNull String corpusId,
    @NotNull CorpusEvidenceTier tier,
    double score
) {
    /**
     * Maps a mod-level corpus capability label (as produced by
     * {@code CompatibilityManager.corpusEvidence(...)}) to the runtime bridge kind it is
     * evidence for, when one exists. Capabilities without a mapping (for example, presentation
     * or reference-only categories) resolve to {@code null} and are not attached to any specific
     * compiled plan's bridge scope.
     */
    private static final Map<String, RuntimeBridgeKind> CAPABILITY_BRIDGE_KINDS = Map.of(
        "item", RuntimeBridgeKind.ITEM_TRANSFER,
        "fluid", RuntimeBridgeKind.FLUID_TRANSFER,
        "storage", RuntimeBridgeKind.MACHINE_INVENTORY,
        "machine", RuntimeBridgeKind.MACHINE_BEHAVIOR,
        "automation", RuntimeBridgeKind.AUTOMATION_ACCESS
    );

    @NotNull
    public static CorpusEvidenceRef of(
        @NotNull String capability,
        @NotNull String corpusId,
        @NotNull CorpusEvidenceTier tier,
        double score
    ) {
        return new CorpusEvidenceRef(capability, CAPABILITY_BRIDGE_KINDS.get(capability), corpusId, tier, score);
    }
}
