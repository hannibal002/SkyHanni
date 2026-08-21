package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.mixins.hooks.EntityRenderDispatcherHook;
import net.minecraft.client.model.Model;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(Model.class)
public abstract class MixinModel {
    @ModifyArg(method = "renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/geom/ModelPart;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V"), index = 4)
    private int modifyRenderAlpha(int argb) {
        Integer entityAlpha = EntityRenderDispatcherHook.getEntityTransparency();
        if (entityAlpha != null) {
            int oldAlpha = (argb >> 24) & 0xFF;
            int newAlpha = Math.min(oldAlpha, entityAlpha);

            argb &= 0xFFFFFF;
            argb |= newAlpha << 24;
        }
        return argb;
    }
}
