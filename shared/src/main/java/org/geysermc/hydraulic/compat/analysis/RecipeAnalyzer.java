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

public final class RecipeAnalyzer implements CompatibilityAnalyzer {
    @Override
    public boolean supports(@NotNull ContentInventory.ContentDescriptor descriptor) {
        return descriptor.kind().equals("recipe");
    }

    @Override
    public @NotNull CompatibilityObject analyze(@NotNull ContentInventory.ContentDescriptor descriptor, @NotNull ContentInventory.ModContentInventory inventory, @NotNull MetadataIndex metadataIndex) {
        IdentifierMapping mapping = metadataIndex.recipeMapping(Identifier.parse(descriptor.javaIdentifier()));
        List<ContentPatch> patches = metadataIndex.contentPatches(Identifier.parse(descriptor.javaIdentifier()));

        Capability recipeData = AnalyzerSupport.capability(CapabilityDomain.STATE_DATA, "recipe_data", "Recipe JSON exists for this identifier.");
        Capability mappingCapability = AnalyzerSupport.capability(CapabilityDomain.STATE_DATA, "recipe_mapping", "Recipe has explicit mapping or patch data when needed.");

        List<CapabilityRequirement> requirements = List.of(AnalyzerSupport.required(recipeData), AnalyzerSupport.required(mappingCapability));
        List<CapabilityResult> results = List.of(
            AnalyzerSupport.result(recipeData, descriptor.assetPresent(), "Recipe detection comes from data/<namespace>/recipes/*.json assets."),
            AnalyzerSupport.result(mappingCapability, mapping != null || !patches.isEmpty() || descriptor.assetPresent(), "Recipe coverage currently depends on recipe JSON presence with optional metadata overrides.")
        );

        CapabilityProfile profile = new CapabilityProfile(descriptor.javaIdentifier(), requirements, results);
        Map<String, SupportResult> supportResults = new LinkedHashMap<>();
        supportResults.put("content", AnalyzerSupport.support("content", SupportLevel.AUTOMATIC, List.of(results.get(0)), List.of("Recipes are asset-backed rather than registry-backed in the current inventory model.")));
        supportResults.put("state_data", AnalyzerSupport.support("state_data", mapping != null || !patches.isEmpty() ? SupportLevel.ADAPTED : SupportLevel.AUTOMATIC, List.of(results.get(0), results.get(1)), List.of("Recipe support reflects JSON presence first and metadata overrides second.")));

        List<String> metadataSources = new ArrayList<>();
        if (mapping != null) {
            metadataSources.add(mapping.sourcePath());
        }

        List<CompatibilityFinding> findings = descriptor.assetPresent() ? List.of() : List.of(
            new CompatibilityFinding("recipe.asset.missing", CompatibilityFinding.Severity.WARNING, "state_data", "Recipe asset discovery failed for " + descriptor.javaIdentifier(), "No recipe JSON was found for this recipe target.", "Add the recipe asset or a metadata patch if this recipe is intentional.", null)
        );

        return AnalyzerSupport.object(
            descriptor.javaIdentifier(),
            descriptor.kind(),
            descriptor.modId(),
            AnalyzerSupport.inventoryFacts(descriptor.registered(), descriptor.assetPresent(), mapping != null ? 1 : 0, patches.size()),
            profile,
            supportResults,
            new Confidence(descriptor.assetPresent() ? (mapping != null || !patches.isEmpty() ? 0.91D : 0.84D) : 0.41D, "Recipe analysis is asset-backed with optional metadata overlays."),
            AnalyzerSupport.provenance(this.getClass().getSimpleName(), mapping != null || !patches.isEmpty(), patches, metadataSources),
            findings
        );
    }
}