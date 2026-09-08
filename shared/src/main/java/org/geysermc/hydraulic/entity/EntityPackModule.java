package org.geysermc.hydraulic.entity;

import com.google.auto.service.AutoService;
import net.minecraft.resources.Identifier;
import org.geysermc.geyser.api.entity.custom.CustomEntityDefinition;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineEntitiesEvent;
import org.geysermc.hydraulic.compat.CompatibilityProfile;
import org.geysermc.hydraulic.compat.CompatibilityRegistry;
import org.geysermc.hydraulic.compat.MappingResolver;
import org.geysermc.hydraulic.compat.model.CompatibilityObject;
import org.geysermc.hydraulic.compat.model.SupportLevel;
import org.geysermc.hydraulic.compat.model.SupportResult;
import org.geysermc.hydraulic.compat.runtime.CompatibilityDecisions;
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
        CompatibilityProfile profile = compatibilityRegistry.report().profile(context.mod().id());
        if (profile == null) {
            return;
        }

        GeyserDefineEntitiesEvent event = context.event();
        int registered = 0;
        for (CompatibilityObject object : profile.objects()) {
            if (!object.contentType().equals("entity")) {
                continue;
            }
            if (!CompatibilityDecisions.allowsCustomEntityRegistration(object)) {
                context.logger().info("Skipping custom entity registration for {} because {}", object.javaIdentifier(), CompatibilityDecisions.entityRegistrationReason(object));
                continue;
            }

            Identifier javaIdentifier = Identifier.parse(object.javaIdentifier());
            MappingResolver.ResolvedIdentifier resolved = compatibilityRegistry.mappingResolver().resolveEntityIdentifier(javaIdentifier);
            if (containsDefinition(event, resolved.identifier().toString())) {
                continue;
            }

            event.register(CustomEntityDefinition.of(resolved.identifier().toString()));
            registered++;
            SupportResult behavior = object.supportResults().get("behavior");
            String behaviorTag = object.inventoryFacts().get("behavior_tag");
            if (behavior != null && behavior.level() != SupportLevel.NATIVE && behavior.level() != SupportLevel.AUTOMATIC && behavior.level() != SupportLevel.ADAPTED) {
                if (behaviorTag == null || behaviorTag.isBlank()) {
                    context.logger().info("Registered metadata-backed custom entity definition for {} as {} while behavior support remains {}", object.javaIdentifier(), resolved.identifier(), behavior.level());
                } else {
                    context.logger().info("Registered metadata-backed custom entity definition for {} as {} while behavior support remains {} (tag: {})", object.javaIdentifier(), resolved.identifier(), behavior.level(), behaviorTag);
                }
            } else {
                context.logger().info("Registered custom entity definition for {} as {}", object.javaIdentifier(), resolved.identifier());
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
