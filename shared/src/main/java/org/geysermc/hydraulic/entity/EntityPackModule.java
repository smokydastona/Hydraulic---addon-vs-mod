package org.geysermc.hydraulic.entity;

import com.google.auto.service.AutoService;
import net.minecraft.resources.Identifier;
import org.geysermc.geyser.api.entity.custom.CustomEntityDefinition;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineEntitiesEvent;
import org.geysermc.hydraulic.compat.CompatibilityRegistry;
import org.geysermc.hydraulic.compat.ir.CompiledCompatibilityPlan;
import org.geysermc.hydraulic.compat.model.SupportLevel;
import org.geysermc.hydraulic.pack.PackModule;
import org.geysermc.hydraulic.pack.context.PackEventContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings({"rawtypes", "this-escape"})
@AutoService(PackModule.class)
public final class EntityPackModule extends PackModule<EntityPackModule> {
    public EntityPackModule() {
        this.listenOn(GeyserDefineEntitiesEvent.class, this::onDefineEntities);
    }

    private void onDefineEntities(@NotNull PackEventContext<GeyserDefineEntitiesEvent, EntityPackModule> context) {
        CompatibilityRegistry compatibilityRegistry = context.hydraulic().getPackManager().compatibilityRegistry();
        GeyserDefineEntitiesEvent event = context.event();
        int registered = 0;
        for (CompiledCompatibilityPlan plan : compatibilityRegistry.dispatchTable().entityPlans(context.mod().id())) {
            if (!plan.allowsCustomRegistration()) {
                context.logger().info("Skipping custom entity registration for {} because {}", plan.javaIdentifier(), plan.customRegistrationReason());
                continue;
            }

            if (plan.resolvedIdentifier() == null || containsDefinition(event, plan.resolvedIdentifier())) {
                continue;
            }

            event.register(CustomEntityDefinition.of(plan.resolvedIdentifier()));
            registered++;
            SupportLevel behaviorLevel = plan.behaviorLevel();
            String behaviorTag = plan.behaviorTag();
            if (behaviorLevel != null && behaviorLevel != SupportLevel.NATIVE && behaviorLevel != SupportLevel.AUTOMATIC && behaviorLevel != SupportLevel.ADAPTED) {
                if (behaviorTag == null || behaviorTag.isBlank()) {
                    context.logger().info("Registered metadata-backed custom entity definition for {} as {} while behavior support remains {}", plan.javaIdentifier(), plan.resolvedIdentifier(), behaviorLevel);
                } else {
                    context.logger().info("Registered metadata-backed custom entity definition for {} as {} while behavior support remains {} (tag: {})", plan.javaIdentifier(), plan.resolvedIdentifier(), behaviorLevel, behaviorTag);
                }
            } else {
                context.logger().info("Registered custom entity definition for {} as {}", plan.javaIdentifier(), plan.resolvedIdentifier());
            }
        }

        if (registered > 0) {
            context.logger().info("Registered {} compatibility-backed custom entities", registered);
        }
    }

    private boolean containsDefinition(@NotNull GeyserDefineEntitiesEvent event, @NotNull String identifier) {
        for (CustomEntityDefinition definition : event.customEntities()) {
            if (sameIdentifier(definition.identifier(), identifier)) {
                return true;
            }
        }
        return false;
    }

    private boolean sameIdentifier(@Nullable org.geysermc.geyser.api.util.Identifier existing, @NotNull String candidate) {
        return existing != null && (existing.namespace() + ":" + existing.path()).equals(candidate);
    }
}
