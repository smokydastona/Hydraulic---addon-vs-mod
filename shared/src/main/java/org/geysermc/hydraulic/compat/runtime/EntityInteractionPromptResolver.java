package org.geysermc.hydraulic.compat.runtime;

import net.minecraft.resources.Identifier;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.hydraulic.compat.CompatibilityRegistry;
import org.geysermc.hydraulic.compat.ir.CompiledCompatibilityPlan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class EntityInteractionPromptResolver {
    private EntityInteractionPromptResolver() {
    }

    public static void apply(@NotNull GeyserSession session, @NotNull Entity entity, @NotNull CompatibilityRegistry compatibilityRegistry) {
        String javaIdentifier = CompatibilityRuntimeDiagnostics.resolveJavaEntityIdentifier(session, entity);
        if (javaIdentifier == null) {
            return;
        }

        CompiledCompatibilityPlan plan = compatibilityRegistry.dispatchTable().entity(Identifier.parse(javaIdentifier));
        String prompt = resolvePrompt(plan);
        if (prompt == null) {
            return;
        }

        session.getPlayerEntity().getMetadata().put(EntityDataTypes.INTERACT_TEXT, prompt);
        session.getPlayerEntity().updateBedrockMetadata();
    }

    @Nullable
    static String resolvePrompt(@Nullable CompiledCompatibilityPlan plan) {
        if (!BridgeAdapterSupport.supportsEntityInteractionPrompt(plan)) {
            return null;
        }
        return plan.interactionPrompt();
    }
}