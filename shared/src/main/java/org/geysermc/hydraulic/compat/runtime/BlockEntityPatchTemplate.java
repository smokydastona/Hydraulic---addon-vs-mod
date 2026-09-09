package org.geysermc.hydraulic.compat.runtime;

import org.geysermc.hydraulic.compat.mapping.ContentPatch;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BlockEntityPatchTemplate {
    private static final String BEDROCK_BLOCK_ENTITY_PREFIX = "bedrock.block_entity.";
    private static final String BEDROCK_BLOCK_ENTITY_DATA_PREFIX = BEDROCK_BLOCK_ENTITY_PREFIX + "data.";

    private final @Nullable String bedrockIdentifier;
    private final @NotNull List<TagMutation> mutations;

    private BlockEntityPatchTemplate(@Nullable String bedrockIdentifier, @NotNull List<TagMutation> mutations) {
        this.bedrockIdentifier = bedrockIdentifier;
        this.mutations = List.copyOf(mutations);
    }

    @Nullable
    public static BlockEntityPatchTemplate resolve(@NotNull List<ContentPatch> patches) {
        String bedrockIdentifier = null;
        Map<String, TagValue> values = new LinkedHashMap<>();

        for (ContentPatch patch : patches) {
            String patchBedrockIdentifier = normalizeString(patch.operation(BEDROCK_BLOCK_ENTITY_PREFIX + "id"));
            if (patchBedrockIdentifier != null) {
                bedrockIdentifier = patchBedrockIdentifier;
            }

            for (Map.Entry<String, String> entry : patch.operations().entrySet()) {
                if (!entry.getKey().startsWith(BEDROCK_BLOCK_ENTITY_DATA_PREFIX)) {
                    continue;
                }

                String path = entry.getKey().substring(BEDROCK_BLOCK_ENTITY_DATA_PREFIX.length());
                if (path.isBlank()) {
                    continue;
                }
                values.put(path, TagValue.parse(entry.getValue()));
            }
        }

        if (bedrockIdentifier == null && values.isEmpty()) {
            return null;
        }

        List<TagMutation> mutations = new ArrayList<>(values.size());
        for (Map.Entry<String, TagValue> entry : values.entrySet()) {
            mutations.add(new TagMutation(List.of(entry.getKey().split("\\.")), entry.getValue()));
        }
        return new BlockEntityPatchTemplate(bedrockIdentifier, mutations);
    }

    public static boolean supports(@NotNull List<ContentPatch> patches) {
        return resolve(patches) != null;
    }

    @Nullable
    public String bedrockIdentifier() {
        return this.bedrockIdentifier;
    }

    @NotNull
    public List<TagMutation> mutations() {
        return this.mutations;
    }

    @Nullable
    private static String normalizeString(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public record TagMutation(@NotNull List<String> path, @NotNull TagValue value) {
        public TagMutation {
            path = List.copyOf(path);
        }
    }

    public record TagValue(@NotNull Kind kind, @Nullable Object value) {
        @NotNull
        static TagValue parse(@Nullable String rawValue) {
            if (rawValue == null || rawValue.equals("null")) {
                return new TagValue(Kind.NULL, null);
            }
            if (rawValue.equalsIgnoreCase("true") || rawValue.equalsIgnoreCase("false")) {
                return new TagValue(Kind.BOOLEAN, Boolean.parseBoolean(rawValue));
            }
            try {
                long longValue = Long.parseLong(rawValue);
                if (longValue >= Integer.MIN_VALUE && longValue <= Integer.MAX_VALUE) {
                    return new TagValue(Kind.INTEGER, (int) longValue);
                }
                return new TagValue(Kind.LONG, longValue);
            } catch (NumberFormatException ignored) {
            }
            try {
                return new TagValue(Kind.DOUBLE, Double.parseDouble(rawValue));
            } catch (NumberFormatException ignored) {
            }
            return new TagValue(Kind.STRING, rawValue);
        }

        public enum Kind {
            NULL,
            BOOLEAN,
            INTEGER,
            LONG,
            DOUBLE,
            STRING
        }
    }
}