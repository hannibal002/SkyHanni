package at.hannibal2.hanni.mixins.transformers.renderer;

import at.hannibal2.hanni.mixins.hooks.EntityRenderStateStore;
import at.hannibal2.hanni.utils.render.HanniOutlineVertexConsumerProvider;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.render.OutlineVertexConsumerProvider;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.command.ModelCommandRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueueImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ModelCommandRenderer.class)
public class MixinModelCommandRenderer {

    @WrapOperation(method = "render(Lnet/minecraft/client/render/command/OrderedRenderCommandQueueImpl$ModelCommand;Lnet/minecraft/client/render/RenderLayer;Lnet/minecraft/client/render/VertexConsumer;Lnet/minecraft/client/render/OutlineVertexConsumerProvider;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/OutlineVertexConsumerProvider;setColor(I)V"))
    private void setHanniOutlineColor(OutlineVertexConsumerProvider outlineConsumer, int color, Operation<Integer> original, @Local(argsOnly = true) OrderedRenderCommandQueueImpl.ModelCommand<?> model) {
        if (model.state() instanceof EntityRenderStateStore currentState && currentState.hanni$isUsingCustomOutline()) {
            original.call(HanniOutlineVertexConsumerProvider.getVertexConsumers(), color);
        } else {
            original.call(outlineConsumer, color);
        }
    }

    @WrapOperation(method = "render(Lnet/minecraft/client/render/command/OrderedRenderCommandQueueImpl$ModelCommand;Lnet/minecraft/client/render/RenderLayer;Lnet/minecraft/client/render/VertexConsumer;Lnet/minecraft/client/render/OutlineVertexConsumerProvider;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/OutlineVertexConsumerProvider;getBuffer(Lnet/minecraft/client/render/RenderLayer;)Lnet/minecraft/client/render/VertexConsumer;"))
    private VertexConsumer getHanniOutlineBuffer(OutlineVertexConsumerProvider outlineConsumer, RenderLayer layer, Operation<VertexConsumer> original, @Local(argsOnly = true) OrderedRenderCommandQueueImpl.ModelCommand<?> model) {
        if (model.state() instanceof EntityRenderStateStore currentState && currentState.hanni$isUsingCustomOutline()) {
            return original.call(HanniOutlineVertexConsumerProvider.getVertexConsumers(), layer);
        } else {
            return original.call(outlineConsumer, layer);
        }
    }

}
