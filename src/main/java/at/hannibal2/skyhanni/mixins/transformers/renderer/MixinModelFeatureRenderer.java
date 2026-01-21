package at.hannibal2.skyhanni.mixins.transformers.renderer;
//? > 1.21.9 {
/*import at.hannibal2.skyhanni.mixins.hooks.EntityRenderStateStore;
import at.hannibal2.skyhanni.utils.render.SkyHanniOutlineVertexConsumerProvider;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.RenderType;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.SubmitNodeStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ModelFeatureRenderer.class)
public class MixinModelFeatureRenderer {

    @WrapOperation(method = "renderModel(Lnet/minecraft/client/renderer/SubmitNodeStorage$ModelSubmit;Lnet/minecraft/client/renderer/RenderType;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/client/renderer/OutlineBufferSource;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/OutlineBufferSource;setColor(I)V"))
    private void setSkyHanniOutlineColor(OutlineBufferSource outlineConsumer, int color, Operation<Integer> original, @Local(argsOnly = true) SubmitNodeStorage.ModelSubmit<?> model) {
        if (model.state() instanceof EntityRenderStateStore currentState && currentState.skyhanni$isUsingCustomOutline()) {
            original.call(SkyHanniOutlineVertexConsumerProvider.getVertexConsumers(), color);
        } else {
            original.call(outlineConsumer, color);
        }
    }

    @WrapOperation(method = "renderModel(Lnet/minecraft/client/renderer/SubmitNodeStorage$ModelSubmit;Lnet/minecraft/client/renderer/RenderType;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/client/renderer/OutlineBufferSource;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/OutlineBufferSource;getBuffer(Lnet/minecraft/client/renderer/RenderType;)Lcom/mojang/blaze3d/vertex/VertexConsumer;"))
    private VertexConsumer getSkyHanniOutlineBuffer(OutlineBufferSource outlineConsumer, RenderType layer, Operation<VertexConsumer> original, @Local(argsOnly = true) SubmitNodeStorage.ModelSubmit<?> model) {
        if (model.state() instanceof EntityRenderStateStore currentState && currentState.skyhanni$isUsingCustomOutline()) {
            return original.call(SkyHanniOutlineVertexConsumerProvider.getVertexConsumers(), layer);
        } else {
            return original.call(outlineConsumer, layer);
        }
    }

}
*///?}
