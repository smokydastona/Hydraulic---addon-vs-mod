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

public final class BlockEntityAnalyzer implements CompatibilityAnalyzer {
    @Override
    public boolean supports(@NotNull ContentInventory.ContentDescriptor descriptor) {
        return descriptor.kind().equals("block_entity");
    }

    @Override
    public @NotNull CompatibilityObject analyze(@NotNull ContentInventory.ContentDescriptor descriptor, @NotNull ContentInventory.ModContentInventory inventory, @NotNull MetadataIndex metadataIndex) {
        List<ContentPatch> patches = metadataIndex.contentPatches(Identifier.parse(descriptor.javaIdentifier()));

        Capability registered = AnalyzerSupport.capability(CapabilityDomain.CONTENT, "registered", "Block entity exists in the Java registry.");
        Capability data = AnalyzerSupport.capability(CapabilityDomain.STATE_DATA, "persistent_data", "Block entity data can be represented on Bedrock.");
        Capability interaction = AnalyzerSupport.capability(CapabilityDomain.INTERACTION, "container_interaction", "Block entity interactions have a runtime bridge.");
        Capability behavior = AnalyzerSupport.capability(CapabilityDomain.BEHAVIOR, "runtime_behavior", "Block entity behavior is translated to Bedrock.");

        List<CapabilityRequirement> requirements = List.of(
            AnalyzerSupport.required(registered),
            AnalyzerSupport.required(data),
            AnalyzerSupport.required(interaction),
            AnalyzerSupport.required(behavior)
        );
        List<CapabilityResult> results = List.of(
            AnalyzerSupport.result(registered, descriptor.registered(), "Registry lookup from BuiltInRegistries.BLOCK_ENTITY_TYPE."),
            AnalyzerSupport.result(data, !patches.isEmpty(), "Patch metadata is the only current explicit data signal."),
            AnalyzerSupport.result(interaction, false, "Block entity interaction bridges are not implemented yet."),
            AnalyzerSupport.result(behavior, false, "Block entity runtime behavior is not implemented yet.")
        );

        CapabilityProfile profile = new CapabilityProfile(descriptor.javaIdentifier(), requirements, results);
        Map<String, SupportResult> supportResults = new LinkedHashMap<>();
        supportResults.put("content", AnalyzerSupport.support("content", SupportLevel.AUTOMATIC, List.of(results.get(0)), List.of("Block entities are discovered directly from the runtime registry.")));
        supportResults.put("state_data", AnalyzerSupport.support("state_data", !patches.isEmpty() ? SupportLevel.ADAPTED : SupportLevel.UNSUPPORTED, List.of(results.get(1)), List.of("Dedicated block entity metadata models are not implemented yet.")));
        supportResults.put("interaction", AnalyzerSupport.support("interaction", SupportLevel.UNSUPPORTED, List.of(results.get(2)), List.of("No block entity interaction bridge exists yet.")));
        supportResults.put("behavior", AnalyzerSupport.support("behavior", SupportLevel.UNSUPPORTED, List.of(results.get(3)), List.of("Block entity behavior translation is not implemented.")));

        return AnalyzerSupport.object(
            descriptor.javaIdentifier(),
            descriptor.kind(),
            descriptor.modId(),
            AnalyzerSupport.inventoryFacts(descriptor.registered(), descriptor.assetPresent(), 0, patches.size()),
            profile,
            supportResults,
            new Confidence(!patches.isEmpty() ? 0.24D : 0.1D, "Block entity analysis is registry-backed with optional patch evidence only."),
            AnalyzerSupport.provenance(this.getClass().getSimpleName(), !patches.isEmpty(), patches, List.of()),
            List.of(new CompatibilityFinding("block_entity.bridge.missing", CompatibilityFinding.Severity.WARNING, "behavior", "Block entity runtime support is not implemented for " + descriptor.javaIdentifier(), "Block entities still need dedicated data, interaction, and behavior bridges.", "Implement block entity bridges before treating block entity support as functional.", null))
        );
    }
}