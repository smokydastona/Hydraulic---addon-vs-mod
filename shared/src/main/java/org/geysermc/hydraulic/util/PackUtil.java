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
import java.util.List;
import java.util.Map;

/**
 * Utility class for packs.
 */
public class PackUtil {
    protected static final Logger LOGGER = LogUtils.getLogger();
    private static final String CONVERSION_KEY_ALGORITHM = "HYDRAULIC_CONVERSION_KEY_V1";

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
        ModResourceIndex.ResourceFingerprint resourceFingerprint = resourceIndex.fingerprint();
        return new ConversionKey(
            CONVERSION_KEY_ALGORITHM,
            mod.id(),
            mod.version(),
            Constants.VERSION,
            SharedConstants.getCurrentVersion().id(),
            resourceFingerprint.stableValue(),
            resourceFingerprint.fileCount(),
            resourceFingerprint.totalSizeBytes(),
            metadataFingerprint(metadataIndex)
        );
    }

    @NotNull
    public static String metadataFingerprint(@NotNull MetadataIndex metadataIndex) {
        Hasher hasher = Hashing.sha256().newHasher();
        hashSection(hasher, "summary", Constants.GSON.toJson(metadataIndex.summary()));
        hashMappings(hasher, "blocks", metadataIndex.blockMappings());
        hashMappings(hasher, "items", metadataIndex.itemMappings());
        hashMappings(hasher, "recipes", metadataIndex.recipeMappings());
        hashMappings(hasher, "entities", metadataIndex.entityMappings());
        hashMappings(hasher, "menus", metadataIndex.menuMappings());
        metadataIndex.contentPatches().entrySet().stream()
            .sorted(Map.Entry.comparingByKey(Comparator.comparing(Object::toString)))
            .forEach(entry -> {
                hasher.putString("patches", StandardCharsets.UTF_8);
                hasher.putString(entry.getKey().toString(), StandardCharsets.UTF_8);
                for (ContentPatch patch : entry.getValue()) {
                    hasher.putString(Constants.GSON.toJson(patch), StandardCharsets.UTF_8);
                }
            });
        for (MetadataValidationIssue issue : metadataIndex.validationIssues()) {
            hasher.putString("validation", StandardCharsets.UTF_8);
            hasher.putString(Constants.GSON.toJson(issue), StandardCharsets.UTF_8);
        }
        return hasher.hash().toString();
    }

    private static void hashMappings(@NotNull Hasher hasher, @NotNull String section, @NotNull Map<?, ?> mappings) {
        mappings.entrySet().stream()
            .sorted(Map.Entry.comparingByKey(Comparator.comparing(Object::toString)))
            .forEach(entry -> {
                hasher.putString(section, StandardCharsets.UTF_8);
                hasher.putString(entry.getKey().toString(), StandardCharsets.UTF_8);
                hasher.putString(Constants.GSON.toJson(entry.getValue()), StandardCharsets.UTF_8);
            });
    }

    private static void hashSection(@NotNull Hasher hasher, @NotNull String section, @NotNull String value) {
        hasher.putString(section, StandardCharsets.UTF_8);
        hasher.putString(value, StandardCharsets.UTF_8);
    }
}
