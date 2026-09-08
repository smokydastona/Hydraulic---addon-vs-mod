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
import org.geysermc.hydraulic.metadata.IdentifierMapping;
import org.geysermc.hydraulic.metadata.MetadataIndex;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EntityAnalyzer implements CompatibilityAnalyzer {
    @Override
    public boolean supports(@NotNull ContentInventory.ContentDescriptor descriptor) {
        return descriptor.kind().equals("entity");
    }

    @Override
    public @NotNull CompatibilityObject analyze(@NotNull ContentInventory.ContentDescriptor descriptor, @NotNull ContentInventory.ModContentInventory inventory, @NotNull MetadataIndex metadataIndex) {
        IdentifierMapping mapping = metadataIndex.entityMapping(Identifier.parse(descriptor.javaIdentifier()));
        List<ContentPatch> patches = metadataIndex.contentPatches(Identifier.parse(descriptor.javaIdentifier()));

        Capability registered = AnalyzerSupport.capability(CapabilityDomain.CONTENT, "registered", "Entity exists in the Java registry.");
        Capability presentation = AnalyzerSupport.capability(CapabilityDomain.PRESENTATION, "presentation_mapping", "Entity has explicit presentation mapping data.");
        Capability interaction = AnalyzerSupport.capability(CapabilityDomain.INTERACTION, "entity_interaction", "Entity interaction has a compatible Bedrock bridge.");
        Capability behavior = AnalyzerSupport.capability(CapabilityDomain.BEHAVIOR, "runtime_behavior", "Entity runtime behavior can be represented on Bedrock.");

        List<CapabilityRequirement> requirements = List.of(
            AnalyzerSupport.required(registered),
            AnalyzerSupport.required(presentation),
            AnalyzerSupport.required(interaction),
            AnalyzerSupport.required(behavior)
        );
        List<CapabilityResult> results = List.of(
            AnalyzerSupport.result(registered, descriptor.registered(), "Registry lookup from BuiltInRegistries.ENTITY_TYPE."),
            AnalyzerSupport.result(presentation, mapping != null || !patches.isEmpty(), "Entity support currently depends on metadata or patch evidence only."),
            AnalyzerSupport.result(interaction, false, "Runtime entity bridges are not implemented yet."),
            AnalyzerSupport.result(behavior, false, "Behavior representation for entities is not implemented yet.")
        );

        CapabilityProfile profile = new CapabilityProfile(descriptor.javaIdentifier(), requirements, results);
        Map<String, SupportResult> supportResults = new LinkedHashMap<>();
        supportResults.put("content", AnalyzerSupport.support("content", SupportLevel.AUTOMATIC, List.of(results.get(0)), List.of("Entity discovery is registry-backed.")));
        supportResults.put("presentation", AnalyzerSupport.support("presentation", mapping != null || !patches.isEmpty() ? SupportLevel.ADAPTED : SupportLevel.UNSUPPORTED, List.of(results.get(1)), List.of("Entity presentation currently relies on explicit metadata rather than automatic translators.")));
        supportResults.put("interaction", AnalyzerSupport.support("interaction", SupportLevel.UNSUPPORTED, List.of(results.get(2)), List.of("Entity interaction bridges are still missing.")));
        supportResults.put("behavior", AnalyzerSupport.support("behavior", SupportLevel.UNSUPPORTED, List.of(results.get(3)), List.of("Entity runtime translation is not implemented.")));

        List<CompatibilityFinding> findings = List.of(
            new CompatibilityFinding("entity.bridge.missing", CompatibilityFinding.Severity.WARNING, "behavior", "Entity runtime support is not implemented for " + descriptor.javaIdentifier(), "Current entity coverage is metadata-declared rather than behavior-backed.", "Implement entity bridges before treating entity support as functional.", null)
        );
        List<String> metadataSources = new ArrayList<>();
        if (mapping != null) {
            metadataSources.add(mapping.sourcePath());
        }

        return AnalyzerSupport.object(
            descriptor.javaIdentifier(),
            descriptor.kind(),
            descriptor.modId(),
            AnalyzerSupport.inventoryFacts(descriptor.registered(), descriptor.assetPresent(), mapping != null ? 1 : 0, patches.size()),
            profile,
            supportResults,
            new Confidence(mapping != null || !patches.isEmpty() ? 0.38D : 0.18D, "Entity analysis is currently metadata-backed and runtime-constrained."),
            AnalyzerSupport.provenance(this.getClass().getSimpleName(), mapping != null || !patches.isEmpty(), patches, metadataSources),
            findings
        );
    }
}