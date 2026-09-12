package org.geysermc.hydraulic.mixin.ext;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.kyori.adventure.key.Key;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import team.unnamed.creative.metadata.pack.PackFormat;
import team.unnamed.creative.model.ItemTransform;
import team.unnamed.creative.model.Model;

import java.util.Map;

@Mixin(targets = "team.unnamed.creative.serialize.minecraft.model.ModelSerializer", remap = false)
public class ModelSerializerMixin {
    @Inject(
        method = "deserializeFromJson(Lcom/google/gson/JsonElement;Lnet/kyori/adventure/key/Key;Lteam/unnamed/creative/metadata/pack/PackFormat;)Lteam/unnamed/creative/model/Model;",
        at = @At("HEAD")
    )
    private void normalizeModelRotations(JsonElement node, Key key, PackFormat packFormat, CallbackInfoReturnable<Model> cir) {
        if (node == null || !node.isJsonObject()) {
            return;
        }
        JsonObject obj = node.getAsJsonObject();
        if (obj.has("elements") && obj.get("elements").isJsonArray()) {
            for (JsonElement element : obj.getAsJsonArray("elements")) {
                if (element.isJsonObject()) {
                    JsonObject elementObj = element.getAsJsonObject();
                    if (elementObj.has("rotation") && elementObj.get("rotation").isJsonObject()) {
                        JsonObject rotObj = elementObj.getAsJsonObject("rotation");
                        if (rotObj.has("angle") && rotObj.get("angle").isJsonPrimitive() && rotObj.get("angle").getAsJsonPrimitive().isNumber()) {
                            float angle = rotObj.get("angle").getAsFloat();
                            rotObj.addProperty("angle", snapValidAngle(angle));
                        }
                    }
                }
            }
        }
    }

    private static float snapValidAngle(float angle) {
        float[] valid = {-45.0f, -22.5f, 0.0f, 22.5f, 45.0f};
        float closest = 0.0f;
        float minDiff = Float.MAX_VALUE;
        for (float v : valid) {
            float diff = Math.abs(angle - v);
            if (diff < minDiff) {
                minDiff = diff;
                closest = v;
            }
        }
        return closest;
    }

    @Redirect(
        method = "deserializeFromJson(Lcom/google/gson/JsonElement;Lnet/kyori/adventure/key/Key;Lteam/unnamed/creative/metadata/pack/PackFormat;)Lteam/unnamed/creative/model/Model;",
        at = @At(
            value = "INVOKE",
            target = "Lteam/unnamed/creative/model/ItemTransform$Type;valueOf(Ljava/lang/String;)Lteam/unnamed/creative/model/ItemTransform$Type;"
        )
    )
    private ItemTransform.Type redirectItemTransformTypeValueOf(String name) {
        // Redirect the ItemTransform.Type.valueOf to return null instead of throwing an exception
        // This prevents an item from failing to register when a mod is using old types
        try {
            return ItemTransform.Type.valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Redirect(
        method = "deserializeFromJson(Lcom/google/gson/JsonElement;Lnet/kyori/adventure/key/Key;Lteam/unnamed/creative/metadata/pack/PackFormat;)Lteam/unnamed/creative/model/Model;",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"
        )
    )
    private Object redirectDisplayMapPut(Map<Object, Object> instance, Object k, Object v) {
        // If the type is null, we skip adding it to the map
        if (k == null) {
            return null;
        }

        return instance.put(k, v);
    }
}
