package org.geysermc.hydraulic.compat.runtime;

import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import net.minecraft.resources.Identifier;
import org.geysermc.geyser.level.block.type.BlockState;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.level.block.entity.BlockEntityTranslator;
import org.geysermc.hydraulic.compat.CompatibilityRegistry;
import org.geysermc.hydraulic.compat.adapter.AdapterFeature;
import org.geysermc.hydraulic.compat.ir.CompiledCompatibilityPlan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class BlockEntityPatchTranslatorFactory {
    private BlockEntityPatchTranslatorFactory() {
    }

    @Nullable
    public static BlockEntityTranslator create(
        @NotNull GeyserSession session,
        @NotNull Vector3i position,
        @NotNull CompatibilityRegistry compatibilityRegistry
    ) {
        String javaIdentifier = CompatibilityRuntimeDiagnostics.resolveJavaBlockEntityIdentifier(session, position);
        if (javaIdentifier == null) {
            return null;
        }

        CompiledCompatibilityPlan plan = compatibilityRegistry.dispatchTable().blockEntity(Identifier.parse(javaIdentifier));
        if (!BridgeAdapterSupport.supportsBlockEntityPatch(plan)) {
            return null;
        }
        return create(plan);
    }

    static boolean supports(@Nullable CompiledCompatibilityPlan plan) {
        return BridgeAdapterSupport.supportsBlockEntityPatch(plan);
    }

    @Nullable
    static BlockEntityTranslator create(@Nullable CompiledCompatibilityPlan plan) {
        if (!supports(plan)) {
            return null;
        }
        return new MetadataBackedBlockEntityTranslator(plan.blockEntityPatchTemplate());
    }

    private static final class MetadataBackedBlockEntityTranslator extends BlockEntityTranslator {
        private final BlockEntityPatchTemplate template;

        private MetadataBackedBlockEntityTranslator(@NotNull BlockEntityPatchTemplate template) {
            this.template = template;
        }

        @Override
        public void translateTag(@NotNull GeyserSession session, @NotNull NbtMapBuilder bedrockTag, @Nullable NbtMap javaTag, @Nullable BlockState blockState) {
            String bedrockIdentifier = this.template.bedrockIdentifier();
            if (bedrockIdentifier != null) {
                bedrockTag.putString("id", bedrockIdentifier);
            }

            for (BlockEntityPatchTemplate.TagMutation mutation : this.template.mutations()) {
                applyMutation(bedrockTag, mutation, javaTag, 0);
            }
        }

        private void applyMutation(@NotNull NbtMapBuilder target, @NotNull BlockEntityPatchTemplate.TagMutation mutation, @Nullable NbtMap javaTag, int index) {
            String key = mutation.path().get(index);
            if (index == mutation.path().size() - 1) {
                putValue(target, key, mutation.value(), javaTag);
                return;
            }

            Object existing = target.get(key);
            NbtMapBuilder compound = existing instanceof NbtMap map ? NbtMapBuilder.from(map) : NbtMap.builder();
            applyMutation(compound, mutation, javaTag, index + 1);
            target.putCompound(key, compound.build());
        }

        private void putValue(@NotNull NbtMapBuilder target, @NotNull String key, @NotNull BlockEntityPatchTemplate.TagValue value, @Nullable NbtMap javaTag) {
            switch (value.kind()) {
                case NULL -> target.remove(key);
                case BOOLEAN -> target.putBoolean(key, (Boolean) value.value());
                case INTEGER -> target.putInt(key, (Integer) value.value());
                case LONG -> target.putLong(key, (Long) value.value());
                case DOUBLE -> target.putDouble(key, (Double) value.value());
                case STRING -> target.putString(key, (String) value.value());
                case COPY_FROM_JAVA -> copyJavaValue(target, key, value, javaTag);
            }
        }

        private void copyJavaValue(@NotNull NbtMapBuilder target, @NotNull String key, @NotNull BlockEntityPatchTemplate.TagValue value, @Nullable NbtMap javaTag) {
            if (javaTag == null) {
                return;
            }

            Object sourceValue = resolveJavaValue(javaTag, value.javaSourcePath(), 0);
            if (sourceValue == null) {
                return;
            }

            if (sourceValue instanceof NbtMap map) {
                target.putCompound(key, map);
                return;
            }

            target.put(key, sourceValue);
        }

        @Nullable
        private Object resolveJavaValue(@Nullable Object current, @Nullable java.util.List<String> path, int index) {
            if (current == null || path == null) {
                return null;
            }
            if (index == path.size()) {
                return current;
            }
            String segment = path.get(index);
            if (current instanceof NbtMap map) {
                return resolveJavaValue(map.get(segment), path, index + 1);
            }
            if (current instanceof java.util.List<?> list) {
                Integer listIndex = parseListIndex(segment, list.size());
                if (listIndex == null) {
                    return null;
                }
                return resolveJavaValue(list.get(listIndex), path, index + 1);
            }
            return null;
        }

        @Nullable
        private Integer parseListIndex(@NotNull String rawIndex, int size) {
            try {
                int parsed = Integer.parseInt(rawIndex);
                return parsed >= 0 && parsed < size ? parsed : null;
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
    }
}