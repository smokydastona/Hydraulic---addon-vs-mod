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
import org.geysermc.hydraulic.metadata.MetadataIndex;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class FluidAnalyzer implements CompatibilityAnalyzer {
    @Override
    public boolean supports(@NotNull ContentInventory.ContentDescriptor descriptor) {
        return descriptor.kind().equals("fluid");
    }

    @Override
    public @NotNull CompatibilityObject analyze(@NotNull ContentInventory.ContentDescriptor descriptor, @NotNull ContentInventory.ModContentInventory inventory, @NotNull MetadataIndex metadataIndex) {
        List<ContentPatch> patches = metadataIndex.contentPatches(Identifier.parse(descriptor.javaIdentifier()));

        Capability registered = AnalyzerSupport.capability(CapabilityDomain.CONTENT, "registered", "Fluid exists in the Java registry.");
        Capability presentation = AnalyzerSupport.capability(CapabilityDomain.PRESENTATION, "fluid_presentation", "Fluid has explicit presentation patch data.");
        Capability interaction = AnalyzerSupport.capability(CapabilityDomain.INTERACTION, "fluid_interaction", "Fluid interactions are implemented on Bedrock.");
        Capability behavior = AnalyzerSupport.capability(CapabilityDomain.BEHAVIOR, "runtime_behavior", "Fluid runtime behavior is represented on Bedrock.");

        List<CapabilityRequirement> requirements = List.of(
            AnalyzerSupport.required(registered),
            AnalyzerSupport.required(presentation),
            AnalyzerSupport.required(interaction),
            AnalyzerSupport.required(behavior)
        );
        List<CapabilityResult> results = List.of(
            AnalyzerSupport.result(registered, descriptor.registered(), "Registry lookup from BuiltInRegistries.FLUID."),
            AnalyzerSupport.result(presentation, !patches.isEmpty(), "Fluid metadata patches are the only current explicit signal."),
            AnalyzerSupport.result(interaction, false, "Fluid bridges are not implemented yet."),
            AnalyzerSupport.result(behavior, false, "Fluid behavior generation is not implemented yet.")
        );

        CapabilityProfile profile = new CapabilityProfile(descriptor.javaIdentifier(), requirements, results);
        Map<String, SupportResult> supportResults = new LinkedHashMap<>();
        supportResults.put("content", AnalyzerSupport.support("content", SupportLevel.AUTOMATIC, List.of(results.get(0)), List.of("Fluid discovery is registry-backed.")));
        supportResults.put("presentation", AnalyzerSupport.support("presentation", !patches.isEmpty() ? SupportLevel.ADAPTED : SupportLevel.UNSUPPORTED, List.of(results.get(1)), List.of("Fluid presentation awaits translator and renderer work.")));
        supportResults.put("interaction", AnalyzerSupport.support("interaction", SupportLevel.UNSUPPORTED, List.of(results.get(2)), List.of("No runtime fluid compatibility layer exists today.")));
        supportResults.put("behavior", AnalyzerSupport.support("behavior", SupportLevel.UNSUPPORTED, List.of(results.get(3)), List.of("Fluid behavior generation is not implemented.")));

        return AnalyzerSupport.object(
            descriptor.javaIdentifier(),
            descriptor.kind(),
            descriptor.modId(),
            AnalyzerSupport.inventoryFacts(descriptor.registered(), descriptor.assetPresent(), 0, patches.size()),
            profile,
            supportResults,
            new Confidence(!patches.isEmpty() ? 0.26D : 0.12D, "Fluid analysis is currently registry-backed with optional patch evidence only."),
            AnalyzerSupport.provenance(this.getClass().getSimpleName(), !patches.isEmpty(), patches, List.of()),
            List.of(new CompatibilityFinding("fluid.bridge.missing", CompatibilityFinding.Severity.WARNING, "behavior", "Fluid runtime support is not implemented for " + descriptor.javaIdentifier(), "Fluids still require dedicated translators and bridges.", "Implement fluid translators and runtime bridges before treating fluid support as functional.", null))
        );
    }
}