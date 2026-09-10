package org.geysermc.hydraulic.compat.analysis;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FlowingFluid;
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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class FluidAnalyzer implements CompatibilityAnalyzer {
    @Override
    public @NotNull String kind() {
        return "fluid";
    }

    @Override
    public @NotNull CompatibilityObject analyze(@NotNull ContentInventory.ContentDescriptor descriptor, @NotNull ContentInventory.ModContentInventory inventory, @NotNull MetadataIndex metadataIndex) {
        Identifier identifier = Identifier.parse(descriptor.javaIdentifier());
        Fluid fluid = BuiltInRegistries.FLUID.getValue(identifier);
        Identifier sourceFluid = sourceFluid(fluid);
        List<ContentPatch> patches = patches(identifier, sourceFluid, metadataIndex);
        Identifier bucketItem = bucketItem(fluid);
        String bucketTexture = patches.stream()
            .map(patch -> patch.operation("visual.bucket_texture"))
            .filter(texture -> texture != null && !texture.isBlank())
            .findFirst()
            .orElse(null);
        boolean bucketBridgeAvailable = bucketItem != null && bucketTexture != null;

        Capability registered = AnalyzerSupport.capability(CapabilityDomain.CONTENT, "registered", "Fluid exists in the Java registry.");
        Capability presentation = AnalyzerSupport.capability(CapabilityDomain.PRESENTATION, "fluid_presentation", "Fluid has explicit presentation patch data.");
        Capability interaction = AnalyzerSupport.capability(CapabilityDomain.INTERACTION, "fluid_interaction", "Fluid interactions are implemented on Bedrock.");
        Capability behavior = AnalyzerSupport.capability(CapabilityDomain.BEHAVIOR, "runtime_behavior", "Fluid runtime behavior is represented on Bedrock.");
        Capability fluidTransfer = AnalyzerSupport.capability(CapabilityDomain.BEHAVIOR, "fluid_transfer", "Fluid transfer capabilities are available for this fluid.");

        List<CapabilityRequirement> requirements = List.of(
            AnalyzerSupport.required(registered),
            AnalyzerSupport.required(presentation),
            AnalyzerSupport.required(interaction),
            AnalyzerSupport.required(behavior),
            AnalyzerSupport.optional(fluidTransfer)
        );
        List<CapabilityResult> results = List.of(
            AnalyzerSupport.result(registered, descriptor.registered(), "Registry lookup from BuiltInRegistries.FLUID."),
            AnalyzerSupport.result(presentation, bucketBridgeAvailable, bucketBridgeAvailable ? "Metadata patch declares a bucket icon fallback that reuses the existing bucket item registration path." : "Fluid presentation bridge fields are not implemented for this fluid yet."),
            AnalyzerSupport.result(interaction, false, "Fluid bridges are not implemented yet."),
            AnalyzerSupport.result(behavior, false, "Fluid behavior generation is not implemented yet."),
            AnalyzerSupport.result(fluidTransfer, false, "Fluid transfer bridges are not implemented yet.")
        );

        CapabilityProfile profile = new CapabilityProfile(descriptor.javaIdentifier(), requirements, results);
        Map<String, SupportResult> supportResults = new LinkedHashMap<>();
        supportResults.put("content", AnalyzerSupport.support("content", SupportLevel.AUTOMATIC, List.of(results.get(0)), List.of("Fluid discovery is registry-backed.")));
        supportResults.put("presentation", AnalyzerSupport.support("presentation", bucketBridgeAvailable ? SupportLevel.ADAPTED : SupportLevel.UNSUPPORTED, List.of(results.get(1)), List.of(bucketBridgeAvailable ? "Fluid presentation can currently reuse an explicit metadata-backed bucket icon fallback." : "Fluid presentation awaits a translator or an explicit bucket bridge field.")));
        supportResults.put("interaction", AnalyzerSupport.support("interaction", SupportLevel.UNSUPPORTED, List.of(results.get(2)), List.of("No runtime fluid compatibility layer exists today.")));
        supportResults.put("behavior", AnalyzerSupport.support("behavior", SupportLevel.UNSUPPORTED, List.of(results.get(3)), List.of("Fluid behavior generation is not implemented.")));
        supportResults.put("transfer", AnalyzerSupport.support("transfer", SupportLevel.UNSUPPORTED, List.of(results.get(4)), List.of("Fluid transfer bridges are not implemented yet.")));

        Map<String, String> inventoryFacts = AnalyzerSupport.inventoryFacts(descriptor.registered(), descriptor.assetPresent(), 0, patches.size());
        if (bucketItem != null) {
            inventoryFacts.put("bucket_item", bucketItem.toString());
        }
        if (bucketTexture != null) {
            inventoryFacts.put("bucket_texture", bucketTexture);
        }
        if (sourceFluid != null && !sourceFluid.equals(identifier)) {
            inventoryFacts.put("source_fluid", sourceFluid.toString());
        }

        List<String> runtimeRequirements = new ArrayList<>();
        if (bucketBridgeAvailable) {
            runtimeRequirements.add("fluid.bucket_texture_fallback");
        } else {
            runtimeRequirements.add("fluid_translator");
        }
        if (!results.get(3).supported()) {
            runtimeRequirements.add("fluid_runtime_bridge");
        }
        if (!results.get(4).supported()) {
            runtimeRequirements.add("fluid_transfer_bridge");
        }

        return AnalyzerSupport.object(
            descriptor.javaIdentifier(),
            descriptor.kind(),
            descriptor.modId(),
            inventoryFacts,
            profile,
            supportResults,
            runtimeRequirements,
            new Confidence(bucketBridgeAvailable ? 0.42D : !patches.isEmpty() ? 0.2D : 0.12D, bucketBridgeAvailable ? "Fluid analysis is registry-backed with an explicit metadata-backed bucket icon fallback." : "Fluid analysis is currently registry-backed with optional patch evidence only."),
            AnalyzerSupport.provenance(this.getClass().getSimpleName(), !patches.isEmpty(), patches, List.of()),
            List.of(new CompatibilityFinding("fluid.bridge.partial", CompatibilityFinding.Severity.WARNING, bucketBridgeAvailable ? "behavior" : "presentation", "Fluid support is partial for " + descriptor.javaIdentifier(), bucketBridgeAvailable ? "Hydraulic can reuse an explicit bucket icon fallback for this fluid, but world translation and runtime behavior are still missing." : "Fluids still require dedicated translators and bridges.", "Implement fluid translators and runtime bridges before treating fluid support as functional.", null))
        );
    }

    private static Identifier bucketItem(Fluid fluid) {
        if (fluid == null) {
            return null;
        }

        Item bucket = fluid.getBucket();
        if (bucket == null) {
            return null;
        }

        Identifier bucketIdentifier = BuiltInRegistries.ITEM.getKey(bucket);
        return bucketIdentifier != null && !BuiltInRegistries.ITEM.getDefaultKey().equals(bucketIdentifier) ? bucketIdentifier : null;
    }

    private static Identifier sourceFluid(Fluid fluid) {
        if (!(fluid instanceof FlowingFluid flowingFluid)) {
            return null;
        }

        Fluid source = flowingFluid.getSource();
        if (source == null || source == fluid) {
            return null;
        }

        Identifier sourceIdentifier = BuiltInRegistries.FLUID.getKey(source);
        return sourceIdentifier != null && !BuiltInRegistries.FLUID.getDefaultKey().equals(sourceIdentifier) ? sourceIdentifier : null;
    }

    private static List<ContentPatch> patches(Identifier identifier, Identifier sourceFluid, MetadataIndex metadataIndex) {
        if (sourceFluid == null || sourceFluid.equals(identifier)) {
            return metadataIndex.contentPatches(identifier);
        }

        List<ContentPatch> localPatches = metadataIndex.contentPatches(identifier);
        List<ContentPatch> sourcePatches = metadataIndex.contentPatches(sourceFluid);
        if (localPatches.isEmpty()) {
            return sourcePatches;
        }
        if (sourcePatches.isEmpty()) {
            return localPatches;
        }

        List<ContentPatch> merged = new ArrayList<>(localPatches.size() + sourcePatches.size());
        merged.addAll(localPatches);
        merged.addAll(sourcePatches);
        return List.copyOf(merged);
    }
}