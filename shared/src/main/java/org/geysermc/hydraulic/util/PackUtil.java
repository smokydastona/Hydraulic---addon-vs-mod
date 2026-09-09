package org.geysermc.hydraulic.util;

import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.mojang.logging.LogUtils;
import net.kyori.adventure.key.Key;
import net.minecraft.SharedConstants;
import org.geysermc.hydraulic.Constants;
import org.geysermc.hydraulic.cache.ConversionKey;
import org.geysermc.hydraulic.compat.mapping.ContentPatch;
import org.geysermc.hydraulic.metadata.BlockMapping;
import org.geysermc.hydraulic.metadata.IdentifierMapping;
import org.geysermc.hydraulic.metadata.MetadataIndex;
import org.geysermc.hydraulic.metadata.MetadataValidationIssue;
import org.geysermc.hydraulic.pack.ModResourceIndex;
import org.geysermc.hydraulic.platform.mod.ModInfo;
import org.geysermc.pack.converter.type.model.ModelStitcher;
import org.geysermc.pack.converter.util.JsonMappings;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import team.unnamed.creative.model.Model;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Utility class for packs.
 */
public class PackUtil {
    protected static final Logger LOGGER = LogUtils.getLogger();
    private static final String CONVERSION_KEY_ALGORITHM = "HYDRAULIC_CONVERSION_KEY_V2";

    public static String getTextureName(@NotNull String modelName) {
        // TODO Sometimes things end up in the minecraft namespace when they shouldn't.
        //      We should look at the current mods resources to see if we find a match there first
        //      EG: betternether:wall_mushroom_red refrencing both mushroom_red_new (its own) and mushroom_block_inside (mc)
        if (modelName.startsWith(Key.MINECRAFT_NAMESPACE)) {
            String modelValue = modelName.split(":")[1];

            // Need to use the Bedrock value for vanilla textures
            JsonMappings mappings = JsonMappings.getMapping("textures");
            if (mappings != null) {
                String output = mappings.map(modelValue).getFirst();

                String value = output.substring(output.indexOf("/") + 1);

                if (modelValue.equals(output)) {
                    return value;
                }

                return Constants.MOD_ID + ":" + output;
            }

            return modelValue.substring(modelValue.indexOf("/") + 1);
        }

        return modelName.replace("block/", "").replace("item/", "");
    }

    /**
     * Walks the parent chain of the given model upwards using the given provider.
     *
     * @param provider the provider to resolve parent models with
     * @param model the model to walk the parents of
     * @return the model and its parents
     */
    @NotNull
    public static List<Key> modelParents(@NotNull ModelStitcher.Provider provider, @NotNull Model model) {
        List<Key> keys = new ArrayList<>();
        keys.add(model.key());

        Model current = model;
        Key parentKey;
        while ((parentKey = current.parent()) != null) {
            keys.add(parentKey);

            Model parent = provider.model(parentKey);
            if (parent == null) {
                break; // e.g. builtin/generated, which has no model file
            }

            current = parent;
        }

        return keys;
    }

    @NotNull
    public static ConversionKey conversionKey(@NotNull ModInfo mod, @NotNull ModResourceIndex resourceIndex, @NotNull MetadataIndex metadataIndex) {
        return conversionKey(mod, resourceIndex, Map.of(mod.id(), resourceIndex), metadataIndex);
    }

    @NotNull
    public static ConversionKey conversionKey(
        @NotNull ModInfo mod,
        @NotNull ModResourceIndex resourceIndex,
        @NotNull Map<String, ModResourceIndex> allIndexes,
        @NotNull MetadataIndex metadataIndex
    ) {
        ModResourceIndex.ResourceFingerprint resourceFingerprint = resourceIndex.fingerprint();
        DependencyFingerprint dependencyFingerprint = dependencyFingerprint(mod, allIndexes);
        return new ConversionKey(
            CONVERSION_KEY_ALGORITHM,
            mod.id(),
            mod.version(),
            Constants.VERSION,
            SharedConstants.getCurrentVersion().id(),
            resourceFingerprint.stableValue(),
            resourceFingerprint.fileCount(),
            resourceFingerprint.totalSizeBytes(),
            metadataFingerprint(metadataIndex),
            dependencyFingerprint.value(),
            dependencyFingerprint.modCount()
        );
    }

    @NotNull
    static DependencyFingerprint dependencyFingerprint(@NotNull ModInfo mod, @NotNull Map<String, ModResourceIndex> allIndexes) {
        ModResourceIndex resourceIndex = allIndexes.get(mod.id());
        if (resourceIndex == null) {
            return new DependencyFingerprint("", 0);
        }

        Set<String> dependentModIds = new java.util.TreeSet<>();
        List<String> pendingNamespaces = new ArrayList<>(resourceIndex.dependencyNamespaces());
        Set<String> visitedNamespaces = new LinkedHashSet<>();

        for (int index = 0; index < pendingNamespaces.size(); index++) {
            String namespace = pendingNamespaces.get(index);
            if (!visitedNamespaces.add(namespace)) {
                continue;
            }

            for (Map.Entry<String, ModResourceIndex> entry : allIndexes.entrySet()) {
                if (mod.id().equals(entry.getKey()) || !entry.getValue().namespaces().contains(namespace)) {
                    continue;
                }

                if (dependentModIds.add(entry.getKey())) {
                    pendingNamespaces.addAll(entry.getValue().dependencyNamespaces());
                }
            }
        }

        Hasher hasher = Hashing.sha256().newHasher();
        for (String dependentModId : dependentModIds) {
            ModResourceIndex dependentIndex = allIndexes.get(dependentModId);
            if (dependentIndex == null) {
                continue;
            }
            hasher.putString(dependentModId, StandardCharsets.UTF_8);
            hasher.putString(dependentIndex.fingerprint().stableValue(), StandardCharsets.UTF_8);
        }
        return new DependencyFingerprint(hasher.hash().toString(), dependentModIds.size());
    }

    @NotNull
    public static String metadataFingerprint(@NotNull MetadataIndex metadataIndex) {
        Hasher hasher = Hashing.sha256().newHasher();
        hashSummary(hasher, metadataIndex.summary());
        hashBlockMappings(hasher, metadataIndex.blockMappings());
        hashIdentifierMappings(hasher, "items", metadataIndex.itemMappings());
        hashIdentifierMappings(hasher, "recipes", metadataIndex.recipeMappings());
        hashIdentifierMappings(hasher, "entities", metadataIndex.entityMappings());
        hashIdentifierMappings(hasher, "menus", metadataIndex.menuMappings());
        metadataIndex.contentPatches().entrySet().stream()
            .sorted(Map.Entry.comparingByKey(Comparator.comparing(Object::toString)))
            .forEach(entry -> {
                hasher.putString("patches", StandardCharsets.UTF_8);
                hasher.putString(entry.getKey().toString(), StandardCharsets.UTF_8);
                for (ContentPatch patch : entry.getValue()) {
                    hashContentPatch(hasher, patch);
                }
            });
        for (MetadataValidationIssue issue : metadataIndex.validationIssues()) {
            hasher.putString("validation", StandardCharsets.UTF_8);
            hasher.putString(issue.code(), StandardCharsets.UTF_8);
            hasher.putString(issue.severity(), StandardCharsets.UTF_8);
            hasher.putString(Objects.toString(issue.message(), ""), StandardCharsets.UTF_8);
            hasher.putString(Objects.toString(issue.sourcePath(), ""), StandardCharsets.UTF_8);
            hasher.putString(Objects.toString(issue.target(), ""), StandardCharsets.UTF_8);
        }
        return hasher.hash().toString();
    }

    private static void hashSummary(@NotNull Hasher hasher, @NotNull MetadataIndex.Summary summary) {
        hasher.putString("summary", StandardCharsets.UTF_8);
        hasher.putInt(summary.fileCount());
        hasher.putInt(summary.blockMappingCount());
        hasher.putInt(summary.itemMappingCount());
        hasher.putInt(summary.recipeMappingCount());
        hasher.putInt(summary.entityMappingCount());
        hasher.putInt(summary.menuMappingCount());
        hasher.putInt(summary.patchCount());
        hasher.putInt(summary.ruleCount());
        hasher.putInt(summary.validationIssueCount());
        summary.ownershipFileCounts().entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> {
                hasher.putString(entry.getKey(), StandardCharsets.UTF_8);
                hasher.putInt(entry.getValue());
            });
    }

    record DependencyFingerprint(@NotNull String value, int modCount) {
    }

    private static void hashBlockMappings(@NotNull Hasher hasher, @NotNull Map<?, BlockMapping> mappings) {
        mappings.entrySet().stream()
            .sorted(Map.Entry.comparingByKey(Comparator.comparing(Object::toString)))
            .forEach(entry -> {
                hasher.putString("blocks", StandardCharsets.UTF_8);
                hasher.putString(entry.getKey().toString(), StandardCharsets.UTF_8);
                hashBlockMapping(hasher, entry.getValue());
            });
    }

    private static void hashIdentifierMappings(@NotNull Hasher hasher, @NotNull String section, @NotNull Map<?, IdentifierMapping> mappings) {
        mappings.entrySet().stream()
            .sorted(Map.Entry.comparingByKey(Comparator.comparing(Object::toString)))
            .forEach(entry -> {
                hasher.putString(section, StandardCharsets.UTF_8);
                hasher.putString(entry.getKey().toString(), StandardCharsets.UTF_8);
                hashIdentifierMapping(hasher, entry.getValue());
            });
    }

    private static void hashBlockMapping(@NotNull Hasher hasher, @NotNull BlockMapping mapping) {
        hasher.putString(mapping.javaIdentifier().toString(), StandardCharsets.UTF_8);
        for (var rule : mapping.rules()) {
            rule.javaWhen().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    hasher.putString(entry.getKey(), StandardCharsets.UTF_8);
                    hasher.putString(entry.getValue(), StandardCharsets.UTF_8);
                });
            hasher.putString(Objects.toString(rule.bedrockIdentifier(), ""), StandardCharsets.UTF_8);
            if (rule.bedrockState() != null) {
                rule.bedrockState().entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> {
                        hasher.putString(entry.getKey(), StandardCharsets.UTF_8);
                        hasher.putString(entry.getValue(), StandardCharsets.UTF_8);
                    });
            }
            hasher.putString(Objects.toString(rule.geometryId(), ""), StandardCharsets.UTF_8);
            hasher.putString(Objects.toString(rule.materialId(), ""), StandardCharsets.UTF_8);
            hasher.putBoolean(rule.behaviorRequired());
            hasher.putString(Objects.toString(rule.behaviorTag(), ""), StandardCharsets.UTF_8);
            hasher.putString(rule.ownership().name(), StandardCharsets.UTF_8);
            hasher.putString(rule.sourcePath(), StandardCharsets.UTF_8);
            hasher.putInt(rule.priority());
            hasher.putInt(rule.order());
        }
    }

    private static void hashIdentifierMapping(@NotNull Hasher hasher, @NotNull IdentifierMapping mapping) {
        hasher.putString(mapping.javaIdentifier().toString(), StandardCharsets.UTF_8);
        hasher.putString(mapping.bedrockIdentifier().toString(), StandardCharsets.UTF_8);
        hasher.putString(mapping.ownership().name(), StandardCharsets.UTF_8);
        hasher.putString(mapping.sourcePath(), StandardCharsets.UTF_8);
        hasher.putInt(mapping.priority());
        hasher.putInt(mapping.order());
    }

    private static void hashContentPatch(@NotNull Hasher hasher, @NotNull ContentPatch patch) {
        hasher.putString(patch.target().toString(), StandardCharsets.UTF_8);
        hasher.putString(Objects.toString(patch.contentType(), ""), StandardCharsets.UTF_8);
        patch.operations().entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> {
                hasher.putString(entry.getKey(), StandardCharsets.UTF_8);
                hasher.putString(entry.getValue(), StandardCharsets.UTF_8);
            });
        hasher.putString(patch.ownership().name(), StandardCharsets.UTF_8);
        hasher.putString(patch.sourcePath(), StandardCharsets.UTF_8);
        hasher.putInt(patch.priority());
        hasher.putInt(patch.order());
    }

    private static void hashSection(@NotNull Hasher hasher, @NotNull String section, @NotNull String value) {
        hasher.putString(section, StandardCharsets.UTF_8);
        hasher.putString(value, StandardCharsets.UTF_8);
    }
}
