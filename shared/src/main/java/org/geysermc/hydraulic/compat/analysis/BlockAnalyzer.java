package org.geysermc.hydraulic.compat.analysis;

import net.minecraft.resources.Identifier;
import org.geysermc.hydraulic.compat.ContentInventory;
import org.geysermc.hydraulic.compat.capability.Capability;
import org.geysermc.hydraulic.compat.capability.CapabilityDomain;
import org.geysermc.hydraulic.compat.capability.CapabilityProfile;
import org.geysermc.hydraulic.compat.capability.CapabilityRequirement;
import org.geysermc.hydraulic.compat.capability.CapabilityResult;
import org.geysermc.hydraulic.compat.mapping.ContentPatch;
import org.geysermc.hydraulic.compat.model.CompatibilityFinding;
import org.geysermc.hydraulic.compat.model.CompatibilityObject;
import org.geysermc.hydraulic.compat.model.Confidence;
import org.geysermc.hydraulic.compat.model.SupportLevel;
import org.geysermc.hydraulic.compat.model.SupportResult;
import org.geysermc.hydraulic.metadata.BlockMapping;
import org.geysermc.hydraulic.metadata.MetadataIndex;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public final class BlockAnalyzer implements CompatibilityAnalyzer {
    @Override
    public @NotNull String kind() {
        return "block";
    }

    @Override
    public @NotNull CompatibilityObject analyze(@NotNull ContentInventory.ContentDescriptor descriptor, @NotNull ContentInventory.ModContentInventory inventory, @NotNull MetadataIndex metadataIndex) {
        Identifier identifier = Identifier.parse(descriptor.javaIdentifier());
        BlockMapping mapping = metadataIndex.blockMapping(identifier);
        List<ContentPatch> patches = metadataIndex.contentPatches(identifier);
        boolean behaviorRequired = mapping != null && mapping.rules().stream().anyMatch(rule -> rule.behaviorRequired()) || patches.stream().anyMatch(patch -> patch.hasOperationPrefix("behavior.") || patch.hasOperationPrefix("interaction."));
        String behaviorTag = mapping != null ? mapping.rules().stream().map(rule -> rule.behaviorTag()).filter(tag -> tag != null && !tag.isBlank()).findFirst().orElse(null) : null;
        if (behaviorTag == null) {
            behaviorTag = patches.stream().map(patch -> patch.operation("behavior.tag")).filter(tag -> tag != null && !tag.isBlank()).findFirst().orElse(null);
        }
        boolean visualPatch = patches.stream().anyMatch(patch -> patch.hasOperationPrefix("visual.") || patch.hasOperationPrefix("bedrock."));

        Capability registered = AnalyzerSupport.capability(CapabilityDomain.CONTENT, "registered", "Block exists in the Java registry.");
        Capability blockAsset = AnalyzerSupport.capability(CapabilityDomain.PRESENTATION, "block_asset", "Block has discoverable blockstate assets for conversion.");
        Capability stateTranslation = AnalyzerSupport.capability(CapabilityDomain.STATE_DATA, "state_translation", "Block has explicit state mapping or patch data.");
        Capability placement = AnalyzerSupport.capability(CapabilityDomain.INTERACTION, "placement", "Block can be placed by Bedrock clients.");
        Capability breaking = AnalyzerSupport.capability(CapabilityDomain.INTERACTION, "breaking", "Block can be broken by Bedrock clients.");
        Capability contextualUse = AnalyzerSupport.capability(CapabilityDomain.INTERACTION, "contextual_use", "Block use interaction can be represented without a runtime bridge.");
        Capability runtimeBehavior = AnalyzerSupport.capability(CapabilityDomain.BEHAVIOR, "runtime_behavior", "Special block behavior does not require a dedicated Bedrock bridge.");

        List<CapabilityRequirement> requirements = List.of(
            AnalyzerSupport.required(registered),
            AnalyzerSupport.required(blockAsset),
            AnalyzerSupport.required(stateTranslation),
            AnalyzerSupport.required(placement),
            AnalyzerSupport.required(breaking),
            AnalyzerSupport.required(contextualUse),
            AnalyzerSupport.required(runtimeBehavior)
        );

        List<CapabilityResult> results = List.of(
            AnalyzerSupport.result(registered, descriptor.registered(), "Registry lookup from BuiltInRegistries.BLOCK."),
            AnalyzerSupport.result(blockAsset, descriptor.assetPresent(), "PackManager found a matching blockstate asset for this block."),
            AnalyzerSupport.result(stateTranslation, mapping != null || !patches.isEmpty(), "Block metadata mappings and patches drive explicit state handling."),
            AnalyzerSupport.result(placement, true, "Current custom block path can place converted blocks."),
            AnalyzerSupport.result(breaking, true, "Current custom block path can break converted blocks."),
            AnalyzerSupport.result(contextualUse, !behaviorRequired, "Complex interaction still depends on future runtime bridges."),
            AnalyzerSupport.result(runtimeBehavior, !behaviorRequired, "Behavior-required blocks still need behavior generation or bridges.")
        );

        CapabilityProfile profile = new CapabilityProfile(descriptor.javaIdentifier(), requirements, results);
        Map<String, SupportResult> supportResults = new LinkedHashMap<>();
        supportResults.put("content", AnalyzerSupport.support("content", SupportLevel.AUTOMATIC, List.of(results.get(0)), List.of("Registry discovery is backed by the current pack manager initialization flow.")));
        supportResults.put("presentation", AnalyzerSupport.support("presentation", visualPatch ? SupportLevel.ADAPTED : SupportLevel.AUTOMATIC, List.of(results.get(1)), List.of("Presentation coverage currently relies on discovered blockstate assets and optional metadata overrides.")));
        supportResults.put("state_data", AnalyzerSupport.support("state_data", mapping != null || !patches.isEmpty() ? SupportLevel.ADAPTED : SupportLevel.APPROXIMATED, List.of(results.get(2)), List.of("Generic state translation is not implemented yet; this score reflects explicit metadata and patch evidence only.")));
        supportResults.put("interaction", AnalyzerSupport.support("interaction", behaviorRequired ? SupportLevel.APPROXIMATED : SupportLevel.AUTOMATIC, List.of(results.get(3), results.get(4), results.get(5)), List.of("Placement and breaking are established, but contextual interaction remains conservative until bridges land.")));
        supportResults.put("behavior", AnalyzerSupport.support("behavior", behaviorRequired ? SupportLevel.UNSUPPORTED : SupportLevel.AUTOMATIC, List.of(results.get(6)), List.of("Behavior pack generation and runtime bridges are not yet implemented.")));

        List<CompatibilityFinding> findings = new ArrayList<>();
        if (!descriptor.assetPresent()) {
            findings.add(new CompatibilityFinding("block.asset.missing", CompatibilityFinding.Severity.WARNING, "presentation", "Block asset discovery failed for " + descriptor.javaIdentifier(), "The block does not currently have a discovered blockstate asset in this mod root.", "Add a blockstate asset or metadata patch for this block.", null));
        }
        if (behaviorRequired) {
            findings.add(new CompatibilityFinding("block.behavior.required", CompatibilityFinding.Severity.WARNING, "behavior", "Block declares behavior requirements that Hydraulic cannot satisfy yet.", behaviorTag != null ? "Metadata or patch data flagged behavior tag '" + behaviorTag + "'." : "Metadata or patch data flagged behavior-dependent handling.", "Implement a behavior bridge or adapter for this block family.", null));
        }

        List<String> metadataSources = new ArrayList<>();
        if (mapping != null) {
            mapping.rules().stream().map(rule -> rule.sourcePath()).distinct().forEach(metadataSources::add);
        }

        Map<String, String> inventoryFacts = AnalyzerSupport.inventoryFacts(descriptor.registered(), descriptor.assetPresent(), mapping != null ? 1 : 0, patches.size());
        inventoryFacts.put("behavior_required", Boolean.toString(behaviorRequired));
        if (behaviorTag != null) {
            inventoryFacts.put("behavior_tag", behaviorTag);
        }

        return AnalyzerSupport.object(
            descriptor.javaIdentifier(),
            descriptor.kind(),
            descriptor.modId(),
            inventoryFacts,
            profile,
            supportResults,
            new Confidence(mapping != null || !patches.isEmpty() ? (descriptor.assetPresent() ? 0.92D : 0.68D) : (descriptor.assetPresent() ? 0.78D : 0.35D), "Inventory-backed block analyzer with metadata and patch signals."),
            AnalyzerSupport.provenance(this.getClass().getSimpleName(), mapping != null || !patches.isEmpty(), patches, metadataSources),
            findings
        );
    }
}