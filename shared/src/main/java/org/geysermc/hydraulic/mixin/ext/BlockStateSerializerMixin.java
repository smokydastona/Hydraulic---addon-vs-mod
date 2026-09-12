package org.geysermc.hydraulic.mixin.ext;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.kyori.adventure.key.Key;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import team.unnamed.creative.blockstate.BlockState;
import team.unnamed.creative.metadata.pack.PackFormat;
import team.unnamed.creative.serialize.minecraft.blockstate.BlockStateSerializer;

@Mixin(value = BlockStateSerializer.class, remap = false)

public class BlockStateSerializerMixin {
    private static Logger LOGGER = LoggerFactory.getLogger("BlockStateSerializerMixin");

    /**
     * This mixin prevents loading of the old forge_marker blockstates format as they are no longer supported
     * <p>
     * forge_marker was removed in 1.15 see <a href="https://docs.minecraftforge.net/en/1.14.x/models/blockstates/forgeBlockstates/">here</a> for more info
     */
    @Inject(
        method = "deserializeFromJson(Lcom/google/gson/JsonElement;Lnet/kyori/adventure/key/Key;Lteam/unnamed/creative/metadata/pack/PackFormat;)Lteam/unnamed/creative/blockstate/BlockState;",
        at = @At("HEAD"),
        cancellable = true
    )
    private void deserializeFromJson(JsonElement node, Key key, PackFormat packFormat, CallbackInfoReturnable<BlockState> cir) {
        if (!node.isJsonObject()) {
            return;
        }
        JsonObject objectNode = node.getAsJsonObject();
        if (objectNode.has("forge_marker")) {
            // This was removed from forge in 1.15
            LOGGER.warn("Ignoring forge_marker blockstate: " + key);
            cir.setReturnValue(null);
            return;
        }
        normalizeRotations(objectNode);
    }

    private static void normalizeRotations(JsonElement element) {
        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            if (obj.has("x") && obj.get("x").isJsonPrimitive() && obj.get("x").getAsJsonPrimitive().isNumber()) {
                int x = obj.get("x").getAsInt();
                int normalized = ((x % 360) + 360) % 360;
                obj.addProperty("x", normalized);
            }
            if (obj.has("y") && obj.get("y").isJsonPrimitive() && obj.get("y").getAsJsonPrimitive().isNumber()) {
                int y = obj.get("y").getAsInt();
                int normalized = ((y % 360) + 360) % 360;
                obj.addProperty("y", normalized);
            }
            for (java.util.Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                normalizeRotations(entry.getValue());
            }
        } else if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                normalizeRotations(child);
            }
        }
    }
}
