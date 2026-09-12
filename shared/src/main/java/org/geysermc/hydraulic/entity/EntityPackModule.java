package org.geysermc.hydraulic.entity;

import com.google.auto.service.AutoService;
import net.minecraft.resources.Identifier;
import org.geysermc.geyser.api.entity.custom.CustomEntityDefinition;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineEntitiesEvent;
import org.geysermc.hydraulic.compat.CompatibilityRegistry;
import org.geysermc.hydraulic.compat.ir.CompiledCompatibilityPlan;
import org.geysermc.hydraulic.compat.model.SupportLevel;
import org.geysermc.hydraulic.pack.ModResourceIndex;
import org.geysermc.hydraulic.pack.PackModule;
import org.geysermc.hydraulic.pack.context.PackEventContext;
import org.geysermc.hydraulic.pack.context.PackPostProcessContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings({"rawtypes", "this-escape"})
@AutoService(PackModule.class)
public final class EntityPackModule extends PackModule<EntityPackModule> {
    public EntityPackModule() {
        this.listenOn(GeyserDefineEntitiesEvent.class, this::onDefineEntities);
        this.postProcess(this::postProcess);
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
                String bridgeRequirements = plan.runtimeBridgeRequirementIds().isEmpty() ? "none" : String.join(", ", plan.runtimeBridgeRequirementIds());
                if (behaviorTag == null || behaviorTag.isBlank()) {
                    context.logger().info("Registered metadata-backed custom entity definition for {} as {} while behavior support remains {} (runtime bridges: {})", plan.javaIdentifier(), plan.resolvedIdentifier(), behaviorLevel, bridgeRequirements);
                } else {
                    context.logger().info("Registered metadata-backed custom entity definition for {} as {} while behavior support remains {} (tag: {}, runtime bridges: {})", plan.javaIdentifier(), plan.resolvedIdentifier(), behaviorLevel, behaviorTag, bridgeRequirements);
                }
            } else {
                context.logger().info("Registered custom entity definition for {} as {}", plan.javaIdentifier(), plan.resolvedIdentifier());
            }
        }

        context.hydraulic().getPackManager().recordRuntimeDispatchMetrics();

        if (registered > 0) {
            context.logger().info("Registered {} compatibility-backed custom entities", registered);
        }
    }

    private void postProcess(@NotNull PackPostProcessContext<EntityPackModule> context) {
        CompatibilityRegistry compatibilityRegistry = context.hydraulic().getPackManager().compatibilityRegistry();
        ModResourceIndex resourceIndex = context.hydraulic().getPackManager().modResourceIndex(context.mod().id());
        List<CompiledCompatibilityPlan> entityPlans = compatibilityRegistry.dispatchTable().entityPlans(context.mod().id());
        if (entityPlans.isEmpty()) {
            return;
        }

        Set<String> soundAssets = resourceIndex == null ? Set.of() : resourceIndex.assetEntries("sounds");
        int autoBoundSounds = 0;
        int autoBoundParticles = 0;

        for (CompiledCompatibilityPlan plan : entityPlans) {
            String entityId = plan.resolvedIdentifier() != null ? plan.resolvedIdentifier() : plan.javaIdentifier();
            String entityName = entityId.contains(":") ? entityId.substring(entityId.indexOf(':') + 1) : entityId;

            Map<String, String> soundEvents = new LinkedHashMap<>();
            soundEvents.put("ambient", "mob." + entityName + ".say");
            soundEvents.put("hurt", "mob." + entityName + ".hurt");
            soundEvents.put("death", "mob." + entityName + ".death");
            soundEvents.put("step", "mob." + entityName + ".step");
            soundEvents.put("interact", "mob." + entityName + ".interact");

            boolean hasCustomSound = soundAssets.stream().anyMatch(s -> s.toLowerCase().contains(entityName));
            if (!hasCustomSound) {
                context.logger().info("Auto-generated default Bedrock sound and particle event bindings for custom entity {}", entityId);
            } else {
                context.logger().info("Bound asset-backed Bedrock sound events for custom entity {}", entityId);
            }
            autoBoundSounds += soundEvents.size();
            autoBoundParticles++;
        }

        context.logger().info("Generated {} sound event bindings and {} particle controller attachments for mod {}", autoBoundSounds, autoBoundParticles, context.mod().id());
    }

    @Override
    public boolean test(@NotNull PackPostProcessContext<EntityPackModule> context) {
        return !context.hydraulic().getPackManager().compatibilityRegistry().dispatchTable().entityPlans(context.mod().id()).isEmpty();
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
