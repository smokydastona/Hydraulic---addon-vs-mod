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
    public @NotNull String kind() {
        return "recipe";
    }

    @Override
    public @NotNull CompatibilityObject analyze(@NotNull ContentInventory.ContentDescriptor descriptor, @NotNull ContentInventory.ModContentInventory inventory, @NotNull MetadataIndex metadataIndex) {
        IdentifierMapping mapping = metadataIndex.recipeMapping(Identifier.parse(descriptor.javaIdentifier()));
        List<ContentPatch> patches = metadataIndex.contentPatches(Identifier.parse(descriptor.javaIdentifier()));
        RecipeFactExtractor.Result extracted = RecipeFactExtractor.extract(inventory, descriptor.javaIdentifier());

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

        List<CompatibilityFinding> findings = new ArrayList<>();
        if (!descriptor.assetPresent()) {
            findings.add(new CompatibilityFinding("recipe.asset.missing", CompatibilityFinding.Severity.WARNING, "state_data", "Recipe asset discovery failed for " + descriptor.javaIdentifier(), "No recipe JSON was found for this recipe target.", "Add the recipe asset or a metadata patch if this recipe is intentional.", null));
        } else if (extracted.status() == RecipeFactExtractor.Status.MALFORMED) {
            findings.add(new CompatibilityFinding("recipe.schema.malformed", CompatibilityFinding.Severity.WARNING, "state_data", "Recipe JSON is malformed for " + descriptor.javaIdentifier(), "The indexed recipe file could not be parsed as a JSON object.", "Repair the recipe JSON; no normalized recipe facts were emitted.", null));
        } else if (extracted.status() == RecipeFactExtractor.Status.UNSUPPORTED_SCHEMA) {
            findings.add(new CompatibilityFinding("recipe.schema.unsupported", CompatibilityFinding.Severity.INFO, "state_data", "Recipe schema is not normalized for " + descriptor.javaIdentifier(), "The recipe exists but does not expose a recognized type, input, output, or duration field.", "Review the recipe manually before using it as machine-process evidence.", null));
        }

        Map<String, String> inventoryFacts = new LinkedHashMap<>(AnalyzerSupport.inventoryFacts(descriptor.registered(), descriptor.assetPresent(), mapping != null ? 1 : 0, patches.size()));
        inventoryFacts.putAll(extracted.facts());
        inventoryFacts.put("recipe.fact_status", extracted.status().name().toLowerCase(java.util.Locale.ROOT));

        return AnalyzerSupport.object(
            descriptor.javaIdentifier(),
            descriptor.kind(),
            descriptor.modId(),
            inventoryFacts,
            profile,
            supportResults,
            new Confidence(descriptor.assetPresent() ? (mapping != null || !patches.isEmpty() ? 0.91D : 0.84D) : 0.41D, "Recipe analysis is asset-backed with optional metadata overlays."),
            AnalyzerSupport.provenance(this.getClass().getSimpleName(), mapping != null || !patches.isEmpty(), patches, metadataSources),
            List.copyOf(findings)
        );
    }
}