package at.hannibal2.skyhanni.mixins.transformers.renderer;

import at.hannibal2.skyhanni.mixins.hooks.GlowingStateStore;
import at.hannibal2.skyhanni.utils.render.SkyHanniOutlineVertexConsumerProvider;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.render.OutlineVertexConsumerProvider;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.command.ItemCommandRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueueImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ItemCommandRenderer.class)
public class MixinItemCommandRenderer {

    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/OutlineVertexConsumerProvider;setColor(I)V"))
    private void setSkyHanniOutlineColor(OutlineVertexConsumerProvider outlineConsumer, int i, Operation<Void> original, @Local OrderedRenderCommandQueueImpl.ItemCommand itemCommand) {
        Object obj = (Object) itemCommand;
        if (obj instanceof GlowingStateStore casted && casted.skyhanni$isUsingCustomOutline()) {
            original.call(SkyHanniOutlineVertexConsumerProvider.getVertexConsumers(), i);
        } else {
            original.call(outlineConsumer, i);
        }
    }

    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/item/ItemRenderer;renderItem(Lnet/minecraft/item/ItemDisplayContext;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;II[ILjava/util/List;Lnet/minecraft/client/render/RenderLayer;Lnet/minecraft/client/render/item/ItemRenderState$Glint;)V", ordinal = 1), index = 2)
    private VertexConsumerProvider modifyOutlineVertexConsumerProvider(VertexConsumerProvider outlineConsumer, @Local OrderedRenderCommandQueueImpl.ItemCommand itemCommand) {
        Object obj = (Object) itemCommand;
        if (obj instanceof GlowingStateStore casted && casted.skyhanni$isUsingCustomOutline()) {
            return SkyHanniOutlineVertexConsumerProvider.getVertexConsumers();
        } else {
            return outlineConsumer;
        }
    }
}
