package org.geysermc.hydraulic.compat.runtime;

import net.minecraft.resources.Identifier;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.geysermc.geyser.level.block.type.BlockState;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.level.block.entity.BlockEntityTranslator;
import org.geysermc.hydraulic.compat.CompatibilityRegistry;
import org.geysermc.hydraulic.compat.mapping.ContentPatch;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

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

        List<ContentPatch> patches = compatibilityRegistry.metadataIndex().contentPatches(Identifier.parse(javaIdentifier));
        BlockEntityPatchTemplate template = BlockEntityPatchTemplate.resolve(patches);
        if (template == null) {
            return null;
        }
        return new MetadataBackedBlockEntityTranslator(template);
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
                applyMutation(bedrockTag, mutation, 0);
            }
        }

        private void applyMutation(@NotNull NbtMapBuilder target, @NotNull BlockEntityPatchTemplate.TagMutation mutation, int index) {
            String key = mutation.path().get(index);
            if (index == mutation.path().size() - 1) {
                putScalar(target, key, mutation.value());
                return;
            }

            Object existing = target.get(key);
            NbtMapBuilder compound = existing instanceof NbtMap map ? NbtMapBuilder.from(map) : NbtMap.builder();
            applyMutation(compound, mutation, index + 1);
            target.putCompound(key, compound.build());
        }

        private void putScalar(@NotNull NbtMapBuilder target, @NotNull String key, @NotNull BlockEntityPatchTemplate.TagValue value) {
            switch (value.kind()) {
                case NULL -> target.remove(key);
                case BOOLEAN -> target.putBoolean(key, (Boolean) value.value());
                case INTEGER -> target.putInt(key, (Integer) value.value());
                case LONG -> target.putLong(key, (Long) value.value());
                case DOUBLE -> target.putDouble(key, (Double) value.value());
                case STRING -> target.putString(key, (String) value.value());
            }
        }
    }
}