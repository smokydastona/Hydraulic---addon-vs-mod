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

public final class ItemAnalyzer implements CompatibilityAnalyzer {
    @Override
    public boolean supports(@NotNull ContentInventory.ContentDescriptor descriptor) {
        return descriptor.kind().equals("item");
    }

    @Override
    public @NotNull CompatibilityObject analyze(@NotNull ContentInventory.ContentDescriptor descriptor, @NotNull ContentInventory.ModContentInventory inventory, @NotNull MetadataIndex metadataIndex) {
        Identifier identifier = Identifier.parse(descriptor.javaIdentifier());
        IdentifierMapping mapping = metadataIndex.itemMapping(identifier);
        List<ContentPatch> patches = metadataIndex.contentPatches(identifier);
        boolean patched = !patches.isEmpty();

        Capability registered = AnalyzerSupport.capability(CapabilityDomain.CONTENT, "registered", "Item exists in the Java registry.");
        Capability icon = AnalyzerSupport.capability(CapabilityDomain.PRESENTATION, "item_asset", "Item has discoverable item-model assets.");
        Capability componentTranslation = AnalyzerSupport.capability(CapabilityDomain.STATE_DATA, "component_translation", "Known item components can be translated to Geyser custom item data.");
        Capability mappingCapability = AnalyzerSupport.capability(CapabilityDomain.STATE_DATA, "identifier_mapping", "Item has explicit identifier mapping or patch data when needed.");
        Capability offhand = AnalyzerSupport.capability(CapabilityDomain.INTERACTION, "offhand", "Item can be used with current Bedrock item registration support.");
        Capability behavior = AnalyzerSupport.capability(CapabilityDomain.BEHAVIOR, "runtime_behavior", "Item behavior does not require a custom runtime bridge.");

        List<CapabilityRequirement> requirements = List.of(
            AnalyzerSupport.required(registered),
            AnalyzerSupport.required(icon),
            AnalyzerSupport.required(componentTranslation),
            AnalyzerSupport.required(mappingCapability),
            AnalyzerSupport.required(offhand),
            AnalyzerSupport.required(behavior)
        );
        List<CapabilityResult> results = List.of(
            AnalyzerSupport.result(registered, descriptor.registered(), "Registry lookup from BuiltInRegistries.ITEM."),
            AnalyzerSupport.result(icon, descriptor.assetPresent(), "PackManager found a matching item-model asset for this item."),
            AnalyzerSupport.result(componentTranslation, true, "ItemPackModule already routes item components through ComponentConverter."),
            AnalyzerSupport.result(mappingCapability, mapping != null || patched || descriptor.assetPresent(), "Items can use discovered models, explicit mappings, or patches."),
            AnalyzerSupport.result(offhand, true, "ItemPackModule currently sets allowOffhand(true) for custom item options."),
            AnalyzerSupport.result(behavior, !patched || patches.stream().noneMatch(patch -> patch.hasOperationPrefix("behavior.")), "Behavior-oriented item patches still require future runtime support.")
        );

        CapabilityProfile profile = new CapabilityProfile(descriptor.javaIdentifier(), requirements, results);
        Map<String, SupportResult> supportResults = new LinkedHashMap<>();
        supportResults.put("content", AnalyzerSupport.support("content", SupportLevel.AUTOMATIC, List.of(results.get(0)), List.of("Items are discovered directly from the active item registry.")));
        supportResults.put("presentation", AnalyzerSupport.support("presentation", patched || mapping != null ? SupportLevel.ADAPTED : SupportLevel.AUTOMATIC, List.of(results.get(1)), List.of("Presentation coverage is based on item-model discovery and optional metadata.")));
        supportResults.put("state_data", AnalyzerSupport.support("state_data", patched || mapping != null ? SupportLevel.ADAPTED : SupportLevel.AUTOMATIC, List.of(results.get(2), results.get(3)), List.of("Current item analysis benefits from the existing component conversion path.")));
        supportResults.put("interaction", AnalyzerSupport.support("interaction", SupportLevel.AUTOMATIC, List.of(results.get(4)), List.of("Custom item registration already covers baseline Bedrock interaction surfaces.")));
        supportResults.put("behavior", AnalyzerSupport.support("behavior", results.get(5).supported() ? SupportLevel.AUTOMATIC : SupportLevel.APPROXIMATED, List.of(results.get(5)), List.of("Behavior-specific item runtime bridges are not implemented yet.")));

        List<CompatibilityFinding> findings = new ArrayList<>();
        if (!descriptor.assetPresent()) {
            findings.add(new CompatibilityFinding("item.asset.missing", CompatibilityFinding.Severity.WARNING, "presentation", "Item asset discovery failed for " + descriptor.javaIdentifier(), "The item does not currently have a discovered item-model asset in this mod root.", "Add an item model or metadata patch for this item.", null));
        }

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
            new Confidence(descriptor.assetPresent() ? (mapping != null || patched ? 0.94D : 0.87D) : 0.52D, "Inventory-backed item analyzer with component translation signals."),
            AnalyzerSupport.provenance(this.getClass().getSimpleName(), mapping != null || patched, patches, metadataSources),
            findings
        );
    }
}